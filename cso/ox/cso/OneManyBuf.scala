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
  
  A finitely buffered Chan implementation, unshared at the writing end.
  The non-sharing is not enforced dynamically.
  
  @author Bernard Sufrin, Oxford
  @version $Revision: 487 $ $Date: 2010-08-12 21:27:10 +0100 (Thu, 12 Aug 2010) $
  
*/

class   OneManyBuf[T: Manifest] (size: Int) 
extends BufImp[T](size)
with    UnSharedInPort[T]
with    SharedOutPort[T]
{
  name = BufImp.newName("OneManyBuf")
  /** The channel is closed */
  override def closeout = { close }
}




