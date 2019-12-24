// @(#)InetConstants.java   1/2004
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

/**
A collection of constants that are used in the INET framework, especially defining port IDs
for auto-build of nodes.
 
@author Hung-ying Tyan
@version 1.0, 10/17/2000
*/
public interface InetConstants
{
	// IDs of components
	public static final String ID_CSL = "csl"; // CoreServiceLayer
	public static final String ID_TRACE_RT = "trace_rt"; // for trace_rt

	// IDs of service/event ports
	/**
	 * The port ID of the unicast query for both the initiating and reacting 
	 * ports of the {@link drcl.inet.contract.IDLookup IDLookup} contract.
	 */
	public static final String UCAST_QUERY_PORT_ID = ".ucastquery";
	/** The port ID of the multicast query for both the initiating and reacting
	 * ports of the {@link drcl.inet.contract.IDLookup IDLookup} contract.
	 */
	public static final String MCAST_QUERY_PORT_ID = ".mcastquery";
	/** The port ID of the packet arrival event for both the exporting and 
	 * receiving ports */
	public static final String EVENT_PKT_ARRIVAL_PORT_ID = ".pktarrival";
	/** The port ID of the identity changed event for both the exporting and 
	 * receiving ports */
	public static final String EVENT_ID_CHANGED_PORT_ID = ".id";
	/** The port ID of the unicast routing entry changed event for both the 
	 * exporting and receiving ports */
	public static final String EVENT_RT_UCAST_CHANGED_PORT_ID = ".rt_ucast";
	/** The port ID of the multicast routing entry changed event for both the 
	 * exporting and receiving ports */
	public static final String EVENT_RT_MCAST_CHANGED_PORT_ID = ".rt_mcast";
	/** The port ID of the interface/neighbor event for both the exporting and 
	 * receiving ports */
	public static final String EVENT_IF_PORT_ID = ".if";
	/** The port ID of the virtual interface/neighbor event for both the 
	 * exporting and receiving ports */
	public static final String EVENT_VIF_PORT_ID = ".vif";
	/** The port ID of the "link broken" event for both the exporting and 
	 * receiving ports */
    public static final String EVENT_LINK_BROKEN_PORT_ID  = ".linkbroken";
	
	// event types
	/** The event type of the packet arrival event. */
	public static final String EVENT_PKT_ARRIVAL = "Packt Arrival";
	/** The event type of the identity added event. */
    public static final String EVENT_IDENTITY_ADDED = "Identity Added";
	/** The event type of the identity removed event. */
    public static final String EVENT_IDENTITY_REMOVED = "Identity Removed";
	/** The event type of the routing entry added event. */
    public static final String EVENT_RT_ENTRY_ADDED = "RT Entry Added";
	/** The event type of the routing entry removed event. */
    public static final String EVENT_RT_ENTRY_REMOVED = "RT Entry Removed";
	/** The event type of the routing entry modified event. */
	public static final String EVENT_RT_ENTRY_MODIFIED = "RT Entry Modified";
	/** The event type of the neighbor-up (a neighbor is discovered) event. */
	public static final String EVENT_IF_NEIGHBOR_UP = "Neighbor Up";
	/** The event type of the neighbor-down (a neighbor is lost) event. */
	public static final String EVENT_IF_NEIGHBOR_DOWN = "Neighbor Down";
	/** The event type of the (virtual) neighbor-up (a neighbor is discovered) 
	 * event. */
	public static final String EVENT_VIF_NEIGHBOR_UP = "VNeighbor Up";
	/** The event type of the (virtual) neighbor-down (a neighbor is lost) 
	 * event. */
	public static final String EVENT_VIF_NEIGHBOR_DOWN = "VNeighbor Down";
	/** The event type of a link broken event. */
    public static final String EVENT_LINK_BROKEN = "Link Broken";
	
	public static final String EVENT_MCAST_HOST_PORT_ID = ".mcastHost";
	public static final String SERVICE_MCAST_PORT_ID = ".service_mcast";

	/** The port ID of the {#link drcl.inet.contract.IDLookup} and 
	 * {#link drcl.inet.contract.IDConfig} services. */
	public static final String SERVICE_ID_PORT_ID = ".service_id";
	
	/** The port ID of the {#link drcl.inet.contract.RTLookup} and 
	 * {#link drcl.inet.contract.RTConfig} services. */
	public static final String SERVICE_RT_PORT_ID = ".service_rt";
	
	/** The port ID of the {#link drcl.inet.contract.IFQuery} services.	 */
	public static final String SERVICE_IF_PORT_ID = ".service_if";
	
	/** The port ID of the {#link drcl.inet.contract.ConfigSwitch} services. */
	public static final String SERVICE_CONFIGSW_PORT_ID =
			".service_configswitch";

	/** The RT extension of an "host" entry. */
	public static final String HOST_ENTRY_EXT = "-toHostSubnet-";
	
	/** Buffer operation mode */
	public static final String BYTE_MODE = "byte";
	
	/** Buffer operation mode */
	public static final String PACKET_MODE = "packet";
	
	public static final double DEFAULT_BANDWIDTH = 1.5e6; // 1.5Mbps
	public static final int DEFAULT_BUFFER_SIZE = Integer.MAX_VALUE;
	public static final int DEFAULT_MTU = Integer.MAX_VALUE;

	public static final int PID_IGMP = 2;
	public static final int PID_HELLO = 3;
	public static final int PID_TCP = 6;
	public static final int PID_UDP = 17;
	public static final int PID_RSVP = 46;
	public static final int PID_OSPF = 89;
	public static final int PID_AODV = 103;
	public static final int PID_DV = 520;
	public static final int PID_DVMRP = 521;
	public static final int PID_TRACE_RT = 1000;
}
