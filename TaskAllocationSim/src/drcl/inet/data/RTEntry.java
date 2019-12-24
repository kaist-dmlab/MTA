// @(#)RTEntry.java   9/2002
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

/**
 * The routing table entry class.
 * 
 * @author Hung-ying Tyan
 * @version 1.0, 10/17/2000
 * @see RTKey
 */
public class RTEntry extends drcl.DrclObj
{
	public drcl.comp.ACATimer handle; // for timeout

	public RTEntry (drcl.data.BitSet bsOutIf_) 
	{
		bsOutIf = bsOutIf_;
	}
		
	public RTEntry (drcl.data.BitSet bsOutIf_, Object extension_) 
	{
		bsOutIf = bsOutIf_;
		extension = extension_;
	}
		
	public RTEntry (RTKey key_, drcl.data.BitSet bsOutIf_, Object extension_,
								 double timeout_) 
	{
		key = key_;
		bsOutIf = bsOutIf_;
		extension = extension_;
		timeout = timeout_;
	}
		
	public RTEntry (long nexthop_, drcl.data.BitSet bsOutIf_) 
	{
		nextHop = nexthop_;
		bsOutIf = bsOutIf_;
	}
		
	public RTEntry (long nexthop_, drcl.data.BitSet bsOutIf_, Object extension_) 
	{
		nextHop = nexthop_;
		bsOutIf = bsOutIf_;
		extension = extension_;
	}
		
	public RTEntry (RTKey key_, long nexthop_, drcl.data.BitSet bsOutIf_,
								 Object extension_, double timeout_) 
	{
		nextHop = nexthop_;
		key = key_;
		bsOutIf = bsOutIf_;
		extension = extension_;
		timeout = timeout_;
	}
		
	RTKey  key;
	long nextHop = drcl.net.Address.NULL_ADDR;
	drcl.data.BitSet bsOutIf; // interfaces
	int[]  outIf   = null; // cache to bsOutIf.getSetBitIndices()
	Object extension; // additional information, 
	double timeout; // absolute time

	public RTEntry()
	{}
		
	public void duplicate(Object source_)
	{
		RTEntry that_ = (RTEntry)source_;
		key = (RTKey)that_.key;
		if (that_.outIf != null) {
			outIf = new int[that_.outIf.length];
			for (int i=0; i<outIf.length; i++) outIf[i] = that_.outIf[i];
		}
		extension = drcl.util.ObjectUtil.clone(that_.extension);
		bsOutIf = (drcl.data.BitSet) drcl.util.ObjectUtil.clone(that_.bsOutIf);
		nextHop = that_.nextHop;
		timeout = that_.timeout;
	}
		
	public void setKey(RTKey k)
	{ key = k; }
	
	public RTKey getKey()
	{ return key; }
	
	public void setNextHop(long nexthop_)
	{ nextHop = nexthop_; }
	
	public long getNextHop()
	{ return nextHop; }
			
	/**
	 * Returns the bit set of interfaces.
	 * An unset bit indicates that the corresponding interface is pruned.
	 */
	public drcl.data.BitSet getOutIf()
	{ return bsOutIf; }

	/**
	 * Returns the (grafted) outgoing interfaces.
	 */
	public int[] _getOutIfs()
	{
		if (outIf == null) {
			return bsOutIf == null? null: bsOutIf.getSetBitIndices();
		}
		return outIf;
	}

	/**
	 * Sets the outgoing interfaces by a bit set.
	 * One bit corresponds to one interface.
	 * A bit set (true) indicates a grafted interface.
	 * An unset bit indicates a pruned interface.
	 */
	public void setOutIf(drcl.data.BitSet outIf) 
	{ 
		bsOutIf = outIf; 
		outIf   = null;
	}

	/**
	 * Prunes the specified interface.
	 *
	 * @param whichIf index of the interface that's got pruned.
	 */
	public boolean prune(int whichIf)
	{
		boolean routechange = bsOutIf.get(whichIf);
		bsOutIf.clear(whichIf);
		outIf = null;
		return routechange;
	}

	/**
	 * Grafts the specified interface.
	 *
	 * @param whichIf index of the interface that's got grafted.
	 */
	public boolean graft(int whichIf)
	{
		boolean routechange = !bsOutIf.get(whichIf);
		bsOutIf.set(whichIf);
		outIf = null;
		return routechange;
	}

	/**
	 * Returns the prune state of the specified interface.
	 */
	public boolean isPruned(int whichIf)
	{
		return !bsOutIf.get(whichIf);
	}
		
	public void setExtension(Object ex_)
	{ extension = ex_; }
		
	public Object getExtension()
	{ return extension; }
	
	public double _getTimeout()
	{ return timeout; }
	
	public void _setTimeout(double t_)
	{ timeout = t_; }
		
	public String toString()
	{ return toString(""); }
		
	public String toString(String prefix)
	{	return prefix + key + ":"
			   + (nextHop == drcl.net.Address.NULL_ADDR? "??": nextHop+"")
			   + "-" + bsOutIf + "-" + extension + "-" + timeout;	}
	
	public boolean equals(Object o_)
	{
		if (o_ == this) return true;
		if (!(o_ instanceof RTEntry)) return false;
		RTEntry that_ = (RTEntry)o_;
		if (!drcl.util.ObjectUtil.equals(key, that_.key)) return false;
		if (!drcl.util.ObjectUtil.equals(bsOutIf, that_.bsOutIf)) return false;
		if (!drcl.util.ObjectUtil.equals(extension, that_.extension)) return false;
		if (Double.isNaN(timeout) && Double.isNaN(that_.timeout)) return true;
		return timeout == that_.timeout;
	}
}
