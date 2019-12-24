// @(#)CBTPacket.java   8/2003
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

package drcl.inet.protocol.cbt;

import drcl.util.StringUtil;

/**
Defines the format of the packet used by {@link CBT}.
The format is simplified from what is defined in RFC2453.
*/
public class CBTPacket extends drcl.DrclObj implements CBTConstants
{ 
	public int type;
	public long requester, lasthop, group, core;
	public long[] addrArray;

	public Object extension;

	public CBTPacket()
	{ super(); }
	
	/** For all types except echo-request/echo-reply. */
	public CBTPacket (int type_, long requester_, long group_, long core_,
					long currenthop_, Object extension_)
	{
		type = type_;
		requester = requester_;
		group = group_;
		core = core_;
		lasthop = currenthop_;
		extension = extension_;
	}

	/** For echo-request/echo-reply. */
	public CBTPacket (int type_, long requester_, long[] addrArray_,
					long currenthop_, Object extension_)
	{
		type = type_;
		requester = requester_;
		addrArray = addrArray_;
		lasthop = currenthop_;
		extension = extension_;
	}

	public void duplicate(Object source_)
	{
		CBTPacket that_ = (CBTPacket)source_;
		type = that_.type;
		requester = that_.requester;
		group = that_.group;
		core = that_.core;
		lasthop = that_.lasthop;
		addrArray = that_.addrArray;
		extension = that_.extension; // XXX: ??
	}

	public Object clone()
	{
		if (addrArray == null)
			return new CBTPacket(type, requester, group, core, 
							lasthop, _cloneExt());
		else
			return new CBTPacket(type, requester, addrArray, 
							lasthop, _cloneExt());
	}

	Object _cloneExt()
	{
		if (extension instanceof drcl.ObjectCloneable)
			return ((drcl.ObjectCloneable)extension).clone();
		else
			return extension;
	}

	public String toString()
	{
		if (addrArray == null)
			return "CBT:" + TYPES[type]
				+ "--(" + group + "," + core + ")--" + requester
				+ ",last=" + lasthop
				+ (extension == null? "":
							   	",ext=" + StringUtil.toString(extension));
		else
			return "CBT:" + TYPES[type] + requester + "--"
				+ StringUtil.toString(addrArray)
				+ ",last=" + lasthop
				+ (extension == null? "":
							   	",ext=" + StringUtil.toString(extension));
	}
}
