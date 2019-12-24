// @(#)AODV_TimeOut_EVT.java   7/2003
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

package drcl.inet.protocol.aodv; 
 
import drcl.net.*; 
 
/** 
 * Class AODV_TimeOut_EVT 
 * Class for handling tiom out event  
 * The timeout event objects dedicated for AODV package.
 * This timeout event is able to carrier an object when it is set. 
 * When the timeout happens, we can process the object carried 
 * in the timeout event.
 *  
 * @author Wei-peng Chen, Hung-ying Tyan 
 * @see AODV 
 * */ 
public class AODV_TimeOut_EVT { 
	public static final int AODV_TIMEOUT_BCAST_ID	= 0; 
	public static final int AODV_TIMEOUT_HELLO		= 1; 
	public static final int AODV_TIMEOUT_NBR		= 2; 
	public static final int AODV_TIMEOUT_ROUTE		= 3; 
	public static final int AODV_TIMEOUT_LOCAL_REPAIR	= 4; 
	public static final int AODV_TIMEOUT_DELAY_FORWARD	= 5; 
	public static final int AODV_TIMEOUT_DELAY_BROADCAST	= 6; 
	static final String[] TIMEOUT_TYPES = {"BCAST_ID", "HELLO", "NBR", "ROUTE", "LOCAL_REPAIR", "DELAY_FORWARD", "DELAY_BROADCAST"}; 
	 
	int		EVT_Type; 
	Object	EVT_Obj; 
	drcl.comp.ACATimer handle; // for cancelling event 
 
	public String toString() 
	{ return TIMEOUT_TYPES[EVT_Type] + ", " + EVT_Obj; } 
 
	/** 
	 * Constructor 
	 * @param tp_: Timeout type, now there is just RXT timeout 
	 */ 
	public AODV_TimeOut_EVT(int tp_) { 
		EVT_Type = tp_; 
	} 
 
	/** 
	 * Constructor 
	 * @param tp_: Timeout type, now there is just RXT timeout 
	 * @param obj_: the associated object with the time out event 
	 * (AODV_Interface, AODV_Neighbor or AODV_LSA) 
	 */ 
	public AODV_TimeOut_EVT(int tp_, Object obj_)  
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
} 
