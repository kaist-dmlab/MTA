// @(#)CoreServiceLayer.java   1/2004
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

import java.util.*;
import drcl.comp.*;
import drcl.net.Module;
import drcl.inet.contract.*;
import drcl.inet.data.*;

/**
The container class of the core service layer in the Inet architecture.
The core service layer encapsulates the functionality of the network layer
and below.  It maintains information of the nodes and provides services
through defined service ports for the upper layer protocols to access/manipulate
the information.  In addition, it spontaneously sends out various types
of events through defined event ports.  The details of the information,
the services and the events are described below.

<p><h4>Information</h4>
<dl>
<dt>Identity
<dd>The core service layer maintains the identities (addresses) that the node
	owns.  It does not distinguish unicast and multicast addresses.
<dt>Routing Table
<dd>The core service layer maintains a routing table for forwarding packets.
	The routing table consists of the routing entries
	({@link RTEntry}) and the keys ({@link RTKey})
	to the entries.
<dt>Neighbor Information
<dd>The core service layer maintains the interface-neighbors mapping of this
	node.  The interfaces of this node is indexed from 0 to <em>n</em>-1 where
	<em>n</em> is the number of interfaces.  For each interface, it uses
	{@link InterfaceInfo} to record the information of neighbors
	that can be accessed at that interface.
</dl>

<p><h4>Services</h4>
<table border=1>
<tr>
	<td valign=top nowrap>SERVICE
	<td valign=top nowrap>CONTRACT
	<td valign=top nowrap>PORT
</tr>
<tr>
	<td valign=top nowrap>Packet Sending
	<td valign=top nowrap>{@link PktSending}
	<td valign=top nowrap>ports in the "up" port group
</tr>
<tr>
	<td valign=top nowrap>Packet Delivery
	<td valign=top nowrap>{@link PktDelivery}
	<td valign=top nowrap>ports in the "up" port group
</tr>
<tr>
	<td valign=top nowrap>ID Lookup
	<td valign=top nowrap>{@link IDLookup}
	<td valign=top nowrap><i>.service_id@</i>
</tr>
<tr>
	<td valign=top nowrap>ID Configuration
	<td valign=top nowrap>{@link IDConfig}
	<td valign=top nowrap><i>.service_id@</i>
</tr>
<tr>
	<td valign=top nowrap>Route Lookup
	<td valign=top nowrap>{@link RTLookup}
	<td valign=top nowrap><i>.service_rt@</i>
</tr>
<tr>
	<td valign=top nowrap>Route Configuration
	<td valign=top nowrap>{@link RTConfig}
	<td valign=top nowrap><i>.service_rt@</i>
</tr>
<tr>
	<td valign=top nowrap>Interface/Neighbor Query
	<td valign=top nowrap>{@link IFQuery}
	<td valign=top nowrap><i>.service_if@</i>
</tr>
<tr>
	<td valign=top nowrap>PacketFilter Configuration
	<td valign=top nowrap>{@link ConfigSwitch}
	<td valign=top nowrap><i>.service_configswitch@</i>
</tr>
<tr>
	<td valign=top nowrap>Multicast Service ( = ID Configuration)
	<td valign=top nowrap>{@link IDConfig}
	<td valign=top nowrap><i>.service_mcast@</i>
</tr>
<tr>
	<td valign=top nowrap>Trace Route
	<td valign=top nowrap>{@link TraceRTPkt}
	<td valign=top nowrap>1000@up
</tr>
</table>

<p><h4>Events</h4>
<table border=1>
<tr>
	<td>EVENT NAME
	<td>EVENT OBJECT
	<td>PORT
	<td>DESCRIPTION
</tr>
<tr>
	<td valign=top nowrap>Packet Arrival
	<td valign=top>The arriving packet ({@link drcl.net.Packet})
	<td valign=top nowrap><i>.pktarrival@</i>
	<td valign=top>Exported for every arriving packet no matter it is from local or network.
</tr>
<tr>
	<td valign=top nowrap>Identity Added
	<td valign=top>The identities being added ({@link drcl.data.LongObj}/<code>long[]</code>)
	<td valign=top nowrap><i>.id@</i>
	<td valign=top>Exported when a node ID (address)/IDs is/are added.
</tr>
<tr>
	<td valign=top nowrap>Identity Removed
	<td valign=top>The identities being removed ({@link drcl.data.LongObj}/<code>long[]</code>)
	<td valign=top nowrap><i>.id@</i>
	<td valign=top>Exported when a node ID (address)/IDs is/are removed.
</tr>
<tr>
	<td valign=top nowrap>Route Entry Added
	<td valign=top>The entry being added ({@link RTEntry})
	<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
	<td valign=top>Exported when a routing entry is added.
</tr>
<tr>
	<td valign=top nowrap>Route Entry Removed
	<td valign=top>The entry being removed ({@link RTEntry})
	<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
	<td valign=top>Exported when a routing entry is removed.
</tr>
<tr>
	<td valign=top nowrap>Route Entry Modified
	<td valign=top>A two-element array that consists of the old and the new entries
		({@link RTEntry})
	<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
	<td valign=top>Exported when a routing entry is modified.
</tr>
<tr>
	<td valign=top nowrap>Neighbor Up
	<td valign=top>Information of the neighbor ({@link NetAddress})
	<td valign=top nowrap><i><i>.if@</i><br><i>.vif@</i>
	<td valign=top>Exported when a neighbor is discovered at an interface.
</tr>
<tr>
	<td valign=top nowrap>Neighbor Down
	<td valign=top>Information of the neighbor ({@link NetAddress})
	<td valign=top nowrap><i>.if@</i><br><i>.vif@</i>
	<td valign=top>Exported when a neighbor is lost at an interface.
</tr>
<tr>
	<td valign=top nowrap>Multicast Host
	<td valign=top>The multicast host event
		({@link drcl.inet.contract.McastHostEvent.Message})
	<td valign=top nowrap><i>.mcastHost@</i>
	<td valign=top>Exported when the first member joins or last member leaves a multicast group
		at an interface
</tr>
</table>

<p>This class also provides a set of methods to directly access/manipulate
the information at a script environment.

@author Hung-ying Tyan
@version 1.0, 04/2001
@see CSLBuilder
 */
public abstract class CoreServiceLayer extends drcl.comp.Component 
	implements InetConstants
{
	public abstract boolean hasRoutingCapability();
	
	public CoreServiceLayer()
	{ super(); }
	
	public CoreServiceLayer(String id_)
	{ super(id_); }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer("Node: "
			+ getDefaultAddress() + "\n");
		InterfaceInfo[] aa_ = getInterfaceInfos();
		if (aa_ != null) {
			boolean first_ = true;
			for (int j=0; j<aa_.length; j++)
				if (aa_[j] != null) {
					if (first_)
						sb_.append("Interface index ----- interface info\n");
					sb_.append(j + " ----- "
							+ aa_[j].print(InetConfig.Addr) + "\n");
					first_ = false;
				}
			if (first_) sb_.append("No interface information.\n");
		}
		return sb_.toString();
	}
	
	/**
	 * Adds an identity to this core service layer.
	 */
	public abstract void addAddress(long addr_);
	
	/**
	 * Returns the default identity in this core service layer.
	 */
	public abstract long getDefaultAddress();

	/**
	 * Removes an identity from this core service layer.
	 */
	public abstract void removeAddress(long addr_);

	/**
	 * Sets the interface information at the specified interface.
	 */
	public abstract void setInterfaceInfo(int ifindex_, InterfaceInfo if_);
	
	/**
	 * Sets the interface information of all the interfaces.
	 */
	public abstract void setInterfaceInfos(InterfaceInfo[] aa_);
	
	/**
	 * Returns the interface information of the specified interface.
	 */
	public abstract InterfaceInfo getInterfaceInfo(int ifIndex_);
	
	/**
	 * Returns all the interface information in an array.
	 */
	public abstract InterfaceInfo[] getInterfaceInfos();
	
	/**
	 * Sets the bandwidth of the specified interface.
	 */
	public abstract void setBandwidth(int ifindex_, double bw_);
	
	/**
	 * Returns the bandwidth of the specified interface.
	 */
	public abstract double getBandwidth(int ifIndex_);
	
	/**
	 * Sets the bandwidth of all the interfaces.
	 */
	public abstract void setBandwidth(double bw_);
	
	/**
	 * Sets the buffer size of all the interfaces.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 */
	public abstract void setBufferSize(int bs_);
	
	/**
	 * Sets the buffer size of all the interfaces.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 * @param mode_ can be either "packet" or "byte"
	 */
	public abstract void setBuffer(int bs_, String mode_);
	
	/**
	 * Sets the buffer size of the specified interface.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 * @param mode_ can be either "packet" or "byte"
	 */
	public abstract void setBuffer(int ifindex_, int bs_, String mode_);
	
	/**
	 * Sets the buffer size of the specified interface.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 */
	public abstract void setBufferSize(int ifindex_, int bs_);

	/**
	 * Sets the buffer mode of all the interfaces.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public abstract void setBufferMode(String mode_);
	
	/**
	 * Sets the buffer mode of the specified interface.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public abstract void setBufferMode(int ifindex_, String mode_);

	/**
	 * Sets the MTU of the specified interface.
	 */
	public abstract void setMTU(int ifindex_, int mtu_);
	
	/**
	 * Sets the MTUs of all interfaces.
	 */
	public abstract void setMTUs(int mtu_);
	
	public abstract String getBufferMode(int ifIndex_);
	
	/**
	 * Returns the buffer size of the specified interface.
	 * @return the buffer size in bytes or packets depending on the mode.
	 * @see #getBufferMode(int)
	 */
	public abstract int getBufferSize(int ifIndex_);
	
	/**
	 * Returns the MTU of the specified interface.
	 */
	public abstract int getMTU(int ifIndex_);

	/** Sets the emulated link propagation delay.  Used with link emulation 
	 * enabled at that interface. */
	public abstract void setLinkPropDelay(int ifIndex_, double delay_);
	
	/** Returns the emulated link propagation delay.  Used with link emulation 
	 * enabled at that interface. */
	public abstract double getLinkPropDelay(int ifIndex_);
	
	public abstract void setLinkEmulationEnabled(int ifIndex_,
					boolean enabled_);
	public abstract boolean isLinkEmulationEnabled(int ifIndex_);
	
	/**
	 * Returns the number of interfaces including virtual ones.
	 */
	public abstract int getNumOfInterfaces();
	
	/**
	 * Returns the number of physical interfaces
	 */
	public abstract int getNumOfPhysicalInterfaces();
	
	/**
	 * Adds a routing entry to this core service layer.
	 */
	public abstract void addRTEntry(RTKey key_, RTEntry entry_,
					double timeout_);
	
	/**
	 * Removes a routing entry from this core service layer.
	 */
	public abstract void removeRTEntry(RTKey key_);
	
	/**
	 * Retrieves an RT entry/RT entries from this core service layer.
	 * @return <code>RTEntry[]</code> if <code>matchMethod_</code> is
	 * 		<code>MATCH_ALL</code>
	 *		or <code>MATCH_WILDCARD</code>.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 * @see drcl.data.Map 
	 */
	public abstract Object retrieveRTEntry(RTKey key_, String matchMethod_);

	/**
	 * Retrieves all RT entries from this core service layer.
	 * @return all RT entries in <code>RTEntry[]</code>.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 */
	public abstract RTEntry[] retrieveAllRTEntries();

	public abstract void setupVIF(int vifindex_, long dest_, int mtu_);
	public abstract void setupVIF(int vifindex_, long src_, long dest_,
					int mtu_);
	
	/**
	 * Subclasses must implement this to adapt to ports being added at run-time.
	 */
	protected abstract void portAdded(Port mine_);

	/** Returns the ports that output packets that are to be enqueued in
		and dequeued/dropped from the buffer. 
		Returned is a 2-dimensional port array.  The first dimension is
		outgoing interface.
		The second dimension consists of three ports: enqueue-event port,
		dequeue-event port, and drop-event port. */
	public abstract Port[][] getNAMPacketEventPorts();
}
