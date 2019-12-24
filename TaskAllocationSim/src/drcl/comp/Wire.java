// @(#)Wire.java   1/2004
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

public class Wire extends drcl.DrclObj
{
	// outports: ports.outwire = this;
	// inports: ports.inwire = this;
	PortPack inports, outports;
	PortPack shadowInports, shadowOutports;
	PortPack inEvtListeners, outEvtListeners; // "data" and "send" events

	// NOTE:
	// "data" events for nonshadow ports are taken care of elsewhere because
	// of complexity and performance concern
	
	public Wire()
	{}
	
	public synchronized void duplicate(Object source_)
	{
		Wire that_ = (Wire)source_;
		if (that_ == null) return;
		// XXX: 
		/*
		disconnect(); // XXX: ???
		if (that_.outports != null)
			outports = (Port[]) that_.outports.clone();
		if (that_.inports != null)
			inports = (Port[]) that_.inports.clone();
		*/
	}

	// attach that_ list to the end of mine_ list
	// mine_ must not be null
	void _attachToEnd(PortPack that_, PortPack mine_)
	{
		while (mine_.next != null) mine_ = mine_.next;
		mine_.next = that_;
	}

	/**
	 * Joins the wire <code>that_</code> to this.
	 * Assumes that the shadow/client relation among ports in these
	 * two wires have been resolved.
	 * @return this wire object; null if something goes wrong.
	 */
	public synchronized Wire join(Wire that_)
	{
		if (this == that_) return this;
		_convertIn(that_.inports);
		_convertIn(that_.shadowInports);
		_convertOut(that_.outports);
		_convertOut(that_.shadowOutports);
		if (outports == null) outports = that_.outports;
		else _attachToEnd(that_.outports, outports);
		if (inports == null) inports = that_.inports;
		else _attachToEnd(that_.inports, inports);
		if (shadowOutports == null) shadowOutports = that_.shadowOutports;
		else _attachToEnd(that_.shadowOutports, shadowOutports);
		if (shadowInports == null) shadowInports = that_.shadowInports;
		else _attachToEnd(that_.shadowInports, shadowInports);
		if (inEvtListeners == null) inEvtListeners = that_.inEvtListeners;
		else _attachToEnd(that_.inEvtListeners, inEvtListeners);
		if (outEvtListeners == null) outEvtListeners = that_.outEvtListeners;
		else _attachToEnd(that_.outEvtListeners, outEvtListeners);
		return this;
	}

	// convert the inwire of the ports in pp_  to this wire
	void _convertIn(PortPack pp_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			tmp_.port.inwire = this;
	}

	// convert the outwire of the ports in pp_  to this wire
	void _convertOut(PortPack pp_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			tmp_.port.outwire = this;
	}
	
	/** Joins the wire as IN to receive data from the wire.*/
	public synchronized Wire joinIn(Port p_)
	{
		if (p_.isShadow()) {
			shadowInports = new PortPack(p_, shadowInports);
			if (p_.flagTraceData)
				inEvtListeners = new PortPack(p_, inEvtListeners);
		}
		else
			inports = new PortPack(p_, inports);
		//if (p_.isDataTraceEnabled())
			//inEvtListeners = new PortPack(p_, inEvtListeners);
		p_.inwire = this;
		return this;
	}

	/** Joins the wire as OUT to send data to the wire.*/
	public synchronized Wire joinOut(Port p_)
	{
		if (p_.isShadow())
			shadowOutports = new PortPack(p_, shadowOutports);
		else
			outports = new PortPack(p_, outports);
		if (p_.isSendTraceEnabled())
			outEvtListeners = new PortPack(p_, outEvtListeners);
		p_.outwire = this;
		return this;
	}

	/** Joins the wire for both send and receive data.*/
	public synchronized Wire joinInOut(Port p_)
	{
		if (p_.isShadow()) {
			shadowInports = new PortPack(p_, shadowInports);
			shadowOutports = new PortPack(p_, shadowOutports);
			if (p_.flagTraceData)
				inEvtListeners = new PortPack(p_, inEvtListeners);
		}
		else {
			outports = new PortPack(p_, outports);
			inports = new PortPack(p_, inports);
		}
		//if (p_.isDataTraceEnabled())
			//inEvtListeners = new PortPack(p_, inEvtListeners);
		if (p_.isSendTraceEnabled())
			outEvtListeners = new PortPack(p_, outEvtListeners);
		p_.inwire = p_.outwire = this;
		return this;
	}

	synchronized Wire shadowJoinIn(Port p_)
	{
		shadowInports = new PortPack(p_, shadowInports);
		//if (p_.isDataTraceEnabled())
			//inEvtListeners = new PortPack(p_, inEvtListeners);

		if (p_.isShadow() && p_.flagTraceData)
			inEvtListeners = new PortPack(p_, inEvtListeners);
		p_.inwire = this;
		return this;
	}

	synchronized Wire shadowJoinOut(Port p_)
	{
		shadowOutports = new PortPack(p_, shadowOutports);
		if (p_.isSendTraceEnabled())
			outEvtListeners = new PortPack(p_, outEvtListeners);
		p_.outwire = this;
		return this;
	}

	synchronized Wire shadowJoinInOut(Port p_)
	{
		shadowInports = new PortPack(p_, shadowInports);
		shadowOutports = new PortPack(p_, shadowOutports);
		if (p_.isShadow() && p_.flagTraceData)
			inEvtListeners = new PortPack(p_, inEvtListeners);
		//if (p_.isDataTraceEnabled())
			//inEvtListeners = new PortPack(p_, inEvtListeners);
		if (p_.isSendTraceEnabled())
			outEvtListeners = new PortPack(p_, outEvtListeners);
		p_.inwire = p_.outwire = this;
		return this;
	}

	/** Attaches the port to this wire as an "IN" port. */
	public void attach(Port p_)
	{
		if (p_ != null)
			inports = new PortPack(p_, inports);
	}
	
	/** Attaches the ports to this wire as "IN" ports. */
	public synchronized void attach(Port[] pp_)
	{
		if (pp_ == null || pp_.length == 0) return;
		for (int i=0; i<pp_.length; i++) {
			Port p_ = pp_[i];
			if (p_ == null) continue;
			inports = new PortPack(p_, inports);
		}
	}
	
	
	/** Detaches the port from "IN" ports. */
	public void detach(Port p_)
	{ inports = _removePort(inports, p_); }
	
	/** Detaches the ports from "IN" ports. */
	public synchronized void detach(Port[] pp_)
	{
		for (int i=0; i<pp_.length; i++) {
			Port p_ = pp_[i];
			if (p_ == null) continue;
			inports = _removePort(inports, p_);
		}
	}
	
	/*
	// don't do synchronized here, it will lock the wire if p_.doReceiving()
	// is blocked
	void doSending(Object data_, Port sender_)
	{
		// trace for shadow ports
		if (outports != null)
			for (int i=0; i<outports.length; i++) {
				Port p_ = outports[i];
				if (p_ != null && p_.isShadow() && p_.host != null
					&& p_.isSendTraceEnabled()
					&& p_.host.isAncestorOf(sender_.host))
					p_.host.trace(Component.Trace_SEND, p_, data_);
			}
		
		if (inports == null) return;
		
		for (int i=0; i<inports.length; i++) {
			Port p_ = inports[i];
			if (p_ == null || p_ == sender_) continue;
			
			// trace for shadow ports
			if (p_.isShadow()) {
				if (p_.host != null && p_.isDataTraceEnabled()
					&& !p_.host.isAncestorOf(sender_.host))
					p_.host.trace(Component.Trace_DATA, p_, data_);
				continue;
			}
			
			if (p_.getType() == Port.PortType_OUT) continue;
			else p_.doReceiving(data_, sender_);
		}
	}
	*/
	
	/** Disconnects all the connected ports from this wire. */
	public synchronized void disconnect()
	{
		inports = _removeInAll(inports);
		shadowInports = _removeInAll(shadowInports);
		outports = _removeOutAll(outports);
		shadowOutports = _removeOutAll(shadowOutports);
		inEvtListeners = outEvtListeners = null;
	}
	
	/** Disconnects the port from this wire. */
	public synchronized void disconnect(Port p_)
	{
		if (p_ == null) return;
		if (p_.inwire == this) {
			p_.inwire = null;
			if (p_.isShadow())
				shadowInports = _removePort(shadowInports, p_);
			else
				inports = _removePort(inports, p_);
		}
		if (p_.outwire == this) {
			p_.outwire = null;
			if (p_.isShadow())
				shadowOutports = _removePort(shadowOutports, p_);
			else
				outports = _removePort(outports, p_);
		}
	}
	
	/** Disconnects the ports from this wire. */
	public synchronized void disconnect(Port[] pp_)
	{
		if (pp_ == null || pp_.length == 0) return;
		for (int i=0; i<pp_.length; i++)
			disconnect(pp_[i]);
	}
	
	/**
	 * Splits the ports from this wire.  Those ports are re-connected in
	 * the same manner to a new wire.
	 * @return the new wire. 
	 */
	public synchronized Wire split(Port[] pp_)
	{
		if (pp_ == null || pp_.length == 0) return null;
		
		Hashtable ht_ = new Hashtable();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i] != null) ht_.put(pp_[i], ht_);
		Wire new_ = new Wire();
		PortPack[] ppp_ = new PortPack[1];
		new_.inports = _split(inports, ht_, ppp_);
		inports = ppp_[0]; ppp_[0] = null;
		new_.outports = _split(outports, ht_, ppp_);
		outports = ppp_[0]; ppp_[0] = null;
		new_.shadowInports = _split(shadowInports, ht_, ppp_);
		shadowInports = ppp_[0]; ppp_[0] = null;
		new_.shadowOutports = _split(shadowOutports, ht_, ppp_);
		shadowOutports = ppp_[0]; ppp_[0] = null;
		new_.inEvtListeners = _split(inEvtListeners, ht_, ppp_);
		inEvtListeners = ppp_[0]; ppp_[0] = null;
		new_.outEvtListeners = _split(outEvtListeners, ht_, ppp_);
		outEvtListeners = ppp_[0];
		new_._convert();
		return new_;
	}


	// convert all the ports' inwire and outwire setting to this wire
	void _convert()
	{
		_convertIn(inports);
		_convertIn(shadowInports);
		_convertOut(outports);
		_convertOut(shadowOutports);
	}

	// split pp_ based on ht_, all ports in pp_ that are also in ht_ form
	// the new PP and the new PP is returned
	// if pp_ is changed, the new value is stored in reply_[0]
	// caller should prepare reply_[0]
	PortPack _split(PortPack pp_, Hashtable ht_, PortPack[] reply_)
	{
		PortPack new_ = null;
		reply_[0] = pp_;
		while (pp_ != null && ht_.containsKey(pp_.port)) {
			PortPack tmp_ = pp_.next;
			pp_.next = new_;
			new_ = pp_;
			pp_ = tmp_;
			reply_[0] = pp_;
		}

		if (pp_ != null)
			for (PortPack tmp_ = pp_; tmp_.next != null; ) {
				if (ht_.containsKey(tmp_.next.port)) {
					PortPack tmp2_ = tmp_.next;
					tmp_.next = tmp2_.next;
					tmp2_.next = new_;
					new_ = tmp2_;
				}
				else
					tmp_ = tmp_.next;
			}
		return new_;
	}

	// get shadow ports (of target_) from pp_ and form another PP
	PortPack _getShadows(PortPack pp_, Port target_)
	{
		PortPack newPP_ = null;
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next) {
			if (tmp_.port.host.isAncestorOf(target_.host)) {
				PortPack new_ = new PortPack(tmp_.port);
				new_.next = newPP_;
				newPP_ = new_;
			}
		}
		return newPP_;
	}

	// get client ports (of target_, a shadow port) from pp_ and form another PP
	PortPack _getClients(PortPack pp_, Port target_)
	{
		PortPack newPP_ = null;
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next) {
			if (target_.host.isAncestorOf(tmp_.port.host)) {
				PortPack new_ = new PortPack(tmp_.port);
				new_.next = newPP_;
				newPP_ = new_;
			}
		}
		return newPP_;
	}

	// returns true if target_ is in pp_
	boolean _isIn(Port target_, PortPack pp_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			if (tmp_.port == target_) return true;
		return false;
	}

	// move p_ to shadow PP
	void _moveToShadow(Port p_)
	{
		if (inports != null) {
			if (inports.port == p_) {
				PortPack tmp_ = inports;
				inports = inports.next;
				tmp_.next = shadowInports;
				shadowInports = tmp_;
			}
			else {
				for (PortPack tmp_ = inports; tmp_.next != null;
								tmp_ = tmp_.next)
					if (tmp_.next.port == p_) {
						PortPack tmp2_ = tmp_.next;
						tmp_.next = tmp2_.next;
						tmp2_.next = shadowInports;
						shadowInports = tmp2_;
						break;
					}
			}
			if (p_.flagTraceData)
				inEvtListeners = new PortPack(p_, inEvtListeners);
		}

		if (outports != null) {
			if (outports.port == p_) {
				PortPack tmp_ = outports;
				outports = outports.next;
				tmp_.next = shadowOutports;
				shadowOutports = tmp_;
			}
			else {
				for (PortPack tmp_ = outports; tmp_.next != null;
								tmp_ = tmp_.next)
					if (tmp_.next.port == p_) {
						PortPack tmp2_ = tmp_.next;
						tmp_.next = tmp2_.next;
						tmp2_.next = shadowOutports;
						shadowOutports = tmp2_;
						break;
					}
			}
			if (p_.isSendTraceEnabled())
				outEvtListeners = new PortPack(p_, outEvtListeners);
		}
	}

	// move p_ out of shadow PP (to client PP)
	void _moveOutOfShadow(Port p_)
	{
		if (shadowInports != null) {
			if (shadowInports.port == p_) {
				PortPack tmp_ = shadowInports;
				shadowInports = shadowInports.next;
				tmp_.next = inports;
				inports = tmp_;
			}
			else {
				for (PortPack tmp_ = shadowInports; tmp_.next != null;
								tmp_ = tmp_.next)
					if (tmp_.next.port == p_) {
						PortPack tmp2_ = tmp_.next;
						tmp_.next = tmp2_.next;
						tmp2_.next = inports;
						inports = tmp2_;
						break;
					}
			}
			if (p_.flagTraceData)
				_removePort(inEvtListeners, p_);
		}

		if (shadowOutports != null) {
			if (shadowOutports.port == p_) {
				PortPack tmp_ = shadowOutports;
				shadowOutports = shadowOutports.next;
				tmp_.next = outports;
				outports = tmp_;
			}
			else {
				for (PortPack tmp_ = shadowOutports; tmp_.next != null;
								tmp_ = tmp_.next)
					if (tmp_.next.port == p_) {
						PortPack tmp2_ = tmp_.next;
						tmp_.next = tmp2_.next;
						tmp2_.next = outports;
						outports = tmp2_;
						break;
					}
			}
			if (p_.isSendTraceEnabled())
				_removePort(outEvtListeners, p_);
		}
	}

	// remove shadow ports (of target_) from pp_ and returns the new PP
	// does not re-assign ports' wires; caller's responsibility
	PortPack _removeShadows(PortPack pp_, Port target_)
	{
		// do this until the first PP is not target's shadow
		while (pp_ != null && (pp_.port.host.isAncestorOf(target_.host)))
			pp_ = pp_.next;

		if (pp_ == null) return null;

		for (PortPack tmp_ = pp_; tmp_.next != null; ) {
			if (tmp_.next.port.host.isAncestorOf(target_.host))
				tmp_.next = tmp_.next.next;
			else
				tmp_ = tmp_.next;
		}
		return pp_;
	}

	// remove client ports (of target_) from pp_ and returns the new PP
	// does not re-assign ports' wires; caller's responsibility
	PortPack _removeClients(PortPack pp_, Port target_)
	{
		// do this until the first PP is not target's client
		while (pp_ != null && (target_.host.isAncestorOf(pp_.port.host)))
			pp_ = pp_.next;

		if (pp_ == null) return null;

		for (PortPack tmp_ = pp_; tmp_.next != null; ) {
			if (target_.host.isAncestorOf(tmp_.next.port.host))
				tmp_.next = tmp_.next.next;
			else
				tmp_ = tmp_.next;
		}
		return pp_;
	}

	// remove target_ from pp_ and returns the new PP
	PortPack _removePort(PortPack pp_, Port target_)
	{
		if (pp_ == null) 
			return null;
		else if (pp_ != null && pp_.port == target_)
			return pp_.next;

		for (PortPack tmp_ = pp_; tmp_.next != null; tmp_ = tmp_.next)
			if (target_ == tmp_.next.port) {
				tmp_.next = tmp_.next.next;
				break;
			}
		return pp_;
	}

	// removes all the ports in pp_
	// returns null
	PortPack _removeInAll(PortPack pp_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; ) {
			tmp_.port.inwire = null;
			PortPack tmp2_ = tmp_;
			tmp_ = tmp_.next;
			tmp2_.next = null;
		}
		return null;
	}

	// removes all the ports in pp_
	// returns null
	PortPack _removeOutAll(PortPack pp_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; ) {
			tmp_.port.outwire = null;
			PortPack tmp2_ = tmp_;
			tmp_ = tmp_.next;
			tmp2_.next = null;
		}
		return null;
	}

	/** Splits this wire with respect to the target port.
	 * This wire becomes the "out" wire of the target port.
	 * The method creates, and returns, the "in" wire for the target
	 * port. */
	public synchronized Wire inoutSplit(Port target_)
	{
		if (target_.isShadow())
			throw new PortException(target_, "cannot do inout split on a shadow"
					+ " port");
			// a shadow port may have more than one client port,
			// it does not make sense to split multiple (client) ports
			// at once

		Wire inwire_ = new Wire();
		inwire_.outports = outports;
		inwire_.shadowOutports = shadowOutports;
		inwire_.outEvtListeners = outEvtListeners;

		// rebuild outports and shadowOutports of this wire
		outports = new PortPack(target_);
		// look for shadow ports
		shadowOutports = _getShadows(shadowOutports, target_);
		outEvtListeners = _getShadows(outEvtListeners, target_);
		if (_isIn(target_, inwire_.outEvtListeners))
			outEvtListeners = new PortPack(target_, outEvtListeners);

		// rebuild inports and shadowInports of inwire_
		// - side effect: the ports in inwire_.outports and 
		//   shadowOutports are reassigned their inwire
		inwire_.inports = new PortPack(target_);
		// look for shadow ports
		inwire_.shadowInports = _getShadows(shadowInports, target_);
		inwire_.inEvtListeners = _getShadows(inEvtListeners, target_);
		if (_isIn(target_, inEvtListeners))
			inwire_.inEvtListeners = new PortPack(target_,
							inwire_.inEvtListeners);
		for (PortPack tmp_ = inwire_.shadowInports; tmp_ != null;
			tmp_ = tmp_.next)
			tmp_.port.inwire = inwire_;
		target_.inwire = inwire_;

		// remove target (and its shadows if necessary) from inports
		// and shadowInports of this wire
		inports = _removeShadows(inports, target_);
		shadowInports = _removeShadows(shadowInports, target_);
		inports = _removePort(inports, target_);
		shadowInports = _removePort(shadowInports, target_);
		
		// remove target (and its shadows if necessary) from outports
		// and shadowOutports of inwire_
		inwire_.outports = _removeShadows(inwire_.outports, target_);
		inwire_.shadowOutports =_removeShadows(inwire_.shadowOutports, target_);
		inwire_.outports = _removePort(inwire_.outports, target_);
		inwire_.shadowOutports = _removePort(inwire_.shadowOutports, target_);

		// check peer ports
		for (PortPack tmp_ = inwire_.outports; tmp_ != null; tmp_ = tmp_.next) {
			Port p_ = tmp_.port;
			if (p_.outwire == this) p_.outwire = inwire_;
		}
		for (PortPack tmp_ = inwire_.shadowOutports; tmp_ != null;
			tmp_ = tmp_.next) {
			Port p_ = tmp_.port;
			if (p_.outwire == this) p_.outwire = inwire_;
		}
		return inwire_;
	}
	
	// Returns true if there exists a port, other than <code>excluded_</code>,
	// which is in pp_
	boolean _anyPortExcept(PortPack pp_, Port excluded_)
	{
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			if (tmp_.port != excluded_) return true;
		return false;
	}

	/**
	 * Returns true if there exists a port, other than <code>excluded_</code>,
	 * which is connected to this wire.
	 */
	public synchronized boolean anyPortExcept(Port excluded_)
	{
		if (excluded_.isShadow())
			return _anyPortExcept(shadowInports, excluded_) 
					| _anyPortExcept(shadowOutports, excluded_);
		else
			return _anyPortExcept(inports, excluded_) 
					| _anyPortExcept(outports, excluded_);
	}
	
	/** Returns true if the specified port is attached to this wire. */
	public synchronized boolean isAttachedToBy(Port p_)
	{
		if (p_.isShadow())
			return _isIn(p_, shadowInports) 
					| _isIn(p_, shadowOutports);
		else
			return _isIn(p_, inports) 
					| _isIn(p_, outports);
	}
	
	/** Returns true if the specified port is attached to this wire as IN
	 * ports. */
	public synchronized boolean isAttachedToInBy(Port p_)
	{
		if (p_.isShadow())
			return _isIn(p_, shadowInports);
		else
			return _isIn(p_, inports);
	}
	
	/** Returns true if the specified port is attached to this wire as OUT
	 * ports. */
	public synchronized boolean isAttachedToOutBy(Port p_)
	{
		if (p_.isShadow())
			return _isIn(p_, shadowOutports);
		else
			return _isIn(p_, outports);
	}
	
	Vector _getPorts(PortPack pp_, Vector v_)
	{
		if (pp_ == null) return v_;
		if (v_ == null) v_ = new Vector();
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			v_.addElement(tmp_.port);
		return v_;
	}

	Vector _getPortsExcept(PortPack pp_, Port excluded_, Vector v_)
	{
		if (pp_ == null) return v_;
		if (v_ == null) v_ = new Vector();
		for (PortPack tmp_ = pp_; tmp_ != null; tmp_ = tmp_.next)
			if (tmp_.port != excluded_) v_.addElement(tmp_.port);
		return v_;
	}

	/** Returns all the "IN" ports that are attached to this wire,
	 * including shadow ports. */
	public synchronized Port[] getInPorts()
	{
		Vector v_ = _getPorts(inports, null);
		v_ = _getPorts(shadowInports, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	/** Returns all the "OUT" ports that are attached to this wire
	 * including shadow ports. */
	public synchronized Port[] getOutPorts()
	{
		Vector v_ = _getPorts(outports, null);
		v_ = _getPorts(shadowOutports, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the ports that are attached to this wire
	 * including shadow ports. */
	public synchronized Port[] getPorts()
	{
		Vector v_ = _getPorts(inports, null);
		v_ = _getPorts(shadowInports, v_);
		v_ = _getPorts(outports, v_);
		v_ = _getPorts(shadowOutports, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the real "IN" ports that are attached to this wire. */
	public synchronized Port[] getRealInPorts()
	{
		Vector v_ = _getPorts(inports, null);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	/** Returns all the real "OUT" ports that are attached to this wire. */
	public synchronized Port[] getRealOutPorts()
	{
		Vector v_ = _getPorts(outports, null);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the ports that are attached to this wire. */
	public synchronized Port[] getRealPorts()
	{
		Vector v_ = _getPorts(inports, null);
		v_ = _getPorts(outports, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the shadow "IN" ports that are attached to this wire. */
	public synchronized Port[] getShadowInPorts()
	{
		Vector v_ = _getPorts(shadowInports, null);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	/** Returns all the shadow "OUT" ports that are attached to this wire. */
	public synchronized Port[] getShadowOutPorts()
	{
		Vector v_ = _getPorts(shadowOutports, null);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the shadow ports that are attached to this wire. */
	public synchronized Port[] getShadowPorts()
	{
		Vector v_ = _getPorts(shadowInports, null);
		v_ = _getPorts(shadowOutports, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the ports, except <code>excluded_</code>, that
	 *  are attached to this wire. */
	public synchronized Port[] getPortsExcept(Port excluded_)
	{
		Vector v_ = _getPortsExcept(inports, excluded_, null);
		v_ = _getPortsExcept(shadowInports, excluded_, v_);
		v_ = _getPortsExcept(outports, excluded_, v_);
		v_ = _getPortsExcept(shadowOutports, excluded_, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	/** Returns all the real ports, except <code>excluded_</code>, that
	 *  are attached to this wire. */
	public synchronized Port[] getRealPortsExcept(Port excluded_)
	{
		Vector v_ = _getPortsExcept(inports, excluded_, null);
		v_ = _getPortsExcept(outports, excluded_, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}

	/** Returns all the shadow ports, except <code>excluded_</code>, that
	 *  are attached to this wire. */
	public synchronized Port[] getShadowPortsExcept(Port excluded_)
	{
		Vector v_ = _getPortsExcept(shadowInports, excluded_, null);
		v_ = _getPortsExcept(shadowOutports, excluded_, v_);
		if (v_ == null) return new Port[0];
		return (Port[])v_.toArray(new Port[v_.size()]);
	}
	
	public synchronized String info()
	{
		StringBuffer sb_ = new StringBuffer();
		int j = 0;
		for (PortPack tmp_ = inports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.inwire == this? "": "(x)";
			sb_.append("   in port " + (j++) + ": " + tmp_.port 
							+ error_ + "\n");
		}
		for (PortPack tmp_ = shadowInports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.inwire == this? "": "(x)";
			sb_.append("   in shadow port " + (j++) + ": " + tmp_.port
						   	+ error_ + "\n");
		}
		j = 0;
		for (PortPack tmp_ = outports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.outwire == this? "": "(x)";
			sb_.append("   out port " + (j++) + ": " + tmp_.port
						   + error_	+ "\n");
		}
		for (PortPack tmp_ = shadowOutports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.outwire == this? "": "(x)";
			sb_.append("   out shadow port " + (j++) + ": " + tmp_.port
						   + error_	+ "\n");
		}
		if (sb_.length() == 0) return this + ": bare wire\n";
		return this + ":\n" + sb_.toString();
	}
	
	public synchronized String toString()
	{
		StringBuffer sb_ = new StringBuffer();
		int j = 0;
		for (PortPack tmp_ = inports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.inwire == this? "": "(x)";
			sb_.append(" ->" + tmp_.port + error_);
		}
		for (PortPack tmp_ = outports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.outwire == this? "": "(x)";
			sb_.append(" <-" + tmp_.port + error_);
		}
		for (PortPack tmp_ = shadowInports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.inwire == this? "": "(x)";
			sb_.append(" =>" + tmp_.port + error_);
		}
		for (PortPack tmp_ = shadowOutports; tmp_ != null; tmp_ = tmp_.next) {
			String error_ = tmp_.port.outwire == this? "": "(x)";
			sb_.append(" <=" + tmp_.port + error_);
		}
		for (PortPack tmp_ = inports; tmp_ != null; tmp_ = tmp_.next)
			if (tmp_.port.isDataTraceEnabled())
				sb_.append(" ::>" + tmp_.port);
		for (PortPack tmp_ = inEvtListeners; tmp_ != null; tmp_ = tmp_.next)
			sb_.append(" ::>" + tmp_.port);
		for (PortPack tmp_ = outEvtListeners; tmp_ != null; tmp_ = tmp_.next)
			sb_.append(" <::" + tmp_.port);
		if (sb_.length() == 0) return super.toString() + "-bare";
		return super.toString() + sb_.toString();
	}
}
