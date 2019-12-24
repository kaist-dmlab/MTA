// @(#)SpecFlow.java   9/2002
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

package drcl.intserv;

import java.util.*;
import drcl.data.*;
import drcl.net.traffic.TrafficModel;

public class SpecFlow extends drcl.DrclObj
{
	// individual or aggregate flow profile
	public SpecR	rspec; 
	public TrafficModel	tspec;
	
	public int     handle = -1;       // automatically set when installed
	
	public SpecFlow ()
	{}

	public SpecFlow (TrafficModel tspec_, SpecR rspec_)
	{
		rspec = rspec_;
		tspec = tspec_;
	}

	public void duplicate(Object source_)
	{
		SpecFlow that_ = (SpecFlow) source_;
		
		if (that_.rspec != null) rspec = (SpecR) that_.rspec.clone();
		if (that_.tspec != null) tspec = (TrafficModel) that_.tspec.clone();

		handle = that_.handle; // well...
	}
	
	/** Merge <code>that_</code> into this flowspec. */
	public void merge(SpecFlow that_)
	{
		if (that_ == null) return;
		tspec = tspec == null? (TrafficModel)that_.tspec.clone(): tspec.merge(that_.tspec);
		rspec = rspec == null? (SpecR)that_.rspec.clone(): rspec.merge(that_.rspec);
	}
	
	/** Adjusts the rspec at a hop toward the sender.  */
	public void perHopAdjust()
	{ if (rspec != null) rspec.perHopAdjust(); }
	
	public String toString()
	{
		return "Handle:" + handle + ",rspec:" + rspec
				   + ",tspec:" + tspec + ",activated:" 
				   + isActivated();
	}
	
	void setHandle(int h) 
	{ 
		handle = h; 
		if (rspec != null) rspec.setHandle(h);
	}

	/** Returns the handle of this flowspec. */
	public int  getHandle()
	{ return handle; }
	
	/** Returns true if the flowspec is activated. */
	public boolean isActivated()          
	{ return rspec != null? rspec.activated: false; }
	
	public SpecR getRspec()
	{ return rspec; }
	public TrafficModel getTspec()
	{ return tspec; }
}
