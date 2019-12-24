// @(#)Hellov.java   9/2002
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

package drcl.inet.core;

import java.util.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.Protocol;
import drcl.inet.data.*;
import drcl.inet.contract.*;

/**
 * Component that resides in a node and exchanges information with neighboring nodes.
 * This component behaves exactly the same as {@link Hello} and in addition,
 * distinguishes virtual interfaces from normal ones.
 * 
 * @see Hello
 */
public class Hellov extends Hello
{	
	Port vEvtPort = addEventPort(EVENT_VIF_PORT_ID);
	int vifStartIndex = Integer.MAX_VALUE;
	
	public Hellov()
	{ super(); }
	
	public Hellov(String id_)
	{ super(id_); }
					  
	public void reset()
	{ super.reset(); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Hellov that_ = (Hellov)source_;
		vifStartIndex = that_.vifStartIndex;
	}
	
	public String info()
	{
		return super.info() + "VIF start index: " + vifStartIndex + "\n";
	}
	
	protected void fireEvent(String evtName_, int ifindex_, NetAddress neighbor_)
	{
		if (ifindex_ < vifStartIndex)
			evtPort.exportEvent(evtName_, new NeighborEvent.Message(ifindex_, neighbor_), null);
		else {
			if (evtName_.equals(EVENT_IF_NEIGHBOR_UP))
				vEvtPort.exportEvent(EVENT_VIF_NEIGHBOR_UP,
					new NeighborEvent.Message(ifindex_, neighbor_), null);
			else
				vEvtPort.exportEvent(EVENT_VIF_NEIGHBOR_DOWN,
					new NeighborEvent.Message(ifindex_, neighbor_), null);
		}
	}
	
	protected InterfaceInfo[] _queryAll()
	{
		if (neighbors == null || neighbors.length == 0)
			return new InterfaceInfo[0];
		
		int length_ = Math.min(vifStartIndex, neighbors.length);
		InterfaceInfo[] tmp_ = new InterfaceInfo[length_];
		for (int i=0; i<length_; i++)
			tmp_[i] = (InterfaceInfo)drcl.util.ObjectUtil.clone(neighbors[i]);
		return tmp_;
	}
	
	
	//
	private void ___SCRIPT___() {}
	//
	
	public void setVIFStartIndex(int index_)
	{	vifStartIndex = index_;	}
	
	public int getVIFStartIndex()
	{ return vifStartIndex; }
}
