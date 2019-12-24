// @(#)Translator_DCTS.java   9/2002
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
 * DCTS translator: works with any traffic model that
 * implements <code>TrafficPeriodic</code>.  It produces Rspecs in the <code>SpecR_DCTS</code> class.
 * The worse-case delay per hop is specified as the period of the traffic model and assigned
 * as D in <code>SpecR_DCTS</code>.  If the resulting end-to-end delay does not satisfy the
 * end-to-end delay requirement, then D is obtained by *equally* dividing the delay requirement
 * to each hop.
 * 
 * @see drcl.net.traffic.TrafficPeriodic
 * @see SpecR_DCTS
 */
public class Translator_DCTS extends drcl.intserv.RspecTranslator
{
	/**
	 */
	public SpecR translate(SpecAd adspec_, QoSRequirement qos_)
	{
		if (adspec_ == null || !(adspec_.tspec instanceof TrafficPeriodic) || 
			qos_ == null) return null;
		
		// check e2e properties
		if (!adspec_.check(qos_)) return null;

		int hop = adspec_.hop;
		TrafficModel tspec_ = adspec_.tspec;
		double D_ = ((TrafficPeriodic)tspec_).getPeriod();
		int C_ = (int)(tspec_.getLoad() * D_ / 8.0);
		if (D_ * hop + adspec_.minPropDelay > qos_.maxE2eDelay) 
			D_ = (qos_.maxE2eDelay - adspec_.minPropDelay) / hop;
			
		return new SpecR_DCTS(C_, D_);
	}
}
