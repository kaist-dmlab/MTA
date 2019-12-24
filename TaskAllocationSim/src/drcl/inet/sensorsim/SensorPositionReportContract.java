// @(#)SensorPositionReportContract.java   1/2004
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

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/** This class implements the contract between mobility model and position tracker.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorPositionReportContract extends Contract
{
	public static final SensorPositionReportContract INSTANCE = new SensorPositionReportContract();

	public SensorPositionReportContract()
	{ super(); }
	
	public SensorPositionReportContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Position Report Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static void report(double X_, double Y_, double Z_, Port out_)
	{ out_.doSending(new Message(X_, Y_, Z_)); }
	
 /** This class implements the underlying message of the contract. */	
    public static class Message extends drcl.comp.Message
	{
        long   nid;             //  node id
        double X, Y, Z;      //  new position
        double X0, Y0, Z0;   //  original position
        
		public Message ()
		{}

        
		public Message (long nid_, double X_, double Y_, double Z_, double X0_, double Y0_, double Z0_)
		{
			nid = nid_; X = X_;  Y = Y_;  Z = Z_; X0 = X0_;  Y0 = Y0_;  Z0 = Z0_;
		}
		
        /* the is only used while SensorMobilityModel responding WirelessPhy's query and reporting its own position */
        public Message (double X_, double Y_, double Z_)
		{
			nid = -1;
            X = X_;  Y = Y_;  Z = Z_; 
            X0 = X_;  Y0 = Y_;  Z0 = Z_;
		}
        
        public long   getNid() { return nid; }
		public double getX()   { return X; }
		public double getY()   { return Y; }
		public double getZ()   { return Z; }
		public double getX0()  { return X0; }
		public double getY0()  { return Y0; }
		public double getZ0()  { return Z0; }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
            nid = that_.nid; X = that_.X;  Y = that_.Y;  Z = that_.Z; X0 = that_.X0;  Y0 = that_.Y0;  Z0 = that_.Z0;
		}
		*/
	
		public Object clone()
		{ return new Message(nid, X, Y, Z, X0, Y0, Z0); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "Position Report:" + separator_ + "nid=" + nid + separator_ + "X=" + X + separator_ + "Y=" + Y + separator_ + "Z=" + Z + separator_ + "X0=" + X0 + separator_ + "Y0=" + Y0 + separator_ + "Z0=" + Z0;
		}
	}
}
