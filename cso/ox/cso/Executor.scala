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


/**
        Interface to an execution mechanism
*/
package ox.cso

trait Executor 
{  /** Acquire a thread and run the given Runnable in it */
   def execute(runnable: java.lang.Runnable)
   
   /** Take any measures needed to close down the execution mechanism */
   def shutdown = ()
}
