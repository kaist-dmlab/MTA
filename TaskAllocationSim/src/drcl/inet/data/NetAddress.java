// @(#)NetAddress.java   1/2004
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
 * The network address class.  A network address consists of an address and
 * an address mask.  The network address is associated with an interface of 
 * a node.  It represents what network the outside world see from that interface.
 * It is particularly useful at the edge interfaces of a domain or a network.
 * 
 * @author Hung-ying Tyan
 * @version 1.0, 10/17/2000
 */
public class NetAddress extends drcl.DrclObj
{
	long addr; 
	long mask;
	
	public NetAddress ()
	{}

	public NetAddress (long addr_, long mask_)
	{
		addr = addr_;
		mask = mask_;
	}
	
	/**
	 * @param nmaskbits number of 0's in the mask.
	 */
	public NetAddress (long addr_, int nmaskbits_)
	{
		addr = addr_;
		mask = getMask(nmaskbits_);
	}
	
	public static long getMask(int nmaskbits_)
	{ return -1 << nmaskbits_; }
	
	public static int getNumMaskBits(long mask_)
	{
		long probe_ = 1;
		for (int i=0; i<64; i++) {
			if ((probe_ & mask_) > 0) return i;
			probe_ <<= 1;
		}
		return 64;
	}
	
	public long getAddress()
	{ return addr; }
	
	public void setAddress(long a_)
	{ addr = a_; }
	
	public long getMask()
	{ return mask; }
	
	public void setMask(long m_)
	{ mask = m_; }
	
	public long getMaskedAddress()
	{ return addr & mask; }
	
	public void duplicate(Object source_)
	{
		NetAddress that_ = (NetAddress)source_;
		addr = that_.addr;
		mask = that_.mask;
	}
	
	public boolean equals(Object o_)
	{
		if (o_ == this) return true;
		if (o_ instanceof NetAddress) {
			NetAddress that_ = (NetAddress)o_;
			return addr == that_.addr && mask == that_.mask;
		}
		else return false;
	}
	
	public String toString()
	{
		return "<" + addr + "," + mask + ">";
	}
	
	public String print(Address addr_)
	{
		return "<" + addr_.ltos(addr) + "," + addr_.ltos(mask) + ">";
	}
}
