// @(#)VIFPack.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.core;

import drcl.net.Address;

public class VIFPack extends drcl.DrclObj
{
	int vifStartIndex = Integer.MAX_VALUE;
	long[] peers; // offset at vifStartIndex
	long[] myself;
		
	public VIFPack ()
	{}

	public VIFPack (int vif_, long peer_)
	{
		_setVIF(vif_, peer_);
	}
	
	public VIFPack (int vif_, long myself_, long peer_)
	{
		_setVIF(vif_, myself_, peer_);
	}
	
	public String info()
	{
		if (peers == null) return "No vif is set up.\n";
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<peers.length; i++)
			if (peers[i] != Address.NULL_ADDR) {
				long myself_ = myself != null && i < myself.length?
							   myself[i]: Address.NULL_ADDR;
				sb_.append("VIF " + (vifStartIndex + i) + ": "
						   + (myself_ == Address.NULL_ADDR? "<default>": String.valueOf(myself_))
						   + " --> " + peers[i] + "\n");
			}
		return sb_.toString();
	}
		
	public void duplicate(Object source_)
	{
		VIFPack that_ = (VIFPack)source_;
		vifStartIndex = that_.vifStartIndex;
		peers = (long[])drcl.util.ObjectUtil.clone(that_.peers);
		myself = (long[])drcl.util.ObjectUtil.clone(that_.myself);
	}
	
	public Object clone()
	{
		VIFPack that_ = new VIFPack();
		that_.duplicate(this);
		return that_;
	}
	
	// return Address.NULL_ADDRESS if error
	public long _getPeer(int vif_)
	{
		if (vif_ < vifStartIndex || peers == null
			|| vif_ - vifStartIndex >= peers.length)
			return Address.NULL_ADDR;
		return peers[vif_ - vifStartIndex];
	}
		
	public long _getMyself(int vif_)
	{
		if (vif_ < vifStartIndex || myself == null
			|| vif_ - vifStartIndex >= myself.length)
			return Address.NULL_ADDR;
		return myself[vif_ - vifStartIndex];
	}
		
	public void _setVIF(int vif_, long peer_)
	{
		if (peers == null) {
			peers = new long[1];
			vifStartIndex = vif_;
		}
		else if (vifStartIndex > vif_) {
			long[] tmp_ = new long[peers.length + (vifStartIndex - vif_)];
			System.arraycopy(peers, 0, tmp_, (vifStartIndex - vif_), peers.length);
			for (int i=vif_+1; i<vifStartIndex; i++)
				tmp_[i-vif_] = Address.NULL_ADDR;
			peers = tmp_;
			vifStartIndex = vif_;
		}
		else if (vif_ - vifStartIndex >= peers.length) {
			long[] tmp_ = new long[vif_ - vifStartIndex + 1];
			System.arraycopy(peers, 0, tmp_, 0, peers.length);
			for (int i=peers.length; i<tmp_.length; i++)
				tmp_[i] = Address.NULL_ADDR;
			peers = tmp_;
		}
		peers[vif_ - vifStartIndex] = peer_;
	}
		
	public void _setVIF(int vif_, long myself_, long peer_)
	{
		if (peers == null) {
			peers = new long[1];
			vifStartIndex = vif_;
		}
		if (myself == null) {
			myself = new long[1];
		}
		else if (vifStartIndex > vif_) {
			long[] tmp_ = new long[peers.length + (vifStartIndex - vif_)];
			System.arraycopy(peers, 0, tmp_, (vifStartIndex - vif_), peers.length);
			for (int i=vif_+1; i<vifStartIndex; i++)
				tmp_[i-vif_] = Address.NULL_ADDR;
			peers = tmp_;
			
			tmp_ = new long[myself.length + (vifStartIndex - vif_)];
			System.arraycopy(myself, 0, tmp_, (vifStartIndex - vif_), myself.length);
			for (int i=vif_+1; i<vifStartIndex; i++)
				tmp_[i-vif_] = Address.NULL_ADDR;
			myself = tmp_;
			
			vifStartIndex = vif_;
		}
		else if (vif_ - vifStartIndex >= peers.length) {
			long[] tmp_ = new long[vif_ - vifStartIndex + 1];
			System.arraycopy(peers, 0, tmp_, 0, peers.length);
			for (int i=peers.length; i<tmp_.length; i++)
				tmp_[i] = Address.NULL_ADDR;
			peers = tmp_;
			
			tmp_ = new long[vif_ - vifStartIndex + 1];
			System.arraycopy(myself, 0, tmp_, 0, myself.length);
			for (int i=myself.length; i<tmp_.length; i++)
				tmp_[i] = Address.NULL_ADDR;
			myself = tmp_;
		}
		peers[vif_ - vifStartIndex] = peer_;
		myself[vif_ - vifStartIndex] = myself_;
	}
	
	public long[] getPeers()
	{
		if (peers == null) return null;
		long[] tmp_ = new long[peers.length + vifStartIndex];
		System.arraycopy(peers, 0, tmp_, vifStartIndex, peers.length);
		return tmp_;
	}
	
	public void setPeers(long[] peers_)
	{
		peers = peers_;
		vifStartIndex = 0;
	}
	
	public long[] getMyself()
	{
		long[] tmp_ = new long[myself.length + vifStartIndex];
		System.arraycopy(myself, 0, tmp_, vifStartIndex, myself.length);
		return tmp_;
	}
	
	public void setMyself(long[] myself_)
	{
		myself = myself_;
	}
		
	public int _getVIFFromPeer(long peer_)
	{
		if (peers == null) return -1;
		for (int i=0; i<peers.length; i++)
			if (peers[i] == peer_) return vifStartIndex + i;
		return -1;
	}
	
	public int getNumOfVIFs()
	{ return myself == null? 0: myself.length; }
}
