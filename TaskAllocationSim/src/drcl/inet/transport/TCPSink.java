// @(#)TCPSink.java   2/2003
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

package drcl.inet.transport;

import java.util.Vector;
import java.util.LinkedList;
import java.util.ListIterator;
import drcl.data.DoubleObj;
import drcl.comp.*;
import drcl.util.scalar.LongInterval;
import drcl.util.scalar.LongSpace;
import drcl.comp.lib.bytestream.ByteStreamContract;
import drcl.inet.InetPacket;
import drcl.net.Address;

/**
This component implements the single-session receiving-side TCP.

<p>By default, when this component receives a TCP packet,
it processes the timestamp option and sends back an acknowledgement.
In addition, one can enable the SACK flag to make this component
append the SACK blocks in the acknowledgement packets.
The realization of SACK is based on [RFC2018].

<p>This component also implements delayed ACK.
The delay timer is 100ms(0.1 second) as specified in [TCPILL2].
When the delayed ACK flag is enabled, an acknowledgment is sent when
(1) the delay timer expires or (2) a new data packet arrives.

<p>Since only one session is handled in this component,
open and close of a connection are not implemented,
nor is 3-way handshaking.

<p>Additional usage infomation:
<ol>
<li> To change TTL value, use {@link #setTTL(int)}.
<li> To change receiving buffer size, use {@link #setReceivingBuffers(int)}.
<li> To change delay timer period, use {@link #setDelayACKTimeout(double)}.
</ol>

References:
<ul>
<li>[TCPILL1] W. Stevens, TCP/IP Illustrated vol.1: The Protocols,
	Addison-Wesley,1994. 
<li>[TCPILL2] G. Wright and W. Stevens, TCP/IP Illustrated vol.2: The
	Implementation, Addison-Wesley,1995 
<li>[RFC793] J. Postel, Transmission Control Protocol, September 1981. 
<li>[RFC2018] M. Mathis, J. Mahdavi, S. Floyd and A. Romanow, TCP Selective
	Acknowledgment Options, Octobor 1996.
<li>[RFC2581] M. Allman, V. Paxson and W. Stevens, TCP Congestion Control,
	April 1999. 
</ul>

@see TCP
@see TCPPacket
@author Yuan Gao, Yung-ching Hsiao, Hung-ying Tyan
 */
public class TCPSink extends drcl.inet.Protocol
			implements TCPConstants, Connection
{ 
	/** Sets to true to make TCP ns compatible */
	public static boolean NS_COMPATIBLE = false;

	static {
		setContract(TCPSink.class, "*@" + drcl.net.Module.PortGroup_UP,
			new drcl.comp.lib.bytestream.ByteStreamContract(
					Contract.Role_REACTOR));
	}

	public String getName()
	{ return "tcp"; }
	
	// seq# of first byte of each received packet
	Port seqNoPort = addEventPort(SEQNO_PORT_ID);

	protected boolean SACK = false;	// SACK flag	
	// sack blocks, sorted by the time the blocks are created, the first one
	// is the most recent
	transient LinkedList llSackBlock = null;

	/**
	 * Delay acknowledgement flag, set it as TRUE will let this sink
	 * use delay acknowledgement. Default delay timer is set as
	 * 100ms as in [TCPILL2].
	 */
	boolean DelayACK = false; // Use delayed ACK
	transient TM_EVT ACKPending = null; // Delayed ACK pending
	
	long peer = Address.NULL_ADDR;	// Used for Des in forwarding
	double delayTimer = 0.1; //Delay ACK timeout value
	
	/* for threeway-handshaking*/
	transient int state = ESTABLISHED;

	int TTL = 255;
	int MSS = 512;
	int RBUFFER_SIZE = TCP.AWND_DEFAULT * MSS;	// receiving buffer size 64K
	long WNDBG = 0;	// Receiving window beginning
	transient LongSpace receivedSeq = new LongSpace(0, 0);
		// used to calculate available receiving buffer and construct SACK
		// blocks
	
	transient Vector rbuffer = new Vector();
		// storing outstanding TCP packets (not yet sent to application)
	transient long snd_nxt = WNDBG;
		// sequence # of next byte to be sent to application
	transient long rcv_nxt = WNDBG;
		// sequence # of next byte expected to be received from the remote peer
	transient boolean appAskedForData = true; 
	Connection connection = this;
    
	static final String[] DEBUG_LEVELS = 
		{"rcv", "send", "sack", "out-of-order", "sample"};
	public static final int DEBUG_RCV= 0;
	public static final int DEBUG_SEND = 1;
	public static final int DEBUG_SACK = 2;
	public static final int DEBUG_OOO = 3;
	public static final int DEBUG_SAMPLE = 4;
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVELS; }
	
	public TCPSink()
	{ super(); }
	
	public TCPSink(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		state = ESTABLISHED;
		if (rbuffer != null) rbuffer.removeAllElements();
		snd_nxt = WNDBG;
		rcv_nxt = WNDBG;
		appAskedForData = true;
		receivedSeq.reset(0, 0);
		if (llSackBlock != null) 
			llSackBlock = null;
	}

	public void duplicate(Object source_) 
	{ 
		super.duplicate(source_);
		TCPSink that_ = (TCPSink)source_;
		setTTL(that_.getTTL());
		setReceivingBuffers(that_.getReceivingBuffers());
		setMSS(that_.getMSS());
		setDelayACKEnabled(that_.isDelayACKEnabled());
		setSackEnabled(that_.isSackEnabled());
		setDelayACKTimeout(that_.getDelayACKTimeout());
	}

	public void setTTL(int ttl)
	{ TTL = ttl; }
	
	public int getTTL()
	{ return TTL;	}
		
	public void setMSS(int mss)
	{
		if (MSS != mss) {
			setReceivingBuffers(RBUFFER_SIZE / MSS * mss);
			MSS = mss;
		}
	}
	
	public int getMSS()
	{ return MSS; }

	public void setReceivingBuffers(int awnd_)
	{ RBUFFER_SIZE = awnd_; }
	
	public int getReceivingBuffers()
	{ return RBUFFER_SIZE;	}
	
	public int getAvailableReceivingBuffers()
	{
		/*
		return RBUFFER_SIZE - (int)receivedSeq.getSize(snd_nxt, snd_nxt
						+ RBUFFER_SIZE);
		*/
		return RBUFFER_SIZE - (int)(rcv_nxt - snd_nxt);
	}
	
	public void setSackEnabled(boolean sack_)
	{
		SACK = sack_;
		if (!SACK)
			llSackBlock = null;
	}

	public boolean isSackEnabled()
	{ return SACK; }

	public void setDelayACKEnabled(boolean delayack_)
	{ DelayACK = delayack_; }

	public boolean isDelayACKEnabled()
	{ return DelayACK; }

	public void setDelayACKTimeout(double v_)
	{ delayTimer = v_; }

	public double getDelayACKTimeout()
	{ return delayTimer; }
	
	/** Handles timeout events. */
	protected void timeout(Object evt_)
	{
		switch(((TM_EVT)evt_).type) {
		case DELAY_ACK:
			synchronized (rbuffer) {
				ack_syn_fin(false, ((TM_EVT)evt_).aTS);
				ACKPending = null;
			}
			break;
		}			
	}

	// This method is used by other methods with upPort_ = null
	/**
	 * The up port group ports follow the
	 * {@link drcl.comp.lib.bytestream.ByteStreamContract}.
	 */
	protected void dataArriveAtUpPort(Object data_, Port upPort_) 
	{
		try {
			if (upPort_ == null
				|| ((ByteStreamContract.Message)data_).isReport())
				// send to application as many bytes as possible
				synchronized (rbuffer) {
					if (rcv_nxt == snd_nxt) return;
					boolean bufferFull_ = getAvailableReceivingBuffers() == 0;
					int i = 0;
					for (; i<rbuffer.size(); ) {
						TCPPacket pkt_ = (TCPPacket)rbuffer.elementAt(i);
						long seqno_ = pkt_.getSeqNo();
						if (seqno_ > snd_nxt) break;
						// end_: exclusive
						long end_ = seqno_ + pkt_.size - pkt_.headerSize;
						//boolean entirePkt_ = rcv_nxt >= end_;
						//if (!entirePkt_) end_ = rcv_nxt;
						ByteStreamContract.Message sendReq_ =
							new ByteStreamContract.Message(
									ByteStreamContract.SEND,
									(byte[])pkt_.getBody(),
									(int)(snd_nxt-seqno_), (int)(end_-snd_nxt));
						// trick: use upPort instead of upPort_
						int len_ = ((Integer)upPort.sendReceive(
												sendReq_)).intValue();
						snd_nxt = len_ >= 0? end_: end_ + len_;
						//if (entirePkt_ && len_ >= 0) // this pkt is cleared
						if (len_ >= 0) // this pkt is cleared
							i++;
						// break the loop if no more bytes available or
						// application cannot receive more
						if (snd_nxt == rcv_nxt || len_ <= 0) {
							appAskedForData = len_ > 0;
							break;
						}
					}
					// remove the first i packets from rbuffer
					if (i > 0) {
						for (int j=i; j<rbuffer.size(); j++)
							rbuffer.setElementAt(rbuffer.elementAt(j), j-i);
						rbuffer.setSize(rbuffer.size()-i);
					}
					if (bufferFull_ && getAvailableReceivingBuffers() > 0)
						ack_syn_fin(false, -1.0);
				}
		}
		catch (Exception e_) {
			if (e_ instanceof ClassCastException)
				error(data_, "dataArriveAtUpPort()", upPort,
								"unrecognized data: " + e_); 
			else
				e_.printStackTrace();
		}
	}	
	
	/** The down port follows the {@link drcl.inet.contract.PktDelivery}
	 * contract. */
	protected synchronized void dataArriveAtDownPort(Object data_,
					Port downPort_) 
	{
		try {
			peer = ((InetPacket)data_).getSource();
			recv((TCPPacket)((InetPacket)data_).getBody());
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error(data_, "dataArriveAtDownPort()", downPort_,
							"unrecognized data? " + e_);
		}
	}
	
	/** Handles incoming packets. */
	protected void recv(TCPPacket pkt_)
	{
		long seq_;
		int size_, bufsize_;
		byte[] payload_ = (byte[])pkt_.getBody();	

		option_process(pkt_);

		seq_ = pkt_.getSeqNo();	// Get the seqence number

		if (seqNoPort._isEventExportEnabled())
			seqNoPort.exportEvent(TCP.SEQNO_EVENT,
							new DoubleObj((double)seq_/MSS), null);

		size_ = pkt_.size - pkt_.headerSize;
		//System.out.println("At receiver, Timestamp = " + pkt_.getTS());

		synchronized (rbuffer) {
			// expected_: is the received segment expected (or out-of-order)?
			boolean expected_ = rcv_nxt == seq_;
			boolean duplicate_ = !expected_
					&& receivedSeq.contains(seq_, seq_ + size_);
			if (expected_) rcv_nxt = seq_ + size_;
			// put pkt to the receiving buffer
			if (!duplicate_) {
				receivedSeq.checkin(seq_, seq_ + size_);
				if (SACK)
					SACK_process(seq_, size_, expected_);
				boolean done_ = false;
				long end_ = seq_ + size_;
				if (end_ > snd_nxt + RBUFFER_SIZE) {
					// receiving buffer overflow, just discard the packet
					size_ = 0;
				}
				else {
					// insert the packet to rbuffer
					for (int i=rbuffer.size()-1; i>=0; i--) {
						TCPPacket tmp_ = (TCPPacket)rbuffer.elementAt(i);
						long tmpseq_ = tmp_.getSeqNo();
						long tmpend_ = tmpseq_ + tmp_.size - tmp_.headerSize;
						/* duplicate being checked above, Tyan, 2/14/2003
						if (tmpseq_ <= seq_ && end_ <= tmpend_) {
							done_ = duplicate_ = true;
							size_ = 0;
							break;
						}
						*/
						if (tmpseq_ < seq_) {
							rbuffer.insertElementAt(pkt_, i+1);
							done_ = true;
							break;
						}
						else if (end_ >= tmpend_)
							rbuffer.removeElementAt(i);
					}
					if (!done_)
						rbuffer.insertElementAt(pkt_, 0);
				}
				if (size_ > 0) {
					// update rcv_nxt:
					if (expected_) {
						/*
						for (int i=0; i<rbuffer.size(); i++) {
							TCPPacket tmp_ = (TCPPacket)rbuffer.elementAt(i);
							if (rcv_nxt >= tmp_.getSeqNo()) {
								end_ = tmp_.getSeqNo() + tmp_.size
										- tmp_.headerSize;
								if (rcv_nxt < end_) rcv_nxt = end_;
							}
						}
						// sanity check
						if (rcv_nxt != receivedSeq.getLongInterval(0).end)
							error("recv()", "rcv_nxt not matched");
						*/
						rcv_nxt = receivedSeq.getLongInterval(0).end;
					}
					// deliver to application as many bytes as possible
					dataArriveAtUpPort(null, null);
				}
			}

			if (isDebugEnabled()) {
				if (isDebugEnabledAt(DEBUG_SAMPLE)
					|| isDebugEnabledAt(DEBUG_RCV)) {
					if (duplicate_)
						debug("RECEIVED: " + (seq_/MSS) + "/" + seq_ + ", "
							+ receivedSeq + ", DUPLICATE");
					else
						debug("RECEIVED: " + (seq_/MSS) + "/" + seq_ + ", "
							+ receivedSeq + "--rbuffer:" + printBuffer(true));
				}
				else if (!expected_ && !duplicate_ && (seq_ - rcv_nxt <= MSS)
					&& isDebugEnabledAt(DEBUG_OOO))
					debug("RECEIVED_OOO: " + (seq_/MSS) + "/" + seq_ + "+"
						+ (pkt_.size - pkt_.headerSize)
						+ ", expected " + rcv_nxt + "(" + (seq_ - rcv_nxt) + "), "
						+ receivedSeq + "--rbuffer:" + printBuffer(true));
			}

			if (expected_ && DelayACK) {
				if (ACKPending == null) {
					ACKPending = new TM_EVT(DELAY_ACK, getTime() + delayTimer,
									pkt_.getTS());
					setTimeoutAt(ACKPending, ACKPending.timeout);
				}
			}
			else // ack right away
				ack_syn_fin(SACK && !expected_ && !duplicate_, pkt_.getTS());
		} // end synchronized(rbuffer)
	}

	/** Processes TCP options in the packet header. */
	protected void option_process(TCPPacket pkt_)
	{
	}

	/** Sends an acknowledgement packet. */
	protected void ack_syn_fin(boolean doSACK_, double aTS_)
	{
		TCPPacket pkt_;
		if (doSACK_)
			pkt_ = SACKHdr(aTS_); // with SACK option 
		else {
			pkt_ = new TCPPacket(connection.getLocalPort(), 
				connection.getRemotePort(),
				-1/*seqno*/, rcv_nxt/*ackno*/, getAvailableReceivingBuffers(),
				true/*ack*/, false/*syn*/, false/*fin*/, -1.0/*TS*/, aTS_,
				NS_COMPATIBLE? 20: 30, 0, null);
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE)
									|| isDebugEnabledAt(DEBUG_SEND)))
				debug("SEND ACK: " + (pkt_.getAckNo()/MSS)
								+ "/" + pkt_.getAckNo());
		}

		// defined in Protocol.java  
		// void forward(PacketBody p_, long src_, long dest_, int dest_ulp_,
		// boolean routerAlert_, int TTL, int ToS): route-lookup forwarding 
		forward(pkt_, getLocalAddr(), peer, false, TTL, 0);
	}

	// expected_: the sequence # is expected to received?
	private void SACK_process(long seq_, int size_, boolean expected_)
	{
		// the new block is either alone or triggers merging of several 
		// previous blocks
		LongInterval new_ = expected_? null:
				new LongInterval(seq_, seq_ + size_);

		if (llSackBlock == null)
			llSackBlock = new LinkedList();

		for (ListIterator li_ = llSackBlock.listIterator(0); li_.hasNext(); ) {
			LongInterval interval_ = (LongInterval)li_.next();
			if (receivedSeq.strictlyContains(interval_.start, interval_.end)) {
				// this block is merged
				if (new_ != null) {
					if (interval_.start < new_.start)
						new_.start = interval_.start;
					else if (interval_.end > new_.end)
						new_.end = interval_.end;
				}
				li_.remove();
			}
		}

		if (new_ != null)
			llSackBlock.addFirst(new_);

		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) 
			&& llSackBlock != null && llSackBlock.size() > 0
			|| isDebugEnabledAt(DEBUG_SACK))) {
			String s_ = sackToString();
			debug("SACK_BLOCK_CHANGE: Triggering seq=" + (seq_/MSS) + "/" 
				+ seq_ + ", blocks="
				+ (s_ == null || s_.length() == 0? "none": s_));
		}
	}
	
	/** Makes a TCP header with SACK option. */
	private TCPPacket SACKHdr(double aTS_)
	{
		TCPPacket pkt_;
		int len_ = llSackBlock == null? 0: llSackBlock.size();

		if (len_ > 3)
			len_ = 3;
			// TCP option field can hold up to 3 SACK blocks with timestamp
			// option
		if (len_ > 0) {
			long[] REblk_ = new long[len_];
			long[] LEblk_ = new long[len_];
			int i = 0;
			for (ListIterator li_ = llSackBlock.listIterator(0);
				li_.hasNext(); ) {
				LongInterval interval_ = (LongInterval)li_.next();
				LEblk_[i] = interval_.start;
				REblk_[i] = interval_.end;
				if (++i == len_) break;
			}

			// header size = 20 bytes + 10 bytes of timestamp option + 2*8n 
			// bytes of sack option
			// n is # of blocks in sack option, n <= 3
			pkt_ = new TCPPacket(connection.getLocalPort(),
				connection.getRemotePort(),
				-1L/*seqno*/, rcv_nxt/*ackno*/, getAvailableReceivingBuffers(),
				true/*ack*/, false/*syn*/, false/*fin*/, -1.0/*TS*/, aTS_,
				true, len_, 30+2+8*len_, 0, null);
			pkt_.setSACKBlocks(LEblk_, REblk_);
		}
		else {
			// header size = 20 bytes + 10 bytes of timestamp option
			pkt_ = new TCPPacket(connection.getLocalPort(), 
				connection.getRemotePort(),
				-1L/*seqno*/, rcv_nxt/*ackno*/, getAvailableReceivingBuffers(),
				true/*ack*/, false/*syn*/, false/*fin*/, -1.0/*TS*/, aTS_,
				NS_COMPATIBLE? 20: 30, 0, null);
		}
		return pkt_;
	}

	void setConnection(Connection conn_)
	{ connection = conn_; }

	public int getLocalPort()
	{ return 0; }
	
	public int getRemotePort()
	{ return 0; }

	public long getLocalAddr()
	{ return drcl.net.Address.NULL_ADDR; }

	public long getPeer()
	{ return peer; }
	
	public String info()
	{
		String sb_ = null;
		if (SACK && llSackBlock != null)
			sb_ = sackToString();
		return "   State = " + STATES[state] + "\n"
		     + "    Peer = " + peer + "\n"
		     + "rcv buffer size = " + getAvailableReceivingBuffers() + "/"
			 	+ RBUFFER_SIZE + "\n"
			 + "    SACK = " + SACK + "\n"
			 + "DelayACK = " + DelayACK + (DelayACK? ", delay = "
			 	+ delayTimer + ", pending:"
				+ (ACKPending == null? "none": ""+ACKPending.timeout): "") 
			 	+ "\n"
			 + "receive_next = " + rcv_nxt/MSS + "---" + rcv_nxt + "\n"
			 + "   send_next = " + snd_nxt/MSS + "---" + snd_nxt + "\n"
			 + "  rcv buffer = " + printBuffer(true) + "---" 
			 	+ printBuffer(false) + "\n"
			 + " receivedSeq = " + receivedSeq + "\n"
			 + (SACK?  " SACK_blocks = " + (sb_ == null || sb_.length() == 0?
									 "none": sb_) + "\n": "");
	}

	String sackToString()
	{
		StringBuffer sb_ = new StringBuffer();
		if (llSackBlock != null) {
			for (ListIterator li_ = llSackBlock.listIterator(0);
				li_.hasNext(); ) {
				LongInterval interval_ = (LongInterval)li_.next();
				sb_.append("[" + (interval_.start/MSS) + "/" + interval_.start
					+ "," + (interval_.end/MSS) + "/" + interval_.end + ")");
			}
		}
		return sb_.toString();
	}

	String printBuffer(boolean mss_)
	{
		StringBuffer sb_ = new StringBuffer();
		long last_ = -1;
		for (int i=0; i<rbuffer.size(); i++) {
			TCPPacket tmp_ = (TCPPacket)rbuffer.elementAt(i);
			if (last_ < tmp_.getSeqNo()) {
				if (mss_) {
					if (last_ >= 0) sb_.append(last_/MSS + ")");
					sb_.append("(" + tmp_.getSeqNo()/MSS + ",");
				}
				else {
					if (last_ >= 0) sb_.append(last_ + ")");
					sb_.append("(" + tmp_.getSeqNo() + ",");
				}
			}
			last_ = tmp_.getSeqNo() + tmp_.size - tmp_.headerSize;
		}
		if (last_ >= 0) {
			if (mss_) sb_.append(last_/MSS + ")");
			else sb_.append(last_ + ")");
		}
		if (sb_.length() == 0) return "()";
		return sb_.toString();
	}

	class TM_EVT
	{
		int type;
		double timeout;
		double aTS;
		
		TM_EVT(int type_, double timeout_, double aTS_)
		{ type = type_; timeout = timeout_; aTS=aTS_; }

		public String toString()
		{ return TIMEOUT_TYPES[type]; }
	}
}
