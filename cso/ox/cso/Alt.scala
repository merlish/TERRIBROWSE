/*

Copyright © 2007, 2008 Bernard Sufrin, Worcester College, Oxford University

Licensed under the Artistic License, Version 2.0 (the "License"). 

You may not use this file except in compliance with the License. 

You may obtain a copy of the License at 

    http://www.opensource.org/licenses/artistic-license-2.0.php

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific
language governing permissions and limitations under the
License.

*/


package ox.cso
import ox.CSO
/**
    An <tt>Alt</tt> is a function-like object constructed from a sequence of guarded
    <tt>Alt.Event</tt>s (of which the most familiar kind are <tt>InPort.Event</tt>s); 
    for example:     
    <pre>
    val f =  new Alt ( List(event1, ... , eventN) )
    </pre> 
    
    An <tt>InPort.Event</tt> usually takes one of the forms: 
    <pre>
       inport (guard)     ==> { bv => cmd }     
       (guard &amp;&amp;&amp; inport) ==> { bv => cmd } 
       inport             ==> { bv => cmd }
    </pre>
    
    (in the third form the guard is implicitly <tt>true</tt>.)
    <p>
    Whenever the guard is true <i>and the port is open</i>, the
    event is said to be enabled. When the port of an enabled event
    is ready to read, the event is said to be ready.  We <i>fire</i>
    an event by evaluating the expression <code>{ bv => cmd
    }(inport?)</code>: <i>i.e.</i> we read a value from the port, then
    apply the function <code>{ bv => cmd }</code> to that value.
    
    <p>
    Additional <code>InPort.Event</code>s take the form:
    <pre>
       inport (guard)     ==>> { bv => cmd }    
       (guard &amp;&amp;&amp; inport) ==>> { bv => cmd } 
       inport             ==>> { bv => cmd }
    </pre>
    
    (in the third form the guard is implicitly <tt>true</tt>.)
    <p>
    We <i>fire</i>
    such events by evaluating the expression <code>(inport?{ bv => cmd })</code>:
    <i>i.e.</i> we perform an extended rendezvous with the port.
    
    <p>
    <b>[Primitive notation]</b>
    The above forms are syntactic sugar for various commands built using
    the following more primitive notation, in which the <code>command</code>
    is required to read from the <code>inport</code>.
    <pre>
       inport (guard)     -?-> { command } 
       (guard &amp;&amp;&amp; inport) -?-> { command } 
       inport             -?-> { command }
    </pre>    
    <p>
    <b>[Pseudo-events]</b>
    There are also two forms of ''pseudo-event'', namely:
    <pre>
       after(expression) ==> { cmd } 
       orelse            ==> { cmd }
    </pre> 
    (no more than one of these may appear).
    
    <p>  
    When <tt>f: Alt</tt> is applied, as <tt>f()</tt>, the guards are first
    evaluated (in the case of an after guard, the Long-valued expression
    is evaluated and determines a timeout in milliseconds). 
    If any of the enabled events is ready, then one of them
    is chosen (nondeterministically), and the event is fired.  [See Note 1]
    
    <p>
    If no enabled event is ready, then the current thread/process
    is suspended until one becomes ready.
    
    <p>
    If no event is enabled, or if they all become disabled while
    the thread is suspended [See Note 2], then an <code>Abort</code> <code>Stop</code>
    exception is thrown (unless the <code>Alt</code> has an
    <code>orelse</code> event associated with it, in which case
    that event's command is executed.
    
    <p>
    If a timeout event is specified (with an <code>after(expression)==>{
    cmd } </code>) guard, and the timeout elapses before any other
    event becomes ready, then the timeout command is executed.
    
    <p>
    <b>[OutPort Events]</b>
    Although they do not appear in classical <b>occam</b>, CSO also implements
    output port events which can also participate in <tt>Alt</tt>s.
    See the documentation of <tt>OutPort</tt> for details.
    
    <p>
    <b>[Restrictions]</b>
    <ul>
      <li>Guards must be side-effect free; as must the timeout 
          specification expression in an <code>after</code> event.</li>
      <li>No  more than one <code>after</code> event may appear.</li>
      <li>No  more than one <code>orelse</code> event may appear.</li>
      <li>A channel's input and output
          ports may not both simultaneously 
          participate in <tt>Alt<tt>s.</li>
    </ul>
    
    <p>
    <b>[Note 1]</b> This implementation is ''fair'' inasmuch as
    successive applications of the same <code>Alt</code> object in
    which the same collection of events turn out to be ready result
    in distinct events being fired.
    
    The algorithm used is adapted from the algorithm in the
    paper: <i>The meaning and implementation of PRI ALT in
    <b>occam</b></i>, by Geoff Barrett, Michael Goldsmith, Geraint
    Jones, and Andrew Kay (1988).  This paper can be found at:
           <a href="ftp://ftp.comlab.ox.ac.uk/pub/Documents/techpapers/Geraint.Jones/OCCAM-1-88.ps.Z">
              <code>ftp://ftp.comlab.ox.ac.uk/pub/Documents/techpapers/Geraint.Jones/OCCAM-1-88.ps.Z</code>
           </a>
    <p> 
    The adaptation adds timeouts. ''Fairness'' is impolemented in a non priority
    alt by starting the scan for a ready port just after the most recently-selected
    port.
    
    <p>
    <b>[Note 2]</b> The only way an <code>InPort.Event</code> can
    become disabled during an alt's wait is by the closure of its port.
       
       
        
    @author Bernard Sufrin, Oxford
    @version $Revision: 509 $ $Date: 2011-01-19 18:58:28 +0000 (Wed, 19 Jan 2011) $
             
*/
class Alt(events: Seq[Alt.Event], priAlt: Boolean)
{ /** Regular constructor */
  def this(events: Seq[Alt.Event]) = this(events, false)
  
  def this(syntax: Alt.Syntax) = this(syntax.elements, false)
  
  private val eventCount = events.length

  /** Did the <code>Alt</code> timeout or was it successful */
  private   var waiting  = false
  /** Result of a <code>select</code> that timed-out */
  private   val TIMEOUT  = -1 
  /** Result of a <code>select</code> for which no enabled port was open */
  private   val ABORT    = -2
  /** Result of a <code>select</code> during which something indecisive happpened */
  private   val RETRY    = -3
  /** Initial value of a select  */
  private   val UNKNOWN  = -1
  /** The index of the  most-recently selected event */
  private   var selected = UNKNOWN
  
  
  // Additional parameters set just after construction
  /** The associated <code>onclosed</code> block; throws <code>Abort</code> by default */  
  protected var onClosed:  ()=>Unit = ()=>{ throw new Abort }
  /** The associated <code>ontimeout</code> block; throws <code>Abort</code> by default */
  protected var onTimeout: ()=>Unit = ()=>{ throw new Abort }
    
  /** repeatedly execute this <code>Alt</code> until a <code>Stop</code> exception 
      of some kind is thrown. 
  */
  def repeat = CSO.repeat { this() }
   
  /** Execute the command of a (nondeterministically chosen) enabled
      event that is ready; if necessary wait for one to become ready
      until the given timeout expires. If the timeout expires
      then execute <tt>onTimeout()</tt>. If all the channels concerned
      are closed or become closed, then execute <tt>onClosed</tt> 
  */
  def apply (): Unit= 
  { var which = select()
    while (which==RETRY) 
    { selected = selected max UNKNOWN
     
      which = select()
    }

    if (which>=0) 
       events(which).cmd() 
    else 
    if (which==TIMEOUT)  
       onTimeout()
    else
    if (which==ABORT)
       onClosed()
  }

  
  /** Passed to <code>event.isReady</code> during the enabling 
      phase of <code>select</code> 
  */
  private def notifyThis () = 
   synchronized { 
       waiting = false
       notify
   }
   
   /** On each alt round, enabled(i) == port i is open and its guard is true */
   private val enabled = new Array[Boolean](events.length)
   
  /** 
      Return the index of an enabled ready event or TIMEOUT if no
      event becomes ready before any timeout has elapsed; ABORT if
      there are no enabled events; RETRY if something happens during
      the wait that changes the set of enabled events, but does not
      cause one of them to become ready. A timeout of 0 means wait
      indefinitely.
  */
  protected def select() : Int =
  { 
    /*
       Terminology

           A port is ''fused''    if isReady(notifyThis) has been called on it
           A port is ''defused''  if isReady(null) has been called on it
           
       A fused port can set waiting = false at any time
                      
    */
    
    var start        = if (priAlt) 0 else (selected+1) % eventCount
    var someEnabled  = false
    var count        = eventCount
    var n            = start
    var waitMS       = 0L
    waiting          = true
    /*
        Fuse the enabled ports in increasing order while no readable
        one has been found
    */
    while (waiting && count!=0)
    { count -=1
      val event  = events(n);
      val guard  = event.guard()
      enabled(n) = guard
      if (guard)
      { someEnabled = true
        if (event.isReady(notifyThis)) waiting = false
      }
      else
      /*
          Is this a timeout event: if so, set up the wait and the command
          (I am a tad uncomfortable with the coding style here) 
      */
      { event match 
        { 
          case Alt.OrElseEvent(cmd) => 
          { 
            onClosed = cmd
          }
          

          case Alt.TimeoutEvent(timeMS, cmd) => 
          { 
            waitMS     = timeMS() 
            onTimeout  = cmd
          }
          
          case _ => {}
        }
      }
      n = (n+1) % eventCount
     } 
     
     /*
        If no ports are enabled the alt aborts (ports never re-open)
     */
     
     if (!someEnabled) { return ABORT } 
     
     /*
        If no enabled port is ready then wait (0 timeout means indefinitely)
     */ 
     
     /////////////////////////////////////////////////////////////////
     synchronized {
       if (waiting && someEnabled)
       {  wait(waitMS)         
          if (waiting) 
             selected = TIMEOUT 
          else 
             selected = RETRY
       }
     }
     /////////////////////////////////////////////////////////////////
     
     /* 
        The alt timed out, or an enabled fused event changed state.
        
        Defuse the enabled ports in decreasing order, selecting the
        earliest enabled one that is ready. If none is ready then
        either the alt has timed out or an event changed state
        without becoming ready (e.g.  by a channel closing). In the
        latter case the alt has to be retried.
     */
     
     while (count < eventCount)
     { count += 1
       if (n==0) n=eventCount; n-=1
       if (enabled(n) && events(n).isReady(null)) selected = n
     }
     selected
  }
  
}

/**
        A <code>PriAlt</code> behaves exactly like an <code>Alt</code> 
        except that instead of choosing nondeterministically between
        ready events to fire, it chooses the lowest-numbered
        ready event in the sequence.
*/
class PriAlt(events: Seq[Alt.Event]) extends Alt(events, true) 
{
  def this(syntax: Alt.Syntax) = this(syntax.elements)
}

object Alt
{ trait Syntax 
  { def apply() : Unit = new Alt(elements) apply
  
    def length:   Int
    def elements: Seq[Event] = 
    { val r = new Array[Event](length)
      copy(0, r)
      r
    }
    
    def copy(n: Int, v: Array[Event]): Int
    def | (b: Syntax)       : Syntax  = new Join(this, b)
    def | (t: Event)        : Syntax  = this | new Singleton(t)
    def | (t: Seq[Event])   : Syntax  = this | new Collection(t)    
    
  }
  
  private class Singleton(it: Event)  extends Syntax
  { def length = 1
    def copy(n: Int, v:Array[Event]) = { v(n)=it; n+1 }
    override def toString = it.toString
  }
  
  private class Collection(them: Seq[Event])  extends Syntax
  { def length = them.length
    def copy(n: Int, v:Array[Event]) = 
    { for (i<-0 until length) v(n+i)=them(i); n+length }
    override def toString = 
    { var s = ""
      for (it <- them) 
      { if (s!="") s+= " | "
        s+=it.toString
      }
      s
    }
  }
  
  private class Join(l: Syntax, r: Syntax) extends Syntax
  { def length = l.length+r.length
    def copy(n: Int, v:Array[Event]) = 
    { r.copy(l.copy(n, v), v) }
    override def toString = l.toString + " | " + r.toString
  }

  def  toSyntax(ev: Alt.Event): Syntax = new Singleton(ev)
  def  toSyntax(evs: Seq[Alt.Event]): Syntax = new Collection(evs)

  /** <tt>Event</tt>s are composed with <tt>|</tt> and embedded in <tt>Alt</tt>s. */
  class Event(_cmd: ()=>Unit, _guard: ()=>Boolean)
  { val cmd       = _cmd
    val guard     = _guard
    /** Abstract syntax of the alt of this and <code>other</code> */
    def | (other:  Event)      : Alt.Syntax = Alt.toSyntax(this) | other
          
    /** Abstract syntax of the alt of this and <code>others<code>other</code> */
    def | (others: Seq[Event]) : Alt.Syntax = Alt.toSyntax(this) | others  
    
    def isReady(whenFired: ()=>Unit): Boolean = false
  }
  
  /** A timeout guard is a precursor to a timeout event */
  case class TimeoutGuard(timeMS: () => Long)
  {
    def ==> (cmd: => Unit) = new TimeoutEvent(timeMS, ()=>cmd)
    def --> (cmd: => Unit) = new TimeoutEvent(timeMS, ()=>cmd)
  }
  
  /** A timeout event is NEVER ready; first-phase alt execution installs its deadline and command in the alt */
  case class TimeoutEvent(timeMS: ()=>Long, override val cmd: ()=>Unit) extends Event(cmd, (()=>false))
  { 
  }
  
  /** An orelse guard is a precursor to an orelse event */
  case object OrElseGuard
  { 
    def ==> (cmd: => Unit) = new OrElseEvent(()=>cmd) 
    def --> (cmd: => Unit) = new OrElseEvent(()=>cmd) 
  }
  

  /** An orelse event is NEVER ready; first-phase alt execution installs
      its command in the alt.  
  */  
  case class OrElseEvent(override val cmd: ()=> Unit) extends Event(cmd, (()=>false))
  { 
  }

}




















