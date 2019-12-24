// @(#)Admission.java   9/2002
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
import drcl.net.*;

/**
 * This class is equipped on {@link Scheduler} to enable runtime admission control
 * on defines a unified interface for other protocols 
 * (such as QoS-based routing and resource management protocols) to make/modify/release
 * a reservation, and to access installed reservations.
 * 
 * <p>
 * A reservation is defined in a {@link SpecFlow flowspec} which contains 
 * a {@link drcl.net.traffic.TrafficModel tspec} and a {@link SpecR rspec}.
 * The tspec specifies the aggregate or the "maximum" traffic of the interested senders.
 * The rspec specifies the parameters used by the packet scheduler for serving the flow.
 * 
 * @see Scheduler
 */
public class Admission extends drcl.DrclObj
{
	/** The scheduler being associated with this admission module. */
	protected Scheduler scheduler;
	
	/**
	 Installs a new flowspec.
	 The method calls {@link #setFlowspec(int, long[], long[], SpecFlow)
	 setFlowspec(-1, new long[]{tos_}, null, fspec_)}.
	 */
	public final SpecFlow addFlowspec(long tos_, SpecFlow fspec_)
	{
		return setFlowspec(-1, new long[]{tos_}, (long[])null, fspec_);
	}
	
	/**
	 Installs a new flowspec.
	 The method calls {@link #setFlowspec(int, long[], long[], SpecFlow)
	 setFlowspec(-1, new long[]{tos_}, new long[]{tosmask_}, fspec_)}.
	 */
	public final SpecFlow addFlowspec(long tos_, long tosmask_, SpecFlow fspec_)
	{
		return setFlowspec(-1, new long[]{tos_}, new long[]{tosmask_}, fspec_);
	}
	
	/**
	 Installs a new flowspec.
	 The method calls {@link #setFlowspec(int, long[], long[], SpecFlow)
	 setFlowspec(-1, tos_, tosmask_, fspec_)}.
	 */
	public final SpecFlow addFlowspec(long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		return setFlowspec(-1, tos_, tosmask_, fspec_);
	}
	
	/**
	 * Installs a new flowspec or changes an existing flowspec
	 * with the specified handle.
	 * This method checks if the available resources (buffer and bandwidth)
	 * are sufficient for the flow.  It returns null if the flow passes;
	 * returns a suggested flowspec if failed.
	 * 
	 * Also, the "handle" field in <code>fspec_</code> is set to either
	 * a valid value (success) or a negative value (failure).
	 *
	 * A subclass may need to override this method to create its own
	 * data structure for the successfully installed flow.
	 * 
	 * @param handle_ the handle assigned to this flow, -1 if installing a new flow.
	 * @param tos_ ID's of flows that share the flowspec.
	 * @param tosmask_ masks of flow ID's.
	 * @param fspec_ the flowspec.
	 * @return null if the installation succeeds or a suggested flowspec if failed.
	 */
	public SpecFlow setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		if (scheduler == null || fspec_ == null || fspec_.rspec == null
			|| (handle_ < 0 && (tos_ == null || tos_.length == 0))) return null;
		
		SpecFlow oldfspec_ = scheduler.getFlowspec(handle_);
		
		// enough resource?
		int bw_ = fspec_.rspec.getBW() - (oldfspec_ == null? 0: oldfspec_.rspec.getBW());
		int buffer_ = fspec_.rspec.getBuffer() - (oldfspec_ == null? 0: oldfspec_.rspec.getBuffer());
		
		if (scheduler.resvBW(bw_)) {
			if (!scheduler.resvQoSBuffer(buffer_)) {
				if (scheduler.isDebugEnabled()) 
					scheduler.debug(scheduler + "adm.setFlowspec()| not enough buffer, needs "
									+ buffer_ + " but has " + scheduler.getAvailableQoSBuffer());
				scheduler.releaseBW(bw_); return null; }
		}
		else {
			if (scheduler.isDebugEnabled()) 
				scheduler.debug(scheduler + "adm.setFlowspec()| not enough BW, needs " + bw_
								+ " but has " + scheduler.getAvailableBW());
			return null;
		}
		
		scheduler.setFlowspec(handle_, tos_, tosmask_, fspec_);
		if (scheduler.isDebugEnabled()) {
			if (fspec_.handle == -1) scheduler.debug(scheduler + "adm.setFlowspec() failed in scheduler.setFlowspec()");
			else scheduler.debug(scheduler + "adm.setFlowspec() succeeded| fspec installed:" + fspec_);
		}
		return null; 
	}
	
	/**
	 * Removes an installed flowspec by its handle.
	 * 
	 * This method releases the resources (buffer and bandwidth) reserved for the flow.
	 * A subclass may need to override this method to further release any data structure
	 * allocated for the flow.
	 * @return the removed flowspec.
	 */
	public SpecFlow removeFlowspec(int handle_)
	{
		if (scheduler == null) return null;
		
		SpecFlow fspec_ = scheduler.getFlowspec(handle_);
		if (fspec_ != null) {
			scheduler.releaseBW(fspec_.rspec.getBW());
			scheduler.releaseQoSBuffer(fspec_.rspec.getBuffer());
		}
		if (scheduler.isDebugEnabled()) 
			scheduler.debug(scheduler + "adm | fspec removed:" + scheduler.getFlowspec(handle_));
		
		return scheduler.removeFlowspec(handle_); 
	}

    /**
	 * Returns a new Adspec that summarizes the QoS of the path from the sender up to the current hop.
	 * Specifically, this method updates hop, packet loss rate, MTU, propagation delay
	 * and bandwidth.
	 * Subclasses should override this method to update end-to-end delay and jitter.
	 */
	public SpecAd advertisement(SpecAd  adspec_)
	{
		SpecAd new_ = (SpecAd)adspec_.clone();
		
		// hop
		int	   hop_	= ++new_.hop;
		
		// pkt loss rate
		new_.pktLossRate = 1.0 - (1.0 - new_.pktLossRate) * scheduler.pktLossRate;
		
		// MTU
		double mtu_	= new_.minMTU = Math.min(new_.minMTU, scheduler.mtu);
		
		// prop delay
		new_.minPropDelay += scheduler.getPropDelay();
		
		// bandwidth
		double bw_ = new_.minBW = Math.min(new_.minBW, scheduler.getAvailableBW());
		
		return new_;
	}

	/** Resets to the initial state. */
	public void reset()
	{
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	public String info()
	{
		return scheduler.resvinfo();
	}
	
	//
	static void ___SCRIPT___() {}
	//
	
	public void removeAllFlowspecs()
	{
		if (scheduler != null) scheduler.removeAllFlowspecs();
	}
	
	/** Returns the associated scheduler. */
	public Scheduler getScheduler() 
	{ return scheduler; }
}

