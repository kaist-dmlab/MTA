// @(#)MacPhyContract.java   1/2004
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
import drcl.comp.*;
import drcl.net.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
 * This contract is defined for <code>WirelessPhy</code> to convey some infomation to mac, which seems not appropriate to be carried in a Packet.
 * In the future release, more complicated information can be defined here to provide mac more information.
 * @author Ye Ge
 */
public class MacPhyContract extends Contract
{
	public static final MacPhyContract INSTANCE = new MacPhyContract();

	public MacPhyContract()
	{ super(); }
	
	public MacPhyContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "MAC WirelessPhy Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
    /**
     * This class defines the data structure of the message between Mac and Phy components.
     */
    public static class Message extends drcl.comp.Message
	{
        /** The flag indicating whether this frame is corrupted or not.  */
        boolean error;
        
        /** Received power */
        double  RxPr;      
        
        /** Capture threshold */
        double  CPThresh;  
        
        /** Receiving threshold. */
    	double  RXThresh;
        
        /** Carrier Sensing Threshold. */
        double  CSThresh;

        /** The data packet.  */
        Object  pkt;        
        
        public Message ()	{ }

   		/** Construct a Mac-Phy Message 
		  * @param error_ - indicating if the frame is corrupted
		  * @param RxPr_ - received power
		  * @param CPThresh_ - capture threshold
		  * @param pkt_ - data packet
		  */
        public Message (boolean error_, double RxPr_, double CPThresh_, Object pkt_)
        {
            error    = error_;
            RxPr     = RxPr_;
            CPThresh = CPThresh_;
            pkt      = pkt_;
        }

		/** Construct a Mac-Phy Message 
		  * @param error_ - indicating if the frame is corrupted
		  * @param RxPr_ - received power
		  * @param CPThresh_ - capture threshold
		  * @param CSThresh_ - carrier sensing threshold
		  * @param RXThresh_ - receiving threshold
		  * @param pkt_ - data packet
		  */
        public Message (boolean error_, double RxPr_, double CPThresh_, double CSThresh_, double RXThresh_, Object pkt_)
        {
            error    = error_;
            RxPr     = RxPr_;
            CPThresh = CPThresh_;
            CSThresh = CSThresh_;
            RXThresh = RXThresh_;
            pkt      = pkt_;
        }

		/** Get the error flag */
        public boolean getError()     { return error; }
		/** Get the transmission power */
        public double  getRxPr()      { return RxPr; }
		/** Get the capture threshold */
        public double  getCPThresh()  { return CPThresh; }
		/** Get the receiving threshold */
        public double  getRXThresh()  { return RXThresh; }
		/** Get the carrier sense threshold */
        public double  getCSThresh()  { return CSThresh; }
		/** Get the data packet */
        public Object  getPkt()       { return pkt; }

        /**
         * Sets the error flag.
         */
        public void    setError(boolean error_) { error = error_; } 

        public Object clone() 
        {
			// the contract is between two components; don't clone pkt
			return new Message(error, RxPr, CPThresh, CSThresh, RXThresh, pkt);
		}

        public Contract getContract()
        { return INSTANCE; }

        public String toString(String separator_) {
            String str;
            str = "Node-Channel Message:" + separator_ + "error=" + error + separator_ + "RxPr=" + RxPr + separator_ + "CPThresh=" + CPThresh + "RXThresh=" + RXThresh + "CSThresh=" + CSThresh;
            str = str + separator_ + "Pkt=" + pkt.toString(); 
            return str;
        }
    }
}
