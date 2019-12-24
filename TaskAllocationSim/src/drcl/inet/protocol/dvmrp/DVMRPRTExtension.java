// @(#)DVMRPRTExtension.java   12/2002
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

package drcl.inet.protocol.dvmrp;

import drcl.net.Address;

/** Routing table entry extension for {@link DVMRP}.
Specifically, it keeps the interfaces and the addresses of the downstream dependents,
the metric of the routing entry, and the "changed" flag. */
public class DVMRPRTExtension extends drcl.DrclObj
{
	boolean changed = false;
	int metric = 0;
	drcl.data.BitSet dependentIfs;
	long[] dependents;
	
	public DVMRPRTExtension ()
	{}

	public DVMRPRTExtension (int metric_)
	{
		metric = metric_;
	}
	
	public DVMRPRTExtension (int metric_, drcl.data.BitSet dependentIfs_,
									  long[] dependents_, boolean changed_)
	{
		metric = metric_;
		dependentIfs = dependentIfs_;
		dependents = dependents_;
		changed = changed_;
	}
	
	public void duplicate(Object source_)
	{
		metric = ((DVMRPRTExtension)source_).metric;
		changed = ((DVMRPRTExtension)source_).changed;
		dependentIfs = ((DVMRPRTExtension)source_).dependentIfs;
		dependents = ((DVMRPRTExtension)source_).dependents;
	}
	
	// when upstream changes
	public void resetDependents(drcl.data.BitSet dependentIfs_)
	{
		dependentIfs = dependentIfs_;
		resetDependents();
	}
	
	// when upstream changes
	public void resetDependents()
	{
		dependentIfs.clear();
		dependents = new long[dependentIfs.getSize()];
		for (int j=0; j<dependents.length; j++)
			dependents[j] = Address.NULL_ADDR;
	}
	
	/**
	 * @return a valid (>=0) interface index if <code>target_</code> is one of the dependents
	 */
	public int getDependentIndex(long target_)
	{
		if (dependents == null) return -1;
		for (int i=0; i<dependents.length; i++)
			if (dependents[i] == target_) return i;
		return -1;
	}
	
	public void setDependent(int index_, long dependent_)
	{
		// let ArrayOutOfBound exception take place
		if (dependent_ == Address.NULL_ADDR) {
			dependentIfs.clear(index_);
		}
		else {
			dependentIfs.set(index_);
		}
		if (index_ >= dependents.length) {
			long[] tmp_ = new long[index_ + 1];
			System.arraycopy(dependents, 0, tmp_, 0, dependents.length);
			for (int i=dependents.length; i<tmp_.length; i++)
				tmp_[i] = Address.NULL_ADDR;
			dependents = tmp_;
		}
		dependents[index_] = dependent_;
	}
	
	public String toString()
	{
		String sMetric_ = metric == DVMRP.INFINITY? 
						  "INF": 
						  (metric > DVMRP.INFINITY? 
						   "INF+" + (metric-DVMRP.INFINITY): String.valueOf(metric));
		StringBuffer sb_ = new StringBuffer("metric:" + sMetric_
											+ (changed? "(changed)": "")
											+ "--dependents:");
		if (dependentIfs != null) {
			int[] indices_ = dependentIfs.getSetBitIndices();
			if (indices_.length > 0) {
				sb_.append("(" + dependents[indices_[0]]);
				for (int i=1; i<indices_.length; i++)
					sb_.append("," + dependents[indices_[i]]);
				sb_.append(")");
				sb_.append("{" + dependentIfs + "}");
			}
			else
				sb_.append("(){}");
		}
		else
			sb_.append("(){}");
		return sb_.toString();
	}
	
	public static String getTitle()
	{ return "metric--dependents(dependent_ifs)"; }
}
