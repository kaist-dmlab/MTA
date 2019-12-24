// @(#)SpecR_DCTS.java   9/2002
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

public class SpecR_DCTS extends SpecR implements SpecR_SP, SpecR_Direct
{
	public SpecR_DCTS ()
	{}

	public SpecR_DCTS (int c_, double d_)
	{
		C = c_;
		D = d_;
	}
	
	public int C; // max # of bits in a period
	public double D; // maximum allowable delay
	
	public double CC; // the actual "computation time" = C / scheduler capacity
	
	public void setC(int c_) { C = c_; }
	public int getC() { return C; }
		
	public void setD(double d_) { D = d_; }
	public double getD() { return D; }
	
	public double getPriority() { return D; }
	
	public int getBuffer() { return C; }
	public int getBW() { return (int)Math.ceil((C << 3) / D); } 

	public void setBW(int bw_) 
	{ D = C / bw_; } 
	
	public void setBuffer(int buffer_)
	{ D = buffer_ / (C / D); C = (int)buffer_; }

	// see superclass
	public SpecR merge(SpecR rspec_)
	{	
		if (!(rspec_ instanceof SpecR_DCTS)) return null;
		
		SpecR_DCTS that_ = (SpecR_DCTS)rspec_;
		D = Math.min(D, that_.D);
		C = (int)(D * Math.max(getBW(), that_.getBW()) / 8.0);
		return  this;
	}

	// see superclass
	public int compareWith(SpecR rspec_)
	{
		if (!(rspec_ instanceof SpecR_PTSP)) return UNCOMPARABLE;
		
		SpecR_PTSP that_ = (SpecR_PTSP)rspec_;
		if (C == that_.C && D == that_.D) return 0;
		if (C >= that_.C && getBW() >= that_.getBW() && D <= that_.D) return TIGHT;
		if (C <= that_.C && getBW() <= that_.getBW() && D >= that_.D) return LOOSE;
		else return UNCOMPARABLE;
	}
		
	public void duplicate(Object source_) 
	{
		if (!(source_ instanceof SpecR_DCTS)) return;
		
		SpecR_DCTS that_ = (SpecR_DCTS)source_;
		C = that_.C;
		D = that_.D;
	}
	
	// see superclass
	public void perHopAdjust()
	{}

	public String toString()
	{
		return "handle=" + handle + ",bw=" + getBW() + ",buffer=" + getBuffer() 
			   + ",C=" + C + ",D=" + D + ",activated=" + activated;
	}
}
