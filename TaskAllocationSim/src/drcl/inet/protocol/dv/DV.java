// @(#)DV.java   2/2004
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

import java.util.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.InetPacket;
import drcl.inet.data.*;
import drcl.inet.contract.*;
import drcl.util.scalar.IntVector;

// XXX: does not have routing entry timeout (what if an entry is not updated by
// 		any neighbor)

// EVENT HANDELING: (v) done; (.) in progress.
// (v) NEIGHBOR UP event: 
//		send a request to the new neighbor
//		set up a regular update timer if not yet done so
// (v) Receiving an update from a neighbor: (dataArriveAtDownPort())
//		set up or adjust the neighbor timer
//		compare each RTE in the message
//		for each change in RT
//			update the entry and disable the delete timer if there's one
//			mark the entry as "changed"
//		if there is any change in RT && no triggered update timer is set
//			set up a triggered update timeout
// (v) Regular update timeout: (timeout())
//		send all RTEs, including an entry of myself (with poison reverse) to
//		all neighbors
// (v) Neighbor timeout: (timeout())
//      for each RT entry with next hop to the down neighbor
//			mark RT metric infinity
//			set up delete timer
//		if there is any change in RT && no triggered update timer is set
//			set up a triggered update timeout
// (v) Delete timeout: (take advantage of the removal service in the RTConfig
//     contract)
//		delete the RT entry from CSL
// (v) Triggered update timeout: (timeout())
//		Go over the RT to see get changed RT entries
//		for each changed RT entry, mark "unchanged"
//		if any change
//			send out an update to all neighbors (consider poison reverse and
//			if mask)
// (v) Receiving a request message from a neighbor: (dataArriveAtDownPort())
//		same as a regular update timeout handling, except the update is only
//		sent to the requested neighbor
// (v) UcastQuery event: (for the case if neighbor is a host with no DV and
//     Hello running)
//		return null

/**
Implements a routing information protocol (RIP, RFC1058, RFC2453).
Split-horizon with poison reverse, triggered updates and "holddown" state
for deleted routes are implemented.

<p>The protocol takes advantages of the <code>NEIGHBOR_UP</code> event from the {@link drcl.inet.CoreServiceLayer
core service layer} to activate the protocol at the beginning.
When a new neighbor is discovered, the protocol sends a request message at the
interface, starts a triggered update at the other interfaces; and sets up the
regular update timer if necessary.

<p> The component can run either on physical interfaces or tunnel (virtual)
interfaces ({@link #setMode(String)}.
By default, it runs on physical interfaces.
The tunnel interfaces are configured in the core service layer.

<p>One may configure this component to operate on a specific set of interfaces
({@link #setIfset(drcl.data.BitSet)}).

@author Bin Wang, Hung-ying Tyan
@version 1.2, 10/22/2003
  */
public class DV extends drcl.inet.protocol.Routing implements drcl.inet.protocol.UnicastRouting
{	
	public String getName()
	{ return "dv"; }
	
	Port idport = createIDServicePort(); // for ID lookup service
	Port ifport = createIFQueryPort();  // for Interface query service
	
	{
		createIFEventPort(); // listening to neighbor events
		createVIFEventPort(); // to receive virtual neighbor events; for overlay
	}
	
	// RFC2453
	final static int VERSION = 2;

	/**
	 * Regular update timeout period.
	 * Recommended in RFC2453.
	 */
	public final static double REGULAR_UPDATE_TIMEOUT_PERIOD = 30.0;

	/**
	 * Timeout variance for updating the distance vector information.
	 * Recommended in RFC2453.
	 */
	public final static double MAX_TIME_OUT_VARIANCE = 5.0;

	/**
	 * Routing table entry timeout period.
	 * Recommended in RFC2453.
	 */
	public final static double NEIGHBOR_TIMEOUT_PERIOD = 180.0;
	
	/**
	 * Hold-down timeout period for entries to be removed.
	 * Recommended in RFC2453.
	 */
	public final static double DELETE_TIMEOUT_PERIOD = 120.0;

	/**
	 * Triggered update timeout period.
	 * This is set up when a triggered update is needed.
	 * The reason for this delay is to prevent excessive traffic from
	 * message exchanges during a transitional period.
	 */
	public final static double TRIGGERED_UPDATE_TIMEOUT_PERIOD =
			MAX_TIME_OUT_VARIANCE;
	
	final static String TRIGGERED_UPDATE_TIMER = "Triggered Update Timeout";
	final static String REGULAR_UPDATE_TIMER = "Regular Update Timeout";

	/**
	 * Infinity metric.
	 * As indicated in RFC2453, "The protocol is limited to networks whose
	 * longest path (the network's diameter) is 15 hops.  The designers
	 * believe that the basic protocol design is inappropriate for larger
	 * networks."
	 */
	public final static int INFINITY = 16;
	
	/** Max hop for reaching a DV neighbor. */
	final static int MAX_HOP = 1;
	
	/** Routing table entry size. */
	final static int RTSIZE = 14;
	
	Object triggeredUpdateTimer = null;
		// null if no trigger timeout is currently set up
	ACATimer regularTimerHandle = null;
	NeighborTimer neighborTimerChain = null;

	public final static int DEBUG_IO          = 0; // io operations: send/rcv
	public final static int DEBUG_TIMEOUT     = 1; // timeout events
	public final static int DEBUG_ROUTE       = 2; // route updates
	public final static int DEBUG_SEND_UPDATE = 3; // update neighbors
	private final static String[] DEBUG_LEVEL_NAMES = {
		"debug_io",
		"debug_timeout",
		"debug_route",
		"debug_send_update",
	};
	
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVEL_NAMES; }
	
	
	/**
	 * Set of interfaces that this DVMRP instance operates on.
	 * Default is null, meaning including all normal interfaces but no virtual
	 * ifs.
	 */
	drcl.data.BitSet ifset = null;

	static final long FLAG_VIRTUAL_ENABLED  = 1L << FLAG_UNDEFINED_START;

	public DV()
	{ super(); }

	public DV(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	/**
	 * Provide routing table information.
	 */
	public String info()
	{
		return "Mode: " + getMode() + "\n"
				+ "Ifset: "
				+ (ifset == null? "all " + getMode().toLowerCase()
								+ " ifs": ifset.toString()) + "\n"
				+ "TriggeredUpdateTimer Set? " 
				+ (triggeredUpdateTimer == null? "No": "Yes") + "\n"
				+ "RegularTimer: " + regularTimerHandle + "\n";
	}
	
	public void reset()
	{
		super.reset();
		triggeredUpdateTimer = null;
		regularTimerHandle = null;
		neighborTimerChain = null;
	}

	/** Sets the set of the interfaces that this protocol is operated on.
	 * Set this *only* before start-up. */
	public void setIfset(drcl.data.BitSet ifset_)
	{ ifset = ifset_; }
	
	/** Returns the set of the interfaces that this protocol is operated on. */
	public drcl.data.BitSet getIfset()
	{ return ifset; }

	/** Sets the operation mode.  Accepted modes: PHYSICAL, VIRTUAL.
	 * PHYSICAL: works with physical interfaces only.
	 * VIRTUAL: works with virtual (tunnel) interfaces only.
	 * Set this *only* before start-up. 
	 */
	public void setMode(String mode_)
	{
		String m = mode_.trim().toLowerCase();
		if (m.equals("physical"))
			_setPhysical();
		else if (m.equals("virtual"))
			_setVirtual();
		else
			error("setMode()", "unrecgonized mode: " + mode_);
	}

	/**
	 * @see #setMode(String).
	 */ 
	public String getMode()
	{ return _isPhysical()? "PHYSICAL": "VIRTUAL"; }

	void _setVirtual()
	{ setComponentFlag(FLAG_VIRTUAL_ENABLED, true); }
	
	void _setPhysical()
	{ setComponentFlag(FLAG_VIRTUAL_ENABLED, false); }

	boolean _isVirtual()
	{ return getComponentFlag(FLAG_VIRTUAL_ENABLED) != 0; }
	
	boolean _isPhysical()
	{ return getComponentFlag(FLAG_VIRTUAL_ENABLED) == 0; }
	
	// Implements {@link drcl.inet.Protocol#neighborUpEventHandler}.
	protected void neighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		if (_isVirtual()) return;
		if (ifset == null) ifset = new drcl.data.BitSet();
		ifset.set(ifindex_);
		_neighborUpEventHandler(ifindex_, neighbor_, inPort_);
	}
	
	// Implements {@link drcl.inet.Protocol#vNeighborUpEventHandler}.
	protected void vNeighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		if (_isPhysical()) return;
		if (ifset == null) ifset = new drcl.data.BitSet();
		ifset.set(ifindex_);
		_neighborUpEventHandler(ifindex_, neighbor_, inPort_);
	}
	
	/**
	 * Neighbor up event: sends a request to the neighbor.
	 * Activates/sets up the regular update timer.
	 * @param ifindex_ index of the interface.
	 */
	void _neighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		// checks if the interface is in the ifset
		if (ifset != null && !ifset.get(ifindex_)) return;

		super.neighborUpEventHandler(ifindex_, neighbor_, inPort_);
		
		// add a route entry if the local NetAddress on that interface is
		// a summary of a network
		NetAddress local_ = IFQuery.getLocalNetAddress(ifindex_, ifport);
		RTKey rtKey_ = new RTKey(0, 0, local_.getAddress(), local_.getMask(),
						0, 0);
		RTEntry rtEntry_ = (RTEntry)retrieveRTEntry(rtKey_,
						RTConfig.MATCH_EXACT);
		if (rtEntry_ == null) {
			DVExtension rtExt_ = new DVExtension(0);
			addRTEntry(rtKey_, Address.NULL_ADDR, (drcl.data.BitSet)null,
							rtExt_, Double.NaN);
		}
		
		// send a route request to the neighbor
		DVPacket p_ = new DVPacket(DVPacket.REQUEST, VERSION);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("SEND REQUEST to " + neighbor_);
		forward(p_, getDVPacketSize(p_), Address.NULL_ADDR,
						neighbor_.getAddress(), false,
						MAX_HOP, InetPacket.CONTROL, ifindex_);
		if (regularTimerHandle != null) cancelTimeout(regularTimerHandle);
		regularTimerHandle = setTimeout(REGULAR_UPDATE_TIMER,
						REGULAR_UPDATE_TIMEOUT_PERIOD);
	}

	static final int MIN_SIZE = 16;

	public static int getDVPacketSize(DVPacket dvpkt_)
	{
		return MIN_SIZE + dvpkt_.getNumRTEs()*20;
	}
	
	protected void neighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{
		// DV determines NEIGHBOR-DOWN event itself
	}
	
	protected void vNeighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{
		// DV determines NEIGHBOR-DOWN event itself
	}
	
	void _activateTriggeredUpdateTimer()
	{
		if (triggeredUpdateTimer == null) {
			triggeredUpdateTimer = TRIGGERED_UPDATE_TIMER;
			setTimeout(triggeredUpdateTimer, TRIGGERED_UPDATE_TIMEOUT_PERIOD);
		}
	}
	
	boolean _isTriggeredUpdateTimerSet()
	{
		return triggeredUpdateTimer != null;
	}
	
	// send updates (the RT entries) to neighbors (to specific neighbor on
	// specific interface if specified)
	// consider reverse poison and if mask
	void _updateNeighbors(RTEntry[] ee_, int outIf_, long specificNeighbor_)
	{
		if (ee_ == null) ee_ = new RTEntry[0];
		//InterfaceInfo[] all_ = IFQuery.getInterfaceInfos(ifport);
		InterfaceInfo[] all_ = IFQuery.getInterfaceInfos(ifset, ifport);
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("PREPARE UPDATE: "
				  + (specificNeighbor_ == Address.NULL_ADDR? "": "for "
						  + specificNeighbor_ + ", ")
				  + drcl.util.StringUtil.toString(ee_)); 
		
		// go through each neighbor at each interface
		for (int i=0; i<all_.length; i++) {
			if (outIf_ >= 0 && outIf_ != i) continue;
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
				neighbors_ = new NetAddress[]{new NetAddress(
								specificNeighbor_, -1)};
			if (neighbors_ == null || neighbors_.length == 0) continue;

			for (int j=0; j<neighbors_.length; j++) {
				if (neighbors_[j] == null) continue;
				long neighbor_ = neighbors_[j].getAddress();
				
				DVPacket pkt_ = new DVPacket(DVPacket.UPDATE, VERSION);
				
				for (int k=0; k<ee_.length; k++) {
					if (ee_[k] == null) continue;
					RTKey key_ = ee_[k].getKey();
					long dest_ = key_.getDestination();
					if (!drcl.inet.InetConfig.Addr.isUnicast(dest_)) continue;
					long destmask_ = key_.getDestinationMask();
					long nexthop_ = ee_[k].getNextHop();
					Object e_ = ee_[k].getExtension();
					// check if DV can handle this entry
					if (e_ == null || !(e_ instanceof DVExtension)
									&& !e_.equals(HOST_ENTRY_EXT)) {
						ee_[k] = null;
						continue;
					}

					// Internal routing entry, keep this to myself
					if (e_ instanceof DVExtension
									&& ((DVExtension)e_).metric == 0) {
						if (isDebugEnabled()
										&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
							debug("- TO NEIGHBOR " + myselfaddr_+ "-->"
								+ neighbor_ + ": " + ee_[k]
								+ "-- internal entry");
						ee_[k] = null; continue;
					}
					
					if (nexthop_ == neighbor_) {
						// reverse poison
						pkt_.addRTE(dest_, destmask_, nexthop_, INFINITY);
						if (isDebugEnabled()
										&& isDebugEnabledAt(DEBUG_SEND_UPDATE))
							debug("+ TO NEIGHBOR " + myselfaddr_+ "-->"
								+ neighbor_ + ": " + ee_[k]
								+ "-- poison reverse");
						continue; // bypass mask test
					}
					
					// mask test
					if (maskedMyself_ != (dest_ & myselfMask_)) {
						long tmpNexthop_ = nexthop_;
						// hide next hop
						if (maskedMyself_ == (nexthop_ & myselfMask_))
							tmpNexthop_ = myself_;
					
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
						pkt_.addRTE(dest_, destmask_, nexthop_, 
							e_ instanceof DVExtension?
							((DVExtension)e_).metric: 1);
						continue;
					}

					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND_UPDATE))
						debug("- TO NEIGHBOR " + myselfaddr_+ "-->"
							+ neighbor_ + ": " + ee_[k] + "-- COVERED: "
							+ ee_[k] + " covered by " + myselfaddr_);
					// don't add entry that is masked by the mask at the
					// interface
					// this entry will be covered by the entry of myself that is
					// artificially added
					//pkt_.addRTE(myself_, myselfMask_, nexthop_, e_.metric);
				} // end k, loop on ee_ (RTEntry[])

				pkt_.addRTE(myself_, myselfMask_, myself_, 0);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND_UPDATE))
					debug("+ TO NEIGHBOR " + myselfaddr_+ "-->" + neighbor_
									+ "-- myself entry");
				
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
					debug("SEND " + pkt_);
				
				forward(pkt_, getDVPacketSize(pkt_), myself_, neighbor_,
								false, MAX_HOP, InetPacket.CONTROL, i);
			} // end j, loop on neighbors_ (NetAddress[])
		} // end i, loop on all_ (InterfaceInfo[])
	}
	
	protected void process(Object data_, drcl.comp.Port inPort_)
	{
		lock(this);
		super.process(data_, inPort_);
		unlock(this);
	}

	// handling timeouts
	protected void timeout(Object data_)
	{ 
		long source = IDLookup.getDefaultID(idport);
		
		if(data_ == REGULAR_UPDATE_TIMER) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug("REGULAR UPDATE");
			RTEntry[] all_ = retrieveAllRTEntries();
			if (all_ == null) return;
			_updateNeighbors(all_, -1, Address.NULL_ADDR);
			regularTimerHandle = setTimeout(REGULAR_UPDATE_TIMER,
							REGULAR_UPDATE_TIMEOUT_PERIOD);
		}
		else if (data_ == triggeredUpdateTimer) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug("TRIGGERED UPDATE");
			triggeredUpdateTimer = null;
			//	Go over the RT to see get changed RT entries
			//	for each changed RT entry, mark "unchanged"
			//	if any change
			//		send out an update to all neighbors (consider poison
			//		reverse and if mask)
			RTEntry[] all_ = retrieveAllRTEntries();
			if (all_ == null) return;
				
			Vector v_ = new Vector(); // store changed entries
			for (int i=0; i<all_.length; i++) {
				RTEntry entry_ = all_[i];
				if (!drcl.inet.InetConfig.Addr.isUnicast(
										entry_.getKey().getDestination()))
					continue;
				Object tmp_ = entry_.getExtension();
				if (!(tmp_ instanceof DVExtension)) continue;
				DVExtension ext_ = (DVExtension)tmp_;
				if (ext_ == null || !ext_.changed) continue;
				ext_.changed = false;
				v_.addElement(entry_);
			}
			if (v_.size() > 0) {
				RTEntry[] tmp_ = new RTEntry[v_.size()];
				v_.copyInto(tmp_);
				_updateNeighbors(tmp_, -1, Address.NULL_ADDR);
			}
		}
		else { // neighbor timeout
			if (neighborTimerChain == null) return;
			try {
				boolean anychange_ = false;
				double now_ = getTime();
				for (NeighborTimer t_ = neighborTimerChain; t_ != null;
								t_ = t_.next) {
					if (t_.time > now_) continue;
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
						debug(t_);
					t_.time = now_ + NEIGHBOR_TIMEOUT_PERIOD;

					long neighbor_ = t_.neighbor;
					RTEntry[] all_ = retrieveAllRTEntries();
					if (all_ == null) return;
				
					// for each RT entry with next hop to the down neighbor
					//		mark RT metric infinity
					//		set up delete timer
					// if there is any change in RT && no triggered update
					// 	 timer is set
					//		set up a triggered update timeout
					for (int i=0; i<all_.length; i++) {
						RTEntry entry_ = all_[i];
						if (entry_.getNextHop() != neighbor_) continue;
						DVExtension ext_ = (DVExtension)entry_.getExtension();
						if (ext_ == null) continue;
						if (ext_.metric < INFINITY) {
							RTEntry tmp_ = (RTEntry)entry_.clone();
							ext_ = (DVExtension)tmp_.getExtension();
							ext_.metric = INFINITY;
							ext_.changed = true;
							anychange_ = true;
							addRTEntry(tmp_, DELETE_TIMEOUT_PERIOD);
						}
						else
							addRTEntry(entry_, DELETE_TIMEOUT_PERIOD);
						if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_ROUTE)
										|| isDebugEnabledAt(DEBUG_TIMEOUT)))
							debug("hold-down " + entry_
											+ " due to neighbor timeout");
					}
				}
				if (anychange_) 
					_activateTriggeredUpdateTimer();
				_setNeighborTimer();
			}
			catch (Exception e_) {
			}
		}
	}

	protected void dataArriveAtDownPort(Object msg_, Port downPort_) 
	{
		InetPacket p_ = (InetPacket)msg_;
		DVPacket dvp_ = (DVPacket)p_.getBody();
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("RECEIVED: " + msg_);
		
		int incomingIf_ = p_.getIncomingIf();

		// drop the packet if incomingIf_ is not in 'ifset'
		if (ifset == null || !ifset.get(incomingIf_)) return;

		long source_ = p_.getSource();
		
		if (dvp_.getCommand() == DVPacket.REQUEST) {
			RTEntry[] all_ = retrieveAllRTEntries();
			if (all_ == null) return;
			_updateNeighbors(all_, incomingIf_, source_);
		}
		else {
			_routingUpdateHandler(dvp_, source_, incomingIf_);
		}
	}

	// Handel unicast query from CSL.
	public int[] routeQueryHandler(RTKey key_, Port inPort_)
	{
		return null;
	}

	// Handel unicast query from CSL.
	public int[] routeQueryHandler(InetPacket pkt_, int incomingIf_,
					Port inPort_)
	{
		return null;
	}
	
	static final DVExtension INFINITE_DVE = new DVExtension(INFINITY);

	private NeighborTimer _lookForNeighborTimer(long neighbor_)
	{
		for (NeighborTimer t_ = neighborTimerChain; t_ != null; t_ = t_.next)
			if (t_.neighbor == neighbor_) return t_;
		return null;
	}

	private void _updateNeighborTimer(long neighbor_)
	{
		NeighborTimer t_ = _lookForNeighborTimer(neighbor_);
		double now_ = getTime();
		if (t_ == null) {
			t_ = new NeighborTimer(neighbor_, now_ + NEIGHBOR_TIMEOUT_PERIOD);
			t_.next = neighborTimerChain;
			neighborTimerChain = t_;
			setTimeoutAt(t_, t_.time);
		}
		else
			t_.time = now_ + NEIGHBOR_TIMEOUT_PERIOD;
	}

	private void _setNeighborTimer()
	{
		double time_ = Double.POSITIVE_INFINITY;
		NeighborTimer next_ = null;
		for (NeighborTimer t_ = neighborTimerChain; t_ != null; t_ = t_.next)
			if (t_.time < time_)
			{ time_ = t_.time; next_ = t_; }
		setTimeoutAt(next_, time_);
	}

	class NeighborTimer
	{
		double time;
		long neighbor;
		NeighborTimer next;

		NeighborTimer(long neighbor_, double time_)
		{
			neighbor = neighbor_;
			time = time_;
		}

		public String toString()
		{ return "NeighborTimeout:" + neighbor + "," + time; }
	}
	
	/**
	 * Update the distance vector table upon receipt of a distance
	 * vector information.
	 * 
	 * @param p is a vector of distance vector or neighbor information.
	 * @param ifs is the incoming interface
	 */
	private void _routingUpdateHandler(DVPacket dvp_, long neighbor_,
					int incomingIf_) 
	{
		//	set up or adjust the neighbor timer
		//	compare each RTE in the message
		//	for each change in RT
		//		update the entry and disable the delete timer if there's one
		//		mark the entry as "changed"
		//	if there is any change in RT && no triggered update timer is set
		//		set up a triggered update timeout
		_updateNeighborTimer(neighbor_);
		DVPacket.RTE[] ss_ = dvp_.getRTEs();
		boolean anychange_ = false;
		for (int i=0; i<ss_.length; i++) {
			DVPacket.RTE rte_ = ss_[i];
			long dest_ = rte_.getDestination();
			long destmask_ = rte_.getMask();
			long nexthop_ = rte_.getNextHop();
			int metric_ = rte_.getMetric();
			RTKey key_ = new RTKey(0, 0, dest_, destmask_, 0, 0);
			RTEntry entry_ = (RTEntry)retrieveRTEntry(key_,
							RTConfig.MATCH_EXACT);
			
			if (entry_ == null) {
				if (metric_ > INFINITY) continue; // invalid metric
			}
			
			Object tmp_ = null;
			if (entry_ != null)
				tmp_ = entry_.getExtension();
			
			if (tmp_ != null && !(tmp_ instanceof DVExtension))
				continue; // a host entry or an entry not created by DV
			DVExtension ext_ = (DVExtension)tmp_;
			
			if (entry_ == null && metric_ < INFINITY
				|| entry_ != null && entry_.getNextHop() == neighbor_
				&& metric_ + 1 != ext_.metric
				|| ext_ != null && metric_ + 1 < ext_.metric) {
				ext_ = new DVExtension(metric_ == INFINITY?
								metric_: metric_ + 1, true);
				anychange_ = true;
				
				if (metric_ < INFINITY) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE)) {
						if (entry_ == null)
							debug("Initial route from neighbor: " + neighbor_
								+ "-" + key_ + "   " + neighbor_ + "("
								+ incomingIf_ + ")   " + ext_);
						else if (entry_.getNextHop() == neighbor_)
							debug("Route update from NEXT-HOP neighbor: "
								+ entry_ + "\n\t\t--->" + ext_);
						else
							debug("Better route from NON-NEXT-HOP neighbor: "
								+ entry_ + "\n\t\t--->" + neighbor_ + "-"
								+ ext_);
					}
					addRTEntry(key_, neighbor_, incomingIf_, ext_, Double.NaN);
				}
				else {
					// invalidate the entry
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_ROUTE)
											|| isDebugEnabledAt(DEBUG_TIMEOUT)))
						debug("hold-down " + entry_
							+ " due to route invalidation from neighbor");
					addRTEntry(key_, neighbor_, incomingIf_, ext_,
									DELETE_TIMEOUT_PERIOD);
				}
			}
		}
		if (anychange_)
			_activateTriggeredUpdateTimer();
	}
}
