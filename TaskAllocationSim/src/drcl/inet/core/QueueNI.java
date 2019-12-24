// @(#)QueueNI.java   4/2003
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

import drcl.data.*;
import drcl.comp.Port;
import drcl.net.*;
import drcl.util.queue.FIFOQueue;

/**
 * Base class for implementing a combo component that combines the functioins
 * of both a buffer and a network interface card.
 */
public abstract class QueueNI extends NI
{
	protected boolean byteMode = true;
	Port dequeuePort = addPort(".deq", false);

	public QueueNI()
	{	super();	}
	
	public QueueNI(String id_)
	{	super(id_);	}
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		QueueNI that_ = (QueueNI)source_;
		byteMode = that_.byteMode;
	}

	protected void transmit(Packet p)
	{
		if (dequeuePort.anyPeer())
			dequeuePort.doSending(p);
		double readyTime_ = getTime() + (double)(p.size << 3)/ bw;
		forkAt(pullPort, this, readyTime_);
		if (linkEmulation)
			sendAt(downPort, p, readyTime_ + propDelay);
		else
			sendAt(downPort, p, readyTime_);
	}
	
	public String info()
	{
		return super.info()
			+ drcl.util.StringUtil.finalPortionClassName(getClass()) + ", in "
			+ getMode() + " mode" + "\n"
			+ "Occupied/Capacity = " + getSize() + "/" + getCapacity() + "\n";
	}

	/**
	 * Sets the operation mode. Byte mode is the default and only acceptable mode.
	 * @param mode_ either "packet" or "byte".
	 */
	public void setMode(String mode_)
	{
		byteMode = mode_.equals(drcl.inet.InetConstants.BYTE_MODE);
	}
	
	public String getMode()
	{
		return byteMode? drcl.inet.InetConstants.BYTE_MODE:
			drcl.inet.InetConstants.PACKET_MODE;
	}

	/** Sets the capacity of the queue. */
	public abstract void setCapacity(int capacity_);
	
	/** Returns the capacity of the queue. */
	public abstract int getCapacity();
	
	/** Returns the current size of the queue. */
	public abstract int getSize();
}
