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
  
  An (Occam-style) synchronised Chan implementation that ensures that
  <tt>!</tt> synchronises with <tt>?</tt>, and that performs a
  sound (but incomplete) dynamic check for inappropriate channel-end 
  sharing.
  <p>
  If a writer overtakes a waiting writer or a reader overtakes a waiting
  reader, then an IllegalStateException is thrown, for in this case at
  least two processes must be sharing an end of the channel. This is
  something that the Scala type system can't protect from. Even worse:
  given a fast enough reader, multiple writers can go undetected (the
  dual statement is also true).
  <p>
  To <i>share</i> neither end of a synchronized channel use
  a <tt>OneOne</tt>.
  <p>
  To <i>share</i> the writer end of a synchronized channel use
  a <tt>ManyOne</tt>.
  <p>
  To <i>share</i> the reader end of a synchronized channel use
  a <tt>OneMany</tt>.
  <p>
  To <i>share</i> the both ends of a synchronized channel use
  a <tt>ManyMany</tt>, but
  if (in this case) you're sure that you don't need
  synchronization, merely <i>serialization</i> 
  then use a <tt>Buf</tt>. 
  
  @author  Bernard Sufrin, Oxford
  @version $Revision: 482 $ $Date: 2010-05-01 16:14:42 +0100 (Sat, 01 May 2010) $
 
*/
class SyncChan [T] (id: String) extends Chan[T]
{ protected var obj:T = _
  protected var readerWaiting = false // a reader is waiting for a writer
  protected var writerWaiting = false // a writer is waiting for a reader
  protected var readerDone = false    // Has the reader finished?
  protected var name     =  SyncChan.genName(id) 
  override  def toString = name
  def stateToString = name + "@<" + hashCode + ">" +
                      (if (writerWaiting) "!"+obj.toString else "") + 
                      (if (readerWaiting) "?" else "") +
                      (if (readerWaiting || writerWaiting) waiter.toString else "")
  
  protected var waiter : Thread = null
  
  def this() = this(SyncChan.newName("SyncChan"))       
  
  override def close = synchronized { 
      _isOpen         = false 
      _isOpenForWrite = false 
      if (writerWaiting) 
         notify
      else
      if (readerWaiting) 
      {  notify                 // Interrupt the read
      }
      
      if (whenReadable!=null)  whenReadable()
      
      if (whenWriteable!=null) whenWriteable()
  }
    
  def !(obj: T) = synchronized {
    if (!_isOpen) throw new Closed(name);
    readerDone = false;
    if (readerWaiting)
    { readerWaiting = false
      this.obj = obj
      notify()                        // notify the waiting reader      
    }
    else
    if (writerWaiting) 
       throw new IllegalStateException(this+" ! "+ obj+" : while writer "+waiter+" waiting from " + 
                                            Thread.currentThread())
    else
    { writerWaiting = true
      this.obj = obj
      if (whenReadable!=null) whenReadable()
    }
    waiter = Thread.currentThread()
    while (!readerDone && _isOpen)         // guard against phantom notify (Nov. 2008)
           wait()                            // await the handshake from the reader
    // check if reader closed while waiting
    if (!_isOpen) throw new Closed(name)
  }

  def ? : T = synchronized {
    if (!_isOpen) throw new Closed(name)
    if (writerWaiting)
       writerWaiting = false
    else
    if (readerWaiting) 
       throw new IllegalStateException(this+" ? : while reader "+waiter+" waiting from " + Thread.currentThread())
    else
    { readerWaiting = true
      if (whenWriteable!=null) whenWriteable()
      waiter = Thread.currentThread()
      while (readerWaiting && _isOpen) wait() // await the writer (or a close)
                                              // guard against phantom notify (Nov. 2008)
      if (!open) throw new Closed(name) 
    }
    readerDone = true
    notify()                                  // handshake (the writer can proceed)
    return obj
  }
  
  def ? [U] (f: T => U) : U  = synchronized {
    if (!_isOpen) throw new Closed(name)
    if (writerWaiting)
       writerWaiting = false
    else
    if (readerWaiting) 
       throw new IllegalStateException(this+" ? : while reader "+waiter+" waiting from " + Thread.currentThread())
    else
    { readerWaiting = true
      if (whenWriteable!=null) whenWriteable()
      waiter = Thread.currentThread()
      while (readerWaiting && _isOpen) wait() // await the writer (or a close)
                                              // guard against phantom notify (Nov. 2008)
      if (!open) throw new Closed(name) 
    }
    readerDone = true
    val result = f(obj)
    notify()                         // handshake (the writer can proceed)
    return result
  }
  
  private var whenReadable: ()  => Unit =  null
  private var whenWriteable: () => Unit =  null
  
  def isReadable( whenReadable: () => Unit ) : Boolean  = synchronized {
      this.whenReadable = whenReadable
      if (this.whenReadable!=null && this.whenWriteable!=null) 
         throw new IllegalStateException(this+"participating in alternations with both input and output guards")
      return writerWaiting
  }

  def isWriteable( whenWriteable: () => Unit ) : Boolean  = synchronized {
      this.whenWriteable = whenWriteable
      if (this.whenReadable!=null && this.whenWriteable!=null) 
         throw new IllegalStateException(this+"participating in alternations with both input and output guards")
      return readerWaiting
  }


}

object SyncChan extends NameGenerator("SyncChan-")


























