// @(#)TrafficAssistant.java   7/2003
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

package drcl.net.traffic;

/**
<code>TrafficAssistant</code> is a utility class which helps to get the
corresponding {@link TrafficShaper} or {@link TrafficSourceComponent}
from a {@link TrafficModel}.
           
<p>To ease the task, TrafficAssistant expects:
<ol>
<li>The name of a traffic model class starts with "traffic_".
For example, traffic_TokenBucket.
<li>The name of a TrafficShaper class or a TrafficSourceComponent class
starts with "ts".  For example, tsTokenBucket.
</ol>
*/
public class TrafficAssistant
{
	/** Returns a {@link TrafficSourceComponent} instance for the given source
	 * traffic model and the shaper traffic model.*/
	public static TrafficSourceComponent getTrafficSource(TrafficModel traffic_,
					TrafficModel shaperTraffic_)
	{
		TrafficSourceComponent source_ =
			(TrafficSourceComponent)getTrafficComponent(traffic_);
		source_.setShaper(getTrafficShaper(shaperTraffic_));
		return source_;
	}

	/** Returns a {@link TrafficComponent} instance for the given traffic model.
	The component returned may be a {@link TrafficSourceComponent} or a
	{@link TrafficShaperComponent},
	depending on the nature of the traffic model.
	*/
	public static TrafficComponent getTrafficComponent(TrafficModel traffic_)
	{
		if (traffic_ instanceof traffic_PeakRate) {
			if (traffic_ instanceof traffic_FixedPoints)
				return new tsFixedPoints((traffic_FixedPoints)traffic_);
			else
				return new tsPeakRate((traffic_PeakRate)traffic_);
		}
		if (traffic_ instanceof traffic_TokenBucket)
			return new TrafficShaperComponent(
							new tsTokenBucket((traffic_TokenBucket)traffic_));
		if (traffic_ instanceof traffic_CDSmooth)
			return new TrafficShaperComponent(
							new tsCDSmooth((traffic_CDSmooth)traffic_));
		if (traffic_ instanceof traffic_RTSmooth)
			return new TrafficShaperComponent(
							new tsRTSmooth((traffic_RTSmooth)traffic_));
		if (traffic_ instanceof traffic_PacketTrain)
			return new tsPacketTrain((traffic_PacketTrain)traffic_);
		if (traffic_ instanceof traffic_ExpOnOff)
			return new tsExpOnOff((traffic_ExpOnOff)traffic_);
		if (traffic_ instanceof traffic_ParetoOnOff)
			return new tsParetoOnOff((traffic_ParetoOnOff)traffic_);
		if (traffic_ instanceof traffic_Periodic)
			return new TrafficShaperComponent(
							new tsPeriodic((traffic_Periodic)traffic_));
		if (traffic_ instanceof traffic_OnOff)
			return new tsOnOff((traffic_OnOff)traffic_);

		Class class_ = null;
		try {
			String className_ = traffic_.getClass().getName();
			// suppose component is in the same package as the traffic model
			// class.  So className is in the form of 
			// "<PackageName>.traffic_XYZ", convert it to 
			// "<PackageName>.tsXYZ"
			int index_ = className_.lastIndexOf("."); 
			String packageName_ = index_ >=0? 
					className_.substring(0, index_+1): "";
			className_ = index_ >=0? 
					className_.substring(index_+1): className_;

			className_ = packageName_ + "ts" + className_.substring(8);
			class_ = Class.forName(className_);
			TrafficComponent tc_ = null;
			if (TrafficShaper.class.isAssignableFrom(class_))
				tc_ = new TrafficShaperComponent(
								(TrafficShaper)class_.newInstance());
			else
				tc_ = (TrafficComponent)class_.newInstance();
			tc_.setTrafficModel(traffic_);
			return tc_;
		}
		catch (ClassNotFoundException e_) {
			drcl.Debug.error("Cannot find traffic component for "
							+ traffic_.getClass());
		}
		catch (InstantiationException e_) {
			drcl.Debug.error(e_.toString());
		}
		catch (IllegalAccessException e_) {
			drcl.Debug.error(e_.toString());
		}
		return null;
	}

	/** Returns a {@link TrafficShaper} instance for the given traffic model.*/
	public static TrafficShaper getTrafficShaper(TrafficModel traffic_)
	{
		// FIXME: hard-coded
		if (traffic_ instanceof traffic_TokenBucket)
			return new tsTokenBucket((traffic_TokenBucket)traffic_);
		if (traffic_ instanceof traffic_CDSmooth)
			return new tsCDSmooth((traffic_CDSmooth)traffic_);
		if (traffic_ instanceof traffic_RTSmooth)
			return new tsRTSmooth((traffic_RTSmooth)traffic_);
		if (traffic_ instanceof traffic_Periodic)
			return new tsPeriodic((traffic_Periodic)traffic_);

		try {
			String className_ = traffic_.getClass().getName();
			// suppose className is in the form of "traffic_XYZ",
			// 	convert it to "tsXYZ"
			Class class_ = Class.forName("ts" + className_.substring(8));
			TrafficShaper ts_ = (TrafficShaper)class_.newInstance();
			ts_.setTrafficModel(traffic_);
			return ts_;
		}
		catch (Exception e_) {
			drcl.Debug.error("Cannot find traffic shaper class for "
							+ traffic_.getClass());
			return null;
		}
	}
}
