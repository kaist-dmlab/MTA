// @(#)DVMRPTimingPack.java   12/2002
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

package drcl.inet.protocol.dvmrp;

/** Defines all the timing parameters used by {@link DVMRP}.  */
public class DVMRPTimingPack
{
	/**
	 * Regular update timeout period.
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static double REGULAR_UPDATE_TIMEOUT_PERIOD = 60.0;

	/**
	 * Timeout variance for updating the distance vector information.
	 * Recommended in RFC2453 (RIPv2).
	 */
	public final static double MAX_TIME_OUT_VARIANCE = 5.0;

	/**
	 * Routing table entry timeout period.
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static double ROUTE_TIMEOUT_PERIOD = 140.0;
	
	/**
	 * Hold-down timeout period for entries to be removed.
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static double DELETE_TIMEOUT_PERIOD = 2 * REGULAR_UPDATE_TIMEOUT_PERIOD;

	/**
	 * Triggered update timeout period.
	 * This is set up when a triggered update is needed.
	 * The reason for this delay is to prevent excessive traffic from
	 * message exchanges during a transitional period.
	 */
	public final static double TRIGGERED_UPDATE_TIMEOUT_PERIOD = MAX_TIME_OUT_VARIANCE;
	
	/**
	 * Prune state lifetime (< 2hr).
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static int PRUNE_LIFETIME = 60 * 60 * 2;
	
	/**
	 * Prune retransmission time (with exponential backoff).
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static double PRUNE_RETX_TIME = 3.0;
	
	/**
	 * Graft retransmission time (with exponential backoff).
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static double GRAFT_RETX_TIME = 5.0;
	
	public double regularUpdatePeriod = REGULAR_UPDATE_TIMEOUT_PERIOD;
	public double maxTimeoutVariance = MAX_TIME_OUT_VARIANCE;
	public double routeTimeoutPeriod = ROUTE_TIMEOUT_PERIOD;
	public double deleteTimeoutPeriod = DELETE_TIMEOUT_PERIOD;
	public double triggeredUpdateDelayPeriod = TRIGGERED_UPDATE_TIMEOUT_PERIOD;
	public int pruneLifetime = PRUNE_LIFETIME;
	public double pruneRetxTime = PRUNE_RETX_TIME;
	public double graftRetxTime = GRAFT_RETX_TIME;
	
	public DVMRPTimingPack ()
	{}

	public DVMRPTimingPack (double regularUpdate_,
									double triggeredUpdateDelay_,
									double routeTimeout_,
									double routeHoldDownTime_,
									double timingVar_,
									int pruneLifetime_,
									double pruneRetxTime_,
									double graftRetxTime_)
	{
		regularUpdatePeriod = regularUpdate_;
		triggeredUpdateDelayPeriod = triggeredUpdateDelay_;
		routeTimeoutPeriod = routeTimeout_;
		deleteTimeoutPeriod = routeHoldDownTime_;
		maxTimeoutVariance = timingVar_;
		pruneLifetime = pruneLifetime_;
		pruneRetxTime = pruneRetxTime_;
		graftRetxTime = graftRetxTime_;
	}
	
	public String info()
	{
		return   "        Regular update period: " + regularUpdatePeriod + " sec.\n"
			   + "       Triggered update delay: " + regularUpdatePeriod + " sec.\n"
			   + "                Route timeout: " + routeTimeoutPeriod + " sec.\n"
			   + "Route deletion hold-down time: " + deleteTimeoutPeriod + " sec.\n"
			   + "        Max. timeout variance: " + maxTimeoutVariance + " sec.\n"
			   + "              Prune  lifetime: " + pruneLifetime + " sec.\n"
			   + "              Prune Retx time: " + pruneRetxTime + " sec.\n"
			   + "              Graft Retx time: " + graftRetxTime + " sec.\n";
	}
	
	public double getRegularUpdatePeriod()
	{ return regularUpdatePeriod; }
	
	public void setRegularUpdatePeriod(double v_)
	{ regularUpdatePeriod = v_; }
	
	public double getTriggeredUpdateDelay()
	{ return triggeredUpdateDelayPeriod; }
	
	public void setTriggeredUpdateDelay(double v_)
	{ triggeredUpdateDelayPeriod = v_; }
	
	public double getRouteTimeout()
	{ return routeTimeoutPeriod; }
	
	public void setRouteTimeout(double v_)
	{ routeTimeoutPeriod = v_; }
	
	public double getRouteHoldDownTime()
	{ return deleteTimeoutPeriod; }
	
	public void setRouteHoldDownTime(double v_)
	{ deleteTimeoutPeriod = v_; }
	
	public double getTimingVariance()
	{ return maxTimeoutVariance; }
	
	public void setTimingVariance(double v_)
	{ maxTimeoutVariance = v_; }
	
	public int getPruneLifetime()
	{ return pruneLifetime; }
	
	public void setPruneLifetime(int v_)
	{ pruneLifetime = v_; }
	
	public double getPruneRetxTime()
	{ return pruneRetxTime; }
	
	public void setPruneRetxTime(double v_)
	{ pruneRetxTime = v_; }
	
	public double getGraftRetxTime()
	{ return graftRetxTime; }
	
	public void setGraftRetxTime(double v_)
	{ graftRetxTime = v_; }
}
