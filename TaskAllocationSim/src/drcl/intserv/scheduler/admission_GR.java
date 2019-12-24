// @(#)admission_GR.java   9/2002
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

import drcl.comp.Component;
import drcl.intserv.*;
import drcl.net.traffic.*;

/**
 * General Rate Admission:
 * works with any <code>TrafficModel</code> and any <code>SpecR</code> that implements
 * <code>SpecR_Direct</code>.
 * 
 * <p>
 * For path advertisement,
 * the minimum end-to-end queueing delay is calculated using the following formula:
<pre>	buffer required at last hop = burst_ + hop * MTU<br>
	end-to-end delay = (buffer required at last hop) / minBW;</pre>
 * 
 * <p>
 * The admission installs the bandwidth and buffer specified in the rspec
 * to the scheduler.
 * 
 * @see drcl.intserv.SpecR
 * @see SpecR_Direct
 * @see drcl.net.traffic.TrafficModel
 */
public class admission_GR extends Admission
{
	public SpecFlow setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		if (fspec_ == null || fspec_.tspec == null
			|| !(fspec_.rspec instanceof SpecR_Direct)) {
			if (fspec_ != null) fspec_.handle = -1;
			return null;
		}
		
		SpecFlow old_ = handle_ < 0? null: scheduler.getFlowspec(handle_);
		int oldBW_ = old_ == null? 0: old_.rspec.getBW();
		int oldBuffer_ = old_ == null? 0: old_.rspec.getBuffer();
		SpecFlow suggested_ = null;
		TrafficModel tspec = fspec_.tspec;
		SpecR_Direct rspec = (SpecR_Direct)fspec_.rspec;

		// Till this step, we know that end-to-end delay and inter-packet jitter
		// are satisfied (see translate()). We only need to check buffer amount.
		int tload_  = (int)Math.ceil(tspec.getLoad());
				
		// Redundant step: (as have been done in the end system)
		// Check if bandwidth satisfy Spec_R, is the reservered rate greater
		// or equal to the Tspec rate.
		if ( rspec.getBW() < tload_ ) {
			suggested_ = (SpecFlow) fspec_.clone();
			((SpecR_Direct)suggested_.rspec).setBW(tload_);
			fspec_.handle = -1;
			if (scheduler.isDebugEnabled())
				scheduler.debug("Resved BW is less than the load of tspec: " + fspec_);
		}
		else {
			super.setFlowspec(handle_, tos_, tosmask_, fspec_);
			if (fspec_.handle < 0) {
				suggested_ = (SpecFlow) fspec_.clone();
				((SpecR_Direct)suggested_.rspec).setBW( 
					Math.min(rspec.getBW(), scheduler.getAvailableBW() + oldBW_));
				((SpecR_Direct)suggested_.rspec).setBuffer(
					Math.min(rspec.getBuffer(), scheduler.getAvailableQoSBuffer() + oldBuffer_));
			}
		}
		
		return fspec_.handle >= 0? fspec_: suggested_;
	}
	
	public SpecFlow removeFlowspec(int handle_)
	{ 
		return super.removeFlowspec(handle_);
	}

	public SpecAd advertisement(SpecAd  adspec_)
	{	
		if (adspec_ == null) return null;
		
		TrafficModel tspec_ = adspec_.tspec;
		SpecAd new_ = super.advertisement(adspec_);
		
		// e2e delay = (burst_ + hop * mtu) / minBW + acc. prop. delay;
		new_.minE2eDelay = (double)((tspec_.getBurst() + new_.hop * new_.minMTU) << 3)/ new_.minBW;
		
		// inter-packet delay
		new_.minIntPktJitter = new_.minE2eDelay;
		
		if (scheduler.isDebugEnabled())
			scheduler.debug("Called advertisement, hopcount = " + new_.hop + "| " + scheduler);

		return new_;
	}

	public void reset()
	{ super.reset(); }

	public void duplicate(Object source_)
	{	super.duplicate(source_);	}
}
