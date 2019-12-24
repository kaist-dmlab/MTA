// @(#)SpecR.java   9/2002
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

// Note: 
// A Link subclass should provide its own Spec_R subclass.
// Corresponding Admission class should recognize its format.
public abstract class SpecR extends drcl.DrclObj
{
	// Used in compareWith function.
	public static final int 
		TIGHT = 0,
		EQUAL = 1,
		LOOSE = 2,
		UNCOMPARABLE = 3,
		WRONGCLASS = 4;		// Two rspecs are not objects of the same class
		
	transient public int handle = -1;     // automatically set when flowspec is installed
	transient public boolean activated = true;
	
	public void    setHandle(int h) { handle = h; }
	public int     getHandle()      { return handle; }
	
	/**
	 * A flow must be activated before its packets can be scheduled by the 
	 * scheduler as QoS packets.
	 */
	public void    setActivated(boolean v) { activated = v; }
	public boolean getActivated()          { return activated; }

	/** Returns the bandwidth requirement for this Rspec. */
	public abstract int getBW();
	
	/** Returns the buffer requirement for this Rspec. */
	public abstract int getBuffer();
	
	// merge this and the specified rspec, least-upper-bound
	// return this.
	public abstract SpecR merge(SpecR rspec_);

	/**
	 *  Returns 1 if this &gt; rspec; 0 if this == rspec
	 *  and -1 if this &lt; rspec.
	 */
	public abstract int compareWith(SpecR rspec);
	
	/** Adjust the rspecs when backing off one hop. */
	public abstract void perHopAdjust();
	
	public String toString()
	{
		return "handle=" + handle + ",bw=" + getBW() + ",buffer=" + getBuffer() 
			   + ",activated=" + activated;
	}
}
