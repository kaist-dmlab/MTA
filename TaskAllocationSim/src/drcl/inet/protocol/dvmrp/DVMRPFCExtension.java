// @(#)DVMRPFCExtension.java   12/2002
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

/** Forwarding cache entry extension for {@link DVMRP}.
Specifically, it keeps the upstream node address, the prune expiration times, graft
ack timeout and backoff, and the <code>sentPruneUpstream</code> flag.
*/
public class DVMRPFCExtension extends drcl.DrclObj
{
	static final double NOT_DEPENDENT = -1.0;
	static final double GRAFTED = 0.0; // take advantage of Java array initial value
	
	long upstream;
	transient boolean sentPruneUpstream = false;
	transient double[] pruneExpires; // time when prune expires
	transient double graftAckTimeout = Double.NaN;
	transient int graftAckBackoff = 0; // times of exponential backoffs
	
	public DVMRPFCExtension ()
	{}

	public DVMRPFCExtension (long upstream_, drcl.data.BitSet dependentIfs_)
	{
		upstream = upstream_;
		resetPrune(dependentIfs_);
	}
	
	public void duplicate(Object source_)
	{
		DVMRPFCExtension that_ = (DVMRPFCExtension)source_;
		upstream = that_.upstream;
		sentPruneUpstream = that_.sentPruneUpstream;
		pruneExpires = (double[])drcl.util.ObjectUtil.clone(that_.pruneExpires);
		graftAckTimeout = that_.graftAckTimeout;
		graftAckBackoff = that_.graftAckBackoff;
	}
	
	public Object clone()
	{
		DVMRPFCExtension new_ = new DVMRPFCExtension();
		new_.duplicate(this);
		return new_;
	}
	
	// when upstream changes
	public void resetPrune(drcl.data.BitSet dependentIfs_)
	{
		pruneExpires = new double[dependentIfs_.getSize()];
		int[] ifs_ = dependentIfs_.getUnsetBitIndices();
		for (int i=0; i<ifs_.length; i++)
			pruneExpires[ifs_[i]] = NOT_DEPENDENT;
	}
	
	public double getPruneExpire(int index_)
	{
		if (pruneExpires == null || index_ >= pruneExpires.length)
			return NOT_DEPENDENT;
		else
			return pruneExpires[index_];
	}
	
	public void setPruneExpire(int index_, double expire_)
	{
		if (index_ >= pruneExpires.length) {
			double[] tmp_ = new double[index_ + 1];
			System.arraycopy(pruneExpires, 0, tmp_, 0, pruneExpires.length);
			for (int i=pruneExpires.length; i<tmp_.length; i++)
				tmp_[i] = NOT_DEPENDENT;
			pruneExpires = tmp_;
		}
		pruneExpires[index_] = expire_;
	}
	
	// removes all the prune states that expires now
	// returns the bit set that reflects interfaces with prune state removed
	drcl.data.BitSet removePruneStates(double now_)
	{
		drcl.data.BitSet graftIfs_ = new drcl.data.BitSet(pruneExpires.length);
		for (int i=0; i<pruneExpires.length; i++) {
			double tmp_ = pruneExpires[i];
			if (tmp_ <= 0.0) continue;
			if (tmp_ <= now_) {
				graftIfs_.set(i);
				pruneExpires[i] = GRAFTED;
			}
		}
		return graftIfs_;
	}
	
	// get the minimum prune expiration time from the prune/graft states
	// return Double.NaN if no prune state exists 
	double getMinExpirationTime()
	{
		double minExpire_ = Double.NaN;
		for (int i=0; i<pruneExpires.length; i++) {
			double tmp_ = pruneExpires[i];
			if (tmp_ <= 0.0) continue;
			if (Double.isNaN(minExpire_) || minExpire_ > tmp_)
				minExpire_ = tmp_;
		}
		if (Double.isNaN(minExpire_) || minExpire_ > graftAckTimeout)
			minExpire_ = graftAckTimeout;
		return minExpire_;
	}
	
	// get the minimum prune life time from the prune states
	int getMinPruneLifetime(int maxLifetime_, double now_)
	{
		int minPruneLifetime_ = maxLifetime_;
		for (int i=0; i<pruneExpires.length; i++) {
			double tmp_ = pruneExpires[i] - now_;
			if (tmp_ <= 0.0) continue;
			if (minPruneLifetime_ > (int)tmp_) minPruneLifetime_ = (int)tmp_;
		}
		return minPruneLifetime_;
	}
	
	public boolean hasPendingGraft()
	{ return !Double.isNaN(graftAckTimeout); }
	
	// Sets/adjusts the graft ack timeout with exp. backoff.
	// base_: the base timeout value for backoff.
	public void graftAckExpBackOff(double now_, double base_)
	{
		if (Double.isNaN(graftAckTimeout)) {
			graftAckTimeout = now_ + base_;
		}
		else {
			graftAckBackoff++;
			graftAckTimeout = base_;
			for (int i=0; i<graftAckBackoff; i++)
				graftAckTimeout *= 2.0;
			graftAckTimeout += now_;
		}
	}
	
	public void clearPendingGraft()
	{
		graftAckTimeout = Double.NaN;
		graftAckBackoff = 0;
		sentPruneUpstream = false;
	}
	
	private static final String S_NOT_DEPENDENT = ".";
	private static final String S_GRAFTED = "+";
	
	public String toString()
	{
		StringBuffer sb_ = new StringBuffer("upstream:" + (upstream == Address.NULL_ADDR? "<null>": String.valueOf(upstream)) + "--");
		if (pruneExpires != null) {
			if (pruneExpires.length > 0) {
				if (pruneExpires[0] == NOT_DEPENDENT)
					sb_.append("(" + S_NOT_DEPENDENT);
				else if (pruneExpires[0] == GRAFTED)
					sb_.append("(" + S_GRAFTED);
				else
					sb_.append("(" + pruneExpires[0]);
				for (int i=1; i<pruneExpires.length; i++)
					if (pruneExpires[i] == NOT_DEPENDENT)
						sb_.append(" " + S_NOT_DEPENDENT);
					else if (pruneExpires[i] == GRAFTED)
						sb_.append(" " + S_GRAFTED);
					else
						sb_.append(" " + pruneExpires[i]);
				sb_.append(")");
			}
			else
				sb_.append("()");
		}
		else
			sb_.append("()");
		return sb_ + (hasPendingGraft()? "--" + "PendingGraftRetxAt:" + graftAckTimeout
										 + "(#backoffs:" + graftAckBackoff + ")": "")
			   + (sentPruneUpstream? "--sentPruneUpstream": "");
	}
}
