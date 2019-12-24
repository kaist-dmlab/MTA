// @(#)TCPb.java   1/2004
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

package drcl.inet.transport;

import java.util.Vector;
import drcl.data.DoubleObj;
import drcl.comp.*;
import drcl.util.scalar.*;
import drcl.util.CircularBuffer;
import drcl.comp.lib.bytestream.ByteStreamContract;
import drcl.inet.InetPacket;
import drcl.net.Address;

/**
Bi-directional (Single-session) TCP Protocol.
 
This class implements both the TCP sender and receiver.
Basically it is a result of carefully merging {@link TCP} and {@link TCPSink}.

<p>In the current implementation, ACKs are not piggy-backed in data segments.
This will be fixed shortly.
  
@version 1.0, 7/2001
 */
public class TCPb extends TCP
{ 
	/** Sets to true to make TCP ns compatible */
	public static boolean NS_COMPATIBLE = false;

	static {
		setContract(TCPb.class, "*@" + drcl.net.Module.PortGroup_UP,
			new drcl.comp.lib.bytestream.ByteStreamContract(
				Contract.Role_REACTOR));
	}

	public String getName()
	{ return "tcp"; }
	
	// seq# of first byte of each received packet
	TCPSink tcpsink = new TCPSink("rcv") {
		// make sure the sending and receiving side see the same local addr
		public long getLocalAddr()
		{ return TCPb.this.getLocalAddr(); }
	};

	{
		addComponent(tcpsink);
		tcpsink.seqNoPort.connectTo(addEventPort(SEQNO_RCV_PORT_ID));
		tcpsink.upPort = upPort;
		tcpsink.downPort = downPort;
		tcpsink.setConnection(this);
	}

	public TCPb()
	{ super(); }
	
	public TCPb(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		tcpsink.reset();
	}
	
	public void duplicate(Object source_) 
	{ 
		super.duplicate(source_);
	}

	public void setDownPort(Port downPort_)
	{ downPort = tcpsink.downPort = downPort_; }

	public void setTTL(int ttl)
	{ super.setTTL(ttl); tcpsink.setTTL(ttl); }
	
	public void setMSS(int mss)
	{ super.setMSS(mss); tcpsink.setMSS(mss); }
	
	public void setReceivingBuffers(int awnd_)
	{ tcpsink.setReceivingBuffers(awnd_); }
	
	public int getReceivingBuffers()
	{ return tcpsink.getReceivingBuffers();	}
	
	public int getAvailableReceivingBuffers()
	{ return tcpsink.getAvailableReceivingBuffers();	}
	
	public void setSackEnabled(boolean sack_)
	{ super.setSackEnabled(sack_); tcpsink.setSackEnabled(sack_); }

	public void setDelayACKEnabled(boolean delayack_)
	{ tcpsink.setDelayACKEnabled(delayack_); }

	public boolean isDelayACKEnabled()
	{ return tcpsink.isDelayACKEnabled(); }

	public void setDelayACKTimeout(double v_)
	{ tcpsink.setDelayACKTimeout(v_); }

	public double getDelayACKTimeout()
	{ return tcpsink.getDelayACKTimeout(); }

	public void setPeer(long peer_)
	{ super.setPeer(peer_); tcpsink.peer = peer_; }
	
	protected void dataArriveAtUpPort(Object data_, Port upPort_) 
	{
		try {
			if (upPort_ == null ||
							((ByteStreamContract.Message)data_).isReport())
				tcpsink.dataArriveAtUpPort(data_, upPort_);
			else
				super.dataArriveAtUpPort(data_, upPort_);
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
			TCPPacket tcppkt_ = (TCPPacket)((InetPacket)data_).getBody();
			if (tcppkt_.isACK())
				recv(tcppkt_);
			if (tcppkt_.getSeqNo() >=0)
				tcpsink.recv(tcppkt_);
		}
		catch (Exception e_) {
			error(data_, "dataArriveAtDownPort()", downPort_,
							"unrecognized data: " + e_);
		}
	}
	
	// XXX:
	protected int getAckNo()
	{ return 0; }
	
	// XXX:
	protected int getAvailableRcvBuffer()
	{ return 0; }


	public String info()
	{
		return super.info() + "\nReceiving side:\n" + tcpsink.info();
	}
}
