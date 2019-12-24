// @(#)NonblockingSocketHandler.java   9/2002
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

import java.io.IOException;
import java.util.Vector;
import drcl.comp.Port;

/** Interface defining callbacks for nonblocking socket calls. */
public interface NonblockingSocketHandler
{
	/**
	 * Called back when accepting a new connection is finished.
	 * @param serverSocket_ where the accepting occurred.
	 * @param new_ socket for the new connection.
	 */
	public void acceptFinished(InetSocket serverSocket_, InetSocket new_);

	/**
	 * Called back when a connection is established.
	 * @param socket_ where the connection is established.
	 */
	public void connectFinished(InetSocket socket_);

	/**
	 * Called back when a connection is closed.
	 * @param socket_ where the connection is closed.
	 */
	public void closeFinished(InetSocket socket_);

	/**
	 * Called back when an error occurs during "accept" or "connect".
	 * @param socket_ to which the error is related.
	 * @param error_ the error message.
	 */
	public void error(InetSocket socket_, IOException error_);

	/**
	 * Called back when a sending is finished.
	 * @param socket_ where the sending is done.
	public void sendFinished(InetSocket socket_);
	 */
}
