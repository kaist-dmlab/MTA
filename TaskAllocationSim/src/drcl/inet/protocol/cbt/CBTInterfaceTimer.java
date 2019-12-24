// @(#)CBTInterfaceTimer.java   5/2003
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

/** Interface timer is the timer for a specific interface.
 * It includes echo-timer, echo-rtx-timer,
 * upstream-expire-timer and downstream-expire-timer. */
public class CBTInterfaceTimer extends CBTTimer
{ 
	public int ifindex;
	public CBTPacket echoRequest;
	public int ntries;

	public CBTInterfaceTimer()
	{}

	public CBTInterfaceTimer(int type_, int if_)
	{
		type = type_;
		ifindex = if_;
	}

	/** For creating echo-rtx-timer. */
	public CBTInterfaceTimer(int type_, int if_, CBTPacket echoReq_,
					int ntries_)
	{
		type = type_;
		ifindex = if_;
		echoRequest = echoReq_;
		ntries = ntries_;
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof CBTInterfaceTimer)) return false;
		// no need to compare echoRequest
		return type == ((CBTTimer)o).type
			&& ifindex == ((CBTInterfaceTimer)o).ifindex;
	}

	public String toString()
	{ return CBTConstants.TIMER_TYPES[type] + ":" + ifindex
			+ (echoRequest == null?
							"," + ntries + "--" + echoRequest: ""); }
}
