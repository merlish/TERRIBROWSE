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
/**
 A communication channel whose <code>InPort.?</code> reads
 values sent down the channel by its <code>OutPort.!</code>.
 
 @author  Bernard Sufrin, Oxford
 @version $Revision: 462 $ $Date: 2008-09-12 23:42:07 +0100 (Fri, 12 Sep 2008) $
*/
trait  Chan [T] extends InPort[T] with OutPort[T] { }

object Chan
{ /** 
      A <tt>Chan.Proxy</tt> is a  <tt>Chan</tt> formed from an
      <tt>InPort</tt> and an <tt>OutPort</tt> whose contract is to
      make data output to its <tt>out</tt> available to its
      <tt>in</tt>. 
      <p>
      In the following example, <tt>Buf1</tt> returns a
      channel that behaves like a buffer of
      size 1.
      <pre>
        def Buf1[T]() : Chan[T] = 
        { val in  = OneOne[T]
          val out = OneOne[T]
          proc { repeat { out!(in?) } ({out.close}||{in.close})() }.fork
          new Proxy(in, out)
        }
      </pre>
  */
  class   Proxy[T](out: OutPort[T], in: InPort[T]) 
  extends Chan[T]
  with    InPort.Proxy[T] 
  with    OutPort.Proxy[T] 
  { val   inport = in
    val   outport = out
  }
}






