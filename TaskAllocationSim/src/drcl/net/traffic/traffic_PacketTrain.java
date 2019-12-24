// @(#)traffic_PacketTrain.java   9/2002
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
/**
 * This class describes the packet train traffic model.
 * It defines the following parameters:
 * <dl>
 * <dt>Packet Size
 * <dd>Size of packets (byte).
 *
 * <dt>Packet Inter-arrival Time
 * <dd>Time interval between the arrival times of any two consecutive packets.
 * </dl>
 */
public class traffic_PacketTrain extends TrafficModel implements TrafficPeriodic
{
	public int   packetSize;
	public double interArrivalTime;
	
	public traffic_PacketTrain()
	{}

	public traffic_PacketTrain(int mtu_, double iat_)
	{ set(mtu_, iat_); }

	public void set(int mtu_, double iat_)
	{
		packetSize = mtu_;
		interArrivalTime = iat_;
	}
	
	public double getLoad() 
	{ return (double)(packetSize << 3)/interArrivalTime; }
	
	public double getPeriod() { return interArrivalTime; }
	public int getBurst() { return packetSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_PacketTrain)) return null;
		traffic_PacketTrain thatTraffic_ = (traffic_PacketTrain) that_;
		double load1_ = getLoad();
		double load2_ = thatTraffic_.getLoad();
		packetSize = Math.max(packetSize, thatTraffic_.packetSize);
		interArrivalTime = (packetSize << 3) / (load1_ + load2_);
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_PacketTrain)) return;
		traffic_PacketTrain that_ = (traffic_PacketTrain) source_;
		packetSize = that_.packetSize;
		interArrivalTime = that_.interArrivalTime;
	}
	
	public String oneline()
	{
		return getClass().getName() + ":packetSize=" + packetSize
			+ ", interArrivalTime=" + interArrivalTime;
	}
	
	// 
	private void ___PROPERTY___() {}
	//
	
	public void setPacketSize(int size_) { packetSize = size_; }
	public int getPacketSize() { return packetSize; }
	
	public void setIntArrivalTime(double t_) { interArrivalTime = t_; }
	public double getIntArrivalTime() { return interArrivalTime; }
	
	public int getMTU() { return packetSize; }
}
