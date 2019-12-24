// @(#)RunningAverage.java   7/2003
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

package drcl.comp.tool;

import java.io.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.comp.lib.bytestream.*;
import drcl.comp.contract.EventContract;
import drcl.comp.contract.DoubleEventContract;

public class RunningAverage extends Extension
{
	double lastTime = 0.0;
	double lastValue = 0.0;
	double lastResult = 0.0;
	double sum = 0.0;
	long count = 0;
	boolean timeAverage = false;

	public RunningAverage()
	{ super(); }
	
	public RunningAverage(String id_)
	{ super(id_); }

	public void setTimeAverageEnabled(boolean e)
	{ timeAverage = e; }

	public boolean isTimeAverageEnabled()
	{ return timeAverage; }

	public void reset()
	{
		lastTime = lastValue = sum = 0.0;
		lastResult = Double.NaN;
		count = 0;
	}

	public String info()
	{
		return "TimeAverage: " + timeAverage
			   	+ "\ncount = " + count
			   	+ "\nlastTime = " + lastTime
			   	+ "\nlastValue = " + lastValue
			   	+ "\nrunningSum = " + sum
			   	+ "\nlastResult = " + lastResult + "\n";

	}
	
	protected synchronized void process(Object data_, Port inPort_) 
	{
		if (data_ == null) return;
		try {
			double time_ = Double.NaN;
			double value_ = Double.NaN;

			if (data_ instanceof EventContract.Message) {
				EventContract.Message s_ = (EventContract.Message)data_;
				data_ = s_.getEvent();
				time_ = s_.getTime();
			}
			else if (data_ instanceof DoubleEventContract.Message) {
				time_ = ((DoubleEventContract.Message)data_).getTime();
			}
			else
				time_ = getTime();

			if (data_ instanceof Number)
				value_ = ((Number)data_).doubleValue();
			else if (data_ instanceof drcl.data.NumberObj)
				value_ = ((drcl.data.NumberObj)data_).doubleValue();
			else if (data_ instanceof Countable) {
				value_ = (double)((Countable)data_).getSize();
			}
			else if (data_ instanceof DoubleEventContract.Message) {
				value_ = ((DoubleEventContract.Message)data_).getValue();
			}
			else if (data_ instanceof String)
				value_ = Double.parseDouble((String)data_);
			else {
				error("process()", "unrecognized class of objects: "
								+ data_.getClass());
				return;
			}

			inPort_.doLastSending(new double[]{
					time_, 
					timeAverage? _timeAvg(time_, value_): _avg(value_)});
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("close()", e_);
		}
	}


	double _avg(double value_)
	{
		count++;
		sum += value_;
		lastResult = sum / count;
		return lastResult;
	}

	double _timeAvg(double time_, double value_)
	{
		count++;
		sum += lastValue * (time_ - lastTime);
		lastTime = time_;
		lastValue = value_;
		lastResult = time_ > 0.0? sum/time_: 0.0;
		return lastResult;
	}
}
