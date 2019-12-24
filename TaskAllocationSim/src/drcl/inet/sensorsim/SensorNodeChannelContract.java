// @(#)SensorNodeChannelContract.java   1/2004
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

/** This class implements the contract between the sensor channel and the sensor physical layer.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorNodeChannelContract extends Contract
{
	public static final SensorNodeChannelContract INSTANCE = new SensorNodeChannelContract();

	public SensorNodeChannelContract()
	{ super(); }
	
	public SensorNodeChannelContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Sensor Node Channel Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
 	/** This class implements the underlying message of the contract. */
    	public static class Message extends drcl.comp.Message
	{
        long   nid;         // sender's node id
        double X, Y, Z;     // sender's position
        double Pt;          // sender's transmittion power
	double Radius;	    // sender's transmission radius
        
        Object pkt;         // the packet
        
        public Message ()	{ }

		public Message (long nid_, double X_, double Y_, double Z_, double Pt_, double Radius_, Object pkt_)
		{
     			nid = nid_;
			X = X_;  Y = Y_;  Z = Z_;
	            Pt = Pt_; 
			Radius = Radius_;
			pkt = pkt_;
		}

		public Message (long nid_, double X_, double Y_, double Z_, double Pt_, Object pkt_)
		{
     			nid = nid_;
			X = X_;  Y = Y_;  Z = Z_;
	            Pt = Pt_; 
			Radius = 0.0 ;
			pkt = pkt_;
		}
        
        public long   getNid()  { return nid; }
		public double getX()  { return X; }
		public double getY()  { return Y; }
		public double getZ()  { return Z; }
		public double getPt()  { return Pt; }
		public double getRadius() { return Radius; }
        public Object getPkt() { return pkt; }
        
		
		/*
        public void duplicate(Object source_)
		{
            int i;
		Message that_ = (Message)source_;
            nid = that_.nid;
            X = that_.X;  Y = that_.Y;  Z = that_.Z;
            Pt = that_.Pt; 
		Radius = that_.Radius;
            pkt = that_.pkt;
		}
		*/
	
		public Object clone()
		{
			// the contract is between two components; dont clone pkt
			return new Message(nid, X, Y, Z, Pt, Radius, pkt);
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
            String str;
            str = "Sensor-Node-Channel Message:" + separator_ + "nid=" + nid + separator_ + "X=" + X + separator_ + "Y=" + Y + separator_ + "Z=" + Z + separator_ ;
            str = str + "Pt=" + Pt + separator_ + "Radius=" + Radius + separator_ + "Pkt=" + pkt.toString(); 
			return str;
		}
	}
}
