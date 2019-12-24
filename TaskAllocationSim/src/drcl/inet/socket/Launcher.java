// @(#)Launcher.java   6/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.socket;

import java.util.*;
import java.net.*;
import drcl.comp.*;
import drcl.inet.Node;

/**
This class is the front end of running real applications on JSim/INET.
The process involves two steps:
<ol>
<li>Configuring the "map" from the real Internet addresses to JSim/INET addresses.
This map is used to retrieve the remote address in JSim when an application
opens a connection with a real Internet address.
<li>Launching the application on a node using {@link #start(String, String[], Node)}.
</ol>
In this framework, we assume:
<ol>
<li>The application class is a standalone Java program (containing the static
<code>main()</code> method to start with).
<li>The application does not create its own <code>ThreadGroup</code>, or if it does,
it does not open a socket in its own <code>ThreadGroup</code>.  Note that a GUI application
may open socket in the GUI thread group, in which case, the application cannot run on
JSim without modification.
</ol>
 */
public class Launcher extends drcl.comp.Component implements SocketImplFactory
{
	Hashtable htAddr = new Hashtable(); // InetAddress --> JSim/INET address, statically configured
	Hashtable htAddrReverse = new Hashtable(); // JSim/INET address --> InetAddress, statically configured
	Hashtable htThreadGroup = new Hashtable(); // thread group --> node component, auto configured
	Hashtable htID = new Hashtable(); // node ID --> thread group, auto configured
	Vector vSockets = new Vector(); // store JSimSocketImpls created for debug
	int counter = 0; // count # of JSimSocketImpls created

	/** Returns the corresponding JSim/INET address (<code>long</code>) given the real
	<code>InetAddress</code>.*/
	public synchronized long getJSimAddr(InetAddress addr_)
	{
		if (addr_ == null) return drcl.net.Address.NULL_ADDR;
		Long javasimAddr_ = (Long)htAddr.get(addr_);
		// XX: how to tell if it is an "any" address?
		if (javasimAddr_ == null) return drcl.net.Address.NULL_ADDR;
		return javasimAddr_.longValue();
	}

	/** Returns the real <code>InetAddress</code> given the JSim/INET address (long).*/
	public synchronized InetAddress getInetAddress(long javasimAddr_)
	{ return (InetAddress)htAddrReverse.get(new Long(javasimAddr_)); }

	/** Returns the node component in which the current thread is working for the {@link JSimSocketImpl}. */
	public synchronized Node getNode()
	{ return (Node)htThreadGroup.get(Thread.currentThread().getThreadGroup()); }

	public void takeover()
	{
		try {
			Socket.setSocketImplFactory(this);
			ServerSocket.setSocketFactory(this);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	public void restore()
	{
		try {
			Socket.setSocketImplFactory(null);
			ServerSocket.setSocketFactory(null);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/**
	 * Configures the launcher with a "map".
	 Each line of the map is in the following format:
	 <pre>&lt;domain name&gt; or &lt;ip&gt;	&lt;JSim address&gt;</pre>
	 */
	public synchronized void configure(String map_)
	{
		try {
			java.io.LineNumberReader reader_ = new java.io.LineNumberReader(new java.io.StringReader(map_));
			while (true) {
				String line_ = reader_.readLine();
				if (line_ == null) break;
				line_ = line_.trim();
				if (line_.length() == 0) continue;
				if (line_.charAt(0) == '#') continue;
				//System.out.println("parse " + line_);
				// format: <domain name> or <ip>	<JSim address>
				String[] ss_ = drcl.util.StringUtil.substrings(line_, " \t");
				InetAddress addr_ = InetAddress.getByName(ss_[0]);
				Long javasimAddr_ = Long.valueOf(ss_[1]);
				htAddr.put(addr_, javasimAddr_);
				htAddrReverse.put(javasimAddr_, addr_);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/** Starts an application. */
	public synchronized void start(String appClassName_, String[] args_, Node node_)
	{
		try {
			Class class_ = Class.forName(appClassName_);
			String id_ = node_.getID();
			ThreadGroup threadGroup_ = (ThreadGroup)htID.get(id_);
			if (threadGroup_ == null) {
				threadGroup_ = new ThreadGroup(id_);
				htID.put(id_, threadGroup_);
				htThreadGroup.put(threadGroup_, node_);
			}
			new MyThread(threadGroup_, class_, args_).start();
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	public synchronized String info()
	{
		StringBuffer sb_ = new StringBuffer("InetAddress <--> JSim/INET address:\n");
		for (Enumeration e_ = htAddr.keys(); e_.hasMoreElements(); ) {
			Object key_ = e_.nextElement();
			Object addr_ = htAddr.get(key_);
			sb_.append("     " + key_ + "\t<--->\t" + addr_ + "\n");
		}
		sb_.append(counter + " JSimSocketImpls created so far.\nActive JSimSocketImpls:\n");
		try {
			for (int i = 0; i < vSockets.size(); i++)
				sb_.append("   socket " + i + ": " + vSockets.elementAt(i) + "\n");
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
		sb_.append("ThreadGroup <--> Node:\n");
		for (Enumeration e_ = htThreadGroup.keys(); e_.hasMoreElements(); ) {
			Object key_ = e_.nextElement();
			Object node_ = htThreadGroup.get(key_);
			sb_.append("     " + key_ + "\t<--->\t" + node_ + "\n");
		}
		return sb_.toString();
	}

	public synchronized void reset()
	{
		super.reset();
		for (Enumeration e_ = htThreadGroup.elements(); e_.hasMoreElements(); ) {
			Node node_ = (Node)e_.nextElement();
			Component wrap_ = node_.getComponent("sockets");
			wrap_.removeAll();
			node_.removeComponent(wrap_);
		}
		htThreadGroup.clear();
		htID.clear();
		vSockets.removeAllElements();
		counter = 0;
	}

	public synchronized SocketImpl createSocketImpl()
	{
		ThreadGroup threadGroup_ = Thread.currentThread().getThreadGroup();
		Node node_ = (Node)htThreadGroup.get(threadGroup_);
		if (node_ == null) {
			// XX: throw an exception?
			System.out.println("Current Thread = " + Thread.currentThread() + ", Threadgroup = " + threadGroup_);
			return null;
		}
		Component tcp_ = node_.getComponent("tcp");

		// create a wrapper component "sockets" if necessary
		WrapperComponent wrap_ = (WrapperComponent)node_.getComponent("sockets");
		Vector vsocket_ = null;
		SocketHandler handler_ = null;
		Port controlPort_;
		if (wrap_ == null) {
			wrap_ = new WrapperComponent("sockets");
			wrap_.setObject(vsocket_ = new Vector());
			controlPort_ = wrap_.addPort("control");
			controlPort_.connect(tcp_.getPort(drcl.net.Module.PortGroup_UP));
			node_.addComponent(wrap_);

			handler_ = new SocketHandler(vsocket_); // to handler QUERY from TCP
			wrap_.setHandler(handler_);
		}
		else {
			vsocket_ = (Vector)wrap_.getObject();
			controlPort_ = wrap_.getPort("control");
			handler_ = (SocketHandler) wrap_.getHandler();
		}

		Port port_ = wrap_.addPort("_tcp" + String.valueOf(counter++));
		InetAddress defaultAddr_ = (InetAddress)htAddrReverse.get(new Long(node_.getDefaultAddress()));
		//System.out.println(node_.getDefaultAddress() + ": " + defaultAddr_);

		JSimSocketImpl s_ = new JSimSocketImpl(this, defaultAddr_, port_, controlPort_, node_);
		vsocket_.addElement(s_);
		vSockets.addElement(s_);
		return s_;
	}

	public synchronized void removeSocket(JSimSocketImpl s_)
	{
		vSockets.removeElement(s_);
		ThreadGroup threadGroup_ = Thread.currentThread().getThreadGroup();
		Node node_ = (Node)htThreadGroup.get(threadGroup_);
		Component tcp_ = node_.getComponent("tcp");
		if (tcp_ == null) {
			// XX: throw an exception?
			return;
		}
		WrapperComponent wrap_ = (WrapperComponent)node_.getComponent("sockets");
		wrap_.removePort(s_.dataPort);
		Vector vsocket_ = (Vector)wrap_.getObject();
		vsocket_.removeElement(s_);
	}

	// to start an application in the right thread group context
	static class MyThread extends Thread
	{
		Class appClass;
		String[] args;

		public MyThread(ThreadGroup threadGroup_, Class class_, String[] args_)
		{
			super(threadGroup_, class_.getName());
			appClass = class_;
			args = args_;
		}

		public void run()
		{
			try {
				appClass.getDeclaredMethod("main", new Class[]{String[].class})
					.invoke(null, new Object[]{args});
			}
			catch (Exception e_) {
				e_.printStackTrace();
			}
		}
	}
}
