// @(#)Routing.java   9/2002
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

package drcl.inet.protocol;

import drcl.comp.*;
import drcl.inet.data.*;
import drcl.inet.contract.RTConfig;

/** The base class for implementing a unicast/multicast routing protocol.
In particular, it provides a set of methods to access/maintain the routing
table in the {@link drcl.inet.CoreServiceLayer core service layer}. */
public abstract class Routing extends drcl.inet.Protocol
{
	/**
	 * The port for resolving ``route query'' from the core service layer.
	 * Bound with {@link drcl.inet.contract.RTLookup} as reactor.
	 */
	protected Port queryPort;
	{
		if (this instanceof UnicastRouting)
			queryPort = createUcastQueryPort();
		if (this instanceof McastRouting)
			queryPort = createMcastQueryPort();
	}
	
	/** The port that is bound with {@link drcl.inet.contract.RTConfig} as initiator. */
	protected Port rtconfigPort = createRTServicePort();
	
	
	public Routing()
	{ super(); }
	
	public Routing(String id_)
	{ super(id_); }
	
	/**
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	protected void addRTEntry(RTEntry entry_, double timeout)
	{
		RTConfig.add(entry_.getKey(), entry_, timeout, rtconfigPort);
	}

	/** 
	 * Replaces the route entry extension object 
	 * and leaves other fields intact via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void replaceRTEntry(RTKey key_, Object entryExtension_)
	{
		RTConfig.add(key_, new RTEntry(RTConfig.NEXT_HOP_NO_CHANGE, RTConfig.OUT_IFS_NO_CHANGE,
			entryExtension_), RTConfig.TIMEOUT_NO_CHANGE, rtconfigPort);
	}

	/** 
	 * Replaces the route entry extension object and timeout,
	 * and leaves other fields intact via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void replaceRTEntry(RTKey key_, Object entryExtension_, double timeout_)
	{
		RTConfig.add(key_, new RTEntry(RTConfig.NEXT_HOP_NO_CHANGE, RTConfig.OUT_IFS_NO_CHANGE,
			entryExtension_), timeout_, rtconfigPort);
	}

	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void addRTEntry(RTKey key_, int[] interfaces, Object entryExtension_, double timeout)
	{
		RTConfig.add(key_, new RTEntry(RTConfig.NEXT_HOP_NO_CHANGE,
			new drcl.data.BitSet(interfaces), entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void addRTEntry(RTKey key_, int interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.add(key_, new RTEntry(RTConfig.NEXT_HOP_NO_CHANGE,
			new drcl.data.BitSet(new int[]{interfaces}), entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void addRTEntry(RTKey key_, drcl.data.BitSet interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.add(key_, new RTEntry(RTConfig.NEXT_HOP_NO_CHANGE, interfaces,
			entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	protected void addRTEntry(RTKey key_, long nexthop_, int[] interfaces,
		Object entryExtension_, double timeout)
	{
		RTConfig.add(key_, new RTEntry(nexthop_, new drcl.data.BitSet(interfaces),
			entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	public void addRTEntry(RTKey key_, long nexthop_, int interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.add(key_, new RTEntry(nexthop_, new drcl.data.BitSet(new int[]{interfaces}),
			entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Adds/replaces a route entry via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */	
	public void addRTEntry(RTKey key_, long nexthop_, drcl.data.BitSet interfaces,
		Object entryExtension_, double timeout)
	{	
		RTConfig.add(key_, new RTEntry(nexthop_, interfaces, entryExtension_), timeout, rtconfigPort);
	}
	/** 
	 * Grafts a route entry to a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void graftRTEntry(RTKey key_, drcl.data.BitSet interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.graft(key_, interfaces, entryExtension_, timeout, rtconfigPort);
	}
	/** 
	 * Grafts a route entry to a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void graftRTEntry(RTKey key_, int interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.graft(key_, new drcl.data.BitSet(new int[]{interfaces}),
			entryExtension_, timeout, rtconfigPort);
	}	
	/** 
	 * Grafts a route entry to a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void graftRTEntry(RTKey key_, int[] interfaces, Object entryExtension_, double timeout)
	{	
		RTConfig.graft(key_, new drcl.data.BitSet(interfaces), entryExtension_, timeout, rtconfigPort);
	}		
	/** 
	 * Prunes a route entry from a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void pruneRTEntry(RTKey key_, int[] interfaces, Object entryExtension_, double timeout)
	{
		RTConfig.prune(key_, new drcl.data.BitSet(interfaces), entryExtension_, timeout, rtconfigPort);
	}
	/** 
	 * Prunes a route entry from a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void pruneRTEntry(RTKey key_, int interfaces, Object entryExtension_, double timeout)
	{
		RTConfig.prune(key_, new drcl.data.BitSet(new int[]{interfaces}),
			entryExtension_, timeout, rtconfigPort);
	}	
	/** 
	 * Prunes a route entry from a list of outgoing interfaces via {@link #rtconfigPort}.
	 * @param entryExtension_ The extension defined in {@link drcl.inet.data.RTEntry}.
	 * @see drcl.inet.contract.RTConfig
	 */		
	public void pruneRTEntry(RTKey key_, drcl.data.BitSet interfaces, Object entryExtension_, double timeout)
	{
		RTConfig.prune(key_, interfaces, entryExtension_, timeout, rtconfigPort);
	}	
	/** 
	 * Removes a route entry via {@link #rtconfigPort}.
	 * @param type see {@link drcl.inet.contract.RTConfig}.
	 */
	public Object removeRTEntry(RTKey key_, String type)
	{ return RTConfig.remove(key_, type, rtconfigPort); }
	
	/** 
	 * Removes all route entries associated with a multicast group via {@link #rtconfigPort}.
	 * @param destination the multicast group address.
	 * @see drcl.inet.contract.RTConfig
	 */
	public Object removeRTEntry(long destination)
	{
		return RTConfig.remove(new RTKey(-1, 0, destination, -1, -1, 0),
			RTConfig.MATCH_WILDCARD, rtconfigPort);
	}
	
	/** 
	 * Retrieves all the route entries via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public RTEntry[] removeAllRTEntries()
	{
		Object result_ = RTConfig.remove(new RTKey(0,0, 0, 0, 0, 0), RTConfig.MATCH_WILDCARD, rtconfigPort);
		if (result_ instanceof RTEntry) return new RTEntry[]{(RTEntry)result_};
		else return (RTEntry[])result_;
	}
	
	/** 
	 * Retrieves matched route entries via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public Object retrieveRTEntry(RTKey key_, String type)
	{ return RTConfig.retrieve(key_, type, rtconfigPort); }
	
	/** 
	 * Retrieves all the route entries via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public RTEntry[] retrieveAllRTEntries()
	{
		Object result_ = RTConfig.retrieve(new RTKey(0,0, 0, 0, 0, 0), RTConfig.MATCH_WILDCARD, rtconfigPort);
		if (result_ instanceof RTEntry) return new RTEntry[]{(RTEntry)result_};
		else return (RTEntry[])result_;
	}
	
	/** 
	 * Retrieves all the route entries of the same destination field via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public Object retrieveRTEntryDest(long destination)
	{
		return RTConfig.retrieve(new RTKey(-1, 0, destination, -1, -1, 0),
			RTConfig.MATCH_WILDCARD, rtconfigPort);
	}
	
	/** 
	 * Retrieves the best (longest matched) route entry of the same
	 * destination field via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public RTEntry retrieveBestRTEntryDest(long destination)
	{
		return (RTEntry)RTConfig.retrieve(
			new RTKey(-1, 0, destination, -1, -1, 0),
			RTConfig.MATCH_LONGEST, rtconfigPort);
	}
	
	/** 
	 * Retrieves all the route entries of the same source field via {@link #rtconfigPort}.
	 * @see drcl.inet.contract.RTConfig
	 */
	public Object retrieveRTEntrySrc(long source)
	{
		return RTConfig.retrieve(new RTKey(source, -1, -1, 0, -1, 0),
			RTConfig.MATCH_WILDCARD, rtconfigPort);
	}
}



