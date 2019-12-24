// @(#)Classifier.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.net.tool;

import java.util.*;
import drcl.comp.Port;
import drcl.data.*;
import drcl.net.Packet;
import drcl.net.PktClassifier;

/**
The component classifies the incoming packets and pass them on at corresponding ports.
Specifically, this component uses the {@link drcl.net.PktClassifier} to obtain an integer key for
an incoming packet and then retrieves the corresponding port from its key-port map.

<p>The class defines a set of <code>add()</code> and <code>remove()</code> methods to
configure the key-port map.
*/
public class Classifier extends drcl.comp.Component
{
	drcl.data.Map map;
	Hashtable ht;
	PktClassifier pktclassifier;
								 
	public Classifier()
	{	this(null);	}
	
	public Classifier(String id_)
	{	super(id_);	}
	
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		if (!(data_ instanceof Packet)) {
			error(data_, "process()", inPort_, "unrecognized data");
			return;
		}
		
		Packet s_ = (Packet)data_;
		Port p_ = null;
		IntObj key_ = new IntObj(pktclassifier.classify(s_));
			
		// first look in hashtable
		if (ht != null)
			p_ = (Port)ht.get(key_);
		else if (map != null) {
			drcl.data.BitSet bs_ = new drcl.data.BitSet(32, key_.value);
			p_ = (Port)map.get(bs_, drcl.data.Map.MATCH_LONGEST);
		}
			
		if (p_ != null) p_.doLastSending(data_);
		else if (isGarbageEnabled())
			drop(data_, "no port assigned to key " + key_);
	}
	
	public void reset()
	{
		super.reset();
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Classifier that_ = (Classifier)source_;
		if (that_.map != null)
			map = (drcl.data.Map)that_.map.clone();
		if (that_.ht != null)
			ht = (Hashtable) ht.clone();
		pktclassifier = that_.pktclassifier; // FIXME
	}

	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		sb_.append("PktClassifier: " + pktclassifier + "\n");
		if (ht != null) {
			Enumeration elements_ = ht.elements();
			for (Enumeration keys_ = ht.keys(); keys_.hasMoreElements(); ) {
				Object element_ = elements_.nextElement();
				Object key_ = keys_.nextElement();
				sb_.append(key_ + "(-1): " + element_ + "\n");
			}
		}
		if (map != null) sb_.append(map.numberRepresentation());
		return sb_.toString();
	}

	public void setPktClassifier(PktClassifier pc_)
	{ pktclassifier = pc_; }

	public PktClassifier getPktClassifier()
	{ return pktclassifier; }
	
	//
	private void ___SCRIPT___() {}
	//
	
	/**
	 * Adds or replaces a mapping.
	 * @param value_	value of the key.
	 * @param mask_		mask of the key.
	 * @param portID_	ID of the port to be mapped from the key.
	 */
	public void add(int value_, int mask_, String portID_)
	{
		Port p_ = addPort(portID_);
		add(value_, mask_, p_);
	}
	
	/**
	 * Adds or replaces a mapping.
	 * @param value_	value of the key.
	 * @param mask_		mask of the key.
	 * @param portID_	ID of the port to be mapped from the key.
	 * @param portGroup_	port group ID of the port to be mapped from the key.
	 */
	public void add(int value_, int mask_, String portGroup_, String portID_)
	{
		Port p_ = addPort(portGroup_, portID_);
		add(value_, mask_, p_);
	}
	
	/**
	 * Adds or replaces a mapping.
	 * @param key_		the key.
	 * @param p_		the port to be mapped from the key.
	 */
	public void add(MapKey key_, Port p_)
	{
		add((int)key_.getValue().getSubset(0), (int)key_.getMask().getSubset(0), p_);
	}
	
	/**
	 * Adds or replaces a mapping.
	 * @param value_	value of the key.
	 * @param mask_		mask of the key.
	 * @param p_		the port to be mapped from the key.
	 */
	public void add(int value_, int mask_, Port p_)
	{
		if (p_ == null || !containsPort(p_)) return;
		if (mask_ == -1) {
			if (ht == null) ht = new Hashtable();
			ht.put(new IntObj(value_), p_);
		}
		else {
			if (map == null) map = new drcl.data.Map();
			map.addEntry(new MapKey(mask_, value_), p_);
		}
	}
	
	/**
	 * Removes a mapping.
	 * @param value_	value of the key.
	 * @param mask_		mask of the key.
	 * @return the port mapped from the key.
	 */
	public Port remove(int value_, int mask_)
	{
		if (mask_ == -1) {
			Object o_ = null;
			if (ht != null) o_ = ht.remove(new IntObj(value_));
			return (Port) o_;
		}
		if (map != null) {
			MapKey key_ = new MapKey(mask_, value_);
			Object o_ = map.remove(key_, drcl.data.Map.MATCH_EXACT);
			return (Port)o_;
		}
		return null;
	}
	
	/**
	 * Removes a mapping.
	 * @param key_		the key.
	 * @return the port mapped from the key.
	 */
	public Port remove(MapKey key_)
	{
		return remove((int)key_.getValue().getSubset(0), (int)key_.getMask().getSubset(0));
	}
	
	/** Removes all the mappings. */
	public void removeAll()
	{
		if (ht != null) ht.clear();
		if (map != null) map.reset();
	}
}
