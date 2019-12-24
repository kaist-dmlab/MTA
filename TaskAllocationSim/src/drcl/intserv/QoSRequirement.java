// @(#)QoSRequirement.java   9/2002
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

package drcl.intserv;

import drcl.data.*;

/**
 * The QoS included is listed below:
 * <UL>
 *  <LI> End-to-End Delay
 *  <LI> Inter-Destination Jitter
 *  <LI> Inter-Packet Jitter
 *  <LI> Minimum Bandwidth
 *  <LI> Packet Loss Rate
 * </UL>
 */
public class QoSRequirement extends drcl.DrclObj
{
	public long[] connectionID;
	public double maxE2eDelay;
	public double maxIntDestJitter;
	public double maxIntPktJitter;
	public double minBW;
	public double maxPktLossRate;
	public int maxHop;
	
	public QoSRequirement (long[] connectionID_)
	{
		this(connectionID_, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY, 0.0, 1.0, Integer.MAX_VALUE);
	}
	
	public QoSRequirement (long[] connectionID_, double ed_, double idj_, double ipj_, double minbw_, 
						double ploss_, int hop_)
	{
		connectionID = connectionID_;
		maxE2eDelay = ed_;
		maxIntDestJitter = idj_;
		maxIntPktJitter = ipj_;
		minBW = minbw_;
		maxPktLossRate = ploss_;
		maxHop = hop_;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof QoSRequirement)) return;
		QoSRequirement that_ = (QoSRequirement) source_;
		connectionID = that_.connectionID;
		maxE2eDelay = that_.maxE2eDelay;
		maxIntDestJitter = that_.maxIntDestJitter;
		maxIntPktJitter = that_.maxIntPktJitter;
		minBW = that_.minBW;
		maxPktLossRate = that_.maxPktLossRate;
		maxHop = that_.maxHop;
	}
	
	public void merge(QoSRequirement that_)
	{
		if (maxE2eDelay > that_.maxE2eDelay)maxE2eDelay = that_.maxE2eDelay;
		if (maxIntDestJitter > that_.maxIntDestJitter)maxIntDestJitter = that_.maxIntDestJitter;
		if (maxIntPktJitter > that_.maxIntPktJitter)maxIntPktJitter = that_.maxIntPktJitter;
		if (minBW > that_.minBW)minBW = that_.minBW;
		if (maxPktLossRate > that_.maxPktLossRate)maxPktLossRate = that_.maxPktLossRate;
		if (maxHop > that_.maxHop)maxHop = that_.maxHop;
	}
	
	public void setConnectionID(long[] g_) { connectionID = g_; }
	public long[] getConnectionID() { return connectionID; }
	
	public void setMaxEndToEndDelay(double d_) { maxE2eDelay = d_; }
	public double getMaxEndToEndDelay() { return maxE2eDelay; }
	
	public void setMaxIntDestJitter(double d_) { maxIntDestJitter = d_; }
	public double getMaxIntDestJitter() { return maxIntDestJitter; }
	
	public void setMaxIntPktJitter(double d_) { maxIntPktJitter = d_; }
	public double getMaxIntPktJitter() { return maxIntPktJitter; }
	
	public void setMinBW(double d_) { minBW = d_; }
	public double getMinBW() { return minBW; }
	
	public void setMaxPktLossRate(double d_) { maxPktLossRate = d_; }
	public double getMaxPktLossRate() { return maxPktLossRate; }
	
	public void setMaxHop(int d_) { maxHop = d_; }
	public int getMaxHop() { return maxHop; }
}
