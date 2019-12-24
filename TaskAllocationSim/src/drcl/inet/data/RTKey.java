// @(#)RTKey.java   9/2002
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

package drcl.inet.data;

import drcl.data.*;

/**
 * The key class to the routing table entry.
 * 
 * @author Hung-ying Tyan
 * @version 1.0, 10/17/2000
 * @see RTEntry
 */
public class RTKey extends drcl.data.MapKey
{
	/**
	 * Creates an <code>RTKey</code> given source, destination and incoming interface.
	 * If source/destination can be ignored, <code>Address.any()</code>
	 * or <code>Address.dontcare()</code> is specified.  If incoming interface
	 * can be ignored, use negative indices.  The corresponding masks are
	 * set to be all 1's if the arguments are not equal to the above special
	 * values, otherwise the masks are set to be 0.
	 * 
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param incoming_ index of incoming interface.
	 */
	public RTKey (long src_, long dest_, int incoming_)
	{
		super();
		value = new drcl.data.BitSet(192);
		mask = new drcl.data.BitSet(192);
		set(src_, dest_, incoming_);
	}
	
	/**
	 * Creates an <code>RTKey</code> given source, destination, incoming interface and masks.
	 * If source/destination can be ignored, <code>Address.any()</code>
	 * or <code>Address.dontcare()</code> is specified.  If incoming interface
	 * can be ignored, use negative indices.  The corresponding masks are
	 * set to be the argument masks if the arguments are not equal to the above special
	 * values, otherwise the masks are set to be 0.
	 * 
	 * @param src_ source.
	 * @param srcmask_ source mask.
	 * @param dest_ destination.
	 * @param destmask_ destination mask.
	 * @param incoming_ index of incoming interface.
	 * @param incomingmask_ mask for the incoming interface index.
	 */
	public RTKey (long src_, long srcmask_, long dest_, long destmask_, int incoming_, int incomingmask_)
	{
		super();
		value = new drcl.data.BitSet(192);
		mask = new drcl.data.BitSet(192);
		set(src_, srcmask_, dest_, destmask_, incoming_, incomingmask_);
	}
	
	public RTKey() 
	{
		super();
		value = new drcl.data.BitSet(192);
		mask = new drcl.data.BitSet(192);
	}

	public long getSource() { return value.getSubset(0); }
	public void setSource(long s_)
	{ value.set(0, s_); }
	
	public long getDestination() { return value.getSubset(1); }
	public void setDestination(long g_)
	{ value.set(1, g_); }
	
	public int getIncomingIf() { return (int)value.getSubset(2); }
	public void setIncomingIf(int i_)
	{ value.set(2, i_); }
			
	public long getSourceMask() { return mask.getSubset(0); }
	public void setSourceMask(long m_)
	{ mask.set(0, m_); }
	
	public long getMaskedSource()
	{ return value.getSubset(0) & mask.getSubset(0); }
	
	public long getDestinationMask() { return mask.getSubset(1); }
	public void setDestinationMask(long m_)
	{ mask.set(1, m_); }
	
	public long getMaskedDestination()
	{ return value.getSubset(1) & mask.getSubset(1); }
	
	public int getIncomingIfMask() { return (int)mask.getSubset(2); }
	public void setIncomingIfMask(int m_)
	{ mask.set(2, m_); }
			
	public int getMaskedIncomingIf()
	{ return (int)(value.getSubset(2) & mask.getSubset(2)); }
	
	/**
	 * Set this <code>RTKey</code> with the given source, destination and incoming interface.
	 * The corresponding masks are set to be all 1's.
	 * 
	 * @param src_ source.
	 * @param dest_ destination.
	 * @param incoming_ index of incoming interface.
	 */
	public void set(long src_, long dest_, int incoming_)
	{
		value.set(0, src_);
		value.set(1, dest_);
		value.set(2, incoming_);
		mask.set(0, -1L);
		mask.set(1, -1L);
		mask.set(2, -1L);
	}
	
	/**
	 * Set this <code>RTKey</code> with the given source, destination, incoming interface and masks.
	 * 
	 * @param src_ source.
	 * @param srcmask_ source mask.
	 * @param dest_ destination.
	 * @param destmask_ destination mask.
	 * @param incoming_ index of incoming interface.
	 * @param incomingmask_ mask for the incoming interface index.
	 */
	public void set(long src_, long srcmask_, long dest_, long destmask_,
							   int incoming_, int incomingmask_)
	{
		value.set(0, src_);
		value.set(1, dest_);
		value.set(2, incoming_);
		mask.set(0, srcmask_);
		mask.set(1, destmask_);
		mask.set(2, incomingmask_);
		value.and(mask);
	}
	
	//
	private void ___MISC___() {}
	//
	
	public int hashCode()
	{
		return (int)value.getSubset(1);
	}
	
	public String toString() 
	{
		return "(" + value.getSubset(0) + "," + value.getSubset(1) + "," + value.getSubset(2) + ")" +
			   "(" + mask.getSubset(0) + "," + mask.getSubset(1) +  "," + mask.getSubset(2) + ")";
	}

	public String print(drcl.net.Address addr_) 
	{
		return "(" + addr_.ltos(value.getSubset(0)) + "," + addr_.ltos(value.getSubset(1))
			+ "," + value.getSubset(2) + ")"
			+ "(" + mask.getSubset(0) + "," + mask.getSubset(1) +  "," + mask.getSubset(2) + ")";
	}
}
