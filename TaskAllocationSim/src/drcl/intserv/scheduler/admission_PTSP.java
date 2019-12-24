// @(#)admission_PTSP.java   9/2002
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
 * <code>SpecR_PTSP</code> and traffic shaper for <code>traffic_Periodic</code>.  
 * The admission test is conducted by calculating
 * the minimum worst-case traversal time (non-preemptive version).
 * 
 * @see SpecR_PTSP
 * @see drcl.net.traffic.TrafficModel
 * @see drcl.net.traffic.TrafficPeriodic
 * @see drcl.net.traffic.traffic_Periodic
 */
public class admission_PTSP extends Admission 
{
	Queue qrspec = QueueAssistant.getBest(); // priority -> rspec
	
	public void reset()
	{
		super.reset();
		qrspec.reset();
	}

	public SpecFlow setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		if (fspec_ == null || !(fspec_.tspec instanceof TrafficPeriodic)
			|| !(fspec_.rspec instanceof SpecR_PTSP)) {
			if (fspec_ != null) fspec_.handle = -1;
			return null;
		}
		
		fspec_.handle = -1;
		
		SpecFlow old_ = handle_ < 0? null: scheduler.getFlowspec(handle_);
		int oldBW_ = old_ == null? 0: old_.rspec.getBW();
		int oldBuffer_ = old_ == null? 0: old_.rspec.getBuffer();
		SpecFlow suggested_ = null;
		TrafficModel tspec_	= fspec_.tspec;
		SpecR_PTSP rspec_	= (SpecR_PTSP)fspec_.rspec;

		// min. worse-case traversal time:
		// returns a priority, NaN if failed, when failed, aux_.C is mwtt
		if (scheduler.isDebugEnabled())
			scheduler.debug(scheduler + "adm| inputed rspec to calc. mwtt:" + rspec_);
		double mwtt_ = MWTT(handle_, rspec_);
		if (scheduler.isDebugEnabled())
			scheduler.debug(scheduler + "adm| MWTT=" + mwtt_ + "--resulting rspec:" + rspec_);
		if (!Double.isNaN(rspec_.priority)) {
			super.setFlowspec(handle_, tos_, tosmask_, fspec_);
		}
		if (fspec_.handle  < 0) {
			// failed
			suggested_ = (SpecFlow)fspec_.clone();
			((SpecR_PTSP)suggested_.rspec).D = Math.max(mwtt_, rspec_.D);
			int newC_ = Math.min(rspec_.C, scheduler.getAvailableQoSBuffer() + oldBuffer_);
			((SpecR_PTSP)suggested_.rspec).C = newC_;
			((SpecR_PTSP)suggested_.rspec).P = 
				Math.max(rspec_.P, (double)(newC_ << 3) / (scheduler.getAvailableBW() + oldBW_));
		}
		else {
			qrspec.enqueue(rspec_.priority, rspec_);
			traffic_Periodic t_ = new traffic_Periodic(rspec_.C, rspec_.P, tspec_.getMTU());
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

	public SpecAd advertisement(SpecAd adspec_)
	{
		if (adspec_ == null || !(adspec_.tspec instanceof TrafficPeriodic)) return null;
		TrafficModel tspec_ = adspec_.tspec;
		try {
			double P_ = ((TrafficPeriodic)tspec_).getPeriod();
			int C_ = (int)Math.ceil(tspec_.getLoad() * P_ / 8.0);
			SpecAd new_ = super.advertisement(adspec_);
			try {
				// e2e delay 
				double mwtt_ = MWTT(-1, new SpecR_PTSP(C_, P_, Double.MAX_VALUE/*nonzero is ok*/));
				new_.minE2eDelay += mwtt_;

				// inter-packet delay
				new_.minIntPktJitter += mwtt_;
				return new_;
			}
			catch (Exception e_) {
				if (new_ == null) scheduler.drop(e_ + ": cannot create a new adspec");
				else scheduler.drop(e_ + ": should be in MWTT()");
			}
		}
		catch (Exception e_) {
			if (tspec_ == null) scheduler.drop(e_ + ": null tspec");
			else scheduler.drop(e_ + ": super???");
		}
		return null;
	}
	
	// Returns the min. worse-case traversal time; priority is assigned in the rspec;
	// assigned NaN if failed (mwtt > D).
	public double MWTT(int handle_, SpecR_PTSP rspec_) 
	{
		int nr_ = qrspec.getLength();
		rspec_.CC = (double)(rspec_.C << 3) / scheduler.getCapacity();
		
		// exclude the spec specified by the handle_
		SpecR_PTSP excluded_ = handle_ < 0? null: (SpecR_PTSP) scheduler.getRspec(handle_);
		if (excluded_ != null) nr_ --;
		
		double bw_ = scheduler.getCapacity();
		if (nr_ == 0) {// first flow on this link
			rspec_.priority = 0.0/*dont care*/; return rspec_.CC; }
		
		// construct the spec array, exclude one if necessary
		SpecR_PTSP[] specs_ = new SpecR_PTSP[nr_];
		Object[] all_ = qrspec.retrieveAll();
		if (excluded_ == null)
			System.arraycopy(all_, 0, specs_, 0, specs_.length);
		else {
			int i;
			for (i=0; i<all_.length; i++) 
				if (all_[i] == excluded_) break;
			if (i >= all_.length) {// something wrong
				specs_ = new SpecR_PTSP[++nr_];
				System.arraycopy(all_, 0, specs_, 0, specs_.length);
			}
			else {
				System.arraycopy(all_, 0, specs_, 0, i);
				System.arraycopy(all_, i+1, specs_, i, nr_ - i);
			}
		}
		
		double newC_ = rspec_.CC;
		double newD_ = rspec_.D;
		double newP_ = rspec_.P;

			
		// let the new flow have the highest priority
		// calculate the worst case traversal time for each existing flow
		double[] r_ = new double[nr_];
		for (int i=0; i<r_.length; i++)	{
			SpecR_PTSP aux_ = specs_[i];
			double C_ = aux_.CC;
			double D_ = aux_.D;
			double P_ = aux_.P;
			// put the info of the new flow in the position of the i'th
			// fow WTT() to work
			aux_.CC = newC_;
			aux_.P = newP_;
			r_[i] = WTT(C_, D_, specs_, i+1) / D_;
			// put them back
			aux_.CC = C_;
			aux_.P = P_;
		}
			
		// find the min index_, the new calculated wtt of the flows after which 
		// do not exceed their max. allowable delay (D).
		int index_ = r_.length - 1;
		for (; index_>=0; index_--) if (r_[index_] > 1.0) break;
		
		double wtt_ = WTT(newC_, newD_, specs_, index_ + 1);
		if (wtt_ > newD_) {
			// failed
			rspec_.priority = Double.NaN;
		}
		else {
			rspec_.priority  = index_ < 0? 
							   specs_[0].priority - 1.0:
							   index_ == specs_.length-1? 
							   specs_[index_].priority:
							   (specs_[index_].priority + specs_[index_ + 1].priority) / 2.0;
		}
		return wtt_;
	}
	
	// Returns the worse-case traversal time given (<code>C_</code>, <code>D_</code>)
	// and parameters of <code>n_</code> higher-priority flows.
	// @param n_ the first <code>n_</code> elements in <code>specs_</code> are effective.
	double WTT(double C_, double D_, SpecR_PTSP[] specs_, int n_)
	{
		double W_ = C_; // initial value 
		while (W_ <= D_) {
			double newW_ = C_;
			for (int j=0; j<n_; j++) {
				SpecR_PTSP aux_ = specs_[j];
				newW_ += aux_.CC * Math.ceil(W_ / aux_.P);
			}
			if (newW_ <= W_) break; // rounding error?
			else if (newW_ - W_ < 1e-6) { W_ = newW_; break; }
			W_ = newW_;
		}
		return W_;
	}
}
