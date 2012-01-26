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
import  scala.collection.mutable.Queue
import  ox.cso.Connection.{Client,Server}

object Farmer
{ 
   def farmer[Req,Rep](svs: Seq[Server[Req,Rep]], in:InPort[Req], out:OutPort[Rep]) : PROC =
       farmer(in, out, svs)
         
   def farmer[Req,Rep](client: Client[Req, Rep], svs: Seq[Server[Req,Rep]]) : PROC = 
       farmer(client, client, svs) 
   
   def farmer[Req,Rep](in:InPort[Req], out:OutPort[Rep], svs: Seq[Server[Req,Rep]]) : PROC =  
   proc
   { var busy = 0                        // number of busy workers
     val free = new Queue[OutPort[Req]]  // queue of free worker connections
     free ++= svs                        // initially all workers are free
     // INVARIANT: busy+free.length=svs.length    
     serve (| (for (worker <- svs) yield 
                   worker (busy>0) ==> 
                   { w => out  ! w
                          free += worker
                          busy = busy-1 
                   }
              )
            | in (free.length>0) ==>
              { work => { val worker = free.dequeue
                          busy = busy+1
                          worker ! work
                        }
              }
           )
    
    ( proc { out.close }
    || || (for (worker <- svs) yield proc { worker.close })
    )()
  }
  
}
















