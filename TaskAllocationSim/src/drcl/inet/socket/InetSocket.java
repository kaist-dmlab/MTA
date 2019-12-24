// @(#)InetSocket.java   1/2004
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

import java.util.*;
import java.net.*;
import java.io.*;
import drcl.data.IntObj;
import drcl.comp.*;
import drcl.comp.lib.bytestream.ByteStreamContract;
import drcl.comp.lib.bytestream.ByteStreamPeer;
import drcl.inet.Node;

/** 
 * This class implements a "socket" equivalent API for application components
 * in the INET framework.  It has nothing to do with {@link JSimSocketImpl}.
*/
public class InetSocket implements SocketStates
{
	public Port dataPort, controlPort; // to TCP
	int connectionID = 0;
	int state = INIT;
	long localAddress, remoteAddress;
	int localport, remoteport;
	ByteStreamPeer helper;

	public InetSocket(Port dataPort_, Port controlPort_)
	{
		this(drcl.net.Address.NULL_ADDR, dataPort_, controlPort_);
	}

	public InetSocket(long localAddress_, Port dataPort_, Port controlPort_)
	{
		super();
		localAddress = localAddress_;
		dataPort = dataPort_;
		controlPort = controlPort_;
		helper = new ByteStreamPeer(dataPort);
	}

	public int getRemotePort()
	{ return remoteport; }
	
	public int getLocalPort()
	{ return localport; }
	
	public long getRemoteAddress()
	{ return remoteAddress; }
	
	public long getLocalAddress()
	{ return localAddress; }

	public void bind(long localAddr_, int localPort_)
	{
		localAddress = localAddr_;
		localport = localPort_;
	}
	
	/* Sets the maximum queue length for incoming connection indications (a
	 * request to connect) to the count argument. If a connection
	 * indication arrives when the queue is full, the connection is
	 * refused.  */
	public void listen(int bufferSize_)
	{
		SocketContract.listen(localAddress, getLocalPort(),
						bufferSize_, controlPort);
	}

	public InetSocket accept(Port dataPort_) throws IOException
	{
		if (state == ACCEPTING)
			throw new SocketException("Socket is already accepting");
		state = ACCEPTING;
		SocketContract.Message reply_ = SocketContract.accept(
			localAddress, getLocalPort(), controlPort);
		return aAcceptFinished(reply_, dataPort_);
	}

	/** Asynchronized version of <code>accept()</code>.
	 * This call is nonblocking.  The caller should call 
	 * {@link #aAcceptFinished(SocketContract.Message, Port)} to finish
	 * the accepting. */
	public void aAccept(Object msgID_)
			throws IOException
	{
		if (state == ACCEPTING)
			throw new SocketException("Socket is already accepting");
		state = ACCEPTING;
		SocketContract.accept(localAddress, getLocalPort(), msgID_,
						controlPort);
	}
	
	/** finishes an asynchronous "accept" when receiving an accept-reply. */
	public InetSocket aAcceptFinished(SocketContract.Message reply_, 
					Port dataPort_) throws IOException
	{
		if (state == CLOSING || state == CLOSED) {
			// reply_.getConnectionID() should also be negative
			drcl.Debug.debug(" accepting is interrupted \n");
			return null;
		}
		if (reply_.isError())
			throw new SocketException(reply_.getError());
		if (reply_.getConnectionID() < 0)
			throw new SocketException("'accept' failed");
		InetSocket s_ = new InetSocket(localAddress, dataPort_, 
						controlPort);
		s_.dataPort.connect(reply_.getPort());
		s_.connectionID = reply_.getConnectionID();
		SocketContract.established(s_.connectionID, s_.dataPort);
			// notify peer (eg, TCP) that the data path is now established
		s_.dataPort.setID("tcp" + s_.connectionID);
		s_.remoteAddress = reply_.getRemoteAddr();
		s_.remoteport = reply_.getRemotePort();
		s_.state = CONNECTED;
		state = NO_OP;
		return s_;
	}

	public int available()
	{
		try {
			return  ByteStreamContract.query(dataPort);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void close() throws IOException
	{
		try {
			SocketContract.Message reply_ = null;
			if (state == CONNECTED || state == CONNECTING) {
				state = CLOSING;

				// cancelling blocked receiving
				// no harm if not currently receiving
				helper.interruptReceiving();

				reply_ = SocketContract.close(connectionID, controlPort);
			}
			else if (state == ACCEPTING) {
				reply_ = SocketContract.close(localAddress, getLocalPort(),
									controlPort);
			}
			if (reply_ != null && reply_.isError()) // error message
				throw new SocketException(reply_.getError());
			aCloseFinished(reply_);

			// XXX: clean up?
		}
		catch (SocketException se) {
			throw se;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Asynchronized version of <code>close()</code>.
	 * This call is nonblocking.  The caller should call 
	 * {@link #aCloseFinished(SocketContract.Message)} to finish
	 * the close. */
	public void aClose(Object msgID_)
	{
		if (state == CONNECTED || state == CONNECTING) {
			state = CLOSING;

			// cancelling blocked receiving
			// no harm if not currently receiving
			helper.interruptReceiving();

			SocketContract.close(connectionID, msgID_, controlPort);
		}
		else if (state == ACCEPTING) {
			SocketContract.close(localAddress, getLocalPort(), msgID_,
								controlPort);
		}
	}

	/** finishes an asynchronous "accept" when receiving an accept-reply. */
	public void aCloseFinished(SocketContract.Message reply_) 
	{
		dataPort.disconnect();
		dataPort.host.removePort(dataPort);
		drcl.Debug.debug(" connection is closed \n");
		state = CLOSED;

		// XXX: clean up?
	}

	public void connect(long remoteAddress_, int port_)
		throws IOException
	{
		if (state == CONNECTING)
			throw new SocketException("Socket is connecting to "
							+ remoteAddress + ":" + remoteport);
		if (state == CONNECTED)
			throw new SocketException("Socket is already connected to "
							+ remoteAddress + ":" + remoteport);
		state = CONNECTING;
		try {
			remoteAddress = remoteAddress_;
			remoteport = port_;
			SocketContract.Message reply_ = SocketContract.connect(
					remoteAddress, remoteport,
					localAddress, localport, controlPort);
			if (state == CLOSING || state == CLOSED) {
				drcl.Debug.debug(" accepting is interrupted \n");
				// reply_.getConnectionID() should also be negative
				return;
			}
			aConnectFinished(reply_);
		}
		catch (IOException e_) {
			throw e_;
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/** Asynchronized version of <code>connect()</code>.
	 * This call is nonblocking.  The caller should call 
	 * {@link #aConnectFinished(SocketContract.Message)} to finish
	 * the connecting. */
	public void aConnect(long remoteAddress_, int port_, Object msgID_)
		throws IOException
	{
		if (state == CONNECTING)
			throw new SocketException("Socket is already connecting to "
							+ remoteAddress + ":" + remoteport);
		if (state == CONNECTED)
			throw new SocketException("Socket is already connected to "
							+ remoteAddress + ":" + remoteport);
		state = CONNECTING;
		remoteAddress = remoteAddress_;
		remoteport = port_;
		SocketContract.connect(remoteAddress, remoteport,
						localAddress, localport, msgID_, controlPort);
	}

	/** finishes an asynchronous "connect" when receiving an connect-reply. */
	public void aConnectFinished(SocketContract.Message reply_)
		throws IOException
	{
		if (state == CLOSING || state == CLOSED) {
			drcl.Debug.debug(" accepting is interrupted \n");
			// reply_.getConnectionID() should also be negative
			return;
		}
		if (reply_.getConnectionID() < 0)
			throw new ConnectException("connection refused");
		dataPort.disconnect();
		dataPort.connect(reply_.getPort());
		connectionID = reply_.getConnectionID();
		SocketContract.established(connectionID, dataPort);
			// notify peer (eg, TCP) that the data path is now established
		dataPort.setID("tcp" + connectionID);
		state = CONNECTED;
	}

	public InputStream getInputStream()
	{ return helper.getInputStream(); }
	
	public OutputStream getOutputStream()
	{ return helper.getOutputStream(); }
	
	
	public String toString()
	{
		return "connectionID=" + connectionID 
			+ ", localport=" + localport + ", remote=" + remoteAddress
			+ ":" + remoteport
			+ ", state=" + (state < 6? STATES[state]: String.valueOf(state))
			+ ", dataPort=" + dataPort
			+ ", helper:" + (helper != null? helper.info(): "null");
	}
	
	/** Not implemented. */
	public Object getOption(int optID)
	{ return null; }
	
	/** Not implemented. */
	public void setOption(int optID, Object value)
	{ /* ignored */ }

	/** Not implemented. */
	public void sendUrgentData(int data) throws IOException
	{ /* ignored */ }
}
