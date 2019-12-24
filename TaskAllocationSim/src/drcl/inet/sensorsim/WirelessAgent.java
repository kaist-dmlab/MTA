// @(#)WirelessAgent.java   12/2003
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

package drcl.inet.sensorsim;

import drcl.comp.Port;
import drcl.inet.InetPacket;

/** This class implements the middleware between the sensor protocol stack and the wireless protocol stack.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class WirelessAgent extends drcl.inet.Protocol
{
       public static final int SLOT_SIZE = 50 ;

       public static final String TO_SENSOR_APP_PORT_ID = ".toSensorApp";
       protected Port toSensorAppPort = addPort(TO_SENSOR_APP_PORT_ID, false);

	public WirelessAgent()
	{ super(); }

	public WirelessAgent(String id_)
	{ super(id_); }

	/** Sends a unicast packet over the wireless channel  */
	protected synchronized void sendPkt(long dst_, int size_, int type_, double snr_, int eventID_, long target_nid_)
	{
		int bytesLeft = size_;
		int bytesSent = 0;

		// send the packet in SLOT_SIZE byte chunk for tdma
		while ( bytesLeft > 0 ) {
			bytesSent = (bytesLeft>=SLOT_SIZE)?SLOT_SIZE:bytesLeft;
			SensorPacket sensorPkt = new SensorPacket(type_, bytesSent, snr_, eventID_, 0, target_nid_);

			forward(sensorPkt, drcl.net.Address.NULL_ADDR, 
				dst_, false, 255, 0);
			bytesLeft -= bytesSent;
		} // end while
	}

	/** Sends a broadcast packet over the wireless channel  */
	protected synchronized void sendBcastPkt(int type_, double snr_, int eventID_)
	{
		SensorPacket sensorPkt = new SensorPacket(type_, 0 /*understood to be sizeof SensorPacket*/, snr_, eventID_, 1, Integer.MIN_VALUE); /* since in sending the broadcast packet, the target_nid is lost, a temporary solution is to use Integer.MIN_VALUE. */

		forward(sensorPkt, drcl.net.Address.NULL_ADDR, drcl.net.Address.ANY_ADDR, false, 255, 0);
	}

	/** Handles data arriving at UP port */
	protected synchronized void dataArriveAtUpPort(Object data_, Port upPort_)
	{
		if (!(data_ instanceof SensorAppWirelessAgentContract.Message)) {
	                error(data_, "processOther()", upPort_, "unknown object");
        	        return;
        	}

		SensorAppWirelessAgentContract.Message msg = (SensorAppWirelessAgentContract.Message)data_ ;
		
		if (msg.getFlag() == SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET)
			sendPkt(msg.getDst(), msg.getSize(), msg.getType(), msg.getSNR(), msg.getEventID(), msg.getTargetNid()) ;
		else if (msg.getFlag() == SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET)
			sendBcastPkt(msg.getType(), msg.getSNR(), msg.getEventID()) ;
	}

	/** Handles data arriving at DOWN port */
	/* received packet from the routing layer, needs to forward to the sensor application layer */
	protected synchronized void dataArriveAtDownPort(Object data_, Port downPort_)
	{
		InetPacket ipkt_ = (InetPacket)data_;
		SensorPacket pkt_ = (SensorPacket)ipkt_.getBody();
		long src_ = ipkt_.getSource();
		toSensorAppPort.doSending(new SensorPacket(pkt_.pktType, pkt_.dataSize,pkt_.maxSNR, pkt_.eventID, pkt_.maxProp, pkt_.target_nid));
	}
}
