// @(#)MacTimeoutEvt.java   1/2004
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

import java.math.*; 
import drcl.inet.*;
import drcl.net.*;

/**
 * This class defines MAC timeout event types.		 
 * @author Ye Ge
 */
public class MacTimeoutEvt  {
	int evt_type;
	static final int Nav_timeout      = 0;
	static final int IF_timeout       = 1;    // timeout while transmission is finished
	static final int Rx_timeout       = 2;    
	static final int Tx_timeout       = 3;    // timeout while no response received
	static final int Defer_timeout    = 4;
	static final int Backoff_timeout  = 5; 
	static final int ATIMEnd_timeout  = 6; 
	static final int Beacon_timeout   = 7; 
	static final int TBTT_timeout     = 8; 
	static final int Testing_timeout  = -1;
	static final String[] TYPES = {
			"NAV", "IF", "RX", "TX", "DEFER", "BACKOFF",
			"ATIM-End", "BEACON", "TBTT"
	};
	
	public MacTimeoutEvt(int evt_type_) {
		evt_type = evt_type_;
	}
	
	public void setType(int evt_type_) {
		evt_type = evt_type_;
	}

	public String toString()
	{
		if (evt_type == 0) return "MAC_TIMEOUT:TESTING";
		else return "MAC_TIMEOUT:" + TYPES[evt_type];
	}
}	


