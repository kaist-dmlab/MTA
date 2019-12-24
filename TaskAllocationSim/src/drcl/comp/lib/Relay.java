// @(#)Relay.java   1/2004
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

package drcl.comp.lib;

import java.util.LinkedList;
import drcl.ObjectCloneable;
import drcl.comp.*;

public class Relay extends Extension
{
	Port[] endPoints = null;

	public Relay()
	{ super(); }
	
	public Relay(String id_)
	{ super(id_); }
	
	boolean copy = false, echo = false;
	
	/** Enables/disables copy of data when relaying the data to all the ports.*/
	public void setCopyEnabled(boolean v_)
	{ copy = v_; }
	
	/** Returns true if copy of data is enabled. */
	public boolean isCopyEnabled()
	{ return copy; }
	
	/** Enables/disables echo of data to the port where the data come. */
	public void setEchoEnabled(boolean v_)
	{ echo = v_; }
	
	/** Returns true if copy of data is enabled. */
	public boolean isEchoEnabled()
	{ return echo; }
		
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Relay that_ = (Relay)source_;
		copy = that_.copy;
		echo = that_.echo;
	}
	
	public String info()
	{
		return "Copy enabled: " + copy + "\n"
				+ "Echo enabled: " + echo + "\n"
				+ "End points=" + drcl.util.StringUtil.toString(endPoints)
					+ "\n";
	}
	
	public void reset()
	{ super.reset(); }

	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		if (endPoints == null) {
			setPortNotificationEnabled(true);
			Port[] pp = getAllPorts();
			LinkedList ll = new LinkedList();
			for (int i=0; i<pp.length; i++)
				if (pp[i] != null && pp[i] != infoPort) ll.add(pp[i]);
			endPoints = new Port[ll.size()];
			ll.toArray(endPoints);
		}

		int len_ = endPoints.length;
		if (len_ == 0) {
			drop(data_, "no port to relay the data");
			return;
		}

		boolean copy_ = copy && data_ instanceof ObjectCloneable;

		// should send data_ itself on "last" port
		int count_ = 0;
		if (echo) {
			if (copy_) {
				for (int i=0; i<len_-1; i++)
					endPoints[i].doSending(((ObjectCloneable)data_).clone());
				endPoints[len_-1].doSending(data_);
			}
			else {
				for (int i=0; i<len_; i++)
					endPoints[i].doSending(data_);
			}
			count_ = len_;
		}
		else if (copy_) {
			// no echo but need to copy
			if (endPoints[len_-1] == inPort_) {
				for (int i=0; i<len_-2; i++)
					endPoints[i].doSending(((ObjectCloneable)data_).clone());
				if (len_ >= 2)
					endPoints[len_-2].doSending(data_);
				count_ = len_-1;
			}
			else {
				for (int i=0; i<len_-1; i++)
					if (endPoints[i] != inPort_) {
						endPoints[i].doSending(
										((ObjectCloneable)data_).clone());
						count_++;
					}
				endPoints[len_-1].doSending(data_);
				count_++;
			}
		}
		else {
			// no echo and no copy
			for (int i=0; i<len_; i++)
				if (endPoints[i] != inPort_) {
					endPoints[i].doSending(data_);
					count_++;
				}
		}
 
		if (count_ == 0) drop(data_, "no other port to relay the data");
	}

	protected void portAdded(Port p)
	{ endPoints = null; }
}
