// @(#)Translator_GR.java   9/2002
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

package drcl.intserv.scheduler;

import drcl.intserv.*;
import drcl.net.traffic.*;

/**
 * Generalized Rate Guarantee Translator: can work with any traffic model.  
 * It produces Rspecs in the <code>SpecR_GR</code> class.
 * 
 * <p>
 * The minimum end-to-end queueing delay is calculated using the following formula:
<pre>	buffer required at last hop = burst_ + hop * MTU<br>
	end-to-end delay = (buffer required at last hop) / minBW.</pre>
 * 
 * @see SpecR_GR
 */
public class Translator_GR extends drcl.intserv.RspecTranslator
{
	/**
	 */
	public SpecR translate(SpecAd adspec_, QoSRequirement qos_)
	{
		if (adspec_ == null || adspec_.tspec == null || 
			qos_ == null) return null;
		
		// check e2e properties
		if (!adspec_.check(qos_)) return null;

		int	   hop_		= adspec_.hop;
		double delay_	= adspec_.minPropDelay; // propagation delay + other hop-related terms
		int mtu_		= adspec_.tspec.getMTU();
		TrafficModel tspec_ = adspec_.tspec;
		int load_	= (int)Math.ceil(tspec_.getLoad());
		int burst_	= tspec_.getBurst();
			
		// calculate min. bw required for satisfying e2e delay using the following formula:
		//	e2e delay = (burst_ + hop_ * mtu_) / minBW_ + acc. prop. delay;
		// Buffer:
		int minBuffer_ = burst_ + hop_ * mtu_;
		int minBW_ = Math.max(load_, (int)Math.ceil((minBuffer_ << 3) / (qos_.maxE2eDelay - delay_)));
		if ( adspec_.minBW < minBW_ ) return null; // the path cannot afford 


		return new SpecR_GR(minBW_, minBuffer_, mtu_);
	}
}
