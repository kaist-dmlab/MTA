// @(#)DVMRP.java   1/2004
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

package drcl.inet.protocol.dvmrp;

import java.util.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.InetPacket;
import drcl.inet.data.*;
import drcl.inet.contract.*;
import drcl.util.scalar.LongVector;
import drcl.inet.protocol.Routing;
import drcl.inet.protocol.McastRouting;

// XXX: when to remove a fc entry?  when a mcast session is closed,
// all the route entries are still kept in the network and they will be kept
// forever...
// XX: need to carefully examine the procedures that modify the prune/graft
// 	states in the forwarding cache when there are changes in the routing 
// 	table
/*
The following is excerpted from table 2 of the DVMRPv3 Internet Draft.
The document does not discuss when to remove a forwarding table entry.
------
   The following table provides a summary of the DVMRP timing
   parameters:

                   Parameter                  Value (seconds)
         ----------------------------------------------------------
         Probe Interval                  10
         Neighbor Time-out Interval      35
         Minimum Flash Update Interval   5
         Route Report Interval           60
         Route Expiration Time           140
         Route Hold-down                 2 x Route Report Interval
         Prune Lifetime                  variable (< 2 hours)
         Prune Retransmission Time       3 with exp. back-off
         Graft Retransmission Time       5 with exp. back-off
         ----------------------------------------------------------
*/
/* -----------------------------------------------------------
BOOKKEEPING

<ol>
<li> Routing table (rt) and extension (rtExt).
<li> Forwarding cache extension (fcExt).
<li> Timers.
</ol>
The dependent list maintained in rtExt must be sync'ed with the prune states
(pruneExpires) in corresponding fcExt's (source of fc entries matches to dest
of the rt entry) in the occurances of the route change (upstream, downstream
changes) events.
-----------------------------------------------------------
EVENT HANDLING

The following lists of pseudo codes serve as the outline of this implementation
and the accounts for what differs from the DVMRPv3 internet draft.

Neighbor Up (_neighborUpEventHandler())
	If this component does not operate on the interface, return.
	Add a routing table entry if the local NetAddress on that interface is
		a summary of a network (edge interface).  The entry is used to store
		downstream dependents for the sources from the network.  This is CIDR
		hierarchical routing stuff.
	Send a request to the new neighbor.
	Set up/reset the regular update timer.

Neighbor Down (_neighborDownEventHandler())
	for each rt entry
		If next hop is the down neighbor
			mark RT metric infinity
			set up delete timer (enter the hold-down period)
		else
			cancel dependent if neighbor is one of the dependents for the entry.
			sync fc entries with the rt entry
			
		If there is any change in RT && no triggered update timer is set,
			set up a triggered update timeout (_activateTriggeredUpdateTimer()).
	[NOTE-0] The FC entries will be modified/removed when route updates offer
		alternative	route.

Receipt of a Route Report (from a neighbor) (_routeUpdateHandler())
	for each RTE in the message
		find the matched RT entry
		if the entry does not exist and the metric in RTE is not valid(>= 2*INF)
			then continue to next RTE.
		if there is a change (i.e., no matched entry is found and the received
			metric is less than INF || next hop is	the neighbor and metric is
			changed || next hop is not the neighbor	and the RTE has smaller
			metric)
			Mark the entry as "changed".
			if (no matched entry)
				create and reset a dependents list for this new entry
			else 
				clone the rtExt and adopt the new metric
				if (next hop is not the neighbor)
					if the neighbor was dependent, 
						cancel the dependency, 
			if (metric in RTE < INF)
				add/modify the rt entry to adopt new metric and/or next hop
				reset the route timer for the entry
				if (next hop is not the neighbor), then it is "change of 
					upstream" and possibly, downstream:
					sync fc entries with the rt entry (__syncFCWithRT())
			else
				the route is invalidated by the [next-hop] neighbor,
				the entry enters hold-down period
		else if neighbor is next hop (and reports the same metric)
			reset the route timer for the entry
		if (neighbor is not next hop) then update the dependents list by 
			checking:
			if (metric in RTE > INF)
				set the neighbor as dependent
				cancel the previous dependent setting if the interface changes
			else if (neighbor was dependent)
				cancel the dependent setting
			if dependent setting changes, then it is the "change of downstream"
				sync fc entries with rtExt (__syncFCWithRT())
	if there is any change in RT && no triggered update timer is set
		set up a triggered update timeout

Group Join (mcastHostJoinEventHandler())
	create the "group" entry if necessary
	set up the outgoing interface in the group entry
	for each fc entry of the group, (_graft())
		graft on the interface
		send graft upstream if sent prune before
	[NOTE-1] if a connecting subnet contains group member hosts, then
		the forwarding cache should contain some entry to the subnet for the 
		key.  This entry is called a "group" entry.  The entry is not used for
		routing mcast packets so the entry is designed to contain
		wildcard source and -1 for the incoming interface.

Group Leave (mcastHostLeaveEventHandler())
	for each fc entry of the group, (_prune())
		prune the interface if the interface is not a downstream dependent 
		interface
		(dont send graft upstream even if no downstream dependent
		 next time when a data packet arrives will do it)

Mcast Query (Arrival of First Data Packet) (routeQueryHandler())
	if the fc entry exists, then send a prune (if not yielded by a prune retx 
	timer) and return null
	otherwise, form the source mask, dependent ifs, upstream and upstream if 
	from all the rt entries the destination of each matches the source of the 
	packet (there may be multiple matched rt entries if the router is an area 
	boundary router) create the outgoing ifs from the dependent ifs and the 
	"group" entry (if exists)
	if (not multihomed && outgoing ifs is null)
		send a prune upstream
	add a new fc entry no matter if a prune is sent upstream 
		(so that later, the router knows whether to send a graft when a host 
		 joins the group)

Receipt of Prune (_pruneHandler())
	retrieve the fcExt of the fc entry
	if (the fc entry does not exist or the inteerface is not a dependent or 
		been pruned)
		return
	if (the interface connects to some host network (check the "group" entry))
		return
	prune the interface
	if (!multihomed and incoming if >= 0 and outgoing if of the fc entry is 
		null)
		send a prune upstream
	[NOTE-2] the v3 internet draft says to prune all the fc entries of the 
		same mcast group having the same downstream interface

Receipt of Graft (_graftHandler())
	retrieve the fc entries of the same group
	for each fc entry (_graft())
		graft the interface and remove the prune state
		send graft upstream if (sent prune and no pending graft)
	send graft-ack
	[NOTE-3] This implementation conforms to the v3 internet draft in which	
		all the forwarding cache entries of the same group having the same
		downstream dependent interface should graft back the interface.

Receipt of Graft-Ack (_graftAckHandler())
	retrieve the fc entry
	for the fc entry, clear the pending graft and the sent-prune-upstream flag

Receipt of a Route Request Message (from a neighbor) (dataArriveAtDownPort())
	same as the sending of route report in a regular update, except the route 
	report is only sent to the requested neighbor

Regular Update Timeout (timeout())
	send a route report to each (interested) neighbor
	refresh the timer
	[NOTE-4] The route report is composed by the internal rt entries.  The 
		following entries are filtered out in the report:
		- the "loopback" entries (metric = 0)
		- the entries being masked by the local NetAddress of the [edge] 
		  interface
		- use reverse poison if the neighbor is the next hop of the rt entry
		- the local NetAddress of the interface is included in the report if 
		  it is an edge interface

Triggered Update Timeout (timeout())
	send a route report to each neighbor
		the route report is composed of only the "changed" rt entries, the 
		rules of filtering the rt entries are the same as in [NOTE-4].
	clear the "changed" state in the entries

Route Timeout (timeout())
(should be only due to invalidation (advertizing INF metric) by the neighbor)
	set the metric of rt entry to be INF
	mark it "changed"
	set up a route hold-down timer
	set up a triggered update timer

Route Hold-down Timeout (timeout())
	delete the RT entry from CSL
	remove the related FC entries

Forwarding Cache Entry Timeout (timeout())
	remove expired prune states and update the outgoing ifs of the fc entry
	if (any prune state being removed) clear pending graft and the sent-prune-upstream flag
	if graft retx timer expires
		send another graft and do backoff
	set up another fc entry timer if applicable

-----------------------------------------------------------
HIERARCHICAL ROUTING

This component supports hierarchical routing by distinguishing core, edge
and host interfaces:
- Core interface:
  Connected to a node of the same domain.
  Does not exchange routes about itself (RelayRouteInfoOnlyEnabled).
- Edge interface:
  Connected to a node of a different domain.
  Does not exchange routes about itself (RelayRouteInfoOnlyEnabled).
  DVMRP filters route info with the NetAddress set in the InterfaceInfo.
- Host interface:
  Connected to a network of hosts.
The type of interfaces can be discovered automatically if local NetAddress is 
set appropriately:
- Host interface: never receives DVMRP route exchange packets.
- Core interface: receives DVMRP route exchange packets and the local NetAddress
  is the default address of the router w/o mask.
- Edge interface: receives DVMRP route exchange packets and the local NetAddress
  is the summary of the network represented.  The router must own an identity
  that is part of the network represented.  (The CIDR concept)

This implementation supposes all the interfaces (indicated in <code>ifset</code>
are either core or edge interface.
 */

/**
This component implements the <em>distance vector multicast routing protocol</em>
(DVMRP).  

<p>The implementation almost complies with the DVMRPv3 Internet Draft
except that:
<ul>
<li>(1) handling of some situations are not mentioned in the draft, and
<li>(2) it takes advantage of the services from the {@link drcl.inet.CoreServiceLayer
core service layer} in the INET framework.
</ul>
 As for (1), we provide our own solutions to those situations which
can be seen as augments to the internet draft in our opinion.  Due to (2), a 
couple of things in this implementation are quite different from what would be 
in a real implementation.  For example, the "probe" messages for discovering 
and maintaining neighboring information are replaced by the neighbor up/down
events provided by the core service layer.  Moreover, this implementation does 
not handle IGMP directly.  Handling of IGMP is separated from this 
implementation and should be provided by another component (probably the core 
service layer).  This implementation expects the
{@link drcl.inet.contract.McastHostEvent multicast host events} from a 
predefined event port.  The events summarize the IGMP activities that are only 
interested in by a multicast routing protocol such as DVMRP.

<p>This component treats the tunnel (virtual) interfaces equally with the 
normal ones.  The tunnel interfaces are configured in the core service layer.
One may configure this component to operate on a specific set of interfaces
({@link #setIfset(drcl.data.BitSet)}).
*/
public class DVMRP extends Routing implements McastRouting
{	
	public String getName()
	{ return "dvmrp"; }

	Port idport = createIDServicePort(); // for ID lookup service
	Port ifport = createIFQueryPort();  // for Interface query service
	
	{
		createIFEventPort(); // to receive neighbor events
		createVIFEventPort(); // to receive virtual neighbor events
		createMcastHostEventPort(); // to receive mcast host join/leave events
	}
	
	// the DVMRPv3 Internet Draft
	final static int VERSION = 3;
	final static DVMRPTimingPack DEFAULT_TIMING = new DVMRPTimingPack();
	final static String TRIGGERED_UPDATE_TIMER = "Triggered Update Timeout";
	final static String REGULAR_UPDATE_TIMER = "Regular Update Timeout";

	/**
	 * Metric of infinity.
	 * Recommended in the DVMRPv3 Internet Draft.
	 */
	public final static int INFINITY = 32;
	
	/**
	 * For reaching a DV neighbor.
	 */
	final static int MAX_HOP = 1;
	
	/**
	 * Routing table entry size.
	 */
	final static int RTSIZE = 14;
	
	Object triggeredUpdateTimer = null;
		// null if no trigger timeout is currently set up

	public final static int DEBUG_IO          = 0; // io operations: send rcv...
	public final static int DEBUG_TIMEOUT     = 1; // timeout events
	public final static int DEBUG_ROUTE       = 2; // route updates
	public final static int DEBUG_SEND_UPDATE = 3; // update neighbors
	public final static int DEBUG_MCAST_QUERY = 4;
	public final static int DEBUG_PRUNE       = 5; // 
	public final static int DEBUG_GRAFT       = 6; // 
	public final static int DEBUG_DEPENDENT   = 7; // 
	public final static int DEBUG_SYNC_FC     = 8; // 
	private final static String[] DEBUG_LEVEL_NAMES = {
		"debug_io",
		"debug_timeout",
		"debug_route",
		"debug_send_update",
		"debug_mcast_query",
		"debug_prune",
		"debug_graft",
		"debug_dependent",
		"debug_sync_fc"
	};
	
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVEL_NAMES; }
	
	/**
	 * Set of interfaces that this DVMRP instance operates on.
	 * Default is null, meaning including all normal interfaces but no virtual
	 * ifs.
	 */
	drcl.data.BitSet ifset = null;

	/** Metric for each outgoing interface.  XX: Not yet used in this 
	 * implementation. */
	int[] metrics = null;
	
	/** The routing table maintained by DVMRP itself. */
	drcl.data.Map routingTable = 
		drcl.data.Map.getBestImplementationForLongestMatch();

	/** The timing parameters used by this DVMRP instance. */
	DVMRPTimingPack timing = DEFAULT_TIMING;

	ACATimer regularTimerHandler = null;
												  
	public DVMRP()
	{ super(); }

	public DVMRP(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	public String info()
	{
		try {
		StringBuffer sb_ =
				new StringBuffer("Entry:<key>\t<next_hop>-<out_ifs>\t<"
						+ DVMRPRTExtension.getTitle() + ">\t<timeout>\n");
		MapKey[] keys_ = routingTable.getAllKeys();
		Object[] entries_ = routingTable.getAllEntries();
		for (int i=0; i<keys_.length; i++) {
			RTEntry entry_ = (RTEntry)entries_[i];
			RTKey key_ = (RTKey)keys_[i];
			Object extension_ = entry_.getExtension();
			if (key_ != null)
				if (entry_ != null) {
					long nexthop_ = entry_.getNextHop();
					sb_.append(key_ + "   \t"
						+ (nexthop_ == drcl.net.Address.NULL_ADDR? "??-":
								nexthop_ + "-")
						+ (entry_.getOutIf() == null? "{}": "{"
								+ entry_.getOutIf().toString() + "}")
						+ (extension_ == null? "": "\t" + extension_.toString())
						+ "\ttimeout:" + entry_._getTimeout() + "\n");
				}
				else sb_.append(key_.toString() + "\tnull entry\n");
		}
			
		sb_ = new StringBuffer("ifset: "
				+ (ifset == null? "all physical ifs": ifset.toString()) + "\n"
				+ (sb_.length() == 0? "No entry in the routing table\n":
						sb_.toString()));
		
		return sb_.toString();
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return e_.toString();
		}
	}
	
	public void reset()
	{
		super.reset();
		triggeredUpdateTimer = null;
		regularTimerHandler = null;
	}
	
	// Implements {@link drcl.inet.Protocol#neighborUpEventHandler}.
	protected void neighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{ _neighborUpEventHandler(ifindex_, neighbor_, inPort_); }
	
	// Implements {@link drcl.inet.Protocol#vNeighborUpEventHandler}.
	protected void vNeighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{ _neighborUpEventHandler(ifindex_, neighbor_, inPort_); }
	
	void _neighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		// checks if the interface is in the ifset
		if (ifset != null && !ifset.get(ifindex_)) return;
		
		super.neighborUpEventHandler(ifindex_, neighbor_, inPort_);
		
		// add a route entry if the local NetAddress on that interface is
		// a summary of a network
		NetAddress local_ = IFQuery.getLocalNetAddress(ifindex_, ifport);
		if (local_.getMask() != -1) {
			RTKey rtKey_ = new RTKey(0, 0, local_.getAddress(),
							local_.getMask(), 0, 0);
			RTEntry rtEntry_ = (RTEntry)_retrieveRTEntry(rtKey_, 
							RTConfig.MATCH_EXACT);
			if (rtEntry_ == null) {
				DVMRPRTExtension rtExt_ = new DVMRPRTExtension(0);
				__initDependents(local_.getAddress(), local_.getMask(), rtExt_);
				rtEntry_ = _addRTEntry(rtKey_, Address.NULL_ADDR, -1, rtExt_,
								Double.NaN);
			}
		}
		
		// send a route request to the neighbor
		DVMRPRTPacket p_ = new DVMRPRTPacket(DVMRPRTPacket.REQUEST, VERSION);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("SEND REQUEST to " + neighbor_);
		forward(p_, p_.size, Address.NULL_ADDR, neighbor_.getAddress(), false, 
						MAX_HOP, InetPacket.CONTROL, ifindex_);
		if (regularTimerHandler != null) cancelTimeout(regularTimerHandler);
		regularTimerHandler = setTimeout(REGULAR_UPDATE_TIMER,
						timing.regularUpdatePeriod);
	}
	
	// Implements {@link drcl.inet.Protocol#neighborDownEventHandler}.
	protected void neighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{ _neighborDownEventHandler(ifindex_, neighbor_, inPort_); }
	
	// Implements {@link drcl.inet.Protocol#vNeighborDownEventHandler}.
	protected void vNeighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{ _neighborDownEventHandler(ifindex_, neighbor_, inPort_); }
	
	void _neighborDownEventHandler(int ifindex_, NetAddress neighborNetAddr_, 
					Port inPort_)
	{
		// checks if the interface is in the ifset
		if (ifset != null && !ifset.get(ifindex_)) return;
		
		super.neighborDownEventHandler(ifindex_, neighborNetAddr_, inPort_);
		long neighbor_ = neighborNetAddr_.getAddress();
		RTEntry[] all_ = _retrieveAllRTEntries();
		if (all_ == null) return;
					
		// for each RT entry
		//		if next hop to the down neighbor
		//			mark RT metric infinity
		//			set up delete timer
		//		else
		//			cancel dependent if neighbor is one of the dependents for 
		//			the entry
		// if there is any change in RT && no triggered update timer is set
		//		set up a triggered update timeout
		boolean anychange_ = false;
		for (int i=0; i<all_.length; i++) {
			RTEntry entry_ = all_[i];
			DVMRPRTExtension ext_ = (DVMRPRTExtension)entry_.getExtension();
			if (ext_ == null) continue;
			if (entry_.getNextHop() != neighbor_) {
				int index_ = ext_.getDependentIndex(neighbor_);
				if (index_ >= 0) {
					ext_.setDependent(index_, Address.NULL_ADDR);
					__syncFCWithRT(entry_);
				}
				continue;
			}
			if (ext_.metric < INFINITY) {
				ext_.metric = INFINITY;
				ext_.changed = true;
				anychange_ = true;
				_addRTEntry(entry_.getKey(), entry_, 
								timing.deleteTimeoutPeriod);
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_ROUTE) 
										|| isDebugEnabledAt(DEBUG_TIMEOUT)))
					debug("hold-down " + entry_ + " due to NEIGHBOR TIMEOUT: "
									+ neighbor_);
			}
		}
		if (anychange_) 
			_activateTriggeredUpdateTimer();
	}
	
	void _activateTriggeredUpdateTimer()
	{
		if (triggeredUpdateTimer == null) {
			triggeredUpdateTimer = TRIGGERED_UPDATE_TIMER;
			setTimeout(triggeredUpdateTimer, timing.triggeredUpdateDelayPeriod);
		}
	}
	
	boolean _isTriggeredUpdateTimerSet()
	{
		return triggeredUpdateTimer != null;
	}
	
	// send updates (the RT entries) to neighbors
	// consider reverse poison and if mask
	void _updateNeighbors(RTEntry[] ee_, int incomingIf_,
					long specificNeighbor_)
	{
		if (ee_ == null) ee_ = new RTEntry[0];
		InterfaceInfo[] all_ = IFQuery.getInterfaceInfos(ifset, ifport);
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("PREPARE UPDATE: "
				  + (specificNeighbor_ == Address.NULL_ADDR? "":
						  "for " + specificNeighbor_ + ", ")
				  + drcl.util.StringUtil.toString(ee_)); 
		
		// go through each neighbor at each interface
		for (int i=0; i<all_.length; i++) {
			if (incomingIf_ >= 0 && incomingIf_ != i) continue;
			InterfaceInfo if_ = all_[i];
			if (if_ == null) continue;
			NetAddress myselfaddr_ = if_.getLocalNetAddress();
			long myself_ = myselfaddr_.getAddress();
			long maskedMyself_ = myselfaddr_.getMaskedAddress();
			long myselfMask_ = myselfaddr_.getMask();
			NetAddress[] neighbors_ = null;
			if (specificNeighbor_ == Address.NULL_ADDR)
				neighbors_ = if_.getPeerNetAddresses();
			else
				neighbors_ = new NetAddress[]{new NetAddress(specificNeighbor_,
								-1)};
			if (neighbors_ == null || neighbors_.length == 0) continue;
			
			for (int j=0; j<neighbors_.length; j++) {
				if (neighbors_[j] == null) continue;
				long neighbor_ = neighbors_[j].getAddress();
				
				DVMRPRTPacket pkt_ = new DVMRPRTPacket(DVMRPRTPacket.UPDATE,
								VERSION);

				int nRTEs_ = 0;
				for (int k=0; k<ee_.length; k++) {
					if (ee_[k] == null) continue;
					//if (isDebugEnabled()
					//	&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
					//	debug("TO NEIGHBOR " + myselfaddr_+ "-->" + neighbor_ 
					//		+ ": " + ee_[k]);
					RTKey key_ = ee_[k].getKey();
					long nexthop_ = ee_[k].getNextHop();
					long dest_ = key_.getDestination();
					long destmask_ = key_.getDestinationMask();
					DVMRPRTExtension e_ = 
							(DVMRPRTExtension)ee_[k].getExtension();
					
					// Internal routing entry, keep this to myself
					if (e_.metric == 0) {
						if (isDebugEnabled()
							&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
							debug("- TO NEIGHBOR " + myselfaddr_+ "-->"
								+ neighbor_ + ": " + ee_[k] 
								+ "-- internal entry");
						ee_[k] = null; continue;
					}
					
					if (nexthop_ == neighbor_) {
						// reverse poison
						pkt_.addRTE(dest_, destmask_, nexthop_, INFINITY 
										+ e_.metric);
						nRTEs_++;
						if (isDebugEnabled()
							&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
							debug("+ TO NEIGHBOR " + myselfaddr_+ "-->" 
								+ neighbor_ + ": " + ee_[k] 
								+ "-- poison reverse");
						continue; // bypass mask test
					}
					
					// mask test
					if (maskedMyself_ != (dest_ & myselfMask_)) {
						// hide next hop
						long tmpNexthop_ = nexthop_;
						if (maskedMyself_ == (nexthop_ & myselfMask_)) {
							tmpNexthop_ = myself_;
						}
					
						if (isDebugEnabled()
							&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
							debug("+ TO NEIGHBOR " + myselfaddr_+ "-->"
								+ neighbor_ + ": " + "(" + dest_ + ","
								+ destmask_ + ")" + " nexthop:"
								+ (tmpNexthop_ == nexthop_? nexthop_ + "":
										nexthop_ + "->" + tmpNexthop_)
								+ "--PASS MASK: " + maskedMyself_+ " != " 
								+ dest_ + " & " + myselfMask_ + "("
								+ (dest_ & myselfMask_) + ")");
						pkt_.addRTE(dest_, destmask_, tmpNexthop_, e_.metric);
						nRTEs_++;
						continue;
					}

					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND_UPDATE))
						debug("- TO NEIGHBOR " + myselfaddr_+ "-->"
							+ neighbor_ + ": " + ee_[k] + "-- COVERED: "
							+ ee_[k] + " covered by " + myselfaddr_);
					// don't add entry that is masked by the mask at the 
					// interface, this entry will be covered by the entry of 
					// myself that is artificially added below
					//pkt_.addRTE(myself_, myselfMask_, nexthop_, e_.metric);
				} // end k, loop on ee_ (RTEntry[])

				if (myselfMask_ != -1) { // edge interface
					nRTEs_++;
					pkt_.addRTE(myself_, myselfMask_, myself_, 0);
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND_UPDATE))
						debug("+ TO NEIGHBOR " + myselfaddr_+ "-->"
									+ neighbor_ + "-- edge interface");
				}
				
				if (nRTEs_ > 0) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
						debug("SEND " + pkt_);
					forward(pkt_, pkt_.size, myself_, neighbor_, false,
								MAX_HOP, InetPacket.CONTROL, i);
				}
			} // end j, loop on neighbors_ (NetAddress[])
		} // end i, loop on all_ (InterfaceInfo[])
	}
	
	protected void process(Object data_, Port inPort_)
	{
		lock(this);
		super.process(data_, inPort_);
		unlock(this);
	}

	protected void timeout(Object data_)
	{ 
		long source = IDLookup.getDefaultID(idport);
		
		if(data_ == REGULAR_UPDATE_TIMER) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug("REGULAR UPDATE");
			RTEntry[] all_ = _retrieveAllRTEntries();
			if (all_ == null) return;
			_updateNeighbors(all_, -1, Address.NULL_ADDR);
			setTimeout(REGULAR_UPDATE_TIMER, timing.regularUpdatePeriod);
		}
		else if (data_ == triggeredUpdateTimer) {
			triggeredUpdateTimer = null;
			//	Go over the RT to see get changed RT entries
			//	for each changed RT entry, mark "unchanged"
			//	if any change
			//		send out an update to all neighbors (consider poison 
			//		reverse and if mask)
			RTEntry[] all_ = _retrieveAllRTEntries();
			if (all_ == null) return;
				
			Vector v_ = new Vector(); // store changed entries
			for (int i=0; i<all_.length; i++) {
				RTEntry entry_ = all_[i];
				DVMRPRTExtension ext_ = (DVMRPRTExtension)entry_.getExtension();
				if (ext_ == null || !ext_.changed) continue;
				ext_.changed = false;
				v_.addElement(entry_);
			}
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug("TRIGGERED UPDATE: # of changed entries = " + v_.size());
			if (v_.size() > 0) {
				RTEntry[] tmp_ = new RTEntry[v_.size()];
				v_.copyInto(tmp_);
				_updateNeighbors(tmp_, -1, Address.NULL_ADDR);
			}
		}
		else if (data_ instanceof RTEntry) {
			RTEntry entry_ = (RTEntry)data_;
			long nexthop_ = entry_.getNextHop();
			double now_ = getTime();
			if (nexthop_ == Address.NULL_ADDR) {
				// forwarding cache entry
				DVMRPFCExtension fcExt_ = 
						(DVMRPFCExtension)entry_.getExtension();
				drcl.data.BitSet graftIfs_ = __removePruneStates(fcExt_, now_);
				if (graftIfs_.getNumSetBits() > 0) {
					// update forwarding cache
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_TIMEOUT)
											|| isDebugEnabledAt(DEBUG_PRUNE)))
						debug("PRUNE-TIMEOUT: cancel prune on ifs " +graftIfs_);
					graftRTEntry(entry_.getKey(), graftIfs_, fcExt_, 0.0);
					fcExt_.clearPendingGraft();
				}
				if (fcExt_.hasPendingGraft() && now_ >=fcExt_.graftAckTimeout) {
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_TIMEOUT)
											|| isDebugEnabledAt(DEBUG_GRAFT)))
						debug("RETX GRAFT: " + entry_);
					__sendGraftOn(now_, entry_, false);
				}
				__setFCExtTimer(entry_, fcExt_, now_);
			}
			else {
				// routing table entry
				double timeout_ = entry_._getTimeout();
				if (Double.isNaN(timeout_))
					return;
				if (now_ < timeout_) {
					setTimeoutAt(entry_, timeout_);
					return;
				}
				DVMRPRTExtension rtExt_ = 
						(DVMRPRTExtension)entry_.getExtension();
				if (rtExt_.metric != INFINITY) {
					// enter hold-down period
					rtExt_.changed = true;
					rtExt_.metric = INFINITY;
					_addRTEntry(entry_.getKey(), entry_, 
									timing.deleteTimeoutPeriod);
					setTimeoutAt(entry_, entry_._getTimeout());
					_activateTriggeredUpdateTimer();
					
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
						debug("hold-down " + entry_ + " due to route timeout");
				}
				else {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
						debug("REMOVE " + entry_ + " due to route timeout");
					
					RTKey rtKey_ = entry_.getKey();
					RTKey fcKey_ = new RTKey(rtKey_.getDestination(),
												rtKey_.getDestinationMask(),
												0, 0, 0, 0);
					
					// remove related fc entries
					removeRTEntry(fcKey_, RTConfig.MATCH_WILDCARD);
					// remove the rt entry from the routing table
					_removeRTEntry(rtKey_, RTConfig.MATCH_EXACT);
				}
			}
		}
	}

	protected void dataArriveAtDownPort(Object msg_, Port downPort_) 
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("RECEIVED: " + msg_);
		
		InetPacket ip_ = (InetPacket)msg_;
		Object p_ = (Object)ip_.getBody();
		int incomingIf_ = ip_.getIncomingIf();
		long source_ = ip_.getSource();
		
		if (p_ instanceof DVMRPRTPacket) {
			// route exchange packets
			DVMRPRTPacket dvp_ = (DVMRPRTPacket)p_;
			if (dvp_.getCommand() == DVMRPRTPacket.REQUEST) {
				RTEntry[] all_ = _retrieveAllRTEntries();
				if (all_ == null) return;
				_updateNeighbors(all_, incomingIf_, source_);
			}
			else {
				_routeUpdateHandler(dvp_, source_, incomingIf_);
			}
		}
		else {
			// DVMRPFCPacket
			DVMRPFCPacket dvp_ = (DVMRPFCPacket)p_;
			if (dvp_.isPrune()) _pruneHandler(dvp_, source_, incomingIf_);
			else if (dvp_.isGraft()) _graftHandler(dvp_, source_, incomingIf_);
			else _graftAckHandler(dvp_, source_, incomingIf_);
		}
	}
	
	// removes all the prune states that expires now
	// returns the bit set that reflects interfaces with prune state removed
	drcl.data.BitSet __removePruneStates(DVMRPFCExtension fcExt_, double now_)
	{	return fcExt_.removePruneStates(now_); }
	
	// get the minimum prune expiration time from the prune/graft states in 
	// the fcExt_ return Double.NaN if no prune state exists 
	double __getFCExtMinExpirationTime(DVMRPFCExtension fcExt_)
	{	return fcExt_.getMinExpirationTime(); }
	
	// get the minimum prune life time from the prune states in the fcExt_
	int __getMinPruneLifetime(DVMRPFCExtension fcExt_, double now_)
	{
		return fcExt_ == null? timing.pruneLifetime:
				fcExt_.getMinPruneLifetime(timing.pruneLifetime, now_);
	}
	
	// sets the timer for the fcEntry_ from the minimum prune expiration times
	void __setFCExtTimer(RTEntry fcEntry_, DVMRPFCExtension fcExt_, double now_)
	{
		double minExpire_ = __getFCExtMinExpirationTime(fcExt_);
		fcEntry_._setTimeout(minExpire_);
		if (!Double.isNaN(minExpire_))
			fcEntry_.handle = setTimeoutAt(fcEntry_, minExpire_);
	}
	
	// Sends a prune message upstream for the specified multicast group.
	// Must provide upstreamIf_ because DVMRP may use VIF to send the message.
	void __sendPruneMessage(long src_, long srcmask_, long group_, 
					int pruneLifetime_, long upstream_, int upstreamIf_)
	{
		DVMRPFCPacket prune_ = new DVMRPFCPacket(VERSION, src_, group_, 
						srcmask_, pruneLifetime_);
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_IO)
								|| isDebugEnabledAt(DEBUG_PRUNE)))
			debug("SEND PRUNE to " + upstream_ + " at if " + upstreamIf_ 
							+ ": " + prune_);
		// XX: src_ should be myself netaddress at the upstream_ interface
		// but prune handler does not verify the neighbor address...
		if (upstreamIf_ >= 0)
			forward(prune_, DVMRPFCPacket.PRUNE_SIZE, Address.NULL_ADDR, 
						upstream_, false, 1, InetPacket.CONTROL, upstreamIf_);
		else
			error(prune_, "__sendPruneMessage()", infoPort, 
							"invalid outgoing if: " + upstreamIf_);
	}
	
	// returns the time when the timer expires
	double __setPruneExpirationTimeOn(RTEntry fcEntry_, 
					DVMRPFCExtension fcExt_, int if_, int lifetime_)
	{
		double now_ = getTime();
		double expireTime_ = now_ + lifetime_;
		//double currentExpireTime_ = fcExt_.getPruneExpire(if_);
		
		// the following is not in the internet draft
		//if (!Double.isNaN(currentExpireTime_)
		//	&& currentExpireTime_ <= expireTime_)
		//	return currentExpireTime_;
		
		double min_ = __getFCExtMinExpirationTime(fcExt_);
		if (!(expireTime_ >= fcEntry_._getTimeout())) {
			if (fcEntry_.handle != null) cancelTimeout(fcEntry_.handle);
			fcEntry_._setTimeout(expireTime_);
			fcEntry_.handle = setTimeoutAt(fcEntry_, expireTime_);
		}
		fcExt_.setPruneExpire(if_, expireTime_);
		return expireTime_;
	}
	
	// retrieve the fc entry when incoming if is unknown and srcmask_ has 
	// less 1's
	RTEntry __findFCEntry(long src_, long srcmask_, long group_)
	{
		RTKey fcKey_ = new RTKey(src_, srcmask_, group_, -1, 0, 0);
		Object[] fcEntries_ = (Object[])retrieveRTEntry(fcKey_, 
						RTConfig.MATCH_WILDCARD);
		if (fcEntries_ == null) return null;
		for (int i=0; i<fcEntries_.length; i++) {
			RTEntry fcEntry_ = (RTEntry)fcEntries_[i];
			RTKey tmp_ = fcEntry_.getKey();
			//if ((src_ & tmp_.getSourceMask()) == tmp_.getMaskedSource()) 
			if (tmp_.getIncomingIf() >= 0)
				return fcEntry_;
		}
		return null;
	}
	
	// Sends a prune message upstream for the specified multicast group.
	// Upstream_ is downstream if is an ack.
	// Must provide upstreamIf_ because DVMRP may use VIF to send the message.
	void __sendGraftMessage(long src_, long srcmask_, long group_,
							long upstream_, int upstreamIf_, boolean ack_)
	{
		DVMRPFCPacket graft_ = new DVMRPFCPacket(VERSION, src_, group_, 
						srcmask_, ack_);

		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_IO)
								|| isDebugEnabledAt(DEBUG_GRAFT))) {
			if (ack_)
				debug("SEND GRAFT-ACK to " + upstream_ + " at if "
								+ upstreamIf_ + ": " + graft_);
			else
				debug("SEND GRAFT to " + upstream_ + " at if "
								+ upstreamIf_ + ": " + graft_);
		}
		// XX: src_ should be myself netaddress at the upstream_ interface
		// but the graft handler does not verify the neighbor address...
		if (upstreamIf_ >= 0)
			forward(graft_, DVMRPFCPacket.GRAFT_SIZE, Address.NULL_ADDR, 
						upstream_, false, 1, InetPacket.CONTROL, upstreamIf_);
		else
			error(graft_, "__sendGraftMessage()", infoPort, 
							"invalid outgoing if: " + upstreamIf_);
	}
	
	// returns the upstream interface (next hop if in the routing table) of the 
	// specific source network
	int __getUpstreamIf(long src_, long srcmask_)
	{
		RTKey rtKey_ = new RTKey(0, 0, src_, srcmask_, 0, 0);
		RTEntry rtEntry_ = (RTEntry)_retrieveRTEntry(rtKey_,
						RTConfig.MATCH_EXACT);
		if (rtEntry_ == null) return -1;
		int[] ifs_ = rtEntry_._getOutIfs();
		if (ifs_ == null || ifs_.length < 1) return -1;
		return ifs_[0];
	}
	
	// retrieves the "group" fc entry
	RTEntry __getGroupEntry(long group_)
	{
		return (RTEntry)retrieveRTEntry(__createGroupEntryKey(group_),
						RTConfig.MATCH_EXACT);
	}

	// create rt key for the "group" fc entry
	RTKey __createGroupEntryKey(long group_)
	{
		return new RTKey(0, 0, group_, -1, -1, -1);
	}
	
	void _pruneHandler(DVMRPFCPacket dvp_, long neighbor_, int incomingIf_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_PRUNE))
			debug("PRUNE from neighbor " + neighbor_ + " at if " + incomingIf_);
		long src_ = dvp_.getSource();
		long srcmask_ = dvp_.getSourceMask();
		long group_ = dvp_.getGroup();
		int pruneLifetime_ = dvp_.getPruneLifetime();
		
		RTEntry fcEntry_ = (RTEntry)__findFCEntry(src_, srcmask_, group_);
		if (fcEntry_ == null) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_PRUNE))
				debug("no fc entry found for pruning for " + src_ + "(" 
								+ srcmask_ + "), " + group_);
			return;
		}
		
		// actual prune operations:
		// 1. modify the forwarding cache entry and record the expiring 
		//    time in the DVMRPFCExtension
		// 2. set a timer for the prune state
		// 3. send a prune upstream if necessary (this is not required in 
		//    the internet draft)
		RTKey fcKey_ = fcEntry_.getKey();
		DVMRPFCExtension fcExt_ = (DVMRPFCExtension)fcEntry_.getExtension();
		
		if (fcExt_.getPruneExpire(incomingIf_) == 
						DVMRPFCExtension.NOT_DEPENDENT) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_PRUNE))
				debug("if " + incomingIf_ + " is not a dependent of entry: " 
								+ fcEntry_);
			return;
		}
		else if (fcExt_.getPruneExpire(incomingIf_) !=
						DVMRPFCExtension.GRAFTED) {
			__setPruneExpirationTimeOn(fcEntry_, fcExt_, incomingIf_, 
							pruneLifetime_);
			return;
		}
		
		// check the "group" entry to see if there are active group
		// members on the incomingIf_. if so, stop here.
		RTEntry fcGroupEntry_ = __getGroupEntry(group_);
		if (fcGroupEntry_ != null) {
			if (fcGroupEntry_.getOutIf().get(incomingIf_)) return;
		}
		
		// XXX: the internet draft says to prune all the fc entries of the 
		// same mcast group having the same downstream interface
		fcExt_ = (DVMRPFCExtension)fcExt_.clone();
		__setPruneExpirationTimeOn(fcEntry_, fcExt_, incomingIf_, 
						pruneLifetime_);
		pruneRTEntry(fcKey_, incomingIf_, fcExt_, RTConfig.TIMEOUT_NO_CHANGE);
		
		boolean multihomed_ = IDLookup.query(group_, idport);
		if (!multihomed_ && fcKey_.getIncomingIfMask() != 0 
			&& fcEntry_.getOutIf().getNumSetBits() == 0) {
			// send a prune upstream
			double now_ = getTime();
			__sendPruneMessage(src_, srcmask_, group_, 
							__getMinPruneLifetime(fcExt_, now_),
							fcExt_.upstream, fcKey_.getIncomingIf());
			fcExt_.sentPruneUpstream = true;
		}
	}
	
	void _graftHandler(DVMRPFCPacket dvp_, long neighbor_, int incomingIf_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_GRAFT))
			debug("GRAFT from neighbor " + neighbor_ + " at if " + incomingIf_);
		
		long src_ = dvp_.getSource();
		long srcmask_ = dvp_.getSourceMask();
		long group_ = dvp_.getGroup();
		//int upstreamIf_ = __getUpstreamIf(src_, srcmask_);

		// the internet draft says that all the forwarding cache entries of
		// the same group having the same downstream dependent interface should
		// graft back the interface.
		// retrieve fc entry
		//RTKey fcKey_ = new RTKey(src_, srcmask_, group_, -1, upstreamIf_, -1);
		RTKey fcKey_ = new RTKey(0, 0, group_, -1, 0, 0);
		Object[] fcEntries_ = (Object[])retrieveRTEntry(fcKey_, 
						RTConfig.MATCH_WILDCARD);
		if (fcEntries_ == null || fcEntries_.length == 0) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_GRAFT))
				debug("GRAFT: no matched fc entry is found for group " +group_);
			return;
		}
		
		// XX: could aggregate grafts sent upstreams, but it is not required
		_graft(fcEntries_, incomingIf_, true/*check dependent*/);
		
		// ack
		__sendGraftMessage(src_, srcmask_, group_, neighbor_,
						incomingIf_, true/*ack*/);
	}
	
	void _graftAckHandler(DVMRPFCPacket dvp_, long neighbor_, int incomingIf_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_GRAFT))
			debug("GRAFT-ACK from neighbor " + neighbor_ + " at if " 
							+ incomingIf_);
		long src_ = dvp_.getSource();
		long srcmask_ = dvp_.getSourceMask();
		long group_ = dvp_.getGroup();
		int upstreamIf_ = __getUpstreamIf(src_, srcmask_);

		// retrieve fc entry
		RTKey fcKey_ = new RTKey(src_, srcmask_, group_, -1, upstreamIf_, -1);
		RTEntry fcEntry_ = (RTEntry)retrieveRTEntry(fcKey_, 
						RTConfig.MATCH_EXACT);
		if (fcEntry_ == null) return;
		DVMRPFCExtension fcExt_ = (DVMRPFCExtension)fcEntry_.getExtension();
		if (fcExt_ == null) return;
		fcExt_.clearPendingGraft();
	}
	
	// Implements {@link drcl.inet.Protocol#mcastQueryHandler}.
	protected int[] routeQueryHandler(RTKey request_, Port inPort_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_MCAST_QUERY))
			debug("MCAST QUERY for " + request_);
		
		RTEntry fcEntry_ = (RTEntry)retrieveRTEntry(request_, 
						RTConfig.MATCH_LONGEST);
		// send a prune if fc entry (not a group entry) already exists and
		// not multihomed,
		// XX: could consider PRUNE_RETX_TIMER here to yield prune
		if (fcEntry_ != null && fcEntry_.getKey().getSourceMask() != 0) {
			long group_ = request_.getDestination();
			if (request_.getIncomingIfMask() == 0) return null; // source itself
			if (IDLookup.query(group_, idport)) return null; // multihomed
			
			RTKey fcKey_ = fcEntry_.getKey();
			DVMRPFCExtension fcExt_ = (DVMRPFCExtension)fcEntry_.getExtension();
			__sendPruneMessage(fcKey_.getSource(), fcKey_.getSourceMask(),
							   group_,
							   __getMinPruneLifetime(fcExt_, getTime()),
							   fcExt_.upstream,
							   request_.getIncomingIf());
			fcExt_.sentPruneUpstream = true;
			return null;
		}
		
		// Reverse path forwarding check: data packet must arrive on the
		// anticipated upstream interface (DVMRP v3 spec)
		// There may be multiple rt entries, each for a different hierarchy,
		// for the specific source.  Need to retrieve them all and form the 
		// forwarding cache from their downstream dependents (must be disjoint).
		RTKey rtkey_ = new RTKey(-1/*dont care*/, request_.getSource(), 
						-1/*dont care*/);
		Object[] rtEntries_ = (Object[])_retrieveRTEntry(rtkey_, 
						RTConfig.MATCH_ALL);
		if (rtEntries_ == null || rtEntries_.length == 0) {
			error("mcastQueryHandler()", "no unicast route back to source: "
							+ request_.getSource());
			return null;
		}
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_MCAST_QUERY))
			debug("MCAST QUERY: matched rt entries: "
							+ drcl.util.StringUtil.toString(rtEntries_));

		long src_ = request_.getSource();
		long group_ = request_.getDestination();
		long srcmask_ = Long.MIN_VALUE;
			// get the "least-covered" or largest mask
		long upstream_ = Address.NULL_ADDR; // from rtEntry_.getNextHop()
		int upstreamIf_ = -1;
		drcl.data.BitSet dependentIfs_ = null;
		
		for (int i=0; i<rtEntries_.length; i++) {
			RTEntry rtEntry_ = (RTEntry)rtEntries_[i];
			
			// update srcmask_
			long srcmask__ = rtEntry_.getKey().getDestinationMask();
			if (srcmask__ > srcmask_) srcmask_ = srcmask__;

			// update dependentIfs_
			DVMRPRTExtension rtExt_ = (DVMRPRTExtension)rtEntry_.getExtension();
			if (dependentIfs_ == null)
				dependentIfs_ = (drcl.data.BitSet)rtExt_.dependentIfs.clone();
			else if (rtExt_.dependentIfs != null) {
				dependentIfs_.or(rtExt_.dependentIfs);
					// XX: no check, must be disjoint
			}
			
			// update upstream_
			if (upstream_ == Address.NULL_ADDR
				&& rtEntry_.getNextHop() != Address.NULL_ADDR) {
				upstream_ = rtEntry_.getNextHop();
				upstreamIf_ = rtEntry_._getOutIfs()[0];
					// should be key_.getIncomingIf()
				if (upstreamIf_ != request_.getIncomingIf()) {
					// transitional route instability?
					// just ignore it
					return null;
				}
			}
		}
		
		boolean multihomed_ = IDLookup.query(group_, idport);
		
		// Add all the dependent interfaces to the forwarding cache.
		DVMRPFCExtension fcExt_ = new DVMRPFCExtension(upstream_, 
						dependentIfs_);
		
		// also need to consider the "group" entry for forming the outgoing 
		// ifs of the fc entry
		drcl.data.BitSet outgoingIfs_ = (drcl.data.BitSet)dependentIfs_.clone();
		RTEntry fcGroupEntry_ = __getGroupEntry(group_);
		if (fcGroupEntry_ != null) {
			if (outgoingIfs_ == null)
				outgoingIfs_ = (drcl.data.BitSet)
						fcGroupEntry_.getOutIf().clone();
			else
				outgoingIfs_.or(fcGroupEntry_.getOutIf());
		}
			
		// establish the forwarding cache entry or send a prune
		if (!multihomed_ && (outgoingIfs_ == null
								|| outgoingIfs_.getNumSetBits() == 0)) {
			// Leaf node, send back prune if this node is not a group member.
			if (upstream_ != Address.NULL_ADDR) {
				// sends a prune upstream
				// XXX: should install a prune retrx timer to allow this prune 
				// message to take effect at upstream (the v3 internet draft),
				// but it is not urgent to have this feature coded.
				__sendPruneMessage(src_, srcmask_, request_.getDestination(),
								timing.pruneLifetime, upstream_, upstreamIf_);
				fcExt_.sentPruneUpstream = true;
			}
		}
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_MCAST_QUERY))
			debug("MCAST QUERY: add fc entry: src=" + src_ + ", srcmask="
				+ srcmask_ + ", upstream=" + upstream_ + "(if="
				+ upstreamIf_ + "), dependentIfs=" + dependentIfs_
				+ ", outgoingIfs=" + outgoingIfs_);
		
		// add an fc entry no matter a prune is sent or not, so that later, 
		// the router knows whether to send a graft when a host joins the group
		// XX: no timeout for this entry, the draft does not discuss this, 
		// 	maybe it is set in the kernel? or it is really a cache?
		// XX: if upstreamIf_ < 0, then it is the "host" router, dont care the 
		// incoming if for now but sure can do better than this...
		if (upstreamIf_ >= 0)
			addRTEntry(new RTKey(src_, srcmask_, request_.getDestination(),
									-1, upstreamIf_, -1),
					   outgoingIfs_, fcExt_, Double.NaN);
		else
			addRTEntry(new RTKey(src_, srcmask_, request_.getDestination(),
									-1, 0, 0),
					   outgoingIfs_, fcExt_, Double.NaN);
		
		if (outgoingIfs_ == null || outgoingIfs_.getNumSetBits() == 0)
			return null;
		else
			return outgoingIfs_.getSetBitIndices();
	}
	
	static final DVMRPRTExtension INFINITE_DVE = new DVMRPRTExtension(INFINITY);
	
	// when upstream (or next hop to a destination) is changed
	// or the extension is initialized
	void __initDependents(long src_, long srcmask_, DVMRPRTExtension rtExt_)
	{
		drcl.data.BitSet tmp_ = null;
		// reset dependent ifs
		if (ifset == null || ifset.getNumSetBits() == 0) {
			tmp_ = new drcl.data.BitSet(IFQuery.getNumOfInterfaces(ifport));
		}
		else {
			tmp_ = (drcl.data.BitSet)ifset.clone();
			tmp_.clear();
		}
		rtExt_.resetDependents(tmp_);
	}
	
	// synchronize fc entries with the rt entry on upstream and downstreams
	// could result in sending graft upstream if there is an addition of
	// a downstream dependent and a prune has been sent upstream.
	// Change of upstream does not result in sending a graft upstream.
	void __syncFCWithRT(RTEntry rtEntry_)
	{
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_DEPENDENT)
								|| isDebugEnabledAt(DEBUG_SYNC_FC)))
			debug("SYNC fc entries with the rt entry: " + rtEntry_);
		double now_ = getTime();
		RTKey rtKey_ = rtEntry_.getKey();
		long src_ = rtKey_.getDestination();
		long srcmask_ = rtKey_.getDestinationMask();
		long nexthop_ = rtEntry_.getNextHop();
		DVMRPRTExtension rtExt_ = (DVMRPRTExtension)rtEntry_.getExtension();
		drcl.data.BitSet dependentIfs_ = rtExt_.dependentIfs;
		
		// retrieve all related fc entries
		RTKey fcKey_ = new RTKey(src_, srcmask_, 0, 0, 0, 0);
		Object[] fcEntries_ = (Object[])retrieveRTEntry(fcKey_, 
						RTConfig.MATCH_WILDCARD);
		if (fcEntries_ == null) return;
		for (int i=0; i<fcEntries_.length; i++) {
			RTEntry fcEntry_ = (RTEntry)fcEntries_[i];
			fcKey_ = fcEntry_.getKey();
			if (fcKey_.getSourceMask() == 0) continue; // the "group" entry
			if (!drcl.inet.InetConfig.Addr.isMcast(fcKey_.getDestination()))
				continue;
			
			Object tmp_ = fcEntry_.getExtension();
			if (!(tmp_ instanceof DVMRPFCExtension)) continue;
			
			DVMRPFCExtension fcExt_ =
					(DVMRPFCExtension)((DVMRPFCExtension)tmp_).clone();
			drcl.data.BitSet bsOutifs_ =
					(drcl.data.BitSet)fcEntry_.getOutIf().clone();
			boolean hasGrafted_ = false;
			boolean anychange_ = false;
			boolean upstreamchange_ = false;
			for (int j=0; j<dependentIfs_.getSize(); j++) {
				boolean prevState_ = 
					fcExt_.getPruneExpire(j) != DVMRPFCExtension.NOT_DEPENDENT;
				boolean currentState_ = dependentIfs_.get(j);
				if (prevState_ && !currentState_) {
					fcExt_.setPruneExpire(j, DVMRPFCExtension.NOT_DEPENDENT);
					bsOutifs_.clear(j);
					anychange_ = true;
				}
				else if (!prevState_ && currentState_) {
					fcExt_.setPruneExpire(j, DVMRPFCExtension.GRAFTED);
					bsOutifs_.set(j);
					anychange_ = true;
				}
				if (fcExt_.getPruneExpire(j) == DVMRPFCExtension.GRAFTED)
					hasGrafted_ = true;
			}

			// do grafting if upstream does not change && sent prune before 
			// but not graft && now has grafted downstream
			// do it at the end after all changes are made effective
			boolean doGraft_ = nexthop_ == fcExt_.upstream
					&& fcExt_.sentPruneUpstream
				&& hasGrafted_ && !fcExt_.hasPendingGraft();
			
			if (!hasGrafted_ && fcExt_.hasPendingGraft()) {
				fcExt_.clearPendingGraft();
				anychange_ = true;
			}
			else if (hasGrafted_ && fcExt_.sentPruneUpstream) {
				fcExt_.sentPruneUpstream = false;
				anychange_ = true;
			}
			
			// Note: while downstreams in an fc entry are possibly collected 
			// from multiple rt entries (in a boundary router in a 
			// hierarchical network), the upstream is from the only
			// "corresponding" (same src and srcmask?) rt entry.
			if (nexthop_ != fcExt_.upstream && src_ == fcKey_.getSource()
				&& srcmask_ == fcKey_.getSourceMask()) {
				fcExt_.upstream = nexthop_;
				upstreamchange_ = true;
				anychange_ = true;
			}
			
			if (anychange_) {
				if (upstreamchange_) {
					removeRTEntry(fcKey_, RTConfig.MATCH_EXACT);
					int upstreamIf_ = rtEntry_._getOutIfs()[0];
					fcKey_.setIncomingIf(upstreamIf_);
					addRTEntry(fcKey_, bsOutifs_, fcExt_, Double.NaN);
				}
				else 
					addRTEntry(fcKey_, bsOutifs_, fcExt_, 
									RTConfig.TIMEOUT_NO_CHANGE);
			}
			
			if (doGraft_) {
				if (anychange_)
					fcEntry_ = (RTEntry)retrieveRTEntry(fcKey_, 
									RTConfig.MATCH_EXACT);
				__sendGraftOn(now_, fcEntry_, true/*adjust timeout*/);
			}
		}
	}
	
	/** Updates the internal routing table.	 */
	private void _routeUpdateHandler(DVMRPRTPacket dvp_, long neighbor_, 
					int incomingIf_) 
	{
		DVMRPRTPacket.RTE[] ss_ = dvp_.getRTEs();
		boolean anychange_ = false;
		double now_ = getTime();
		for (int i=0; i<ss_.length; i++) {
			DVMRPRTPacket.RTE rte_ = ss_[i];
			long dest_ = rte_.getDestination();
			long destmask_ = rte_.getMask();
			long nexthop_ = rte_.getNextHop();
			int metric_ = rte_.getMetric();
			RTKey key_ = new RTKey(0, 0, dest_, destmask_, 0, 0);
			RTEntry rtEntry_ = (RTEntry)_retrieveRTEntry(key_, 
							RTConfig.MATCH_EXACT);
			
			if (rtEntry_ == null) {
				if (metric_ >= (INFINITY << 1)) continue; // invalid metric
			}
			
			DVMRPRTExtension rtExt_ = null;
			long oldNexthop_ = Address.NULL_ADDR;
			
			if (rtEntry_ != null) {
				rtExt_ = (DVMRPRTExtension)rtEntry_.getExtension();
				oldNexthop_ = rtEntry_.getNextHop();
			}
			
			boolean anylinkchange_ = false;
				// if true then need to sync fc entries with the rt entry
			
			if (rtEntry_ == null && metric_ < INFINITY
				|| rtEntry_ != null && oldNexthop_ == neighbor_
				&& (metric_ == INFINITY? INFINITY: metric_ + 1) != rtExt_.metric
				|| rtExt_ != null && metric_ + 1 < rtExt_.metric) {
				anychange_ = true;
				/*
 *				if (no matched entry)
 *					create and reset a dependents list for this new entry
 *				else 
 *					clone the rtExt and adopt the new metric
 *					if (next hop is not the neighbor)
 *						if the neighbor was dependent, 
 *							cancel the dependency, 
 *				if (metric in RTE < INF)
 *					add/modify the rt entry to adopt new metric and/or next hop
 *					reset the route timer for the entry
 *					if (next hop is not the neighbor), then it is "change of 
 *						upstream" and possibly, downstream:
 *						sync fc entries with the rt entry (__syncFCWithRT())
 *				else
 *					the route is invalidated by the [next-hop] neighbor,
 *					the entry enters hold-down period
				*/
				if (rtEntry_ == null) {
					rtExt_ = new DVMRPRTExtension(metric_ + 1, null, null,true);
					__initDependents(dest_, destmask_, rtExt_);
				}
				else {
					rtExt_ = (DVMRPRTExtension)rtExt_.clone();
					rtExt_.changed = true;
					rtExt_.metric = metric_ == INFINITY? metric_: metric_ + 1;
					if (oldNexthop_ != neighbor_) {
						int ifindex_ = rtExt_.getDependentIndex(neighbor_);
						if (ifindex_ >= 0) {
							rtExt_.setDependent(ifindex_, Address.NULL_ADDR);
							if (isDebugEnabled()
								&& isDebugEnabledAt(DEBUG_DEPENDENT))
								debug("REMOVE DEPENDENT " + neighbor_
									+ " at if " + ifindex_ + " for source "
									+ rtEntry_.getKey().getDestination());
						}
						anylinkchange_ = true;
					}
				}
				
				if (metric_ < INFINITY) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE)) {
						if (rtEntry_ == null)
							debug("Initial route from neighbor: " + neighbor_
								+ "-" + key_ + "   " + neighbor_ + "("
								+ incomingIf_ + ")   " + rtExt_);
						else if (oldNexthop_ == neighbor_)
							debug("Route update from NEXT-HOP neighbor: "
								+ rtEntry_ + "\n\t\t--->" + rtExt_);
						else debug("Better route from NON-NEXT-HOP neighbor: "
								+ rtEntry_ + "\n\t\t--->" + neighbor_ + "-" 
								+ rtExt_);
					}
					
					RTEntry tmpentry_ = _addRTEntry(key_, neighbor_, 
									incomingIf_, rtExt_,
									timing.ROUTE_TIMEOUT_PERIOD);
					//if (rtEntry_ != null && rtEntry_.handle != null)
					//	cancelTimeout(rtEntry_.handle);
					//tmpentry_.handle = setTimeout(tmpentry_,
					//	timing.ROUTE_TIMEOUT_PERIOD);
					if (rtEntry_ == null)
						setTimeout(tmpentry_, timing.ROUTE_TIMEOUT_PERIOD);
				}
				else {
					// invalidate the entry and turn on the delete timer
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
						debug("hold-down " + rtEntry_
							+ " due to route invalidation from NEXT-HOP "
							+ "neighbor\n\t\t--->" + rtExt_);
					RTEntry tmpentry_ = _addRTEntry(key_, neighbor_,
							incomingIf_, rtExt_, timing.deleteTimeoutPeriod);
				}
			}
			else if (rtEntry_ != null && oldNexthop_ == neighbor_) {
				// reporting same metric, refresh the entry
				//if (rtEntry_.handle != null) cancelTimeout(rtEntry_.handle);
				//rtEntry_.handle = setTimeout(rtEntry_, 
				//	timing.ROUTE_TIMEOUT_PERIOD); // refresh the entry
				rtEntry_._setTimeout(now_ + timing.ROUTE_TIMEOUT_PERIOD);
					// refresh the entry
			}
			
			// if old entry's next hop != neighbor_, then need to check/update 
			// dependents
			if (rtEntry_ != null && oldNexthop_ != neighbor_) {
				// cases where rtEntry_.getNextHop() != neighbor_ 
				// 	&& metric_ + 1 >= rtExt_.metric
				int ifindex_ = rtExt_.getDependentIndex(neighbor_);
				if (metric_ > INFINITY) {
					// poison reverse
					// should be true that nexthop_ == myself_
					if (ifindex_ != incomingIf_) {
						if (isDebugEnabled()
							&& isDebugEnabledAt(DEBUG_DEPENDENT))
							debug("ADD DEPENDENT " + neighbor_ + " at if "
								+ incomingIf_ + " for source " 
								+ rtEntry_.getKey().getDestination());
						rtExt_.setDependent(incomingIf_, neighbor_);
						if (ifindex_ >= 0) {
							if (isDebugEnabled()
								&& isDebugEnabledAt(DEBUG_DEPENDENT))
								debug("REMOVE DEPENDENT " + neighbor_ 
									+ " at if " + incomingIf_ + " for source "
									+ rtEntry_.getKey().getDestination());
							rtExt_.setDependent(ifindex_, Address.NULL_ADDR);
						}
						anylinkchange_ = true;
					}
				}
				else if (ifindex_ >= 0) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DEPENDENT))
						debug("REMOVE DEPENDENT " + neighbor_ + " at if "
							+ incomingIf_ + " for source "
							+ rtEntry_.getKey().getDestination());
					rtExt_.setDependent(ifindex_, Address.NULL_ADDR);
					anylinkchange_ = true;
					
				}
				
				if (anylinkchange_) __syncFCWithRT(rtEntry_);
			}
		}
		if (anychange_)
			_activateTriggeredUpdateTimer();
	}
	
	/** Sets the set of the interfaces that this protocol is operated on. */
	public void setIfset(drcl.data.BitSet ifset_)
	{ ifset = ifset_; }
	
	/** Returns the set of the interfaces that this protocol is operated on. */
	public drcl.data.BitSet getIfset()
	{ return ifset; }
	
	/**
	 * Sets the metrics on the interfaces that this protocol is operated on.
	 * The indices to the metrics array are the indices to the interfaces.
	 * The values in the array that correspond to the interfaces that this 
	 * protocol is not operated on are ignored.
	 * 
	 * @see #setIfset(drcl.data.BitSet)
	 */
	public void setMetrics(int[] metrics_)
	{	metrics = metrics_;	}
	
	/**
	 * Returns the metrics on the interfaces that this protocol is operated on.
	 * The indices to the returned metrics array are the indices to the 
	 * interfaces.  The values in the array that correspond to the interfaces 
	 * that this protocol is not operated on are arbitrary.
	 * 
	 * @return null if all the interfaces have default metric of zero.
	 * 
	 * @see #setIfset(drcl.data.BitSet)
	 */
	public int[] getMetrics()
	{	return metrics;	}
	
	/** Retrieve all the RT entries. */
	RTEntry[] _retrieveAllRTEntries()
	{
		Object[] oo_ = (Object[])routingTable.get(new RTKey(0,0, 0, 0, 0, 0), 
						RTConfig.MATCH_WILDCARD);
		RTEntry[] ee_ = new RTEntry[oo_.length];
		System.arraycopy(oo_, 0, ee_, 0, oo_.length);
		return ee_;
	}
	
	/** 
	 * @param type exact match, longest match, or match all.
	 * @return the routing table entry requested.
	 */
	Object _retrieveRTEntry(RTKey key_, String type)
	{
		return routingTable.get(key_, type);
	}
	
	/** 
	 * @param type exact match, longest match, or match all.
	 * @return the routing table entry requested.
	 */
	Object _removeRTEntry(RTKey key_, String type)
	{
		return routingTable.remove(key_, type);
	}
	
	/** Add/replace an entry to the routing table. */
	RTEntry _addRTEntry(RTKey key_, RTEntry entry_, double timeout_)
	{
		if (entry_.getKey() == null) entry_.setKey(key_);
		RTEntry old_ = (RTEntry)routingTable.get(key_, RTConfig.MATCH_EXACT);
		if (old_ == null) {
			routingTable.addEntry(key_, entry_);
		}
		else {
			// replace
			old_.duplicate(entry_);
			entry_ = old_;
		}
		
		//if (old_ != null && old_.handle != null) cancelTimeout(old_.handle);
		if (Double.isNaN(timeout_) || timeout_ <= 0.0)
			entry_._setTimeout(Double.NaN);
		else
			entry_._setTimeout(getTime() + timeout_);
		return entry_;
	}
	
	/** Add/replace an entry to the routing table.  */
	RTEntry _addRTEntry(RTKey key_, long nexthop_, int interface_, 
					Object entryExt_, double timeout)
	{	
		drcl.data.BitSet ifset_ = interface_ >= 0?
								  new drcl.data.BitSet(new int[]{interface_}):
								  null;
		RTEntry entry_ = new RTEntry(nexthop_, ifset_, entryExt_);
		entry_ = _addRTEntry(key_, entry_, timeout);
		return entry_;
	}
	
	public void setTimingPack(DVMRPTimingPack timing_)
	{ if (timing_ != null) timing = timing_; }
	
	public DVMRPTimingPack getTimingPack()
	{ return timing == DEFAULT_TIMING? null: timing; }
	
	public String timingInfo()
	{
		return timing.info();
	}
	
	public void setPruneLifetime(int pruneLifetime_)
	{
		timing.setPruneLifetime(pruneLifetime_);
	}
	
	// adjustTimeout_: true to adjust the fc entry timeout if necessary
	//			set to false if called by the fc entry timeout handler
	void __sendGraftOn(double now_, RTEntry fcEntry_, boolean adjustTimeout_)
	{
		// set graft ack timeout, record this timeout on the fcExt_
		// send graft message
		RTKey fcKey_ = fcEntry_.getKey();
		DVMRPFCExtension fcExt_ = (DVMRPFCExtension)fcEntry_.getExtension();
		fcExt_.graftAckExpBackOff(now_, timing.graftRetxTime);
		if (adjustTimeout_
			&& (!(fcEntry_._getTimeout() <= fcExt_.graftAckTimeout))) {
			if (fcEntry_.handle != null) cancelTimeout(fcEntry_.handle);
			fcEntry_._setTimeout(fcExt_.graftAckTimeout);
			fcEntry_.handle = setTimeoutAt(fcEntry_, fcExt_.graftAckTimeout);
		}
		
		long src_ = fcKey_.getSource();
		long srcmask_ = fcKey_.getSourceMask();
		int upstreamIf_ = __getUpstreamIf(src_, srcmask_);
		__sendGraftMessage(src_, srcmask_, fcKey_.getDestination(),
						   fcExt_.upstream, upstreamIf_, false/*not an ack*/);
	}
	
	// send graft upstream on the fc entries, and graft at the specified 
	// interface checkDependent_: set true to check if ifindex_ is one of
	// the dependent if
	void _graft(Object[] fcEntries_, int ifindex_, boolean checkDependent_)
	{
		double now_ = getTime();
		for (int i=0; i<fcEntries_.length; i++) {
			RTEntry fcEntry_ = (RTEntry)fcEntries_[i];
			if (fcEntry_ == null) continue;
			RTKey fcKey_ = fcEntry_.getKey();
			if (fcKey_.getIncomingIf() < 0) continue; // the group entry
			
			DVMRPFCExtension fcExt_ = (DVMRPFCExtension)fcEntry_.getExtension();
			if (fcExt_ == null) continue;
			if (ifindex_ >= 0 && fcEntry_.getOutIf().get(ifindex_))
				// the interface is already grafted
				continue;
			else if (checkDependent_ && ifindex_ >= 0
				&& fcExt_.getPruneExpire(ifindex_) ==
				DVMRPFCExtension.NOT_DEPENDENT) {
				// not a dependent, dont graft it
				continue;
			}
		
			// graft on the interface and remove the prune state
			if (ifindex_ >= 0) {
				if (checkDependent_) {
					fcExt_ = (DVMRPFCExtension)fcExt_.clone();
					fcExt_.setPruneExpire(ifindex_, DVMRPFCExtension.GRAFTED);
					graftRTEntry(fcKey_, ifindex_, fcExt_, 
									RTConfig.TIMEOUT_NO_CHANGE);
				}
				else
					graftRTEntry(fcKey_, ifindex_, 
						RTConfig.EXTENSION_NO_CHANGE, 
						RTConfig.TIMEOUT_NO_CHANGE);
			}
			
			// send graft upstream and set up graft retx timer
			if (fcExt_.sentPruneUpstream && !fcExt_.hasPendingGraft())
				__sendGraftOn(now_, fcEntry_, true);
		}
	}
	
	// prune the fc entries at the interface to a host network
	// be careful if the interface is also a downstream dependent interface
	void _prune(Object[] fcEntries_, int ifindex_)
	{
		if (ifindex_ < 0) return;
		double now_ = getTime();
		for (int i=0; i<fcEntries_.length; i++) {
			RTEntry fcEntry_ = (RTEntry)fcEntries_[i];
			if (fcEntry_ == null) continue;
			RTKey fcKey_ = fcEntry_.getKey();
			
			// prune the interface if not a downstream dependent
			Object ext_ = fcEntry_.getExtension();
			if (ext_ == null
				|| !(ext_ instanceof DVMRPFCExtension)
				|| ((DVMRPFCExtension)ext_).getPruneExpire(ifindex_) ==
							DVMRPFCExtension.NOT_DEPENDENT)
				pruneRTEntry(fcKey_, ifindex_, RTConfig.EXTENSION_NO_CHANGE, 
								RTConfig.TIMEOUT_NO_CHANGE);
		}
	}
	
	/*
	 * Implements {@link drcl.inet.Protocol#mcastHostJoinEventHandler}.
	 * @param ifindex_ could be -1 if it is a local join (multihomed router).
	 */
	protected void mcastHostJoinEventHandler(long src_, long srcmask_, 
					long group_, int ifindex_, Port inPort_)
	{
		super.mcastHostJoinEventHandler(src_, srcmask_, group_, ifindex_, 
						inPort_);
		if (src_ != 0 || srcmask_ != 0) {
			if (isDebugEnabled())
				debug("MCAST JOIN: don't know how to handle specific source "
								+ "join, treat it as a general group join");
		}

		// retrieve the "group" entry
		RTKey fcKey_ = __createGroupEntryKey(group_);
		RTEntry fcGroupEntry_ = __getGroupEntry(group_);

		if (ifindex_ >= 0) {
			if (fcGroupEntry_ == null) {
				// create the "group" entry
				addRTEntry(fcKey_, ifindex_, "-GroupEntry-", Double.NaN);
				//fcGroupEntry_ = (RTEntry)retrieveRTEntry(fcKey_,
				//	RTConfig.MATCH_EXACT);
			}
			else
				// graft for the "group" entry
				graftRTEntry(fcKey_, ifindex_, RTConfig.EXTENSION_NO_CHANGE,
								RTConfig.TIMEOUT_NO_CHANGE);
		}
		
		Object[] fcEntries_ = (Object[])retrieveRTEntry(
						new RTKey(0, 0, group_, -1, 0, 0),
						RTConfig.MATCH_WILDCARD);
		// make the forwarding cache entries graft on the interface 
		// and send graft upstream if sent prune before.
		_graft(fcEntries_, ifindex_, false);
	}
	
	/*
	 * Implements {@link drcl.inet.Protocol#mcastHostLeaveEventHandler}.
	 * @param ifindex_ could be -1 if it is a local leave (multihomed router).
	 */
	protected void mcastHostLeaveEventHandler(long src_, long srcmask_, 
					long group_, int ifindex_, Port inPort_)
	{
		super.mcastHostLeaveEventHandler(src_, srcmask_, group_, ifindex_, 
						inPort_);
		if (src_ != 0 || srcmask_ != 0) {
			if (isDebugEnabled())
				debug("MCAST LEAVE: don't know how to handle specific source "
								+ "leave, treat it as a general group leave");
		}
		if (ifindex_ < 0) return;
		Object[] fcEntries_ = (Object[])retrieveRTEntry(
						new RTKey(0, 0, group_, -1, 0, 0),
						RTConfig.MATCH_WILDCARD);
		_prune(fcEntries_, ifindex_);
	}
}
