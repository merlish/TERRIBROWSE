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
 A many-to-one synchronized channel
        
 @author   Bernard Sufrin, Oxford
 @version $Revision: 472 $ $Date: 2008-09-22 19:07:26 +0100 (Mon, 22 Sep 2008) $
*/
class   ManyOne[T](name: String) 
extends SyncChan[T](name) 
with    UnSharedInPort[T]
with    SharedOutPort[T]
{ val lock = new AnyRef
  override def !(obj: T) = 
           if (isOpen) 
              lock synchronized super.!(obj) 
           else 
              throw new Closed(name)
  def this() = this(SyncChan.newName("ManyOne")) 
}












