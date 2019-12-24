// @(#)Bouncer.java   9/2002
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

package drcl.comp.lib;

import drcl.comp.*;

/** This component bounces back any arriving data at the port where the data comes.
One can delay the response by specifying the delay parameter.
The component also keeps a counter to count the data arrivals. */
public class Bouncer extends Component
{
	long count = 0;
	double delay = 0.0;

	public Bouncer()
	{ super(); }
	
	public Bouncer(String id_)
	{ super(id_); }
	
	public String info()
	{
		return "count = " + count + "\ndelay = " + delay + "\n";
	}
	
	public void reset()
	{
		super.reset();
		count = 0;
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		count = ((Bouncer)source_).count;
		delay = ((Bouncer)source_).delay;
	}

	public synchronized void process(Object data_, drcl.comp.Port inPort_) 
	{
		count++;
		if (delay > 0.0)
			send(inPort_, data_, delay);
		else
			inPort_.doLastSending(data_);
	}
	
	/** Sets the counter value. */
	public void setCount(long v_)
	{ count = v_; }
	
	/** Returns the counter value. */
	public long getCount()
	{ return count; }
	
	/** Sets the delay. */
	public void setDelay(double v_)
	{ delay = v_; }
	
	/** Returns the delay. */
	public double getDelay()
	{ return delay; }
}
