// @(#)SensorNeighborQueryContract.java   1/2004
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

/** This class implements the contract between the sensor channel and the node position tracker.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorNeighborQueryContract extends Contract
{
	public static final SensorNeighborQueryContract INSTANCE = new SensorNeighborQueryContract();

	public SensorNeighborQueryContract()
	{ super(); }
	
	public SensorNeighborQueryContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Sensor Neighbor Query Contract"; }
	
	public Object getContractContent()
	{ return null; }

	/** Sends a query and receives a reply */	
	public static Object query(long nid_, double X_, double Y_, double Z_, Port out_)
	{ return out_.sendReceive(new Message(nid_, X_, Y_, Z_)); }
	
	/** Sends a reply  */
	public static void reply(long[] nodeList_, Port out_)
	{ out_.doSending(new Message(nodeList_)); }

	/** This class implements the underlying message of the contract. */	
    	public static class Message extends drcl.comp.Message
	{
        long   nid;
        double X, Y, Z;
        long[] nodeList;
	  double Radius ; // transmission range
        
		public Message ()	{ }

		public Message (long nid_, double X_, double Y_, double Z_)
		{
		nid = nid_;
            X = X_;  Y = Y_;  Z = Z_;
		Radius = 0.0 ;
            nodeList = null;
		}
		
		public Message (long nid_, double X_, double Y_, double Z_, double Radius_)
		{
		nid = nid_;
            X = X_;  Y = Y_;  Z = Z_;
		Radius = Radius_ ;
            nodeList = null;
		}
        
	      public Message (long[] nodeList_) 
      	{
            nid = -1;
            X = 0; Y = 0; Z = 0;
		Radius = 0.0;
            nodeList = nodeList_;
	      }

		public Message (long nid_, double X_, double Y_, double Z_, double Radius_, long[] nodeList_)
		{
            nid = nid_;
		X = X_;  Y = Y_;  Z = Z_;
		Radius = Radius_;
            if ( nodeList_ != null ) {
                int n = nodeList_.length;
                nodeList = new long[n];
                for ( int i = 0; i < n; i ++ ) 
                    nodeList[i] = nodeList_[i];
            }
            else 
                nodeList = null;
		}
        
		public Message (long nid_, double X_, double Y_, double Z_, long[] nodeList_)
		{
            nid = nid_;
		X = X_;  Y = Y_;  Z = Z_;
		Radius = 0.0 ;
            if ( nodeList_ != null ) {
                int n = nodeList_.length;
                nodeList = new long[n];
                for ( int i = 0; i < n; i ++ ) 
                    nodeList[i] = nodeList_[i];
            }
            else 
                nodeList = null;
		}
        
        public long   getNid()     { return nid; }
		public double getX()       { return X; }
		public double getY()       { return Y; }
		public double getZ()       { return Z; }
		public double getRadius()	{ return Radius; }
        
        public long[] getNodeList() { 
            return nodeList; 
        }
		
		/*
        public void duplicate(Object source_)
		{
            int i;
			Message that_ = (Message)source_;
            nid = that_.nid;
            X = that_.X;  Y = that_.Y;  Z = that_.Z;
		Radius = that_.Radius ;
            if ( that_.nodeList != null ) {
                int n = that_.nodeList.length;
                nodeList = new long[n];
                for ( i = 0; i < n; i ++ ) 
                    nodeList[i] = that_.nodeList[i];
            }
            else 
                nodeList = null;
		}
		*/
	
		public Object clone()
		{ return new Message(nid, X, Y, Z, Radius, nodeList); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
            String str;
            str = "Neighbor Query:" + separator_ + "nid=" + nid + separator_ + "X=" + X + separator_ + "Y=" + Y + separator_ + "Z=" + Z + separator_ + "Radius=" + Radius + separator_ + "nodeList =";
            if ( nodeList != null ) 
                if ( nodeList.length != 0 )
                    for ( int i = 0; i < nodeList.length; i ++ ) 
                        str = str + nodeList[i] + " ";
		return str;
		}
	}
}
