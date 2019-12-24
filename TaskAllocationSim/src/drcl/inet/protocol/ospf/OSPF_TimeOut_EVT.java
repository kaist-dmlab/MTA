// @(#)OSPF_TimeOut_EVT.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.protocol.ospf;

import drcl.net.*;

/**
 * Class for handling timeout event.
 * 
 * @author Wei-peng Chen, Hung-ying Tyan
 * @see OSPF
 * */
public class OSPF_TimeOut_EVT {
	public static final int OSPF_TIMEOUT_HELLO				= 0;
	public static final int OSPF_TIMEOUT_LS_REFRESH			= 1;
	public static final int OSPF_TIMEOUT_LSMAXAGE_REACH		= 2;
	public static final int OSPF_TIMEOUT_ACK_DELAY_REACH	= 3;
	public static final int OSPF_TIMEOUT_NBR_INACTIVE		= 4;
	public static final int OSPF_TIMEOUT_DBDESC_RETRANS		= 5;
	public static final int OSPF_TIMEOUT_LSUPDATE_RETRANS	= 6;
	public static final int OSPF_TIMEOUT_LSREQ_RETRANS		= 7;
	public static final int OSPF_TIMEOUT_DELAY_FLOOD		= 8;
	static final String[] TIMEOUT_TYPES = {
		"HELLO", "LS_REFRESH", "MAXAGE_REACHED", "DELAY_ACK",
		"NBR_INACTIVE", "DBDESC_RETX", "LSA_RETX", "LSREQ_RETX", "DELAY_FLOOD"};
	
	int		EVT_Type;
	Object	EVT_Obj;
	drcl.comp.ACATimer handle; // for cancelling event

	public String toString()
	{ return TIMEOUT_TYPES[EVT_Type] + ", " + EVT_Obj; }

	/**
	 * Constructor
	 * @param tp_: Timeout type, now there is just RXT timeout
	 */
	public OSPF_TimeOut_EVT(int tp_) {
		EVT_Type = tp_;
	}

	/**
	 * Constructor
	 * @param tp_: Timeout type, now there is just RXT timeout
	 * @param obj_: the associated object with the time out event
	 * (OSPF_Interface, OSPF_Neighbor or OSPF_LSA)
	 */
	public OSPF_TimeOut_EVT(int tp_, Object obj_) 
	{
		EVT_Type = tp_;
		EVT_Obj  = obj_;
	}
		
	/**
	 * Functions to set or get information for a event
	 * 
	 */
	public void setEVT_Type(int tp_) {
		EVT_Type = tp_;
		return;
	}

	public int getEVT_Type() {
		return EVT_Type;
	}
		
	public void setObject(Object obj_) {
		EVT_Obj = obj_;
		return;
	}

	public Object getObject() {
		return EVT_Obj;
	}

	// Tyan: 05/08/2001, add this for cancelling LS_REFRESH timeout
	public boolean equals(Object o_)
	{
		if (o_ == this) return true;
		if (EVT_Type != OSPF_TIMEOUT_LS_REFRESH
			|| !(o_ instanceof OSPF_TimeOut_EVT))
			return false;

		OSPF_TimeOut_EVT that_ = (OSPF_TimeOut_EVT)o_;
		return EVT_Type == that_.EVT_Type && EVT_Obj == that_.EVT_Obj;
	}
}
