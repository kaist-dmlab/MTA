// @(#)InterfaceInfo.java   1/2004
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

package drcl.inet.data;

import drcl.net.Address;

/**
 * Defines the information of an interface of a network node.
 * The information includes the network address ({@link drcl.inet.data.NetAddress})
 * of the node seen from that interface, the network addresses of
 * the neighbor nodes that can be reached from that interface, the maximum
 * transmission unit (MTU), the bandwidth, and the interface type.
 * The interface type is not defined in INET, and should be interpreted by
 * the upper layer protocols.  Since this information is shared by multiple
 * protocols, they must have consensus of its definition.
 * 
 * <p>This class provides a set of methods to augment ({@link #addPeerNetAddress})
 * and remove ({@link #removePeerNetAddress}) a neighbor.  It also provides methods
 * to retrieve the local network address and the neighbor network addresses, and
 * to query if a network address is one of the neighbors that can be reached from
 * that interface.
 * 
 * <p>The information of all the interfaces is stored and maintained
 * at the core service layer in the Inet architecture, in which
 * the protocols make use of the <code>IFQuery</code>
 * contract to access the information from the core service layer.
 * 
 * @author Hung-ying Tyan
 * @version 1.0, 4/22/2001
 * @see drcl.inet.data.NetAddress
 * @see drcl.inet.contract.IFQuery
 * @see drcl.inet.CoreServiceLayer
 */
public class InterfaceInfo extends drcl.DrclObj implements drcl.inet.InetConstants
{
	public NetAddress local;
	public NetAddress[] peers;
	double[] timeouts; // elements correspond to the ones in peers
	int mtu = DEFAULT_MTU;
	//int bufferSize = DEFAULT_BUFFER_SIZE;
	//double bandwidth = DEFAULT_BANDWIDTH;
	int bufferSize = -1;
	double bandwidth = -1;
	int type;
	
	public InterfaceInfo (long localAddr_, long mask_)
	{
		local = new NetAddress(localAddr_, mask_);
	}
	
	public InterfaceInfo (NetAddress local_)
	{
		local = local_;
	}
	
	public InterfaceInfo (int type_)
	{
		type = type_;
	}
	
	public InterfaceInfo (NetAddress local_, NetAddress[] peer_,
									   double[] timeout_)
	{
		local = local_;
		peers = peer_;
		timeouts = timeout_;
	}
	
	public InterfaceInfo (NetAddress local_, NetAddress peer_,
									   double timeout_)
	{
		local = local_;
		peers = new NetAddress[]{peer_};
		timeouts = new double[]{timeout_};
	}
	
	/**
	 * @param mtu_ set to the default value if a negative value is given.
	 * @param bw_ set to the default value if a negative value is given.
	 * @param bufferSize_ set to the default value if a negative value is given.
	 * @see #DEFAULT_MTU
	 * @see #DEFAULT_BUFFER_SIZE
	 * @see #DEFAULT_BANDWIDTH
	 */
	public InterfaceInfo (int mtu_, double bw_, int bufferSize_)
	{
		mtu = mtu_ < 0? DEFAULT_MTU: mtu_;
		bandwidth = bw_ < 0.0? DEFAULT_BANDWIDTH: bw_;
		bufferSize = bufferSize_ < 0? DEFAULT_BUFFER_SIZE: bufferSize_;
	}

	/**
	 * @param mtu_ set to the default value if a negative value is given.
	 * @param bw_ set to the default value if a negative value is given.
	 * @param bufferSize_ set to the default value if a negative value is given.
	 * @see #DEFAULT_MTU
	 * @see #DEFAULT_BUFFER_SIZE
	 * @see #DEFAULT_BANDWIDTH
	 */
	public InterfaceInfo (long localAddress_, long localAddressMask_,
									   int mtu_, double bw_, int bufferSize_)
	{
		local = new NetAddress(localAddress_, localAddressMask_);
	}
	
	/**
	 * @param mtu_ set to the default value if a negative value is given.
	 * @param bw_ set to the default value if a negative value is given.
	 * @param bufferSize_ set to the default value if a negative value is given.
	 * @see #DEFAULT_MTU
	 * @see #DEFAULT_BUFFER_SIZE
	 * @see #DEFAULT_BANDWIDTH
	 */
	public InterfaceInfo (int type_, long localAddress_, long localAddressMask_,
									   int mtu_, double bw_, int bufferSize_)
	{
		type = type_;
		local = new NetAddress(localAddress_, localAddressMask_);
	}
	
	public InterfaceInfo()
	{ super(); }
	
	public void duplicate(Object source_)
	{
		InterfaceInfo that_ = (InterfaceInfo)source_;
		local = (NetAddress)drcl.util.ObjectUtil.clone(that_.local);
		peers = (NetAddress[])drcl.util.ObjectUtil.clone(that_.peers);
		timeouts = (double[])drcl.util.ObjectUtil.clone(that_.timeouts);
	}
	
	public NetAddress getLocalNetAddress()
	{ return local; }
	
	public void setLocalNetAddress(NetAddress addr_)
	{ local = addr_; }
	
	public NetAddress[] getPeerNetAddresses()
	{ return peers; }
	
	public void addPeerNetAddress(NetAddress addr_, double timeout_)
	{
		NetAddress[] tmp_ = new NetAddress[peers == null? 1: peers.length+1];
		double[] ttmp_ = new double[tmp_.length];
		if (peers != null) {
			System.arraycopy(peers, 0, tmp_, 0, peers.length);
			System.arraycopy(timeouts, 0, ttmp_, 0, peers.length);
		}
		tmp_[tmp_.length-1] = addr_;
		ttmp_[tmp_.length-1] = timeout_;
		peers = tmp_;
		timeouts = ttmp_;
	}
	
	public void removePeerNetAddress(NetAddress addr_)
	{
		int where_ = findPeer(addr_);
		if (where_ < 0) return;
		NetAddress[] tmp_ = new NetAddress[peers.length-1];
		double[] ttmp_ = new double[tmp_.length];
		System.arraycopy(peers, 0, tmp_, 0, where_);
		System.arraycopy(timeouts, 0, ttmp_, 0, where_);
		if (where_ < peers.length-1) {
			System.arraycopy(peers, where_+1, tmp_, where_, peers.length - where_ - 1);
			System.arraycopy(timeouts, where_+1, ttmp_, where_, peers.length - where_ - 1);
		}
		peers = tmp_;
		timeouts = ttmp_;
	}
	
	int findPeer(NetAddress peer_)
	{
		if (peers == null) return -1;
		for (int i=0; i<peers.length; i++) {
			if (peers[i].equals(peer_)) return i;
		}
		return -1;
	}
	
	public double[] getPeerTimeouts()
	{ return timeouts; }
	
	public void setPeerNetAddresses(NetAddress[] addrs_)
	{
		peers = addrs_;
		if (peers == null) {
			timeouts = null;
		}
		else {
			timeouts = new double[peers.length];
			for (int i=0; i<timeouts.length; i++) timeouts[i] = Double.NaN;
		}
	}
	
	public boolean containsPeer(NetAddress addr_)
	{
		if (peers == null) return false;
		for (int i=0; i<peers.length; i++)
			if (peers[i] == null) System.out.println("OOPS...");
			else if (peers[i].equals(addr_)) return true;
		return false;
	}
	
	public boolean containsPeer(long addr_)
	{
		if (peers == null) return false;
		for (int i=0; i<peers.length; i++)
			if (peers[i].getAddress() == addr_) return true;
		return false;
	}
	
	public void setTimeout(NetAddress addr_, double timeout_)
	{
		int where_ = findPeer(addr_);
		if (where_ >= 0) timeouts[where_] = timeout_;
	}
	
	public void resetTimeout()
	{
		if (timeouts != null)
			for (int i=0; i<timeouts.length; i++)
				timeouts[i] = Double.NaN;
	}
	
	public int getMTU()
	{ return mtu; }
	
	public void setMTU(int mtu_)
	{ mtu = mtu_; }
	
	public double getBandwidth()
	{ return bandwidth; }
	
	public void setBandwidth(double bw_)
	{ bandwidth = bw_; }
	
	public int getBufferSize()
	{ return bufferSize; }
	
	public void setBufferSize(int bs_)
	{ bufferSize = bs_; }

	/** Returns the interface type. */
	public int getType()
	{ return type; }

	/** Sets the interface type. */
	public void setType(int type_)
	{ type = type_; }
	
	public String toString()
	{
		return "type:" + type + ", local:" + local
			   + ", peers:" + drcl.util.StringUtil.toString(peers)
			   + ", timeouts:" + drcl.util.StringUtil.toString(timeouts)
			   + ", MTU="+ mtu + ", BW=" + bandwidth + ", buffer=" + bufferSize;
	}

	public String print(Address addr_)
	{
		return "type:" + type
			+ ", local:" + (local == null? "<null>": local.print(addr_))
			+ ", peers:" + _printPeers(addr_)
			+ ", timeouts:" + drcl.util.StringUtil.toString(timeouts)
			+ ", MTU="+ mtu + ", BW=" + bandwidth + ", buffer=" + bufferSize;
	}

	String _printPeers(Address addr_)
	{
		if (peers == null || peers.length == 0) return "<null>";
		StringBuffer sb = new StringBuffer(peers[0].print(addr_));
		for (int i=1; i<peers.length; i++)
			sb.append("," + peers[i].print(addr_));
		return "(" + sb + ")";
	}
}
