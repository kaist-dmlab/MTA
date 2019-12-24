// @(#)CBTInterface.java   5/2003
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

/** Holds the CBT-related information regarding a network interface for
 * a specific {@link CBTState CBT state}.
 * @see CBTState
 */
public class CBTInterface
{ 
	/** True if the interface leads to the upstream router
	 * and implies that the interface is also a router interface but not
	 * a host interface.  */
	public boolean isUpstream;
	/** True if the interface is a broadcast LAN. (not used) */
	public boolean isBroadcast;
	/** True if the interface is connected to at least an end host member
	 * and implies that the interface is not an upstream interface 
	 * (downstream). */
	public boolean isHostIf;
	/** True if the interface is connected to at least a router. */
	public boolean isRouterIf;
	/** Time when this interface expires (not refreshed by echo messages). */
	public double expireTime;

	public Object extension;

	CBTInterface()
	{}

	public CBTInterface(boolean upstream_, boolean broadcast_,
					boolean hostIf_, boolean routerIf_, Object extension_)
	{
		isUpstream = upstream_;
		isBroadcast = broadcast_;
		isHostIf = hostIf_;
		isRouterIf = routerIf_;
		extension = extension_;
	}

	public String toString()
	{
		return (isUpstream? "up": "down")
				+ (isBroadcast? ",bcast": ",p2p")
				+ (isHostIf? ",host": "")
				+ (isRouterIf? ",router": "")
				+ ",expire=" + expireTime
				+ (extension == null? "": ",ext=" + 
								drcl.util.StringUtil.toString(extension));
	}
}
