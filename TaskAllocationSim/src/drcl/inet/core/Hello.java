// @(#)Hello.java   1/2004
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

package drcl.inet.core;

import java.util.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.Protocol;
import drcl.inet.InetPacket;
import drcl.inet.data.*;
import drcl.inet.contract.*;

// XXX: add timeout variations
// XXX: timingPack for configuration purpose
/**
 * Component that resides in a node and exchanges information with neighboring
 * nodes.
 * The information collectd is stored in the data structure defined in 
 * <code>InterfaceInfo</code>.  Other components may query for the neighbor 
 * information by sending <code>null</code> to one of its port; in return,
 * the component sends back array of <code>InterfaceInfo</code>.
 * 
 * The component may run in two modes: dynamic or static.  When in dynamic mode,
 * the neighbor information is maintained with soft-state approach, so that it
 * adapts to topology change.  When topology is fixed, it would be better to
 * run it in static mode where information is exchanged once and the neighbor 
 * information is never timed out.  One may change the mode back and forth 
 * during simulation at any time.
 * 
 * @see IFQuery
 * @see InterfaceInfo
 */
public class Hello extends Protocol
		implements ActiveComponent, InetCoreConstants
{	
	public String getName()
	{ return "hello"; }

	static {
		setContract(Hello.class, SERVICE_IF_PORT_ID + "@",
						new IFQuery(Contract.Role_REACTOR));
	}
	
	Port idport = createIDServicePort();
	Port evtPort = addEventPort(EVENT_IF_PORT_ID);
	
	{
		upPort.setRemovable(true); removePort(upPort); upPort = null;
		addServerPort(SERVICE_IF_PORT_ID);
	}

	/** Based on DVMRPv3 Internet Draft. */
	static double HELLO_REMOVAL_TIMEOUT	= 35.0;
	/** Based on DVMRPv3 Internet Draft. */
	static double HELLO_TIMEOUT			= 10.0;
	/**
	 * During the initialization period, the hello message is broadcast
	 * to all interfaces to discover neighbors.  After that, the hello meesage
	 * is only sent to the interfaces to the known neighbors.
	 */
	static double INIT_PERIOD			= HELLO_TIMEOUT * 3.0 + 1.0;
	final static String HELLO_REMOVAL		= "Hello Removal";
	final static String HELLO				= "Hello";
	
	public static final void setHelloRemovalTimeout(double timeout_)
	{ HELLO_REMOVAL_TIMEOUT = timeout_;	}
	
	public static final double getHelloRemovalTimeout()
	{ return HELLO_REMOVAL_TIMEOUT;	}
	
	public static final double getHelloTimeout()
	{ return HELLO_TIMEOUT;	}
	
	/**
	 * The vector which keeps track of all the available interfaces
	 * of a node.
	 */
	protected InterfaceInfo[] neighbors;  // if -> InterfaceInfo
	protected long myself = Long.MAX_VALUE; // nonnetwork address
	double startingTime;
	
	// set true to stop exchaning info w/ neighbors
	boolean staticMode = false;
	
	public Hello()
	{ super(); }
	
	public Hello(String id_)
	{ super(id_); }
					  
	public void reset()
	{
		super.reset();
		if (neighbors != null) {
			for (int i=0; i<neighbors.length; i++) {
				InterfaceInfo neighbor_ = neighbors[i];
				if (neighbor_ == null) continue;
				double[] timeouts_ = neighbor_.getPeerTimeouts();
				if (timeouts_ != null) {
					NetAddress[] peers_ = neighbor_.getPeerNetAddresses();
					for (int j=timeouts_.length-1; j>=0; j--)
						if (timeouts_[j] >= 0.0)
							neighbor_.removePeerNetAddress(peers_[j]);
				}
			}
		}
	}
	
	/** Clears the neighbor database. */
	public void clear()
	{ neighbors = null; }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Hello that_ = (Hello)source_;
		neighbors = (InterfaceInfo[]) drcl.util.ObjectUtil.clone(
						that_.neighbors, false);
		staticMode = that_.staticMode;
	}
	
	/**
	 * Implements the startup method of {@link drcl.comp.ActiveComponent}.
	 * Specifically, when a Hello starts, it broadcasts its identity to its 
	 * neighbors via all the available interfaces.
	 */
	protected synchronized void _start()
	{
		startingTime = getTime();
		myself = IDLookup.getDefaultID(idport);
		
		// INTERFACE_UP events
		if (neighbors != null)
			for (int i=0; i<neighbors.length; i++)
				if (neighbors[i] != null) {
					if (neighbors[i].local == null)
						neighbors[i].local = new NetAddress(myself, 0);
					NetAddress[] peers_ = neighbors[i].getPeerNetAddresses();
					if (peers_ != null)
						if (evtPort._isEventExportEnabled())
							for (int j=0; j<peers_.length; j++)
								fireEvent(EVENT_IF_NEIGHBOR_UP, i, peers_[j]);
				}
		
		hello();
		
		if (!staticMode) setTimeout(HELLO, HELLO_TIMEOUT);
		if (!staticMode) setTimeout(HELLO_REMOVAL, HELLO_REMOVAL_TIMEOUT);
	}
	
	/** Hookup for {@link Hellov} extension. */
	protected void fireEvent(String evtName_, int ifindex_, 
					NetAddress neighbor_)
	{
		evtPort.exportEvent(evtName_, new NeighborEvent.Message(ifindex_, 
								neighbor_), null);
	}
	
	// say hello to neighbors
	void hello()
	{
		if (neighbors == null) {
			broadcast(new NetAddress(myself, 0), 16, myself, -1/*dont care*/,
						true/*router alert*/, 1/* TTL */, InetPacket.CONTROL);		
		}
		else {
			drcl.data.BitSet bset_ = new drcl.data.BitSet();
			for (int i=0; i<neighbors.length; i++) {
				InterfaceInfo neighbor_ = neighbors[i];
				if (neighbor_ != null) {
					if (neighbor_.local == null)
						neighbor_.local = new NetAddress(myself, 0);
					forward(neighbor_.local.clone(), 16, myself,
						Address.NULL_ADDR/*dont care*/, true/*router alert*/,
						1/* TTL */, InetPacket.CONTROL, i);
					bset_.set(i);
				}
			}
			
			if (getTime() - startingTime <= INIT_PERIOD) {
				// broadcast other interfaces
				broadcast(new NetAddress(myself, 0), 16, myself,
					Address.NULL_ADDR/*dont care*/, true/*router alert*/,
					1/* TTL */, InetPacket.CONTROL, bset_.getSetBitIndices());		
			}
		}
	}

	
	protected synchronized void timeout(Object data_)
	{ 
		if(data_ == HELLO) {
			if (isDebugEnabled()) debug("Hello timeout");
			if (!staticMode) {
				hello();
				setTimeout(HELLO, HELLO_TIMEOUT);
			}
		}
		else {
			// Hello removal
			if (isDebugEnabled()) debug("Hello removal timeout");
			double now_ = getTime();
			if (neighbors != null) {
				for (int i=0; i<neighbors.length; i++) {
					InterfaceInfo neighbor_ = neighbors[i];
					if (neighbor_ == null) continue;
					double[] timeouts_ = neighbor_.getPeerTimeouts();
					if (timeouts_ != null) {
						NetAddress[] peers_ = neighbor_.getPeerNetAddresses();
						for (int j=timeouts_.length-1; j>=0; j--)
							if (timeouts_[j] < now_) {
								neighbor_.removePeerNetAddress(peers_[j]);
								// topology change notification
								if (evtPort._isEventExportEnabled())
									fireEvent(EVENT_IF_NEIGHBOR_DOWN, i, 
												peers_[j]);
							}
					}
				}
			}
			if (!staticMode) setTimeout(HELLO_REMOVAL, HELLO_REMOVAL_TIMEOUT);
		}
	}

	protected synchronized void processOther(Object data_,
					drcl.comp.Port inPort_)
	{
		// query: 
		if (data_ == null) {
			// query all
			inPort_.doLastSending(_queryAll());
		}
		else if (data_ instanceof IntObj) {
			// query specific interface
			InterfaceInfo tmp_ = (InterfaceInfo)drcl.util.ObjectUtil.clone(
							_getInterfaceInfo(((IntObj)data_).value));
			inPort_.doLastSending(tmp_);
		}
		else if (data_ instanceof drcl.data.BitSet) {
			// query set of interfaces
			if (neighbors == null || neighbors.length == 0) {
				inPort_.doLastSending(new InterfaceInfo[0]);
				return;
			}
			InterfaceInfo[] tmp_ = new InterfaceInfo[neighbors.length];
			int[] set_ = ((drcl.data.BitSet)data_).getSetBitIndices();
			for (int i=0; i<set_.length; i++) {
				int index_ = set_[i];
				if (index_ >= neighbors.length) break;
				tmp_[index_] = (InterfaceInfo)drcl.util.ObjectUtil.clone(
								neighbors[index_]);
			}
			inPort_.doLastSending(tmp_);
		}
		else if (data_ instanceof InterfaceInfo[]) {
			// set all
			setInterfaceInfos((InterfaceInfo[])data_);
		}
		else {
			// set specific interface
			try {
				Object[] req_ = (Object[])data_;
				IntObj index_ = (IntObj)req_[0];
				InterfaceInfo if_ = (InterfaceInfo)req_[1];
				_setInterfaceInfo(index_.value, if_);
			}
			catch (Exception e_) {
				error(data_, "processOther()", inPort_, "unrecognized request");
			}
		}
	}
	
	/** Hookup for {@link Hellov} extension.  */
	protected InterfaceInfo[] _queryAll()
	{
		return (InterfaceInfo[])drcl.util.ObjectUtil.clone(neighbors);
	}
	

	protected synchronized void dataArriveAtDownPort(Object data_, 
					Port downPort_) 
	{
		// Hello from neighbor
		if (!(data_ instanceof InetPacket)) {
			error(data_, "dataArriveAtDownPort()", downPort_, 
							"unrecognized data");
			return;
		}
		InetPacket p_ = (InetPacket)data_;

		NetAddress peer_ = (NetAddress)p_.getBody();
		int incoming_ = p_.getIncomingIf();
		if (neighbors == null || neighbors.length <= incoming_) {
			InterfaceInfo[] newneighbors_ = new InterfaceInfo[incoming_ + 1];
			if (neighbors != null) 
				System.arraycopy(neighbors, 0, newneighbors_, 0, 
								neighbors.length);
			neighbors = newneighbors_;
		}
		if (neighbors[incoming_] == null) {
			NetAddress local_ = new NetAddress(myself, 0);
			neighbors[incoming_] = new InterfaceInfo(local_);
			if (mtu > 0) neighbors[incoming_].setMTU(mtu);
		}
			
		InterfaceInfo neighbor_ = neighbors[incoming_];
		if (isDebugEnabled())
			debug(this + " receives hello " + peer_ + " at if " + incoming_);
			
		if (!neighbor_.containsPeer(peer_)) {
			neighbor_.addPeerNetAddress(peer_, 
					staticMode? Double.NaN: getTime() + HELLO_REMOVAL_TIMEOUT);
			// fire events
			if (evtPort._isEventExportEnabled())
				fireEvent(EVENT_IF_NEIGHBOR_UP, incoming_, peer_);
		}
		else if (!staticMode)
			neighbor_.setTimeout(peer_, getTime() + HELLO_REMOVAL_TIMEOUT);
	}
	
	/** Enables/disables the static mode. */
	public synchronized void setStaticEnabled(boolean v_)
	{
		if (v_ != staticMode) {
			staticMode = v_;
			if (staticMode) {
				if (neighbors != null)
					for (int i=0; i<neighbors.length; i++)
						if (neighbors[i] != null) neighbors[i].resetTimeout();
			}
			else {
				// re-initiate refresh process
				if (myself == Long.MAX_VALUE) _start();
				else {
					hello(); 
					setTimeout(HELLO, HELLO_TIMEOUT);
					setTimeout(HELLO_REMOVAL, HELLO_REMOVAL_TIMEOUT);
				}
			}
		}
	}
	
	/** Returns true if the static mode is enabled. */
	public synchronized boolean isStaticEnabled()
	{ return staticMode; }
	
	public synchronized String info()
	{
		StringBuffer sb_ = new StringBuffer("Node "
						+ drcl.inet.InetConfig.Addr.ltos(myself) + ": ");
		if (neighbors == null || neighbors.length == 0)
			sb_.append("No neighbor.\n");
		else {
			boolean hasNeighbor_ = false;
			for (int j=0; j<neighbors.length; j++)
				if (neighbors[j] != null) {
					if (!hasNeighbor_)
						sb_.append("Interface index ----- interface info\n");
					sb_.append(j + " ----- " + neighbors[j] + "\n");
					hasNeighbor_ = true;
				}
			if (!hasNeighbor_) sb_.append("No neighbor.\n");
		}
		return sb_.toString();
	}
	
	//
	private void ___SCRIPT___() {}
	//
	
	/** Sets the information of a specific interface. */
	public void _setInterfaceInfo(int ifindex_, NetAddress local_, 
					NetAddress peer_)
	{ _setInterfaceInfo(ifindex_, new InterfaceInfo(local_, peer_,
							Double.NaN));	}
	
	/** Sets the information of a specific interface. */
	public void _setInterfaceInfo(int ifindex_, InterfaceInfo if_)
	{
		if (neighbors == null || neighbors.length <= ifindex_) {
			InterfaceInfo[] newneighbors_ = new InterfaceInfo[ifindex_ + 3];
			if (neighbors != null) 
				System.arraycopy(neighbors, 0, newneighbors_, 0, 
								neighbors.length);
			neighbors = newneighbors_;
		}
		// XXX: event?
		neighbors[ifindex_] = if_;
		if (mtu > 0 && if_.getMTU() == InterfaceInfo.DEFAULT_MTU)
			if_.setMTU(mtu);
	}
	
	/** Returns the information of a specific interface. */
	public InterfaceInfo _getInterfaceInfo(int ifIndex_)
	{
		if (neighbors == null || ifIndex_ >= neighbors.length)
			return null;
		else
			return neighbors[ifIndex_];
	}
	
	/** Returns the information of all interfaces. */
	public InterfaceInfo[] getInterfaceInfos()
	{	return neighbors; }
	
	/** Sets the interface information for all interfaces. */
	public void setInterfaceInfos(InterfaceInfo[] aa_)
	{	neighbors = aa_; }
	
	int mtu = -1;
				 
	/** Sets the default MTU value. */
	public void _setMTUs(int mtu_)
	{ mtu = mtu_; }
}
