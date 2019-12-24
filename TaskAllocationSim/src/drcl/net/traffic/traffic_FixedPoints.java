// @(#)traffic_FixedPoints.java   7/2003
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

import java.util.Vector;
import drcl.data.DoubleObj;

/**
 * This class allows one to specify packets to be generated at fixed time points.
 * The sizes of packets are in the uniform distribution as specified in the
 * {@link traffic_PeakRate peak rate} model.
 *
 * In addition to the parameters defined in the {@link traffic_PeakRate peak rate}
 * model, this class also defines:
 * <dl>
 * <dt>Start Time
 * <dd>Time to start generating packets according to the peak-rate model.
 * <dt>Fixed time points
 * <dd>The time points to generate packets.
 * </dl>
 */
public class traffic_FixedPoints extends traffic_PeakRate 
{
	public double startTime;
	public Vector fp;
	
	public traffic_FixedPoints()
	{}

	public traffic_FixedPoints(
					int min_, int max_, double miniat_, double maxiat_)
	{
		super(min_, max_, miniat_, maxiat_);
		startTime = 0.0;
		fp = new Vector(0);
	}
	
	public traffic_FixedPoints(
					int min_, int max_, double miniat_, double maxiat_,
				   	double startTime_, double[] timepoints_)
	{
		super(min_, max_, miniat_, maxiat_);
		startTime = startTime_;
		setTimePoints(timepoints_);
	}
	public void set(int min_, int max_, double miniat_, double maxiat_)
	{
		super.set(min_, max_, miniat_, maxiat_);
		startTime = 0.0;
		fp = new Vector(0);
	}
	
	public void set(int min_, int max_, double miniat_, double maxiat_,
		double startTime_, double[] timepoints_)
	{
		super.set(min_, max_, miniat_, maxiat_);
		startTime = startTime_;
		setTimePoints(timepoints_);
	}


	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		traffic_FixedPoints that_ = (traffic_FixedPoints) source_;
		startTime = that_.startTime; 
		fp = (Vector) that_.fp.clone();
	}
		
	public String oneline()
	{ return super.oneline() + ", startTime=" + startTime + ", FixedPoints=" + fp; }
	
	protected void finalize()
	{ fp.removeAllElements(); }
	
	//
	private void ___PROPERTY___() {}
	//
	
	public void setStartTime(double time_) { startTime = time_; }
	public double getStartTime() { return startTime; }

	public void addTimePoint(double tp_) 
	{   
		int i;
		for (i = fp.size() - 1; i >= 0; i--)
			if (((DoubleObj)(fp.elementAt(i))).value <= tp_)
				break;
		if (++i >= fp.size())
			fp.addElement(new DoubleObj(tp_));
		else
			fp.insertElementAt(new DoubleObj(tp_), i);
	}

	public void addTimePoints(double[] atp_) 
	{
		if (atp_ == null) return;
		if (fp == null) setTimePoints(atp_);
		else {
			for (int i=0; i<atp_.length; i++)
				addTimePoint(atp_[i]);
		}
	}

	public void setTimePoints(double[] atp_)
	{
		if (atp_ == null) return;
		if (fp == null) fp = new Vector(atp_.length);
		else {
			if (fp.size() < atp_.length) fp.setSize(atp_.length);
			fp.removeAllElements();
		}
		for (int i=0; i<atp_.length; i++)
			addTimePoint(atp_[i]);
	}

	public double[] getTimePoints()
	{
		if (fp == null) return new double[0];
		double atp_[] = new double[fp.size()];
		for (int i=0; i<atp_.length; i++)
			atp_[i] = ((DoubleObj)fp.elementAt(i)).value;
		return atp_;
	}
}
