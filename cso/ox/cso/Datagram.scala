package ox.cso

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
        An analogue of NetIO that uses Datagrams as transport. 
        Connections are still bidirectional, but they are not
        considered to be streamed. Messages between the
        same pair of sockets may overtake each other. Messages may
        also be dropped.
        <p>
        The Server object returned from clientConnection is used
        to send a Client's ''requests'' to a server; the same Server
        object is used to read the server's ''replies.''
        <p>
        The Client objects returned from serverConnection provide
        clients' requests together with the addresses that they
        came from; they accept replies that are annotated with
        the addresses to which they should be sent.
        <p>
        The following is an extract from a very simple client that stops
        when the server it is connected to sends it a zero. 
<pre><code>
   val server = Datagram.clientConnection[int,int](length, host, port)
   val n      = 10
   repeat (n!=0)
   { server ! n
     server ? { case m => n=m }
   }
</code></pre>
        <p>
        The following is an extract from a very simple server, that subtracts 1 from 
        each number
        it is sent, and returns the result to the client that sent it.
<pre><code>
   val clients = Datagram.serverConnection[int,int](length, port)
   repeat  
   { client ? match
             { case (n, theClient)  => client!(n-1, theClient)  }
   }
   client.close
</code></pre>
        
*/

object Datagram
{ import java.net.{SocketException,SocketTimeoutException,DatagramPacket,DatagramSocket,MulticastSocket,Socket,SocketAddress,InetAddress,InetSocketAddress}
  import java.io.IOException
  import ox.cso.ObjectIO.{ObjectOutput,ObjectInput,Serial}
  import ox.CSO._
  import ox.CSO
  import ox.cso.Connection._
  
  type Packet  = DatagramPacket
  type Address = SocketAddress
  
  def toPacket[T <: Serial](obj: T, to: SocketAddress) : Packet =
  { val byteStream = new java.io.ByteArrayOutputStream()
    val out = new ObjectOutput(byteStream)
    out.writeObject(obj)
    out.flush
    val bytes  = byteStream.toByteArray
    val length = bytes.length
    new DatagramPacket(bytes, 0, length, to)
  }
  
  def toPacket[T <: Serial](obj: T) : Packet =
  { val byteStream = new java.io.ByteArrayOutputStream()
    val out = new ObjectOutput(byteStream)
    out.writeObject(obj)
    out.flush
    val bytes  = byteStream.toByteArray
    val length = bytes.length
    new DatagramPacket(bytes, 0, length, null)
  }
  
  def fromPacket[T <: Serial](packet: Packet) : T =
  { val byteStream = new java.io.ByteArrayInputStream(packet.getData, packet.getOffset, packet.getLength)
    val in = new ObjectInput(byteStream)
    in.readObject.asInstanceOf[T]   
  }
  
  def PortToSocket[T <: Serial](in: ?[T], sender: DatagramSocket, to: SocketAddress) : PROC = 
  proc
  { var exn = null : IOException
    try
    { repeat { in ? { case obj => { sender.send(toPacket(obj, to)) } } } }
    catch 
    {
      case e: IOException => { exn = e }
    }    
    (sender.close || in.close)()
    
    if (exn!=null) throw exn
  }
  
  
  def SocketToPort[T <: Serial](length: Int, receiver: DatagramSocket, out: ![T]) : PROC = 
  proc
  { val buffer = new DatagramPacket(new Array[Byte](length), length)
    var exn = null : IOException
    try 
    { 
      repeat 
      {  receiver.receive(buffer) 
         out ! (fromPacket(buffer))
      }
    }
    catch 
    { 
      case e: IOException => { exn = e }
    }
    
    (receiver.close || out.close)()
    
    if (exn!=null) throw exn
  }
  
  def PacketsToSocket[T <: Serial](in: ?[(T, SocketAddress)], sender: DatagramSocket) : PROC = 
  proc
  { var exn = null : IOException
    try
    { 
      repeat { in ? { case (obj, to) => { sender.send(toPacket(obj, to)) } } }
    }
    catch 
    {
      case e: IOException => { exn = e }
    }
    (sender.close || in.close)()

    if (exn!=null) throw exn
  }
  
  def SocketToPackets[T <: Serial](length: Int, receiver: DatagramSocket, out: ![(T, SocketAddress)]) : PROC = 
  proc
  { val buffer = new DatagramPacket(new Array[Byte](length), length)
    var exn = null : IOException
    try
    {
      repeat 
      {  receiver.receive(buffer)
         out ! (fromPacket(buffer), buffer.getSocketAddress)
      }
    }
    catch 
    { 
      case e: IOException => { exn = e }
    }
    
    (receiver.close || out.close)()
    
    if (exn!=null) throw exn
  }
    
  def clientConnection[Req <: Serial, Rep <: Serial](length: Int, host: String, port: Int, timeout: Int) : Server[Req,Rep] = 
      clientConnection[Req,Rep](length, InetAddress.getByName(host), port, timeout)
  
  /** 
      A client connection accepts requests and provides replies. The length
      parameter specifies the size of the incoming datagram buffer; this must be
      larger than any data that is expected to be received. 
  */
  def clientConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, host: InetAddress, port: Int, timeout: Int) 
      : Server[Req, Rep] =  
  { 
    val socket = new DatagramSocket()         
    val socketAddr = new InetSocketAddress(host, port)
    val req = CSO.OneOne[Req]
    val rep = CSO.OneOne[Rep]
    if (timeout!=0) socket.setSoTimeout(timeout)
    ( PortToSocket(req, socket, socketAddr).withName("Datagram.clientConnection P2S")
    ||
      SocketToPort(length, socket, rep).withName("Datagram.clientConnection S2P")
    ).fork
    new Server(socket, req, rep)
  } 
      
  def multicastClientConnection[Req <: Serial, Rep <: Serial](length: Int, host: String, port: Int, timeout: Int) : Server[Req,Rep] = 
      multicastClientConnection[Req,Rep](length, InetAddress.getByName(host), port, timeout)
  
  /** 
      A multicast client connection accepts requests and provides replies. The length
      parameter specifies the size of the incoming datagram buffer; this must be
      larger than any data that is expected to be received. 
  */
  def multicastClientConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, host: InetAddress, port: Int, timeout: Int) 
      : MulticastServer[Req, Rep] =  
  { 
    val socket = new MulticastSocket()         
    val socketAddr = new InetSocketAddress(host, port)
    if (timeout!=0) socket.setSoTimeout(timeout)
    val req = CSO.OneOne[Req]
    val rep = CSO.OneOne[Rep]
    ( PortToSocket(req, socket, socketAddr).withName("Datagram.clientConnection P2S")
    ||
      SocketToPort(length, socket, rep).withName("Datagram.clientConnection S2P")
    ).fork
    new MulticastServer(socket, req, rep)
  } 
      
  /** 
      A server connection provides address-annotated requests, for
      which it listens at the given port and accepts address-annotated
      replies which it sends to the addresses with which they are
      annotated.  The length parameter specifies the size of the
      incoming datagram buffer; this must be larger than any data
      that is expected to be received.
  */
  def serverConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =  
  { 
    val socket = new DatagramSocket(port)         
    val req = CSO.OneOne[(Req, Address)]
    val rep = CSO.OneOne[(Rep, Address)]
    if (timeout!=0) socket.setSoTimeout(timeout)
    ( PacketsToSocket(rep, socket).withName("Datagram.serverConnection P2S")
    ||
      SocketToPackets(length, socket, req).withName("Datagram.serverConnection S2P")
    ).fork
    new Client(socket, req, rep)
  } 
  
  def multicastServerConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, group: String, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =
      multicastServerConnection(length, InetAddress.getByName(group), port, timeout)
      
  /** 
      A multicast server connection provides address-annotated
      requests, for which it listens at the given port and accepts
      address-annotated replies which it sends to the addresses
      with which they are annotated.  The length parameter specifies
      the size of the incoming datagram buffer; this must be larger
      than any data that is expected to be received.
  */
  def multicastServerConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, group: InetAddress, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =  
  { 
    val socket = new MulticastSocket(port)
    socket.joinGroup(group)
    if (timeout!=0) socket.setSoTimeout(timeout)

    val req = CSO.OneOne[(Req, Address)]
    val rep = CSO.OneOne[(Rep, Address)]
    (  PacketsToSocket(rep, socket).withName("Datagram.multicastServerConnection P2S")
    ||
       SocketToPackets(length, socket, req).withName("Datagram.multicastServerConnection S2P")
    ).fork
    new Client(socket, req, rep)
  } 
  
  /** A bidirectional multicast connection to a server */
  class MulticastServer[Req, Rep]
        (theSocket: MulticastSocket, 
         req:       ![Req], 
         rep:       ?[Rep]
        ) 
  extends 
        Server[Req, Rep](theSocket, req, rep)
        { def setScope(n: Int)                  = theSocket.setTimeToLive(n)
          def setLoopbackMode(disable: Boolean) = theSocket.setLoopbackMode(disable)
          override def toString =
                   "Datagram.MulticastClient("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }
  
  /** A Client connection that uses Datagrams as transport */
  class Client[Req, Rep](theSocket: DatagramSocket, req: InPort[Req], rep: OutPort[Rep]) 
  extends 
        Connection.ProxyClient[Req, Rep](req, rep)
        { def socket = theSocket 
          override def toString =
                   "Datagram.Client("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }
  
  /** A Server connection that uses Datagrams as transport */     
  class Server[Req, Rep](theSocket: DatagramSocket, req: OutPort[Req], rep: InPort[Rep]) 
  extends 
        Connection.ProxyServer[Req, Rep](req, rep)
        { def socket = theSocket 
          override def toString =
                   "Datagram.Server("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }  
}








