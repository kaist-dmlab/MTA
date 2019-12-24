// @(#)FooPacket.java   8/2003
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

package drcl.net;

/** A packet class that keeps both packet count and byte count. */
public class FooPacket extends Packet
{
	int pktcount;
	long bytecount;

	public FooPacket()
	{ super(); }

	public FooPacket(int pktsize_, int pktcnt_, long bytecnt_)
	{ super(pktsize_); pktcount = pktcnt_; bytecount = bytecnt_; }

	public String getName()
	{ return "FOO"; }

	public void setPacketCount(int cnt_)
	{ pktcount = cnt_; }

	public int getPacketCount()
	{ return pktcount; }

	public int getNumberCount()
	{ return pktcount; }

	public boolean isPacketCountSupported()
	{ return true; }

	public void setByteCount(long cnt_)
	{ bytecount = cnt_; }

	public long getByteCount()
	{ return bytecount; }

	public long getSizeCount()
	{ return bytecount; }

	public boolean isByteCountSupported()
	{ return true; }

	public void set(int pktcount_, long bytecount_)
	{
		pktcount = pktcount_;
		bytecount = bytecount_;
	}

	/** Returns <code>null</code>; this class does not provide packet type information. */
	public String getPacketType()
	{ return null; }

	/*
	public void duplicate(Object p_)
	{
		super.duplicate(p_);
		pktcount = ((FooPacket)p_).pktcount;
		bytecount = ((FooPacket)p_).bytecount;
	}
	*/

	public Object clone()
	{ return new FooPacket(size, pktcount, bytecount); }

	public String _toString(String separator_)
	{ return "pktcnt:" + pktcount + separator_ + "bytecnt:" + bytecount; }
}
