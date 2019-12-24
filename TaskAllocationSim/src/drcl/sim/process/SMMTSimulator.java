// @(#)SMMTSimulator.java   9/2002
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

package drcl.sim.process;

import java.beans.*;
import java.util.*;
import drcl.comp.*;
import drcl.Debug;
import drcl.util.queue.*;
import drcl.util.StringUtil;

/**
 * This class implements a <i>Single-Machine Multi-Threaded</i> simulation
 * engine.
 */
public class SMMTSimulator extends ARuntime
{
	public static final String Debug_LEAP_FORWARD = "LEAP";

	public SMMTSimulator()
	{ this("SMMT");	}
	
	public SMMTSimulator(String name_)
	{	
		super(name_);
		//setTimeScale(1.0e200); //1.0e9); // us level
		setTimeScale(Double.POSITIVE_INFINITY); //1.0e9); // us level
		addDebugLevel(Debug_LEAP_FORWARD);
	}
	
	double timeDiff = 0.0;

	//
	private void ___OVERRIDES___() {}
	//
	
	public void reset()
	{
		super.reset();
		timeDiff = 0.0;
	}
	
	// Advance in time to the first sleeping thread in Q.
	protected void systemBecomesIdle(drcl.comp.AWorkerThread exe_)
	{
		//if (state == State_SUSPENDED || state == State_INACTIVE) return;
		if (state == State_SUSPENDED) return;
		if (getQ().isEmpty()) return;
		
		// adjust the system time to that of the nearest future event.
		double time_ = getQ().firstKey();
		
		double newTimeDiff_ = timeScaleReciprocal == 0.0? time_:
			time_ - timeScaleReciprocal * (System.currentTimeMillis()
				- startTime);

		if (debug && isDebugEnabledAt(Debug_LEAP_FORWARD))
			println(Debug_LEAP_FORWARD, null, "-->" + time_);
		
		timeDiff = newTimeDiff_;
	}

	protected double _getTime() 
	{
		if (state == State_SUSPENDED) return time;
		if (timeScaleReciprocal == 0.0)
			time = timeDiff;
		else {
			if (state != State_INACTIVE)
				ltime = System.currentTimeMillis() - startTime;
			time = (double)ltime * timeScaleReciprocal + timeDiff; 
		}
		return time;
	}

	protected String t_info(String prefix_)
	{
		if (state != State_SUSPENDED && state != State_INACTIVE)
			ltime = System.currentTimeMillis() - startTime;
		StringBuffer sb_ = new StringBuffer();
		sb_.append(prefix_ + "timeScale     = " + (timeScale/1.0e3)
					+ " (wall time/virtual time)\n");
		sb_.append(prefix_ + "1.0/timeScale = " + (timeScaleReciprocal*1.0e3)
					+ " (virtual time/wall time)\n");
		sb_.append(prefix_ + "startTime     = " + startTime + "\n");
		sb_.append(prefix_ + "ltime         = " + ltime + "\n");
		sb_.append(prefix_ + "timeDiff      = " + timeDiff + "\n");
		sb_.append(prefix_ + "currentTime   = " + _getTime() + "\n");
		return sb_.toString();
	}
}
