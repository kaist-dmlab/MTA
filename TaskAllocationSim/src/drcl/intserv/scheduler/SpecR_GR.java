// @(#)SpecR_GR.java   9/2002
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

package drcl.intserv.scheduler;

import drcl.intserv.*;

public class SpecR_GR extends SpecR implements SpecR_Direct
{
	public int bw;		// minimum bandwidth required to fullfil the qos requirement
	public int buffer = 0;
	
	// the followings are for calculating buffer requirement at each hop
	int mtu = 0;
	
	public SpecR_GR ()
	{}

	public SpecR_GR (int bw_, int buffer_, int mtu_)
	{
		bw = bw_;
		buffer = buffer_;
		mtu = mtu_;
	}
	
	public int getBuffer() { return buffer; }
	public void setBuffer(int buffer_) { buffer = buffer_; }

	public void setBW(int bw_) { bw = bw_; }
	public int getBW() { return bw; }

	// see superclass
	public SpecR merge(SpecR rspec_)
	{	
		if (!(rspec_ instanceof SpecR_GR)) return null;
		
		SpecR_GR tmp_ = (SpecR_GR)rspec_;
			
		bw = Math.max( bw, tmp_.bw );
		buffer = Math.max( buffer, tmp_.buffer );
		return  this;
	}

	// see superclass
	public int compareWith(SpecR rspec_)
	{
		if (!(rspec_ instanceof SpecR_GR)) return Integer.MAX_VALUE;
		
		SpecR_GR tmp_ = (SpecR_GR)rspec_;
				
		if (bw == tmp_.bw && buffer == tmp_.buffer) return 0;
		if (bw > tmp_.bw && buffer > tmp_.buffer) return 1;
		if (bw < tmp_.bw && buffer < tmp_.buffer) return -1;
		return Integer.MAX_VALUE;
	}
		
	public void duplicate(Object source_) 
	{
		if (!(source_ instanceof SpecR_GR)) return;
		
		SpecR_GR that_ = (SpecR_GR)source_;
		bw = that_.bw;
		buffer = that_.buffer;
		mtu = that_.mtu;
	}
	
	// see superclass
	public void perHopAdjust()
	{
		buffer -= mtu;
	}
	
	public String toString()
	{
		return "handle=" + handle + ",bw=" + getBW() + ",buffer=" + getBuffer() 
			   + ",MTU=" + mtu + ",activated=" + activated;
	}
}
