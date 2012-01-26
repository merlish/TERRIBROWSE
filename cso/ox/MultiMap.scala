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


package ox
/**
   A <tt>MultiMap[K,D]</tt> represents a mapping <tt>m<tt> 
   from <tt>K</tt> to <tt>List[D]</tt> that initially
   maps all keys to <tt>Nil</tt>
*/

import  scala.collection.mutable._

class MultiMap[K,D] extends Function1[K,List[D]]
{ val rep = new HashMap[K,List[D]]
  /**
      <tt> m := m + { k -> d :: m (k) } </tt>
  */
  def put(k:K, d:D) : Unit =
  { if (rep.contains(k)) rep.update(k, d :: rep.apply(k)) else rep.update(k, d :: Nil) } 
  
  /** 
      Returns <tt> m(k) </tt>
  */
  def apply(k: K) = 
  { if (rep.contains(k)) rep.apply(k) else Nil
  }
  
  /**
      Print (on the given <tt>PrintWriter</tt>)
      the maplets that do not map to <tt>Nil</tt>      
  */
  def printOut(stream: java.io.PrintWriter) = stream.println(rep)
}


