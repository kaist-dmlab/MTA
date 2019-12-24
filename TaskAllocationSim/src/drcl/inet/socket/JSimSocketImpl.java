// @(#)JSimSocketImpl.java   6/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

/** In the framework of running real applications on JSim, this class 
 * provides a special socket implementation to intercept all the standard
 * Java socket API calls.  Refer to <code>java.net.SocketImpl</code> for
 * details.  This class has nothing to do with {@link InetSocket}.
*/
public class JSimSocketImpl extends SocketImpl implements SocketStates
{
	public Port dataPort, controlPort; // to TCP
	int connectionID = 0;
	int state = INIT;
	InetAddress localAddress;
	int localport;
	Launcher launcher;
	Node node;
	ByteStreamPeer helper;

	public JSimSocketImpl(Launcher l_, InetAddress default_, Port dataPort_, Port controlPort_, Node node_)
	{
		super();
		launcher = l_;
		localAddress = default_;
		//System.out.println("create socket: local = " + localAddress + ", " + this._toString());
		dataPort = dataPort_;
		controlPort = controlPort_;
		node = node_;
		helper = new ByteStreamPeer(dataPort);
	}

	protected FileDescriptor getFileDescriptor()
	{ 
		return null; //??
	}
	
	// remote address
	protected InetAddress getInetAddress()
	{ return address; }
	
	// remote port
	protected int getPort()
	{ return port; }
	
	protected int getLocalPort()
	{ return localport; }
	
	InetAddress getRemoteAddress()
	{ return address; }
	
	InetAddress getLocalAddress() throws UnknownHostException
	{ return localAddress; }
	
	int getRemotePort()
	{ return port; }
	
	protected synchronized void accept(SocketImpl s) throws IOException
	{
		state = ACCEPTING;
		SocketContract.Message reply_ = SocketContract.accept(
			launcher.getJSimAddr(localAddress), getLocalPort(), controlPort);
		if (reply_.isError())
			throw new SocketException(reply_.getError());
		((JSimSocketImpl)s).dataPort.connect(reply_.getPort());
		SocketContract.established(reply_.getConnectionID(), 
			((JSimSocketImpl)s).dataPort);
		// notify TCP that the data path is now established
		((JSimSocketImpl)s).dataPort.setID("tcp" + reply_.getConnectionID());
		((JSimSocketImpl)s).connectionID = reply_.getConnectionID();
		((JSimSocketImpl)s).address = launcher.getInetAddress(reply_.getRemoteAddr());
		((JSimSocketImpl)s).port = reply_.getRemotePort();
		((JSimSocketImpl)s).state = CONNECTED;
		state = NO_OP;
	}
	
	protected synchronized int available()
	{
		try {
			return  ByteStreamContract.query(dataPort);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	protected void bind(InetAddress host_, int port_)
	{
		//System.out.println("-BIND-" + host_ + ", " + port_);
		// XX: ugly...
		// Java is trying to bind "anylocal" address to it
		if (launcher.getJSimAddr(host_) != drcl.net.Address.NULL_ADDR)
			localAddress = host_;
		if (port_ > 0)
			localport = port_;
	}
	
	protected synchronized void close() throws IOException
	{
		try {
			if (state == CONNECTED || state == CONNECTING) {
				state = CLOSING;
				SocketContract.close(connectionID, controlPort);
			}
			dataPort.host.removePort(dataPort);
			drcl.Debug.debug(" connection is closed \n");
			state = CLOSED;

			if (!launcher.isDebugEnabled())
				launcher.removeSocket(this);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected synchronized void connect(InetAddress address_, int port_) throws IOException
	{
		state = CONNECTING;
		try {
			address = address_;
			port = port_;
			//System.out.println("-CONNECT-:" + localAddress + "(" + launcher.getJSimAddr(localAddress) + ") to " + address + ", " + this._toString());
			SocketContract.Message reply_ = SocketContract.connect(
					launcher.getJSimAddr(address), port,
					launcher.getJSimAddr(localAddress), localport, controlPort);
			dataPort.disconnect();
			dataPort.connect(reply_.getPort());
			connectionID = reply_.getConnectionID();
			SocketContract.established(connectionID, dataPort);
				// notify TCP that the data path is now established
			dataPort.setID("tcp" + connectionID);
			state = CONNECTED;
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/**
	 * <code>timeout_</code> is ignored.
	 */
	protected synchronized void connect(SocketAddress address_, int timeout_) throws IOException
	{
	    if (address_ instanceof InetSocketAddress) {
		connect(((InetSocketAddress)address_).getAddress(),
		    ((InetSocketAddress)address_).getPort());
	    }
	    else if (address_ != null)
		throw new IOException(address_.getClass() + " is not supported");
	}
	
	protected synchronized void connect(String host_, int port_) throws java.io.IOException
	{
		InetAddress address_ = InetAddress.getByName(host_);
		if (address_ == null) return;
		connect(address_, port_);
	}
	
	protected void create(boolean stream)
	{
		if (!stream)
			drcl.Debug.error("Only \"stream\" type is supported");
	}
	
	protected InputStream getInputStream()
	{ return helper.getInputStream(); }
	
	protected OutputStream getOutputStream()
	{ return helper.getOutputStream(); }
	
	/* Sets the maximum queue length for incoming connection indications (a
	 * request to connect) to the count argument. If a connection
	 * indication arrives when the queue is full, the connection is
	 * refused.
	 */
	protected void listen(int backlog) throws IOException
	{
	/* ignored */
	}

	String _toString()
	{ return super.toString(); }
	
	public String toString()
	{
		return "localport=" + localport + ", remote=" + address + "(" + launcher.getJSimAddr(address)
			+ "):" + port + ", state=" + (state < 6? STATES[state]: String.valueOf(state))
			+ ", dataPort=" + dataPort
			+ ", helper:" + (helper != null? helper.info(): "null");
	}
	
	// interface SocketOptions
	public Object getOption(int optID)
	{ return null; }
	
	public void setOption(int optID, Object value)
	{ /* ignored */ }

	protected void sendUrgentData(int data) throws IOException
	{
	}
}
