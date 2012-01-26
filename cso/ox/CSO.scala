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


package ox;
import  ox.cso._

/**
 <p>
   This module exports the main types and control constructs
   of the CSO (Communicating Scala Objects) notation. It is
   our Intent to provide a notation for communicating objects
   that is in the spirit of the OCCAM and Eclectic CSP
   languages.
 </p>
 <p>
   A brief summary of the ideas involved appears below, but
   much of the detail of the implementation is hidden in
   classes in the package <code>ox.cso</code>.
 </p>
 <ul>
 <li> <b>Processes</b>
 <ul>
      <li> The simple process expression 
      <pre><b>proc</b>{<i>commands</i>}</pre> yields a new <i>PROC</i>ess </li>
    
      <li>The parallel composition process expressions
          <pre>  <i>PROC</i>1 || <i>PROC</i>2</pre>  
          and (more generally)     
          <pre>  <i>PROC</i>1 || <i>PROC</i>2 || ... || <i>PROC</i>n</pre>  
          yield a new process; as does the prefix form:
          <pre>  || (<b>for</b> (<i>var</i> &lt;- <i>ITERABLE</i>) <b>yield</b> <i>PROC(var)</i>)</pre>
      </li> 
        
<hr width="25%"/>  
    
      <li> If <i>PROC</i> is a process, then it can be run in one of three ways
      <ol>
           <li><i>PROC</i>()
           -- which has the following effects:
           <ul>
              <li> A simple process executes its <i>commands</i> in the current thread</li>
              <li> A composite (parallel) process: 
                      <pre>  (<i>PROC</i>1 || <i>PROC</i>2)()</pre>
               runs <i>PROC</i>1 in a new thread, 
               runs <i>PROC</i>2 in the current thread, and
               terminates when both <i>PROC</i>1 and <i>PROC</i>2 have terminated.
               <p>
               Parallel composition is essentially associative, so 
               <pre>  (<i>PROC</i>1 || <i>PROC</i>2 || ... || <i>PROC</i>n)()</pre>
               runs its component processes in <code>n-1</code> new threads
               and terminates when they have all terminated.
              </p>
              </li>
                        
           </ul>
           </li>
          <li><i>PROC</i><code>.fork</code> -- which starts running <i>PROC</i> in a 
              new thread, and returns a ''handle'' for that thread.
          </li>
          <li><i>PROC</i><code>.serve</code> -- which starts running <i>PROC</i> in a 
              new <i>daemon</i> thread, and returns a ''handle'' for that thread.
              The java documentation explains that daemon threads are ''long-lived''
              threads set up to provide services for other threads. As Long as
              there are non-daemon processes running in a program a daemon may be
              called on to serve one of them; when only daemons are left, the Java
              runtime system terminates the program.
          </li>
      </ol>
      </li>
      
 <hr width="25%"/>
      
      <li>
          <b>Syntactic Sugar:</b> Within the scope of the ox.CSO declarations and 
          in a context that requires a simple value of type <tt>PROC</tt>ess,
          a <tt>Unit</tt>-valued expression will be coerced to a
          process.
                   
          <i>
          <b>
          This notational device does not work within the <code>yield</code>-expressions
          of iterators.
          </b>
          </i>
          
          <p> For example
          <code>(left.close || right.close)</code> is the parallel
          composition of <code>proc{left.close}</code> and
          <code>proc{right.close}</code>, but the following
          will be flagged as a type error:
          <pre>
          <code>||(for (i<-0 until n) yield out(i).close</code>
          </pre>
          while the following is correct:
          <pre>
          <code>||(for (i<-0 until n) yield proc {out(i).close}</code>
          </pre>
          </p>
          
      </li>   
 </ul>
 </li>
 <hr width="50%"/>
 
 <li> <b>Channels</b>
       <p>
         A channel communicates data from its input to its output
         port. There are several types of channel implementation.
         All implementations are point-to-point (in the sense that
         each datum is communicated from a single process to a single
         process).
       </p>
       <p>
          <pre>  <code><i>chan</i>!<i>value</i></code></pre> 
          writes the <i>value</i>
          to the output port of the channel. The value can
          subsequently be read from the input port of the
          channel by  
          <pre>  <code><i>chan</i>?</code></pre>
       </p>
       <p>
         All but <code>Buf</code> channels are synchronized (in the
         sense that both communicant processes are suspended until
         data has been transferred).       
       </p>
       <p>
         Synchronized channel implementations differ only insofar as
         they permit contention between processes at the sending
         (writer) and receiving (reader) ends. Implementations that
         permit no contention at their reading (respectively writing)
         end perform a partial dynamic check that <i>can</i> catch
         two processes in contention to read (respectively write)
         from a writing (respectively reading) process that is not
         keeping up with them. <i>This check -- the best that can be
         done without static enforcement of a non-sharing rule --
         is sound, but not complete.</i>
       </p>
       <p> An <i>extended rendezvous read</i> takes the form
           <pre>  <code><i>chan</i>?<i>function</i></code></pre>
           In this case, the given function is applied to the
           value read from the input port (when it arrives), and
           the writing process is suspended until the computation
           of the function is complete.        
       </p>
       
 </li>
 
 <hr width="50%"/>
 <li> <b>Alternation</b>
    <br/>
    <ul>
    <li> <b>Semantics</b>
         The detailed semantics of alternations is explained in the
         documentation of the <code>Alt</code> class.
    </li>
    <li> <b>Notation (by example):</b>
    
    <ol><li> 
    <pre>
    <b>alt</b> (
        | left  (counter>0)  ==> { x  => print(x); print(" "); counter -= 1}
        | reset (counter==0) ==> { () => println(); counter = 10 }
        )
    </pre>
    or
    <pre>
    <b>alt</b> (
        | (counter>0 &&& left)   ==> { x  => print(x); print(" "); counter -= 1}
        | (counter==0 &&& reset) ==> { () => println(); counter = 10 }
        )
    </pre>
    If the counter is positive and the left channel is open then
    read a value from <code>left</code>, print it, then decrement
    the counter. If the counter is zero then read a () from the
    reset channel, print a newline, and restart the counter at 10.
    </li>    
    
    <li>
    <pre>
    <b>alt</b> (
        | (<b>for</b> (port<-ports) <b>yield</b> port ==> { x => println(x) })
        | quit ==> { () => <b>for</b> (port<-ports) port.closin } 
        )
    </pre>
    Read and print a value from the one of the <code>ports</code> 
    that are ready (or become ready) to be read; or close them all
    in response to reading a () from quit.
    </li>
    
    <li>
    <pre>
    <b>serve</b> (
          | (<b>for</b> (port<-ports) <b>yield</b> port ==> { x => println(x) }
          | after(400) ==> { println("------------") }
          | orelse     ==> { println("============"); stop }
          )
    </pre>
    or
    <pre>
    <b>repeatalt</b> (
          | (<b>for</b> (port<-ports) <b>yield</b> port ==> { x => println(x) }
          | after(400) ==> { println("------------") }
          | orelse     ==> { println("============"); stop }
          )
    </pre>
    Repeatedly read and print values from any of the ports that are ready.
    If there is ever a period of 400ms during which no port becomes ready,
    then print a line of dashes; and one call the ports have closed
    print a line of equals signs and stop.  
    </li>
    
    </ol>
        
    </li>   
    

 </li>
 </ul>
 
 @author  Bernard Sufrin, Oxford
 @version <pre>$Revision: 487 $ $Date: 2010-08-12 21:27:10 +0100 (Thu, 12 Aug 2010) $</pre>
*/

object CSO
{  
   /** Prototype of a process.
       @see ox.cso.Process
   */
   type PROC    = ox.cso.Process
   
   /** An input port. 
       @see ox.cso.InPort 
   */
   type InPort[+T]  = ox.cso.InPort[T]
   
   /** Alternation syntax 
       @see ox.cso.Alt 
   */
   type AltSyntax  = Alt.Syntax
   
   /** An input port. 
       @see ox.cso.InPort 
   */
   type ?[+T]  = ox.cso.InPort[T]
   
   /** An output port. 
       @see ox.cso.OutPort 
   */
   type OutPort[-T] = ox.cso.OutPort[T]
   
   /** An output port. 
       @see ox.cso.OutPort 
   */
   type ![-T] = ox.cso.OutPort[T]
   
   /** A shared input port. 
       @see ox.cso.SharedInPort 
   */
   type SharedInPort[+T]  = ox.cso.SharedInPort[T]
   
   /** A shared output port. 
       @see ox.cso.SharedOutPort 
   */
   type SharedOutPort[-T] = ox.cso.SharedOutPort[T]
   
   /** A nunshared input port. 
       @see ox.cso.SharedInPort 
   */
   type UnSharedInPort[+T]  = ox.cso.UnSharedInPort[T]
   
   /** A nunshared output port. 
       @see ox.cso.SharedOutPort 
   */
   type UnSharedOutPort[-T] = ox.cso.UnSharedOutPort[T]
   
   /** 
       A channel (communicating data from its input to its output
       port). 
       @see ox.cso.Chan 
   */
   type Chan[T]    = ox.cso.Chan[T]
   
   /** A channel implementation type: single reader; single writer 
       @see ox.cso.OneOne
    */
   type OneOne[T]  = ox.cso.OneOne[T]
   /** A channel implementation type: many writers; one reader 
       @see ox.cso.ManyOne
   */
   type ManyOne[T] = ox.cso.ManyOne[T]
   /** A channel implementation type: one writer; many readers  
       @see ox.cso.OneMany
   */
   type OneMany[T] = ox.cso.OneMany[T]
   /** A channel implementation type: many writers; many readers  
       @see ox.cso.ManyMany
   */
   type ManyMany[T] = ox.cso.ManyMany[T]
   /** A channel implementation type 
       @see ox.cso.Buf
   */
   type Buf[T] = ox.cso.Buf[T]
      
   /** A process with the given body */
   def proc (body: => Unit) : PROC = new Process (null) (()=>body)
   /** A process with the given body */
   def π (body: => Unit) : PROC = new Process (null) (()=>body)
      
   /** A named process with the given body */
   def proc (name: String) (body: => Unit) : PROC = new Process (name) (()=>body)
   /** A named process with the given body */
   def π (name: String) (body: => Unit) : PROC = new Process (name) (()=>body)
   
   /** Coerce a Unit-valued expression Into a process */
   implicit def UnitToProc(body: => Unit) : PROC = new Process (null) (()=>body)
 
   /** The process that simply terminates */
   val skip : PROC                 = proc {}
   
   /** Construct a process from the given body, and run it  
       in a thread concurrently with the current process
   */
   def fork(body: => Unit) : ox.cso.ThreadHandle = 
       new Process (null) (()=>body) . fork
      
   /** Construct a process from the given body, and run it  
       in a daemon thread concurrently with the current process
   */
   def forkdaemon(body: => Unit) : ox.cso.ThreadHandle = 
       new Process (null) (()=>body) . forkdaemon

   /** The parallel composition of a collection of processes */
   def ||   (collection: Iterable[PROC]) : PROC = || (collection.iterator)
   
   /** The parallel composition of an iterator of processes */
   def ||   (processes: Iterator[PROC]) : PROC =
   { var r = if (processes.hasNext) processes.next else skip
     for (p <- processes) r = r || p
     r
   }
                      
   /** Construct and execute the alternation of a bunch of InPort events */
   def alt(syntax: AltSyntax) : Unit = new Alt(syntax.elements, false) apply
   
   /** Construct and execute the alternation of a bunch of InPort events */
   def alt(events: Iterable[Alt.Event]) : Unit = new Alt(events) apply
   
   /** Construct and execute the prioritized alternation of a bunch of InPort events */
   def prialt(events: Iterable[Alt.Event]) : Unit =  new Alt(events, true) apply
   

   /** Construct and execute the prioritized alternation of a bunch of InPort events */
   def prialt(syntax: AltSyntax) : Unit = new Alt(syntax.elements, true) apply
   
   /** Construct the syntax of the alternation of a bunch of events */
   def |(events: Iterable[Alt.Event]) : AltSyntax = Alt.toSyntax(events)

   /** Timeout guard for an alt */
   def after(waitMS: => Long) = new Alt.TimeoutGuard(()=>waitMS)
   
   /** Orelse guard for an alt */
   val orelse = Alt.OrElseGuard
   
   /** Yield a OneOne */
   def OneOne[T]()     = new OneOne[T]()
   
   def OneOne[T](name: String) = new OneOne[T](name)
   
   /** Yield an array of OneOne */
   def OneOne[T](n: Int) = for (i<-upto(n)) yield new OneOne[T]
   
   /** Yield an array of OneOne */
   def OneOne[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new OneOne[T](name+"."+i.toString) 
   
   /** Construct a ManyOne */
   def ManyOne[T] = new ManyOne[T]
   
   /** Construct an array of ManyOne */
   def ManyOne[T](n: Int) = 
       for (i<-upto(n)) yield new ManyOne[T]    
   
   /** Construct an array of ManyOne */
   def ManyOne[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new ManyOne[T](name+"."+i.toString)    
      
   /** Construct a OneMany */
   def OneMany[T] = new OneMany[T]
   
   /** Construct an array of OneMany */
   def OneMany[T](n: Int) = for (i<-upto(n)) yield new OneMany[T]    
   
   /** Construct an array of OneMany */
   def OneMany[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new OneMany[T](name+"."+i.toString)    
      
   /** Construct a ManyOne */
   def ManyMany[T] = new ManyMany[T]
   
   /** Construct an array of ManyMany */
   def ManyMany[T](n: Int) = for (_<-upto(n)) yield new ManyMany[T]    
   
   /** Construct an array of ManyMany */
   def ManyMany[T](n: Int, name: String) = for (i<-upto(n)) yield new ManyMany[T](name+"."+i.toString)    
      
   /** Construct a Buf(size) */
   def Buf[T: Manifest](size: Int) = new Buf[T](size)
   
   /** Construct an array of Buf(size) */  
   def Buf[T: Manifest](size: Int, n: Int) = for (_<-upto(n)) yield new Buf[T](size) 
      
   /** Construct a OneOneBuf(size) */
   def OneOneBuf[T: Manifest](size: Int) = new OneOneBuf[T](size)
   
   /** Construct an array of OneOneBuf(size) */  
   def OneOneBuf[T: Manifest](size: Int, n: Int) = for (_<-upto(n)) yield new OneOneBuf[T](size) 
      
   /** Repeatedly execute <code>cmd</code> until it throws a
       <code>Stop</code>.
   */
   def repeat (cmd: => Unit) : Unit =
   { var go = true;
     while (go) try { cmd } catch { case ox.cso.Stop(_,_) => go=false }
   }
   
   /**
        Repeatedly apply an alternation constructed from the given Alt
        syntax. This is equivalent to:
        <code> {val a=alt(syntax); repeat { a() }} </code>
   */
   def serve (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements) repeat
   }
   
   def serve (events: Iterable[Alt.Event]) : Unit =
   { 
     new Alt(events) repeat
   }
   
   /**
        Repeatedly apply a priority alternation constructed from the given Alt
        syntax. This is equivalent to:
        <code> {val a=prialt(syntax); repeat { a() }} </code>
   */
   def priserve (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements, true) repeat
   }
   
   /**
        Repeatedly apply an alternation constructed from the given Alt
        syntax. This is equivalent to:
        <code> {val a=alt(syntax); repeat { a() }} </code>
   */
   def repeatalt (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements) repeat
   }
   
   def repeatAlt (events: Iterable[Alt.Event]) : Unit =
   { 
     new Alt(events) repeat
   }
   
   /**
        Repeatedly apply a priority alternation constructed from the given Alt
        syntax. This is equivalent to:
        <code> {val a=prialt(syntax); repeat { a() }} </code>
   */
   def repeatprialt (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements, true) repeat
   }
   
   /** Repeatedly execute <code>cmd</code> while <code>guard</code>
       is true until it throws a <code>Stop</code>  or terminates.
   */
   def repeat (guard: => Boolean) (cmd: => Unit) : Unit =
   { var go = guard;
     while (go) try { cmd; go=guard } catch { case ox.cso.Stop(_,_) => go=false }
   }   
         
   /** Execute <code>cmd</code>. If it throws a <code>Stop</code>
       then execute <code>alt</code>
   */
   def attempt (cmd: => Unit) (alt: => Unit) : Unit =
   { try { cmd } catch { case ox.cso.Stop(_,_) => alt } }

   /** Break out of a repeat, or fail an attempt */
   def stop = throw new ox.cso.Stop("stop", null)
   
   /** Convenience object for matching ox.cso.Stop exceptions */
   object Stop
   { def unapply(v: ox.cso.Stop) : Option[(String, Throwable)] =
         v match { 
           case ox.cso.Stop(s,t) => new Some((s,t)) 
           //case _ => None
         }
   }
   
   /** Convenience object for matching ox.cso.Closed exceptions */
   object Closed
   { def unapply(v: ox.cso.Stop) : Option[(String)] =
         v match { 
           case ox.cso.Stop(s, t) => if (v.isInstanceOf[ox.cso.Closed]) new Some((s)) else None
         }
   }
   
   /** Convenience object for matching ox.cso.Abort exceptions */
   object Abort
   { def unapply(v: ox.cso.Stop) : Option[Unit] =
         v match { 
           case ox.cso.Stop(s, t) => if (v.isInstanceOf[ox.cso.Abort]) new Some(()) else None
         }
   }
   
   /** Maps an iterable Into a sequence */    
   implicit def toSeq[T](it: Iterable[T]): Seq[T] =
   if (it.isInstanceOf[Seq[T]])
      it.asInstanceOf[Seq[T]]
   else
   { val seq = new scala.collection.mutable.ArrayBuffer[T]
     seq ++= it
     seq
   } 
   
   /** Maps a Boolean Into a pre-guard as part of the formation of
       guarded I/O events of the form:
       <code>(Boolean &amp&amp;&amp; IOport) --&gt; { command }</code>
   */
   implicit def toGuard(guard: => Boolean): PreGuard = new PreGuard(()=>guard)
   
   /** Syntactic precursor of a guarded I/O event */
   protected class PreGuard(guard: ()=>Boolean)
   { 
     def &&&[T](port: ?[T]): InPort.GuardedInPortEvent[T] = 
         new InPort.GuardedInPortEvent[T](port, ()=>port.isOpen && guard())
     
     def &&&[T](port: ![T]): OutPort.GuardedOutPortEvent[T] =
         new OutPort.GuardedOutPortEvent[T](port, ()=>port.isOpenForWrite && guard())
   }
   
  /** The currently-running thread sleeps for the specified time milliseconds) */
  def sleep(ms: Long) = Thread.sleep(ms)
  
  /** Threadpools (if any) are closed down cleanly. The effect of this is to
      terminate any ``resting'' pooled threads immediately. That does not
      in itself cause the currently-running program to exit; nor does
      it prevent new processes being run (and generating new threads).
      The correct code to exit a program immediately is:
      <pre><tt> { CSO.exit; System.exit(anInteger)} </tt></pre>
      Without the <tt>CSO.exit</tt> call the program waits
      for its resting pooled threads to die their natural deaths, and
      the length of time this will take depends on the <i>keepalive</i> time for 
      pooled threads -- set by JVM property
      <tt>ox.cso.pool</tt>
      
      @see PooledExecutor
  */
  def exit { ThreadHandle.exit }
  
  /** Coercion of an Alt.Event Into the syntax of an an Alt.
      This is to permit single-branched alts to be constructed quietly.      
  */
  implicit def EventToAlt(ev: Alt.Event): AltSyntax = Alt.toSyntax(ev)
    
  /** Implicit values */
  implicit val implicit_long:Long     = 0l
  implicit val implicit_int:Int       = 0
  implicit val implicit_ref:AnyRef    = null
  implicit val implicit_float:Float   = 0.0f
  implicit val implicit_double:Double = 0.0
  implicit val implicit_String:String = ""

   /** Concise representation of the open Interval [0..n), which
       <b>unlike</b> <code>o until n</code> has a <code>map</code
       method that is evaluated non-lazily, and therefore behaves 
       appropriately as a generator within <code>for</code> 
       comprehensions.
   */
   def upto (n: Int) : Seq[Int] = 
       new Seq[Int] 
       {
         def iterator = new Iterator[Int] 
         {
             var i=0
             def next    = { val r = apply(i); i=i+1; r }
             def hasNext = i < n
           }
         
         def length       = n
         
         def apply(i: Int) = 
             if (i<n) i else throw new IndexOutOfBoundsException(i + ">=" +n)
             
         /* override */ def map[T: Manifest](f: Int=>T) =
         { val r = new Array[T](length)
           var i = 0
           while (i<n) { r(i) = f(i); i+=1 }
           r    
         }

       }
       
   /** Concise representation of the open Interval [m..n), which
       <b>unlike</b> <code>o until n</code> has a <code>map</code
       method that is evaluated non-lazily, and therefore behaves 
       appropriately as a generator within <code>for</code> comprehensions.
   */
   class range(m: Int, n: Int) extends Seq[Int]
   {
     def iterator = new Iterator[Int] 
     {   var i=m
         def next    = { val r = i; i=i+1; r }
         def hasNext = i < n
     }
     
     def length       = n-m
     
     def apply(i: Int) =
         if (m+i<n) m+i else throw new IndexOutOfBoundsException(i + ">=" + length)
     
     /* override */ def map[T: Manifest ](f: Int=>T) =
     { val r = new Array[T](length)
       var i = m
       while (i<n) { r(i-m) = f(i); i+=1 }
       r    
     }
   }
   
   def range(m: Int, n: Int) : Seq[Int] = new range(m, n)

}





















































