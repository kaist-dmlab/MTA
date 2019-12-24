// @(#)Protocol.java   10/2003
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

package drcl.inet;

import java.util.*;
import drcl.comp.*;
import drcl.comp.contract.EventContract;
import drcl.net.*;
import drcl.data.IntObj;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.util.scalar.IntSpace;

/**
The base class for transport, routing and other signaling protocols.

A protocol may receive services and events from {@link CoreServiceLayer}.
<p>It provides a complete set of methods (<code>create&lt;xxx&gt;Port()</code>)
for a subclass to create service and event ports for the purpose,
and a complete set of event handler/callback methods
(<code>&lt;xxx&gt;Handler(...)</code>) that a subclass may override to handle
interested events.  Refer to {@link CoreServiceLayer} for details regarding 
what services and events are available.

<p>A subclass must create the corresponding service port and then take 
advantage of the corresponding service contract class in order to use a
service.  For example, to use
the {@link RTConfig Route Configuration} services, a subclass must
create a port by calling {@link #createRTServicePort()} and then invoke the 
services with the port and the contract class.

<p>To receive and handle a type of events, a subclass must create the 
corresponding event port and then override the corresponding handler method.
For example, to receive the packet arrival event, the protocol subclass may call
{@link #createPktArrivalEventPort()} in the initialized codes, and then override
{@link #pktArrivalHandler(EventContract.Message, Port)}.
 
@author Hung-ying Tyan, Will Chen
@version 1.1, 8/2002
*/
public class Protocol extends Module implements InetConstants
{
	static {
		Contract c1_ = new IDLookup(Contract.Role_INITIATOR);
		Contract c2_ = new IDConfig(Contract.Role_INITIATOR);
		setContract(Protocol.class, SERVICE_ID_PORT_ID + "@",
						new ContractMultiple(c1_, c2_));
		c1_ = new RTLookup(Contract.Role_INITIATOR);
		c2_ = new RTConfig(Contract.Role_INITIATOR);
		setContract(Protocol.class, SERVICE_RT_PORT_ID + "@",
						new ContractMultiple(c1_, c2_));
		setContract(Protocol.class, SERVICE_IF_PORT_ID + "@",
						new IFQuery(Contract.Role_INITIATOR));
		setContract(Protocol.class, SERVICE_CONFIGSW_PORT_ID + "@",
						new ConfigSwitch(Contract.Role_INITIATOR));
		setContract(Protocol.class, UCAST_QUERY_PORT_ID + "@",
						new RTLookup(Contract.Role_REACTOR));
		setContract(Protocol.class, MCAST_QUERY_PORT_ID + "@",
						new RTLookup(Contract.Role_REACTOR));
		setContract(Protocol.class, EVENT_IF_PORT_ID + "@",
						new NeighborEvent(Contract.Role_REACTOR));
		setContract(Protocol.class, EVENT_VIF_PORT_ID + "@",
						new NeighborEvent(Contract.Role_REACTOR));
		setContract(Protocol.class, EVENT_MCAST_HOST_PORT_ID + "@",
						new McastHostEvent(Contract.Role_REACTOR));
		//setContract(Protocol.class, CoreServiceLayer.DUAL_HELP_PORT_ID + "@",
		//	new RTLookup(Contract.Role_REACTOR));
		c1_ = new PktSending(Contract.Role_INITIATOR);
		c2_ = new PktDelivery(Contract.Role_REACTOR);
		setContract(Protocol.class, Module.PortGroup_DOWN + "@",
						new ContractMultiple(c1_, c2_));
	}
	
	/**
	 Creates and returns a port to use with the {@link IDLookup}
	 and/or the {#link IDConfig} contracts
	 in this protocol implementation.
	 */
	public Port createIDServicePort()
	{ return addPort(SERVICE_ID_PORT_ID, false); }

	/**
	 Creates and returns a port to use with the {@link RTLookup}
	 and/or the {@link RTConfig RTConfig} contracts
	 in this protocol implementation.
	 */
	public Port createRTServicePort()
	{ return addPort(SERVICE_RT_PORT_ID, false); }

	/**
	 Creates and returns a port to use with the {@link IFQuery} contract
	 in this protocol implementation.
	 */
	public Port createIFQueryPort()
	{ return addPort(SERVICE_IF_PORT_ID, false); }

	/**
	 Creates and returns a port to use with the {@link ConfigSwitch} contract
	 in this protocol implementation.
	 */
	public Port createConfigSwitchPort()
	{ return addPort(SERVICE_CONFIGSW_PORT_ID, false); }

	/**
	 Creates and returns a port to accept the unicast queries from the core
	 service layer in this protocol implementation.
	 @see #routeQueryHandler(RTKey, Port)
	 @see #routeQueryHandler(InetPacket, int, Port)
	 */
	public Port createUcastQueryPort()
	{ return addServerPort(UCAST_QUERY_PORT_ID); }

	/**
	 Creates and returns a port to accept the multicast queries from the core 
	 service layer in this protocol implementation.
	 @see #routeQueryHandler(RTKey, Port)
	 @see #routeQueryHandler(InetPacket, int, Port)
	 */
	public Port createMcastQueryPort()
	{ return addServerPort(MCAST_QUERY_PORT_ID); }

	/**
	 Creates and returns a port to accept the packet arrival events from the 
	 core service layer in this protocol implementation.
	 @see #pktArrivalHandler(EventContract.Message, Port)
	 */
	public Port createPktArrivalEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_PKT_ARRIVAL_PORT_ID, 
					false); }

	/**
	 Creates and returns a port to accept the identity changed events from the 
	 core service layer in this protocol implementation.
	 @see #idAddedEventHandler(Object, Port)
	 @see #idRemovedEventHandler(Object, Port)
	 */
	public Port createIDChangedEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_ID_CHANGED_PORT_ID, 
					false); }

	/**
	 Creates and returns a port to accept the unicast routing entry changed 
	 events from the core service layer in this protocol implementation.
	 @see #rtAddedEventHandler(Object, Port)
	 @see #rtRemovedEventHandler(Object, Port)
	 @see #rtModifiedEventHandler(Object, Port)
	 */
	public Port createUnicastRTChangedEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_RT_UCAST_CHANGED_PORT_ID,
					false); }

	/**
	 Creates and returns a port to accept the multicast routing entry changed 
	 events from the core service layer in this protocol implementation.
	 @see #rtAddedEventHandler(Object, Port)
	 @see #rtRemovedEventHandler(Object, Port)
	 @see #rtModifiedEventHandler(Object, Port)
	 */
	public Port createMulticastRTChangedEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_RT_MCAST_CHANGED_PORT_ID,
					false); }

	/**
	 Creates and returns a port to accept the interface/neighbor events from 
	 the core service layer in this protocol implementation.
	 @see #neighborUpEventHandler(int, NetAddress, Port)
	 @see #neighborDownEventHandler(int, NetAddress, Port)
	 */
	public Port createIFEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_IF_PORT_ID, false); }

	/**
	 Creates and returns a port to accept the virtual interface/neighbor events 
	 from the core service layer in this protocol implementation.
	 @see #vNeighborUpEventHandler(int, NetAddress, Port)
	 @see #vNeighborDownEventHandler(int, NetAddress, Port)
	 */
	public Port createVIFEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_VIF_PORT_ID, false); }

	/**
	 Creates and returns a port to accept the multicast host join/leave events
	 in this protocol implementation.
	 @see #mcastHostJoinEventHandler(long, long, long, int, Port)
	 @see #mcastHostLeaveEventHandler(long, long, long, int, Port)
	 */
	public Port createMcastHostEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_MCAST_HOST_PORT_ID, 
					false); }
	
	/**
	 Called back when a unicast/multicast query event is received.
	 @see RTLookup
	 @see #createUcastQueryPort()
	 @see #createMcastQueryPort()
	 */
	protected int[] routeQueryHandler(RTKey request_, Port inPort_)
	{
		if (isDebugEnabled()) debug("ROUTE QUERY: " + request_);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////
	// added by Will
	/**
	 Called back when a unicast/multicast query event is received.
	 @see RTLookup
	 @see #createUcastQueryPort()
	 @see #createMcastQueryPort()
	 */
	protected int[] routeQueryHandler(InetPacket p, int incomingIf_,
					Port inPort_)
	{
		return routeQueryHandler(new RTKey(p.getSource(), p.getDestination(),
								incomingIf_), inPort_);
	}

	/**
	 Called back when a link broken event is received.
	 @see #createLinkBrokenEventPort()
	 */
	public void  LinkBrokenEventHandler(InetPacket p, Port inPort_) {
		if (isDebugEnabled()) debug("LINK BROKEN EVENT: " + p);
	}

	/**
	 Creates and returns a port to accept the link broken events from CSL.
	 */
	public Port createLinkBrokenEventPort()
	{ return addPort(new Port(Port.PortType_IN), EVENT_LINK_BROKEN_PORT_ID,
					false); }
	//////////////////////////////////////////////////////////////////////

	/**
	 Called back when a packet arrival event is received.
	 @see #createPktArrivalEventPort()
	 */
	protected void pktArrivalHandler(EventContract.Message event_, Port inPort_)
	{
		if (isDebugEnabled()) debug("PKT ARRIVAL EVENT: " + event_);
	}
	
	/**
	 Called back when an identity added event is received.
	 @see #createIDChangedEventPort()
	 */
	protected void idAddedEventHandler(Object data_, Port inPort_)
	{
		if (isDebugEnabled()) debug("ID ADDED EVENT: " + data_);
	}
	
	/**
	 Called back when an identity removed event is received.
	 @see #createIDChangedEventPort()
	 */
	protected void idRemovedEventHandler(Object data_, Port inPort_)
	{
		if (isDebugEnabled()) debug("ID REMOVED EVENT: " + data_);
	}
	
	/**
	 Called back when a routing entry added event is received.
	 @see #createUnicastRTChangedEventPort()
	 @see #createMulticastRTChangedEventPort()
	 */
	protected void rtAddedEventHandler(Object data_, Port inPort_)
	{
		if (isDebugEnabled()) debug("RT ADDED EVENT: " + data_);
	}
	
	/**
	 Called back when a routing entry removed event is received.
	 @see #createUnicastRTChangedEventPort()
	 @see #createMulticastRTChangedEventPort()
	 */
	protected void rtRemovedEventHandler(Object data_, Port inPort_)
	{
		if (isDebugEnabled()) debug("RT REMOVED EVENT: " + data_);
	}
	
	/**
	 Called back when a routing entry modified event is received.
	 @see #createUnicastRTChangedEventPort()
	 @see #createMulticastRTChangedEventPort()
	 */
	protected void rtModifiedEventHandler(Object data_, Port inPort_)
	{
		if (isDebugEnabled()) debug("RT MODIFIED EVENT: " + data_);
	}
	
	/**
	 Called back when a neighbor up event is received.
	 @param ifindex_ index of the interface.
	 @see #createIFEventPort()
	 */
	protected void neighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		if (isDebugEnabled())
			debug("NEIGHBOR UP EVENT: " + neighbor_ + " at " + ifindex_);
	}
	
	/**
	 Called back when a neighbor down event is received.
	 @param ifindex_ index of the interface.
	 @see #createIFEventPort()
	 */
	protected void neighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{
		if (isDebugEnabled())
			debug("NEIGHBOR DOWN EVENT: " + neighbor_ + " at " + ifindex_);
	}
	
	/**
	 Called back when a (virtual) neighbor up event is received.
	 @param ifindex_ index of the virtual interface.
	 @see #createVIFEventPort()
	 */
	protected void vNeighborUpEventHandler(int ifindex_, NetAddress neighbor_, 
					Port inPort_)
	{
		if (isDebugEnabled())
			debug("V_NEIGHBOR UP EVENT: " + neighbor_ + " at " + ifindex_);
	}
	
	/**
	 Called back when a (virtual) neighbor down event is received.
	 @param ifindex_ index of the interface.
	 @see #createVIFEventPort()
	 */
	protected void vNeighborDownEventHandler(int ifindex_, NetAddress neighbor_,
					Port inPort_)
	{
		if (isDebugEnabled())
			debug("V_NEIGHBOR DOWN EVENT: " + neighbor_ + " at " + ifindex_);
	}
	
	/**
	 Called back when a multicast host join event is received.
	 @param group_ the multicast group the host network joins.
	 @param ifindex_ index of the interface where the host network is connected.
	 	Could be -1 if it is a local join (multihomed router).
	 @see #createMcastHostEventPort()
	 */
	protected void mcastHostJoinEventHandler(long src_, long srcmask_, 
					long group_, int ifindex_, Port inPort_)
	{
		if (isDebugEnabled()) {
			if (src_ == 0 && srcmask_ == 0)
				debug("MCAST HOST JOIN EVENT: " + group_ + " at if "
								+ ifindex_);
			else if (srcmask_ == -1)
				debug("MCAST HOST JOIN EVENT: (" + src_ + "," + group_ 
								+ ") at if " + ifindex_);
			else
				debug("MCAST HOST JOIN EVENT: (" + src_ + "," + srcmask_ + "," 
								+ group_ + ") at if " + ifindex_);
		}
	}
	
	/**
	 Called back when a multicast host leave event is received.
	 @param group_ the multicast group the host network leaves.
	 @param ifindex_ index of the interface where the host network is connected.
	 	Could be -1 if it is a local leave (multihomed router).
	 @see #createMcastHostEventPort()
	 */
	protected void mcastHostLeaveEventHandler(long src_, long srcmask_,
					long group_, int ifindex_, Port inPort_)
	{
		if (isDebugEnabled()) {
			if (src_ == 0 && srcmask_ == 0)
				debug("MCAST HOST LEAVE EVENT: " + group_ + " at if "
								+ ifindex_);
			else if (srcmask_ == -1)
				debug("MCAST HOST LEAVE EVENT: (" + src_ + "," + group_
								+ ") at if " + ifindex_);
			else
				debug("MCAST HOST LEAVE EVENT: (" + src_ + "," + srcmask_ + ","
								+ group_ + ") at if " + ifindex_);
		}
	}
	
	protected void processOther(Object data_, Port inPort_)
	{
		String portid_ = inPort_.getID();
		boolean processed_ = true;
		
		if (portid_.equals(UCAST_QUERY_PORT_ID)
			|| portid_.equals(MCAST_QUERY_PORT_ID)) {
			if (data_ instanceof RTKey) {
				inPort_.doLastSending(routeQueryHandler((RTKey)data_, inPort_));
			}
			else if (data_ instanceof InetPacket) {
				inPort_.doLastSending(routeQueryHandler((InetPacket)data_,
									  -1, inPort_));
			}
			else if (data_ instanceof RTLookup.Message) {
				RTLookup.Message s = (RTLookup.Message)data_;
				inPort_.doLastSending(routeQueryHandler(
										s.getPacket(),
										s.getIncomingIf(), inPort_));
			}
			else
				processed_ = false;
		}
		else if (portid_.equals(EVENT_PKT_ARRIVAL_PORT_ID))
			pktArrivalHandler((EventContract.Message)data_, inPort_);
		else if (portid_.equals(EVENT_MCAST_HOST_PORT_ID)) {
			McastHostEvent.Message s_ = (McastHostEvent.Message)data_;
			long group_ = s_.getGroup();
			long src_ = s_.getSource();
			long srcmask_ = s_.getSourceMask();
			int ifindex_ = s_.getIfIndex();
			if (s_.isJoin())
				mcastHostJoinEventHandler(src_, srcmask_, group_, ifindex_,
								inPort_);
			else
				mcastHostLeaveEventHandler(src_, srcmask_, group_, ifindex_,
								inPort_);
		}
		else {
			// further distinguish event type
			String evtName_ = ((EventContract.Message)data_).getEventName();
			Object evt_ = ((EventContract.Message)data_).getEvent();
			
			if (portid_ == EVENT_ID_CHANGED_PORT_ID) {
				if (evtName_ == EVENT_IDENTITY_ADDED)
					idAddedEventHandler(evt_, inPort_);
				else if (evtName_ == EVENT_IDENTITY_REMOVED)
					idRemovedEventHandler(evt_, inPort_);
				else
					processed_ = false;
			}
			else if (portid_.equals(EVENT_RT_UCAST_CHANGED_PORT_ID)
					|| portid_.equals(EVENT_RT_MCAST_CHANGED_PORT_ID)) {
				if (evtName_.equals(EVENT_RT_ENTRY_ADDED))
					rtAddedEventHandler(evt_, inPort_);
				else if (evtName_.equals(EVENT_RT_ENTRY_REMOVED))
					rtRemovedEventHandler(evt_, inPort_);
				else if (evtName_.equals(EVENT_RT_ENTRY_MODIFIED))
					rtModifiedEventHandler(evt_, inPort_);
				else
					processed_ = false;
			}
			else if (portid_.equals(EVENT_IF_PORT_ID)) {
				NeighborEvent.Message s_ = (NeighborEvent.Message)evt_;
				int ifindex_ = s_.getIfIndex();
				NetAddress neighbor_ = s_.getNeighbor();
				if (evtName_.equals(EVENT_IF_NEIGHBOR_UP))
					neighborUpEventHandler(ifindex_, neighbor_, inPort_);
				else if (evtName_.equals(EVENT_IF_NEIGHBOR_DOWN))
					neighborDownEventHandler(ifindex_, neighbor_, inPort_);
				else
					processed_ = false;
			}
			else if (portid_.equals(EVENT_VIF_PORT_ID)) {
				NeighborEvent.Message s_ = (NeighborEvent.Message)evt_;
				int ifindex_ = s_.getIfIndex();
				NetAddress neighbor_ = s_.getNeighbor();
				if (evtName_.equals(EVENT_VIF_NEIGHBOR_UP))
					vNeighborUpEventHandler(ifindex_, neighbor_, inPort_);
				else if (evtName_.equals(EVENT_VIF_NEIGHBOR_DOWN))
					vNeighborDownEventHandler(ifindex_, neighbor_, inPort_);
				else
					processed_ = false;
			}
			///////////////////////////////////////////////////////////////////
			// added by Will
			else if (portid_.equals(EVENT_LINK_BROKEN_PORT_ID)) {
				InetPacket ipkt_ = (InetPacket)evt_;
				if (evtName_.equals(EVENT_LINK_BROKEN))
					LinkBrokenEventHandler(ipkt_, inPort_);
				else
					processed_ = false;
			}
			////////////////////////////////////////////////////////////////////
		}
		if (!processed_) super.processOther(data_, inPort_);
	}
	
	public Protocol()
	{ super(); }
	
	public Protocol(String id_)
	{ super(id_); }
		
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Protocol that_ = (Protocol)source_;
	}
	
	//
	void ___SEND_PACKET___() {}
	//
	
	/////////////////////////////////////////////////////
	// added by Will 06/25/2003
	// specify the next hop 
	/** Broadcast the packet with nexthop specified. */
	public void broadcast(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, long nexthop_)
	{
		downPort.doSending(PktSending.getBcastPack(p_, src_, dest_, 
								routerAlert_, TTL_, ToS_, null, nexthop_));
	}
	////////////////////////////////////////////////////
	
	/** Broadcast the packet. */
	public void broadcast(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_)
	{
		downPort.doSending(PktSending.getBcastPack(p_, src_, dest_, 
								routerAlert_, TTL_, ToS_, null));
	}
	
	/** Broadcast a raw packet body. */
	public void broadcast(Object body_, int bodySize_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_)
	{
		downPort.doSending(PktSending.getBcastPack(body_, bodySize_, src_, 
								dest_, routerAlert_, TTL_, ToS_, null));
	}

	/**
	 Broadcast excluding the specified link.
	 Useful as incoming link is usually excluded.
	 */
	public void broadcast(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, int excludeIf_)
	{
		downPort.doSending(PktSending.getBcastPack(p_, src_, dest_,
								routerAlert_, TTL_, ToS_, excludeIf_));
	}

	/**
	 Broadcast excluding the specified link.
	 Useful as incoming link is usually excluded.
	 */
	public void broadcast(Object body_, int bodySize_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, int excludeIf_)
	{
		downPort.doSending(PktSending.getBcastPack(body_, bodySize_, src_, 
								dest_, routerAlert_, TTL_, ToS_, excludeIf_));
	}

	
	/** Broadcast excluding the specified links. */
	public void broadcast(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_,
					int[] excludeIfs_)
	{
		downPort.doSending(PktSending.getBcastPack(p_, src_, dest_, 
								routerAlert_, TTL_, ToS_, excludeIfs_));
	}
	
	/** Broadcast excluding the specified links. */
	public void broadcast(Object p_, int bodysize_, long src_, long dest_,
					boolean routerAlert_, int TTL_, long ToS_,
					int[] excludeIfs_)
	{
		downPort.doSending(PktSending.getBcastPack(p_, bodysize_, src_, dest_, 
								routerAlert_, TTL_, ToS_, excludeIfs_));
	}
	
	/** Forward packet via specified link. */
	public void forward(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, int if_)
	{
		downPort.doSending(PktSending.getMcastPack(p_, src_, dest_,
								routerAlert_, TTL_, ToS_, new int[]{if_}));
	}
	
	/** Forward packet via specified link. */
	public void forward(Object p_, int bodysize_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, int if_)
	{
		downPort.doSending(PktSending.getMcastPack(p_, bodysize_, src_, dest_,
								routerAlert_, TTL_, ToS_, new int[]{if_}));
	}
	
	/**
	 This method presents the most general case where the packet is forwarded
	 on arbitrarily specified interfaces.
	 */
	public void forward(Packet p_, long src_, long dest_,
					boolean routerAlert_, int TTL_, long ToS_, int[] ifs_)
	{
		downPort.doSending(PktSending.getMcastPack(p_, src_, dest_, 
								routerAlert_, TTL_, ToS_, ifs_));
	}
	
	/**
	 This method presents the most general case where the packet is forwarded
	 on arbitrarily specified interfaces.
	 */
	public void forward(Object p_, int bodysize_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_, int[] ifs_)
	{
		downPort.doSending(PktSending.getMcastPack(p_, bodysize_, src_, dest_,
								routerAlert_, TTL_, ToS_, ifs_));
	}
	
	/** Route lookup forwarding. */
	public void forward(Packet p_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_)
	{
		downPort.doSending(PktSending.getForwardPack(p_, src_, dest_,
								routerAlert_, TTL_, ToS_));
	}
	
	/** Route lookup forwarding. */
	public void forward(Object p_, int bodysize_, long src_, long dest_, 
					boolean routerAlert_, int TTL_, long ToS_)
	{
		downPort.doSending(PktSending.getForwardPack(p_, bodysize_, src_, dest_,
								routerAlert_, TTL_, ToS_));
	}
}
