// @(#)SocketContract.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.inet.socket;

import drcl.comp.Port;
import drcl.comp.Contract;

/**
This class defines the Socket contract.
<dl>
<dt><code>Accept</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 0 (the "accept" command),
	<li> local address (<code>long</code>),
	<li> local port (<code>long</code>).
	<li> message ID(<code>Object</code>).
	</ol>
	In response, the reactor sends back the following message after a 
	connection is established
	<ol>
	<li> an integer of value 1 (the "accept-reply" message),
	<li> connection ID (<code>int</code>),
	<li> remote address (<code>long</code>),
	<li> remote port (<code>long</code>),
	<li> TCP data port ({@link Port}).
	<li> message ID(<code>Object</code>).
	</ol>
	The message ID in the reply is the same object as in the reqest.
	After connecting the data path (with the TCP data port), the initiator 
	sends back an <code>established</code> message through the TCP data port
	to notify of the establishment of the data path.
<dt><code>Connect</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 2 (the "connect" command),
	<li> remote address (<code>long</code>),
	<li> remote port (<code>long</code>),
	<li> local address (<code>long</code>),
	<li> local port (<code>long</code>).
	<li> message ID(<code>Object</code>).
	</ol>
	In response, the reactor sends back the following message after a 
	connection is established
	<ol>
	<li> an integer of value 3 (the "connect-reply" message),
	<li> connection ID (<code>int</code>),
	<li> TCP data port ({@link Port}).
	<li> message ID(<code>Object</code>).
	</ol>
	The message ID in the reply is the same object as in the reqest.
	After connecting the data path (with the TCP data port), the initiator 
	sends back an <code>established</code> message to the TCP data port
	to notify of the establishment of the data path.
<dt> <code>Close</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 4 (the "close" command) and
	<li> connection ID (<code>int</code>).
	<li> message ID(<code>Object</code>).
	</ol>
	In response, the reactor sends back the following message after the 
	connection is closed:
	<ol>
	<li> an integer of value 5 (the "close-reply" command) and
	<li> connection ID (<code>int</code>).
	<li> message ID(<code>Object</code>).
	</ol>
	The message ID in the reply is the same object as in the reqest.
<dt> <code>Established</code>
<dd> The initiator uses this message to notify of the establishment of
    the data path (see "Accept" and "Connect").  This message is needed
	because the data path is established on demand.
	The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 6 (the "established" message) and
	<li> connection ID (<code>int</code>).
	</ol>
	In response, the reactor sends back a <code>null</code> to complete
	the signaling.
<dt><code>Error</code>
<dd> In any of the above processes, if an error occurs, the reactor may send 
    back an error message instead of a normal reply.  The error message 
	consists of:
	<ol>
	<li> an integer of value 6 (the "error" message) and
	<li> description (<code>String</code>).
	</ol>
</dl>
*/
public class SocketContract extends Contract implements SocketConstants
{
	public SocketContract()
	{ super(); }
	
	public SocketContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Stream Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	/** Send out "listen" command. */
	public static void listen(long localAddr_, int localPort_, int bufferSize_,
					Port port_)
	{ port_.doSending(new Message(localAddr_, localPort_, bufferSize_)); }
	
	/** Send out "accept" command using "send-receive". */
	public static Message accept(long localAddr_, int localPort_, Port port_)
	{ return (Message)port_.sendReceive(new Message(localAddr_, localPort_,
							null)); }
	
	/** Send out "accept" command using "send". */
	public static void accept(long localAddr_, int localPort_, Object msgID_,
					Port port_)
	{ port_.doSending(new Message(localAddr_, localPort_, msgID_)); }
	
	/** Send out "connect" command using "send-receive". */
	public static Message connect(long remoteAddr_, int remotePort_,
					long localAddr_, int localPort_, Port port_)
	{ return (Message)port_.sendReceive(new Message(remoteAddr_, remotePort_,
							localAddr_, localPort_, null)); }
	
	/** Send out "connect" command using "send". */
	public static void connect(long remoteAddr_, int remotePort_,
					long localAddr_, int localPort_, Object msgID_, Port port_)
	{ port_.doSending(new Message(remoteAddr_, remotePort_,
							localAddr_, localPort_, msgID_)); }
	
	/** Send out "close" command using "send-receive". */
	public static Message close(int connectionID_, Port port_)
	{ return (Message) port_.sendReceive(new Message(connectionID_, false,
							null)); }

	/** Send out "close" command using "send-receive". */
	public static Message close(long localAddr_, int localPort_, Port port_)
	{ return (Message)port_.sendReceive(new Message(-1, localAddr_,
							localPort_, null)); }
	
	/** Send out "close" command using "send". */
	public static void close(int connectionID_, Object msgID_, Port port_)
	{ port_.doSending(new Message(connectionID_, false, msgID_)); }

	/** Send out "close" command using "send". */
	public static void close(long localAddr_, int localPort_, Object msgID_,
					Port port_)
	{ port_.doSending(new Message(-1, localAddr_,
							localPort_, msgID_)); }
	
	public static void error(String error_, int connectionID_, 
					long remoteAddr_, int remotePort_,
					long localAddr_, int localPort_,
					Object msgID_, Port port_)
	{ port_.doSending(new Message(error_, connectionID_,
		remoteAddr_, remotePort_, localAddr_, localPort_, msgID_)); }

	public static void connectReply(int connectionID_, Port tcpPort_,
					Object msgID_, Port port_)
	{ port_.doSending(new Message(connectionID_, tcpPort_, msgID_)); }
	
	public static void acceptReply(int connectionID_,
					long remoteAddr_, int remotePort_, Port tcpPort_,
					Object msgID_, Port port_)
	{ port_.doSending(new Message(connectionID_, remoteAddr_, remotePort_, 
					tcpPort_, msgID_)); }
	
	public static void closeReply(int connectionID_, Object msgID_, Port port_)
	{ port_.doSending(new Message(connectionID_, true, msgID_)); }

	/** Send out the "established" message. */
	public static void established(int connectionID_, Port port_)
	{ port_.sendReceive(new Message(connectionID_)); }
	
	public static Message getAcceptPack(long localAddr_, int localPort_,
					Object msgID_)
	{ return new Message(localAddr_, localPort_, msgID_); }
	
	public static Message getAcceptReplyPack(int connectionID_,
					long remoteAddr_, int remotePort_, Port tcpPort_,
					Object msgID_)
	{ return new Message(connectionID_, remoteAddr_, remotePort_, tcpPort_,
					msgID_); }
	
	public static Message getConnectPack(long remoteAddr_, int remotePort_,
					long localAddr_, int localPort_, Object msgID_)
	{ return new Message(remoteAddr_, remotePort_, localAddr_, localPort_,
					msgID_); }
	
	public static Message getConnectReplyPack(int connectionID_, Port tcpPort_,
					Object msgID_)
	{ return new Message(connectionID_, tcpPort_, msgID_); }
	
	public static Message getClosePack(int connectionID_)
	{ return new Message(connectionID_, false, null); }

	public static Message getCloseReplyPack(int connectionID_, Object msgID_)
	{ return new Message(connectionID_, true, msgID_); }

	public static class Message extends drcl.comp.Message
	{
		int type;
		int connection_id;
		int localPort, remotePort;
		long localAddr, remoteAddr;
		Port tcpPort;
		String error;
		transient Object msgID;
		
		public Message ()
		{}

		/** Creates an ESTABLISHED msg. */
		public Message (int connectionID_)
		{
			type = ESTABLISHED;
			connection_id = connectionID_;
		}

		/** Creates an LISTEN request.
		 * @param size_ buffer to be allocated for this number of requests
		 */
		public Message (long localAddr_, int localPort_, int size_)
		{
			type = LISTEN;
			localPort = localPort_;
			localAddr = localAddr_;
			connection_id = size_;
		}


		/** Creates a CLOSE/CLOSE_REPLY request. */
		public Message (int connectionID_, boolean reply_, Object msgID_)
		{
			type = reply_? CLOSE_REPLY: CLOSE;
			connection_id = connectionID_;
			msgID = msgID_;
		}

		/** Creates a CLOSE request. */
		public Message (int connectionID_, long localAddr_, int localPort_,
						Object msgID_)
		{
			type = CLOSE;
			connection_id = connectionID_;
			localPort = localPort_;
			localAddr = localAddr_;
			msgID = msgID_;
		}

		/** Creates an ACCEPT request. */
		public Message (long localAddr_, int localPort_, Object msgID_)
		{
			type = ACCEPT;
			localPort = localPort_;
			localAddr = localAddr_;
			msgID = msgID_;
		}

		/** Creates an ACCEPT_REPLY. */
		public Message (int connectionID_, long remoteAddr_, int remotePort_,
						Port tcpPort_, Object msgID_)
		{
			type = ACCEPT_REPLY;
			connection_id = connectionID_;
			remoteAddr = remoteAddr_;
			remotePort = remotePort_;
			tcpPort = tcpPort_;
			msgID = msgID_;
		}

		/** Creates a CONNECT request. */
		public Message (long remoteAddr_, int remotePort_, long localAddr_,
						int localPort_, Object msgID_)
		{
			type = CONNECT;
			remoteAddr = remoteAddr_;
			remotePort = remotePort_;
			localAddr = localAddr_;
			localPort = localPort_;
			msgID = msgID_;
		}

		/** Creates an CONNECT_REPLY. */
		public Message (int connectionID_, Port tcpPort_, Object msgID_)
		{
			type = CONNECT_REPLY;
			connection_id = connectionID_;
			tcpPort = tcpPort_;
			msgID = msgID_;
		}

		/** Creates an ERROR message. */
		public Message (String error_, int connectionID_, 
						long remoteAddr_, int remotePort_, long localAddr_,
						int localPort_, Object msgID_)
		{
			type = ERROR;
			error = error_;
			connection_id = connectionID_;
			remoteAddr = remoteAddr_;
			remotePort = remotePort_;
			localAddr = localAddr_;
			localPort = localPort_;
			msgID = msgID_;
		}

		private Message (int type_, long localAddr_, int localPort_,
						long remoteAddr_, int remotePort_, int connection_id_,
						Port tcpPort_, Object msgID_)
		{
			type = type_;
			localAddr = localAddr_;
			localPort = localPort_;
			remoteAddr = remoteAddr_;
			remotePort = remotePort_;
			connection_id = connection_id_;
			tcpPort = tcpPort_;
			msgID = msgID_;
		}

		
		public int getConnectionID()
		{ return connection_id; }
	
		/** With LISTEN message. */
		public int getBufferSize()
		{ return connection_id; }
	
		public long getLocalAddr()
		{ return localAddr; }
	
		public int getLocalPort()
		{ return localPort; }

		public void setLocalPort(int localPort_)
		{ localPort = localPort_; }
	
		public long getRemoteAddr()
		{ return remoteAddr; }
	
		public int getRemotePort()
		{ return remotePort; }
	
		public Port getPort()
		{ return tcpPort; }
	
		public String getError()
		{ return error; }
	
		public boolean isAccept()
		{ return type == ACCEPT; }
	
		public boolean isAcceptReply()
		{ return type == ACCEPT_REPLY; }
	
		public boolean isListen()
		{ return type == LISTEN; }
	
		public boolean isConnect()
		{ return type == CONNECT; }
	
		public boolean isConnectReply()
		{ return type == CONNECT_REPLY; }
	
		public boolean isClose()
		{ return type == CLOSE; }

		public boolean isCloseReply()
		{ return type == CLOSE_REPLY; }

		public boolean isEstablished()
		{ return type == ESTABLISHED; }

		public boolean isError()
		{ return type == ERROR; }

		public int getType()
		{ return type; }

		public Object getMessageID()
		{ return msgID; }
	
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			connection_id = that_.connection_id;
			localAddr = that_.localAddr;
			localPort = that_.localPort;
			remoteAddr = that_.remoteAddr;
			remotePort = that_.remotePort;
			tcpPort = that_.tcpPort;
			msgID = that_.msgID;
		}
		*/
	
		public Object clone()
		{
			return new Message(type, localAddr, localPort, remoteAddr,
							remotePort, connection_id, tcpPort, msgID);
		}
	
		public Contract getContract()
		{ return new SocketContract(); }

		public String toString(String separator_)
		{
			switch (type) {
			case CLOSE:
				return "SOCKET_CLOSE:" + connection_id
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			case CLOSE_REPLY:
				return "SOCKET_CLOSE_REPLY:" + connection_id;
			case ACCEPT:
				return "SOCKET_ACCEPT" + separator_ + localAddr + ":"
					+ localPort
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			case LISTEN:
				return "SOCKET_LISTEN" + separator_ + localAddr + ":"
					+ localPort + separator_ + getBufferSize();
			case CONNECT:
				return "SOCKET_CONNECT" + separator_ + localAddr + ":"
					+ localPort + "-->" + remoteAddr + ":" + remotePort
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			case ACCEPT_REPLY:
				return "SOCKET_ACCEPT_REPLY:" + connection_id + separator_
					+ remoteAddr + ":" + remotePort + separator_ + tcpPort
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			case CONNECT_REPLY:
				return "SOCKET_CONNECT_REPLY:" + connection_id + separator_
					+ tcpPort
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			case ESTABLISHED:
				return "SOCKET_ESTABLISHED:" + connection_id;
			case ERROR:
				return "SOCKET_ERROR:" + error + separator_
					+ connection_id + separator_
					+ localAddr + ":" + localPort + "<-->"
					+ remoteAddr + ":" + remotePort
					+ (msgID == null? "": separator_ + "MsgID:" + msgID);
			default:
				return "SOCKET_???:" + type;
			}
		}
	}
}




