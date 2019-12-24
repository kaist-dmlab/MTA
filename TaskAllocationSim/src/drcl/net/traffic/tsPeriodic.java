// @(#)tsPeriodic.java   9/2002
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

package drcl.net.traffic;

import java.util.*;
import drcl.util.queue.*;

/**
 * This class implements a traffic shaper that conforms to the {@link traffic_Periodic periodic}
 * traffic model.
 * @see traffic_Periodic
 */
public class tsPeriodic extends TrafficShaper
{
	double pend; // end of the current period
	int remaining; // in this period
	traffic_Periodic traffic = null;

	public tsPeriodic()
	{ this(new traffic_Periodic()); }
	
	public tsPeriodic(traffic_Periodic traffic_)
	{ super(); traffic = traffic_; reset(); }
	
	public void reset()
	{
		super.reset();
		if (traffic != null) {
			pend = traffic.P;
			remaining = traffic.C;
		}
	}

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+ "State: end of current period=" + pend + ", remaining="
			+ remaining + "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_Periodic)traffic_; reset(); }
	
	protected double adjust(double now_, int size_) 
	{
		int C_ = traffic.C;
		double P_ = traffic.P;
		if (size_ > C_) return Double.NaN;
		
		if (now_ < pend) {
			// adjust the end of period so that it can accommodate
			// more (bursty?) traffic
			if (remaining == C_) pend = now_ + P_;
			
			remaining -= size_;
			if (remaining < 0) {
				// start a new period
				remaining = C_ - size_;
				pend += P_;
				return pend - P_ - now_;
			}
			else if (now_ >= pend - P_) return 0.0;
			else return pend -P_ - now_;
		}
		else { // new period
			pend = now_ + P_;
			remaining = C_ - size_;
			return 0.0;
		}
	}

	public void setMaxPacketSize(int size_) { traffic.maxPacketSize = size_; }
	public int getMaxPacketSize() { return traffic.maxPacketSize; }
	
	public void setC(int c_) { traffic.C = c_; }
	public int getC() { return traffic.C; }
	
	public void setPeriod(double p_) { traffic.P = p_; }
	public double getPeriod() { return traffic.P; }

	public void set(int c_, double p_, int mtu_)
	{ traffic.set(c_, p_, mtu_); reset(); }
}
