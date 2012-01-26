/* 

Copyright © Gavin Lowe, 2008, 2010

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

A <tt>Semaphore has operations <code>up</code> and <code>down</code>.  If
<code>down</code> is called while the semaphore is in the down state, the
proces is blocked; a subsequent call to <code>up</code> unblocks the process.
*/

class Semaphore{
  private var isDown = false;

  /** Try to move the semaphore into the down position, blocking if it is
  already down. */
  def down = synchronized{
    while(isDown) wait();
    isDown = true;
  }

  /** Move the semaphore into the up position; if there is a blocked process,
  then unblock one such.  */
  def up = synchronized{
    isDown = false;
    notify();
  }
}

