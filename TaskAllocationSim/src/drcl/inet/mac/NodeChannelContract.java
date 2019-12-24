// @(#)NodeChannelContract.java   1/2004
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

package drcl.inet.mac;

import drcl.data.*;
import drcl.net.*;
import drcl.comp.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
 * The NodeChannelContract contract. This contract defines the message format
 * between <code>Node</code> and <code>Channel</code> components.
 *
 * @see WirelessPhy
 * @see Channel
 * @author Ye Ge
 */
public class NodeChannelContract extends Contract
{
	public static final NodeChannelContract INSTANCE = new NodeChannelContract();

	public NodeChannelContract()
	{ super(); }
	
	public NodeChannelContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Node Channel Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
    /** 
     * The message class which is defined for exchanging information between Node
     * and Channel components.
     */
    public static class Message extends drcl.comp.Message
	{
        /** The sender's node id  */
        public  long   nid;         
        /** The x coordinate of the sender's position */
        public  double X;
        /** The y coordinate of the sender's position */
        public  double Y;
        /** The z coordinate of the sender's position */
        public  double Z;    
        /** The sender's transmission power */
        public  double Pt;          // sender's transmittion power
        /** The sender's antenna gain */
        public  double Gt;          // sender's antenna gain
        
        /** The packet being transmitted*/
        Object pkt;         // the packet
        
        public Message ()	{ }

		/** 
         * Constructs a message
         *
         *@param nid_ the id of the sender node.
         *@param X_ the x coordinate of the sender node's current position.
         *@param Y_ the y coordinate of the sender node's current position.
         *@param Z_ the z coordinate of the sender node's current position.
         *@param Pt the transmission power
	     *@param Gt_  the transmitting antenna gain
	     *@param pkt_ the packet being transmitted
         *
		 */
		public Message (long nid_, double X_, double Y_, double Z_, double Pt_, double Gt_, Object pkt_)
		{
            nid = nid_;
			X = X_;  Y = Y_;  Z = Z_;
            Pt = Pt_; Gt = Gt_; pkt = pkt_;
		}
        
		/** Gets the node id */
        public long   getNid()  { return nid; }
		/** Gets the X coordinate */
		public double getX()  { return X; }
		/** Gets the Y coordinate */
		public double getY()  { return Y; }
		/** Gets the Z coordinate */
		public double getZ()  { return Z; }
		/** Gets the channel gain */
		public double getGt()  { return Gt; }
		/** Gets the transmission power */
		public double getPt()  { return Pt; }
		/** Gets the packet */
        public Object getPkt() { return pkt; }

		public Object clone()
		{
			// the contract is between channel and multiple nodes;
			// need to clone pkt
			return new Message(nid, X, Y, Z, Pt, Gt,
					pkt instanceof drcl.ObjectCloneable?
					((drcl.ObjectCloneable)pkt).clone(): pkt);
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
            String str;
            str = "Node-Channel Message:" + separator_ + "nid=" + nid + separator_ + "X=" + X + separator_ + "Y=" + Y + separator_ + "Z=" + Z + separator_ ;
            str = str + "Pt=" + Pt + separator_ + "Gt=" + Gt + separator_ + "Pkt=" + pkt.toString(); 
			return str;
		}
	}
}
