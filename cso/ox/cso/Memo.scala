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
class Memo[T](v: => T) extends Function0[T]
{ var value     = null.asInstanceOf[T]
  var evaluated = false
  def apply()   = 
  { if (!evaluated) { value=v; evaluated=true }
    value
  }
}

object Memo
{ def memo[T](v: => T) = new Memo(v) }
