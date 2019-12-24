// @(#)tsCDSmooth.java   9/2002
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

import drcl.data.LongObj;
import drcl.data.DoubleObj;
import drcl.util.queue.*;
import drcl.util.queue.Queue;

/**
 * A traffic shaper that conforms to the {@link traffic_CDSmooth (C,D)-smooth}
 * traffic model.
 * @see traffic_CDSmooth
 */
public class tsCDSmooth extends TrafficShaper
{
	Queue qrestrict = QueueAssistant.getBest();
	// time -> LongObj (# of bits allowed at the time)
	double nextTime;
	traffic_CDSmooth traffic = null;
	
	public tsCDSmooth()
	{ this(new traffic_CDSmooth()); }

	public tsCDSmooth(traffic_CDSmooth traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void reset()
	{
		super.reset();
		if (qrestrict != null) qrestrict.reset();
		nextTime = 0.0;
	}

	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.info(prefix_) + prefix_
				+ "State: nextTime=" + nextTime + ", restrictions:");
		if (qrestrict == null || qrestrict.isEmpty())
			sb_.append("none\n");
		else {
			double[] times_ = qrestrict.keys();
			Object[] restricts_ = qrestrict.retrieveAll();
			for (int i=0; i<times_.length; i++)
				sb_.append("(" + times_[i] + "," + restricts_[i] + ")");
			sb_.append("\n");
		}
		return sb_.toString();
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_CDSmooth)traffic_; reset(); }
	
	protected double adjust(double now_, int size_) 
	{
		int C_ = traffic.C;
		double D_ = traffic.D;
		if (size_ > C_) return Double.NaN;
		
		nextTime = Math.max(now_, nextTime);
		
		if (qrestrict == null) qrestrict = QueueAssistant.getBest();
		
		if (!qrestrict.isEmpty()) {
			Enumeration times_ = qrestrict.getKeyEnumerator();
			Enumeration restricts_ = qrestrict.getElementEnumerator();
			for (; times_.hasMoreElements(); ) {
				DoubleObj timeObj_ = (DoubleObj)times_.nextElement();
				LongObj restrictObj_ = (LongObj) restricts_.nextElement();
				if (timeObj_.value <= nextTime) {
					// remove it
					qrestrict.remove(restrictObj_);
				}
				else if (restrictObj_.value < size_) {
					nextTime = timeObj_.value;
					// remove it
					qrestrict.remove(restrictObj_);
				}
				else {
					restrictObj_.value -= size_;
				}
			}
		}
			
		// add restriction
		if (qrestrict.isEmpty() || 
			Math.abs(nextTime + D_ - qrestrict.lastKey()) > 1e-6) // rouding error
			qrestrict.enqueue(nextTime + D_, new LongObj(C_ - size_));
		
		return nextTime - now_;
	}

	public void setMaxPacketSize(int size_)
	{ traffic.maxPacketSize = size_; }

	public int getMaxPacketSize()
	{ return traffic.maxPacketSize; }
	
	public void setC(int c_)
	{ traffic.C = c_; }

	public int getC()
	{ return traffic.C; }
	
	public void setD(double d_)
	{ traffic.D = d_; }

	public double getD()
	{ return traffic.D; }

	public void set(int c_, double d_, int mtu_)
	{ traffic.set(c_, d_, mtu_); reset(); }
}
