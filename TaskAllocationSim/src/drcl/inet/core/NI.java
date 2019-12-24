// @(#)NI.java   4/2003
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

package drcl.inet.core;

import java.util.Hashtable;
import drcl.comp.*;
import drcl.comp.queue.ActiveQueueContract;

/**
 * Base class for modeling network interface card.
 */
public abstract class NI extends drcl.comp.Component implements InetCoreConstants
{

	static {
		setContract(NI.class, PULL_PORT_ID + "@", new ActiveQueueContract(Contract.Role_INITIATOR));
	}
	
	/** Port to pull data from data source (buffer).  */
	protected Port pullPort = addPort(PULL_PORT_ID, false/*not removable*/);

	/** Port to send out packets. */
	protected Port downPort = addPort(drcl.net.Module.PortGroup_DOWN, false/*not removable*/);

	/** The propagation delay of the emulated link. */
	protected double propDelay = 0.0;

	/** Link emulation is enabled? */
	protected boolean linkEmulation = false;

	/** Bandwidth of this network interface. */
	protected double bw = DEFAULT_BANDWIDTH;
	
	/** Maximum transmission unit.  Default is infinity. */
	protected int mtu = DEFAULT_MTU;
	
	public NI()
	{ super(); }
	
	public NI(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_) 
	{
		if (!(source_ instanceof NI)) return;
		super.duplicate(source_);
		NI that_ = (NI)source_;
		propDelay = that_.propDelay;
		linkEmulation = that_.linkEmulation;
		bw = that_.bw;
		mtu = that_.mtu;
	}
	
	
	public void reset()
	{
		super.reset();
		//rcvBuffer.clear();
	}
	
	public String info()
	{
		return   "Bandwidth = " + bw + ",  MTU = " + mtu + "\n"
			   + "State:" + (isReady()? "ready": "busy")
			   + (linkEmulation? ", Link emulation enabled with prop. delay " + propDelay: "") + "\n";
	}
	
	/** Returns the bandwidth of this network interface. */
	public double getBandwidth()
	{ return bw; }
	
	/** Sets the bandwidth of this network interface. */
	public void setBandwidth(double bw_)
	{ bw = bw_; }
	
	/** Returns the MTU, not including packet header, of this network interface. */
	public int getMTU()
	{ return mtu; }
	
	/** Sets the MTU, not including packet header, of this network interface. */
	public void setMTU(int mtu_)
	{ mtu = mtu_; }
	
	/** Returns true if the interface is ready to transmit more packets.  */
	public abstract boolean isReady();
	
	public void setLinkEmulationEnabled(boolean v_)
	{ linkEmulation = v_; }
	
	public boolean isLinkEmulationEnabled()
	{ return linkEmulation; }
	
	/** Returns the propagation delay of the emulated link. */
	public double getPropDelay() 
	{ return propDelay; }
	
	/** Enables link emulation and sets the propagation delay of the emulated link. */
	public void setPropDelay(double delay_) 
	{ propDelay = delay_; linkEmulation = true; }
}




