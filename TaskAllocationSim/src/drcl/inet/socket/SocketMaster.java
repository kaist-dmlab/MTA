// @(#)SocketMaster.java   11/2003
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

import java.io.IOException;
import java.net.SocketException;
import java.util.Hashtable;
import drcl.comp.Component;
import drcl.comp.Port;

/** Application helper class for using {@link InetSocket}.
 * The master component must call {@link #processSocket(Object, Port)}
 * in <code>process(Object, Port)</code>
 * to relay socket contract related messages for processing in this helper
 * class.
 *
 * <p>SocketMaster can work with both {@link TCP_socket} (for single-session
 * applications) and {@link TCP_full} (for multi-session applications).
 * By default, SocketMaster is configured to work with multiple TCP sessions
 * and {@link TCP_full}.  To work with single-session applications (and
 * communicate with {@link TCP_socket}), the application should disable the 
 * multi-session flag with {@link #setMultiSessionEnabled(boolean)}.
 * @see SocketContract */
public class SocketMaster 
{
	Hashtable htSockets = new Hashtable();
	Port controlPort;
	int socketCount = 0;
	Component masterComp;
	boolean multiSession = true;

	public SocketMaster(Port controlPort_, Component masterComp_)
	{
		super();
		controlPort = controlPort_;
		masterComp = masterComp_;
	}

	public void reset()
	{
		socketCount = 0;
	}

	public String info()
	{
		return "sockets created so far = " + socketCount + "\n"
			+ "active sockets: " + htSockets + "\n"
			+ "multi-session? " + multiSession + "\n";
	}

	/** Creates and installs a new server/client {@link InetSocket} in this
	 * application. */
	public InetSocket newSocket()
	{
		Port p_ = multiSession? masterComp.addPort(".tcp" + (socketCount++)):
				controlPort;
		InetSocket s_ = new InetSocket(p_, controlPort);
		htSockets.put(p_, s_);
		return s_;
	}

	public void listen(InetSocket serverSocket_, int backlog_)
	{
		serverSocket_.listen(backlog_);
	}

	/** Accepting a new connection at the server socket.  
	 * It returns the client socket that handles the new connection. */
	public InetSocket accept(InetSocket serverSocket_)
		throws java.io.IOException
	{
		Port p_ = multiSession? masterComp.addPort(".tcp" + (socketCount++)):
				controlPort;
		InetSocket s_ = serverSocket_.accept(p_);
		if (s_ != null) htSockets.put(p_, s_);
		return s_;
	}

	/** Accepting a new connection at the server socket.  
	 * This call is nonblocking.  
	 * The handler (<code>h_</code>) is called back when a new connection
	 * is established from the server socket.
	 */
	public void aAccept(InetSocket serverSocket_, 
					NonblockingSocketHandler h_) throws java.io.IOException
	{
		if (h_ == null)
			throw new java.net.SocketException("No handler to handle the event" 
							+ " when a new connection is established");
		htSockets.put(serverSocket_, h_);
		serverSocket_.aAccept(serverSocket_);
	}


	// Finishes a nonblocking accept()/connect.
	private void nonblockingFinished(SocketContract.Message msg_)
	{
		try {
			if (msg_.isAcceptReply()) {
				InetSocket serverSocket_ =
						(InetSocket)msg_.getMessageID();
				Port p_ = multiSession?
						masterComp.addPort(".tcp" + (socketCount++)):
						controlPort;
				NonblockingSocketHandler h_ = (NonblockingSocketHandler)
						htSockets.remove(serverSocket_);
				try {
					InetSocket s_ = serverSocket_.aAcceptFinished(msg_, p_);
					if (s_ != null) htSockets.put(p_, s_);
					h_.acceptFinished(serverSocket_, s_);
				}
				catch (IOException ioe_) {
					h_.error(serverSocket_, ioe_);
				}
			}
			else if (msg_.isConnectReply()) {
				InetSocket socket_ = (InetSocket)msg_.getMessageID();
				NonblockingSocketHandler h_ = (NonblockingSocketHandler)
						htSockets.remove(socket_);
				try {
					socket_.aConnectFinished(msg_);
					h_.connectFinished(socket_);
				}
				catch (IOException ioe_) {
					h_.error(socket_, ioe_);
				}
			}
			else if (msg_.isCloseReply()) {
				InetSocket socket_ = (InetSocket)msg_.getMessageID();
				htSockets.remove(socket_.dataPort);
				masterComp.removePort(socket_.dataPort);
				socket_.aCloseFinished(msg_);
				NonblockingSocketHandler h_ = (NonblockingSocketHandler)
						htSockets.remove(socket_);
				h_.closeFinished(socket_);
			}
			else if (msg_.isError()) {
				InetSocket socket_ = (InetSocket)msg_.getMessageID();
				NonblockingSocketHandler h_ = (NonblockingSocketHandler)
						htSockets.remove(socket_);
				h_.error(socket_, new SocketException(msg_.getError()));
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/** Connecting to a remote node at the client socket. */
	public void connect(InetSocket clientSocket_, long remoteAddress_,
					int remotePort_) throws java.io.IOException
	{
		clientSocket_.connect(remoteAddress_, remotePort_);
	}

	/** Connecting to a remote node at the client socket.
	 * This call is nonblocking.  
	 * The handler (<code>h_</code>) is called back when the connection
	 * is established.
	 */
	public void aConnect(InetSocket clientSocket_, long remoteAddress_,
					int remotePort_, NonblockingSocketHandler h_)
		throws java.io.IOException
	{
		if (h_ == null)
			throw new java.net.SocketException("No handler to handle the event" 
							+ " when a new connection is established");
		htSockets.put(clientSocket_, h_);
		clientSocket_.aConnect(remoteAddress_, remotePort_, clientSocket_);
	}

	/** Closes the server/client socket. */
	public void close(InetSocket socket_) throws java.io.IOException
	{
		socket_.close();
		htSockets.remove(socket_.dataPort);
		masterComp.removePort(socket_.dataPort);
	}

	/** Closes the server/client socket.
	 * This call is nonblocking.  
	 * The handler (<code>h_</code>) is called back when the connection
	 * is closed.
	 */
	public void aClose(InetSocket socket_, NonblockingSocketHandler h_)
			throws java.io.IOException
	{
		if (h_ == null)
			throw new java.net.SocketException("No handler to handle the event" 
							+ " when the connection is closed");
		htSockets.put(socket_, h_);
		socket_.aClose(socket_);
	}

	/** Binds the server/client socket to a specific (address, port). */
	public void bind(InetSocket socket_, long localAddress_, int localPort_)
	{
		socket_.bind(localAddress_, localPort_);
	}

	public boolean isMultiSessionEnabled()
	{ return multiSession; }

	public void setMultiSessionEnabled(boolean enabled_)
	{ multiSession = enabled_; }

	/** Processes the socket contract related messages for the master
	 * component.
	 * Returns true if the socket master consumes the data. */
	public boolean processSocket(Object data_, Port inPort_)
	{
		if (data_ instanceof SocketContract.Message) {
			nonblockingFinished((SocketContract.Message)data_);
			return true;
		}
		else if (htSockets.containsKey(inPort_)) {
			InetSocket s_ = (InetSocket)htSockets.get(inPort_);
			if (s_ == null) {
				masterComp.error("SocketMaster.processSocket()",
								"no socket for " + inPort_);
				return false;
			}
			else {
				s_.helper.handle((drcl.comp.lib.bytestream.ByteStreamContract.
									Message)data_);
				return true;
			}
		}
		return false;
	}
}
