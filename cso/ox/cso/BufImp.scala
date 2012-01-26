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
  
  A finitely buffered Chan implementation that can be shared by both
  readers and writers.
  
  @author Bernard Sufrin, Oxford
  @version $Revision: 518 $ $Date: 2012-01-03 22:15:22 +0000 (Tue, 03 Jan 2012) $
  
*/
class BufImp [T: Manifest] (size: Int) extends Chan [T] 
{ private   var buf     = new Array[T](size)
  private   val readers = new CountingSemaphore(0)
  private   val writers = new CountingSemaphore(size)
  private   var front   = 0
  private   var rear    = 0
  protected var name : String = _
  override def toString = name
  
  override def close = synchronized 
  { 
      _isOpen = false 
      if (writers.forceAcquires)
         { /* waiting writers have been notified */ }
      else
      if (readers.forceAcquires) 
         { /* waiting readers have been notified */ }
      
      if (whenReadable!=null) // 
         whenReadable() 
      else
      if (whenWriteable!=null) // 
         whenWriteable() 
  }
  
  def !(value:T) = 
  { if (!_isOpen) throw new Closed(this.toString);
    writers.acquire
    synchronized { buf(rear)=value; rear = (rear+1) % size }
    readers.release
    synchronized { if (whenReadable!=null) whenReadable() }
  }
  
  def ? : T = 
  { if (!_isOpen && readers.count<=0) throw new Closed(this.toString)
    readers.acquire
    synchronized
    { if (!_isOpen && readers.isForced) throw new Closed(this.toString)
      val value = buf(front)
      front = (front+1) % size 
      writers.release
      if (whenWriteable!=null) whenWriteable()
      value
    }
  }
  
  def ?[U] (f: T => U) : U = 
  { if (!_isOpen && readers.count<=0) throw new Closed(this.toString)
    readers.acquire
    synchronized
    { if (!_isOpen && readers.isForced) throw new Closed(this.toString)
      val value = buf(front)
      front = (front+1) % size 
      val result = f(value)
      writers.release
      if (whenWriteable!=null) whenWriteable()
      result
    }
  }

  private var whenReadable:  () => Unit =  null
  private var whenWriteable: () => Unit =  null
  
  def isReadable( whenReadable: () => Unit ) : Boolean  = 
  synchronized 
  {
      if (whenReadable!=null && this.whenReadable!=null) 
         throw new RuntimeException("More than one ALT scanning a Buf")
      this.whenReadable = whenReadable
      return readers.count>0 || readers.isForced
  }  
  
  def isWriteable( whenWriteable: () => Unit ) : Boolean  = 
  synchronized 
  {
      if (whenWriteable!=null && this.whenWriteable!=null) 
         throw new RuntimeException("More than one ALT scanning a Buf")
      this.whenWriteable = whenWriteable
      return writers.count>0 || writers.isForced
  }  
}

object BufImp extends NameGenerator("BufImp")















