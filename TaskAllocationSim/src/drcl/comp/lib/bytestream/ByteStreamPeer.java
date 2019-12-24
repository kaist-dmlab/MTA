// @(#)ByteStreamPeer.java   2/2004
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
import drcl.comp.Port;
import drcl.comp.WorkerThread;
import drcl.util.CircularBuffer;

/**
<code>ByteStreamPeer</code> is a helper class to conduct the 
{@link ByteStreamContract} both as an initiator and a reactor.
This class provides <em>blocked</em> send() and receive() methods to 
send/receive byte streams to/from the peer.

@author Hung-ying Tyan
@version 1.0, 6/2001
@see ByteStreamContract
*/
public class ByteStreamPeer  extends drcl.DrclObj implements ByteStreamConstants
{
	static final int DEFAULT_BUFFER_SIZE = 10240;

	CircularBuffer rbuffer; // for receive
	ByteStreamContract.Message pendingReceive; // store pending receive() call
	Object receiveLock = new Object();
	int rbuffersize = DEFAULT_BUFFER_SIZE;

	IOException exceptionToThrowForSend = null;
	IOException exceptionToThrowForReceive = null;

	// Available send buffer at reactor:
	// initialized to -1 so that the initiator will query for available send
	//     buffer the first time
	int sbuffer = -1;

	Port downPort;

	//String sstate = null; // for debug
	//String rstate = null; // for debug
	//String hstate = null; // for debug

    public ByteStreamPeer()
	{	super();  }
	
	public ByteStreamPeer(Port down_)
	{
		this();
		downPort = down_;
	}

	public void reset()
	{
		sbuffer = -1;
		rbuffer = null;
		pendingReceive = null;
		exceptionToThrowForSend = null;
		exceptionToThrowForReceive = null;
	}
	
	public void hookup(Port down_)
	{ downPort = down_; }
	
	public InputStream getInputStream()
	{
		return new InputStream() {
			public void close() throws IOException
			{}
				
			public int read(byte[] b_) throws IOException
			{ return receive(b_, 0, -1);	}
				
			public int read() throws IOException
			{
				byte[] oneByte = new byte[1];
				int reply_ = receive(oneByte, 0, -1);
				if (reply_ == 0) return -1;
				int ch_ = oneByte[0] & (int)0x0FF;
				return ch_;
			}
				
			// len_ can be 0 or negative, it will return whatever is available
			public int read(byte[] b_, int offset_, int len_) throws IOException
			{
				int reply_ = receive(b_, offset_, 
								b_ != null && len_ == b_.length? -1: len_);
				return reply_ == 0? -1: reply_; // conforms to InputStream spec
			}
				
			public int available() throws IOException
			{
				// XXX: could do better
				return 0;
			}
		};
	}
	
	public OutputStream getOutputStream()
	{
		return new OutputStream() {
			public void close() throws IOException
			{}
				
			public void flush() throws IOException
			{
				// XXX:
			}
				
			public void write(byte[] b_) throws IOException
			{ send(b_, 0, b_.length); }
				
			public void write(int b_) throws IOException
			{ send(new byte[]{(byte)b_}, 0, 1); }
				
			public void write(byte[] b_, int offset_, int len_)
				throws IOException
			{ send(b_, offset_, len_); }
		};
	}
	
	public String info()
	{
		return "downPort=" + downPort
				+ ",rcvBuffer=" + rbuffer
				+ ",pendingRcv=" + pendingReceive;
				//+ ",rstate=" + rstate
				//+ ",sstate=" + sstate
				//+ ",hstate=" + hstate;
	}

	public void setReceiveBufferSize(int size_)
	{ rbuffersize = size_; }
	
	public int getReceiveBufferSize()
	{ return rbuffersize; }
	
    /**
     * Sends <code>len_</code> bytes of data thru the down port.
	 * The lower layer protocol is supposed to contruct and send 
	 * <em>pseudo</em>-packets with no real data in it.
	 * 
     * @param len_   number of bytes to send.
     * @see ByteStreamContract
     */
    public void send(int len_) throws IOException
	{ send(null, 0, len_); }
	
   /**
     * Sends a buffer of data from <code>buffer_</code> thru the down port.
     * 
     * @param buffer_  where is put the data to be sent.
     * @see ByteStreamContract
     */
     public void send(byte[] buffer_) throws IOException
	{ send(buffer_, 0, buffer_.length); }
	 
   /**
     * Sends <code>len_</code> bytes of data from <code>buffer_</code> thru the 
	 * down port.
	 * The lower layer (transport) is supposed to contruct and send 
	 * <em>pseudo</em>-packets with no real data in it if <code>buffer_</code>
	 * is null.
	 * 
     * @param buffer_  where is put the data to be sent.
     * @param len_   number of bytes to send.
     * @see ByteStreamContract
     */
    public void send(byte[] buffer_, int len_) throws IOException
	{ send(buffer_, 0, len_); }
	
    /**
     * Sends <code>len_</code> bytes of data from <code>buffer_</code> thru the
	 * down port.
	 * The lower layer (transport) is supposed to contruct and send 
	 * <em>pseudo</em>-packets with no real data in it if <code>buffer_</code>
	 * is null.
	 * 
     * @param buffer_  where is put the data to be sent.
     * @param offset_ offset to the buffer where to start sending data.
     * @param len_   number of bytes to send.
     * @see ByteStreamContract
     */
    public void send(byte[] buffer_, int offset_, int len_) throws IOException
	{
		if (sbuffer < 0) {
			sbuffer = ((Integer)downPort.sendReceive(
							new ByteStreamContract.Message(QUERY))).intValue();
		}
		synchronized (this) {
			while (true) {
				ByteStreamContract.Message req_ = new
					ByteStreamContract.Message(SEND, buffer_, offset_, len_);
				//sstate = "send(): sendReceive()'ing...";
				Object reply_ = downPort.sendReceive(req_);
				if (!(reply_ instanceof Integer))
					throw new IOException("Error occurs in sending, got "
									+ reply_);
				sbuffer = ((Integer)reply_).intValue();
				if (sbuffer >= 0) break;
				// sbuffer is negative, reactor is overflowed
				// (-sbuffer) is leftover,
				// (len_ + sbuffer) is the number of bytes received at reactor
				offset_ += (len_ + sbuffer);
				len_ = -sbuffer;
				sbuffer = 0;

				exceptionToThrowForSend = null;
				//sstate = "send(): wait inside...";
				downPort.host.wait(this); // wait to be notified by reactor...
				if (exceptionToThrowForSend != null)
					throw exceptionToThrowForSend;
			}
			if (sbuffer == 0) {
				//sstate = "send(): wait outside...";
				downPort.host.wait(this); // wait to be notified by reactor...
				if (exceptionToThrowForSend != null)
					throw exceptionToThrowForSend;
			}
			//sstate = "send(): done.";
		}
    }

	/** Handles a message from the peer. */
	public void handle(ByteStreamContract.Message msg_)
	{
		switch (msg_.getType()) {
		case SEND:
			//hstate = "handle(): SEND synchronize on receiveLock...";
			synchronized (receiveLock) {
				//hstate = "handle(): SEND synchronize on receiveLock (after)...";
				if (pendingReceive != null && pendingReceive.offset >= 0) {
					// transfer directly from msg to user buffer

					if (pendingReceive.length > msg_.length) {
						// incoming bytes do not fill the user buffer
						if (msg_.buffer != null && pendingReceive.buffer
										!= null) {
							System.arraycopy(msg_.buffer, msg_.offset,
								pendingReceive.buffer, pendingReceive.offset,
									msg_.length);
						}
						pendingReceive.length -= msg_.length;
						pendingReceive.offset += msg_.length;
						msg_.length = 0;
					}
					else {
						// fill up the user buffer, or anything available
						if (pendingReceive.length < 0) {
							// copy any bytes available and stop
							pendingReceive.length = pendingReceive.buffer.length
								- pendingReceive.offset;
							if (pendingReceive.length > msg_.length)
									pendingReceive.length = msg_.length;
							// copy bytes
							if (msg_.buffer != null && pendingReceive.buffer
											!= null) {
								System.arraycopy(msg_.buffer, msg_.offset,
									pendingReceive.buffer,
									pendingReceive.offset,
									pendingReceive.length);
							}
							msg_.length -= pendingReceive.length;
							msg_.offset += pendingReceive.length;
						}
						else {
							// copy bytes
							if (msg_.buffer != null && pendingReceive.buffer
											!= null) {
								System.arraycopy(msg_.buffer, msg_.offset,
									pendingReceive.buffer,
									pendingReceive.offset,
									pendingReceive.length);
							}
							msg_.length -= pendingReceive.length;
							msg_.offset += pendingReceive.length;
							pendingReceive.length = 0;
						}
						pendingReceive.offset = -1; // ...
						//downPort.host.debug("Notify receive...");
						//hstate = "handle(): SEND notify on receiveLock...";
						downPort.host.notify(receiveLock);
					}
				}
				// leftover in msg_ stored in rbuffer
				if (msg_.length > 0) {
					if (rbuffer == null)
						rbuffer = new CircularBuffer(rbuffersize);
					int len_ = rbuffer.append(msg_.buffer, msg_.offset,
									msg_.length);
					if (len_ < msg_.length) { // overflow
						downPort.doSending(new Integer(len_ - msg_.length));
						break;
					}
				}
				downPort.doSending(new Integer(rbuffer == null? rbuffersize:
										rbuffer.getAvailableSpace()));
			}
			break;
		case QUERY:
			//hstate = "handle(): QUERY synchronize on receiveLock...";
			synchronized (receiveLock) {
				downPort.doSending(new Integer(rbuffer == null? rbuffersize:
										rbuffer.getAvailableSpace()));
			}
			break;
		case REPORT:
			downPort.host.notify(this);
			break;
		default:
			// XX: warning?
			//hstate = "handle(): wrong data.";
			return;
		}
		//hstate = "handle(): done...";
	}
	
    /**
     * Receives whatever is currently buffered from the lower layer protocol.
	 * No real data is received.
	 * 
     * @return the number of bytes that are really received.
	 * 		0 if EOF is encountered.
     * @see ByteStreamContract
     */
    public int receive() throws IOException
	{ return receive(null, 0, -1); }
	
    /**
     * Receives <code>size_</code> bytes of data from the lower layer protocol.
	 * No real data is received.
	 * 
     * @param size of data expected to receive.
     * @return the number of bytes that are really received.
	 * 		0 if EOF is encountered.
     * @see ByteStreamContract
     */
    public int receive(int size_) throws IOException
	{ return receive(null, 0, size_); }
	
    /**
     * Receives a buffer of data from the lower layer protocol.
     * This method will be blocked until some bytes are filled in.
     * 
     * @param buffer_ the buffer to store the incoming data.
     * @return the number of bytes that are really received.
	 * 		0 if EOF is encountered.
     * @see ByteStreamContract
     */
    public int receive(byte[] buffer_) throws IOException
	{ return receive(buffer_, 0, -1); }

    /**
     * Receives a maximun of <code>size_</code> data from the lower layer 
	 * protocol.
	 * No real data is received if <code>buffer_</code> is null.
	 * If <code>size_</code> is greater than 0, this method will be blocked 
	 * until exactly the size of data is filled to the buffer.
	 * If <code>size_</code> is less than 0, then the method will grab all the
	 * data available right now till the buffer is full.
	 * 
     * @param buffer_ the buffer to store the incoming data.
     * @param size of data expected to receive.
     * @return the number of bytes that are really received.
	 * 		0 if EOF is encountered.
     * @see ByteStreamContract
     */
    public int receive(byte[] buffer_, int size_) throws IOException
	{ return receive(buffer_, 0, size_); }
	
    /**
     * Receives a maximun of <code>size_</code> data from the lower layer 
	 * protocol.
	 * No real data is received if <code>buffer_</code> is null.
	 * If <code>size_</code> is greater than 0, this method will be blocked 
	 * until exactly the size of data is filled to the buffer.
	 * If <code>size_</code> is less than 0, then the method will grab all the 
	 * data available right now till the buffer is full.
	 * 
     * @param buffer_ the buffer to store the incoming data.
     * @param offset_ offset in the buffer to start storing the incoming data.
     * @param size_ size of data expected to receive.
     * @return the number of bytes that are really received.
	 * 		0 if EOF is encountered.
     * @see ByteStreamContract
     */
    public int receive(byte[] buffer_, int offset_, int size_) 
		throws IOException
	{
		//System.out.println("receive:" + size_ + "/buffer:" + buffer_.length
		//						+ ", rbuffer:" + rbuffer);
		//rstate = "receive(): synchronize on receiveLock...";
		synchronized (receiveLock) {
			// enough bytes in receiving buffer
			if (rbuffer != null && rbuffer.getSize() > 0
					&& rbuffer.getSize() >= size_) {
				int len_ = rbuffer.remove(buffer_, offset_, size_);
				//rstate = "receive(): doSending()-1...";
				downPort.doSending(new ByteStreamContract.Message(REPORT,
										rbuffer.getAvailableSpace()));
				//rstate = "receive(): done.";	
				return len_;
			}

			if (pendingReceive != null) {
				//rstate = "receive(): done but error.";	
				return ERROR;
			}

			pendingReceive = new ByteStreamContract.Message(-1, buffer_,
							offset_, size_);

			if (rbuffer != null && rbuffer.getSize() > 0) {
				int len_ = rbuffer.remove(buffer_, pendingReceive.offset,
								pendingReceive.length);
				//rstate = "receive(): doSending()-2...";
				downPort.doSending(new ByteStreamContract.Message(REPORT,
										rbuffer.getAvailableSpace()));
				pendingReceive.offset += len_;
				pendingReceive.length -= len_;
				//downPort.host.debug("Pending: " + pendingReceive
				//		+ ", rbuffer:" + rbuffer);
			}

			// wait until new bytes come in
			//System.out.println("Pending wait...");
			//rstate = "receive(): pending wait...";	
			exceptionToThrowForReceive = null;
			downPort.host.wait(receiveLock);
			if (exceptionToThrowForReceive != null) {
				//rstate = "receive(): throw exception.";	
				throw exceptionToThrowForReceive;
			}

			//System.out.println("Pending done:" + pendingReceive);
			// if size_ < 0, then the actual receive size is in 
			// pendingReceive.length, see handle()
			if (size_ < 0) size_ = pendingReceive.length;
			pendingReceive = null;
			//rstate = "receive(): done.";	
			return size_;
		} // synchronized (receiveLock)
    }

	/** Interrupts a blocked sending with the default IO exception.
	 * @see #send(byte[], int, int) 
	 */
	public void interruptSending()
	{ interruptSending(new IOException("send() being interrupted")); }

	/** Interrupts a blocked sending with the specified IO exception.
	 * @see #send(byte[], int, int) 
	 */
	public void interruptSending(IOException e)
	{
		synchronized (this) {
			exceptionToThrowForSend = e;
			downPort.host.notify(this);
		}
	}

	/** Interrupts a blocked receiving with the default IO exception.
	 * @see #receive(byte[], int, int) 
	 */
	public void interruptReceiving()
	{ interruptReceiving(new IOException("receive() being interrupted")); }

	/** Interrupts a blocked receiving with the given exception.
	 * @see #receive(byte[], int, int) 
	 */
	public void interruptReceiving(IOException e)
	{
		synchronized (receiveLock) {
			exceptionToThrowForReceive = e;
			downPort.host.notify(receiveLock);
		}
	}

	public String toString()
	{ return super.toString() + ":" + info(); }
}
