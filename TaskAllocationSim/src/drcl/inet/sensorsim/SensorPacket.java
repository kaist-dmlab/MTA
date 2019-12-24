// @(#)SensorPacket.java   12/2003
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
import drcl.net.Packet;

/** This class implements the packet that a sensor node sends/forwards to the sink node.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorPacket extends Packet
{
	int pktType ;
	int dataSize ;
	double maxSNR ;
	int eventID ;
	long target_nid; /* target to which this information pertains */
	int maxProp ;

	/** Gets the data size.  */
	public int getDataSize()   {return dataSize;}

	/** Gets the MaxSNR.  */
	public double getMaxSnr()  {return maxSNR;}

	/** Gets the ID of the target node to which the enclosed information pertains.  */
	public long getTargetNid() {return target_nid;}

	public String getName()
	{ return "Sensor Packet"; }

	SensorPacket(int pktType_, int dataSize_, double maxSNR_, int eventID_, int maxProp_, long target_nid_)
	{
		pktType = pktType_ ;
		dataSize = dataSize_ ;
		maxSNR = maxSNR_ ;
		eventID = eventID_ ;
		maxProp = maxProp_ ;
		target_nid = target_nid_ ;
	}

        public void duplicate(Object source_)
	{
		SensorPacket that_ = (SensorPacket)source_;
		pktType = that_.pktType ;
		dataSize = that_.dataSize ;
		maxSNR = that_.maxSNR ;
		eventID = that_.eventID ;
		maxProp = that_.maxProp ;
		target_nid = that_.target_nid ;
	}
	
	public Object clone()
	{ 
		return new SensorPacket(pktType, dataSize, maxSNR, eventID, maxProp, target_nid); 
	}

	public String toString(String separator_)
	{
		String str;
        	str = "Sensor Packet dataSize =" + separator_ + dataSize + separator_ + "maxSNR=" + maxSNR + separator_ + "target_nid=" + target_nid ; 
		return str;
	}
}
