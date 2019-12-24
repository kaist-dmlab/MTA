// @(#)NodeBuilder.java   1/2004
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

/**
The container class for automating the process of constructing a {@link Node network node}.

<p>This class has a set of <code>build(...)</code> methods to build the internal
structure of nodes.  But before using this class to build other nodes, one needs
to build the NodeBuilder first.  Building a NodeBuilder is no difference from
building a real node (i.e., adding components and connecting them).
But since the relation between the modules in the upper protocol layer (UPL) and
the core service layer (CSL) is defined, we can further automate the process by
having a set of <i>port naming rules</i> for a protocol:</p>

  <table border="1">
    <tr>
      <th nowrap colspan="2">Rules</th>
      <th nowrap>How NodeBuilder Behaves</th>
    </tr>
    <tr>
      <td nowrap><i>If a protocol wants to have&nbsp;<br>
        (service), or listen to (event)...</i></td>
      <td valign="top" nowrap><i>then it must have (port)</i></td>
      <td nowrap><i>when sees this port, NodeBuilder<br>
        connects it with (port) of CSL</i></td>
    </tr>
    <tr>
      <td nowrap>data services</td>
      <td nowrap>down@</td>
      <td nowrap>&lt;protocol ID&gt;@up</td>
    </tr>
    <tr>
      <td nowrap>packet arrival events</td>
      <td nowrap>.pktarrival@</td>
      <td nowrap>.pktarrival@</td>
    </tr>
    <tr>
      <td nowrap>identity services</td>
      <td nowrap>.service_id@</td>
      <td nowrap>.service_id@</td>
    </tr>
    <tr>
      <td nowrap>identity events</td>
      <td nowrap>.id@</td>
      <td nowrap>.id@</td>
    </tr>
    <tr>
      <td nowrap>routing table services</td>
      <td nowrap>.service_rt@</td>
      <td nowrap>.service_rt@</td>
    </tr>
    <tr>
      <td nowrap>unicast route query</td>
      <td nowrap>.ucastquery@</td>
      <td nowrap>.ucastquery@</td>
    </tr>
    <tr>
      <td nowrap>multicast route query</td>
      <td nowrap>.mcastquery@</td>
      <td nowrap>.mcastquery@</td>
    </tr>
    <tr>
      <td nowrap>unicast route events</td>
      <td nowrap>.rt_ucast@</td>
      <td nowrap>.rt_ucast@</td>
    </tr>
    <tr>
      <td nowrap>multicast route events</td>
      <td nowrap>.rt_mcast@</td>
      <td nowrap>.rt_mcast@</td>
    </tr>
    <tr>
      <td nowrap>interface/neighbor services</td>
      <td nowrap>.service_if@</td>
      <td nowrap>.service_if@</td>
    </tr>
    <tr>
      <td nowrap>neighbor events (physical)</td>
      <td nowrap>.if@</td>
      <td nowrap>.if@</td>
    </tr>
    <tr>
      <td nowrap>neighbor events (virtual)</td>
      <td nowrap>.vif@</td>
      <td nowrap>.vif@</td>
    </tr>
    <tr>
      <td nowrap>packet filter configuration service</td>
      <td nowrap>.configswitch@</td>
      <td nowrap>.configswitch@</td>
    </tr>
  </table>

<p>If a protocol follows the rules, then we can simply add the protocol to
NodeBuilder and NodeBuilder will take care of the connections between
the protocol and CSL.  At the end of <code>build(...)</code>,
<code>NodeBuilder</code> uses CSL builder to build the internal structure of
CSL. If CSL builder is not specified in the <code>build(...)</code> method,
the default CSL builder is used. 

<h4>Node Map</h4>
<p>If the upper-protocol-layer modules in a node forms a layered protocol
stack, then one may use a <i>node map</i> to further ease the task of building
a <code>NodeBuilder</code>.
A node map is a plain text which describes the layering relation between
modules.&nbsp; For example, the node map for a UDP application laying on top of
UDP at port 1100 is as follows:</p>

<blockquote>
<pre>udp	17/csl		drcl.inet.transport.UDP
app	1100/udp	Application_Class</pre>
</blockquote>

<p>And then we can use the {@link #loadmap(String)} method
to load a node map into a <code>NodeBuilder</code>.&nbsp; Each line in a node
map corresponds to a module.  The syntax is as follows:</p>

<blockquote>
<pre>&lt;module_id&gt;	&lt;port&gt;/&lt;lower_layer_module_id&gt;	&lt;module_class&gt;</pre>
</blockquote>

<p>After parsing a line, <code>NodeBuilder</code> creates the module and
connects the <code>down@</code> port of the module to the <code>&lt;port&gt;@up</code>
port of the lower layer module.&nbsp; If the lower layer module does not
multiplex/demultiplex, then <code>&lt;port&gt;</code> should be given by &quot;<code>-</code>&quot;
(the &quot;minus&quot; sign).&nbsp; In this case, <code>NodeBuilder</code>
connects the <code>down@</code> port of the module to the <code>up@</code> port
of the lower layer module.</p>

<p>The current implementation of <code>NodeBuilder</code> requires that the
lower layer module be defined ahead. The core service layer (<code>csl</code>)
is pre-defined.&nbsp; One may use it as lower layer module without defining it.</p>
<p>If the lower layer module is the core service layer (<code>csl</code>), then
we may omit the <code>&lt;port&gt;/csl</code> field if the protocol is known to
INET. (INET recognizes many Internet standard protocols such as TCP, UDP, IGMP,
DV, OSPF and so on).&nbsp;</p>

<p>There are four <code>build(...)</code> methods in <code>NodeBuilder</code>
for including a node map as an argument:</p>
<pre>public void <b>build</b>(Object c_, String nodemap_);
public void <b>build</b>(Object[] cc_, String nodemap_);
public void <b>build</b>(Object c_, CSLBuilder cb_, String nodemap_);
public void <b>build</b>(Object[] cc_, CSLBuilder cb_, String nodemap_);</pre>
<p>These methods call <code>loadmap(String)</code> first and then calls the
corresponding <code>build(...)</code> methods without the node map argument.</p>
<p>Note that loading a node map overrides the existing structure in the <code>NodeBuilder</code>.&nbsp;
One may use the same <code>NodeBuilder</code> instance to build different types
of nodes with different node maps.

<h4>NodeBuilder Properties</h4>
<p>Some properties can be set in the <code>NodeBuilder</code>.
When the <code>build(...)</code> method is called, these properties are conveyed
and set in the nodes being built. It is
particularly useful when one wishes to set these properties that are common to
the nodes being built. The following table lists the properties defined in the
this class:</p>

  <table border="1">
    <tr>
      <th valign="top" nowrap>Property</th>
      <th nowrap valign="top">&quot;Set&quot; Method</th>
      <th valign="top">Description</th>
    </tr>
    <tr>
      <td valign="top" nowrap>interface bandwidth</td>
      <td nowrap valign="top">{@link #setBandwidth(double)}</td>
      <td valign="top">Sets the bandwidth (in bps) of interfaces. The default
        value applies if non-positive value is given.</td>
    </tr>
    <tr>
      <td valign="top" nowrap>interface buffer size</td>
      <td nowrap valign="top">{@link #setBufferSize(int)}</td>
      <td valign="top">Sets the buffer size (in bytes or packets, depending on
        the buffer mode) of interfaces. The default value applies if
        non-positive value is given.</td>
    </tr>
    <tr>
      <td valign="top" nowrap>interface buffer mode</td>
      <td nowrap valign="top">{@link #setBufferMode(String mode_)}</td>
      <td valign="top">Sets the buffer mode. The <code>mode_ </code>argument
        could be &quot;byte&quot; or &quot;packet&quot;.</td>
    </tr>
    <tr>
      <td valign="top" nowrap>interface maximum&nbsp;<br>
 transmission unit<br>
        (MTU)</td>
      <td nowrap valign="top">{@link #setMTU(int)}</td>
      <td valign="top">Sets the MTU (in bytes) of interfaces, including the size
        of the packet header (if there is any) for link transimission. The
        default value applies if negative value is given. Giving zero turns off
        MTU check (e.g., fragmentation in CSL).</td>
    </tr>
  </table>

<h4>Service and Event Ports</h4>
<p>In addition to relying on <code>NodeBuilder</code> to create service
and event ports at CSL on demands (of UPL modules), one can command
<code>NodeBuilder</code> to create some or all of those ports, including
&quot;up&quot; ports.
To make it happen, one simply adds the service and event ports to the
<code>NodeBuilder</code>, and then, when building a node, the NodeBuilder
duplicates all the service and event ports on it at the CSL created for the
node.</p>

@see CoreServiceLayer Service and Event Ports at CSL
@see CSLBuilder
 */
public class NodeBuilder extends Component implements InetConstants
{
	public NodeBuilder()
	{ super(); }

	public NodeBuilder(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		NodeBuilder that_ = (NodeBuilder)source_;
		bw = that_.bw;
		bs = that_.bs;
		mtu = that_.mtu;
		bufferMode = that_.bufferMode;
	}
	
	public String info()
	{
		String mtu_ = mtu > 0? String.valueOf(mtu): mtu == 0? "disabled":
			"default value (depending on CSL implementation)";
		return   "   Bandwidth = " + (bw > 0.0? String.valueOf(bw):
					"default value (depending on CSL implementation)") + "\n"
			   + " Buffer mode : " + bufferMode + "\n"
			   + " Buffer size = " + (bs > 0.0? String.valueOf(bs):
			   		"default value (depending on CSL implementation)") + "\n"
			   + "Fragmentation: " + mtu_ + "\n"
			   + _printPID()
			   + (map == null? "": "----------\nlast parsed map:\n" + map);
	}

	String _printPID()
	{
		if (htPID == null) return "";
		StringBuffer sb_ = new StringBuffer();
		for (Enumeration e_ = htPID.keys(); e_.hasMoreElements(); ) {
			String name_ = (String)e_.nextElement();
			Object pid_ = htPID.get(name_);
			sb_.append("    " + name_ + "   " + pid_ + "\n");
		}
		if (sb_.length() > 0) return "PIDs:\n" + sb_;
		else return "";
	}

	/**
	 * Extracts the structure of an existing Node to the builder.
	 */
	public void extract(Node node_)
	{
		if (node_ == null) return;
		addComponent(new Component(ID_CSL));
		iduplicate(node_);
		/*
		Component[] tmp_ = node_.getAllComponents();
		Vector v_ = new Vector(); // protocols
		for (int i=0; i<tmp_.length; i++) {
			Component c_ = tmp_[i];
			if (c_ instanceof Protocol) v_.addElement(c_);
			// ignore other components
		}
		for (int i=0; i<v_.size(); i++) {
			Component c_ = (Component)v_.elementAt(i);
			removeComponent(c_.getID());
			addComponent((Component)c_.clone());
		}
		*/
	}
	
	/** Builds up the node structure inside the specified containers.
	The method calls <code>build(cc_, (CSLBuilder)null)</code>.
	@see #build(Object[], CSLBuilder) */
	public void build(Object[] cc_)
	{ build(cc_, (CSLBuilder)null); }

	/** Builds up the node structure inside the specified containers with a
	node map.
	The method calls <code>build(cc_, null, map_)</code>.
	@see #build(Object[], CSLBuilder, String) */
	public void build(Object[] cc_, String map_)
	{ build(cc_, null, map_); }

	/** Builds up the node structure inside the specified containers
	together with the CSL builder and the node map.
	This method first calls <code>loadmap(map_)</code> to create protocols and
	applications, and then <code>build(cc_, cb_)</code> to build the nodes.
	@param cc_ array of components in which the node structure is constructed.
		Components other than <code>drcl.inet.Node</code> are omitted processed.
	@param cb_ the CSL builder.
	@param map_ the node map.
	@see #loadmap(String)
	@see #build(Object[], CSLBuilder) */
	public void build(Object[] cc_, CSLBuilder cb_, String map_)
	{
		// create components according to map_
		loadmap(map_);
		build(cc_, cb_);
	}

	/** Loads the node map into this node builder.
	Appropriate components and connections are established as the map instructs.
   	*/
	public void loadmap(String map_)
	{
		// create components according to map_
		if (map_ != null && map_ != map) try {
			removeAll();
			if (htPID != null)
				htPID.clear();
			map = map_;
			java.io.LineNumberReader reader_ =
				new java.io.LineNumberReader(new java.io.StringReader(map_));
			while (true) {
				String line_ = reader_.readLine();
				if (line_ == null) break;
				line_ = line_.trim();
				if (line_.length() == 0) continue;
				if (line_.charAt(0) == '#') continue;
				//System.out.println("parse " + line_);
				// format: <id>    [<port#>/<llp_id>]    [<class_name>]
				String[] ss_ = drcl.util.StringUtil.substrings(line_, " \t");
				Component c_ = getComponent(ss_[0]);
				if (c_ == null) {
					String className_ = ss_.length > 2? ss_[2]: ss_[1];
					try {
						c_ = (Component)Class.forName(className_).newInstance();
						c_.setID(ss_[0]);
						addComponent(c_);
					}
					catch (Exception e_) {
						if (className_.indexOf("/") >= 0)
							drcl.Debug.debug(ss_[0] + " does not exist.\n");
						else
							e_.printStackTrace();
						drcl.Debug.debug("Building process stops.\n");
						return;
					}
				}
				// parse port# and llp_id
				if (ss_.length < 2 || ss_[1].indexOf("/") < 0) {
					// well-defined protocol above CSL
					if (InetUtil.getPID(c_) < 0) {
						if (isDebugEnabled())
							debug("ProtocolID is not provided for "
									+ c_.getName() + " to connect to CSL; "
									+ "suppose it's an application or "
									+ "a standalone component");
						continue; // ignored
					}
					else {
						//do nothing, well-defined protocol is connected later
					}
				}
				else {
					int i = ss_[1].indexOf("/");
					int port_ = ss_[1].charAt(0) == '-'?
							-1: Integer.parseInt(ss_[1].substring(0,i));
					String llpid_ = ss_[1].substring(i+1);
					if (llpid_.equals(ID_CSL)) {
						// protocol above CSL
						int defaultPID_ = InetUtil.getPID(c_);
						if (defaultPID_ != port_) {
							//System.out.println(c_ + " on port " + port_ + ", default port=" + defaultPID_);
							if (defaultPID_ >= 0
								&& c_.getID().equals(c_.getName()))
								drcl.Debug.debug("* warning * the supplied PID "
									+ port_ + " overrides the default one "
									+ defaultPID_ + ".\n");
							if (htPID == null)
								htPID = new Hashtable();
							htPID.put(c_.getID(), new Integer(port_));
						}
					}
					else {
						// application
						Component llp_ = getComponent(llpid_);
						if (llp_ == null) {
							// XX: can do better than this...
							drcl.Debug.debug(llpid_ + " not found for " + ss_[0]
											+ ".\nBuilding process stops.\n");
							return;
						}
						if (port_ >= 0)
							c_.addPort(Module.PortGroup_DOWN).connect(
											llp_.addPort(Module.PortGroup_UP,
											String.valueOf(port_)));
						else
							c_.addPort(Module.PortGroup_DOWN).connect(
											llp_.addPort(Module.PortGroup_UP));
					}
				}
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			drcl.Debug.debug("Building process stops.\n");
			return;
		}
	}

	String map; // save last map to avoid parsing the same map
	Hashtable htPID = null; // ID --> PID, 

	int _getPID(Component protocol_)
	{
		if (htPID != null) {
			Object tmp_ = htPID.get(protocol_.getID());
			if (tmp_ != null) return ((Integer)tmp_).intValue();
		}
		return InetUtil.getPID(protocol_);
	}

	/** Builds up the node structure inside the specified containers
	together with the CSL builder.
	If <code>cb_</code> is <code>null</code>, then this method finds the
	CSL builder inside this node builder as a child component with ID "csl".
	If no CSL builder exists, the default CSL builder 
	(<code>CSLBuilder.DEFAULT_BUILDER</code>) is used.
	@param cc_ array of components in which the node structure is constructed.
		Components other than <code>drcl.inet.Node</code> are omitted processed.
	@param cb_ the CSL builder.
	@see CSLBuilder
	 */
	public void build(Object[] cc_, CSLBuilder cb_)
	{
		try {
		if (cb_ == null) {
			Component tmp_ = getComponent(ID_CSL);
			if (tmp_ instanceof CSLBuilder)
				cb_ = (CSLBuilder)getComponent(ID_CSL);
			else
				cb_ = CSLBuilder.DEFAULT_BUILDER;
		}
		if (cc_ == null) return;
		
		// Bandwidth and buffer size
		// The settings in NodeBuilder overrides the settings in CSLBuilder
		if (bw > 0.0) cb_.setBandwidth(bw);
		if (bs > 0.0) cb_.setBufferSize(bs);
		if (mtu >= 0) cb_.setMTU(mtu);
		cb_.setBufferMode(bufferMode);
		cb_.setLinkEmulationEnabled(linkEmu);
		if (linkEmu)
			cb_.setLinkPropDelay(linkPropDelay);

		// CoreServiceLayer
		Component csl_ = addCSL(this, cb_);
		Port[] pp_ = new Port[]{
			getPort(EVENT_ID_CHANGED_PORT_ID),
			getPort(EVENT_RT_UCAST_CHANGED_PORT_ID),
			getPort(EVENT_RT_MCAST_CHANGED_PORT_ID),
			getPort(EVENT_IF_PORT_ID),
			getPort(EVENT_VIF_PORT_ID),
			getPort(EVENT_PKT_ARRIVAL_PORT_ID),
			getPort(EVENT_MCAST_HOST_PORT_ID),
			getPort(SERVICE_ID_PORT_ID),
			getPort(SERVICE_RT_PORT_ID),
			getPort(SERVICE_IF_PORT_ID),
			getPort(SERVICE_CONFIGSW_PORT_ID),
			getPort(SERVICE_MCAST_PORT_ID),
			getPort(UCAST_QUERY_PORT_ID),
			getPort(MCAST_QUERY_PORT_ID)
		};
		for (int i=0; i<pp_.length; i++) {
			Port p_ = pp_[i];
			if (p_ != null)
				csl_.addPort((Port)p_.clone(), p_.getGroupID(), p_.getID());
		}

		Port[] up_ = getAllPorts(Module.PortGroup_UP);
		if (up_ != null) {
			for (int i=0; i<up_.length; i++)
				csl_.addPort(Module.PortGroup_UP, up_[i].getID());
		}
		
		// get protocols
		Component[] tmp_ = getAllComponents();
		for (int i=0; i<tmp_.length; i++) {
			if (csl_ == tmp_[i]) continue;
			addProtocol(this, csl_, tmp_[i]);
		}
		
		//System.out.println(this + ": " + drcl.util.StringUtil.toString(protocol_));
		Vector v_ = new Vector();
		for (int i=0; i<cc_.length; i++) {
			//if (Math.random() > .999) System.out.print(i + " ");
			if (!(cc_[i] instanceof Node)) {
				if (cc_[i] instanceof Network)
					build(((Component)cc_[i]).getAllComponents(), cb_);
				continue;
			}
			Node target_ = (Node)cc_[i];
			target_.setComponentFlag(Component.FLAG_COMPONENT_NOTIFICATION,
							false);
			Component thatcsl_ = addCSL(target_, cb_);
				// this takes care of the shadow connections
			target_.iduplicate(this);
				// duplicate everything except the shadow connections
			target_.setComponentFlag(Component.FLAG_COMPONENT_NOTIFICATION,
							true);
			v_.addElement(thatcsl_);
		} // end i, loop on cc_
		tmp_ = new Component[v_.size()];
		v_.copyInto(tmp_);
		//System.out.println("build csl...");
		cb_.build(tmp_);
		for (int i=0; i<tmp_.length; i++)
			tmp_[i].setComponentFlag(Component.FLAG_PORT_NOTIFICATION, true);
		
		// set node address
		//System.out.println("set node addresses...");
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Node) || this == cc_[i]) continue;
			Node target_ = (Node)cc_[i];
			if (target_.addr != drcl.net.Address.NULL_ADDR) {
				target_.addAddress(target_.addr);
			}
		} // end i, loop on cc_
		}
		catch (Throwable e_) {
			e_.printStackTrace();
		}
	}
	
	/** Builds the node structure inside the specified container.
	The method calls <code>build(new Object[]{c_}, (CSLBuilder)null)</code>.
	@param c_ the container component.
	@see #build(Object[], CSLBuilder) */
	public void build(Object c_)
	{ build(new Object[]{c_}, (CSLBuilder)null); }

	/** Builds the node structure inside the specified container with a node
	map.
	The method calls <code>build(new Object[]{c_}, null, map_)</code>.
	@param c_ the container component.
	@param map_ the node map.
	@see #build(Object[], CSLBuilder, String) */
	public void build(Object c_, String map_)
	{ build(new Object[]{c_}, null, map_); }

	/** Builds the node structure inside the specified container
	together with the CSL builder.
	The method calls <code>build(new Object[]{c_}, cb_)</code>.
	@param c_ the container component.
	@param cb_ the CSL builder.
	@see #build(Object[], CSLBuilder) */
	public void build(Object c_, CSLBuilder cb_)
	{ build(new Object[]{c_}, cb_); }

	/** Builds the node structure inside the specified container
	together with the CSL builder and a map.
	The method calls <code>build(new Object[]{c_}, cb_, map_)</code>.
	@param c_ the container component.
	@param cb_ the CSL builder.
	@param map_ the node map.
	@see #build(Object[], CSLBuilder, String) */
	public void build(Object c_, CSLBuilder cb_, String map_)
	{ build(new Object[]{c_}, cb_, map_); }

	Component addCSL(Component target_, CSLBuilder cb_)
	{
		Component csl_ = target_.getComponent(ID_CSL);
		if (csl_ == null) {
			csl_ = cb_.createCSL();
			target_.addComponent(csl_);
		}
		else 
			csl_.setComponentFlag(Component.FLAG_PORT_NOTIFICATION, false);
		
		// create CSL up ports
		Port[] pp_ = cb_.getAllPorts(Module.PortGroup_UP);
		if (pp_ != null)
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				String id_ = p_.getID();
				csl_.addPort(drcl.net.Module.PortGroup_UP, id_);
			}

		// create CSL down ports
		pp_ = target_.getAllPorts(PortGroup_DEFAULT_GROUP);
		if (pp_ != null)
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				String id_ = p_.getID();
				if (id_.startsWith(".")) continue;
				Port tmp_ = csl_.addPort(drcl.net.Module.PortGroup_DOWN, id_);
				p_.connectTo(tmp_);
				tmp_.connectTo(p_);
			}
		
		pp_ = cb_.getAllPorts(Module.PortGroup_DOWN);
		if (pp_ != null)
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				String id_ = p_.getID();
				Port tmp_ = csl_.addPort(drcl.net.Module.PortGroup_DOWN, id_);
				p_.connectTo(tmp_);
				tmp_.connectTo(p_);
			}
		
		return csl_;
	}
	
	Component addProtocol(Component target_, Component csl_, Component pr_)
	{
		Component tmp_ = target_.getComponent(pr_.getID());
		if (tmp_ == null) {
			pr_ = (Component)pr_.clone();
			target_.addComponent(pr_);
		}
		//else if (!(tmp_ instanceof Protocol)) return null;
		else pr_ = tmp_;
		
		int pid_ = _getPID(pr_);
		if (pid_ >= 0) {
			pr_.addPort(Module.PortGroup_DOWN).connect(csl_.addPort(
									Module.PortGroup_UP, String.valueOf(pid_)));
		}
		
		// service ports
		String[] portIDs_ = new String[]{
			SERVICE_ID_PORT_ID,
			SERVICE_RT_PORT_ID,
			SERVICE_IF_PORT_ID,
			SERVICE_CONFIGSW_PORT_ID
		};
		for (int i=0; i<portIDs_.length; i++) {
			Port p_ = pr_.getPort(portIDs_[i]);
			if (p_ != null) p_.connect(csl_.addPort(new Port(Port.PortType_SERVER), portIDs_[i]));
		}

		// service ports: bidirection connection 
		portIDs_ = new String[]{
			SERVICE_MCAST_PORT_ID
		};
		for (int i=0; i<portIDs_.length; i++) {
			Port p_ = pr_.getPort(portIDs_[i]);
			if (p_ != null) {
				Port p2_ = csl_.addPort(new Port(Port.PortType_SERVER),
							portIDs_[i]);
				p_.connectTo(p2_);
				p2_.connectTo(p_); // for receiving join/leave replies
			}
		}
		
		// helping ports:
		portIDs_ = new String[]{
			UCAST_QUERY_PORT_ID,
			MCAST_QUERY_PORT_ID
		};
		for (int i=0; i<portIDs_.length; i++) {
			Port p_ = pr_.getPort(portIDs_[i]);
			if (p_ != null) p_.connect(csl_.addPort(portIDs_[i]));
		}

		// event ports
		portIDs_ = new String[]{
			EVENT_PKT_ARRIVAL_PORT_ID,
			EVENT_ID_CHANGED_PORT_ID,
			EVENT_RT_UCAST_CHANGED_PORT_ID,
			EVENT_RT_MCAST_CHANGED_PORT_ID,
			EVENT_IF_PORT_ID,
			EVENT_VIF_PORT_ID,
			EVENT_MCAST_HOST_PORT_ID
		};
		for (int i=0; i<portIDs_.length; i++) {
			Port p_ = pr_.getPort(portIDs_[i]);
			if (p_ != null) csl_.addPort(portIDs_[i]).connectTo(p_);
		}
		return pr_;
	}
	
	double bw; // global bandwidth setting
	int bs; // global buffer size setting
	int mtu = -1; // -1: depends on CSL, 0: no fragmentation, >0: override CSL setting
	String bufferMode = "byte";
	boolean linkEmu = false;
	double linkPropDelay = Double.NaN; // for link emulation
	
	/** Sets the bandwidth (in bps) for all the interfaces. */
	public void setBandwidth(double bw_)
	{ bw = bw_; }
	
	public double getBandwidth()
	{ return bw; }
	
	/** Sets the buffer size (in bytes) for all the interfaces. */
	public void setBufferSize(int bs_)
	{ bs = bs_; }
	
	public int getBufferSize()
	{ return bs; }
	
	/**
	 * Sets the maximum transmission unit (MTU) for all interfaces.
	 * The length includes the size of the packet header for physical link transmission.
	 * A positive value also enables the fragmentation in the core service layer.
	 * Zero disables it.  The default value applies if a negative value is given.
	 */
	public void setMTU(int mtu_)
	{ mtu = mtu_; }

	public int getMTU()
	{ return mtu; }

	/**
	 * Sets the buffer mode of all the interfaces.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBufferMode(String mode_)
	{ bufferMode = mode_; }

	public String getBufferMode()
	{ return bufferMode; }

	public void setLinkEmulationEnabled(boolean enabled_)
	{ linkEmu = enabled_; }

	public boolean isLinkEmulationEnabled()
	{ return linkEmu; }

	/** Sets the (global) emulated link propagation delay setting.
	 * Used with link emulation enabled. */
	public void setLinkPropDelay(double delay_)
	{ linkPropDelay = delay_; }

	/** Returns the (global) emulated link propagation delay setting.
	 * Used with link emulation enabled. */
	public double getLinkPropDelay()
	{ return linkPropDelay; }
}
