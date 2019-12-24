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

package drcl.inet.core;

import java.util.Arrays;
import java.util.Comparator;
import drcl.comp.*;
import drcl.net.Module;
import drcl.inet.data.*;

/**
 * The container class for constructing a network node.
 */
public class CoreServiceLayer extends drcl.inet.CoreServiceLayer
{
	public CoreServiceLayer()
	{ super(); }
	
	public CoreServiceLayer(String id_)
	{ super(id_); }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer(super.info());
		Identity id_ = (Identity)getComponent(CSLBuilder.ID_IDENTITY);
		if (id_ != null) sb_.append("Identity: " + id_.info("   "));
		else sb_.append("No identity database.\n");
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ != null) sb_.append("Routing Table: " + rt_.info("   "));
		else sb_.append("No routing capability.\n");

		// Note: the interface IDs may not start from 0 and may not
		// be consecutive numbers
		Port[] pp_ = getAllPorts(Module.PortGroup_DOWN);
		if (pp_ != null) {
			if (pp_.length > 0) sb_.append("Interfaces:\n");

			int[][] ifids_ = new int[pp_.length][2];
			String[] ifinfo_ = new String[pp_.length];

			for (int i=0; i<pp_.length; i++) {
				String ifid_ = pp_[i].getID();
				ifids_[i][0] = i; // remember this for sorting later
				try {
					ifids_[i][1] = Integer.parseInt(ifid_);
				}
				catch (NumberFormatException e) {
					ifids_[i][1] = -1;
					continue; // ifinfo_[i] is null
				}
				StringBuffer ifsb_ = new StringBuffer("   " + ifid_ + ": ");
				StringBuffer tmp_ = new StringBuffer();
				Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifid_);
				if (q_ != null) {
					tmp_.append("BufferMode=" + q_.getMode() + ", BufferSize=" + q_.getCapacity());
				}
				NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifid_);
				if (ni_ != null) {
					if (q_ != null) tmp_.append(", ");
					else {
						QueueNI qni_ = (QueueNI)ni_;
						tmp_.append("BufferMode=" + qni_.getMode() + ", BufferSize=" + qni_.getCapacity());
					}
					tmp_.append(", Bandwidth=" + ni_.getBandwidth() + ", MTU=" + ni_.getMTU()
						+ ", LinkEmu=" + ni_.isLinkEmulationEnabled());
					if (ni_.isLinkEmulationEnabled())
						tmp_.append(", PropDelay=" + ni_.getPropDelay());
				}
				if (tmp_.length() == 0) ifsb_.append("no info available.\n");
				else ifsb_.append(tmp_ + "\n");
				ifinfo_[i] = ifsb_.toString();
			}

			// sort out interfaces
			Arrays.sort(ifids_, new Comparator() {
				public int compare(Object o1, Object o2) {
					int v1 = ((int[])o1)[1];
					int v2 = ((int[])o2)[1];
					return v1 > v2? 1: (v1 == v2? 0: -1);
				}
			});
			for (int i=0; i<ifids_.length; i++) {
				if (ifids_[i][1] < 0) continue;
				sb_.append(ifinfo_[ifids_[i][0]]);
			}
		}
		else sb_.append("No interfaces.\n");
		return sb_.toString();
	}
	
	public boolean hasRoutingCapability()
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(drcl.inet.core.CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) return false;
		return pd_._hasRoutingCapability();
	}
	
	public void addAddress(long addr_)
	{
		Identity id_ = (Identity)getComponent(CSLBuilder.ID_IDENTITY);
		id_.add(addr_);
	}
	public long getDefaultAddress()
	{
		Identity id_ = (Identity)getComponent(CSLBuilder.ID_IDENTITY);
		return id_.getDefaultID();
	}

	public void removeAddress(long addr_)
	{
		Identity id_ = (Identity)getComponent(CSLBuilder.ID_IDENTITY);
		id_.remove(addr_);
	}

	public void setBandwidth(int ifindex_, double bw_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		ni_.setBandwidth(bw_);
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) {
			InterfaceInfo ifinfo_ = getInterfaceInfo(ifindex_);
			if (ifinfo_ != null) ifinfo_.setBandwidth(bw_);
			else setInterfaceInfo(ifindex_, new InterfaceInfo(-1, bw_, -1));
		}
	}
	
	public void setBandwidth(double bw_)
	{
		Port[] pp_ = getAllPorts(Module.PortGroup_DOWN);
		for (int i=0; i<pp_.length; i++) {
			int ifindex_ = Integer.parseInt(pp_[i].getID());
			setBandwidth(ifindex_, bw_);
		}
	}
	
	
	public double getBandwidth(int ifindex_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		return ni_.getBandwidth();
	}
	
	public void setBufferSize(int bs_)
	{
		Port[] pp_ = getAllPorts(Module.PortGroup_DOWN);
		for (int i=0; i<pp_.length; i++) {
			int ifindex_ = Integer.parseInt(pp_[i].getID());
			setBufferSize(ifindex_, bs_);
		}
	}
	
	public void setBuffer(int bs_, String mode_)
	{
		Port[] pp_ = getAllPorts(Module.PortGroup_DOWN);
		for (int i=0; i<pp_.length; i++) {
			int ifindex_ = Integer.parseInt(pp_[i].getID());
			setBuffer(ifindex_, bs_, mode_);
		}
	}
	
	public void setBufferSize(int ifindex_, int bs_)
	{
		Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifindex_);
		if (q_ != null)
			q_.setCapacity(bs_);
		else {
			QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + ifindex_);
			qni_.setCapacity(bs_);
		}
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) {
			InterfaceInfo ifinfo_ = getInterfaceInfo(ifindex_);
			if (ifinfo_ != null) ifinfo_.setBufferSize(bs_);
			else setInterfaceInfo(ifindex_, new InterfaceInfo(-1, -1.0, bs_));
		}
	}
	
	public void setBuffer(int ifindex_, int bs_, String mode_)
	{
		Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifindex_);
		if (q_ != null) {
			q_.setMode(mode_);
			q_.setCapacity(bs_);
		}
		else {
			QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + ifindex_);
			qni_.setMode(mode_);
			qni_.setCapacity(bs_);
		}
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) {
			InterfaceInfo ifinfo_ = getInterfaceInfo(ifindex_);
			if (ifinfo_ != null) ifinfo_.setBufferSize(bs_);
			else setInterfaceInfo(ifindex_, new InterfaceInfo(-1, -1.0, bs_));
		}
	}
	
	public void setBufferMode(String mode_)
	{
		Port[] pp_ = getAllPorts(Module.PortGroup_DOWN);
		for (int i=0; i<pp_.length; i++) {
			int ifindex_ = Integer.parseInt(pp_[i].getID());
			setBufferMode(ifindex_, mode_);
		}
	}
	
	public void setBufferMode(int ifindex_, String mode_)
	{
		Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifindex_);
		if (q_ != null) 
			q_.setMode(mode_);
		else {
			QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + ifindex_);
			qni_.setMode(mode_);
		}
	}
	
	public String getBufferMode(int ifindex_)
	{
		Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifindex_);
		if (q_ != null) 
			return q_.getMode();
		else {
			QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + ifindex_);
			return qni_.getMode();
		}
	}
	
	public int getBufferSize(int ifindex_)
	{
		Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + ifindex_);
		if (q_ != null) 
			return q_.getCapacity();
		else {
			QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + ifindex_);
			return qni_.getCapacity();
		}
	}
	
	public void setLinkEmulationEnabled(int ifindex_, boolean enabled_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		ni_.setLinkEmulationEnabled(enabled_);
	}
	
	public boolean isLinkEmulationEnabled(int ifindex_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		return ni_.isLinkEmulationEnabled();
	}

	/** Sets the emulated link propagation delay.
	 * Used with link emulation enabled at that interface. */
	public void setLinkPropDelay(int ifindex_, double delay_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		ni_.setPropDelay(delay_);
	}
	
	/** Returns the emulated link propagation delay.
	 * Used with link emulation enabled at that interface. */
	public double getLinkPropDelay(int ifindex_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		return ni_.getPropDelay();
	}

	public void setMTU(int ifindex_, int mtu_)
	{
		NI ni_ = (NI)getComponent(CSLBuilder.ID_NI + ifindex_);
		if (ni_ != null)
			ni_.setMTU(mtu_);
		
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ != null) pd_._setMTU(ifindex_, mtu_);
		
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) {
			InterfaceInfo ifinfo_ = getInterfaceInfo(ifindex_);
			if (ifinfo_ != null) ifinfo_.setMTU(mtu_);
			else setInterfaceInfo(ifindex_, new InterfaceInfo(mtu_, -1.0, -1));
		}
	}
	
	// XXX: does not consider there may be difference in MTU at each interface
	public void setMTUs(int mtu_)
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ != null) pd_._setMTUs(mtu_);
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) hello_._setMTUs(mtu_);
	}
	
	public int getMTU(int ifindex_)
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		return pd_ == null? -1: pd_._getMTU(ifindex_);
	}
	
	public void setInterfaceInfo(int ifindex_, InterfaceInfo if_)
	{
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) hello_._setInterfaceInfo(ifindex_, if_);
	}
	
	public void setInterfaceInfos(InterfaceInfo[] aa_)
	{
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ != null) hello_.setInterfaceInfos(aa_);
	}
	
	public InterfaceInfo getInterfaceInfo(int ifindex_)
	{
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		return hello_ == null? null: hello_._getInterfaceInfo(ifindex_);
	}
	
	public InterfaceInfo[] getInterfaceInfos()
	{
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		return hello_ == null? null: hello_.getInterfaceInfos();
	}
	
	public int getNumOfInterfaces()
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) return -1;
		VIFPack vifpack_ = pd_.getVIFs();
		return getNumOfPhysicalInterfaces() + (vifpack_ == null? 0: vifpack_.getNumOfVIFs());
	}
	
	public int getNumOfPhysicalInterfaces()
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) return -1;
		Port[] pp_ = pd_.getAllPorts(Module.PortGroup_DOWN);
		return pp_ == null? 0: pp_.length;
	}
	
	public void addRTEntry(RTKey key_, RTEntry entry_, double timeout_)
	{
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ == null) {
			PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
			if (pd_ != null && !pd_.isRTLookup()) return;
		}
		rt_.add(key_, entry_, timeout_); // arise exception if rt should be there
	}
	
	public void removeRTEntry(RTKey key_)
	{
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ == null) {
			PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
			if (pd_ != null && !pd_.isRTLookup()) return;
		}
		rt_.remove(key_, drcl.inet.contract.RTConfig.MATCH_EXACT);
	}

	public Object retrieveRTEntry(RTKey key_, String matchMethod_)
	{
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ == null)
			return null;
		else
			return rt_.get(key_, matchMethod_);
	}
	
	public RTEntry[] retrieveAllRTEntries()
	{
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ == null)
			return null;
		else
			return rt_._getAll();
	}
	
	
	protected void portAdded(Port p_)
	{
		String gid_ = p_.getGroupID();
		if (gid_.equals(Module.PortGroup_UP))
			_addUpPort(p_);
		else if (gid_.equals(Module.PortGroup_DOWN))
			_addDownPort(p_);
		else //if (gid_.equals(PortGroup_EVENT) || gid_.equals(PortGroup_SERVICE))
			_addDefaultPort(p_);
	}
	
	void _addUpPort(Port pup_)
	{
		Component pd_ = getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ != null) pup_.connect(pd_.addPort(Module.PortGroup_UP, pup_.getID()));
	}
	
	void _addDownPort(Port pdown_)
	{
		// XXX: add pkt filter bank
	}
	
	void _addDefaultPort(Port p_)
	{
		try {

			String id_ = p_.getID();
			if (id_.equals(UCAST_QUERY_PORT_ID) || id_.equals(MCAST_QUERY_PORT_ID) || id_.equals(EVENT_PKT_ARRIVAL_PORT_ID)) {
				Component pd_ = getComponent(CSLBuilder.ID_PKT_DISPATCHER);
				pd_.getPort(p_.getGroupID().intern(), id_.intern()).connectTo(p_);
			}
			else if (id_.equals(EVENT_ID_CHANGED_PORT_ID)|| id_.equals(SERVICE_ID_PORT_ID)) {
				Component idc_ = getComponent(CSLBuilder.ID_IDENTITY);
				idc_.getPort(p_.getGroupID().intern(), id_.intern()).connectTo(p_);
			}
			else if (id_.equals(EVENT_RT_UCAST_CHANGED_PORT_ID) || id_.equals(EVENT_RT_MCAST_CHANGED_PORT_ID)
					|| id_.equals(SERVICE_RT_PORT_ID)) {
				Component rt_ = getComponent(CSLBuilder.ID_RT);
				rt_.getPort(p_.getGroupID().intern(), id_.intern()).connectTo(p_);
			}
			else if (id_.equals(EVENT_IF_PORT_ID) || id_.equals(EVENT_VIF_PORT_ID) || id_.equals(SERVICE_IF_PORT_ID)) {
				Component hello_ = getComponent(CSLBuilder.ID_HELLO);
				hello_.getPort(p_.getGroupID().intern(), id_.intern()).connectTo(p_);
			}
			else if (id_.equals(SERVICE_CONFIGSW_PORT_ID)) {
				Component sw_ = getComponent(CSLBuilder.ID_PKT_FILTER_SWITCH);
				sw_.getPort(p_.getGroupID().intern(), id_.intern()).connect(p_);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			drcl.Debug.error("Failed to complete the addition of " + p_ + " due to " + e_);
		}
	}
	
	public void setupVIF(int vifindex_, long peer_, int mtu_)
	{ setupVIF(vifindex_, drcl.net.Address.NULL_ADDR, peer_, mtu_);	}
		
	public void setupVIF(int vifindex_, long src_, long peer_, int mtu_)
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(drcl.inet.core.CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) {
			drcl.Debug.error("No PktDispatcher in the CoreServiceLayer to set up the VIF component", false);
			return;
		}
		if (mtu_ >= 0 && mtu_ != PktDispatcher.DEFAULT_MTU) {
			pd_.setFragmentEnabled(true);
			// remove the header size from the virtual interface
			pd_._setMTU(vifindex_, mtu_ - pd_.getHeaderSize());
		}
		
		if (src_ == drcl.net.Address.NULL_ADDR)
			pd_.setVIF(vifindex_, peer_);
		else
			pd_.setVIF(vifindex_, src_, peer_);

		if (mtu_ < 0) mtu_ = Integer.MAX_VALUE;

		//System.out.println("handle hello");
		Hello hello_ = (Hello)getComponent(CSLBuilder.ID_HELLO);
		if (hello_ instanceof Hellov) {
			long tmp_ = ((Hellov)hello_).getVIFStartIndex();
			if (tmp_ > vifindex_) ((Hellov)hello_).setVIFStartIndex(vifindex_);
			
			InterfaceInfo info_ = hello_._getInterfaceInfo(vifindex_);
			if (info_ == null)
				info_ = new InterfaceInfo(mtu_, -1.0/*bw: dont care*/,
								-1/*bs: dont care*/);
			else
				info_.setMTU(mtu_);
			hello_._setInterfaceInfo(vifindex_, info_);
		}
		else {
			drcl.Debug.error("No Hellov in the CoreServiceLayer to adjust"
							+ " the vif start index", false);
		}
	}

	public Port[][] getNAMPacketEventPorts()
	{
		PktDispatcher pd_ = (PktDispatcher)getComponent(CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) return null;
		Port[] pp_ = pd_.getAllPorts(Module.PortGroup_DOWN);
		if (pp_ == null) return null;
		Port[][] ppp_ = new Port[pp_.length][3];
		for (int i=0; i<pp_.length; i++) {
			Queue q_ = (Queue)getComponent(CSLBuilder.ID_QUEUE + i);
			if (q_ != null) {
				ppp_[i][0] = q_.getPort(Module.PortGroup_UP);
				ppp_[i][1] = q_.getPort(drcl.comp.queue.ActiveQueueContract.OUTPUT_PORT_ID);
				ppp_[i][2] = q_.infoPort;
			}
			else {
				QueueNI qni_ = (QueueNI)getComponent(CSLBuilder.ID_NI + i);
				ppp_[i][0] = qni_.getPort(InetCoreConstants.PULL_PORT_ID);
				ppp_[i][1] = qni_.getPort(".deq");
				ppp_[i][2] = qni_.infoPort;
			}
		}
		return ppp_;
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		// bind id and rt to pd
		PktDispatcher pd_ = (PktDispatcher)getComponent(
						CSLBuilder.ID_PKT_DISPATCHER);
		if (pd_ == null) return;
		Identity id_ = (Identity)getComponent(CSLBuilder.ID_IDENTITY);
		pd_.bind(id_);
		RT rt_ = (RT)getComponent(CSLBuilder.ID_RT);
		if (rt_ != null)
			pd_.bind(rt_);
	}
}

