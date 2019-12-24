// @(#)McastTestApp.java   1/2004
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

package drcl.inet.application;

import drcl.comp.*;
import drcl.inet.InetPacket;
import drcl.inet.contract.IDConfig;
import drcl.inet.contract.McastHostEvent;
import drcl.inet.contract.PktSending;

/**
 * Application component to test multicast routing. 
 */
public class McastTestApp extends Component
{ 
	Port downPort = addPort("down", false);
	Port mcastPort = addPort(".service_mcast", false);

	public McastTestApp()
	{ super(); }

	public McastTestApp(String id_)
	{ super(id_); }

	/** To join a multicast group. */
	public void join(long group_)
	{
		IDConfig.add(group_, Double.NaN, mcastPort);
	}

	/** To leave a multicast group. */
	public void leave(long group_)
	{
		IDConfig.remove(group_, mcastPort);
	}

	/** To send data to a multicast group. */
	public void send(long group_, Object msg_, int size_)
	{
		debug("SEND to " + group_ + ": " + msg_ + ", size=" + size_);

		downPort.doSending(PktSending.getForwardPack(
								msg_, size_,
								drcl.net.Address.NULL_ADDR,
								group_,
								false,
								255,
								0));
	}

	protected void process(Object data_, Port inPort_)
	{
		if (data_ instanceof McastHostEvent.Message) {
			McastHostEvent.Message s_ = (McastHostEvent.Message)data_;
			long group_ = s_.getGroup();
			long src_ = s_.getSource();
			long srcmask_ = s_.getSourceMask();
			int ifindex_ = s_.getIfIndex();
			if (s_.isJoin())
				debug("JOIN to group " + group_);
			else
				debug("LEAVE group " + group_);
		}
		else if (data_ instanceof InetPacket) {
			long group_ = ((InetPacket)data_).getDestination();
			Object pkt_ = ((InetPacket)data_).getBody();
			int psize_ = ((InetPacket)data_).getPacketSize();
			int hsize_ = ((InetPacket)data_).getHeaderSize();
			debug("RECEIVE from " + group_ + ": " + pkt_ + ", size="
							+ (psize_ - hsize_));
		}
		else {
			debug("RECEIVE from " + inPort_ + ": " + data_);
		}
	}
}
