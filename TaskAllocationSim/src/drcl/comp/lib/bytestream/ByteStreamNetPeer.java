// @(#)ByteStreamNetPeer.java   2/2004
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

package drcl.comp.lib.bytestream;

import java.io.*;
import java.net.*;
import drcl.comp.*;

// XXX: how to "stop" this component

/**
An intermediate component that acts as between a {@link ByteStreamContract}
component and a real network socket.
<p>As a reactor, it absorts any size of incoming data from the initiator and 
sends the data to the real socket.  It always reports available buffer size of 1
to keep the initiator sending data.
<p>As an initiator, it relays data from the real socket to the reactor in
bulks of size <code>dataUnit</code>.
*/
public class ByteStreamNetPeer extends Component
		implements ByteStreamConstants, ActiveComponent
{
	Socket socket;
	Port upPort = addPort("up", false);
	ByteStreamPeer helper = new ByteStreamPeer(upPort);
	int dataUnit = 10240;
	//String state = null; // for debug
	
	public ByteStreamNetPeer() { super(); }
	
	public ByteStreamNetPeer(String id_) { super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		ByteStreamNetPeer that_ = (ByteStreamNetPeer)source_;
	}
	
	public String info()
	{
		return "Socket: " + socket + "\n"
				+ "Helper: " + helper + "\n";
				//+ "state = " + state + "\n";
	}
	
	public void reset()
	{
		super.reset();
		if (socket != null) {
			try { socket.close(); } catch (Exception e_) {}
			socket = null;
		}
		helper.reset();
		socketReady = false;
	}
	
	ServerSocket ssocket;
	boolean socketReady = false;

	/** Sets up a real socket and makes the component listens to the socket. */
    public void accept(int serverPort_)
    {
        try {
            ssocket = new ServerSocket(serverPort_);
        }
        catch (Exception e_) {
            drcl.Debug.error(this, "connect(): " + e_);
            return;
        }
 
        new Thread(new Runnable() { // to avoid the shell from getting stuck
            public void run()
            {
                if (isStarted()) {
                    drcl.Debug.error(this, "connect(): already started, "
							+ "need to reset() to start again.");
                    return;
                }
 
                try {
					synchronized (ByteStreamNetPeer.this) {
                    	socket = ssocket.accept();
						System.out.println(ByteStreamNetPeer.this
								+ ": socket is ready.");
						socketReady = true;
						ByteStreamNetPeer.this.notify();
					}
					System.out.println("Server connected");
                    ByteStreamNetPeer.this.run();
                }
                catch (Exception e_) {
                    drcl.Debug.error(this, "connect().thread: " + e_);
                }
            }
        }).start();
    }

	/** Sets up a real socket and makes the component connects to the real
	 * remote socket. */
	public void connect(String serverAddress_, int serverPort_)
	{
		if (isStarted()) {
			drcl.Debug.error(this, "connect(): already started, "
							+ "need to reset() to start again.");
			return;
		}
		
		try {
			InetAddress serverAddr_ = InetAddress.getByName(serverAddress_);
			socket = new Socket(serverAddress_, serverPort_);
			System.out.println("Client connected");
			synchronized (this) {
				socketReady = true;
				this.notify();
			}
			run();
		}
		catch (Exception e_) {
			drcl.Debug.error(this, "connect(): " + e_);
		}
	}

	public void setDataUnit(int dataUnit_)
	{ dataUnit = dataUnit_; }

	public int getDataUnit()
	{ return dataUnit; }
	
	protected void _start()
	{
		if (socket == null) {
			error("_start()", "no socket, should use connect() or accept() "
							+ "to start");
			return;
		}
		if (helper == null) {
			error("_start()", "no helper");
			return;
		}
		
		byte[] buffer_ = new byte[dataUnit];
		
		// all we need to do is
		// keep transfering what is from network to the helper buffer
		if (ssocket == null)
			debug("Net client started.");
		else
			debug("Net server started.");
		try {
			while (true) {
				int size_ = socket.getInputStream().read(buffer_);
				if (isDebugEnabled())
					debug("read " + size_ + ": "
									+ drcl.util.StringUtil.toString(buffer_));
				if (size_ < 0) {
					// XXX: close helper
					reset();
					break;
				}
				helper.send(buffer_, 0, size_);
			}
		}
		catch (IOException e_) {
			error("_start()", e_);
		}
	}
	
	protected void process(Object data_, Port inPort_) 
	{
		ByteStreamContract.Message msg_ = (ByteStreamContract.Message)data_;

		switch (msg_.getType()) {
		case SEND:
			//state = "SEND: synchronized on this";
			synchronized (this) {
				if (!socketReady) debug("socket not ready...");
				while (!socketReady)
					try {
						this.wait();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
			}

			if (socket == null) {
				error(data_, "process()", inPort_,
								"no socket is open to transmit the data");
				inPort_.doLastSending(new Integer(ERROR));
					// notify end of sending
			}
			//state = "SEND: write to socket";
			try {
				if (msg_.buffer == null)
					msg_.buffer = new byte[msg_.offset + msg_.length];
				socket.getOutputStream().write(msg_.buffer, msg_.offset,
								msg_.length);
				inPort_.doLastSending(new Integer(1));
					// as long as it's positive...
			}
			catch (Exception e_) {
				error(data_, "process()", inPort_, e_);
				inPort_.doLastSending(new Integer(ERROR));
			}
			//state = "SEND: done";
			break;
		case QUERY: 
			inPort_.doLastSending(new Integer(1));
				// as long as it's positive...
			break;
		default:
			helper.handle(msg_);
		}
	}
}
