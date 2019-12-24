// @(#)CBT.java   1/2004
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

package drcl.inet.protocol.cbt;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Vector;
import drcl.comp.*;
import drcl.comp.tool.TimerList;
import drcl.net.Address;
import drcl.inet.InetPacket;
import drcl.inet.data.RTKey;
import drcl.inet.data.RTEntry;
import drcl.inet.data.NetAddress;
import drcl.inet.contract.IDLookup;
import drcl.inet.contract.RTConfig;
import drcl.inet.contract.McastHostEvent;

/*
---------------------------------------------
EVENT HANDLING
+ receiving McastHostJoin or a join
  - retreive the corresponding state, create it (in 'init' state)
    if necessary
  - if the state is in 'quiting'
    - cancel the quit rtx timer
    - remove all the interfaces
    - change the state to 'init' and continue below
  - if corresponding CBTInterface exists
    - if it's the upstream
      - report an error; stop
    - else
      - update host/router bit for that interface
      - continue;
  - else
    - create CBTInterface for that interface
  - if this node is the core and the state is in 'init'
    - change the state to 'confirmed' (no matter what it was)
    - add the state
    - create a route entry w/o any downstream
  - if the state is in 'confirmed'
    - if McastHostJoin
      - notify CSL
    - else
      - send back join-ack
      - set downstream expire timer for that interface if no such timer exists
      - set expireTime for that interface
    - graft the interface on the routing entry
  - if the state is in 'init'
    - change the state to 'transient'
    - create the upstream interface
    - forward a join upstream
    - set up join rtx timeout if McastHostJoin
    - set up transient/join timeout
    - add the state
  - if the state is in 'transient'
    - if it's a retransmissioin
      - forward the join upstream
    - else
      - stop
+ receiving a join-ack
  - retreive the corresponding state
  - if the state does not exist or the state is not in 'transient'
    - ignore the message; stop
  - if the corresponding CBTInterface is not upstream
    - report an error; stop
  - cancel join rtx and transient/join timers
  - change state to 'confirmed'
  - forward join-ack to all downstream router ifs
  - notify CSL
  - set expireTime for the upstream interface
  - if echo request timer does not exist for the upstream
    - set up echo request timer for the upstream
  - for each router downstream interface
    - set downstream expire timer for that interface if no such timer exists
    - set expireTime for that interface
  - create a route entry w/ the downstream interfaces grafted
+ QUIT_SEQUENCE on a state
  - change the state to 'quiting'
  - send a quit-notification upstream
  - start a quit rtx timer
  - cancel upstream expire timer if the state is the only one for the stream
  - cancel downstream expire timer if the state is the only one for the
    downstream
+ receiving a McastHostLeave or quit-notification
  - retreive the corresponding state
  - if the state does not exist or the state is in 'quiting'
    or no CBTInterface corresponds to the incoming if (this includes the case
    where McastHostLeave is received but the CBTInterface is not a host if,
    or a quit is received on a host if)
    - ignore the msg; stop
  - if the state is in 'transient'
    - if the child interface is the only interface in the state
      - cancel join rtx and transient/join timers
      - remove the state
    - else (another interface exists)
      - remove the interface
  - if the state is in 'confirmed'
    - if the child interface is the only interface in the state
      - start the "QUIT_SEQUENCE" on the state
    - else (another interface exists)
      - cancel downstream expire timer if the state is the only one for the
        downstream
      - remove the interface
      - prune the interface on the routing entry
+ receiving a echo-request
  - for each group in the request
    - retreive the corresponding state
    - if the state does not exist or is not in 'confirmed' or
      the incoming interface is not one of the downstream for the state
      - continue to next group
    - update expireTime for that interface
  - prepare and send a echo-reply on the incoming interface
+ receiving a echo-reply
  - for each group in the request
    - retreive the corresponding state
    - if the state does not exist or is not in 'confirmed' or
      the incoming interface is not the upstream for the state
      - continue to next group
    - update expireTime for that interface
  - schedule next echo request timeout
  - cancel echo-rtx timer
+ receiving a flush-tree
  - retreive the corresponding state
  - if the state does not exist
    - stop
  - if the state is in 'quitting'
    - cancel the quit rtx timer
  - if the state is in 'transient'
    - cancel the join rtx and transient timers
    - send flush-tree on all downstream ifs
  - if the state is in 'confirm'
    - send flush-tree on all downstream ifs
  - delete the state
+ delete/remove a state
  - for each downstream if that is host interface
    - notify CSL
+ timeout: join rtx
  - retreive the corresponding state
  - if the state does not exist
    - stop
  - if the state is not in 'transient'
    - report an error; stop
  - send a join-request upstream
  - set join rtx timer
+ timeout: transient/join
  - retreive the corresponding state
  - if the state does not exist
    - stop
  - if the state is not in 'transient'
    - report an error; stop
  - delete the state
+ timeout: quit rtx
  - retreive the corresponding state
  - if the state does not exist or is not in 'quitting'
    - stop
  - send a quit-notification upstream
  - if ++(quit rtx times) <= MAX_RTX
    - schedule next quit rtx timeout
  - else
    - delete the state
+ timeout: echo request timer
  - get the upstream interface from the timer
  - retrieve all the states that are in 'confirmed' and have matched upstream
  - if at lease one such state exists
    - put the groups in an echo-request and send it at the upstream interface
    - schedule echo request rtx timer
    - set up upstream expire timer for the upstream
    - set expireTime for the upstream interfaces of all states found
+ timeout: echo request rtx timer
  - if ++(#attempts) <= MAX_RTX
    - send echo request like it's echo request timeout
      (see timeout: echo request timer)
+ timeout: upstream expires
  - for each state that has matched upstream
    - if the upstream's expireTime <= now
      - send flush-tree for the state
      - remove the state
+ timeout: downstream expires
  - for each state that has matched downstream
    - if the downstream's expireTime <= now
      - remove the downstream if the interface is not a host one
      - prune the interface on the routing entry
      - remove the state if no downstream for the state
  - schedule next downstream expire timer
*/

/**
Implements the Core Based Tree (CBT) Multicast Routing protocol.
This version does not work on virtual or tunnel interfaces.

Doesn't implement
- support of (*, Core) and (S, G)
- unidirectional pruned state (instead, a quit simply deletes the child 
  interface from the entry)
- broadcast LAN, HELLO protocol, determining DR
- Bootstrap
- check on incoming interface for data forwarding
- exact tunneling for a non-member source to send pkts (instead, just forward
  it hop-by-hop toward the core, the pkts may be picked up by an on-tree
  router along the way)

@author Hung-ying Tyan
@version 1.0, 11/2002
  */
public class CBT extends drcl.inet.protocol.Routing
	implements drcl.inet.protocol.McastRouting, CBTConstants, ActiveComponent
{
	public String getName()
	{ return "CBT"; }
	
	protected Port idport = createIDServicePort(); // for ID lookup service
	protected Port mcastPort = addPort(".service_mcast"); // report error back
	//Port monitorPort = addPort(".monitor"); // report every CBT pkt received
	
	protected Port mcastHostPort = createMcastHostEventPort();
   		// to receive mcast host join/leave events

	//static final long FLAG_MONITOR_ENABLED  = 1L << FLAG_UNDEFINED_START;

	//public void setMonitorEnabled(boolean enabled_)
	//{ setComponentFlag(FLAG_MONITOR_ENABLED, enabled_); }

	//public boolean isMonitorEnabled()
	//{ return getComponentFlag(FLAG_MONITOR_ENABLED) != 0; }

	public final static int DEBUG_IO          = 0; // send/rcv
	public final static int DEBUG_TIMEOUT     = 1; // timeout events
	public final static int DEBUG_ROUTE       = 2; // route updates
	public final static int DEBUG_STATE       = 3; // state changes
	private final static String[] DEBUG_LEVEL_NAMES = {
		"debug_io",
		"debug_timeout",
		"debug_route",
		"debug_state"
	};
	
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVEL_NAMES; }

	static final CBTTimingPack DEFAULT_TIMING_PACK = new CBTTimingPack();

	protected CBTTimingPack timing = DEFAULT_TIMING_PACK;

	/** Router ID.  Only immediate routers need toneed to  have it. */
	protected long routerID = -1;

	/** Stores all the states. */
	protected HashMap mapState; // group -> CBTState

	protected TimerList timers = new TimerList();

	boolean doEcho = true;
	
	public CBT()
	{ super(); }

	public CBT(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	public String info()
	{
		if (mapState == null)
			return "Router:" + routerID + "--inactive, echo:"
					+ (doEcho? "enabled":"disabled");
		StringBuffer sb_ = new StringBuffer("Router:" + routerID + "\n"
					+ ", echo:" + (doEcho? "enabled":"disabled"));
		sb_.append(mapState.size() == 0?  "No state\n": "States: \n");

		for (Iterator it_ = mapState.entrySet().iterator(); it_.hasNext(); ) {
			Object s = ((Map.Entry)it_.next()).getValue();
			sb_.append("   " + s + "\n");
		}

		if (timers.size() == 0)
			sb_.append("No timer\n");
		else {
			sb_.append("Timers:\n");
			sb_.append(timers.info("   "));
		}
		return sb_.toString();
	}
	
	public void reset()
	{
		super.reset();
		if (mapState != null) mapState.clear();
	}
	
	public void setEchoEnabled(boolean e)
	{ doEcho = e; }

	public boolean isEchoEnabled()
	{ return doEcho; }

	protected void _start()
	{
		routerID = (int) IDLookup.getDefaultID(idport);
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
		if (data_ != timers) {
			error("timeout()", "wrong data: " + data_);
			return;
		}
		double now_ = getTime();
		CBTTimer t = (CBTTimer)timers.timeout(timerPort, now_);
		if (t == null) // cancelled timer
			return;
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
			debug("timeout: " + t);
		if (t instanceof CBTStateTimer) {
			CBTState s = ((CBTStateTimer)t).state;
			if (s == null || mapState.get(new Long(s.group)) != s)
				return;
			stateTimeout(t, s, now_);
		}
		else if (t instanceof CBTInterfaceTimer)
			ifTimeout(t, ((CBTInterfaceTimer)t).ifindex, now_);
		else
			error("timeout()", "unrecognized timer: " + t);
	}

	protected void stateTimeout(CBTTimer t, CBTState s, double now_)
	{
		switch (t.type) {
		case JOIN_RTX:
			if (s.state != TRANSIENT) {
				error("timeout()", "JOIN_RTX but not in transient: " + s);
				return;
			}
			else {
				_sendJoin(routerID, s.group, s.core, s.upstreamIf);
				_setJoinRtxTimer(s, now_);
			}
			break;
		case JOIN_TIMEOUT:
			if (s.state != TRANSIENT) {
				error("timeout()", "JOIN_TIMEOUT but not in transient: " + s);
				return;
			}
			else
				_removeState(s);
			break;
		case QUIT_RTX:
			if (s.state != QUITTING) return;
			_sendQuit(s.group, s.core, s.upstreamIf);
			if (++s.ntries < timing.maxRtx-1)
				// "-1": count the one that was sent the first time (not
				// due to timeout)
				_setQuitRtxTimer(s, now_);
			else
				_removeState(s);
			break;
		default:
			error("timeout()", "unrecognized timer: " + t);
		}
	}

	protected void ifTimeout(CBTTimer t, int ifindex_, double now_)
	{
		if (!doEcho) return;

		switch (t.type) {
		case ECHO:
		case ECHO_RTX:
			int ntries_ = ((CBTInterfaceTimer)t).ntries + 1;
			double expireTime_ = Double.NaN;
			if (t.type == ECHO) {
				_setUpstreamExpireTimer(ifindex_, now_);
				expireTime_ = now_;
			}
			else if (ntries_ > timing.maxRtx)
				break;
			CBTPacket p = _sendEcho(ECHO_REQUEST, ifindex_, expireTime_);
			if (p != null)
				_setEchoRtxTimer(ifindex_, p, ntries_, now_);
			break;
		case UPSTREAM_EXPIRE:
			for (Iterator it_ = mapState.entrySet().iterator(); 
							it_.hasNext(); ) {
				CBTState s = (CBTState)((Map.Entry)it_.next()).getValue();
				if (s.state != CONFIRMED || s.upstreamIf != ifindex_)
					continue;
				CBTInterface if_ = s.getIf(ifindex_);
				if (if_.expireTime <= now_) {
					_sendFlush(s);
					_removeState(s);
				}
			}
			break;
		case DOWNSTREAM_EXPIRE:
			for (Iterator it_ = mapState.entrySet().iterator(); 
							it_.hasNext(); ) {
				CBTState s = (CBTState)((Map.Entry)it_.next()).getValue();
				if (s.state != CONFIRMED || !s.isDownstreamIf(ifindex_))
					continue;
				CBTInterface if_ = s.getIf(ifindex_);
				// XXX: host interface should be refreshed by IGMP
				// membership report
				if (if_.expireTime <= now_ && !if_.isHostIf) {
					s.removeIf(ifindex_);
  					// prune the interface on the routing entry
					pruneRTEntry(new RTKey(0, 0, s.group, -1, 0, 0),
									ifindex_, getName(), -1.0);
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE))
						debug("PRUNE " + ifindex_ + " from group " + s.group);
					if (!s.anyDownstreamIf())
						_removeState(s);
				}
			}
			_setDownstreamExpireTimer(ifindex_, now_);
			break;
		default:
			error("timeout()", "unrecognized timer: " + t);
		}
	}

	protected void dataArriveAtDownPort(Object msg_, Port downPort_) 
	{
		InetPacket ip_ = (InetPacket)msg_;
		CBTPacket p = (CBTPacket)ip_.getBody();

		//if (isMonitorEnabled())
		//	monitorPort.doSending(p);
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("RECEIVED: " + msg_);
		
		int incomingIf_ = ip_.getIncomingIf();
		
		switch (p.type) {
		case JOIN_REQUEST:
			joinHandler(p, incomingIf_, ip_.getDestination(), false);
			break;
		case JOIN_ACK:
			ackHandler(p, incomingIf_);
			break;
		case QUIT_NOTIFICATION:
			quitHandler(p, incomingIf_, false);
			break;
		case ECHO_REQUEST:
		case ECHO_REPLY:
			echoHandler(p, incomingIf_);
			break;
		case FLUSH_TREE:
			flushHandler(p, incomingIf_);
			break;
		default:
			otherHandler(p, incomingIf_, ip_.getDestination());
		}
	}

	/** Stores core router IDs. */
	protected static HashMap mapCore = new HashMap();

	public static void addCore(long group_, long core_)
	{
		mapCore.put(new Long(group_), new Long(core_));
	}

	protected static synchronized long getCore(long group_)
	{
		Long core_ = (Long)mapCore.get(new Long(group_));
		if (core_ == null)
			throw new AssertionError("No core for multicast group " + group_
				+ "; use drcl.inet.protocol.cbt.CBT.addCore(group, core)"
				+ " to add the core for the group");
		return core_.longValue();
	}

	/** @param hostJoin_ true if the join is from a host attached locally. */
	protected void joinHandler(CBTPacket p, int incomingIf_, 
					long targetRouter_, boolean hostJoin_)
	{
		long requester_ = p.requester;
		long group_ = p.group;
		long core_ = p.core;
		CBTState s = _getState(group_);

		if (s == null)
			s = new CBTState(group_, core_); // in INIT state by default

		if (s.state == QUITTING) {
			s.reset();
			_cancelQuitRtxTimer(s);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_STATE))
				debug("STATE reset from QUITTING: " + s);
		}

		CBTInterface if_ = s.getIf(incomingIf_);
		boolean ifcreated_ = false;
		if (if_ == null) {
			if_ = s.addIf(incomingIf_,
							false, // upstream
							false, // broadcast
							hostJoin_, // hostIf
							!hostJoin_, // routerIf
							null); // extension
			ifcreated_ = true;
		}
		else {
			if (if_.isUpstream) {
				error("joinHandler", "join received at an upstream if: " + s);
				return;
			}
			if (hostJoin_)
				if_.isHostIf = true;
			else
				if_.isRouterIf = true;
		}

		if (routerID == core_ && s.state == INIT) {
			s.state = CONFIRMED;
			_addState(group_, s);
			// create a route entry (w/o any outgoing ifs)
			addRTEntry(new RTKey(0, 0, group_, -1, 0, 0), (int[])null,
							getName(), -1.0);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE))
				debug("CREATE empty RT for group " + group_);
		}

		double now_ = getTime();

		if (s.state == CONFIRMED) {
			// on tree
			if (hostJoin_) {
				_notifyCSL(true, group_, incomingIf_);
			}
			else {
				// send ack back at the interface where request comes
				_sendAck(requester_, group_, core_, incomingIf_, null);
				_setDownstreamExpireTimer(incomingIf_, now_);
				if_.expireTime = now_ + timing.getDownstreamExpireTime();
			}
  			// graft the interface on the routing entry
			graftRTEntry(new RTKey(0, 0, group_, -1, 0, 0),
										incomingIf_, getName(), -1.0);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE))
				debug("GRAFT at " + incomingIf_ + " for group " + group_);
		}
		else if (s.state == INIT) {
			// off tree
			RTEntry e = super.retrieveBestRTEntryDest(targetRouter_);
			int[] ifs_ = e == null? null: e._getOutIfs();
			if (e == null || ifs_ == null || ifs_.length == 0) {
				error("joinHandler()", "no route to target router: " + p);
				return;
			}
			int upstreamIf_ = ifs_[0];
			s.state = TRANSIENT;
			s.addIf(upstreamIf_,
							true, // upstream
							false, // broadcast
							false, // hostIf
							false, // routerIf
							null); // extension
			s.ntries = 1;
			_addState(group_, s);
			// pkt size = 8 (fixed header) + 3 (addr) * 4
			p.lasthop = routerID; 
			forward(p, 20, routerID, targetRouter_, true, 1, 
							InetPacket.CONTROL, upstreamIf_);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
				debug("SEND at if" + upstreamIf_ + ": " + p);
			_setJoinTimers(s, hostJoin_, now_);
		}
		else {
			// else s is in 'transient'
			if (!ifcreated_) {
				// a retransmission
				int upstreamIf_ = s.upstreamIf;
				if (upstreamIf_ < 0) {
					error("joinHandler()", "no upstream to forward a "
									+ "retransmitted join-request: " + s);
				}
				p.lasthop = routerID; 
				forward(p, 20, routerID, core_, true, 1, 
							InetPacket.CONTROL, upstreamIf_);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
					debug("SEND at if" + upstreamIf_ + ": " + p);
			}
			// else do nothing
		}
	}

	protected void ackHandler(CBTPacket p, int incomingIf_)
	{
		long requester_ = p.requester;
		long group_ = p.group;
		long core_ = p.core;
		CBTState s = _getState(group_);

		if (s == null || s.state != TRANSIENT)
			return;

		CBTInterface if_ = s.getIf(incomingIf_);
		if (if_ == null || !if_.isUpstream) {
			error("ackHandler()",
				"join-ack received at a non-upstream if: " + incomingIf_
							+ ", state = " + s);
			return;
		}

		s.state = CONFIRMED;
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_STATE))
			debug("STATE changed from TRANSIENT: " + s);
		s.ntries = 0;
		double now_ = getTime();

		_cancelJoinTimers(s);
		_setEchoRequestTimer(incomingIf_, now_);
		if_.expireTime = Double.NaN;

		drcl.data.BitSet bs_ = new drcl.data.BitSet(s.ifs.length);
		bs_.set(incomingIf_); // include upstream as well
		for (int i=0; i<s.ifs.length; i++) {
			if_ = s.ifs[i];

			if (if_ == null || if_.isUpstream)
				continue;
			bs_.set(i);
			_setDownstreamExpireTimer(i, now_);
			if_.expireTime = now_ + timing.getDownstreamExpireTime();

			if (if_.isRouterIf)
				_sendAck(requester_, group_, core_, i, null);
			if (if_.isHostIf)
				_notifyCSL(true, group_, i);
		}
  		// create a route entry w/ the downstream interfaces grafted
		addRTEntry(new RTKey(0, 0, group_, -1, 0, 0), bs_, getName(), -1.0);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE))
			debug("CREATE RT for group " + group_ + " at " + bs_);
	}

	protected void quitHandler(CBTPacket p, int incomingIf_, boolean hostJoin_)
	{
		long requester_ = p.requester;
		long group_ = p.group;
		long core_ = p.core;
		CBTState s = _getState(group_);

		if (s == null || s.state == QUITTING)
			return;

		CBTInterface if_ = s.getIf(incomingIf_);
		if (if_ == null || (hostJoin_ && !if_.isHostIf)
						|| (!hostJoin_ && !if_.isRouterIf))
			return;

		if (if_ != null && if_.isUpstream) {
			error("quitHandler()", "receive a quit at upstream? "
							+ "quit:" + p + ", state:" + s);
		}

		if (if_.isHostIf)
			_notifyCSL(false, group_, incomingIf_);
		s.removeIf(incomingIf_);
		if (s.state == TRANSIENT) {
			if (!s.anyDownstreamIf()) {
				_cancelJoinTimers(s);
				_removeState(s);
			}
		}
		else if (s.state == CONFIRMED) {
			_cancelDownstreamExpireTimer(incomingIf_);
			if (!s.anyDownstreamIf())
				_startQuitSequence(s);
			else {
  				// prune the interface on the routing entry
				pruneRTEntry(new RTKey(0, 0, group_, -1, 0, 0),
										incomingIf_, getName(), -1.0);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE))
					debug("PRUNE " + incomingIf_ + " from group " + group_);
			}
		}
		else {
			error("quitHandler()", "wrong state to quit: " + s);
		}
	}

	protected void echoHandler(CBTPacket p, int incomingIf_)
	{
		if (mapState == null) {
			error("echoHandler()", "no state exists in this router");
			return;
		}
		long requester_ = p.requester;
		long[] aa_ = p.addrArray;
		boolean isRequest_ = p.type == ECHO_REQUEST;
		HashMap map_ = new HashMap();
		for (int i=0; i<aa_.length; i++)
			map_.put(new Long(aa_[i]), map_);

		double now_ = getTime();
		for (Iterator it_ = mapState.entrySet().iterator(); it_.hasNext(); ) {
			CBTState s = (CBTState)((Map.Entry)it_.next()).getValue();
			if (s.state != CONFIRMED) continue;
			CBTInterface if_ = s.getIf(incomingIf_);
			if (if_ == null
				|| (isRequest_ && if_.isUpstream)
				|| (!isRequest_ && !if_.isUpstream))
				continue;
			if (map_.containsKey(new Long(s.group))) {
				if (isRequest_)
					if_.expireTime = now_ + timing.getDownstreamExpireTime();
				else
					if_.expireTime = Double.NaN; // being refreshed
			}
		}
		if (isRequest_)
			_sendEcho(ECHO_REPLY, incomingIf_, Double.NaN);
		else {
  			// schedule next echo request timeout
			_setEchoRequestTimer(incomingIf_, now_);
			_cancelEchoRtxTimer(incomingIf_);
			// don't cancel upstream expire timer as some state may not be
			// refreshed by the echo-reply
		}
	}

	protected void flushHandler(CBTPacket p, int incomingIf_)
	{
		long requester_ = p.requester;
		long group_ = p.group;
		long core_ = p.core;
		CBTState s = _getState(group_);
		if (s == null) return;

		if (s.state == QUITTING)
			_cancelQuitRtxTimer(s);
		else if (s.state == TRANSIENT) {
			_cancelJoinTimers(s);
			_sendFlush(s);
		}
		else if (s.state == CONFIRMED)
			_sendFlush(s);
		_removeState(s);
	}

	// for extension
	protected void otherHandler(CBTPacket p, int incomingIf_, 
					long targetRouter_)
	{
		error("otherHandler()", "UNKNOWN msg type" + p);
	}

	/** Handel multicast query from CSL. */
	public int[] routeQueryHandler(RTKey key_, Port inPort_)
	{
		// XX: should encapsulate it in a unicast pkt and send it to core
		long group_ = key_.getDestination();
		long core_ = getCore(group_);
		RTEntry e = retrieveBestRTEntryDest(core_);
		int[] ifs_ = e._getOutIfs();
		return ifs_;
	}

	/** Starts the QUIT_SEQUENCE for the state. */
	protected void _startQuitSequence(CBTState s)
	{
		if (s.core == routerID) {
			_cancelDownstreamExpireTimers(s);
			_removeState(s);
			return;
		}
		if (s.upstreamIf < 0) {
			error("_startQuitSequence()", "no upstream to quit: " + s);
			return;
		}
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_STATE))
			debug("STATE changed to QUITTING: " + s);
		s.state = QUITTING;
		_sendQuit(s.group, s.core, s.upstreamIf);
		_setQuitRtxTimer(s, getTime());

		_cancelUpstreamExpireTimer(s.upstreamIf);
		_cancelDownstreamExpireTimers(s);
	}

	

	// routerJoin_ indicates whether the router joins or leaves the group
	// the notification (via IGMP) is sent at interface if_ 
	protected void _notifyCSL(boolean routerJoin_, long group_, int if_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("NOTIFY_CSL: " + (routerJoin_? "joined ": "left ")
						   	+ group_ + " at if " + if_);
		// notify CSL 
		// 		this can be distinguished from multi-homed router which may
		// 		act like a multicast host and join a multicast group
		// 		by sending out a IDConfig message to the same
		// 		.service_mcast port of CSL's.
		if (routerJoin_)
 			mcastPort.doSending(McastHostEvent.createJoinEvent(
								group_, if_));
		else
 			mcastPort.doSending(McastHostEvent.createLeaveEvent(
								group_, if_));
	}

	/** Sends flush-tree at all downstream interfaces of the state. */
	protected void _sendFlush(CBTState s)
	{
		CBTPacket p = new CBTPacket(FLUSH_TREE, routerID, s.group, s.core,
					   	routerID, null);
		boolean debug_ = isDebugEnabled() && isDebugEnabledAt(DEBUG_IO);
		if (s.ifs == null) return;
		for (int i=0; i<s.ifs.length; i++)
			if (s.isDownstreamIf(i)) {
				if (debug_)
					debug("SEND at if" + i + ": " + p);
				// pkt size = 8 (fixed header) + 3 (addr) * 4
				forward(p, 20, routerID, Address.NULL_ADDR, true, 1, 
							InetPacket.CONTROL, i);
			}
	}

	/** Sends an echo request or reply.
	 * This method checks every 'confirmed' state to see if
	 * the state has matched upstream (for echo-request) or
	 * downstream (for echo-reply).
	 * Returns the request/reply packet or null if no such state is found. */
	protected CBTPacket _sendEcho(int type_, int ifindex_, double expireTime_)
	{
		boolean isRequest_ = type_ == ECHO_REQUEST;
		Vector v = new Vector(); // put all groups found

		boolean setExpireTime_ = !Double.isNaN(expireTime_);
		for (Iterator it_ = mapState.entrySet().iterator(); it_.hasNext(); ) {
			CBTState s = (CBTState)((Map.Entry)it_.next()).getValue();
			if (s.state != CONFIRMED
				|| (isRequest_ && s.upstreamIf != ifindex_)
				|| (!isRequest_ && !s.isDownstreamIf(ifindex_)))
				continue;
			if (setExpireTime_)
				s.getIf(ifindex_).expireTime = expireTime_;
			v.addElement(new Long(s.group));
		}

		boolean debug_ = isDebugEnabled() && isDebugEnabledAt(DEBUG_IO);
		if (v.size() > 0) {
			long[] aa_ = new long[v.size()];
			for (int i=0; i<aa_.length; i++)
				aa_[i] = ((Long)v.elementAt(i)).longValue();
			CBTPacket p = new CBTPacket(type_, routerID, aa_, routerID, null);
			if (debug_)
				debug("SEND at if" + ifindex_ + ": " + p);
			// pkt size= 8 (fixed header) + aa_.length * 4
			forward(p, 8 + aa_.length * 4, routerID, Address.NULL_ADDR,
							true, 1, InetPacket.CONTROL, ifindex_);
			return p;
		}
		else
			return null;
	}

	protected CBTPacket _sendQuit(long group_, long core_, int if_)
	{
		CBTPacket q = new CBTPacket(QUIT_NOTIFICATION, routerID, group_,
						core_, routerID, null);
		// pkt size = 8 (fixed header) + 3 (addr) * 4
		forward(q, 20, routerID, Address.NULL_ADDR, true, 1, 
							InetPacket.CONTROL, if_);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("SEND at if" + if_ + ": " + q);
		return q;
	}

	protected CBTPacket _sendJoin(long requester_, long group_, long core_, 
					int if_)
	{
		CBTPacket p = new CBTPacket(JOIN_REQUEST, requester_, group_, core_,
					   	routerID, null);
		// pkt size = 8 (fixed header) + 3 (addr) * 4
		forward(p, 20, routerID, core_, true, 1, 
							InetPacket.CONTROL, if_);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("SEND at if" + if_ + ": " + p);
		return p;
	}

	// Original CBT replies only positive ack
	protected CBTPacket _sendAck(long requester_, long group_, long core_, 
					int if_, Object extension_)
	{
		CBTPacket ack_ = new CBTPacket(JOIN_ACK, requester_, group_, core_,
						routerID, extension_);
		// pkt size = 8 (fixed header) + 3 (addr) * 4
		forward(ack_, 20, routerID, requester_, true, 1, 
							InetPacket.CONTROL, if_);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_IO))
			debug("SEND at if" + if_ + ": " + ack_);
		return ack_;
	}

    /** Sets downstream expire timer for that interface
	 * if no such timer exists. */
	protected void _setDownstreamExpireTimer(int if_, double now_)
	{
		CBTInterfaceTimer t = new CBTInterfaceTimer(DOWNSTREAM_EXPIRE, if_);
		if (timers.containsKey(t))
			return;
		else
			timers.set(timerPort, t, t,
							now_ + timing.getDownstreamExpireTime());
	}

    /** Sets upstream expire timer for that interface
	 * if no such timer exists. */
	protected void _setUpstreamExpireTimer(int if_, double now_)
	{
		CBTInterfaceTimer t = new CBTInterfaceTimer(UPSTREAM_EXPIRE, if_);
		if (timers.containsKey(t))
			return;
		else
			timers.set(timerPort, t, t, now_ + timing.getUpstreamExpireTime());
	}

	/** Sets a timer for sending next echo request. */
	protected void _setEchoRequestTimer(int if_, double now_)
	{
		CBTInterfaceTimer t = new CBTInterfaceTimer(ECHO, if_);
		if (timers.containsKey(t))
			return;
		else
			timers.set(timerPort, t, t, now_ + timing.getEchoInterval());
	}

	protected void _cancelEchoRequestTimer(int if_)
	{
		timers.cancel(timerPort, new CBTInterfaceTimer(ECHO, if_));
	}

	protected void _setEchoRtxTimer(int if_, CBTPacket echoReq_, int ntries_,
					double now_)
	{
		CBTInterfaceTimer t = new CBTInterfaceTimer(ECHO_RTX, if_, echoReq_,
						ntries_);
		timers.set(timerPort, t, t, now_ + timing.getEchoRtxTime());
	}

	protected void _cancelEchoRtxTimer(int if_)
	{
		timers.cancel(timerPort, new CBTInterfaceTimer(ECHO_RTX, if_));
	}

	/** Cancels the downstream expire timer if no downstream router exists
	 * for the interface. */
	protected void _cancelDownstreamExpireTimer(int if_)
	{
		timers.cancel(timerPort, new CBTInterfaceTimer(DOWNSTREAM_EXPIRE, if_));
	}

	/** Cancels all the downstream expire timers if no downstream router exists
	 * for the downstream interfaces of the state. */
	protected void _cancelDownstreamExpireTimers(CBTState s)
	{
		if (s.ifs != null)
			for (int i=0; i<s.ifs.length; i++) {
				CBTInterface if_ = s.ifs[i];
				if (if_ == null || if_.isUpstream) continue;
				_cancelDownstreamExpireTimer(i);
			}
	}

	/** Cancels the upstream expire timer if no upstream router exists
	 * for the interface. */
	protected void _cancelUpstreamExpireTimer(int if_)
	{
		timers.cancel(timerPort, new CBTInterfaceTimer(UPSTREAM_EXPIRE, if_));
	}

	/** Sets up join rtx timeout if hostJoin_ and sets up the
	 * transient/join timer, for the state.
	 * @param hostJoin_ true if the join is from a host attached locally. */
	protected void _setJoinTimers(CBTState s, boolean hostJoin_, double now_)
	{
		// for simplicity, we use join-timeout type rather than
		// transient-timeout type but set different values depending on
		// whether it's hostJoin_ or not
		CBTStateTimer timer_ = new CBTStateTimer(JOIN_TIMEOUT, s);
		if (hostJoin_) {
			CBTStateTimer t = new CBTStateTimer(JOIN_RTX, s);
			timers.set(timerPort, t, t, now_ + timing.rtxInterval);
			timers.set(timerPort, timer_, timer_,
							now_ + timing.getJoinTimeout());
		}
		else
			timers.set(timerPort, timer_, timer_,
							now_ + timing.getTransientTimeout());
	}

	protected void _setJoinRtxTimer(CBTState s, double now_)
	{
		CBTStateTimer t = new CBTStateTimer(JOIN_RTX, s);
		timers.set(timerPort, t, t, now_ + timing.rtxInterval);
	}


	/** Sets up the quit-rtx timer. */
	protected void _setQuitRtxTimer(CBTState s, double now_)
	{
		CBTStateTimer timer_ = new CBTStateTimer(QUIT_RTX, s);
		timers.set(timerPort, timer_, timer_, now_ + timing.holdTime);
	}

	/** Cancels join rtx timeout (if exists) and the
	 * transient/join timer, for the state. */
	protected void _cancelJoinTimers(CBTState s)
	{
		timers.cancel(timerPort, new CBTStateTimer(JOIN_RTX, s));
		timers.cancel(timerPort, new CBTStateTimer(JOIN_TIMEOUT, s));
	}

	protected void _cancelQuitRtxTimer(CBTState s)
	{
		timers.cancel(timerPort, new CBTStateTimer(QUIT_RTX, s));
	}
	
	protected CBTState _getState(long group_)
	{
		if (mapState == null) return null;
		return (CBTState)mapState.get(new Long(group_));
	}

	protected void _addState(long group_, CBTState s)
	{
		if (mapState == null) mapState = new HashMap();
		mapState.put(new Long(group_), s);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_STATE))
			debug("STATE added: " + s);
	}

	protected void _removeState(CBTState s)
	{
		if (mapState == null) return;
		if (s.upstreamIf >= 0) {
			_cancelUpstreamExpireTimer(s.upstreamIf);
			_cancelEchoRequestTimer(s.upstreamIf);
		}
		// notify CSL on all host ifs of the forced leave
		for (int i=0; i<s.ifs.length; i++) {
			CBTInterface if_ = s.ifs[i];
			if (if_ != null && if_.isHostIf)
				_notifyCSL(false, s.group, i);
		}
		mapState.remove(new Long(s.group));
		// remove the corresponding routing entry
		removeRTEntry(new RTKey(0, 0, s.group, -1, 0, 0), RTConfig.MATCH_EXACT);
		if (isDebugEnabled()) {
			if (isDebugEnabledAt(DEBUG_STATE))
				debug("STATE removed: " + s);
			if (isDebugEnabledAt(DEBUG_ROUTE))
				debug("REMOVE RT for group " + s.group);
		}
	}

	RTEntry _getRTEntry(long group_)
	{
		// ignore incoming interface
		RTKey key_ = new RTKey(0, 0, group_, -1, 0, 0);
		return (RTEntry)retrieveRTEntry(key_, RTConfig.MATCH_EXACT);
	}

	/*
	 * Implements {@link drcl.inet.Protocol#mcastHostJoinEventHandler}.
	 * @param ifindex_ could be -1 if it is a local join (multihomed router).
	 */
	protected void mcastHostJoinEventHandler(
				long src_, long mask_,
				long group_, int ifindex_, Port inPort_)
	{
		super.mcastHostJoinEventHandler(src_, mask_, group_,
						ifindex_, inPort_);
		if (ifindex_ < 0) return;
		if (src_ != 0 || mask_ != 0) {
			if (isDebugEnabled())
				debug("MCAST JOIN: don't know how to handle specific source"
								+ " join, treat it as a general group join");
		}

		long core_ = getCore(group_);
		CBTPacket p = new CBTPacket(JOIN_REQUEST, routerID, group_, core_,
						routerID, null);
		joinHandler(p, ifindex_, core_, true);
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
		if (ifindex_ < 0) return;
		if (src_ != 0 || srcmask_ != 0) {
			if (isDebugEnabled())
				debug("MCAST LEAVE: don't know how to handle specific source"
								+ " leave, treat it as a general group leave");
		}

		long core_ = getCore(group_);
		CBTPacket p = new CBTPacket(QUIT_NOTIFICATION, routerID, group_, core_,
						routerID, null);
		quitHandler(p, ifindex_, true);
	}
}
