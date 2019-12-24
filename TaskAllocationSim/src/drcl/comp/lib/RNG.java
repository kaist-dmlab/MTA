// @(#)RNG.java   7/2003
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

package drcl.comp.lib;

import drcl.comp.*;
import drcl.data.*;
import drcl.util.random.RandomNumberGenerator;

/**
 * A random number generation component.
 */
public class RNG extends Component implements ActiveComponent
{
	RandomNumberGenerator rng = null;
	double delay = 0.0;
	long seed;
	int ttl = -1;
	int TTL = -1;
	Port out = addPort("out", false);
	
	public RNG()
	{ super(); }

	public RNG(String id_)
	{ super(id_); }

	public void reset()
	{
		super.reset();
		rng.setSeed(seed);
		ttl = TTL;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		RNG that_ = (RNG) source_;
		seed = that_.seed;
		delay = that_.delay;
		if (that_.rng != null) 
			rng = (RandomNumberGenerator)that_.rng.clone();
	}

	public String info()
	{
		if (rng == null)
			return "delay=" + delay + ", TTL=" + ttl + "\n";
		else
			return  rng.oneline() + ", delay=" + delay + ", TTL=" + ttl + "\n";
	}
	
	protected void _start()
	{
		process(null, null);
	}

	public synchronized void _resume()
	{
		if (ttl != 0)
			process(null, null);
	}

	protected void process(Object data_, Port inPort_)
	{
		if (isStopped()) return;
		if (ttl != 0) {
			if (rng != null)
				out.doSending(new Double(rng.nextDouble()));
			if (ttl > 0) ttl--;
			if (ttl != 0) fork(out, null, delay);
		}
	}

	public long getSeed()
	{ return seed; }

	public void setSeed(long seed_)
	{ seed = seed_; }

	public double getDelay()
	{ return delay; }

	public void setDelay(double d)
	{ delay = d; }

	public int getTTL()
	{ return TTL; }

	/** Sets the time-to-live value; -1 to generate numbers forever. */
	public void setTTL(int ttl_)
	{ ttl = TTL = ttl_; }

	public RandomNumberGenerator getRNG()
	{ return rng; }

	public void setRNG(RandomNumberGenerator rng_)
	{ rng = rng_; }
}
