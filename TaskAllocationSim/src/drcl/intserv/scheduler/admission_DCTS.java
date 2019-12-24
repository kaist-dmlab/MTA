// @(#)admission_DCTS.java   9/2002
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

//  The addition of new session may affect existing sessions
//  if they used specialized D in admission. Thus, let's use
//  original D in admission control. Now the admission control
//  contains two steps: 1. hop * oldD <= Deadline. 
//  2. specialized density <= 1

import java.util.*;

import drcl.data.*;
import drcl.comp.Component;
import drcl.intserv.*;
import drcl.net.traffic.*;
import drcl.util.scalar.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;

/**
 * Works with <code>TrafficModel</code> that implements <code>TrafficPeriodic</code>,
 * <code>SpecR_DCTS</code> and traffic shaper for <code>traffic_CDSmooth</code>.  
 * The admission test is the non-preemptive version.
 * 
 * @see SpecR_DCTS
 * @see drcl.net.traffic.TrafficModel
 * @see drcl.net.traffic.TrafficPeriodic
 * @see drcl.net.traffic.traffic_CDSmooth
 */
public class admission_DCTS extends Admission 
{
	Queue qrspec = QueueAssistant.getBest(); // D -> rspec
	
	public void reset()
	{
		super.reset();
		if (qrspec != null) qrspec.reset();
	}

	public SpecFlow setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		if (fspec_ == null || !(fspec_.tspec instanceof TrafficPeriodic)
			|| !(fspec_.rspec instanceof SpecR_DCTS)) {
			if (fspec_ != null) fspec_.handle = -1;
			return null;
		}
		
		fspec_.handle = -1;
		
		// info of previous reservation
		SpecFlow old_ = handle_ < 0? null: scheduler.getFlowspec(handle_);
		int oldBW_ = old_ == null? 0: old_.rspec.getBW();
		int oldBuffer_ = old_ == null? 0: old_.rspec.getBuffer();
		SpecFlow suggested_ = null;
		TrafficModel tspec_	= fspec_.tspec;
		SpecR_DCTS rspec_ = (SpecR_DCTS)fspec_.rspec;
		rspec_.CC = (double)(rspec_.C << 3) / scheduler.getCapacity();

		// returns a suggestion if failed
		SpecR_DCTS new_ = test(handle_, rspec_);
		if (new_ == null) super.setFlowspec(handle_, tos_, tosmask_, fspec_);
		if (fspec_.handle < 0) {
			// failed
			suggested_ = (SpecFlow)fspec_.clone();
			if (new_ == null) new_ = (SpecR_DCTS)suggested_.rspec;
			// check available resource
			new_.C = Math.min(new_.C, (int)(scheduler.getAvailableQoSBuffer() + oldBuffer_));
			new_.D = Math.max(new_.D, (double)(new_.C << 3) / (scheduler.getAvailableBW() + oldBW_));
			suggested_.rspec = new_;
		}
		else {
			qrspec.enqueue(rspec_.D, rspec_);
			traffic_CDSmooth t_ = new traffic_CDSmooth(rspec_.C, rspec_.D, tspec_.getMTU());
			scheduler.addShaper(fspec_.handle, tos_, tosmask_, 
				(drcl.net.traffic.TrafficShaper)drcl.net.traffic.TrafficAssistant.getTrafficShaper(t_));
		}
		return fspec_.handle >= 0? fspec_: suggested_;
	}

	public SpecFlow removeFlowspec(int handle_)
	{
		SpecFlow f_ = super.removeFlowspec(handle_);
		if (f_ != null) {
			qrspec.remove(f_.rspec);
			scheduler.removeShaper(handle_);
		}
		return f_;
	}
	
	public SpecAd advertisement(SpecAd  adspec_)
	{
		if (adspec_ == null || !(adspec_.tspec instanceof TrafficPeriodic)) return null;
		TrafficModel tspec_ = adspec_.tspec;
		double D_ = ((TrafficPeriodic)tspec_).getPeriod();
		int C_ = (int)Math.ceil(tspec_.getLoad() * D_ / 8.0);
		SpecAd new_ = super.advertisement(adspec_);
		
		// e2e delay 
		new_.minE2eDelay += D_;

		// inter-packet delay
		new_.minIntPktJitter += D_;

		return new_;
	}

	// Test if the system utilization exceeds 1.0 after
	// adding the new flow.
	// @return null if ok; a suggestion of rspec if failed.
	SpecR_DCTS test(int handle_, SpecR_DCTS rspec_)
	{
		int nr_ = qrspec.getLength();
		
		// exclude the spec specified by the handle_
		SpecR_DCTS excluded_ = handle_ < 0? null: (SpecR_DCTS) scheduler.getRspec(handle_);
		if (excluded_ != null) nr_ --;
		if (nr_ == 0) // first flow on this link
			return null;
		
		// construct the spec array, exclude one if necessary
		SpecR_DCTS[] specs_ = new SpecR_DCTS[nr_+1];
		Object[] all_ = qrspec.retrieveAll();
		if (excluded_ == null)
			System.arraycopy(all_, 0, specs_, 0, all_.length);
		else {
			int i;
			for (i=0; i<all_.length; i++) 
				if (all_[i] == excluded_) break;
			if (i >= all_.length) {// something wrong
				specs_ = new SpecR_DCTS[++nr_];
				System.arraycopy(all_, 0, specs_, 0, all_.length);
			}
			else {
				System.arraycopy(all_, 0, specs_, 0, i);
				System.arraycopy(all_, i+1, specs_, i, nr_ - i);
			}
		}
		
		// insert the new flow
		double newC_ = rspec_.CC;
		double newD_ = rspec_.D;
		SpecR_DCTS tmp_ = (SpecR_DCTS)rspec_.clone();
		int index_;
		for (index_=0; index_<nr_; index_++) if (specs_[index_].D >= newD_) break;
		if (index_ < nr_) 
			System.arraycopy(specs_, index_, specs_, index_+1, nr_ - index_);
		specs_[index_] = tmp_;

		// the followings are for constructing suggestion rspec
		double min_ = Double.MAX_VALUE; // minimum reduction in CC
		
		// specialization and test
		for (int i=0; i<specs_.length; i++) {
			double r_ = specs_[i].D;
			double d_ = 0.0; // density
			double newflowD_ = 0.0; // store new_d for the new flow
			// Specialization wrt r_
			for (int j=0; j<specs_.length; j++) {
				// new D = r_ * 2 ^ ( floor( log2(D / r_) ) )
				double newd_ = r_ * Math.pow(2.0, Math.floor(Math.log(specs_[j].D / r_) / Math.log(2.0)));
				d_ += specs_[j].CC / newd_;
				if (j == index_) newflowD_ = newd_;
			}

			if (d_ <= 1.0) return null;
			else {
				newflowD_ = (d_ - 1.0) * newflowD_;
				if (min_ > newflowD_) min_ = newflowD_;
			}
		}

		// construct suggestion
		// min_: portion of CC that is over capacity
		tmp_.CC -= min_;
		tmp_.C = (int)Math.ceil(tmp_.CC * scheduler.getCapacity() / 8.0);
		return tmp_;
	}
}
