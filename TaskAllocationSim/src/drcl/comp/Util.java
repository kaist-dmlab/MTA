// @(#)Util.java   1/2004
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

import java.io.*;
import java.util.*;
import java.beans.*;

/**
 * Utilities to manipulate the "path" of a component/port.
 */
public class Util
{
	public static boolean OPTIMIZED = false;

	/** Returns the ID's of all port groups being created in the component. */
	public static String[] getAllPortGroupIDs(Component c_)
	{
		Port[] pp_ = c_.getAllPorts();
		Hashtable ht_ = new Hashtable();
		for (int i=0; i<pp_.length; i++) {
			if (pp_[i].getGroupID().equals(Component.PortGroup_DEFAULT_GROUP))
				ht_.put("<default>", c_);
			else
				ht_.put(pp_[i].getGroupID(), c_);
		}
		String[] tmp_ = new String[ht_.size()];
		int i = 0;
		for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
			tmp_[i++] = (String)e_.nextElement();
		}
        return tmp_;
	}

	/** Returns the full path of a component. */
	public static String getFullID(Component o_)
	{
		return getID(o_, (Component)null);
	}
	
	/**
	 Returns the partial path of a component starting from 
	 the <code>stop_</code> ancester.
	 */
	public static String getID(Component o_, Component stop_)
	{
		if (o_ == null) return "?/";
		else if (o_ == stop_) return "";
		else if (o_ == o_.getRoot()) return "/";
		else {
			String prefix_ = getID(o_.parent, stop_);
			return prefix_ + drcl.util.StringUtil.addEscape(o_.id, "/\\ ") +"/";
		}
	}
	
	/**
	 Returns the partial path of a component starting from the closest
	 ancester that belongs to the given class.
	 */
	public static String getID(Component o_, Class stopClass_)
	{
		if (o_ == null) return "?/";
		else if (o_.getClass().equals(stopClass_)) return "";
		else if (o_ == o_.getRoot()) return "/";
		else {
			String prefix_ = getID(o_.parent, stopClass_);
			return prefix_ + drcl.util.StringUtil.addEscape(o_.id, "/\\ ") +"/";
		}
	}
	
	/** Returns the full path of a port.  The port type is not shown. */
	public static String getPortFullID(Port p_)
	{ return getPortID(p_, (Component)null, false); }
	
	/**
	 Returns the full path of a port.  
	 @param type_ set to true to show the port type.
	 */
	public static String getPortFullID(Port p_, boolean type_)
	{ return getPortID(p_, (Component)null, type_); }
	
	/**
	 Returns the partial path of a port starting from the <code>stop_</code>
	 ancester.
	 Port name and type are not shown.
	 */
	public static String getPortID(Port p_, Component stop_)
	{
		return getPortID(p_, stop_, false);
	}
	
	/**
	 Returns the partial path of a port starting from the <code>stop_</code>
	 ancester.
	 @param type_ set to true to show the port type.
	 */
	public static String getPortID(Port p_, Component stop_, 
								   boolean type_)
	{
		String hostid_ = getID(p_.host, stop_);
		
		if (hostid_ == null) return null;
		else
			return  hostid_ + drcl.util.StringUtil.addEscape(p_.id, "/\\ ") +"@"
					+ drcl.util.StringUtil.addEscape(p_.groupID, "/\\ ") + 
			   (type_? "(" + p_.getTypeInString() + ")": "");
	}
	
	/**
	 Returns the partial path of a port starting from the closest ancester
	 that belongs to the given class.
	 The port type is not shown.
	 */
	public static String getPortID(Port p_, Class stopClass_)
	{
		return getPortID(p_, stopClass_, false);
	}
	
	/**
	 Returns the partial path of a port starting from the closest ancester
	 that belongs to the given class.
	 @param type_ set to true to show the port type.
	 */
	public static String getPortID(Port p_, Class stopClass_, 
								   boolean type_)
	{
		String hostid_ = getID(p_.host, stopClass_);
		
		if (hostid_ == null) return null;
		else
			return hostid_ + drcl.util.StringUtil.addEscape(p_.id, "/\\ ") + "@"
					+ drcl.util.StringUtil.addEscape(p_.groupID, "/\\ ") + 
			   (type_? "(" + p_.getTypeInString() + ")": "");
	}
	
	/** Returns the canonical form of the port ID. */
	public static String getPortID(Port p_)
	{ return p_.getID() + "@" + p_.getGroupID(); }
	
	/**
	 Returns the component, given a relative path and the starting
	 component of the path.
	 @return null if the path is not valid.
	 */
	public static Component resolvePath(String path_, Component start_)
	{
		int i = 0;
		if (path_.charAt(0) == '/') {
			start_ = start_.getRoot();
			i = 1;
		}
		char[] cc_ = path_.toCharArray();
		int len_ = cc_.length;
		while (true) {
			for (; i<len_ && cc_[i] == '/'; i++);
			if (i >= len_) break;
			int j = path_.indexOf('/', i);
			while (j > 0 && cc_[j-1] == '\\') // skip "escape" char
				j = path_.indexOf('/', j+1);
			String sub_ = j >= 0? path_.substring(i, j): path_.substring(i);
			if (sub_.equals("..")) {
				if (start_.parent != null) start_ = start_;
			}
			else if (!sub_.equals(".")) {
				sub_ = drcl.util.StringUtil.removeEscape(sub_);
				Component child_ = start_.getComponent(sub_);
				if (child_ == null) return null;
				start_ = child_;
			}
			if (j < 0) break;
			i = j+1;
		}
		return start_;
	}
	
	/**
	 Does a structural comparison between two components.
	 Returns the list of differences.
	 */
	public static String compare(Component c1_, Component c2_)
	{
		StringBuffer sb_ = new StringBuffer();
		// Component itself
		if (c1_ == null || c2_ == null)
			return "One or both components are null.\n";
		String class1_ = c1_.getClass().getName();
		String class2_ = c2_.getClass().getName();
		if (!class1_.equals(class2_))
			sb_.append(c1_ + " is a " + class1_ + " but " + c2_ + " is a "
						+ class2_ + ".\n");
					   
		// children
		Component[] cc1_ = c1_.getAllComponents();
		Component[] cc2_ = c2_.getAllComponents();
		if (cc1_.length != cc2_.length) {
			sb_.append(c1_ + " has " + cc1_.length + " children, " + 
					   c2_ + " has " + cc2_.length + ".\n");
		}
		else {
			for (int i=0; i<cc1_.length; i++) {
				Component tmp_ = c2_.getComponent(cc1_[i].id);
				if (tmp_ == null)
					sb_.append(c2_ + " does not have child '" + cc1_[i].id
								+ "'.\n");
				else sb_.append(compare(cc1_[i], tmp_));
			}
			
			for (int i=0; i<cc2_.length; i++) 
				if (!c1_.containsComponent(cc2_[i].id))
					sb_.append(c1_ + " does not have child '" + cc2_[i].id
								+ "'.\n");
		}
		
		// ports
		Port[] pp1_ = c1_.getAllPorts();
		Port[] pp2_ = c2_.getAllPorts();
		if (pp1_.length != pp2_.length) {
			sb_.append(c1_ + " has " + pp1_.length + " ports, " + 
					   c2_ + " has " + pp2_.length + ".\n");
		}
		else {
			for (int i=0; i<pp1_.length; i++) {
				Port tmp_ = c2_.getPort(pp1_[i].groupID, pp1_[i].id);
				if (tmp_ == null)
					sb_.append(c2_ + " does not have port '" + pp1_[i].id
								+ "@" + pp1_[i].groupID + "'.\n");
				else sb_.append(compare(pp1_[i], tmp_));
			}
			
			for (int i=0; i<pp2_.length; i++) 
				if (c1_.getPort(pp2_[i].groupID, pp2_[i].id) == null)
					sb_.append(c1_ + " does not have port '" + pp2_[i].id
								+ "@" + pp2_[i].groupID + "'.\n");
		}
		return sb_.toString();
	}
	
	/**
	 Does a structural comparison between two ports.
	 Returns the list of differences.
	 */
	public static String compare(Port p1_, Port p2_)
	{
		StringBuffer sb_ = new StringBuffer();
		// Port itself
		if (p1_ == null || p2_ == null) return "One or both ports are null.\n";
		String class1_ = p1_.getClass().getName();
		String class2_ = p2_.getClass().getName();
		if (!class1_.equals(class2_))
			sb_.append(p1_ + " is a " + class1_ + " but " + p2_ + " is a "
						+ class2_ + ".\n");

		if (!p1_.groupID.equals(p2_.groupID))
			sb_.append(p1_ + "'s group ID is " + p1_.groupID + " but " +
					   p2_ + "'s is " + p2_.groupID + ".\n");
		
		if (!p1_.id.equals(p2_.id))
			sb_.append(p1_ + "'s ID is " + p1_.id + " but " +
					   p2_ + "'s is " + p2_.id + ".\n");
		
		if (p1_.getType() != p2_.getType())
			sb_.append(p1_ + "'s type is " + p1_.getTypeInString() + " but " +
					   p2_ + "'s is " + p2_.getTypeInString() + ".\n");
		
		if (p1_.isExecutionBoundary() ^ p2_.isExecutionBoundary())
			sb_.append(p1_ + " exe boundary flag is " 
						+ p1_.isExecutionBoundary() + " but " + p2_
						+ "'s is " + p2_.isExecutionBoundary() + ".\n");
		
		return sb_.toString();
	}
	
	/**
	 Returns true if the <code>container_</code> component is an ancester of
	 the <code>child_</code> component.
	 */
	public static boolean contains(Component container_, Component child_)
	{
		if (container_ == null || child_ == null || container_ == child_)
			return false;
		if (container_.containsComponent(child_)) return true;
		Component[] cc_ = container_.getAllComponents();
		for (int i=0; i<cc_.length; i++) 
			if (contains(cc_[i], child_)) return true;
		return false;
	}
	
	public static final String BI_CONNECT   = "<->";
	public static final String TO_CONNECT   = "-->";
	public static final String FROM_CONNECT = "<--";
	public static final String NO_CONNECT   = "---";
	public static final String BI_ATTACH   = "<~>";
	public static final String TO_ATTACH   = "~~>";
	public static final String FROM_ATTACH = "<~~";
	static final String NO_ATTACH = null;
	
	/**
	 Checks if two ports are connected and the type of connection if
	 there is any.
	 @return combination of the connection result ({@link #BI_CONNECT},
	 	{@link #TO_CONNECT}(--&gt),	{@link #FROM_CONNECT}(&lt--) or
	 		{@link #NO_CONNECT}) and the attachment result ({@link #BI_ATTACH},
	 	{@link #TO_ATTACH} or {@link #FROM_ATTACH}).
	 */
	public static String checkConnect(Port p1_, Port p2_)
	{
		String conn_ = null;
		if (p1_.inwire == p2_.outwire && p1_.outwire == p2_.inwire) {
			if (p1_.inwire == null && p1_.outwire == null) return NO_CONNECT;
			else if (p1_.inwire == null) conn_ = TO_CONNECT;
			else if (p1_.outwire == null) conn_ = FROM_CONNECT;
			else return BI_CONNECT;
		}
		else if (p1_.inwire == p2_.outwire && p1_.outwire != p2_.inwire) {
			if (p1_.inwire != null) conn_ = FROM_CONNECT;
			else conn_ = NO_CONNECT;
		}
		else if (p1_.inwire != p2_.outwire && p1_.outwire != p2_.inwire) {
			conn_ = NO_CONNECT;
		}
		else {//if (p1_.inwire != p2_.outwire && p1_.outwire == p2_.inwire) {
			if (p1_.outwire != null) conn_ = TO_CONNECT;
			else conn_ = NO_CONNECT;
		}

		// further check attach
		String attach_ = null;
		if (conn_ == TO_CONNECT) {
			// p1 --> p2, check if p1 <~~ p2
			if (p2_.outwire == null
				|| !p2_.outwire.isAttachedToInBy(p1_))
				attach_ = NO_ATTACH;
			else
				attach_ = FROM_ATTACH;
		}
		else if (conn_ == FROM_CONNECT) {
			// p1 <-- p2, check if p1 ~~> p2
			if (p1_.outwire == null
				|| !p1_.outwire.isAttachedToInBy(p2_))
				attach_ = NO_ATTACH;
			else
				attach_ = TO_ATTACH;
		}
		else {
			// p1 --- p2, check if p1 ~~>,<~~ or <~> p2
			boolean fromAttach_ = p2_.outwire != null
						&& p2_.outwire.isAttachedToInBy(p1_);
			boolean toAttach_ = p1_.outwire != null
						&& p1_.outwire.isAttachedToInBy(p2_);
			if (fromAttach_ && toAttach_)
				return BI_ATTACH;
			else if (fromAttach_)
				attach_ = FROM_ATTACH;
			else if (toAttach_)
				attach_ = TO_ATTACH;
			else
				attach_ = NO_ATTACH;
		}

		// combine results
		if (attach_ == NO_ATTACH) return conn_;
		else if (conn_ == NO_CONNECT) return attach_;
		else return conn_ + ", " + attach_;
	}
	
	//
	static void ___COMPONENT_CONN___() {}
	//
	
	/**
	 * Connects child components' info ports to the parent (recursively).
	public static void connectInfoPort(Component c_)
	{
		Component[] oo_ = c_.getAllComponents();
		for (int i=0; i<oo_.length; i++) {
			Component child_ = oo_[i];
			if (!child_.infoPort.isConnectedWith(c_.infoPort))
				child_.infoPort.connectTo(c_.infoPort);
			connectInfoPort(child_);
		}
	}
	 */
	
	/**
	 * Connects child components' state report ports to the parent
	 * (recursively).
	public static void connectEventPorts(Component c_)
	{
		Component[] oo_ = c_.getAllComponents();
		for (int i=0; i<oo_.length; i++) {
			Component child_ = oo_[i];
			Port[] pp_ = child_.getAllPorts(Component.PortGroup_EVENT);
			Port p_ = c_.getPort(pp_[i].groupID, pp_[i].id); // shadow port
			if (p_ == null) {
				p_ = c_.addPort(pp_[i].groupID, pp_[i].id); // shadow port
				pp_[i].connectTo(p_);
			}
			else if (!pp_[i].isConnectedWith(p_))
				pp_[i].connectTo(p_);
			connectEventPorts(child_);
		}
	}
	 */

	/**
	 Returns a report of the connections inside the component.
	 The report also includes unconnected ports, port type,
	 hidden ports and connections that go outside of the component.
	 */
	public static String showConnections(Component c_) 
	{ return showConnections(c_, true, true, true, false); }
	
	/**
	 Returns a report of the connections inside the component.
	 @param showNoConn_ set to true to include unconnected ports in the report.
	 @param showType_   set to true to include port type in the report.
	 @param showHidden_ set to true to include hidden ports in the report.
	 @param showOutside_ set to true to include connections that go outside of
	 	the component.
	 */
	public static String showConnections(Component c_, boolean showNoConn_, 
										 boolean showType_, boolean showHidden_,
										 boolean showOutside_)
	{
		//System.out.println("Showconnections: " + c_);
		StringBuffer sb_ = new StringBuffer("Connections:\n");
		int initlen_ = sb_.length();

		Wire[] wires_ = c_.getAllWiresInsideOut();
		Vector vin_ = new Vector(), vout_ = new Vector(), vinout_ =new Vector();
		Hashtable htPout_ = new Hashtable();
		int k = 0; // wire index
		for (int i=0; i<wires_.length; i++) {
			Wire w = wires_[i];
			vin_.removeAllElements();
			vout_.removeAllElements();
			vinout_.removeAllElements();
			htPout_.clear();
			
			//Port[] pp_ = w.getPorts();
			Port[] pin_ = w.getInPorts();
			Port[] pout_ = w.getOutPorts();
			for (int j=0; j<pout_.length; j++)
				htPout_.put(pout_[j], w);

			for (int j=0; j<pin_.length; j++) {
				Port p_ = pin_[j];
				if (!showOutside_ && p_.host != c_ && p_.host.parent != c_
					&& p_.host.parent != c_.parent) continue;
					//&& (c_.isAncestorOf(p_.host) || 
				if (!showHidden_ && (p_.getGroupID().startsWith(".")
									 || p_.getID().startsWith(".")
									 || (p_.host.parent == c_
										&& p_.host.getID().startsWith("."))))
					continue;
				if (htPout_.containsKey(p_)) {
					htPout_.remove(p_);
					vinout_.addElement(p_);
				}
				else vin_.addElement(p_);
			}
			for (int j=0; j<pout_.length; j++) {
				Port p_ = pout_[j];
				if (!htPout_.containsKey(p_)) continue;
				if (!showOutside_ && p_.host != c_ && p_.host.parent != c_
					&& p_.host.parent != c_.parent) continue;
					//&& (c_.isAncestorOf(p_.host) || 
				if (!showHidden_ && (p_.getGroupID().startsWith(".")
									 || p_.getID().startsWith(".")
									 || (p_.host.parent == c_
										&& p_.host.getID().startsWith("."))))
					continue;
				vout_.addElement(p_);
			}
			int n = vin_.size() + vout_.size() + vinout_.size();
			if (n == 0) continue;
			if (n == 1 && !showNoConn_) continue;
			StringBuffer tmp_ = new StringBuffer("     Wire " + (++k) + " ");
			sb_.append(tmp_);
			boolean first_ = true;
			if (vinout_.size() > 0) {
				first_ = false;
				sb_.append("<--> ");
				for (int j=0; j<vinout_.size(); j++)
					sb_.append(showPort(c_, (Port)vinout_.elementAt(j),
								showType_) + " ");
				//sb_.append("\n");
			}
			if (vin_.size() > 0) {
				//if (!first_) for (int j=0; j<tmp_.length(); j++)
				//	sb_.append(" ");
				first_ = false;
				sb_.append("---> ");
				for (int j=0; j<vin_.size(); j++)
					sb_.append(showPort(c_, (Port)vin_.elementAt(j), showType_)
								+ " ");
				//sb_.append("\n");
			}
			if (vout_.size() > 0) {
				//if (!first_) for (int j=0; j<tmp_.length(); j++)
				//	sb_.append(" ");
				first_ = false;
				sb_.append("<--- ");
				for (int j=0; j<vout_.size(); j++)
					sb_.append(showPort(c_, (Port)vout_.elementAt(j), showType_)
								+ " ");
				//sb_.append("\n");
			}
			sb_.append("\n");
		}
		if (showNoConn_) {
			Port[] pp_ = c_.getAllPorts();
			LinkedList v_ = new LinkedList();
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				if (p_ == c_.infoPort) continue;
				if (!showHidden_ && (p_.getGroupID().startsWith(".")
									|| p_.getID().startsWith("."))) continue;
				if (!p_.anyConnection()) v_.add(p_);
			}
			Component[] child_ = c_.getAllComponents();
			for (int j=0; j<child_.length; j++) {
				if (!showHidden_ && child_[j].getID().startsWith(".")) continue;
				pp_ = child_[j].getAllPorts();
				for (int i=0; i<pp_.length; i++) {
					Port p_ = pp_[i];
					if (p_ == child_[j].infoPort) continue;
					if (!showHidden_ && (p_.getGroupID().startsWith(".")
									|| p_.getID().startsWith("."))) continue;
					if (!p_.anyConnection()) v_.add(p_);
				}
			}
			if (v_.size() > 0) {
				sb_.append("Ports that are not connected:\n     ");
				for (int i=0; i<v_.size(); i++)
				for (Iterator it_ = v_.iterator(); it_.hasNext();)
					sb_.append(showPort(c_, (Port)it_.next(),
											showType_) + " ");
				sb_.append("\n");
			}
		}
		if (sb_.length() == initlen_)
			sb_.append("     None is shown.\n");
		return sb_.toString();
	}

	/** Returns the information of a port in one line of String. */
	static String showPort(Component c_, Port p_, boolean showType_)
	{
		return (p_.host == c_? "": Util.getID(p_.host, c_)) + 
			   p_.id + "@" + 
			   p_.groupID + (showType_? "(" + p_.getTypeInString() + ")": "") + " ";
	}
	
	static Component cloneSource = null;
	
	/** Returns a clone of this component using object serialization. */
	public static drcl.comp.Component sClone(Component comp_)
	{
		Component c_ = null;
		Component parent_ = comp_.parent;
		comp_.parent = null; // only serialize subtree
		try {
			if (cloneSource != comp_) {
				__baos__.reset();
				ObjectOutputStream  s_  =  new  ObjectOutputStream(__baos__);
				s_.writeObject(comp_);
				s_.flush();
				s_.close();
				System.out.println(__baos__.size());
				cloneSource = comp_;
			}
			
			ObjectInputStream sin_ = new ObjectInputStream(
							new ByteArrayInputStream(__baos__.toByteArray()));
			c_ = (Component) sin_.readObject();
		} catch (Exception e_) {
			e_.printStackTrace();
		}
		comp_.parent = parent_;
		return c_;
	}
	
	
	//
	private  void ___SERIALIZATION___() {}
	//
	
	static ByteArrayOutputStream __baos__ = new ByteArrayOutputStream();
																	  
	/**
	 Saves the component hierarchy with the given root to the
	 ObjectOutputStream.  using object serialization.
	 */
	public static void save(ObjectOutputStream s_, Component subroot_)
	{
		try {
			//FileOutputStream f_ = new FileOutputStream(filename_);
			//ObjectOutputStream  s_ = new  ObjectOutputStream(__baos__);//f_);
			Component parent_ = subroot_.parent;
			subroot_.parent = null;
			s_.writeObject(subroot_);
			s_.flush();
			s_.close();
			//f_.close();
			//System.out.println(__baos__.size());
			subroot_.parent = parent_;
		} catch (Exception e_) {
			drcl.Debug.error("Component.save()", e_.toString());
		}
	}
	
	/**
	 Loads a component hierarchy from the ObjectInputStream.
	 Returns the root component of the hierarchy.
	 */
	public static Component load(ObjectInputStream in_)
	{
		try {
			//FileInputStream in_ = new FileInputStream(filename_);
			ObjectInputStream s_ = new ObjectInputStream(in_);
			Component subroot_  = (Component)s_.readObject();
			s_.close(); in_.close();
			
			return subroot_;
		} catch (Exception e_) {
			drcl.Debug.error("Component.load()", e_.toString());
			return null;
		}
	}
	
	
	//
	static void ___COMPONENT_OP___() {}
	//
	
	/** Associates the runtime ({@link ACARuntime}) to the component. */
	public static void setRuntime(Component c_, ACARuntime m_)
	{	c_.setRuntime(m_);	}
	
	/** Returns the associated runtime ({@link ACARuntime}) of the component. */
	public static ACARuntime getRuntime(Component c_)
	{ return c_.getRuntime(); }
	
	/** Associates the {@link ForkManager} to the component. */
	public static void setForkManager(Component c_, ForkManager fm_)
	{	c_.fm = fm_;	}
	
	/** Returns the associated {@link ForkManager} of the component. */
	public static ForkManager getForkManager(Component c_)
	{ return c_.fm; }
	
	static String OP_START = "start";
	static String OP_STOP = "stop";
	static String OP_RESUME = "resume";

	/**
	 Starts/stops/resumes components.  The method stops the associated
	 runtime(s) before operating the components and resumes the runtime(s)
	 afterwards.  It eliminates randomness between the (current) process of
	 operating the components and the processes of operated components.
	 */
	public static void operate(Object[] cc_, String operation_)
	{
		try {
		if (cc_ == null) return;
		int op_ = operation_.equals(OP_START)? Task.TYPE_START:
			operation_.equals(OP_STOP)? Task.TYPE_STOP:
			operation_.equals(OP_RESUME)? Task.TYPE_RESUME: Task.TYPE_UNKNOWN;
		if (op_ == Task.TYPE_UNKNOWN) {
			drcl.Debug.error("Unknown operation: " + op_);
			return;
		}
		Hashtable ht_ = new Hashtable(cc_.length);
		for (int i=0; i<cc_.length; i++) {
			if (cc_[i] == null || !(cc_[i] instanceof Component))
				continue;

			Component c_ = (Component)cc_[i];
			ACARuntime m_ = c_.getRuntime();

			if (m_ == null)
				drcl.Debug.error("Cannot " + operation_ + " " + c_
								+ ": no associated runtime");

			Vector vtasks_ = null;
			if (!ht_.containsKey(m_)) {
				vtasks_ = _operate(c_, 0.0, op_, null);
				if (vtasks_ != null) ht_.put(m_, vtasks_);
			}
			else {
				vtasks_ = (Vector)ht_.get(m_);
				vtasks_ = _operate(c_, 0.0, op_, vtasks_);
			}
		}
		for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
			ACARuntime m_ = (ACARuntime)e_.nextElement();
			Vector vtasks_ = (Vector)ht_.get(m_);
			if (vtasks_.size() > 0)
				m_.newTasks(vtasks_);
		}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	static Vector _operate(Component c_, double time_, int op_, 
					Vector vtasks_)
	{
		if (c_ instanceof ActiveComponent) {
			if (vtasks_ == null) vtasks_ = new Vector();
			vtasks_.addElement(new TaskSpecial(c_.infoPort, op_,
					c_.runtime.getTime() + time_));
		}
		Component[] cc_ = c_.getAllComponents();
		for (int i=0; i<cc_.length; i++)
			vtasks_ = _operate(cc_[i], time_, op_, vtasks_);
		return vtasks_;
	}

	/** Injects <code>data_</code> at the specified ports. */
	public static void inject(Object data_, Object[] ports_)
	{
		if (ports_ == null) return;
		for (int i=0; i<ports_.length; i++) {
			if (!(ports_[i] instanceof Port)) continue;
			Port p_ = (Port)ports_[i];
			p_.doReceiving(data_); // this makes sure all InClients receive
			//p_.host.runtime.newTask(new Task(p_, data_,
			//	p_.host.getTaskPriority()), null);
		}
	}

	/** Injects <code>data_</code> at the specified port(s). */
	public static void inject(Object data_, Object port_)
	{
		if (port_ == null) return;
		if (port_ instanceof Object[])
			inject(data_, (Object[])port_);
		else if (port_ instanceof Port) {
			((Port)port_).doReceiving(data_);
				// this makes sure all InClients receive

			//p_.host.runtime.newTask(new Task(p_, data_,
			//	p_.host.getTaskPriority()), null);
		}
	}


	//
	static void ___VERIFICATION___() {}
	//
	
	public static final String VERROR_IN =
		"is an 'IN' port but no port connects to it";
	public static final String VERROR_IN2 =
		"is an 'IN' port but connected to other ports";
	public static final String VERROR_OUT =
		"is an 'OUT' port but it does not connect to any other port";
	public static final String VERROR_OUT2 = 
		"is an 'OUT' port but it is connected to by other ports";
	public static final String VERROR_INOUT_IN = 
		"is an 'INOUT' port but is not connected to by any other port";
	public static final String VERROR_INOUT_OUT = 
		"is an 'INOUT' port but it does not connect to any other port";
	public static final String VERROR_INOUT = 
		"is an 'INOUT' port but it does not connect to, " 
		+ "nor is connected by, any other port";
	public static final String VERROR_OK= "is ok";
	public static final String VERROR_INFOPORT = 
		"does not connect to the host parent's info port";

	/**
	 Verifies the connections inside the specified container component.
	 Returns a hashtable with port as key and error message as result.
	 */
	public static Hashtable verify(Component[] components_, boolean recursive_)
	{	return _verify(new Hashtable(), components_, recursive_); }
	
	// verify the components, recursively
	static Hashtable _verify(Hashtable ht_, Component[] components_,
					boolean recursive_)
	{
		if (components_ == null) return ht_;

		for (int i=0; i<components_.length; i++) {
			Component c_ = components_[i];
			Port[] pp_ = c_.getAllPorts();
			for (int j=0; j<pp_.length; j++) {
				Port p_ = pp_[j];
				if (p_ == p_.host.infoPort) {
					if (p_.host.parent != null
						&& p_.getOutShadows().length == 0)
						ht_.put(p_, VERROR_INFOPORT);
				}
				else if (p_.getType() == Port.PortType_IN) {
					if (p_.outwire != null && p_.outwire.anyPortExcept(p_))
						ht_.put(p_, VERROR_IN2);
					if (p_.getOutPeers().length + p_.getInShadows().length == 0)
						ht_.put(p_, VERROR_IN);
				}
				else if (p_.getType() == Port.PortType_OUT) {
					if (p_.inwire != null && p_.inwire.anyPortExcept(p_))
						ht_.put(p_, VERROR_OUT2);
					if (p_.getInPeers().length + p_.getOutShadows().length == 0)
						ht_.put(p_, VERROR_OUT);
				}
				else if (p_.getType() == Port.PortType_FORK) {
					// nothing
				}
				else if (p_.getType() == Port.PortType_SERVER) {
					// nothing
				}
				//else if (p_.getType() == Port.PortType_UNKNOWN) {
				//	ht_.put(p_, "'s type is unknown!");
				//}
				else if (p_.getType() == Port.PortType_EVENT) {
					// nothing
				}
				else { // inout port
					if (!p_.anyConnection())
						ht_.put(p_, VERROR_INOUT);
					else if (p_.getOutPeers().length
								+ p_.getInShadows().length == 0) {
						Port[] tmp_ = p_.getInPeers();
						boolean ok_ = false;
						for (int k=0; k<tmp_.length; k++) {
							if (tmp_[k].getType() == Port.PortType_SERVER)
								{ ok_ = true; break; }
						}
						if (!ok_) {
							tmp_ = p_.getInClients();
							for (int k=0; k<tmp_.length; k++) {
								if (tmp_[k].getType() == Port.PortType_SERVER)
									{ ok_ = true; break; }
							}
						}
						if (!ok_)
							ht_.put(p_, VERROR_INOUT_IN);
					}
					else if (p_.getInPeers().length
								+ p_.getOutShadows().length == 0)
						ht_.put(p_, VERROR_INOUT_OUT);
				}
			}
				// XXX: should also verify the wire...
			if (recursive_) ht_ = _verify(ht_, c_.getAllComponents(), true);
		}
		return ht_;
	}
	
	
	//
	private void ___NEIGHBOR___() {}
	//
	
	/** The class describes a uni-directional connection between two ports. */
	public static class Link extends drcl.DrclObj
	{
		public Port from, to;
		public Component nextHop; // used in adjust() only
		
		public Link (Port from_, Port to_)
		{
			from = from_;
			to = to_;
		}
		
		public Link (Port from_, Component next_, Port to_)
		{
			from = from_;
			to = to_;
			nextHop = next_;
		}
		
		public void duplicate(Object source_)
		{
			if (!(source_ instanceof Link)) return;
			Link that_ = (Link) source_;
			from = that_.from;
			to = that_.to;
		}
		
		public String toString()
		{
			return from + " -> "  + (nextHop == null? "": nextHop + " -> ")
				+ to;
		}
	}
	
	/**
	 Explores a port's peers in a hierarchical mannger.
	 Returns all the connections (in an array of {$link Link}s} emerged from 
	 the specified port.
	 */
	public static Link[] getLinks(Port p_)
	{
		LinkedList vpeers_ = _getLinks(p_, null);
		return (Link[])vpeers_.toArray(new Link[vpeers_.size()]);
	}
	
	static LinkedList _getLinks(Port p_, LinkedList vpeers_)
	{
		if (p_ == null || p_.host == null || p_.outwire == null
			|| !p_.outwire.anyPortExcept(p_)) {
			if (vpeers_ == null) return new LinkedList();
			else return vpeers_;
		}
		
		Port[] pp2_ = p_.getPeers();
		if (pp2_ == null) {
			if (vpeers_ == null) return new LinkedList();
			else return vpeers_;
		}
		if (vpeers_ == null)
			vpeers_ = new LinkedList(); // save real peers
		int index_ = vpeers_.size();

		for (int j=0; j<pp2_.length; j++) {
			Port peer_ = pp2_[j];
			if (peer_ == null) continue;
			if (peer_.host == p_.host.parent) continue;
			if (p_.outwire != peer_.inwire) continue;
			//if (peer_.host.parent == p_.host.parent) 
				// add to peer only if parents of host are the same
				vpeers_.add(new Link(p_, peer_));
		}
		return vpeers_;
	}
	
	/**
	 Explores a component's neighbors in a hierarchical mannger.
	 Returns all the connections (in an array of {$link Link}s} emerged from 
	 the specified port.
	public static Link[] getLinks(Component target_)
	{
		Port[] pp_ = target_.getAllPorts();
		Link[][] lll_ = new Link[pp_.length][];
		int n_ = 0;
		for (int i=0; i<pp_.length; i++) {
			Port p_ = pp_[i];
			if (p_ == target_.infoPort) continue;
			if (p_.getType() == Port.PortType_IN) continue;
			Link[] ll_ = getLinks(p_);
			lll_[i] = ll_;
			n_ += ll_.length;
		}
		
		Link[] all_ = new Link[n_]; n_ = 0;
		for (int i=0; i<lll_.length; i++) {
			Link[] ll_ = lll_[i];
			if (ll_ == null) continue;
			System.arraycopy(ll_, 0, all_, n_, ll_.length);
			n_ += ll_.length;
		}
		return all_;
	}
	 */

	public static Link[] getLinks(Component target_)
	{
		LinkedList vpeers_ = _getLinks(target_, null);
		return (Link[])vpeers_.toArray(new Link[vpeers_.size()]);
	}
	
	static LinkedList _getLinks(Component target_, LinkedList vpeers_)
	{
		Port[] pp_ = target_.getAllPorts();
		for (int i=0; i<pp_.length; i++) {
			Port p_ = pp_[i];
			if (p_ == target_.infoPort) continue;
			if (p_.getType() == Port.PortType_IN) continue;
			vpeers_ = _getLinks(p_, vpeers_);
		}
		
		if (vpeers_ == null)
			return new LinkedList();
		else
			return vpeers_;
	}
	
	/**
	 Explores the topology seen from the specified component in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param transparent_ class/component to be transparent during exploration.
	 @return a hashtable which maps a component to its connections
	 	(<code>{@link Link}[]</code>).
	 */
	public static Hashtable explore(Component target_, Object transparent_)
	{
		Hashtable topology_ = _explore(target_, null, false);
		return adjust(topology_, new Object[]{transparent_}, null, false);
	}
	
	/**
	 Explores the topology seen from the specified component in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param transparent_ array of classes/components to be transparent during 
	 	exploration.
	 @return a hashtable which maps a component to its connections
	 	(<code>{@link Link}[]</code>).
	 */
	public static Hashtable explore(Component target_, Object[] transparent_)
	{
		Hashtable topology_ = _explore(target_, null, false);
		return adjust(topology_, transparent_, null, false);
	}
	
	/**
	 Explores the topology seen from the specified component in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable explore(Component target_, Object[] transparent_, 
									Object[] excluded_)
	{
		Hashtable topology_ = _explore(target_, excluded_, false);
		return adjust(topology_, transparent_, null, false);
	}

	/**
	 Explores the topology seen from the specified component in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable explore(Component target_, Object[] transparent_, 
									Object[] excluded_, boolean verbose_)
	{
		Hashtable topology_ = _explore(target_, excluded_, verbose_);
		return adjust(topology_, transparent_, null, verbose_);
	}
	
	/**
	 Explores the topology seen from the specified components in a hierarchical
	 mannger.
	 @param targets_ the specified components.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable explore(Component[] targets_,
					Object[] transparent_, Object[] excluded_, boolean verbose_)
	{
		Hashtable topology_ = _explore(targets_, excluded_, verbose_);
		return adjust(topology_, transparent_, null, verbose_);
	}
	
	/**
	 Explores the topology seen from the specified component in a "flat"
	 (as opposed to "hierarchical") mannger.
	 @param target_ the specified component.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @param stop_ components that are not further explored inside.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable exploreFlat(Component target_,
					Object[] transparent_, Object[] excluded_,
					Object[] stop_)
	{
		Hashtable topology_ = _exploreFlat(target_, excluded_, stop_, false);
		return adjust(topology_, transparent_, null, false);
	}
	
	/**
	 Explores the topology seen from the specified component in a "flat"
	 (as opposed to "hierarchical") mannger.
	 @param target_ the specified component.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @param stop_ components that are not further explored inside.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable exploreFlat(Component target_,
					Object[] transparent_, Object[] excluded_,
					Object[] stop_, boolean verbose_)
	{
		Hashtable topology_ = _exploreFlat(target_, excluded_, stop_, verbose_);
		return adjust(topology_, transparent_, null, verbose_);
	}
	
	/**
	 Explores the topology seen from the specified components in a "flat"
	 (as opposed to "hierarchical") mannger.
	 @param targets_ the specified components.
	 @param transparent_ array of classes to be transparent during exploration.
	 @param excluded_ is the array of classes to be excluded.
	 @param stop_ components that are not further explored inside.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	public static Hashtable exploreFlat(Component[] targets_,
					Object[] transparent_, Object[] excluded_,
					Object[] stop_, boolean verbose_)
	{
		Hashtable topology_ = _exploreFlat(targets_, excluded_, stop_,
						verbose_);
		return adjust(topology_, transparent_, null, verbose_);
	}
	
	/*
	 Explores the topology seen from the specified component in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param excludedObjs_ objects/classes to be excluded during exploration.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	static Hashtable _explore(Component target_, Object[] excludedObjs_,
					boolean verbose_)
	{
		Hashtable all_ = new Hashtable(); // component -> Links
		LinkedList todo_ = new LinkedList();
		todo_.add(target_);
		
		// separate excluded components and classes
		Hashtable htExcludedComponents_ = new Hashtable();
		Hashtable htExcludedClasses_ = new Hashtable();
		Hashtable httmp_ = null;
		if (excludedObjs_ != null) {
			for (int i=0; i<excludedObjs_.length; i++) {
				Object o_ = excludedObjs_[i];
				if (o_ instanceof Component) httmp_ = htExcludedComponents_;
				else if (o_ instanceof Class) httmp_ = htExcludedClasses_;
				else continue;
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		if (verbose_) {
			drcl.Debug.debug("Excluded components: " + htExcludedComponents_
							+ "\n");
			drcl.Debug.debug("Excluded classes: " + htExcludedClasses_ + "\n");
		}
		
		int loopcount_ = 0; // for reporting progress
		if (verbose_) drcl.Debug.debug("Exploring from " + target_ + "...\n");
		LinkedList tmp_ = new LinkedList();
		while (todo_.size() > 0) {
			Component c_ = (Component)todo_.getFirst();
			if (verbose_ && Math.random() < .1) {
				loopcount_++;
				if ((loopcount_ % 8) == 0) drcl.Debug.debug("\n");
				drcl.Debug.debug(c_.getID() + "(" + todo_.size() + ") ");
			}
			LinkedList vpeers_ = _getLinks(c_, null);
			todo_.removeFirst();
			tmp_.clear();
			//for (int i=0; i<ll_.length; i++) {
			for (Iterator it_ = vpeers_.iterator(); it_.hasNext(); ) {
				Link l_ = (Link)it_.next();
				Component hmm_ = l_.to.host;
				// what follows is the only part different from _exploreFlat()
				if (!contains(hmm_, target_)) {
					if (hmm_.parent == null || contains(hmm_.parent, target_))
						tmp_.add(l_);
				}
				else {
					if (!l_.to.isShadow() || !l_.to.anyClient())
						tmp_.add(l_);
				}
			}
			Link[] ll_ = (Link[])tmp_.toArray(new Link[tmp_.size()]);
			all_.put(c_, ll_);
			
			// examine all the neighbors
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				Component neighbor_ = l_.to.host;
				if (htExcludedComponents_.containsKey(neighbor_)
					|| htExcludedClasses_.containsKey(neighbor_.getClass()))
					ll_[i] = null;
				else if (!all_.containsKey(neighbor_)) {
					todo_.add(neighbor_);
					all_.put(neighbor_, neighbor_);
						// to eliminate duplicates in todo_
				}
			}
		}
		
		return all_;
	}
	
	/*
	 Explores the topology seen from the specified components in a hierarchical
	 mannger.
	 @param target_ the specified component.
	 @param excludedObjs_ objects/classes to be excluded during exploration.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	static Hashtable _explore(Component[] targets_, Object[] excludedObjs_,
					boolean verbose_)
	{
		if (targets_ == null || targets_.length == 0)
			return null;
		Hashtable all_ = new Hashtable(); // component -> Links
		LinkedList todo_ = new LinkedList();
		for (int i=0; i<targets_.length; i++)
			todo_.add(targets_[i]);
		
		// separate excluded components and classes
		Hashtable htExcludedComponents_ = new Hashtable();
		Hashtable htExcludedClasses_ = new Hashtable();
		Hashtable httmp_ = null;
		if (excludedObjs_ != null) {
			for (int i=0; i<excludedObjs_.length; i++) {
				Object o_ = excludedObjs_[i];
				if (o_ instanceof Component) httmp_ = htExcludedComponents_;
				else if (o_ instanceof Class) httmp_ = htExcludedClasses_;
				else continue;
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		if (verbose_) {
			drcl.Debug.debug("Excluded components: " + htExcludedComponents_
							+ "\n");
			drcl.Debug.debug("Excluded classes: " + htExcludedClasses_ + "\n");
		}
		
		int loopcount_ = 0; // for reporting progress
		if (verbose_)
			drcl.Debug.debug("Exploring from "
						+ drcl.util.StringUtil.toString(targets_) + "...\n");
		LinkedList tmp_ = new LinkedList();
		while (todo_.size() > 0) {
			Component c_ = (Component)todo_.getFirst();
			LinkedList vpeers_ = _getLinks(c_, null);
			if (verbose_) {// && Math.random() < .1) {
				loopcount_++;
				if ((loopcount_ % 8) == 0) drcl.Debug.debug("\n");
				drcl.Debug.debug(c_.getID() + "(" + todo_.size()
								+ "," + vpeers_.size() + ") ");
			}
			todo_.removeFirst();
			tmp_.clear();
			//for (int i=0; i<ll_.length; i++) {
			for (Iterator it_ = vpeers_.iterator(); it_.hasNext(); ) {
				Link l_ = (Link)it_.next();
				Component hmm_ = l_.to.host;
				int pass_ = 1; // 0: failed, 1: pass 1st, 2: ok
				for (int j=0; j<targets_.length; j++)
					if (contains(hmm_, targets_[j])) {
						if (!l_.to.isShadow() || !l_.to.anyClient())
							pass_ = 2;
						else
							pass_ = 0;
						break;
					}
				if (pass_ == 1) {
					pass_ = 0;
					for (int j=0; j<targets_.length; j++) {
						Component target_ = targets_[j];
						if (hmm_.parent == null
							|| contains(hmm_.parent, target_)) {
							pass_ = 2;
							break;
						}
					}
				}
				if (pass_ == 2)
					tmp_.add(l_);
			}
			Link[] ll_ = (Link[])tmp_.toArray(new Link[tmp_.size()]);
			all_.put(c_, ll_);
			
			// examine all the neighbors
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				Component neighbor_ = l_.to.host;
				if (htExcludedComponents_.containsKey(neighbor_)
					|| htExcludedClasses_.containsKey(neighbor_.getClass()))
					ll_[i] = null;
				else if (!all_.containsKey(neighbor_)) {
					todo_.add(neighbor_);
					all_.put(neighbor_, neighbor_);
						// to eliminate duplicates in todo_
				}
			}
		}
		
		return all_;
	}
	
	/*
	 Explores the topology seen from the specified component in a "flat"
	 (as opposed to "hierarchical") mannger.
	 @param target_ the specified component.
	 @param excludedObjs_ objects/classes to be excluded during exploration.
	 @param stop_ components/classes that are not further explored inside.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	static Hashtable _exploreFlat(Component target_, Object[] excludedObjs_,
					Object[] stop_, boolean verbose_)
	{
		Hashtable all_ = new Hashtable(); // component -> Links

		try {

		LinkedList todo_ = new LinkedList();
		todo_.add(target_);
		
		// separate excluded components and classes
		HashMap htExcludedComponents_ = new HashMap();
		HashMap htExcludedClasses_ = new HashMap();
		HashMap httmp_ = null;
		if (excludedObjs_ != null) {
			for (int i=0; i<excludedObjs_.length; i++) {
				Object o_ = excludedObjs_[i];
				if (o_ instanceof Component) httmp_ = htExcludedComponents_;
				else if (o_ instanceof Class) httmp_ = htExcludedClasses_;
				else continue;
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		// put stop_ to a hash table
		HashMap hmStopComponents_ = new HashMap();
		HashMap hmStopClasses_ = new HashMap();
		if (stop_ != null) {
			for (int i=0; i<stop_.length; i++) {
				Object o_ = stop_[i];
				if (o_ instanceof Component) httmp_ = hmStopComponents_;
				else if (o_ instanceof Class) httmp_ = hmStopClasses_;
				else continue;
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		if (verbose_) {
			drcl.Debug.debug("Excluded components: " + htExcludedComponents_
							+ "\n");
			drcl.Debug.debug("Excluded classes: " + htExcludedClasses_ + "\n");
			drcl.Debug.debug("Stop components: " + hmStopComponents_ + "\n");
			drcl.Debug.debug("Stop classes: " + hmStopClasses_ + "\n");
		}
		
		int loopcount_ = 0; // for reporting progress
		if (verbose_) drcl.Debug.debug("Exploring from " + target_ + "...\n");
		LinkedList tmp_ = new LinkedList();
		while (todo_.size() > 0) {
			Component c_ = (Component)todo_.getFirst();

			if (verbose_ && Math.random() < .1) {
				loopcount_++;
				if ((loopcount_ % 8) == 0) drcl.Debug.debug("\n");
				drcl.Debug.debug(c_.getID() + "(" + todo_.size() + ") ");
			}

			LinkedList vpeers_ = _getLinks(c_, null);
			_checkStopClasses(vpeers_, hmStopComponents_, hmStopClasses_);
			todo_.removeFirst();
			tmp_.clear();
			//for (int i=0; i<ll_.length; i++) {
			for (Iterator it_ = vpeers_.iterator(); it_.hasNext(); ) {
				Link l_ = (Link)it_.next();
				Component hmm_ = l_.to.host;
				// what follows is the only part different from other _explore()
				if (!l_.to.isShadow() || !l_.to.anyClient() ||
							hmStopComponents_.containsKey(hmm_))
					if (!contains(target_, hmm_))
						tmp_.add(l_);
			}
			Link[] ll_ = (Link[])tmp_.toArray(new Link[tmp_.size()]);
			all_.put(c_, ll_);
			
			// examine all the neighbors
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				Component neighbor_ = l_.to.host;
				if (htExcludedComponents_.containsKey(neighbor_)
					|| htExcludedClasses_.containsKey(neighbor_.getClass()))
					ll_[i] = null;
				else if (!all_.containsKey(neighbor_)) {
					todo_.add(neighbor_);
					all_.put(neighbor_, neighbor_);
						// to eliminate duplicates in todo_
				}
			}
		}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return all_;
	}
	
	/*
	 Explores the topology seen from the specified components in a "flat"
	 (as opposed to "hierarchical") mannger.
	 @param target_ the specified component.
	 @param excludedObjs_ objects/classes to be excluded during exploration.
	 @param stop_ components/classes that are not further explored inside.
	 @return a hashtable which maps a component to its connections (Link[]).
	 */
	static Hashtable _exploreFlat(Component[] targets_, Object[] excludedObjs_,
					Object[] stop_, boolean verbose_)
	{
		if (targets_ == null || targets_.length == 0)
			return null;
		Hashtable all_ = new Hashtable(); // component -> Links
		LinkedList todo_ = new LinkedList();
		for (int i=0; i<targets_.length; i++)
			todo_.add(targets_[i]);
		
		// separate excluded components and classes
		HashMap htExcludedComponents_ = new HashMap();
		HashMap htExcludedClasses_ = new HashMap();
		HashMap httmp_ = null;
		if (excludedObjs_ != null) {
			for (int i=0; i<excludedObjs_.length; i++) {
				Object o_ = excludedObjs_[i];
				if (o_ instanceof Component) httmp_ = htExcludedComponents_;
				else if (o_ instanceof Class) httmp_ = htExcludedClasses_;
				else continue;
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		// put stop_ to a hash table
		HashMap hmStopComponents_ = new HashMap();
		HashMap hmStopClasses_ = new HashMap();
		if (stop_ != null) {
			for (int i=0; i<stop_.length; i++) {
				Object o_ = stop_[i];
				if (o_ instanceof Component) httmp_ = hmStopComponents_;
				else if (o_ instanceof Class) httmp_ = hmStopClasses_;
				else continue;
				System.out.println("STOP: " + o_);
				if (!httmp_.containsKey(o_)) httmp_.put(o_, o_);
			}
		}

		if (verbose_) {
			drcl.Debug.debug("Excluded components: " + htExcludedComponents_
							+ "\n");
			drcl.Debug.debug("Excluded classes: " + htExcludedClasses_ + "\n");
			drcl.Debug.debug("Stop components: " + hmStopComponents_ + "\n");
			drcl.Debug.debug("Stop classes: " + hmStopClasses_ + "\n");
		}
		
		int loopcount_ = 0; // for reporting progress
		if (verbose_) drcl.Debug.debug("Exploring from " + targets_ + "...\n");
		LinkedList tmp_ = new LinkedList();
		while (todo_.size() > 0) {
			Component c_ = (Component)todo_.getFirst();
			if (verbose_ && Math.random() < .1) {
				loopcount_++;
				if ((loopcount_ % 8) == 0) drcl.Debug.debug("\n");
				drcl.Debug.debug(c_.getID() + "(" + todo_.size() + ") ");
			}
			LinkedList vpeers_ = _getLinks(c_, null);
			_checkStopClasses(vpeers_, hmStopComponents_, hmStopClasses_);
			todo_.removeFirst();
			tmp_.clear();
			//for (int i=0; i<ll_.length; i++) {
			for (Iterator it_ = vpeers_.iterator(); it_.hasNext(); ) {
				Link l_ = (Link)it_.next();
				Component hmm_ = l_.to.host;

				if (!l_.to.isShadow() || !l_.to.anyClient() ||
							hmStopComponents_.containsKey(hmm_)) {
					boolean pass_ = true;
					for (int j=0; j<targets_.length; j++)
						if (contains(targets_[j], hmm_)) {
							pass_ = false;
							break;
						}
					if (pass_)
						tmp_.add(l_);
				}
			}
			Link[] ll_ = (Link[])tmp_.toArray(new Link[tmp_.size()]);
			all_.put(c_, ll_);
			
			// examine all the neighbors
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				Component neighbor_ = l_.to.host;
				if (htExcludedComponents_.containsKey(neighbor_)
					|| htExcludedClasses_.containsKey(neighbor_.getClass()))
					ll_[i] = null;
				else if (!all_.containsKey(neighbor_)) {
					todo_.add(neighbor_);
					all_.put(neighbor_, neighbor_);
						// to eliminate duplicates in todo_
				}
			}
		}
		
		return all_;
	}

	/*
	 * Removes components from ll that are inside "stop" components
	 * (components in ll that are in one of the stop classes in hmStopClass_)
	 * @param ll	"Links" under consideration 
	 * @param hmStopComponent_  Component as key 
	 * @param hmStopClass_  Class as key 
	 */
	private static void _checkStopClasses(LinkedList ll,
					HashMap hmStopComponent_, HashMap hmStopClass_)
	{
		if (hmStopClass_.isEmpty() && hmStopComponent_.isEmpty()) return;

		// find out all stop components first
		for (Iterator it_ = ll.iterator(); it_.hasNext(); ) {
			Link l = (Link)it_.next();
			Component hmm_ = l.to.host;
			if (hmStopClass_.containsKey(hmm_.getClass()))
				hmStopComponent_.put(hmm_, hmm_);
		}

		// check if hmm is covered by one of the stop components
		// if it is, remove it from the list 
		for (Iterator it_ = ll.iterator(); it_.hasNext(); ) {
			Link l = (Link)it_.next();
			Component hmm_ = l.to.host;

			boolean covered_ = false; 
			Component parent_ = hmm_.getParent();
			while (parent_ != null && !covered_) {
				if (hmStopComponent_.containsKey(parent_))
					covered_ = true;
				else
					parent_ = parent_.getParent();
			}
			//System.out.println("CHECK: " + hmm_ + ": " +
			//				(covered_? "": "not ") + "covered");

			if (covered_) it_.remove();
		}
	}
	
	/**
	 Adjusts the explored topology by making the components of the specified
	 class/component transparent.
	 @param topology_ is a hashtable mapping from a component to
	 	<code>{@link Link}[]<code>.
	 @param transparent_ is the class/component to be treated transparently.
	 @return the new topology.
	 */
	public static Hashtable adjust(Hashtable topology_, Object transparent_)
	{ return adjust(topology_, new Object[]{transparent_}, null, false); }
	
	/**
	 Adjusts the explored topology by making the components of the specified 
	 class/component transparent.
	 @param topology_ is a hashtable mapping from a component to
	 	<code>{@link Link}[]<code>.
	 @param transparentObjs_ is the array of classes to be treated 
	 	transparently.
	 @param excludedObjs_ is the array of classes/components to be excluded.
	 @return the new topology.
	 */
	public static Hashtable adjust(Hashtable topology_,
					Object[] transparentObjs_, Object[] excludedObjs_)
	{ return adjust(topology_, transparentObjs_, excludedObjs_, false); }

	/**
	 Adjusts the explored topology by making the components of the specified 
	 class/component transparent.
	 @param topology_ is a hashtable mapping from a component to
	 	<code>{@link Link}[]<code>.
	 @param transparentObjs_ is the array of classes to be treated
	 	transparently.
	 @param excludedObjs_ is the array of classes/components to be excluded.
	 @return the new topology.
	 */
	public static Hashtable adjust(Hashtable topology_,
					Object[] transparentObjs_, Object[] excludedObjs_,
					boolean verbose_)
	{
		if (topology_ == null || topology_.size() == 0 
			|| (transparentObjs_ == null || transparentObjs_.length == 0)
			&& (excludedObjs_ == null || excludedObjs_.length == 0))
			return topology_;
		
		// move all the transparent components from topology_ to htTransparent_
		Hashtable htTransparent_ = new Hashtable(); // Component -> Link[]
		if (transparentObjs_ != null && transparentObjs_.length > 0)
		{
			Class[] cc_ = new Class[transparentObjs_.length];
			int j = 0;
			for (int i=0; i<transparentObjs_.length; i++) {
				Object o_ = transparentObjs_[i];
				if (o_ == null) continue;
				if (o_ instanceof Component) {
					if (topology_.containsKey(o_)) 
						htTransparent_.put(o_, topology_.remove(o_));
				}
				else if (o_ instanceof Class) cc_[j++] = (Class)o_;
			}
			if (j > 0) // # of classes
				for (Enumeration e_ = topology_.keys(); e_.hasMoreElements();) {
					Component c_ = (Component)e_.nextElement();
					for (int i=0; i<j; i++) {
						if (cc_[i] == null) continue;
						if (cc_[i].isInstance(c_)) {
							htTransparent_.put(c_, topology_.remove(c_));
							break;
						}
					}
				}
		}
		
		// move all the excluded components from topology_ to htExcluded_
		Hashtable htExcluded_ = new Hashtable(); // Component -> Link[]
		if (excludedObjs_ != null && excludedObjs_.length > 0)
		{
			Class[] cc_ = new Class[excludedObjs_.length];
			int j = 0; // # of classes
			for (int i=0; i<excludedObjs_.length; i++) {
				Object o_ = excludedObjs_[i];
				if (o_ == null) continue;
				if (o_ instanceof Component) {
					if (topology_.containsKey(o_)) 
						htExcluded_.put(o_, topology_.remove(o_));
				}
				else if (o_ instanceof Class) cc_[j++] = (Class)o_;
			}
			if (j > 0)
				for (Enumeration e_ = topology_.keys(); e_.hasMoreElements();) {
					Component c_ = (Component)e_.nextElement();
					for (int i=0; i<j; i++)
						if (cc_[i].isInstance(c_)) {
							htExcluded_.put(c_, topology_.remove(c_));
							break;
						}
				}
		}
		if (htTransparent_.size() == 0 && htExcluded_.size() == 0)
			return topology_;
		if (htExcluded_.size() == 0) htExcluded_ = null;
		
		if (verbose_) drcl.Debug.debug("\nWorking on transparent components("
			+ htTransparent_.size() + ")...\n");
		int loopcount_ = 0; // for reporting progress
		int count_ = 0; // for reporting progress

		// work on the transparent components so that their connections do not
		// end up with other transparent components.
		Enumeration elements_ = htTransparent_.elements();
		for (Enumeration e_ = htTransparent_.keys(); e_.hasMoreElements(); ) {
			if (verbose_) {
				count_++;
				if (Math.random() < .1) {
					if ((++loopcount_ % 15) == 0) drcl.Debug.debug("\n");
					drcl.Debug.debug(count_ + " ");
				}
			}

			// work on c_'s connections
			Component c_ = (Component)e_.nextElement();
			Link[] ll_ = (Link[])elements_.nextElement();
			LinkedList v_ = new LinkedList();
				// stores connections to nontransparent components
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				if (l_ == null) continue;
				if (htExcluded_ != null && htExcluded_.containsKey(l_.to.host))
					continue;
				else if (htTransparent_.containsKey(l_.to.host)) {
					// expand l_ until components reached are all
					// nontransparent
					Hashtable htTraversed_ = new Hashtable();
						// transparent components traversed
					htTraversed_.put(c_, c_);
						// put myself and neighbor first, 2nd argument not used
					htTraversed_.put(l_.to.host, c_); // 2nd argument not used
					LinkedList vReached_ = new LinkedList();
						// transparent components reached
					vReached_.add(l_.to.host);
					// expand until all the reached transparent components
					// are exhausted
					while (!vReached_.isEmpty()) {
						Component c2_ = (Component)vReached_.removeLast();
						Link[] tmp_ = (Link[])htTransparent_.get(c2_);
						for (int j=0; j<tmp_.length; j++) {
							if (tmp_[j] == null) continue;
							Component c3_ = tmp_[j].to.host;
							if (htTraversed_.containsKey(c3_)) continue;
							else if (htExcluded_ != null
									&& htExcluded_.containsKey(c3_)) continue;
							else if (htTransparent_.containsKey(c3_)) {
								vReached_.add(c3_);
								htTraversed_.put(c3_, c_/*dont care*/);
							}
							else {
								Component nexthop_ = tmp_[j].nextHop;
								if (nexthop_ == null
									|| !htTraversed_.containsKey(nexthop_))
									v_.add(new Link(l_.from, l_.to.host,
															tmp_[j].to));
							}
						}
					}
				}
				else {
					v_.add(l_);
				}
			}
			ll_ = (Link[])v_.toArray(new Link[v_.size()]);
			htTransparent_.put(c_, ll_);
		}

		if (verbose_)
			drcl.Debug.debug("\nWorking on nontransparent components...\n");
		// Now work on the nontransparent components so that their connections
		// do not end up with transparent components.
		elements_ = topology_.elements();
		for (Enumeration e_ = topology_.keys(); e_.hasMoreElements(); ) {
			// work on c_'s connections
			Component c_ = (Component)e_.nextElement();
			Link[] ll_ = (Link[])elements_.nextElement();
			LinkedList v_ = new LinkedList();
				// stores connections to nontransparent components
			for (int i=0; i<ll_.length; i++) {
				Link l_ = ll_[i];
				if (l_ == null) continue;
				if (htExcluded_ != null && htExcluded_.containsKey(l_.to.host))
					continue;
				else if (htTransparent_.containsKey(l_.to.host)) {
					Link[] tmp_ = (Link[])htTransparent_.get(l_.to.host);
					// Links in tmp_ should be all connected to nontransparent
					// components
					for (int j=0; j<tmp_.length; j++) {
						Component c3_ = tmp_[j].to.host;
						if (c3_ != c_)
							v_.add(new Link(l_.from, tmp_[j].to));
					}
				}
				else {
					v_.add(l_);
				}
			}
			ll_ = (Link[])v_.toArray(new Link[v_.size()]);
			topology_.put(c_, ll_);
		}
		
		return topology_;
	}
	// end of adjust()

	/** Returns the report of the lock information in the component. */
	public static String lockinfo(Component c_)
	{
		if (c_ == null) return "";

		if (c_.locks == null)
			return "No locked object.";
		else 
			return "Locked objects:\n" + c_.locks.printAll();
	}

	/** Returns all the properties of the component. */
	public static String getAllProperties(Component c_)
	{ return null; }

	/** Returns all the properties of the component/object. */
	public static PropertyDescriptor[] getAllPropertyDescriptors(Object c_)
	{
		try {
			BeanInfo info_ = null;
			if (Component.class.isAssignableFrom(c_.getClass()))
				info_ =Introspector.getBeanInfo(c_.getClass(), Component.class);
			else
				info_ = Introspector.getBeanInfo(c_.getClass());
			return info_.getPropertyDescriptors();
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return null;
		}
	}

	/** Returns the property of the component in String. */
	public static String getProperty(Component c_, String pname_)
	{ return null; }
	
	/** Makes the <code>java.beans.PropertyDescriptor</code>s for the class. */
	public static PropertyDescriptor[] makePropertyDescriptors(
					String[] properties_, Class beanClass_, Class refClass_)
	{
		try {
			if (properties_ == null) properties_ = new String[0];
			PropertyDescriptor[] pds_ =
				Introspector.getBeanInfo(refClass_).getPropertyDescriptors();
			if (pds_ == null) pds_ = new PropertyDescriptor[0];
			PropertyDescriptor[] new_ = new PropertyDescriptor[pds_.length
											+ properties_.length];
			for (int i=0; i<pds_.length; i++)
				new_[i] = new PropertyDescriptor(pds_[i].getName(), beanClass_);
			for (int i=0; i<properties_.length; i++)
				new_[i + pds_.length] = new PropertyDescriptor(properties_[i],
								beanClass_);
			return new_;
		}
		catch (IntrospectionException e_) {
			drcl.Debug.error("drcl.comp.Util.makePropertyDescriptors()", e_);
			return null;
		}
	}

	/** Returns the number of components inside <code>root_</code>. */
	public static long count(Component root_)
	{
		Component[] all_ = root_.getAllComponents();
		if (all_ == null) return 0;
		long count_ = all_.length;
		for (int i=0; i< all_.length; i++)
			count_ += count(all_[i]);
		return count_;
	}

	public static void optimize(Component root_)
	{
		Component[] cc_ = root_.getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			Component c_ = cc_[i];
			Port[] pp_ = c_.getAllPorts();
			for (int j=0; j<pp_.length; j++)
				pp_[j].setExecutionBoundary(false);
			optimize(c_);
		}
	}

	/* * Optimizes the component hierarchy for better performance
	    in expense of disabling trace messages.
	public static void optimize(Component root_)
	{
		OPTIMIZED = true;
		Wire[] wires_ = getAllWires(root_);
		for (int i=0; i<wires_.length; i++) {
			Wire w = wires_[i];
			if (w.inports != null) {
				Port[] pp = w.inports;
				int count_ = 0;
				for (int j=0; j<pp.length; j++)
					if (pp[j] != null && !pp[j].isShadow())
						count_++;
				if (count_ < pp.length) {
					Port[] tmp_ = new Port[count_];
					count_ = 0;
					for (int j=0; j<pp.length; j++)
						if (pp[j] != null && !pp[j].isShadow())
							tmp_[count_++] = pp[j];
					w.inports = tmp_;
				}
			}
			if (w.outports != null) {
				// same codes from above except 'inports' -> 'outports'
				Port[] pp = w.outports;
				int count_ = 0;
				for (int j=0; j<pp.length; j++)
					if (pp[j] != null && !pp[j].isShadow())
						count_++;
				if (count_ < pp.length) {
					Port[] tmp_ = new Port[count_];
					count_ = 0;
					for (int j=0; j<pp.length; j++)
						if (pp[j] != null && !pp[j].isShadow())
							tmp_[count_++] = pp[j];
					w.outports = tmp_;
				}
			}
		}
	}
	*/

	/** Returns all wires in the component hierarchy. */
	public static Wire[] getAllWires(Component root_)
	{
		Hashtable htWires_ = new Hashtable();
		LinkedList vWires_ = new LinkedList();

		LinkedList remaining_ = new LinkedList();
		remaining_.add(root_);
		while (!remaining_.isEmpty()) {
			Component c = (Component)remaining_.removeLast();
			Component[] child_ = c.getAllComponents();
			if (child_ != null)
				for (int i=0; i<child_.length; i++)
					remaining_.add(child_[i]);

			Wire[] wires_ = c.getAllWiresOut();
			if (wires_ != null)
				for (int i=0; i<wires_.length; i++)
					if (!htWires_.containsKey(wires_[i])) {
						htWires_.put(wires_[i], wires_[i]);
						vWires_.add(wires_[i]);
					}
		}
		Wire[] tmp_ = (Wire[])vWires_.toArray(new Wire[vWires_.size()]);
		return tmp_;
	}

	/** Returns all components in the component hierarchy, including
	 * <code>root_</code>. */
	public static Component[] getAllComponents(Component root_)
	{
		LinkedList all_ = new LinkedList();
		LinkedList remaining_ = new LinkedList();
		remaining_.add(root_);
		while (!remaining_.isEmpty()) {
			Component c = (Component)remaining_.removeLast();
			all_.add(c);
			Component[] child_ = c.getAllComponents();
			if (child_ != null)
				for (int i=0; i<child_.length; i++)
					remaining_.add(child_[i]);
		}
		Component[] tmp_ = (Component[])all_.toArray(
						new Component[all_.size()]);
		return tmp_;
	}

	/**
	 Component property tester.
	 Given component class and the property name, it returns the default value
	 of the property in String.
	 */
	public static void main(String[] args)
	{
		if (args == null || args.length < 2) {
			System.out.println("Util <component_class_name> <property_name>");
			return;
		}
		String property_ = args[1];
		System.out.println("Component class: " + args[0]);
		System.out.println("Property   name:   " + property_);
		
		try {
			Object o_ = Class.forName(args[0]).newInstance();
			if (!(o_ instanceof Component)) {
				System.out.println(args[0] + " is not a component!");
				return;
			}
			Component c_ = (Component)o_;
			System.out.println("info():\n" + c_.info());
			System.out.println(property_ + ": " + getProperty(c_, property_));
		}
		catch (Exception e_) {
			System.out.println(e_);
		}
	}
}



