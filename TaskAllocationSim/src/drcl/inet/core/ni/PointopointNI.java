// @(#)PointopointNI.java   9/2002
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

package drcl.inet.core.ni;

import drcl.data.*;
import drcl.comp.Port;
import drcl.net.*;
import drcl.comp.queue.ActiveQueueContract;

/**
The class implements the point-to-point network interface and emulates the physical
link propagation.
It is fully specified by the bandwidth, MTU and propagation delay.  
As a network interface, only one packet can be transmitted at a time.
However as an emulated link, multiple packets may be outstanding, the number of which
dependes on the propagation delay, bandwidth and packet size.
 */
public class PointopointNI extends drcl.inet.core.NI
{
	{ downPort.setType(Port.PortType_OUT); }
	
	public PointopointNI()
	{	super();	}
	
	public PointopointNI(String id_)
	{	super(id_);	}
	
	protected synchronized void process(Object data_, Port inPort_)
	{
		if (data_ == null) return;
		if (ready == 0) {
			error(data_, "process()", inPort_, "Not ready to txmit");
			return;
		}

		if (data_ == this) {
			ready++;
			pullPort.doSending(ActiveQueueContract.getPullRequest());
			return;
		}

		Packet pkt_ = (Packet)data_;
		if (pkt_.size > mtu) {
			if (isGarbageEnabled()) drop(data_, "pkt size > mtu(" + mtu + ")");
			return;
		}
	
		double readyTime = getTime() + (double)(pkt_.size << 3)/ bw;
		forkAt(pullPort, this, readyTime);
		if (linkEmulation)
			sendAt(downPort, pkt_, readyTime + propDelay);
		else
			sendAt(downPort, pkt_, readyTime);
	}
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		PointopointNI that_ = (PointopointNI)source_;
	}
	
	
	public void reset()
	{
		super.reset();
		ready = 1;
	}
	
	/** Time ready to transmit next packet. */
	protected int ready = 1;
	
	/** Returns true if the interface is ready to transmit more packets. */
	public boolean isReady() 
	{ return ready > 0; }
}
