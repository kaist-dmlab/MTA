// @(#)CBTConstants.java   7/2003
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

package drcl.inet.protocol.cbt;

public interface CBTConstants
{ 
	// msg type
	public final static int 
		JOIN_REQUEST				= 0,
		JOIN_ACK					= 1,
		QUIT_NOTIFICATION			= 2,
		ECHO_REQUEST				= 3,	//
		ECHO_REPLY					= 4,	//
		FLUSH_TREE					= 5;	//

	public static String[] TYPES = {
		"JOIN_REQUEST",
		"JOIN_ACK",
		"QUIT_NOTIFICATION",
		"ECHO_REQUEST",
		"ECHO_REPLY",
		"FLUSH_TREE",
		null, null, null, null, null, null, null, null, null, null
	};

	// states
	public final static int INIT = 0;
	public final static int TRANSIENT = 1;
	public final static int CONFIRMED = 2;
	public final static int QUITTING = 3;

	public static final String[] STATES = {
		"INIT", "TRANSIENT", "CONFIRMED", "QUITTING"
	};

	// timer types
	public final static int JOIN_RTX = 0;
	public final static int JOIN_TIMEOUT = 1;
	public final static int QUIT_RTX = 2;
	public final static int ECHO = 3;
	public final static int ECHO_RTX = 4;
	public final static int UPSTREAM_EXPIRE = 5;
	public final static int DOWNSTREAM_EXPIRE = 6;
	public static final String[] TIMER_TYPES = {
		"join-rtx", "join/transient-timeout", "quit-rtx",
		"echo-request-timer", "echo-request-rtx",
		"upstream-expire-timer", "downstream-expire-timer"
	};
}
