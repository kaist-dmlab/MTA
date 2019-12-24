// @(#)TCP_socket.java   1/2004
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

import drcl.comp.*;
import drcl.net.Module;
import drcl.net.Packet;
import drcl.util.scalar.LongInterval;
import drcl.inet.InetPacket;
import drcl.inet.transport.TCPPacket;

/**
This class adds 3way-handshaking and {@link SocketContract} to
{@link drcl.inet.transport.TCPb}.

@author Guanghui He, Hung-ying Tyan
*/
public class TCP_socket extends drcl.inet.transport.TCPb
{
	static {
		setContract(TCP_socket.class, Module.PortGroup_UP + "@",
			new ContractMultiple(new SocketContract(Contract.Role_REACTOR),
			new drcl.comp.lib.bytestream.ByteStreamContract(
					Contract.Role_REACTOR)));
	}

	static final long FLAG_NONBLOCKING_ACCEPT  = 1L << FLAG_UNDEFINED_START;
	static final long FLAG_NONBLOCKING_CONNECT = 1L << (FLAG_UNDEFINED_START+1);
	static final long FLAG_NONBLOCKING_CLOSE1  = 1L << (FLAG_UNDEFINED_START+2);
	static final long FLAG_NONBLOCKING_CLOSE2  = 1L << (FLAG_UNDEFINED_START+3);

	int connectionID = 0; // set by parent
	int localPort = 0; // set by parent
	int remotePort = 0 ;
	long localAddr = drcl.net.Address.NULL_ADDR;
	transient boolean connecting = true;
		// true until bidirection comm. is established
	transient ConnectTimer connectTimer = null;
		// for restarting 3way handshaking or closing
	transient boolean reallyEstablished = false; 
		// true if the data path is really connected
	transient Object msgID = null;
	transient Port nonblockingUpPort;

	static final String[] CLOSING_STATES = {
		null,
		"APP_SEND",
		"APP_RECV",
		"APP_SEND/APP_RECV",
		"PEER_SEND",
		"APP_SEND/PEER_SEND",
		"APP_RECV/PEER_SEND",
		"APP_SEND/APP_RECV/PEER_SEND"
	};

	transient int closing = 0;
		// closing states:
		// bit 0: app send close (to send FIN to peer) 
		// bit 1: app recv close (don't expect data in)
		// bit 2: peer send close (peer send FIN)

	/** State indicating that this TCP is about to accept but
	 * the actual accept comes in later than the SYN packet.
	 * SYN packet should be treated like it is in LISTEN. */
	static final int PRE_LISTEN = 100;
	static final int PRE_LISTEN2 = 101;

	{ state = CLOSED; }

	public TCP_socket()
	{ super(); }
	
	public TCP_socket(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		state = CLOSED;
		connecting = true;
		closing = 0;
		msgID = null;
		setConnectNonblocking(false);
		setAcceptNonblocking(false);
		setPeer(drcl.net.Address.NULL_ADDR);
		if (connectTimer != null) {
			cancelTimeout(connectTimer.handle);
			connectTimer = null;
		}
		reallyEstablished = false;
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TCP_socket that_ = (TCP_socket)source_;
		connectionID = that_.connectionID;
		localPort = that_.localPort;
		remotePort = that_.remotePort;
		localAddr = drcl.net.Address.NULL_ADDR;
	}

	public String info()
	{
		try {
		return "Connection: " + connectionID + ", " + localAddr + ":"
			+ localPort + " <--> "
			+ getPeer() + ":" + remotePort + "\n"
			+  "    States: "
			+ (connecting? "connecting, ":
				(closing > 0? "closed:" + CLOSING_STATES[closing] + ", ":""))
			+ "ConnectTimer=" + connectTimer 
			+ ", reallyEstablished=" + reallyEstablished
			+ (isAcceptNonblocking()? ", accept_nonblocking": "")
			+ (isConnectNonblocking()? ", connect_nonblocking": "")
			+ (isCloseNonblocking1()? ", close_nonblocking_one": "")
			+ (isCloseNonblocking2()? ", close_nonblocking_two": "")
			+ (nonblockingUpPort == null? "": ", " +nonblockingUpPort + "\n")
			+ super.info();
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	boolean isAppSendClose()
	{ return (closing & 0x01) > 0; }

	boolean isAppRecvClose()
	{ return (closing & 0x02) > 0; }

	boolean isAppClose()
	{ return (closing & 0x03) == 0x03; }

	boolean isPeerSendClose()
	{ return (closing & 0x04) > 0; }

	void setAppSendClose()
	{ closing |= 0x01; }

	// close for both send and recv
	void setAppClose()
	{ closing |= 0x03; }

	void setPeerSendClose()
	{ closing |= 0x04; }

    boolean isAcceptNonblocking()
    { return getComponentFlag(FLAG_NONBLOCKING_ACCEPT) != 0; }

    void setAcceptNonblocking(boolean enabled_)
    { setComponentFlag(FLAG_NONBLOCKING_ACCEPT, enabled_); }

    boolean isConnectNonblocking()
    { return getComponentFlag(FLAG_NONBLOCKING_CONNECT) != 0; }

    void setConnectNonblocking(boolean enabled_)
    { setComponentFlag(FLAG_NONBLOCKING_CONNECT, enabled_); }

    boolean isCloseNonblocking1()
    { return getComponentFlag(FLAG_NONBLOCKING_CLOSE1) != 0; }

    void setCloseNonblocking1(boolean enabled_)
    { setComponentFlag(FLAG_NONBLOCKING_CLOSE1, enabled_); }

    boolean isCloseNonblocking2()
    { return getComponentFlag(FLAG_NONBLOCKING_CLOSE2) != 0; }

    void setCloseNonblocking2(boolean enabled_)
    { setComponentFlag(FLAG_NONBLOCKING_CLOSE2, enabled_); }

	void notifyCallingThread()
	{
    	if (isAcceptNonblocking()) {
    		setAcceptNonblocking(false);
			acceptBottomHalf();
		}
		else if (isConnectNonblocking()) {
    		setConnectNonblocking(false);
			connectBottomHalf();
		}
		else if (isCloseNonblocking1()) {
    		setCloseNonblocking1(false);
			closeBottomHalf(false);
		}
		else if (isCloseNonblocking2()) {
    		setCloseNonblocking2(false);
			closeBottomHalf2();
		}
		else {
			notify(this);
		}
	}

	void acceptBottomHalf()
	{
		if (connectTimer != null) {
			cancelTimeout(connectTimer.handle);
			connectTimer = null;
		}

		if (state == CLOSED) {
			SocketContract.acceptReply(-1, getPeer(), remotePort,
							upPort, msgID, nonblockingUpPort);
		}
		else
			SocketContract.acceptReply(connectionID, getPeer(), remotePort,
							upPort, msgID, nonblockingUpPort);
	}

	void connectBottomHalf()
	{
		cancelTimeout(connectTimer.handle);
		connectTimer = null;

		if (state == CLOSED)
			SocketContract.connectReply(-1, upPort, msgID, nonblockingUpPort);
		else
			SocketContract.connectReply(connectionID, upPort, msgID,
							nonblockingUpPort);
	}

	void closeBottomHalf(boolean blocking_)
	{
		ack_syn_fin(false, false, true); // FIN
		if(state == ESTABLISHED)
			state = FIN_WAIT_1;
		else if(state == CLOSE_WAIT)
			state = LAST_ACK;

		double time_ = getTime() + rxt_timer(backoff);
		connectTimer = new ConnectTimer(TIMEOUT_CLOSE, time_);
		connectTimer.handle = setTimeoutAt(connectTimer, time_);
		if (blocking_) {
			wait(this);
			closeBottomHalf2();
		}
		else 
			setCloseNonblocking2(true);
	}

	void closeBottomHalf2()
	{
		if (connectTimer != null) {
			cancelTimeout(connectTimer.handle);
			connectTimer = null;
		}
		SocketContract.closeReply(connectionID, msgID, nonblockingUpPort);
	}

	protected synchronized void timeout(Object evt_)
	{
		if (!(evt_ instanceof ConnectTimer)) {
			super.timeout(evt_);
			return;
		}
		if (connectTimer == null)
			// timer has been cancelled
			return;
		else if (connectTimer.counter > 10) {
			if (isDebugEnabled())
				debug("Give up, state = " + STATES[state]
						+ ", close the socket now");
			state = CLOSED;
			//notify(this);
			notifyCallingThread();
			return;
		}

		switch (connectTimer.type) {
		case TIMEOUT_CONNECT:
			if (isDebugEnabled())
				debug("re-sending SYN at " + localPort + " to " + getPeer()
								+ ":" + remotePort);
			ack_syn_fin(false, true, false); // SYN
			state = SYN_SENT;
			break;
		case TIMEOUT_CLOSE:
			if (isDebugEnabled())
				debug("re-sending FIN at " + localPort + " to " + getPeer()
					+ ":" + remotePort + ", count=" + connectTimer.counter);
			ack_syn_fin(false, false, true); // FIN
			break;
		}
		// X: backoff?
		double time_ = getTime() + rxt_timer(backoff);
		connectTimer.time = time_;
		connectTimer.counter++;
		setTimeoutAt(connectTimer, time_);
	}
	
	protected void dataArriveAtUpPort(Object data_, Port upPort_)
	{
		if (data_ instanceof SocketContract.Message) {
			SocketContract.Message pack_ = (SocketContract.Message)data_;
			// ACCEPT
			if(pack_.isAccept()) {
				synchronized (this) {
					if (state != CLOSED && state < PRE_LISTEN) {
						SocketContract.error("the socket is already in use",
							-1, drcl.net.Address.NULL_ADDR, -1, localAddr,
							localPort, pack_.getMessageID(), upPort_);
						return;
					}
					if (state == PRE_LISTEN2) {
						// notify the downport thread in case if loop-back
						// request comes in earlier
						notify(this);
					}
					state = LISTEN;
					localPort = pack_.getLocalPort();
					if (isDebugEnabled())
						debug("accepting at port " + localPort);
					msgID = pack_.getMessageID();
					nonblockingUpPort = upPort_;
				}
				if (msgID == null) {
					wait(this);
					acceptBottomHalf();
				}
				else
					setAcceptNonblocking(true);
				return;
			}
			// CONNECT
			else if(pack_.isConnect()) {
				if (state != CLOSED) {
					SocketContract.error("the socket is already in use",
							-1, getPeer(), remotePort, localAddr,
							localPort, pack_.getMessageID(), upPort_);
					return;
				}
				localPort = pack_.getLocalPort();
				remotePort = pack_.getRemotePort();
				setPeer(pack_.getRemoteAddr());
				if (isDebugEnabled())
					debug("connecting at " + localPort + " to " + getPeer()
									+ ":" + remotePort);
				ack_syn_fin(false, true, false); // SYN
				double time_ = getTime() + rxt_timer(backoff);
				connectTimer = new ConnectTimer(TIMEOUT_CONNECT, time_);
				connectTimer.handle = setTimeoutAt(connectTimer, time_);
				state = SYN_SENT;
				msgID = pack_.getMessageID();
				nonblockingUpPort = upPort_;
				if (msgID == null) {
					wait(this);
					connectBottomHalf();
				}
				else
					setConnectNonblocking(true);
				return;
			}
			// CLOSE
			else if(pack_.isClose()) {
				synchronized(this){
					setAppClose();
					if (!reallyEstablished) {
						SocketContract.error("the data path for the socket "
										+ "is not established yet",
							connectionID, getPeer(), remotePort, 
							localAddr, localPort, null, upPort_);
						return;
					}
					switch (state) {
					case ESTABLISHED:
					case CLOSE_WAIT:
						connecting = false;
						msgID = pack_.getMessageID();
						if (isDebugEnabled())
							debug("CLOSING: msgID=" + msgID 
								+ ", sending_Buffer=" + getSendingBuffer());
						nonblockingUpPort = upPort_;
						if(getSendingBuffer() > 0){
							if (msgID == null) {
								wait(this);
								closeBottomHalf(true);
							}
							else
								setCloseNonblocking1(true);
						}
						else {
							closeBottomHalf(msgID == null);
						}
						break;
					case FIN_WAIT_1:
					case LAST_ACK:
						SocketContract.error("the socket is already in closing",
							connectionID, getPeer(), remotePort, 
							localAddr, localPort, pack_.getMessageID(),
										upPort_);
						break;
					default:
						if (isDebugEnabled())
							debug("CLOSING: state=" + state);
						// connecting
						if (connectTimer != null) {
							cancelTimeout(connectTimer.handle);
							connectTimer = null;
						}
						state = CLOSED;
						//notify(this); // the "connect" or "accept" thread
						notifyCallingThread();
						SocketContract.closeReply(connectionID,
									pack_.getMessageID(), upPort_);
					}
				} // synchronized (this)
			}
			else if(pack_.isEstablished()) {
				// data path (port connection) is established
				synchronized (this) {
					reallyEstablished = true;
					notify(this);
					upPort_.doSending(null); // anything is fine
				}
			}
		}
		else // send/query
			super.dataArriveAtUpPort(data_,upPort_);
	}
	
	// to be accessed by TCP_full directly
	protected void dataArriveAtDownPort(Object data_, Port downPort_)
	{
		InetPacket ipkt_ = (InetPacket)data_;
		TCPPacket tcppkt_ = (TCPPacket)ipkt_.getBody();

		synchronized (this) {

		if (connecting) {
			// SYN
			if (tcppkt_.isSYN() && !tcppkt_.isFIN() && !tcppkt_.isACK()) {
				if(state != LISTEN && state != PRE_LISTEN){
					ack_syn_fin(true, false, false); // ACK
				}
				else {
					if (state == PRE_LISTEN) {
						// wait until the "accept" command is established
						state = PRE_LISTEN2;
						wait(this);
					}

					setPeer(ipkt_.getSource());
					remotePort = tcppkt_.getSPort();
					localAddr = ipkt_.getDestination();
					ack_syn_fin(true, true, false); // ACK & SYN
					// in TCP state diagram, the state should be SYN_RCVD,
					// but it just compliates things.
					// to simplify it and to be robust, TCP should be
					// only concerned its side of connection (sending)
					// and use connectTimer to restarting its side of
					// connection.  This way, we only need SYN_SENT and
					// ESTABLISHED.
					// This ESTABLISHED state only indicates establishment
					// of my half of connection
					state = SYN_SENT;
					if (connectTimer == null) { // this side is accepting
						double time_ = getTime() + rxt_timer(backoff);
						connectTimer = new ConnectTimer(TIMEOUT_CONNECT, time_);
						connectTimer.handle = setTimeoutAt(connectTimer, time_);
					}
				}
			}
			// SYN & ACK
			else if(tcppkt_.isSYN() && tcppkt_.isACK() && !tcppkt_.isFIN()) {
				ack_syn_fin(true, false, false); // ACK
				if(state == SYN_SENT) {
					state = ESTABLISHED;
					if (isDebugEnabled())
						debug(" Connection is established");
					connecting = false;
					//notify(this);
					notifyCallingThread();
				}
			}
			// ACK
			else if(tcppkt_.isACK() && !tcppkt_.isSYN() && !tcppkt_.isFIN()) {
				if (state == SYN_SENT) {
					state = ESTABLISHED;
					if (isDebugEnabled())
						debug(" Connection is established");
					connecting = false;
					//notify(this);
					notifyCallingThread();
				}
				else if (state == LISTEN) {
					// ACK from previous session? just ignore
				}
				else {
					// shouldn't be here
					error("recv()", "recv an ack, invalid state:"
						+ STATES[state] + " and connecting is true: " + data_);
				}
			}
			return;
		}
		// FIN, may receive this when "connecting" is true
	    if (tcppkt_.isFIN() && !tcppkt_.isACK() && !tcppkt_.isSYN()){
			//if (connecting && state == ESTABLISHED)
			if (connecting)
				connecting = false;
			ack_syn_fin(true, false, false); // ACK

			setPeerSendClose();

			if(state == FIN_WAIT_1){
				state = CLOSING;
				return;
			}
			else if(state == ESTABLISHED){
				state = CLOSE_WAIT;
				return;
			}
			else if(state == FIN_WAIT_2){
				// XX: should be TIME_WAIT
				state = CLOSED;

				if (isAppClose())
					notifyCallingThread();

				return;
			}
			else {
				// just ignore
				//error("recv()", "recv a fin, invalid state:" + STATES[state]);
			}
			return;
		}
		else if (!connecting && state != ESTABLISHED && state != CLOSE_WAIT) {
			if(state == CLOSED)
				// just ignore
				return;

			// ACK
			if(tcppkt_.isACK() && !tcppkt_.isFIN() && !tcppkt_.isSYN()) {
				if (state == FIN_WAIT_1){
					state = FIN_WAIT_2;
					// have closed this end, waiting for peer to close its end
					if (connectTimer != null && connectTimer.type ==
									TIMEOUT_CLOSE) {
						cancelTimeout(connectTimer.handle);
						connectTimer = null;
					}
					// Tyan: don't notify application now because the other
					// end may erroneously continue sending data
					//// Tyan: can notify application now 
					////notify(this);
					//notifyCallingThread();
					return;
				}
				else if(state == CLOSING){
					// XX: should be TIME_WAIT
					state = CLOSED;
					//notify(this);
					notifyCallingThread();
					return;
				}
				else if(state == LAST_ACK){
					state = CLOSED;
					//notify(this);
					notifyCallingThread();
					return;
				}
				else {
					error("recv()", "recv an ack, invalid state:"
							+ STATES[state]);
					return;
				}
			}
			// FIN & ACK
			else if(tcppkt_.isACK() && tcppkt_.isFIN() && !tcppkt_.isSYN()){
				if(state == FIN_WAIT_1){
					ack_syn_fin(true, false, false); // ACK
					// XX: should be TIME_WAIT
					state = CLOSED;
					//notify(this);
					notifyCallingThread();
					return;
				}
				error("recv()", "recv an ack/fin, invalid state:"
							+ STATES[state]);
				return;
			}
			else if (isAppRecvClose()) {
				// cannot receive anymore
				if (isDebugEnabled())
					debug(" ** WARNING ** application closed for receive: " + ipkt_);
				return;
			}
			else
				;//receive otherwise
		}

		//if (connecting && tcppkt_.getSeqNo() > 0)
		//	connecting = false; // bi-drectional communication is established!

			if (!reallyEstablished)
				wait(this);

		} // end synchronized (this)

		super.dataArriveAtDownPort(data_, downPort_);

		// This is an ack that just cleared sending buffer
		// so notify the calling thread to start this half's closing
		// with sending FIN
		if (isAppSendClose() && getSendingBuffer() == 0 && tcppkt_.isACK())
			//notify(this);
			notifyCallingThread();
	}
	
	public int getLocalPort()
	{ return localPort; }

	public int getRemotePort()
	{ return remotePort; }

	public long getLocalAddr()
	{ return localAddr; }

	static final int TIMEOUT_CONNECT = 0;
	static final int TIMEOUT_CLOSE = 1;

	// for 3way handshaking and closing
	class ConnectTimer
	{
		int type;
		double time;
		int counter = 0;
		ACATimer handle;

		ConnectTimer(int type_, double time_)
		{
			type = type_; 
			time = time_;
		}

		public String toString()
		{
			return (type == 0? "CONNECT:":"CLOSE:") + time
				+ ", count=" + counter;
		}
	}
}
