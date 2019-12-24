// @(#)InetPacket.java   1/2004
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

package drcl.inet;

import drcl.net.Address;
import drcl.net.Packet;
import drcl.util.StringUtil;
import drcl.util.ObjectUtil;

/**
Defines the packet structure for the INET framework.
@version 1.1, 08/2002
 */
public class InetPacket extends Packet
{
	/** Name of the InetPacket.*/
	public static final String NAME    = "INET";

	// ToS
	/** Packet type bit mask in the ToS field. */
	public static final long TYPE_MASK    = 0x01;
	/** Data packet type. */
	public static final long DATA    = 0x00;
	/** Control packet type. */
	public static final long CONTROL = 0x01;
	/** ECT (ECN-Capable Transport) bit mask in the ToS field. */
	public static final long ECT     = 0x02;
	/** CE (Congestion Experience) bit mask in the ToS field. */
	public static final long CE      = 0x04;
	
	// flags
	/** The ``don't fragment'' bit mask in the flag field. */
	public static final int DONT_FRAGMENT = 0x01 << 16;
	/** The ``more fragment'' bit mask in the flag field. */
	public static final int MORE_FRAGMENT = 0x02 << 16;
	/** The ``packet-in-packet'' bit mask in the flag field. */
	public static final int PACKET_IN_PACKET = 0x04 << 16;
	/** The ``labelled'' bit mask in the flag field. */
	public static final int LABELLED = 0x08 << 16;
	
	static String[] FLAGS = {null, "DF", "MF", "ERROR!",
							 "PIP", "DF_PIP", "MF_PIP", "ERROR!"};

	long src, dest, tos;
	// flag: 16 MSB for flag bits, 16 LSB for label
	int ttl, hops, ulp, id, flag, fragmentOffset;
	boolean routerAlert;
	Object extension;
					
	public InetPacket()
	{ this(0, 0, 0, 0, 0, false, 0, 0, 0, 0, null, 0); }
	
	public InetPacket(long src_, long des_, int ulp_, int ttl_, int hops_,
		boolean ra_, long tos_, int id_, int flag_, int fragment_, Object pkt_,
		int pktsize_)
	{
		super(20/*header size*/, pktsize_, pkt_);
		
		src = src_;
		dest = des_;
		ulp = ulp_;
		ttl = ttl_;
		hops = hops_;
		tos = tos_;
		routerAlert = ra_;
		id = id_;
		flag = flag_;
		fragmentOffset = fragment_;
	}
	
	public InetPacket(long src_, long des_, int ulp_, int ttl_, int hops_,
		boolean ra_, long tos_, int id_, int flag_, int fragment_, Object pkt_,
		int pktsize_, long nexthop_)
	{
		super(20/*header size*/, pktsize_, pkt_);
		
		src = src_;
		dest = des_;
		ulp = ulp_;
		ttl = ttl_;
		hops = hops_;
		tos = tos_;
		routerAlert = ra_;
		id = id_;
		flag = flag_;
		fragmentOffset = fragment_;
		nexthop = nexthop_;
	}
	
	private InetPacket(long src_, long des_, int ulp_, int ttl_, int hops_,
		boolean ra_, long tos_, int id_, int flag_, int fragment_, Object ext_,
		int headerSize_, Object pkt_, int pktsize_, long nexthop_)
	{
		super(headerSize_, pktsize_, pkt_);
		
		src = src_;
		dest = des_;
		ulp = ulp_;
		ttl = ttl_;
		hops = hops_;
		tos = tos_;
		routerAlert = ra_;
		id = id_;
		flag = flag_;
		fragmentOffset = fragment_;
		extension = ext_;
		nexthop = nexthop_;
	}
	
	public void setSource(long src_)
	{ src = src_; }
	
	public long getSource()
	{ return src; }
	
	public void setDestination(long des)
	{ dest = des;	}
	
	public long getDestination()
	{ return dest; }
	
	public void setProtocol(int ulp_)
	{ ulp = ulp_; }
	
	public int getProtocol()
	{ return ulp; }
	
	public void setTTL(int ttl_)
	{ ttl = ttl_; }

	public int getTTL()
	{ return ttl; }
	
	public void setHops(int hops_)
	{ hops = hops_; }
	
	public int getHops()
	{ return hops; }
	
	public void setRouterAlertEnabled(boolean ra)
	{ routerAlert = ra; }

	public boolean isRouterAlertEnabled()
	{ return routerAlert; }
	
	public void setTOS(long tos_)
	{ tos = tos_; }
	
	public long getTOS()
	{ return tos; }
	
	public void setID(int no_)
	{ id = no_; }
	
	public int getID()
	{ return id; }
	
	public void setFlag(int flag_)
	{ flag = (flag_ << 16) | (flag & 0x0FFFF); }
	
	public int getFlag()
	{ return flag >> 16; }
	
	/** One shot to set all the fragment parameters. */
	public void setFragmentParam(boolean more_, int offset_, int id_)
	{
		if (more_) flag |= MORE_FRAGMENT;
		else flag &= ~MORE_FRAGMENT;
		fragmentOffset = offset_;
		id = id_;
	}

	public void setFragmentOffset(int offset_)
	{ fragmentOffset = offset_; }
	
	public int getFragmentOffset()
	{ return fragmentOffset; }

	public boolean dontFragment()
	{	return (flag & DONT_FRAGMENT) > 0;	}
	
	public void setDontFragment(boolean value_)
	{
		if (value_) flag |= DONT_FRAGMENT;
		else flag &= ~DONT_FRAGMENT;
	}
	
	public boolean hasMoreFragment()
	{	return (flag & MORE_FRAGMENT) > 0;	}
	
	public boolean isFragment()
	{	return (flag & MORE_FRAGMENT) > 0 || fragmentOffset > 0; }

	public void setMoreFragment(boolean value_)
	{
		if (value_) flag |= MORE_FRAGMENT;
		else flag &= ~MORE_FRAGMENT;
	}
	
	public boolean isPacketInPacket()
	{	return (flag & PACKET_IN_PACKET) > 0;	}
	
	public void setPacketInPacket(boolean value_)
	{
		if (value_) flag |= PACKET_IN_PACKET;
		else flag &= ~PACKET_IN_PACKET;
	}

	public boolean isLabelled()
	{	return (flag & LABELLED) > 0;	}
	
	public void setLabelled(boolean value_)
	{
		if (value_) flag |= LABELLED;
		else flag &= ~LABELLED;
	}

	public short getLabel()
	{ return (short)(flag & 0x0FFFF); }

	public void setLabel(short label_)
	{ flag = (flag & 0xFFFF0000) | ((int)label_ & 0x0FFFF); }

	public Object getExtension()
	{ return extension; }
	
	public void setExtension(Object o_)
	{ extension = o_; }
	
	public boolean isDataPacket()
	{ return (tos & 0x01) == DATA; }
	
	public boolean isControlPacket()
	{ return (tos & 0x01) == CONTROL; }
	
	public boolean isECT()
	{ return (tos & ECT) > 0; }
	
	public boolean isCE()
	{ return (tos & CE) > 0; }
	
	public void setECT(boolean enabled_)
	{ setTOSBit(ECT, enabled_); }

	public void setCE(boolean enabled_)
	{ setTOSBit(CE, enabled_); }

	public void setTOSBit(long bitmask_, boolean enabled_)
	{
		if (enabled_) tos |= bitmask_;
		else tos &= ~bitmask_;
	}

	public int getIncomingIf()
	{ return ulp; }

	public void setIncomingIf(int if_)
	{ ulp = if_; }
	
	////////////////////////////////////////////////////////////////////////
	// added by Will
	long nexthop = drcl.net.Address.NULL_ADDR;

	public void setNextHop(long nexthop_)
	{ nexthop = nexthop_; }
	
	public long getNextHop()
	{ return nexthop; }
	////////////////////////////////////////////////////////////////////////		
	/*
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		InetPacket that_ = (InetPacket)source_;
		src = that_.src;
		dest = that_.dest;
		tos = that_.tos;
		ulp = that_.ulp;
		ttl = that_.ttl;
		hops = that_.hops;
		routerAlert = that_.routerAlert;
		id = that_.id;
		flag = that_.flag;
		fragmentOffset = that_.fragmentOffset;
		
        // added by Will
        nexthop = that_.getNextHop();
		
		extension = ObjectUtil.clone(that_.extension);
	}
	*/

	public boolean equals(Object o_)
	{
		if (this == o_) return true;
		if (!(o_ instanceof InetPacket)) return false;
		InetPacket that_ = (InetPacket) o_;
		// NOTE: ulp is also used to encode incoming if, not compared here
		// the chance that two different packets with all other fields are
		// equal should be none
		return size == that_.size && headerSize == that_.headerSize
			&& ObjectUtil.equals(body, that_.body)
			&& src == that_.src && dest == that_.dest && tos == that_.tos
			// && ulp == that_.ulp
			//&& ttl == that_.ttl && hops == that_.hops
			&& routerAlert == that_.routerAlert
			&& id == that_.id && flag == that_.flag
			&& fragmentOffset == that_.fragmentOffset
			&& ObjectUtil.equals(extension, that_.extension);
	}
	
	public Object clone()
	{
		return new InetPacket(src, dest, ulp, ttl, hops, routerAlert,
			tos, id, flag, fragmentOffset, ObjectUtil.clone(extension),
			headerSize,
			ObjectUtil.clone(body), size-headerSize, nexthop);
	}

	/** Returns a clone of this packet.
	  * The difference between this method and {@link #clone()}
	  * is that this method only copies the reference of the encapsulated
	  * object rather than the object content. */
	public InetPacket _clone()
	{
		return new InetPacket(src, dest, ulp, ttl, hops, routerAlert,
			tos, id, flag, fragmentOffset, extension, headerSize,
			body, size-headerSize, nexthop);
	}

	String _addr(long addr_)
	{ return Address._ltos(addr_); }

	public String getName()
	{ return NAME; }

	public String _toString(String separator_)
	{
		String s_ = "src:" + _addr(src) + separator_ + "dest:" + _addr(dest)
			+ separator_
			+ "prot:" + ulp + separator_ + "TTL:" + hops + "/" + ttl
			+ (routerAlert? separator_ + "ROUTER_ALERT": "")
			+ separator_ + "ToS:#"
			+ StringUtil.toHex(tos, true/*skip leading zeros*/)
			+ separator_ + "label:" + (flag & 0x0FFFF)
			+ (FLAGS[flag >> 16] == null? "": separator_ + FLAGS[flag >> 16])
			+ (!dontFragment() && isFragment()?
			   separator_ + "fragment:id" + id + ",offset" + fragmentOffset: "")
			+ (extension == null? "": separator_ + "ext:"
							+ StringUtil.toString(extension));
		if (Address._isNull(nexthop))
			return s_;
		else
			return s_ + separator_ + "nexthop:" + _addr(nexthop);
	}
}
