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
import  ox.CSO._
/**
        Components that (mostly) work on finite or infinite streams of values
        presented as channels. All components are designed to
        terminate (ie. to close all the ports that they
        connect) cleanly.
        <p>
        Some of these components were inspired by (or copied from) components
        from the Plug'n'Play collection of JCSP (withoutr necessarily
        retaining the P'n'P names).
        <p>
        @author Bernard Sufrin, Oxford
        @version $Revision: 487 $ $Date: 2010-08-12 21:27:10 +0100 (Thu, 12 Aug 2010) $
*/
object Components
{ /**
  Copy from the given input stream to the given output streams, performing
  the outputs concurrently. Terminate when the input stream or any of the
  output streams is closed.
  <pre><code>
           in   /|----> x, ...
  x, ... >---->{ | : outs
                \|----> x, ...
  </code></pre>
  */
  def tee[T](in: ?[T], outs: Seq[![T]]) = proc
  { var v = null.asInstanceOf[T]    
     val outputs = (|| (for (out<-outs) yield proc {out!v}))
     repeat { v = in?; outputs() }
    (proc { in.closein} || || (for (out<-outs) yield proc {out.closeout}))()
  }
  
  /**
  Merge several input streams Into a single output stream. Terminate when
  the output stream, or <i>all</i> the input streams have closed.
  <pre><code>            
     >---->|\  out
   ins :   | }-----> 
     >---->|/    
  </code></pre>  
  */
  def merge[T](ins: Seq[?[T]], out: ![T]) = proc
  { 
    serve ( for (in <- ins) yield in ==> { x => out!x } )
   
    (proc {out.closeout} || || (for (in <- ins) yield proc {in.closein}))()
  }
  
  /**
  <pre><code>            
  </code></pre>  
  */
  def zipwith[L,R,O](f: (L, R) => O)(lin: ?[L], rin: ?[R], out: ![O]) = 
  proc
  { var l     = null.asInstanceOf[L]
    var r     = null.asInstanceOf[R]
    val input = proc { l = lin? } || proc { r = rin? }
    
    repeat { input(); out!f(l, r) }
    
    (lin.closein || rin.closein || out.closeout)()
  }

  /**
  <pre><code>            
  </code></pre>  
  */   
  def zip[L,R](lin: ?[L], rin: ?[R], out: ![(L,R)]) = 
  proc
  { var l = null.asInstanceOf[L]
    var r = null.asInstanceOf[R]
    val doInputs = proc { l = lin? } || proc { r = rin? }
    
    repeat { doInputs(); out!(l, r) }
    
    (lin.closein || rin.closein || out.closeout)()
  }
  
  /**
  Output all the given <tt>ts</tt> onto the
  output port, then terminate.
  <pre><code> 
    +------------+ t1, ...
    | t1, ... tn +---------> 
    +------------+           
  </code></pre>  
  */   
  def const[T](ts: T*)(out: ![T]) = proc
  { for (t<-ts) out!t; out.closeout }
  
  /**
  A composite component that sums its input stream onto its output stream
  <pre><code>            

                  in                        out
   [x,y,z,...] >------>|\              /|---------> [x,x+y,x+y+z,...]
                       |+}----------->{ |
                   +-->|/              \|--+
                   |                       |
                   +----------<{(0)}<------+
  </code></pre>  
  */   
  def Integrator(in: ?[Long], out: ![Long]) = 
  { val mid, back, addl = OneOne[Long]
    (  zipwith ((x: Long, y: Long)=>x+y) (in, addl, mid)
    || tee (mid, List(out, back))
    || prefix(0l)(back, addl)
    )
  }
  
  /**
  <pre><code>            
  </code></pre>  
  */   
  def inj[T](in: ?[T], inj: ?[T], out: ![T]) = proc
  { 
    priserve ( inj ==> { x => out!x } | in ==> { x => out!x } )
    (out.closeout || in.closein || inj.closein)()
  }
  
  /**
  Repeatedly orders the inputs from its two input ports and 
  outputs them (in parallel) to its two output ports. 
  <pre><code>            
    x, ...--->[\/]---> max(x,y), ...
    y, ...--->[/\]---> min(x,y), ...
  </code></pre> 
  Here is
  a four-channel sorting network composed of 5 such components.
  <pre><code>            
    -->[\/]--------->[\/]------------>
    -->[/\]---+  +-->[/\]--+
              |  |         |
              |  |         +-->[\/]-->
    -->[\/]------+         +-->[/\]-->
    -->[/\]-+ |            |
            | +---->[\/]---+
            +------>[/\]------------->
  </code></pre>  
  */   
  def exchanger[T <% Ordered[T]](l:  ?[T],  r:  ?[T], lo: ![T],  hi: ![T]) = 
  proc
  { var lv, rv = null.asInstanceOf[T]
    val rdBoth = proc  { lv=l? } || proc  { rv=r? }
    val wrBoth = proc  { lo!rv } || proc  { hi!lv }
    repeat 
    { rdBoth()
      if (lv < rv) { val t = lv; lv=rv; rv=t }
      wrBoth()
    }
    (  l.closein   || r.closein
    || lo.closeout || hi.closeout)()
  }  
  
  /**
  Make generate a <tt>?[Unit]</tt> on which an () is made available 
  by a server process every <tt>periodMS</tt> milliseconds.
  Terminate the server when the port is closed.
  <pre><code> 
   +----------+           
   | periodMS |>-------------> () 
   +----------+           
  </code></pre>  
  */   
  def Ticker(periodMS: Long) : ?[Unit] =
  { import ox.CSO._
    val ticks = OneOne[Unit]
    fork { repeat { ticks!(); sleep(periodMS) } } 
    return ticks
  }
  
  /**
  Drop the first value read from <tt>in</tt>, then copy
  values from <tt>in</tt> to <tt>out</tt>.
    
  */   
  def tail[T](in: ?[T], out: ![T]) = proc { in?; copy(in, out)() }
  
  /**
  Output the given <tt>ts</tt> to <tt>out</tt>, then copy
  values from <tt>in</tt> to <tt>out</tt>.
    
  */   
  def prefix[T](ts:T*)(in: ?[T], out: ![T]) = proc { for (t<-ts) out!t; copy(in, out)() }
  
  /**
  Repeatedly copy values from <tt>in</tt> to <tt>out</tt>.  
  */   
  def copy[T] (in: ?[T], out: ![T]) = proc { repeat { out!(in?) }; out.closeout; in.closein }
  
  /**
  Copy values from <tt>in</tt> to <tt>out</tt> that satisfy <tt>pass</tt>. 
    
  */   
  def filter[T] (pass: T => Boolean) (in: ?[T], out: ![T]) = proc
  { repeat { val v=in?; if (pass(v)) out!v }  
    (out.closeout || in.closein)()
  }    
  
  /**
  <pre><code> 
     x, ... >-->[f]>-->f(x), ...            
  </code></pre>  
  */   
  def map[I,O] (f: I => O) (in: ?[I], out: ![O]) = proc 
  { repeat { out!(f(in?)) }
    (out.closeout || in.closein)() 
  }
  
  /**
  Repeatedly write the string forms of values read from <tt>in</tt>
  onto the standard output stream.
  <pre><code>            
  </code></pre>  
  */   
  def console[T](in: ?[T]) = proc { repeat { Console.println(in?) } }
  
  /**
  Repeatedly output lines read from the given <tt>LineNumberReader</tt>.
  <pre><code>            
  </code></pre>  
  */   
  def lines(in: java.io.LineNumberReader, out: ![String]): PROC = proc
  { repeat 
    { val line = try { in.readLine } catch { case _ => null }
      if (line==null) stop
      out!line
    }
    (out.closeout || in.close)() 
  }
  
  /**
  Repeatedly output lines read from the given <tt>Reader</tt>.
  <pre><code>            
  </code></pre>  
  */   
  def lines(in: java.io.Reader, out: ![String]): PROC = lines(new java.io.LineNumberReader(in), out)
  
  /**
  Repeatedly output lines read from the standard input stream.
  <pre><code>            
  </code></pre>  
  */   
  def keyboard(out: ![String]) = lines(new java.io.InputStreamReader(System.in), out)
  
  def sampler[T](periodMS: Long, in: ?[T], out: ![T]) = proc
  { val ticker = Ticker(periodMS) 
    var datum  = null.asInstanceOf[T]
    priserve ( ticker ==> { case () => out!datum } // Scala syntactic eccentricity for ()=>
             | in     ==> { case d  => datum = d } 
             ) 
    (in.closein || out.closeout || ticker.close)()   
  }
}






