// @(#)Node.java   1/2004
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

import drcl.comp.*;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.net.Address;
import drcl.net.Module;

/**
The container class for a network node.
<p>In the INET framework, a node consists of the
{@link CoreServiceLayer core service layer} (CSL) and the "upper" protocol layer (UPL).
UPL contains transport protocols, applications and other modules.
The structure in this layer is not defined in the framework, so components in this layer
may form an arbitrary graph.  CSL provides a set of well-defined services (that are common
in most network architectures) to modules in the UPL.
In some sense, CSL encapsulates the functionality of all the network layer, the link layer
and the physical layer.
 */
public class Node extends Component implements InetConstants
{
	//{ useLocalForkManager(); }
	
	public Node()
	{ super(); }
	
	public Node(String id_)
	{ super(id_); }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		Component csl_ = getComponent(ID_CSL);
		if (csl_ != null)
			sb_.append(csl_.info());
		return sb_.toString();
	}
	
	// node address
	long addr = drcl.net.Address.NULL_ADDR;
	
	public static Node create(long nodeAddr_)
	{
		Node n_ = new Node();
		n_.addr = nodeAddr_;
		return n_;
	}

	public CoreServiceLayer getCSL()
	{ return (CoreServiceLayer)getComponent(ID_CSL); }
	
	public boolean hasRoutingCapability()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).hasRoutingCapability();
		else
			return false;
	}
	
	/**
	 * Returns the default network address of this node.
	 */
	public long getDefaultAddress()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getDefaultAddress();
		return addr;
	}
	
	/**
	 * Adds a network address to this node.
	 * The first address added is set to be the default.
	 */
	public void addAddress(long addr_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).addAddress(addr_);
		else addr = addr_;
	}
	
	/**
	 * Removes a network address from this node.
	 */
	public void removeAddress(long addr_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).removeAddress(addr_);
	}

	/**
	 * Sets the bandwidth of the <em>ifindex_</em>th interface.
	 */
	public void setBandwidth(int ifindex_, double bw_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBandwidth(ifindex_, bw_);
	}
	
	/**
	 * Sets the bandwidth of all the interfaces.
	 */
	public void setBandwidth(double bw_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBandwidth(bw_);
	}
	
	/**
	 * Returns the bandwidth of the <em>ifindex_</em>th interface.
	 */
	public double getBandwidth(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getBandwidth(ifIndex_);
		return Double.NaN;
	}
	
	/**
	 * Sets the buffer size of the <em>ifindex_</em>th interface.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 */
	public void setBufferSize(int ifindex_, int bs_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBufferSize(ifindex_, bs_);
	}
	
	/**
	 * Sets the buffer size of all the interfaces.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 */
	public void setBufferSize(int bs_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBufferSize(bs_);
	}
	
	/**
	 * Sets the buffer size of all the interfaces.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBuffer(int bs_, String mode_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBuffer(bs_, mode_);
	}
	
	/**
	 * Sets the buffer size of the <em>ifindex_</em>th interface.
	 * @param bs_ buffer size in bytes or packets depending on the mode.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBuffer(int ifindex_, int bs_, String mode_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBuffer(ifindex_, bs_, mode_);
	}
	
	/**
	 * Sets the buffer mode of all the interfaces.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBufferMode(String mode_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBufferMode(mode_);
	}
	
	/**
	 * Sets the buffer mode of the specified interface.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBufferMode(int ifIndex_, String mode_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setBufferMode(ifIndex_, mode_);
	}
	
	/**
	 * Returns the buffer size of the <em>ifindex_</em>th interface.
	 * @return the buffer size in bytes or packets depending on the mode.
	 * @see #getBufferMode(int)
	 */
	public int getBufferSize(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getBufferSize(ifIndex_);
		return -1;
	}

	/**
	 * Returns the buffer operation mode.
	 * @return either "packet" or "byte".
	 */
	public String getBufferMode(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getBufferMode(ifIndex_);
		return "N/A";
	}
	
	/**
	 * Sets the MTU of the specified interface.
	 */
	public void setMTU(int ifindex_, int mtu_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setMTU(ifindex_, mtu_);
	}
	
	/**
	 * Sets the MTU of all the interfaces.
	 */
	public void setMTUs(int mtu_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setMTUs(mtu_);
	}
	
	/**
	/**
	 * Returns the MTU of the specified interface.
	 */
	public int getMTU(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getMTU(ifIndex_);
		return -1;
	}
	
	/**
	 * Sets the interface information of the <em>ifindex_</em>th interface.
	 * @see drcl.inet.data.InterfaceInfo
	 */
	public void setInterfaceInfo(int ifindex_, InterfaceInfo if_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setInterfaceInfo(ifindex_, if_);
	}
	
	/**
	 * Returns the interface information of the <em>ifindex_</em>th interface.
	 * @see drcl.inet.data.InterfaceInfo
	 */
	public InterfaceInfo getInterfaceInfo(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getInterfaceInfo(ifIndex_);
		return null;
	}
	
	/**
	 * Returns the interface information of all the interfaces.
	 * @see drcl.inet.data.InterfaceInfo
	 */
	public InterfaceInfo[] getInterfaceInfos()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getInterfaceInfos();
		return null;
	}
	
	/**
	 * Sets the interface information of all the interfaces.
	 * @see drcl.inet.data.InterfaceInfo
	 */
	public void setInterfaceInfos(InterfaceInfo[] aa_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setInterfaceInfos(aa_);
	}
	
	/**
	 * Returns the number of interfaces including virtual ones.
	 */
	public int getNumOfInterfaces()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getNumOfInterfaces();
		return -1;
	}
	
	/**
	 * Returns the number of physical interfaces
	 */
	public int getNumOfPhysicalInterfaces()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getNumOfPhysicalInterfaces();
		return -1;
	}

	final static RTKey DEFAULT_ROUTE_KEY = new RTKey(0, 0, 0, 0, 0, 0);
	final static drcl.data.BitSet DEFAULT_ROUTE_BS = new drcl.data.BitSet(1);
	static {
		DEFAULT_ROUTE_BS.set(0);
	}
	final static RTEntry DEFAULT_ROUTE_ENTRY = new RTEntry(DEFAULT_ROUTE_BS);

	/**
	 * Adds a default route entry to the routing table of this node. 
	 * @param if_ interface index of the default route. 
	 */
	public void addDefaultRoute(int if_)
	{
		if (if_ == 0)
			addRTEntry(DEFAULT_ROUTE_KEY, DEFAULT_ROUTE_ENTRY, -1.0);
		else {
			drcl.data.BitSet bs_ = new drcl.data.BitSet();
			bs_.set(if_);
			RTEntry entry_ = new RTEntry(bs_);
			addRTEntry(DEFAULT_ROUTE_KEY, entry_, -1.0);
		}
	}

	/**
	 * Adds an RT entry to this node.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 */
	public void addRTEntry(RTKey key_, RTEntry entry_, double timeout_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).addRTEntry(key_, entry_, timeout_);
	}
	
	/**
	 * Removes an RT entry from this node.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 */
	public void removeRTEntry(RTKey key_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).removeRTEntry(key_);
	}
	
	/**
	 * Retrieves an RT entry/RT entries from this node.
	 * @return <code>RTEntry[]</code> if <code>matchMethod_</code> is
	 * 		<code>MATCH_ALL</code> or <code>MATCH_WILDCARD</code>.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 * @see drcl.data.Map 
	 */
	public Object retrieveRTEntry(RTKey key_, String matchMethod_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).retrieveRTEntry(key_, matchMethod_);
		else
			return null;
	}
	
	/**
	 * Retrieves all RT entries from this node.
	 * @return all RT entries in <code>RTEntry[]</code>.
	 * @see drcl.inet.data.RTKey
	 * @see drcl.inet.data.RTEntry
	 */
	public RTEntry[] retrieveAllRTEntries()
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).retrieveAllRTEntries();
		else
			return null;
	}

	/** Sets the emulated link propagation delay.  Used with link emulation 
	 * enabled at that interface. */
	public void setLinkPropDelay(int ifIndex_, double delay_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setLinkPropDelay(ifIndex_, delay_);
	}
	
	/** Returns the emulated link propagation delay.  Used with link emulation 
	 * enabled at that interface. */
	public double getLinkPropDelay(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).getLinkPropDelay(ifIndex_);
		else
			return Double.NaN;
	}
	
	public void setLinkEmulationEnabled(int ifIndex_, boolean enabled_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			((CoreServiceLayer)csl_).setLinkEmulationEnabled(ifIndex_, enabled_);
	}

	public boolean isLinkEmulationEnabled(int ifIndex_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ instanceof CoreServiceLayer)
			return ((CoreServiceLayer)csl_).isLinkEmulationEnabled(ifIndex_);
		else
			return false;
	}
	
	/** Returns the result of trace route in String. */
	public String traceRoute(long destAddress_)
	{
		Object[] oo = traceRouteInObj(destAddress_);
		if (oo == null || oo.length == 0)
			return "Error: destinatioin not reachable";

		StringBuffer sb = new StringBuffer();
		for (int i=1; i<=oo.length; i++)
			sb.append(i + ": " + oo[i-1] + "\n");
		return sb.toString();
	}

	/** Returns the result of trace route in an array.
	 * Every two elements in the array represent time (Double) and address
	 * (Long) for the corresponding hop along the route.
	 * Returns null if destination is not reachable. */
	public Object[] traceRouteInObj(long destAddress_)
	{
		Component csl_ = getComponent(ID_CSL);
		if (csl_ == null) return null;

		Component c = getComponent(ID_TRACE_RT);
		TraceRT tracert_ = null;
		if (c instanceof TraceRT)
			tracert_ = (TraceRT)c;
		else {
			tracert_ = new TraceRT(ID_TRACE_RT);
			addComponent(tracert_);
		}
		Port p = csl_.addPort("up", String.valueOf(InetConstants.PID_TRACE_RT));
		tracert_.downPort.connect(p);
		return tracert_.traceRoute(destAddress_);
	}
	
	/**
	 * Notified when a component is added.
	 */
	protected void componentAdded(Component child_)
	{
		if (child_ instanceof Protocol) {
			Protocol pr_ = (Protocol)child_;
			CoreServiceLayer csl_ = (CoreServiceLayer)getComponent(ID_CSL);
			if (csl_ != null) _addProtocol(csl_, pr_);
		}
	}
	
	/**
	 * Notified when a port is added.
	 */
	protected void portAdded(Port mine_)
	{
		if (!mine_.getGroupID().equals(Component.PortGroup_DEFAULT_GROUP)) return;
		try {
			int ifindex_ = Integer.parseInt(mine_.getID());
			CoreServiceLayer csl_ = (CoreServiceLayer)getComponent(ID_CSL);
			if (csl_ == null) return;
			Port p_ = csl_.addPort(Module.PortGroup_DOWN, mine_.getID());
			p_.connectTo(mine_);
			mine_.connectTo(p_);
		}
		catch (NumberFormatException e_) {
		}
	}
	
	void _addProtocol(Component csl_, Component pr_)
	{
		if (pr_ instanceof Module && ((Module)pr_).downPort != null) {
			int pid_ = InetUtil.getPID(pr_);
			if (pid_ >= 0)
				((Module)pr_).downPort.connect(csl_.addPort(Module.PortGroup_UP, String.valueOf(pid_)));
		}
		Port p_ = pr_.getPort(SERVICE_ID_PORT_ID);
		if (p_ != null) p_.connect(csl_.addPort(new Port(Port.PortType_SERVER), PortGroup_SERVICE, SERVICE_ID_PORT_ID));
		p_ = pr_.getPort(SERVICE_RT_PORT_ID);
		if (p_ != null) p_.connect(csl_.addPort(new Port(Port.PortType_SERVER), PortGroup_SERVICE, SERVICE_RT_PORT_ID));
		p_ = pr_.getPort(SERVICE_IF_PORT_ID);
		if (p_ != null) p_.connect(csl_.addPort(new Port(Port.PortType_SERVER), PortGroup_SERVICE, SERVICE_IF_PORT_ID));
		p_ = pr_.getPort(SERVICE_CONFIGSW_PORT_ID);
		if (p_ != null) p_.connect(csl_.addPort(new Port(Port.PortType_SERVER), PortGroup_SERVICE, SERVICE_CONFIGSW_PORT_ID));
		
		p_ = pr_.getPort(UCAST_QUERY_PORT_ID);
		if (p_ != null) p_.connect(csl_.addPort(PortGroup_EVENT, UCAST_QUERY_PORT_ID));
		else {
			p_ = pr_.getPort(MCAST_QUERY_PORT_ID);
			if (p_ != null) p_.connect(csl_.addPort(PortGroup_EVENT, MCAST_QUERY_PORT_ID));
			/*
			else {
				p_ = pr_.getPort(DUAL_HELP_PORT_ID);
				if (p_ != null) {
					p_.connect(csl_.addPort(PortGroup_EVENT, UCAST_QUERY_PORT_ID));
					p_.connect(csl_.addPort(PortGroup_EVENT, MCAST_QUERY_PORT_ID));
				}
			}
			*/
		}
		p_ = pr_.getPort(EVENT_PKT_ARRIVAL_PORT_ID);
		if (p_ != null) csl_.addPort(PortGroup_EVENT, EVENT_PKT_ARRIVAL_PORT_ID).connectTo(p_);
		p_ = pr_.getPort(EVENT_ID_CHANGED_PORT_ID);
		if (p_ != null) csl_.addPort(PortGroup_EVENT, EVENT_ID_CHANGED_PORT_ID).connectTo(p_);
		p_ = pr_.getPort(EVENT_IF_PORT_ID);
		if (p_ != null) csl_.addPort(PortGroup_EVENT, EVENT_IF_PORT_ID).connectTo(p_);
		p_ = pr_.getPort(EVENT_VIF_PORT_ID);
		if (p_ != null) csl_.addPort(PortGroup_EVENT, EVENT_VIF_PORT_ID).connectTo(p_);
	}

	public void add(Component c_, String llpid_, int port_)
	{
		Component llp_ = getComponent(llpid_);
		if (llp_ == null) {
			drcl.Debug.error("Lower layer protocol '" + llpid_ + "' does not exist.\n");
			return;
		}
		Component csl_ = getComponent(ID_CSL);
		if (csl_ != null) _addProtocol(csl_, c_);
		if (port_ >= 0)
			c_.addPort(Module.PortGroup_DOWN).connect(llp_.addPort(Module.PortGroup_UP, String.valueOf(port_)));
		else
			c_.addPort(Module.PortGroup_DOWN).connect(llp_.addPort(Module.PortGroup_UP));
	}

	/** Propagates the address scheme through this node.
	public void propagates(Address addr_)
	{
		InetUtil.propagates(addr_, getAllComponents());
	}
	*/
}
