// @(#)SApplication.java   2/2004
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

package drcl.inet.application;

import java.io.*;
import drcl.comp.*;
import drcl.comp.lib.bytestream.ByteStreamContract;
import drcl.comp.lib.bytestream.ByteStreamPeer;

// Implementation note: the original design has open(),close() function allowing
// applications to setup a connection by itself. This scheme does match the rest of the
// simulation system. So they are left unimplemented.
// Considerations: these features might be needed by multicasting applications.

/**
 * Single-session (or simplified) application base class.
 * No open or close is needed.  Assume that the connection is already established
 * at the transport layer.  This class provides a set of send() and receive()
 * methods to send/receive byte streams to/from the peer application process.
 * One can also use getInputStream() and getOutputStream() to send/receive
 * byte stream using the stream classes in java.io.
 * 
 * @author Yung-ching Hsiao, Hung-ying Tyan
 */
public class SApplication  extends drcl.net.Module
{
    public SApplication()
	{	super();  }
	
    public SApplication(String id_)
	{	super(id_);   }

	ByteStreamPeer helper = new ByteStreamPeer(downPort);
	
	public InputStream getInputStream()
	{	return helper.getInputStream();	}
	
	public OutputStream getOutputStream()
	{	return helper.getOutputStream();	}
	
	public String info()
	{
		return "ByteStreamPeer:" + helper.info() + "\n";
	}

    /**
     * Sends <code>len_</code> bytes of <em>pseudo</em>-data through the down port.
     * @param len_   size of the data to send.
     */
    protected void send(int len_) throws IOException
	{ helper.send(null, len_); }
	
    /**
     * Sends <code>len_</code> bytes of data from <code>buffer_</code> through the down port.
	 * <em>Pseudo</em>-data is sent if <code>buffer_</code> is null.
     * @param buffer_  where is put the data to be sent.
     * @param len_   size of the data to send.
     */
    protected void send(byte[] buffer_, int len_) throws IOException
	{	helper.send(buffer_, len_);    }
	
    /**
     * Receives whatever is currently available through the down port.
	 * No real data is received.
     * @return the number of bytes received. 0 if EOF is encountered.
     */
    protected int receive() throws IOException
	{ return helper.receive(null, -1); }
	
    /**
     * Receives <code>size_</code> bytes of data through the down port.
	 * No real data is received.
	 * If <code>size_</code> is greater than 0, this method will be blocked until exactly
	 * the amount of data is available.
	 * If <code>size_</code> is less than 0, then the method will grab all the data
	 * available right now.
     * @param size of data expected to receive.
     * @return the number of bytes received. 0 if EOF is encountered.
     */
    protected int receive(int size_) throws IOException
	{ return helper.receive(null, size_); }
	
    /**
     * Receives a buffer of data through the down port.
     * This method will be blocked until the buffer is filled.
     * @param buffer_ the buffer to store the incoming data.
     * @return the number of bytes received. 0 if EOF is encountered.
     */
    protected int receive(byte[] buffer_) throws IOException
	{ return helper.receive(buffer_, buffer_.length); }

    /**
     * Receives a maximun of <code>size_</code> data through the down port.
	 * No real data is received if <code>buffer_</code> is null.
	 * If <code>size_</code> is greater than 0, this method will be blocked until exactly
	 * the amount of data is filled to the buffer.
	 * If <code>size_</code> is less than 0, then the method will grab all the data
	 * available right now till the buffer is full.
     * @param buffer_ the buffer to store the incoming data.
     * @param size of data expected to receive.
     * @return the number of bytes received. 0 if EOF is encountered.
     */
    protected int receive(byte[] buffer_, int size_) throws IOException
	{ return helper.receive(buffer_, size_); }

	protected void process(Object data_, Port inPort_)
	{
		helper.handle((ByteStreamContract.Message)data_);
	}

	// for testing
	public void interrupt()
	{ interruptReceive(new IOException("read() being interrupted")); }

	/** Interrupts a receive() with an exception.
	 * The process that is blocked by receive() will be interrupted
	 * with the given exception. 
	 */
	protected void interruptReceive(IOException e)
	{
		helper.interruptReceiving(e);
	}
}
