// @(#)Meter.java   9/2002
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

package drcl.diffserv;

import drcl.net.*;

/**
This defines the base class for a meter.
Subclasses needs to override: {@link #measure(drcl.net.Packet, double)},
{@link #reset()}, {@link #duplicate(Object)} and {@link #info(String)}.

@author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
version 1.0 07/16/00   
  */ 
public abstract class Meter extends drcl.DrclObj
{
	public Meter()
	{ super(); }
	
	/** Returns the label for the packet based on the measurement and
	the agreement of the connection the packet belongs to.  */
	protected abstract int measure(Packet p, double now_);
	
	/** Resets this meter to be used anew. */
	public abstract void reset();

	public abstract void duplicate(Object source_);
	
	public String info()
	{ return info(""); }

	/** Prints out the content of this meter.
	@param prefix_ prefix that should be prepended to each line of the result.*/
	public abstract String info(String prefix_);
}
