// @(#)NeighborQueryContract.java   1/2004
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
 * The NeightbourQueryContract contract. This contract defines the commands
 * and the message format between <code>Channel</code> component
 * and <code>NodePositionTracker</code> component.   
 *
 * @see Channel
 * @see NodePositionTracker
 * @author Ye Ge
 */
public class NeighborQueryContract extends Contract
{
	public static final NeighborQueryContract INSTANCE = new NeighborQueryContract();

	public NeighborQueryContract()
	{ super(); }
	
	public NeighborQueryContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Position Report Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
    /** 
     * Queries the neighbours of the sender whose current position is (X_, Y_, Z_).
     */
	public static Object query(long nid_, double X_, double Y_, double Z_, Port out_)
	{ return out_.sendReceive(new Message(nid_, X_, Y_, Z_)); }

    /**
     * Sends back a reply through the out_ port
     *
     *@param nodeList_ an array of the nid's of neighboring nodes.
     *@out_  the port through which a reply message should be sent back. 
     */
	public static void reply(long[] nodeList_, Port out_)
	{ out_.doSending(new Message(nodeList_)); }
	
    public static class Message extends drcl.comp.Message
	{
        long   nid;
        double X, Y, Z;
        long[] nodeList;
        
        // All nodes in nGrids away subareas are considered as neighbours.
        // By default, nGrids is set to 1, which means all nodes in 9 adjacent
        // subareas (including the subarea where the sender is located) are neighbours.
        int    nGrids = 1;
        
		public Message ()	{ }

        /**
         * Constructor. Assumes nGrids is 1.
         *
         *
         *@param nid_ the id of the querying node.
         *@param X_ the x coordinate of the node current position.
         *@param Y_ the y coordinate of the node current position.
         *@param Z_ the z coordinate of the node current position.
         *
         */
		public Message (long nid_, double X_, double Y_, double Z_)
		{
			nid = nid_;
            X = X_;  Y = Y_;  Z = Z_;
            nodeList = null;
            nGrids = 1;
		}
		
        
        /**
         * Constructor. 
         *
         *
         *@param nid_ the id of the querying node.
         *@param X_ the x coordinate of the node current position.
         *@param Y_ the y coordinate of the node current position.
         *@param Z_ the z coordinate of the node current position.
         *@nGrids_ the nGrid value.
         */
		public Message (long nid_, double X_, double Y_, double Z_, int nGrids_)
		{
			nid = nid_;
            X = X_;  Y = Y_;  Z = Z_;
            nodeList = null;
            nGrids = nGrids_;
		}
        
        /**
         * Construct. Constructs a reply message containning only a node list.
         *
         *@param nodeList_ the list of all neighbour node id.
         *
         */
        public Message (long[] nodeList_) 
        {
            nid = -1;
            X = 0; Y = 0; Z = 0;
            nGrids = nGrids;
            nodeList = nodeList_;
        }
        
		/** 
          * Constructor. Constructs a message 
          *
		  * @param nid_ the node id
		  * @param X_   the X coordinate of the querying node
		  * @param Y_   the Y coordinate of the querying node
		  * @param Z_   the Z coordinate of the querying node
		  * @param nodeList_ the id list of all neighbour nodes 
		  * @param nGrids_ the value of nGrids
		  */
		public Message (long nid_, double X_, double Y_, double Z_, long[] nodeList_, int nGrids_)
		{
            nid = nid_;
			X = X_;  Y = Y_;  Z = Z_;
            if ( nodeList_ != null ) {
                int n = nodeList_.length;
                nodeList = new long[n];
                for ( int i = 0; i < n; i ++ ) 
                    nodeList[i] = nodeList_[i];
            }
            else 
                nodeList = null;
            nGrids = nGrids_;
		}
        
		/** Gets the node id */
        public long   getNid()     { return nid; }
		/** Gets the X coordinate */
		public double getX()       { return X; }
		/** Gets the Y coordinate */
		public double getY()       { return Y; }
		/** Gets the Z coordinate */
		public double getZ()       { return Z; }
		/** Gets nGrids */
		public int    getnGrids()  { return nGrids; }

        /** Gets node list in the message */
        public long[] getNodeList() { 
            return nodeList; 
        }
		
		public Object clone()
		{ return new Message(nid, X, Y, Z, nodeList, nGrids); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
            String str;
            str = "Neighbor Query:" + separator_ + "nid=" + nid + separator_ + "X=" + X + separator_ + "Y=" + Y + separator_ + "Z=" + Z + separator_ + "nodeList =";
            if ( nodeList != null ) 
                if ( nodeList.length != 0 )
                    for ( int i = 0; i < nodeList.length; i ++ ) 
                        str = str + nodeList[i] + " ";
            str = str + separator_ + "nGrids=" + nGrids;
			return str;
		}
	}
}
