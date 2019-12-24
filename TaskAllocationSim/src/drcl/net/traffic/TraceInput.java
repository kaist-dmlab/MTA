// @(#)TraceInput.java   9/2002
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

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import drcl.comp.*;

/**
<code>TraceInput</code> output packets to <code>down@</code> port based on a trace file.
To use the class, one must assigned to it a <code>java.io.Reader</code> which is associated with
the opened trace file.

<p>A subclass must override {@link #setNextPacket(drcl.net.FooPacket)} to 
set the size of next packet and return its birth time based on the trace file.
In addition, a subclass also needs to implement the looping operation.
When the looping flag is enabled, the trace file is repeated every <code>loopPeriod</code> second.
The associated traffic model is made up by the load, burst and MTU properties.
 */
public abstract class TraceInput extends TrafficSourceComponent
{
	static final long FLAG_LOOP_ENABLED  = 1L << FLAG_UNDEFINED_START;

	public TraceInput()
	{ super(); }

	public TraceInput(String id_)
	{ super(id_); }

	protected transient Reader reader;
	double loopPeriod = 0.0;

	protected abstract double setNextPacket(drcl.net.FooPacket nextpkt_);

	public void reset()
	{
		super.reset();
		if (isLoopEnabled())
			loopingTest(false/*dont mark*/);
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof TraceInput)) return;
		super.duplicate(source_);
		TraceInput that_ = (TraceInput) source_;
	}
	
	public String info()
	{
		return super.info()
			+ (isLoopEnabled()? "LoopEnabled: period = " + loopPeriod + "\n": "");
	}
	
	/** Loads the trace file into this component. */
	public void load(String filename_)
	{
		try{
			setReader(new FileReader(filename_));
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}		
	}

	public void setReader(Reader r_)
	{
		reader = r_;
		if (isLoopEnabled())
			loopingTest(true);
	}

	public Reader getReader()
	{ return reader; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = traffic_; }

	public TrafficModel getTrafficModel()
	{ return traffic; }
	
	public double load;
	public int mtu;
	public int burst;
	
	TrafficModel traffic = new TrafficModel() {
		public double getLoad() { return load; }
		public TrafficModel merge(TrafficModel that_) {return this;}			
		public int getMTU() { return mtu; }
		public int getBurst() { return burst; }
		public String oneline() { return "<TraceFile>"; }
	};
	
	public void setLoad(double load_) { load = load_; }
	public double getLoad() { return load; }
	
	public void setBurst(int b_) { burst = b_; }
	public int getBurst() { return burst; }
	
	public void setMTU(int mtu_) { mtu = mtu_; }
	public int getMTU() { return mtu; }

	/** Returns true if looping the trace is enabled.*/
	public boolean isLoopEnabled()
	{ return getComponentFlag(FLAG_LOOP_ENABLED) != 0; }
	
	/** Enables/disables looping. */
	public void setLoopEnabled(boolean v_)
	{
		setComponentFlag(FLAG_LOOP_ENABLED, v_);
		if (isLoopEnabled())
			loopingTest(true);
	}

	void loopingTest(boolean mark_)
	{
		// try reset() and then mark(), turn off looping if both failed
		try {
			try {
				reader.reset();
				return;
			}
			catch (Exception e_) {
				if (mark_) {
					if (!reader.markSupported())
						reader = new BufferedReader(reader);
					reader.mark(1<<20);
					return;
				}
				drcl.Debug.error(e_ + "\n");
			}
		}
		catch (Exception e_) {
			drcl.Debug.error(e_ + "\n");
		}
		drcl.Debug.error("Turn off the looping flag.\n");
		setLoopEnabled(false);
	}

	/** Returns the looping period. */
	public double getLoopPeriod()
	{ return loopPeriod; }

	/** Sets the looping period. */
	public void setLoopPeriod(double p_)
	{ loopPeriod = p_; }
}
