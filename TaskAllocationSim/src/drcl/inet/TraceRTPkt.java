// @(#)TraceRTPkt.java   12/2003
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

package drcl.inet;

import java.util.*;

/***
 * This class is a simple hack to support traceroute
 *
 * This new packet contains a Linked List useful to store InetAdress of
 * each Hop
 */


public class TraceRTPkt extends drcl.inet.InetPacket
{
    public static final int RT_REQUEST = 0; // Request a reverse trace-route
    public static final int RT_RESPONSE= 1; // Reverse trace-route response

    int iType;
    LinkedList listIP = new LinkedList();
	
    public TraceRTPkt(int iType, long destAddr_, int pktSize_)
    {
		super(drcl.net.Address.NULL_ADDR, // source
				destAddr_,
				InetConstants.PID_TRACE_RT, // protocol ID
				255, // TTL
				0, // hops
				iType == RT_REQUEST, // router alert
				CONTROL, // ToS
				0, // ID
				0, // flag
				0, // fragment
				null, // body
				pktSize_);
		this.iType= iType;
    }
    
    /**
     * This method adds the given ip address at
     * the end of the list
     *
     * @param ip 	The ip address
     */
    
    
    public void addHop(double now_, long ip)
    {
	listIP.addLast(new Double(now_));
	listIP.addLast(new Long(ip));
    }
    
    /**
     * This method returns the whole list of IP Address
     *
     * @return Object[] Content of IP list
     */
    
    public Object[] getList()
    {
	return listIP.toArray();
    }

    // ----- getType ------------------------------------------------
    /**
     * Return the TraceRT packet type.
     * - RT_REQUEST
     * - RT_RESPONSE
     */
    public int getType()
    {
	return iType;
    }

	public void setType(int type_)
	{ iType = type_; }

	public String _toString(String separator_)
	{
		return super._toString(separator_) + separator_
				+ (iType == RT_REQUEST? "RT_REQUEST:": "RT_RESPONSE:")
				+ listIP;
	}
} 
