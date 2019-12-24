// @(#)PktSending.java   1/2004
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

package drcl.inet.contract;

import drcl.comp.*;
import drcl.net.Packet;
import drcl.inet.InetPacket;
import drcl.util.StringUtil;

/**
The packet sending contract.
This constract defines three packet sending services at the reactor:
<dl>
<dt><code>DefaultForwarding</code>
<dd>The initiator sends an {@link drcl.inet.InetPacket}.  Upon receipt of such
	a packet, the reactor should perform routing table lookup to determine the
	outgoing interface(s) at which the packet is sent.
<dt>Explicit Multicasting
<dd>The initiator sends a message consisting of:
    <ol>
    <li> an integer (<code>int</code>) of value that indicates which sending
	service should be applied to this sending,
    <li> the packet ({@link drcl.inet.InetPacket}) to be sent,
    <li> the indices of the interfaces (<code>int[]</code>).
    </ol>
	Upon receipt of such a request, the reactor sends the packet at the
	specified interfaces.
<dt><code>ExclusiveBroadcasting</code>
<dd>The initiator sends a message that has exactly the same format as a
	explicit-multicasting request.  Upon receipt of such a request, the
	reactor sends the packet at all interfaces except the ones specified.
</dl>
This class also provides a set of static methods
({@link #getForwardPack(drcl.net.Packet, long, long, boolean, int, long)
getForwardPack(...)},
{@link #getMcastPack(drcl.net.Packet, long, long, boolean, int, long, int[])
getMcastPack()s}
and {@link #getBcastPack(drcl.net.Packet, long, long, boolean, int, long, int)
getBcastPack(...)s}),
at least one for each sending service,
to faciliate constructing the sending packages at the initiator.
These methods are useful in implementing the protocols that are right above
the core service layer.

@author Hung-ying Tyan
@version 1.0, 05/2001
@see drcl.inet.CoreServiceLayer
*/
public class PktSending extends Contract
{
	public static final PktSending INSTANCE = new PktSending();

	public PktSending()
	{ super(); }
	
	public PktSending(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "PacketSending Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	/**
	 * Creates and returns a default-forwarding request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 */
	public static InetPacket getForwardPack(Packet pkt_, long src_, long dest_,
									boolean routerAlert_, int ttl_, long tos_)
	{
		return new InetPacket(src_, dest_, 0/*protocol*/, ttl_, 0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pkt_.size);
	}
	
	/**
	 * Creates and returns a default-forwarding request.
	 * 
	 * @param pkt_ the packet.
	 * @param pktsize_ the packet size.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 */
	public static InetPacket getForwardPack(Object pkt_, int pktsize_,
					long src_, long dest_, boolean routerAlert_, int ttl_,
					long tos_)
	{
		return new InetPacket(src_, dest_, 0/*protocol*/, ttl_, 0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pktsize_);
	}
	
	/**
	 * Creates and returns an explicit-multicasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param ifs_ indices of the interfaces to forward the packet on.
	 */
	public static Message getMcastPack(Packet pkt_, long src_, long dest_,
					boolean routerAlert_, int ttl_, long tos_, int[] ifs_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/, routerAlert_, tos_, 0/*id*/, 0/*flag*/,
				0/*frag offset*/, pkt_, pkt_.size);
		return new Message(MULTICAST, ipkt_, ifs_);
	}
	
	/**
	 * Creates and returns an explicit-multicasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param pktsize_ the packet size.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param ifs_ indices of the interfaces to forward the packet on.
	 */
	public static Message getMcastPack(Object pkt_, int pktsize_, long src_,
					long dest_, boolean routerAlert_, int ttl_, long tos_,
					int[] ifs_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pktsize_);
		return new Message(MULTICAST, ipkt_, ifs_);
	}
	
	///////////////////////////////////////////////////////////////////////
	// added by Will 06/25/2003
	// specify the next hop
	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ index of the interface to be excluded from
	 * 		forwarding of the packet.
	 * @param nexthop_ address of the next hop 
	 */
	public static Message getBcastPack(Packet pkt_, long src_, long dest_,
									  boolean routerAlert_, int ttl_,
									  long tos_, int excludedIf_, long nexthop_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pkt_.size, nexthop_);
		return new Message(BROADCAST, ipkt_, new int[]{excludedIf_});
	}
	
	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ indices of the interfaces to be excluded from
	 * 		forwarding of the packet.
	 * @param nexthop_ address of the next hop 
	 */
	public static Message getBcastPack(Packet pkt_, long src_, long dest_,
								  boolean routerAlert_, int ttl_,
								  long tos_, int[] excludedIfs_, long nexthop_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pkt_.size, nexthop_);
		return new Message(BROADCAST, ipkt_, excludedIfs_);
	}
	
	////////////////////////////////////////////////////////////////////

	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ index of the interface to be excluded from
	 * 		forwarding of the packet.
	 */
	public static Message getBcastPack(Packet pkt_, long src_, long dest_,
									  boolean routerAlert_, int ttl_,
									  long tos_, int excludedIf_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pkt_.size);
		return new Message(BROADCAST, ipkt_, new int[]{excludedIf_});
	}
	
	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param pktsize_ the packet size.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ index of the interface to be excluded from
	 * 		forwarding of the packet.
	 */
	public static Message getBcastPack(Object pkt_, int pktsize_, long src_,
					long dest_, boolean routerAlert_, int ttl_, long tos_,
					int excludedIf_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pktsize_);
		return new Message(BROADCAST, ipkt_, new int[]{excludedIf_});
	}
	
	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ indices of the interfaces to be excluded from
	 * 		forwarding of the packet.
	 */
	public static Message getBcastPack(Packet pkt_, long src_, long dest_,
									  boolean routerAlert_, int ttl_,
									  long tos_, int[] excludedIfs_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pkt_.size);
		return new Message(BROADCAST, ipkt_, excludedIfs_);
	}
	
	/**
	 * Creates and returns an exclusive-broadcasting request.
	 * 
	 * @param pkt_ the packet.
	 * @param pktsize_ the packet size.
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param routerAlert_ "router alert" flag.
	 * @param ttl_ "time-to-live".
	 * @param tos_ type of service.
	 * @param excludedIf_ indices of the interfaces to be excluded from
	 * 		forwarding of the packet.
	 */
	public static Message getBcastPack(Object pkt_, int pktsize_, long src_,
					long dest_, boolean routerAlert_, int ttl_, long tos_,
					int[] excludedIfs_)
	{
		InetPacket ipkt_ = new InetPacket(src_, dest_, 0/*protocol*/, ttl_,
				0/*hops*/,
				routerAlert_, tos_, 0/*id*/, 0/*flag*/, 0/*frag offset*/,
				pkt_, pktsize_);
		return new Message(BROADCAST, ipkt_, excludedIfs_);
	}
	
	
	// sending type
	public static final int MULTICAST = 0;
	public static final int BROADCAST = 1;
	static final String[] TYPES = {"explicit_mcast", "excluded_bcast"};
	
	public static class Message extends drcl.comp.Message
	{
		int type;
		InetPacket pkt;
		int[] ifs;
		
		public Message ()
		{}

		public Message (int type_, InetPacket pkt_, int[] ifs_)
		{
			type = type_;
			pkt = pkt_;
			ifs = ifs_;
		}

		public boolean isMulticast()
		{ return type == MULTICAST; }
	
		public boolean isBroadcast()
		{ return type == BROADCAST; }
	
		public InetPacket getInetPacket()
		{ return pkt; }
	
		public int[] getIfs()
		{ return ifs; }
	
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			pkt = that_.pkt == null? null: (InetPacket)that_.pkt.clone();
			ifs = that_.ifs == null? null: (int[])that_.ifs.clone();
		}
		*/
	
		public Object clone()
		{
			// the contract is between two components; dont clone pkt and ifs
			return new Message(type, pkt, ifs);
		}
	
		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "PKT_SENDING:" + TYPES[type]
				+ separator_ + drcl.util.StringUtil.toString(ifs)
				+ separator_ + pkt;
		}
	}
}




