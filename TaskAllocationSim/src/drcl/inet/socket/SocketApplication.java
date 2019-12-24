// @(#)SocketApplication.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

/** Application base class for using {@link InetSocket}.
 * Subclasses must call super.process(Object, Port) to delegate incoming
 * data to opened sockets. */
public class SocketApplication extends drcl.comp.Component
	implements NonblockingSocketHandler
{
	protected SocketMaster socketMaster =
			new SocketMaster(addPort("down"), this);

	public SocketApplication()
	{ super(); }

	public SocketApplication(String id_)
	{ super(id_); }

	public void reset()
	{
		super.reset();
		socketMaster.reset();
	}

	public String info()
	{
		return socketMaster.info();
	}

	public boolean isMultiSessionEnabled()
	{ return socketMaster.isMultiSessionEnabled(); }

	public void setMultiSessionEnabled(boolean enabled_)
	{ socketMaster.setMultiSessionEnabled(enabled_); }

	/**
	 * Called back when accepting a new connection is finished.
	 * Override this only when nonblocking "accept" is used.
	 * @param serverSocket_ where the accepting occurred.
	 * @param new_ socket for the new connection.
	 */
	public void acceptFinished(InetSocket serverSocket_, InetSocket new_)
	{
		error("nonblocking accept", "'accept' finished is not handled");
	}

	/**
	 * Called back when a connection is established.
	 * Override this only when nonblocking "connect" is used.
	 * @param socket_ where the connection is established.
	 */
	public void connectFinished(InetSocket socket_)
	{
		error("nonblocking connect", "'connect' finished is not handled");
	}

	/**
	 * Called back when a connection is closed.
	 * Override this only when nonblocking "close" is used.
	 * @param socket_ where the connection is closed.
	 */
	public void closeFinished(InetSocket socket_)
	{
		error("nonblocking close", "'close' finished is not handled");
	}

	/**
	 * Called back when an error occurs during "accept" or "connect".
	 * Override this only when nonblocking "accept"/"connect" is used.
	 * @param socket_ to which the error is related.
	 * @param error_ the error message.
	 */
	public void error(InetSocket socket_, java.io.IOException error_)
	{
		error("socket error", error_);
	}

	protected void process(Object data_, Port inPort_)
	{
		if (!socketMaster.processSocket(data_, inPort_))
			super.process(data_, inPort_);
	}
}
