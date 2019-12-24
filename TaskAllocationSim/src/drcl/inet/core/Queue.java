// @(#)Queue.java   7/2003
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

import drcl.comp.*;

public abstract class Queue extends drcl.comp.queue.ActiveQueue implements InetCoreConstants
{
	{
		// input port, follows PktFilter
		addPort(new Port(Port.PortType_IN), drcl.net.Module.PortGroup_UP);
		_setRequesting(true);
	}
	
	public Queue()
	{ super(); }
	
	public Queue(String id_)
	{ super(id_); }
	
	
	public void reset()
	{
		super.reset();
		_setRequesting(true);
	}
	
	public String info(String prefix_)
	{
		return prefix_ + drcl.util.StringUtil.finalPortionClassName(getClass()) + ", in " + mode + " mode" + "\n"
			+ super.info(prefix_)
			+ prefix_ + "Blocked? " + !_isRequesting() + "\n";
	}
		
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Queue that_ = (Queue)source_;
		mode = that_.mode;
	}
	
	/** For a subclass to create the configuration port for other components to configure this component. */
	protected Port createConfigPort()
	{ return addServerPort(CONFIG_PORT_ID); }

	/**
	 * Sets the capacity (in bytes) of the queue.
	 * @param capacity_ the new capacity.
	 */
	public abstract void setCapacity(int capacity_);
	
	/** Returns the capacity of the queue. */
	public abstract int getCapacity();
	
	/** Returns the current size of the queue.  */
	public abstract int getSize();

	String mode = BYTE_MODE;
	
	/**
	 * Sets the operation mode. Byte mode is the default and only acceptable mode.
	 * @param mode_ either "packet" or "byte".
	 */
	public void setMode(String mode_)
	{ mode = mode_; }
	
	public String getMode()
	{ return mode; }
	
	protected final boolean isByteMode()
	{ return mode.equals(BYTE_MODE); }
	
	protected final boolean isPacketMode()
	{ return mode.equals(PACKET_MODE); }
}
