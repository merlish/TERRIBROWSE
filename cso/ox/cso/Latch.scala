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


package ox.cso;
/**
  A <code>Latch</code> is constructed in its waiting state. A call of
  <code>await</code> suspends its calling process until another process
  calls <code>signal</code>, at which point all awaiting callers become
  runnable.
 
 @author  Bernard Sufrin, Oxford
 @version $Revision: 401 $ $Date: 2008-07-28 12:09:13 +0100 (Mon, 28 Jul 2008) $
  
*/
class Latch
{ protected var waiting = true
  
  def await  : Unit = 
      synchronized { while (waiting) wait() }
  
  def signal : Unit = 
      synchronized { if (waiting) { waiting = false; notifyAll(); } }
}



