// @(#)DVPacket.java   2/2004
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

package drcl.inet.protocol.dv;

import java.util.Vector;

/**
Defines the format of the packet used by {@link DV}.
The format is simplified from what is defined in RFC2453.
*/
public class DVPacket implements drcl.ObjectCloneable
{ 
	// commands
	public static final int REQUEST = 1;
	public static final int UPDATE = 2;
	
	int cmd, version;
	Vector rte;

	public DVPacket()
	{ super(); }
	
	public DVPacket (int cmd_, int version_)
	{
		cmd = cmd_;
		version = version_;
	}

	private DVPacket (int cmd_, int version_, Vector rte_)
	{
		cmd = cmd_;
		version = version_;
		rte = rte_;
	}
	
	/** Returns the command of this packet. */
	public int getCommand()
	{	return cmd; }
	
	/** Returns the version of this packet. */
	public int getVersion()
	{	return version;	}

	public void addRTE(long dest_, long mask_, long nexthop_, int metric_)
	{
		RTE rte_ = new RTE(dest_, mask_, nexthop_, metric_);
		if (rte == null) rte = new Vector();
		rte.addElement(rte_);
	}
	
	public void setCommand(int value_)
	{ cmd = value_; }
	
	public void setVersion(int value_)
	{ version = value_; }
	
	public int getNumRTEs()
	{ return rte == null? 0: rte.size(); }
	
	public RTE getRTE(int index_)
	{ return (RTE)rte.elementAt(index_); }
	
	public RTE[] getRTEs()
	{
		RTE[] ss_ = new RTE[rte.size()];
		rte.copyInto(ss_);
		return ss_;
	}

	/*
	public void duplicate(Object source_)
	{
		DVPacket that_ = (DVPacket)source_;
		cmd = that_.cmd;
		version = that_.version;
		rte = (Vector)drcl.util.ObjectUtil.clone(that_.rte);
	}
	*/

	public Object clone()
	{ return new DVPacket(cmd, version, (Vector)rte.clone()); }

	public String toString()
	{
		if (cmd == REQUEST)
			return "DVv" + version + "_REQUEST";
		else
			return "DVv" + version + "_UPDATE,rte:" + rte;
	}

	static class RTE implements drcl.ObjectCloneable//extends drcl.DrclObj
	{
		long dest, mask, nexthop;
		int metric;
		
		public RTE ()
		{}

		public RTE (long dest_, long mask_, long nexthop_, int metric_)
		{
			dest = dest_;
			mask = mask_;
			nexthop = nexthop_;
			metric = metric_;
		}
		
		/*
		public void duplicate(Object source_)
		{
			super.duplicate(source_);
			RTE that_ = (RTE)source_;
			dest = that_.dest;
			mask = that_.mask;
			nexthop = that_.nexthop;
			metric = that_.metric;
		}
		*/

		public Object clone()
		{ return new RTE(dest, mask, nexthop, metric); }

		/** Retrieves the destination field from an RTE.  */
		public long getDestination()
		{ return dest; }
	
		/** Retrieves the mask field from an RTE. */
		public long getMask()
		{ return mask; }
	
		/** Retrieves the next-hop field from an RTE. */
		public long getNextHop()
		{ return nexthop; }
	
		/** Retrieves the metric field from an RTE. */
		public int getMetric()
		{ return metric; }

		public String toString()
		{ return dest + "/" + mask +":" + nexthop + ":" + metric; }
	}
}
