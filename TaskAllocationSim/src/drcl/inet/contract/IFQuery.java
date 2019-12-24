// @(#)IFQuery.java   9/2002
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

package drcl.inet.contract;

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.data.*;

/**
The InterfaceInfoQuery contract.
It also provides a set of static methods to conduct the contract at
a specified port.  These methods are particularly useful in implementing
a protocol that makes use of the interface/neighbor information.

This contract defines three services at the reactor:
<dl>
<dt> <code>GetAllInterfacesInfo</code>
<dd> The initiator sends a <code>null</code>
	and the reactor returns an array of interface info
	({@link drcl.inet.data.InterfaceInfo}<code>[]</code>) stored at the reactor.
<dt> <code>GetOneInterfaceInfo</code>
<dd> The initiator sends the interface index ({@link drcl.data.IntObj}).
	and the reactor returns the interface info requested, null if 
	the interface does not exist.
<dt> <code>SetAllInterfacesInfo</code>
<dd> The initiator sends an array of interface info
	({@link drcl.inet.data.InterfaceInfo}<code>[]</code>)
	and the reactor replaces what it has with the new set of information.
<dt> <code>SetOneInterfaceInfo</code>
<dd> The initiator sends an array of two objects; the first of which is
	a {@link drcl.data.IntObj} indicating which interface to set;
	the second of which is the interface information 
	({@link drcl.inet.data.InterfaceInfo}) to replace with.
	In response, the reactor replaces the interface info at the specified
	interface.
</dl>
This class also provides a set of static methods to conduct the above services
from the specified port in various ways.  These methods are particularly useful in implementing
a protocol that maintains, or needs to be aware of, the interface info of the node.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
@see drcl.inet.data.InterfaceInfo
*/
public class IFQuery extends Contract
{
	public IFQuery()
	{ super(); }
	
	public IFQuery(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "InterfaceInfoQuery Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static InterfaceInfo[] getInterfaceInfos(Port out_)
	{ return (InterfaceInfo[]) out_.sendReceive(null); }
	
	public static InterfaceInfo[] getInterfaceInfos(drcl.data.BitSet ifset_, Port out_)
	{ return (InterfaceInfo[]) out_.sendReceive(ifset_); }
	
	public static InterfaceInfo getInterfaceInfo(int index_, Port out_)
	{ return (InterfaceInfo) out_.sendReceive(new IntObj(index_)); }
	
	public static void setInterfaceInfos(InterfaceInfo[] aa_, Port out_)
	{ out_.sendReceive(aa_); }
	
	public static void setInterfaceInfo(int index_, InterfaceInfo if_, Port out_)
	{ out_.sendReceive(new Object[]{new IntObj(index_), if_}); }
	
	public static NetAddress[] getPeerNetAddresses(int index_, Port out_)
	{
		InterfaceInfo if_ = (InterfaceInfo) out_.sendReceive(new IntObj(index_));
		if (if_ == null) return null;
		else return if_.getPeerNetAddresses();
	}
	
	public static NetAddress getLocalNetAddress(int index_, Port out_)
	{
		InterfaceInfo if_ = (InterfaceInfo) out_.sendReceive(new IntObj(index_));
		if (if_ == null) return null;
		else return if_.local;
	}
	
	public static NetAddress getLocalNetAddress(long peerAddr_, Port out_)
	{
		InterfaceInfo[] all_ = (InterfaceInfo[]) out_.sendReceive(null);
		if (all_ == null) return null;
		for (int i=0; i<all_.length; i++) {
			if (all_[i] != null && all_[i].containsPeer(peerAddr_))
				return all_[i].local;
		}
		return null;
	}
	
	public static int getIndexByLocalAddr(long localAddr_, Port out_)
	{
		InterfaceInfo[] all_ = (InterfaceInfo[]) out_.sendReceive(null);
		if (all_ == null) return -1;
		for (int i=0; i<all_.length; i++) {
			if (all_[i] != null && all_[i].local.getAddress() == localAddr_) return i;
		}
		return -1;
	}
	
	public static int getIndexByPeerAddr(long peerAddr_, Port out_)
	{
		InterfaceInfo[] all_ = (InterfaceInfo[]) out_.sendReceive(null);
		if (all_ == null) return -1;
		for (int i=0; i<all_.length; i++) {
			if (all_[i] == null) continue;
			if (all_[i].containsPeer(peerAddr_)) return i;
		}
		return -1;
	}
	
	public static int getNumOfInterfaces(Port out_)
	{
		InterfaceInfo[] all_ = (InterfaceInfo[]) out_.sendReceive(null);
		if (all_ == null) return 0;
		else return all_.length;
	}
	
	public static int getMTU(int index_, Port out_)
	{
		InterfaceInfo ifinfo_ = (InterfaceInfo) out_.sendReceive(new IntObj(index_));
		return ifinfo_ == null? -1: ifinfo_.getMTU();
	}
	
	public static double getBandwidth(int index_, Port out_)
	{
		InterfaceInfo ifinfo_ = (InterfaceInfo) out_.sendReceive(new IntObj(index_));
		return ifinfo_ == null? -1.0: ifinfo_.getBandwidth();
	}
	
	public static int getBufferSize(int index_, Port out_)
	{
		InterfaceInfo ifinfo_ = (InterfaceInfo) out_.sendReceive(new IntObj(index_));
		return ifinfo_ == null? -1: ifinfo_.getBufferSize();
	}
}




