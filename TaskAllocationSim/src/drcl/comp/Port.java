// @(#)Port.java   2/2004
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

package drcl.comp;

import java.util.*;
import drcl.data.*;
import drcl.comp.contract.*;

/**
 * The implementation of a port in the autonomous component architecture (ACA).
 * A port has separate input and output channels.  Each channel can be
 * connected to a different {@link Wire}.  The wire that is connected to the
 * input channel is called the "in-wire" of the port.  The wire that is 
 * connected to the output channel is called the "out-wire" of the port.
 * Use {@link #getInWire()} and {@link #getOutWire()} to access those wires.
 *
 * @see Component
 * @see Wire
 */
public class Port extends drcl.DrclObj
{
	/** Set to true to make ports created from now execution boundary.*/
	public static boolean EXECUTION_BOUNDARY = true;

	/** The duplex port type. */
	public final static int PortType_INOUT = 0;
	/** The input-only port type. */
	public final static int PortType_IN = 1;
	/** The output-only port type. */
	public final static int PortType_OUT = 2;
	/** The server port type */
	public final static int PortType_SERVER = 3;
	/** The event port type */
	public final static int PortType_EVENT = 4;
	/** The <i>fork</i> port type */
	public final static int PortType_FORK = 5;

	final static String[] PortType_ALL = new String[]{
		"IN&OUT", "IN", "OUT", "SERVER", "EVENT", "FORK"
	};


	//
	private  void ___FLAG___() {}
	//
	
	// Mask of the port type encoded in the port flag, default is PortType_INOUT
	static final int Flag_TYPE        = 7; // first 3 bits
	// Bit mask of the "execution boundary" flag, default is on
	// Turn off the flag to have the thread which delivers data to the port
	// continue processing the data in the host component.
	static final int Flag_EXEBOUNDARY = 1 << 3;
	// Bit mask of the "removable" flag, default is on
	static final int Flag_REMOVABLE   = 1 << 4;
	// Bit mask of the "shadow port" flag, default is off
	static final int Flag_SHADOW      = 1 << 5;
	// Bit mask of the "trace" flag, default is off
	static final int Flag_TRACE_SEND  = 1 << 6;
	//static final int Flag_TRACE_DATA  = 1 << 7;
	// Bit mask of the "event export" flag, default is on
	static final int Flag_EVENT_EXPORT= 1 << 8;
	
	/**
	 ID of the port group this port belongs to.
	 Must use {@link #setGroupID(String)} or {@link #set(String, String)} to
	 set the group ID of a port
	 because it affects the bookkeeping in the host component.
	 */
	public String groupID;
	
	/**
	 ID of the port, unique in the port group it belongs to.
	 Must use {@link #setID(String)} or {@link #set(String, String)} to set
	 the ID of a port
	 because it affects the bookkeeping in the host component.
	 @see #groupID
	 */
	public String id;
	
	// The component that contains the port.
	public Component host;
	
	protected Wire outwire, inwire;
	
	
	// By default, exe boundary, removable and event export
	int flag = EXECUTION_BOUNDARY?
			(Flag_EXEBOUNDARY | Flag_REMOVABLE | Flag_EVENT_EXPORT):
			(Flag_REMOVABLE | Flag_EVENT_EXPORT);

	// separate this from other flags for performance reason
	boolean flagTraceData = false;
	
	public static final Object SEND_RCV_REQUEST = "SEND_RCV_REQ";

	//
	private void ___CONSTRUCTOR_INIT___() {}
	//
	
	/** Constructor, default duplex port type. */
	public Port()
	{ this(PortType_INOUT);	}
	
	/** Constructor, with specified port type. */
	public Port(int type_)
	{ setType(type_);	}

	/** Constructor, with specified port type and properties. */
	public Port(int type_, boolean exeBoundary_)
	{
		this(type_);
		if (!exeBoundary_) flag = flag & ~Flag_EXEBOUNDARY;
	}
	
	public final void reset()
	{
	}
	
	//
	private void ___PROPERTIES___() {}
	//
	
	/** Sets the host component. */
	public final void setHost(Component host_)
	{ host = host_;	}
	
	/** Returns the host component. */
	public final Component getHost()
	{ return host; }
	
	/**
	 * Sets the id of the group this port belongs to.
	 * Returns false if failed.
	 */
	public final boolean setGroupID(String groupID_) 
	{ 
		if (id == null) {
			groupID = groupID_== null? null: groupID_.intern();
			return true;
		}
		else return _set(groupID_, id); 
	}
	
	/** Get the id of the group this port belongs to. */
	public final String getGroupID()
	{ return groupID; }
	
	/**
	 * Set the id of the port, unique in the port group it belongs to.
	 * Returns false if failed.
	 */
	public final boolean setID(String id_) 
	{ 
		if (groupID == null) {
			id = id_== null? null: id_.intern();
			return true;
		}
		else return _set(groupID, id_); 
	}
	
	/** Sets the group ID and port ID of this port. */
	public final boolean set(String gid_, String id_)
	{
		if (gid_ != null) gid_ = gid_.intern();
		if (id_ != null) id_ = id_.intern();
		return _set(gid_, id_);
	}

	boolean _set(String gid_, String id_)
	{
		if (host != null && (gid_ == null || id_ == null)) return false;
		
		if (host == null || gid_ == null || id == null) {
			groupID = gid_; id = id_;
			return true;
		}
		else {
			if (host.getPort(gid_, id_) != null) return false;
			boolean removable_ = isRemovable();
			if (!removable_) setRemovable(true);
			host.removePort(this);
			host.addPort(this, gid_, id_);
			if (!removable_) setRemovable(false);
			return true;
		}
	}
	
	/** Returns the id of the port. */
	public final String getID()
	{ return id; }
	
	public final void setType(int type_) 
	{ 
		flag &= ~Flag_TYPE;
		flag |= type_;
	}

	public final void setType(String type_) 
	{
		for (int i=0; i<PortType_ALL.length; i++)
			if (type_.equals(PortType_ALL[i])) {
				setType(i);
				return;
			}
	}

	/** Returns the port type of this port. */
	public final String getTypeInString()
	{
		try {
			return PortType_ALL[flag & Flag_TYPE];
		}
		catch (Exception e_) {
			return "UNKNOWN";
		}
	}

	public final int getType()
	{
		return flag & Flag_TYPE;
	}
	
	public final void setExecutionBoundary(boolean flag_) 
	{ 
		if (flag_) flag |= Flag_EXEBOUNDARY; 
		else flag &= ~Flag_EXEBOUNDARY;
	}
	
	public final boolean isExecutionBoundary() 
	{ return (flag & Flag_EXEBOUNDARY) != 0;	}
	
	public final int getFlag()
	{ return flag; }
	
	public final void setRemovable(boolean flag_) 
	{ 
		if (flag_) flag |= Flag_REMOVABLE; 
		else flag &= ~Flag_REMOVABLE;
	}
	
	public final boolean isRemovable() 
	{ return (flag & Flag_REMOVABLE) != 0;	}
	
	public final void setTraceEnabled(boolean enabled_) 
	{ 
		if (!isShadow())
			flagTraceData = enabled_;
		if (enabled_) {
			if (isShadow() && !flagTraceData) {
			//if ((flag & Flag_TRACE_DATA) == 0) {
			//	flag |= Flag_TRACE_DATA; 
				flagTraceData = enabled_;
				if (inwire != null)
					inwire.inEvtListeners = new PortPack(this,
									inwire.inEvtListeners);
			}
			if ((flag & Flag_TRACE_SEND) == 0) {
				flag |= Flag_TRACE_SEND; 
				if (outwire != null)
					outwire.outEvtListeners = new PortPack(this,
									outwire.outEvtListeners);
			}
		}
		else {
			if (isShadow() && flagTraceData) {
			//if ((flag & Flag_TRACE_DATA) != 0) {
			//	flag &= ~Flag_TRACE_DATA;
				flagTraceData = enabled_;
				if (inwire != null)
					inwire.inEvtListeners = 
						inwire._removePort(inwire.inEvtListeners, this);
			}
			if ((flag & Flag_TRACE_SEND) != 0) {
				flag &= ~Flag_TRACE_SEND;
				if (outwire != null)
					outwire.outEvtListeners = 
						outwire._removePort(outwire.outEvtListeners, this);
			}
		}
	}
	
	public final void setDataTraceEnabled(boolean enabled_) 
	{ 
		if (!isShadow())
			flagTraceData = enabled_;
		//else if (enabled_ ^ (flag & Flag_TRACE_DATA) != 0) {
		else if (enabled_ ^ flagTraceData) {
		if (enabled_) {
		//	flag |= Flag_TRACE_DATA; 
			flagTraceData = enabled_;
			if (inwire != null)
				inwire.inEvtListeners = new PortPack(this,
								inwire.inEvtListeners);
		}
		else {
		//	flag &= ~Flag_TRACE_DATA;
			flagTraceData = enabled_;
			if (inwire != null)
				inwire.inEvtListeners = 
					inwire._removePort(inwire.inEvtListeners, this);
		}
		}
	}
	
	public final boolean isDataTraceEnabled() 
	//{ return (flag & Flag_TRACE_DATA) != 0;	}
	{ return flagTraceData; }

	public final void setSendTraceEnabled(boolean enabled_) 
	{ 
		if (enabled_ ^ (flag & Flag_TRACE_SEND) != 0) {
		if (enabled_) {
			flag |= Flag_TRACE_SEND; 
			if (outwire != null)
				outwire.outEvtListeners = new PortPack(this,
								outwire.outEvtListeners);
		}
		else {
			flag &= ~Flag_TRACE_SEND;
			if (outwire != null)
				outwire.outEvtListeners = 
					outwire._removePort(outwire.outEvtListeners, this);
		}
		}
	}
	
	public final boolean isSendTraceEnabled() 
	{ return (flag & Flag_TRACE_SEND) != 0;	}

	public final void setEventExportEnabled(boolean enabled_) 
	{ 
		if (enabled_) flag |= Flag_EVENT_EXPORT; 
		else flag &= ~Flag_EVENT_EXPORT;
	}
	
	public final boolean isEventExportEnabled() 
	{ return (flag & Flag_EVENT_EXPORT) != 0;	}

	public final boolean isShadow()
	{ return (flag & Flag_SHADOW) != 0;	}
	
	public void setShadow(boolean flag_)
	{
		if (flag_ ^ (flag & Flag_SHADOW) != 0) {
		if (flag_ ^ anyClient()) {
			if (flag_)
				throw new PortException("Port has no client to become shadow: "
								+ this);
			else
				throw new PortException("Port has at least one client.  "
								+ "Cannot become nonshadow: " + this);
		}
		_setShadow(flag_);
		}
	}
	void _setShadow(boolean flag_)
	{
		if (flag_) {
			if (inwire != null)
				inwire._moveToShadow(this);
			if (outwire != inwire && outwire != null)
				outwire._moveToShadow(this);
			flag |= Flag_SHADOW; 
		}
		else {
			if (inwire != null)
				inwire._moveOutOfShadow(this);
			if (outwire != inwire && outwire != null)
				outwire._moveOutOfShadow(this);
			flag &= ~Flag_SHADOW;
		}
	}
	
	public final Contract getContract()
	{
		if (anyClient()) { // this port is a shadow port
			Port[] pp_ = getConceptualClients();
			Vector vContract_ = new Vector();
			for (int i=0; i<pp_.length; i++)
				if (pp_[i] != null) vContract_.addElement(pp_[i].getContract());
			if (vContract_.size() == 0)
				return ContractAny.INSTANCE;
			else if (vContract_.size() == 1)
				return (Contract)vContract_.firstElement();
			else {
				// create a multiple-contract
				Contract[] cc_ = new Contract[vContract_.size()];
				vContract_.copyInto(cc_);
				return new ContractMultiple(cc_);
			}
		}
		else {
			return Component.getContract(this);
		}
	}
	
	public Wire getOutWire()
	{ return outwire; }
	
	public Wire getInWire()
	{ return inwire; }
	
	/** Returns true if the port connects to or shadows for at least one port.
	 */
	public final boolean anyConnection() 
	{
		if (outwire != null && outwire.anyPortExcept(this)) return true;
		if (inwire != null && inwire != outwire)
			return inwire.anyPortExcept(this);
		return false;
	}
	
	/** Returns true if the port connects to or shadows for at least one port.
	 */
	public final boolean anyOutConnection() 
	{
		if (outwire != null && outwire.anyPortExcept(this)) return true;
		return false;
	}
	
	/**
	 * Clients = InClients + OutClients.
	 * @see {@link #getInClients()} 
	 * @see {@link #getOutClients()} 
	 */
	public final boolean anyClient()
	{
		if (outwire == null && inwire == null) return false;
		Port[] tmp_ = null;
		if (outwire != null) {
			tmp_ = outwire.getPortsExcept(this);
			for (int i=0; i<tmp_.length; i++)
				if (host == tmp_[i].host.parent) return true;
		}
		if (inwire != null) {
			tmp_ = inwire.getPortsExcept(this);
			for (int i=0; i<tmp_.length; i++)
				if (host == tmp_[i].host.parent) return true;
		}
		return false;
	}
	
	Port[] _getAllConnectedPorts(boolean removeDuplicate_)
	{
		if (inwire == null && outwire == null) return new Port[0];
		Port[] pp1 = inwire == null? new Port[0]:
			inwire.getPortsExcept(this);
		Port[] pp2 = outwire == null? new Port[0]:
			outwire.getPortsExcept(this);
		return _merge(pp1, pp2, removeDuplicate_);
	}

	// merge pp1 and pp2 to a new array
	// pp1 and pp2 and their elements cannot be null
	// assume pp1 and pp2 do not contain duplicates 
	static Port[] _merge(Port[] pp1, Port[] pp2, boolean removeDuplicate_)
	{
		if (removeDuplicate_) {
			LinkedList ll = new LinkedList();
			HashMap map_ = new HashMap();
			// put pp1 to map
			for (int i=0; i<pp1.length; i++)
				map_.put(pp1[i], pp1[i]);
			// check if any port in pp2 also appear in pp1
			for (int i=0; i<pp2.length; i++)
				if (!map_.containsKey(pp2[i]))
					ll.add(pp2[i]);
			pp2 = (Port[])ll.toArray(new Port[ll.size()]);
		}
		Port[] tmp_ = new Port[pp1.length + pp2.length];
		System.arraycopy(pp1, 0, tmp_, 0, pp1.length);
		System.arraycopy(pp2, 0, tmp_, pp1.length, pp2.length);
		return tmp_;
	}
	
	/**
	 * Clients = InClients + OutClients.
	 * @see {@link #getInClients()} 
	 * @see {@link #getOutClients()} 
	 */
	public final Port[] getClients()
	{
		if (inwire == null && outwire == null) return new Port[0];
		Port[] in_ = getInClients();
		Port[] out_ = getOutClients();
		return _merge(in_, out_, true);
	}
	
	/**
	 * OutClients: the set of ports at which if data is sent, 
	 * the data would arrive at this port.
	 */
	public final Port[] getOutClients()
	{
		if (outwire == null) return new Port[0];
		Port[] tmp_ = outwire.getOutPorts();
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (host.isAncestorOf(p_.host))
				v_.addElement(p_);
		}
		tmp_ = new Port[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	/**
	 * InClients: the set of ports at which if data is sent, 
	 * the data would arrive at this port.
	 */
	public final Port[] getInClients()
	{
		if (inwire == null) return new Port[0];
		Port[] tmp_ = inwire.getInPorts();
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (host.isAncestorOf(p_.host))
				v_.addElement(p_);
		}
		tmp_ = new Port[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}

	/**
	 * ConceptualClients = ConceptualOutClients + ConceptualInClients.
	 * @see {@link #getConceptualInClients()} 
	 * @see {@link #getConceptualOutClients()} 
	 */
	public final Port[] getConceptualClients()
	{
		if (inwire == null && outwire == null) return new Port[0];
		Port[] in_ = getConceptualInClients();
		Port[] out_ = getConceptualOutClients();
		return _merge(in_, out_, true);
	}
	
	/**
	 * ConceptualOutClients: the OutClients that belong to direct child
	 * components.
	 */
	public final Port[] getConceptualOutClients()
	{
		if (inwire == null) return new Port[0];
		Port[] tmp_ = outwire.getOutPorts();
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (host == p_.host.parent) 
				v_.addElement(p_);
		}
		tmp_ = new Port[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}

	/**
	 * ConceptualInClients: the InClients that belong to direct child
	 * components.
	 */
	public final Port[] getConceptualInClients()
	{
		if (outwire == null) return new Port[0];
		Port[] tmp_ = inwire.getInPorts();
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (host == p_.host.parent) 
				v_.addElement(p_);
		}
		tmp_ = new Port[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	
	/** Any peer conntected to this port?  Peer = InPeer or OutPeer.
	 * @see {@link #getInPeers()} 
	 * @see {@link #getOutPeers()} 
	 */
	public final boolean anyPeer()
	{
		if (inwire == null && outwire == null) return false;
		Port[] tmp_ = _getAllConnectedPorts(false);
		for (int i=0; i<tmp_.length; i++)
			if (!host.isAncestorOf(tmp_[i].host)) return true;
		return false;
	}
	
	/**
	 * Peers = InPeers + OutPeers.
	 * @see {@link #getInPeers()} 
	 * @see {@link #getOutPeers()} 
	 */
	public final Port[] getPeers()
	{
		if (inwire == null && outwire == null) return new Port[0];
		return _getPeers(_getAllConnectedPorts(true));
	}
	
	/**
	 * OutPeers: the set of ports (including shadow ports) at which if data
	 * is sent, the data would arrive at this port.
	 */
	public final Port[] getOutPeers()
	{
		if (inwire == null) return new Port[0];
		return _getPeers(inwire.getOutPorts());
	}
	
	/**
	 * InPeers: if data is sent at this port, the set of
	 * ports (including shadow ports) at which the data would arrive. 
	 */
	public final Port[] getInPeers()
	{
		if (outwire == null) return new Port[0];
		return _getPeers(outwire.getInPorts());
	}

	// identifies and returns peers of this port
	Port[] _getPeers(Port[] tmp_)
	{
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (p_ == null) System.out.println("Port: oops...");
			if (!host.isDirectlyRelatedTo(p_.host))
				v_.addElement(p_);
		}
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/**
	 * ConceptualInPeers: the InPeers except those are hidden by
	 * outer components.
	 */
	public final Port[] getConceptualInPeers()
	{
		Port[] ancestorsOut = getOutAncestors(); 
		Port[] shadowsIn = outwire == null? new Port[0]:
				outwire.getShadowInPorts(); 
		Port[] connectedTo = getInPeers(); 
		
		// find my shadow out ports
		int nshadow = 0;
		Component outermost = null;
		Component innermost = null;
		for (int i=0; i<ancestorsOut.length; i++) {
			Port p = ancestorsOut[i];
			if (p.host.isAncestorOf(this.host)) {
				if (nshadow == 0)
					outermost = innermost = p.host;
				else if (p.host.isAncestorOf(outermost))
					outermost = p.host;
				else if (innermost.isAncestorOf(p.host))
					innermost = p.host;
				ancestorsOut[nshadow++] = p;
			}
		}

		LinkedList ll = new LinkedList();
		for (int i=0; i<connectedTo.length; i++) {
			Port p = connectedTo[i];
			if (nshadow > 0 && !innermost.isAncestorOf(p.host))
				continue; // dont consider this connection
			boolean pass_ = true;
			for (int j=0; pass_ && j<shadowsIn.length; j++)
				if (shadowsIn[j].host.isAncestorOf(p.host))
					pass_ = false;
			if (pass_) ll.add(p);
		}
		return (Port[])ll.toArray(new Port[ll.size()]);
	}
	
	/** Returns all the ancestor ports (def: the host of which contains
	 * the host of this port. */
	public final Port[] getAncestors()
	{
		if (inwire == null && outwire == null) return new Port[0];
		return _getAncestors(_getAllConnectedPorts(true));
	}
	
	/** Returns all the ancestor out ports (def: the host of which contains
	 * the host of this port. */
	public final Port[] getOutAncestors()
	{
		if (outwire == null) return new Port[0];
		return _getAncestors(outwire.getShadowOutPorts());
	}
	
	/** Returns all the ancestor in ports (def: the host of which contains
	 * the host of this port. */
	public final Port[] getInAncestors()
	{
		if (inwire == null) return new Port[0];
		return _getAncestors(inwire.getShadowInPorts());
	}

	// identifies and returns ancestors of this port
	Port[] _getAncestors(Port[] tmp_)
	{
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (p_.host.isAncestorOf(host))
				v_.addElement(p_);
		}
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	public final Port[] getShadows()
	{
		if (inwire == null && outwire == null) return new Port[0];
	   	return _getShadows(_getAllConnectedPorts(true));
	}
	
	public final Port[] getOutShadows()
	{
		if (outwire == null) return new Port[0];
	   	return _getShadows(outwire.getOutPorts());
	}
	
	public final Port[] getInShadows()
	{
		if (inwire == null) return new Port[0];
	   	return _getShadows(inwire.getInPorts());
	}

	// identifies and returns shadow ports of this
	Port[] _getShadows(Port[] tmp_)
	{
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (p_.host == host.parent)
				v_.addElement(p_);
		}
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	
	/**
	 * Returns host's ancestor's peers and the shadow ports.
	 */
	public final Port[] getParentPeers()
	{
		if (inwire == null && outwire == null) return new Port[0];
		Port[] tmp_ = _getAllConnectedPorts(true);
		if (tmp_ == null) return new Port[0];
		Vector v_ = new Vector();
		for (int i=0; i<tmp_.length; i++) {
			Port p_ = tmp_[i];
			if (!host.parent.isAncestorOf(p_.host))
				v_.addElement(p_);
		}
		tmp_ = new Port[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	public boolean isConnectedWith(Port p_)
	{
		if (inwire == null && outwire == null) return false;
		else if (inwire != null && inwire.isAttachedToBy(p_)) return true;
		else if (outwire != null && outwire.isAttachedToBy(p_)) return true;
		else return false;
	}
	
	//
	private void ___DATA_TRANSPORT___() {}
	//
	
	// for Util.inject()
	final void doReceiving(Object data_)
	{
		if (isShadow()) {
			if (isDataTraceEnabled())
				host.trace(Component.Trace_DATA, this, data_, "(shadow)");
			Port[] clients_ = getInClients();
			// do not copy data_
			for (int i=0; i<clients_.length; i++) {
				Port client_ = clients_[i];
				client_.doReceiving(data_);
			}
		}
		else {
			//doReceiving(data_, null, null);
			//if (isDataTraceEnabled())
			//	host.trace(Component.Trace_DATA, this, data_,
			//					"(create context)");
			//Note: trace check is done in TaskReceive	
			host.runtime.newTask(new TaskReceive(this, data_), null);
		}
	}

	/** Called by the host component to send data at this port. */
	public final void doSending(Object data_)
	{ doSending(data_, host.runtime.getThread()); }

	//public static boolean DEBUG = false;

	/** Internal implementation of doSending() */
	protected void doSending(Object data_, WorkerThread thread_)
	{
		// server port:
		// bypassing other ports on the out wire of this port.
		if (thread_ != null &&
			thread_.returnPort != null && (flag & Flag_TYPE) == 3) {
			//((Port)thread_.returnPort).doReceiving(data_, this, thread_);

			Port p_ = (Port)thread_.returnPort;
			Component host_ = p_.host;

			// trace for event listeners
			if (outwire != null) {
				for (PortPack tmp_ = outwire.outEvtListeners; tmp_ != null;
								tmp_ = tmp_.next) {
					Port p2_ = tmp_.port;
					if (p2_.host != null && 
						(p2_ == this || p2_.host.isAncestorOf(host)))
						p2_.host.trace(Component.Trace_SEND, p2_, data_);
				}
				for (PortPack tmp_ = p_.outwire.inEvtListeners; tmp_ != null;
							tmp_ = tmp_.next) {
					Port p2_ = tmp_.port;
					if (p2_ == p_ || (p2_.host != null
						&& p2_.host.isAncestorOf(host_)))
						p2_.host.trace(Component.Trace_DATA, p2_, data_,
										"(shadow)");
				}
			}

			// sendReceiving
			if (thread_.sendingPort == p_) {
				//if (p_.isDataTraceEnabled())
				//	host_.trace(Component.Trace_DATA, p_, data_,
				//					"(for sendReceive())");
				thread_.sendingPort = data_;
				return;
			}

			if (thread_.sendingPort == this || !p_.isExecutionBoundary()) {
				if (host_.isEnabled()) {

				//if (p_.isDataTraceEnabled())
				//	host_.trace(Component.Trace_DATA, p_, data_,
				//					"(not across exe boundary)");

				// the following execution order is important
				String prevState_ = thread_.state;
				thread_.totalNumEvents ++;
	
				if (p_.flagTraceData)
					host_.trace(Component.Trace_DATA, p_, data_);
				try {
					host_.process(data_, p_);
				}
				catch (Exception e_) {
					if (host_.isErrorNoticeEnabled()) 
						host_.error(data_, "process()", p_, e_);
					e_.printStackTrace();
				}
	
				thread_.releaseAllLocks(host_);
				thread_.state = prevState_;
				}
			}
			else {// server port: set return port
				//if (p_.isDataTraceEnabled())
				//	host_.trace(Component.Trace_DATA, p_, data_,
				//				"(new context)");
				host_.runtime.newTask(new TaskReceive(p_, data_), thread_);
			}
			return;
		}
		
		if (outwire != null) {
			//outwire.doSending(data_, this);
			
			// trace for event listeners
			for (PortPack tmp_ = outwire.outEvtListeners; tmp_ != null;
							tmp_ = tmp_.next) {
				Port p_ = tmp_.port;
				if (p_ == this)
					p_.host.trace(Component.Trace_SEND, p_, data_);
				else if (p_.host != null && p_.host.isAncestorOf(host))
					p_.host.trace(Component.Trace_SEND, p_, data_,
									"(shadow)");
			}
			for (PortPack tmp_ = outwire.inEvtListeners; tmp_ != null;
							tmp_ = tmp_.next) {
				Port p_ = tmp_.port;
				if (p_ != this && p_.isShadow() && p_.host != null
					&& !p_.host.isAncestorOf(host))
					p_.host.trace(Component.Trace_DATA, p_, data_,
									"(shadow)");
			}

			for (PortPack tmp_ = outwire.inports; tmp_ != null;
							tmp_ = tmp_.next) {
				Port p_ = tmp_.port;
				Component host_ = p_.host;
				//if (p_ == this || !host_.isEnabled()) continue;
				if (p_ == this) continue;
				//p_.doReceiving(data_, this, thread_);

				if (thread_ == null) {
					//if (p_.isDataTraceEnabled()) 
					//	host_.trace(Component.Trace_DATA, p_, data_,
					//					"(create context)");
					//Note: trace check is done in TaskReceive	
					host_.runtime.newTask(new TaskReceive(p_, data_), thread_);
					continue;
				}

				// sendReceiving
				if (thread_.sendingPort == p_) {
					/*
					if (p_.flagTraceData)
						host_.trace(Component.Trace_DATA, p_, data_,
										"(for sendReceive())");
					*/
					thread_.sendingPort = data_;
					continue;
				}

				// Cross execution boundary if one of the following conditions
				// is true
				// 1. !isExecutionBoundary()
				// 2. thread's sendingPort is peer_
				if (thread_.sendingPort == this || !p_.isExecutionBoundary()) {
					if (host_.isEnabled()) {
					// Not execution boundary, the current thread will process
					// the data.
					// MUST do it out of the synchronized claus.
					// Context needs to be changed beforehand and restored
					// afterwards.
	
					//if (p_.isDataTraceEnabled())
					//	host_.trace(Component.Trace_DATA, p_, data_,
					//					"(not across exe boundary)");
	
					// server port: set return port
					// XXX: we probably need to back up returnPort here
					//      though it is backed up in sendReceive()
					//      because if sending component calls only
					//      doSending() and this port is not an
					//      execution boundary, then the returnPort
					//      is not backed up, so the previous setting
					//      may be overwritten
					if ((p_.flag & Port.Flag_TYPE) == 3)
						thread_.returnPort = this;
		
					// the following execution order is important
					String prevState_ = thread_.state;
					thread_.totalNumEvents ++;
		
					//if (DEBUG && thread_.sendingPort == this)
					//	System.out.println("SEND-RECEIVE: " + this
					//				+ " ---> " + p_ + ": data=" + data_);

					if (p_.flagTraceData)
						host_.trace(Component.Trace_DATA, p_, data_);
					try {
						host_.process(data_, p_);
					}
					catch (Exception e_) {
						if (host_.isErrorNoticeEnabled()) 
							host_.error(data_, "process()", p_, e_);
						e_.printStackTrace();
					}
		
					// restore context
					thread_.releaseAllLocks(host_);
						// Don't hold locks across components!
					thread_.state = prevState_;
					}
				}
				else {// server port: set return port
					//if (p_.isDataTraceEnabled())
					//	host_.trace(Component.Trace_DATA, p_, data_,
					//				"(new context)");
					if ((p_.flag & Port.Flag_TYPE) == 3)
						// set up return port
						host_.runtime.newTask(new TaskReceive(p_, data_,
												this), thread_);
					else
						host_.runtime.newTask(new TaskReceive(p_, data_),
										thread_);
				}
			} // for (PortPack tmp_...
		}
	}
	
	/**
	 * Same as {@link #doSending(Object)} in terms of functionality.
	 * Performance-aware components may use
	 the runtime
	 * notifies the runtime
	 */
	public final void doLastSending(Object data_)
	{
		host.finishing();
		doSending(data_, host.runtime.getThread());
	}

	class SendReceiveRunnable implements Runnable
	{
		Port port;
		Object data;

		SendReceiveRunnable(Port port_, Object data_)
		{
			port = port_;
			data = data_;
		}

		public void run()
		{
			data = port.sendReceive(data);
			synchronized (this) {
				this.notify();
			}
		}
	}

	
	public final Object sendReceive(Object data_)
	{
		// the following check is covered by codes below, commented out for reducing overhead
		//if (!anyOutConnection()) 
		//	throw new SendReceiveException("sendreceive()| no connection");
		
		WorkerThread thread_ = host.runtime.getThread();
		if (thread_ == null) {
			//throw new SendReceiveException("sendreceive()| not in a WorkerThread");
			try {
				SendReceiveRunnable srr_ = new SendReceiveRunnable(this, data_);
				synchronized (srr_) {
					host.runtime.addRunnable(0.0, this, srr_);
					srr_.wait();
					return srr_.data;
				}
			}
			catch (Exception e_) {
				e_.printStackTrace();
				throw new SendReceiveException("sendreceive()| " + e_);
			}
		}
		
		Object prevSendingPort_ = thread_.sendingPort;
		thread_.sendingPort = this;

		// store returnPort here in case peer is a server port as well
		Port prevReturnPort_ = thread_.returnPort;
		thread_.returnPort = null;

		doSending(data_, thread_);
		
		if (thread_.sendingPort == this) {
			if (!host.runtime.resetting)
				throw new SendReceiveException("sendreceive()| did not get reply from " 
					+ this + ".sendReceive(), data=" + data_);
				//drcl.Debug.error("sendreceive()| did not get reply from " + this
				//				+ ", returns <null> instead");
			else
				data_ = null; // runtime is resetting...
		}
		
		data_ = thread_.sendingPort;
		thread_.sendingPort = prevSendingPort_;
		thread_.returnPort = prevReturnPort_;

		if (flagTraceData)
			host.trace(Component.Trace_DATA, this, data_,
				"(end sendReceive())");
				
		return data_;
	}
		
	
	//
	private void ___CONNECTION___() {}
	//
	
	boolean _shadowConnect(Port client_)
	{
		// type mismatch?
		if (getType() == PortType_IN && client_.getType() == PortType_OUT)
			return false;
		else if (getType() == PortType_OUT
			&& client_.getType() == PortType_IN)
			return false;

		setType(client_.getType());
		
		if (client_.getType() == PortType_SERVER) {
			if (!_shadowConnectTo(client_, false)) return false;
			return _shadowConnectTo(client_, true);
		}

		if (!isShadow())
			// move to shadow list
			_setShadow(true);
		
		if (inwire == outwire && client_.inwire == client_.outwire) {
			// lump all together
			if (inwire != null) {
				if (client_.inwire != null) {
					inwire.join(client_.inwire);
				}
				else
					inwire.joinInOut(client_);
			}
			else if (client_.inwire != null)
				client_.inwire.shadowJoinInOut(this);
			else
				new Wire().joinInOut(client_).shadowJoinInOut(this);
			return true;
		}
		
		// split in/out if necessary
		if (inwire == outwire && inwire != null
			&& client_.inwire != client_.outwire
			&& (client_.inwire != null || client_.outwire != null))
			inoutSplit();
		else if (client_.inwire == client_.outwire && client_.inwire != null
			&& inwire != outwire && (inwire != null || outwire != null))
			client_.inoutSplit();
		
		if (inwire != null) {
			if (client_.inwire != null)
				inwire.join(client_.inwire);
			else
				inwire.joinIn(client_);
		}
		else if (client_.inwire != null)
			client_.inwire.shadowJoinIn(this);
		else
			new Wire().joinIn(client_).shadowJoinIn(this);
		
		if (outwire != null) {
			if (client_.outwire != null)
				outwire.join(client_.outwire);
			else
				outwire.joinOut(client_);
		}
		else if (client_.outwire != null)
			client_.outwire.shadowJoinOut(this);
		else
			new Wire().joinOut(client_).shadowJoinOut(this);
		
		return true;
	}


	/**
	 * Bi-direction connection, consider proxying.
	 * Returns true if either direction is set up successfully.
	 */
	public final boolean connect(Port peer_) 
	{
		if (host.isAncestorOf(peer_.host)) return _shadowConnect(peer_);
		else if (peer_.host.isAncestorOf(host))
			return peer_._shadowConnect(this);
		
		// server port?
		if (getType() == PortType_SERVER) {
			if (peer_.getType() == PortType_IN
				|| peer_.getType() == PortType_SERVER) return false;
			if (!peer_.connectTo(this)) return false;
			if (peer_.inwire == null)
				peer_.inwire = inwire;
			else
				inwire.join(peer_.inwire);
			if (outwire == null)
				outwire = new Wire().joinOut(this);
			return true;
		}
		else if (peer_.getType() == PortType_SERVER) {
			if (getType() == PortType_IN) return false;
			if (!connectTo(peer_)) return false;
			if (inwire == null)
				inwire = peer_.inwire;
			else
				peer_.inwire.join(inwire);
			if (peer_.outwire == null)
				peer_.outwire = new Wire().joinOut(peer_);
			return true;
		}
		
		// type mismatch?
		if (getType() == PortType_IN) {
			if (peer_.getType() == PortType_IN)
				return false;
			return peer_.connectTo(this);
		}
		else if (getType() == PortType_OUT) {
			if (peer_.getType() == PortType_OUT)
				return false;
			return connectTo(peer_);
		}
		else if (peer_.getType() == PortType_OUT)
			return peer_.connectTo(this);
		else if (peer_.getType() == PortType_IN)
			return connectTo(peer_);
		
		if (inwire == outwire && peer_.inwire == peer_.outwire) {
			// lump all together
			if (inwire != null) {
				if (peer_.inwire != null)
					inwire.join(peer_.inwire);
				else
					inwire.joinInOut(peer_);
			}
			else if (peer_.inwire != null)
				peer_.inwire.joinInOut(this);
			else
				new Wire().joinInOut(peer_).joinInOut(this);
			return true;
		}
		
		// split in/out if necessary
		if (inwire == outwire && inwire != null
			&& peer_.inwire != peer_.outwire
			&& (peer_.inwire != null || peer_.outwire != null))
			inoutSplit();
		else if (peer_.inwire == peer_.outwire && peer_.inwire != null
			&& inwire != outwire && (inwire != null || outwire != null))
			peer_.inoutSplit();
		
		// join wires
		if (inwire != null) {
			if (peer_.outwire != null)
				inwire.join(peer_.outwire);
			else
				inwire.joinOut(peer_);
		}
		else if (peer_.outwire != null)
			peer_.outwire.joinIn(this);
		else
			new Wire().joinOut(peer_).joinIn(this);
		
		if (outwire != null) {
			if (peer_.inwire != null)
				outwire.join(peer_.inwire);
			else
				outwire.joinIn(peer_);
		}
		else if (peer_.inwire != null)
			peer_.inwire.joinOut(this);
		else
			new Wire().joinIn(peer_).joinOut(this);

		return true;
	}
	
	/**
	 * Disconnects from the given ports.
	 */
	public final void connect(Port[] pp_)
	{
		for (int i=0; i<pp_.length; i++)
			connect(pp_[i]);
	}

	/** Attaches the port to the IN wire for receiving data from the wire.  */
	public final void attachIn(Port p_)
	{ attachIn(new Port[]{p_}); }
	
	/** Attaches the ports to the IN wire for receiving data from the wire.  */
	public final void attachIn(Port[] pp_)
	{
		Wire wire_ = getInWire();
		if (wire_ == null) {
			wire_ = new Wire();
			wire_.joinIn(this);
		}
		wire_.attach(pp_);
	}

	/** Removes the port from the IN wire.  */
	public final void detachIn(Port p_)
	{ detachIn(new Port[]{p_}); }
	
	/** Removes the ports from the IN wire.  */
	public final void detachIn(Port[] pp_)
	{
		Wire wire_ = getInWire();
		if (wire_ != null) wire_.detach(pp_);
	}
	
	/** Attaches the port to the OUT wire for receiving data from the wire.  */
	public final void attachOut(Port p_)
	{ attachOut(new Port[]{p_}); }
	
	/** Attaches the ports to the OUT wire for receiving data from the wire.  */
	public final void attachOut(Port[] pp_)
	{
		Wire wire_ = getOutWire();
		if (wire_ == null) {
			wire_ = new Wire();
			wire_.joinOut(this);
		}
		wire_.attach(pp_);
	}

	/** Removes the port from the OUT wire.  */
	public final void detachOut(Port p_)
	{ detachOut(new Port[]{p_}); }
	
	/** Removes the port from the OUT wire.  */
	public final void detachOut(Port[] pp_)
	{
		Wire wire_ = getOutWire();
		if (wire_ != null) wire_.detach(pp_);
	}
	
	boolean _shadowConnectTo(Port client_, boolean reverse_)
	{
		// type mismatch?
		if (!reverse_) {
			if (getType() == PortType_OUT || client_.getType() == PortType_OUT)
				return false;
		}
		else
			if (getType() == PortType_IN && client_.getType() == PortType_IN)
				return false;
		
		if (!isShadow())
			// move to shadow list
			_setShadow(true);
		
		if (reverse_) {
			// it's outwire
			if (outwire != null) {
				if (client_.outwire != null)
					outwire.join(client_.outwire);
				else
					outwire.joinOut(client_);
			}
			else if (client_.outwire != null)
				client_.outwire.joinOut(this);
			else
				new Wire().joinOut(client_).joinOut(this);
		}
		else {
			// it's inwire
			if (inwire != null) {
				if (client_.inwire != null)
					inwire.join(client_.inwire);
				else
					inwire.joinIn(client_);
			}
			else if (client_.inwire != null)
				client_.inwire.joinIn(this);
			else
				new Wire().joinIn(client_).joinIn(this);
		}
		return true;
	}
	
	/**
	 * Uni-direction connection, consider proxying.
	 * Returns true if the connection is set up successfully.
	 */
	public final boolean connectTo(Port peer_) 
	{
		if (host.isAncestorOf(peer_.host))
			return _shadowConnectTo(peer_, false);
		else if (peer_.host.isAncestorOf(host))
			return peer_._shadowConnectTo(this, true);
		
		// type mismatch?
		if (getType() == PortType_IN || peer_.getType() == PortType_OUT)
			return false;

		if (outwire == null && peer_.inwire == null)
			return new Wire().joinOut(this).joinIn(peer_) != null;
		else if (outwire != null) {
			if (peer_.inwire != null)
				outwire.join(peer_.inwire);
			else
				outwire.joinIn(peer_);
		}
		else
			peer_.inwire.joinOut(this);
		return true;
	}
	
	// shortcut for probing component
	void _connectTo(Port peer_)
	{
		if (outwire == null && peer_.inwire == null)
			new Wire().joinOut(this).joinIn(peer_);
		else if (outwire != null) {
			if (peer_.inwire != null)
				outwire.join(peer_.inwire);
			else
				outwire.joinIn(peer_);
		}
		else
			peer_.inwire.joinOut(this);
	}
	
	/**
	 * Connects to the given ports.
	 */
	public final void connectTo(Port[] pp_)
	{
		for (int i=0; i<pp_.length; i++)
			connectTo(pp_[i]);
	}
	
	/**
	 * Disconnect with all peers.
	 */
	public final void disconnect() 
	{ 
		disconnectPeers();
	}
	
	/**
	 * Disconnect with all peers and clients on the IN wire.
	 */
	public final void disconnectInWire() 
	{ 
		if (inwire != null) inwire.disconnect(this);
		inwire = null;
		if (isShadow() && !anyClient())
			_setShadow(false);
	}
	
	/**
	 * Disconnect with all peers and clients on the OUT wire.
	 */
	public final void disconnectOutWire() 
	{ 
		if (outwire != null) outwire.disconnect(this);
		outwire = null;
		if (isShadow() && !anyClient())
			_setShadow(false);
	}
	
	/**
	 * Removes peers from the wires attached with this port.
	 */
	public final void disconnectPeers()
	{
		if (this == host.infoPort) {
			outwire = null;
			return;
		}
		Port[] peers_ = getPeers();
		if (peers_ == null || peers_.length == 0) return;
		if (inwire != null) {
			inwire.split(peers_);
			if (!inwire.anyPortExcept(this)) inwire = null;
		}
		if (outwire != null) {
			if (outwire != inwire) outwire.split(peers_);
			if (!outwire.anyPortExcept(this)) outwire = null;
		}
	}
	
	public final void disconnectClients()
	{
		Port[] clients_ = getClients();
		if (clients_ == null || clients_.length == 0) return;
		if (inwire != null)
			inwire.split(clients_);
		if (outwire != null && outwire != inwire)
			outwire.split(clients_);
		_setShadow(false);
	}
	
	public final void disconnectWithParent()
	{
		Port[] peers_ = getParentPeers();
		if (peers_ == null || peers_.length == 0) return;
		if (inwire != null) inwire.split(peers_);
		if (outwire != null && outwire != inwire) outwire.split(peers_);
		for (int i=0; i<peers_.length; i++) {
			Port parent_ = peers_[i];
			if (!parent_.anyClient()) parent_._setShadow(false);
		}
	}
	
	/**
	 * Splits the "IN" wire/"OUT" wire of this port.
	 */
	public synchronized void inoutSplit()
	{
		if (inwire != outwire || inwire == null) return;
		/*
		outwire = (Wire)inwire.clone();
		outwire.inports = inwire.inports;
		inwire.inports = new Port[]{this};
		outwire.outports = new Port[]{this};
		if (inwire.outports != null)
			for (int i=0; i<inwire.outports.length; i++)
				if (inwire.outports[i] == this) inwire.outports[i] = null;
		if (outwire.inports != null)
			for (int i=0; i<outwire.inports.length; i++)
				if (outwire.inports[i] == this) outwire.inports[i] = null;
				else if (outwire.inports[i] != null)
					outwire.inports[i].inwire = outwire;
		*/
		inwire.inoutSplit(this);
	}
	
	//
	private void ___MISC___() {}
	//

	public final void exportEvent(String evtName_, Object evtObj_, 
					String evtDescription_)
	{
		doSending(new EventContract.Message(host.getTime(), evtName_,
								toString(), evtObj_, evtDescription_));
	}

	public final void exportEvent(String evtName_, double value_,
					String evtDescription_)
	{
		doSending(new DoubleEventContract.Message(host.getTime(), evtName_,
								toString(), value_, evtDescription_));
	}
	
	public final boolean _isEventExportEnabled()
	{ return ((flag & Flag_EVENT_EXPORT) != 0) && anyConnection(); }
	
	/**
	 Duplicates the content of the port from <code>source_</code>.
	 This method is meant to be invoked by {@link Component}.
	 It only duplicates the flag.  ID, groupID and wires are not copied.
	 */
	public final void duplicate(Object source_)
	{
		Port that_ = (Port)source_;
		//id = that_.id;
		//groupID = that_.groupID;
		flag = that_.flag;
		//setRemovable(that_.isRemovable()); 
		//inwire = outwire = null; // don't clone wires
	}
	
	String fullpath;
	/** Returns the full path. */
	public final String toString() 
	{
		if (fullpath == null) fullpath = Util.getPortFullID(this);
		return fullpath;
	}
	
	public final String info()
	{
		StringBuffer sb_ = new StringBuffer(Util.getPortFullID(this) + "\n");
		sb_.append("Type: " + getTypeInString() + "\n");
		sb_.append("           Shadow? " + isShadow() + "\n");
		sb_.append("ExecutionBoudnary? " + isExecutionBoundary() + "\n");
		sb_.append("        Removable? " + isRemovable() + "\n");
		sb_.append(" DataTraceEnabled? " + isDataTraceEnabled() + "\n");
		sb_.append(" SendTraceEnabled? " + isSendTraceEnabled() + "\n");
		int i = 0, k = 0;
		StringBuffer tmp_ = new StringBuffer();
		
		/*
		Port[] all_ = _getAllConnectedPorts(true);
		for (int j=0; j<all_.length; j++) {
			Port p_ = all_[j];
			String portid_ = Util.getPortID(p_, host != null && host.parent != null?
												host.parent: null);
			if (host.isAncestorOf(p_.host))
				tmp_.append(Util.checkConnect(this, p_) + "Client " + ++k + ": " + portid_ + "\n");
			else
				sb_.append(Util.checkConnect(this, p_) + "Peer " + ++i + ": " + portid_ + "\n");
		}
		if (i + k == 0) sb_.append("No peer and client.\n");
		else if (i==0) sb_.append("No peer.\n" + tmp_);
		else if (k==0) sb_.append("No client.\n");
		else sb_.append(tmp_);
		*/
		if (inwire == outwire) {
			if (inwire == null) sb_.append("No connection.\n");
			else
				sb_.append("one wire for in/out: " + inwire + "\n");
		}
		else {
			if (inwire != null)
				sb_.append("inwire: " + inwire + "\n");
			if (outwire != null)
				sb_.append("outwire:" + outwire + "\n");
		}
		return sb_.toString();
	}
}



