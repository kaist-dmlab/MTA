// @(#)SimpleTrace.java   9/2002
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
import java.io.BufferedReader;
import drcl.comp.*;

/**
 * This class implements a simple trace component.
It recognizes the trace file in the following format:
<pre>
	&lt;time&gt;	&lt;packet_size&gt;
</pre>
And it ignores any line starting with "<code>#</code>".
 */
public class SimpleTrace extends TraceInput
{
	double lastTime, lastPeriodStart;

	public SimpleTrace()
	{ super(); }

	public SimpleTrace(String id_)
	{ super(id_); }

	BufferedReader breader;

	protected double setNextPacket(drcl.net.FooPacket nextpkt_) 
	{
		try{
			String line_ = null;
			if (breader == null)
				breader = reader instanceof BufferedReader?
					(BufferedReader)reader: new BufferedReader(reader);
			do {
				line_ = breader.readLine();
				if (line_ != null) line_ = line_.trim();
			}
			while (line_ != null && (line_.length() == 0 || line_.charAt(0) == '#'));

			if (line_ == null) {
				if (isLoopEnabled()) {
					try {
						reader.reset();
						lastPeriodStart += getLoopPeriod();
						if (lastPeriodStart < lastTime)
							lastPeriodStart = lastTime;
						do {
							line_ = breader.readLine();
							if (line_ != null) line_ = line_.trim();
						}
						while (line_ != null && (line_.length() == 0 || line_.charAt(0) == '#'));
						if (line_ == null)
							return Double.NaN;
					}
					catch (Exception e_) {
						drcl.Debug.error(this + " cannot reset the trace for looping");
						return Double.NaN;
					}
				}
				else
					return Double.NaN;
			}
			String[] elements_ = drcl.util.StringUtil.substrings(line_);
			nextpkt_.setPacketSize(Integer.parseInt(elements_[1]));
			lastTime = Double.valueOf(elements_[0]).doubleValue() + lastPeriodStart;
			return lastTime;
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return Double.NaN;
		}
	}
	
	public void reset()
	{
		super.reset();
		lastTime = lastPeriodStart = 0.0;
		breader = null;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	public String info()
	{
		return super.info()
			+ "lastPeriodStart = " + lastPeriodStart + "\n"
			+ "   lastTimeSent = " + lastTime + "\n";
	}
}
