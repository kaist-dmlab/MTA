// @(#)Link.java   1/2004
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

package drcl.inet;

import java.util.LinkedList;
import drcl.ObjectCloneable;
import drcl.comp.*;

/**
The base class for modeling a physical link.
While one may extend this class to provide sophisticated link models,
this class implements a simple multi-endpoint link.
Data injected from one end point is propagated to all the other end points.
The propagation delay is the same for all the end points.
<p>The ports in the Link are numbered from 0, i.e., "0", "1", "2" etc.
 */
public class Link extends Component
{
	Port[] endPoints = null;

	public Link()
	{ super(); }
	
	public Link(String id_)
	{ super(id_); }
	
	public Link (double delay_)
	{
		setPropDelay(delay_);
	}
	
	protected void process(Object data_, Port inPort_) 
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
		if (len_ == 0) return;

		// should send data_ itself on "last" port
		if (!(data_ instanceof ObjectCloneable)) {
			for (int i=0; i<len_; i++)
				if (endPoints[i] != inPort_)
					send(endPoints[i], data_, propDelay);
		}
		else if (endPoints[len_-1] == inPort_) {
			for (int i=0; i<len_-2; i++)
				send(endPoints[i], ((ObjectCloneable)data_).clone(), propDelay);
			if (len_ >=2)
				send(endPoints[len_-2], data_, propDelay);
		}
		else {
			for (int i=0; i<len_-1; i++)
				if (endPoints[i] != inPort_)
					send(endPoints[i], ((ObjectCloneable)data_).clone(),
									propDelay);
			send(endPoints[len_-1], data_, propDelay);
		}
	}

	protected void portAdded(Port p)
	{ endPoints = null; }
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		Link that_ = (Link)source_;
		propDelay = that_.propDelay;
	}

	public String info()
	{
		return "propagation delay = " + propDelay + "\n"
			+ "End points=" + drcl.util.StringUtil.toString(endPoints) + "\n";
	}
	
	public void attach(Node n1_, Node n2_)
	{
		connect(n1_, false/* not on shared wire */);
		connect(n2_, false);
	}
	
	public void attach(Port p1_, Port p2_)
	{
		Port mine1_ = findAvailable();
		mine1_.connectTo(p1_);
		p1_.connectTo(mine1_);
		Port mine2_ = findAvailable();
		mine2_.connectTo(p2_);
		p2_.connectTo(mine2_);
	}
	
	/** The propagation delay of the link.  */
	protected double propDelay = 0.0;
	
	/** Returns the propagation delay of the link.  */
	public double getPropDelay() 
	{ return propDelay; }
	
	/** Sets the propagation delay of the link.  */
	public void setPropDelay(double delay_) 
	{ propDelay = delay_; }
}
