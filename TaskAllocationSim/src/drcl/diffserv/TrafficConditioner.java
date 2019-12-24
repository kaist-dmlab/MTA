// @(#)TrafficConditioner.java   10/2003
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

package drcl.diffserv;

import drcl.inet.InetPacket;
import drcl.comp.*;
import drcl.data.BitSet;
import drcl.data.Map;
import drcl.data.MapKey;

/**
This implements a wrapper class for classifier and profiler(meter, marker).
A packet is first classified to find the profile it belongs to by the source,
destination and DF fields in the packet header,
and then it will be passed to the meter and marker installed for the profile for further
processing.

@author Rong Zheng
@version 1.0 10/26/00   
  */
public class TrafficConditioner extends drcl.inet.core.PktFilter
{
	Map sla;
	boolean inspect = false;
	
	public TrafficConditioner()
	{ super(); }

	public TrafficConditioner(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TrafficConditioner that_ = (TrafficConditioner)source_;
		if (that_.sla == null) sla = null;
		else {
			sla = new Map();
			MapKey[] keys_ = that_.sla.getAllKeys();
			for(int i=0; i < keys_.length; i++) {
				MapKey key_ = keys_[i]; // share keys, keys should be read-only in the map
				DFProfile dfp_ = (DFProfile)that_.sla.get(key_, Map.MATCH_EXACT);
				sla.addEntry(key_, dfp_.clone());
			}
		}
	}
	
	public void reset()
	{
		super.reset();
		if (sla == null) return;
		Object[] profiles_ = sla.getAllEntries();
		for (int i=0; i<profiles_.length; i++)
			((DFProfile)profiles_[i]).reset();
	}

	public String info()
	{ 
		if (sla == null || sla.getSize() == 0)
			return "TrafficConditioner: no profile is installed.\n";

		StringBuffer sb_ = new StringBuffer("TrafficConditioner: " + sla.getSize() + " profile(s)\n");
		MapKey[] keys_ = sla.getAllKeys();
		for(int i=0; i < keys_.length; i++) {
			MapKey key_ = keys_[i]; // no clone, the key should be read-only in the map
			DFProfile dfp_ = (DFProfile)sla.get(key_, Map.MATCH_EXACT);
			sb_.append("   profile" + i + ": " + DFUtil.printProfileKey(key_)
						+ "\n" + dfp_.info("      "));
		}
		return sb_.toString();
	}

	public void addProfile(long src_, long srcmask_, long dest_, long destmask_,
		long dscp_, long dscpmask_, DFProfile dfp_)
	{
		if (sla == null) sla = new Map();
		sla.addEntry(new MapKey(new long[]{dscpmask_, destmask_, srcmask_},
			new long[]{dscp_, dest_, src_}), dfp_);
	}

	public void addDefaultProfile()
	{
		if (sla == null) sla = new Map();
		sla.addEntry(new MapKey(new long[]{0, 0, 0}, new long[]{0, 0, 0}),
						new DFProfile()); // without meter and marker
	}
	
	/** This method calls <code>addProfile(src_, -1L, dest_, -1L, dscp_, -1L, dfp_)</code>.
	@see #addProfile(long, long, long, long, long, long, DFProfile) */
	public void addProfile(long src_, long dest_, long dscp_, DFProfile dfp_)
	{ addProfile(src_, -1L, dest_, -1L, dscp_, -1L, dfp_); }
	
	/** This method calls <code>addProfile(src_, -1L, dest_, -1L, 0L, 0L, dfp_)</code>.
	@see #addProfile(long, long, long, long, long, long, DFProfile) */
	public void addProfile(long src_, long dest_, DFProfile dfp_)
	{ addProfile(src_, -1L, dest_, -1L, 0L, 0L, dfp_); }
		
	/** This method calls <code>addProfile(src_, -1L, 0L, 0L, 0L, 0L, dfp_)</code>.
	@see #addProfile(long, long, long, long, long, long, DFProfile) */
	public void addProfile(long src_, DFProfile dfp_)
	{ addProfile(src_, -1L, 0L, 0L, 0L, 0L, dfp_); }

	/** Returns the longest matched profile. */
	public DFProfile getProfile(long src_, long dest_, long dscp_)
	{
		return (DFProfile)sla.get(new drcl.data.BitSet(192, new long[]{dscp_, dest_, src_}),
			Map.MATCH_LONGEST);
	}
	
	public void removeProfile(DFProfile dfp_)
	{
		sla.removeEntry(dfp_);
	}
	
	/** Removes the longest matched profile. */
	public void removeProfile(long src_, long dest_, long dscp_)
	{
		sla.remove(new drcl.data.BitSet(192, new long[]{dscp_, dest_, src_}),
			Map.MATCH_LONGEST);
	}
	
	synchronized protected void dataArriveAtUpPort(Object data_, Port upPort_)
	{	
		InetPacket p_ = (InetPacket)data_;
		
		DFProfile dfp_ = getProfile(p_.getSource(), 
									p_.getDestination(),
									DFUtil.getDSCP(p_));
		if (dfp_ == null) {
			if (isGarbageEnabled()) drop(p_, "no profile for the pkt"); 
			return;
		}
			
		if (dfp_.marker == null) {
			//fine, means no contract, free way
			downPort.doLastSending(data_);
			return;
		}
			
		int label_ = dfp_.meter == null? 0: dfp_.meter.measure(p_, getTime());
		if(dfp_.marker.markPacket(p_, label_)) {
			// for debug:
			if (inspect) {
				Port port_ = getPort(String.valueOf(p_.getSource()),
							DFUtil.classify(p_));
				if (port_ != null)
					port_.doSending(p_);
				port_ = getPort(String.valueOf(p_.getSource()), "all");
				if (port_ != null)
					port_.doSending(p_);
			}
			downPort.doLastSending(data_);
		}
		else if (isGarbageEnabled())
			drop(data_, "label:" + label_);
	}

	/** Enables/disables inspection of individual flows. */
	public void setInspectionEnabled(boolean enabled_)
	{ inspect = enabled_; }

	/** Returns true if inspection of individual flows is enabled. */
	public boolean isInspectionEnabled()
	{ return inspect; }
}
