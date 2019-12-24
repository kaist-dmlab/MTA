// @(#)TCPPacket.java   1/2004
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

import drcl.net.Packet;
import drcl.util.StringUtil;

/**
This class defines the TCP packet header.
Fields implemented in this class are
SOURCE_port, DEST_port, SEQNo, ACKNo, AdvertisedWnd, ACK, SYN, FIN, SACK option,
timestamp option.

RFC requirements NOT implemented in this class are
URG, PSH, RST, TCP checksum, Urgent pointer, and other options

References:
<ul>
<li>[TCPILL1] W. Stevens, TCP/IP Illustrated vol.1: The Protocols, Addison-Wesley,1994.
<li>[TCPILL2] G. Wright and W. Stevens, TCP/IP Illustrated vol.2: The Implementation, Addison-Wesley,1995
<li>[RFC793] J. Postel, Transmission Control Protocol, September 1981.
<li>[RFC2018] M. Mathis, J. Mahdavi, S. Floyd and A. Romanow, TCP Selective Acknowledgment Options, Octobor 1996.
<li>[RFC2581] M. Allman, V. Paxson and W. Stevens, TCP Congestion Control, April 1999.
</ul>
 */
public class TCPPacket extends Packet
{
	/** Size of maximum transmission unit, used to set/calculate packet count from sequence number.*/
	public static long MSS = 512;
	public static final int FLAG_ACK = 1;
	public static final int FLAG_SYN = 2;
	public static final int FLAG_FIN = 4;
	public static final int FLAG_SACK = 8;

	// standard fields
	private int sport;
	private int dport;
	private	long SeqNo;		/* sequence number */
	private	long AckNo;		/* ACK number for FullTcp */
	private	int AdvWin;	/* Advertised Window */
	
	// flags
	int flag;

	// options
	private	double TS;		/* time packet generated (at source) */
	private double aTS;		/* time packet generated (at sink) */
	private int sackLen;
	private long[] LEblk, REblk; // Left and right edge of blocks

	public String getName()
	{ return "TCP"; }

	public TCPPacket()
	{ super(20); }

	/**
	 * Constructor for TCPPacket without SACK option.
	 *
	 * @param Seqno_ Sequence number of data
	 * @param AckNo_ Acknowledge sequence number
	 * @param AdvWin_ Advertised window
	 * @param Ack_ flag ACK 
	 * @param SYN_ flag SYN
	 * @param FIN_ flag FIN
	 * @param ts_ Timestamp
	 * @param ats_ Ackknowledge Timestamp
	 * @param hsize_ header size, calculated by caller
	 * @param bsize_ body size
	 * @param body_ body 
	 */
	public TCPPacket (int sport_, int dport_, long Seqno_, long AckNo_, int AdvWin_,
		boolean Ack_, boolean SYN_, boolean FIN_, double ts_, double ats_,
		int hsize_, int bsize_, Object body_)
	{
		super(hsize_, bsize_, body_);
		sport = sport_;
		dport = dport_;
		SeqNo = Seqno_;
		AckNo = AckNo_;
		AdvWin = AdvWin_;
		setACK(Ack_);
		setSYN(SYN_);
		setFIN(FIN_);
		setSACK(false);
		TS = ts_;
		aTS = ats_;
	}

	/**
	 * Constructor for TCPPacket with SACK option.
	 * @param Seqno_ Sequence number of data
	 * @param AckNo_ Acknowledge sequence number
	 * @param AdvWin_ Advertised window
	 * @param Ack_ flag ACK 
	 * @param SYN_ flag SYN
	 * @param FIN_ flag FIN
	 * @param ts_ Timestamp
	 * @param ats_ Ackknowledge Timestamp
	 * @param sack_ flag SACK 
	 * @param sackLen_ # of SACK blocks 
	 * @param hsize_ header size, calculated by caller
	 * @param bsize_ body size
	 * @param body_ body 
	 */
	public TCPPacket (int sport_, int dport_, long Seqno_, long AckNo_, int AdvWin_,
		boolean Ack_, boolean SYN_, boolean FIN_, double ts_, double ats_,
		boolean sack_, int sackLen_, int hsize_, int bsize_, Object body_)
	{
		super(hsize_);
		sport = sport_;
		dport = dport_;
		SeqNo = Seqno_;
		AckNo = AckNo_;
		AdvWin = AdvWin_;
		setACK(Ack_);
		setSYN(SYN_);
		setFIN(FIN_);
		setSACK(sack_);
		TS = ts_;
		aTS = ats_;
		if (sack_) {
			sackLen = sackLen_;
			REblk = new long[sackLen_];
			LEblk = new long[sackLen_];
		}
	}

	private TCPPacket (int sport_, int dport_, long Seqno_, int AdvWin_, int flag_, long AckNo_,
		double ts_, double ats_, int sackLen_, long[] LEblk_, long[] REblk_,
		int hsize_, int bsize_, Object body_)
	{
		super(hsize_, bsize_, body_);
		sport = sport_;
		dport = dport_;
		SeqNo = Seqno_;
		AckNo = AckNo_;
		AdvWin = AdvWin_;
		flag = flag_;
		TS = ts_;
		aTS = ats_;
		sackLen = sackLen_;
		LEblk = LEblk_;
		REblk = REblk_;
	}

	public int getPacketCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getPacketCount();
		else
			return (int)(SeqNo/MSS);
	}
	
	public long getByteCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getByteCount();
		else
			return SeqNo;
	}

	public int getDPort()
	{ return dport; }

	public int getSPort()
	{ return sport; }
	
	public double getTS()
	{ return TS; }

	public void setTS(double ts_)
	{TS = ts_; }

	public double getaTS()
	{ return (aTS); }

	public void setaTS(double aTS_)
	{ aTS=aTS_; }

	public void setSeqNo(long seqno_)
	{ SeqNo = seqno_; }

	public long getSeqNo()
	{ return SeqNo; }

	public long getAckNo()
	{ return AckNo; }
	
	public int getAdvWin()
	{ return AdvWin; }

	public void setACK(boolean enabled_)
	{
		if (enabled_) flag |= FLAG_ACK;
		else flag &= ~FLAG_ACK;
	}

	public boolean isACK()
	{ return (flag & FLAG_ACK) != 0;}

	public void setSYN(boolean enabled_)
	{
		if (enabled_) flag |= FLAG_SYN;
		else flag &= ~FLAG_SYN;
	}

	public boolean isSYN()
	{ return (flag & FLAG_SYN) != 0;}

	public void setFIN(boolean enabled_)
	{
		if (enabled_) flag |= FLAG_FIN;
		else flag &= ~FLAG_FIN;
	}

	public boolean isFIN()
	{ return (flag & FLAG_FIN) != 0;}

	public void setSACK(boolean enabled_)
	{
		if (enabled_) flag |= FLAG_SACK;
		else flag &= ~FLAG_SACK;
	}

	public boolean isSACK()
	{ return (flag & FLAG_SACK) != 0;}

	public int getSACKLen()
	{ return sackLen; }

	public void setSACKLen (int len_)
	{ sackLen = len_; }

	public long[] getLEblk ()
	{ return LEblk; }

	public void setLEblk (long[] LEblk_)
	{ LEblk = LEblk_; }

	public long[] getREblk ()
	{ return REblk; }

	public void setREblk (long[] REblk_)
	{ REblk = REblk_; }

	public void setSACKBlocks (long[] LEblk_, long[] REblk_)
	{
		LEblk = LEblk_;
		REblk = REblk_;
	}

	public Object clone()
	{
		return new TCPPacket(sport, dport, SeqNo, AdvWin, flag, AckNo, TS, aTS,
			sackLen, LEblk, REblk, headerSize, size-headerSize,
			(body instanceof drcl.ObjectCloneable?
			 	((drcl.ObjectCloneable)body).clone(): body));
	}

	/*
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TCPPacket that_ = (TCPPacket)source_;
		sport = that_.sport;
		dport = that_.dport;
		TS = that_.TS;
		aTS = that_.aTS;
		SeqNo = that_.SeqNo;
		AckNo = that_.AckNo;
		AdvWin = that_.AdvWin;
		flag = that_.flag;
		sackLen = that_.sackLen;
		LEblk = that_.LEblk;
		REblk = that_.REblk;
	}
	*/

	public String getPacketType()
	{
		if (flag == 0) return "TCP";
		else if (isSYN())
			return isACK()? "TCP-SYN-ACK": "TCP-SYN";
		else if (isFIN())
			return isACK()? "TCP-FIN-ACK": "TCP-FIN";
		else if (size == headerSize)
			return isSACK()? "TCP-SACK": "TCP-ACK";
		else
			return isSACK()? "TCP/SACK": "TCP/ACK";
	}

	public String _toString(String separator_)
	{
		return "s:" + sport + separator_ + "d:" + dport + separator_ + "seq" + SeqNo
			+ separator_ + "AWND:" + AdvWin + separator_ + "TS" + TS
			+ (isSYN() || isFIN() || isACK()? separator_ + (isSYN()? "Syn": "") + (isFIN()? "Fin": "")
			+ (isACK()? "Ack:" + AckNo + "," + aTS: ""): "")
			+ (isSACK()? separator_ + "SACK" + sackLen + "," + StringUtil.toString(LEblk) + "-" +
				StringUtil.toString(REblk): "");
	}
}
