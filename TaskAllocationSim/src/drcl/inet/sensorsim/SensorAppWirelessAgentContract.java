// @(#)SensorAppWirelessAgentContract.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

import drcl.data.*;
import drcl.net.*;
import drcl.comp.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/** This class implements the contract between the sensor application layer and the middleware layer .
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorAppWirelessAgentContract extends Contract
{
	public static final SensorAppWirelessAgentContract INSTANCE = new SensorAppWirelessAgentContract();
	public static int UNICAST_SENSOR_PACKET = 0 ;
	public static int BROADCAST_SENSOR_PACKET = 1 ;

	public SensorAppWirelessAgentContract()
	{ super(); }
	
	public SensorAppWirelessAgentContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Sensor Application Wireless Agent Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	/** This class implements the underlying message of the contract. */
	public static class Message extends drcl.comp.Message
	{
		long dst ; /* should be address of sink */
		int size ;
		int type ; /* SUPPRESS, COHERENT, NON_COHERENT */
		double snr ;
		int eventID ;
		int UniBcast_flag ; /* a flag that is added to differentiate between sendPkt() (UniBcast_flag=0) and sendBcastPkt() (UniBcast_flag=1). */
		long target_nid;
        
        	public Message ()	{ }

		/** Constructor for unicast packets */
		public Message (int UniBcast_flag_, long dst_, int size_, int type_, double snr_, int eventID_, long target_nid_)
		{
			UniBcast_flag = UniBcast_flag_ ;
			dst = dst_ ;
			size = size_ ;
			type = type_ ;
			snr = snr_ ;
			eventID = eventID_;
			target_nid = target_nid_ ;
		}

		/** Constructor for broadcast packets */
		public Message (int UniBcast_flag_, int type_, double snr_, int eventID_)
		{
			UniBcast_flag = UniBcast_flag_ ;
			type = type_ ;
			snr = snr_ ;
			eventID = eventID_;
		}

 		public long getDst() { return dst; }
 		public int getSize() { return size; }
 		public int getType() { return type; }
 		public double getSNR() { return snr; }
 		public int getEventID() { return eventID; }
		public int getFlag() { return UniBcast_flag; }
		public long getTargetNid() { return target_nid; }

		/*
        public void duplicate(Object source_)
		{
		    Message that_ = (Message)source_;
		    UniBcast_flag = that_.UniBcast_flag ;
		    dst = that_.dst ;
		    size = that_.size ;
		    type = that_.type ;
		    snr = that_.snr ;
		    eventID = that_.eventID;
		    target_nid = that_.target_nid;
		}
		*/
	
		public Object clone()
		{ 
			return new Message(UniBcast_flag, dst, size, type, snr, eventID, target_nid); 
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
	            String str;
        	    str = "Sensor-App-Agent Message:" + separator_ + "UniBcast_flag=" + UniBcast_flag + "dst=" + dst + "size=" + size  + "type=" + type  + "snr=" + snr  + "eventID=" + eventID + "target_nid=" + target_nid; 
			return str;
		}
	}
}
