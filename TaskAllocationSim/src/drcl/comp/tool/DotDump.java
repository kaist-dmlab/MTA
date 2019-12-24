// @(#)DotDump.java   7/2003
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

package drcl.comp.tool;
/*
 * Tool to create and view directed graph of JavaSim port connections.
 * Copyright (C) 2003 Peter Kolloch
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of 
 *     conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this 
 *     list of conditions and the following disclaimer in the documentation and/or other 
 *     materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products 
 *     derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

import drcl.comp.Component;
import drcl.comp.Port;

import java.awt.BorderLayout;
import java.awt.Image;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;

import java.util.Collection;
import java.util.Map;
import java.util.Iterator;

import javax.imageio.ImageIO;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

//modified by Hung-ying Tyan:
//(1) for subclassing
//(2) add show(): to work with Tcl/Java
//(2) add dumpToFile(String fileName)

/**
 * Static methods to visualize JavaSim component connections as a directed graph.
 * Only components which have not-hidden ports are shown.
 * 
 * "Normal" ports are displayed in blue, shadow ports in green (including shadow
 * connections). If there is a blue arrow from port1 to port2 it means, that a 
 * message sent at port1 is delivered to port2. (except unexplainable loops from 
 * a port to itself)
 * 
 * The class uses the "dot" program from the graphviz package 
 * (http://www.graphviz.org) for graph creation. It must be installed and in the path.
 *
 * @author Peter Kolloch
 */
public class DotDump implements Runnable
{
	public String INDENT = "\t";
	public String SHADOW_CONNECTION_COLOR = "#319E41";
	public String REAL_CONNECTION_COLOR = "#356ABF";

	Component root;
	boolean showBarePort = false;
	boolean showRoot = false;

    Collection links = new java.util.LinkedList();
	Map components = new java.util.HashMap(); // Component -> ComponentRecord
	Map pendingPorts = new java.util.HashMap(); // Port -> dont care
	Collection pendingLinks = new java.util.LinkedList(); // Port[]{from, to}

	// components being blocked
	// ports of blocked components are also blocked 
	Map compmap = new java.util.HashMap(); // Component -> dont care

	public DotDump()
	{ this(Component.Root); }

	public DotDump(Component root_)
	{
		root = root_;
	}

	public void setRoot(Component root_)
	{ root = root_; }

	public Component getRoot()
	{ return root; }

	/**
	 * Automatically called before each rendering. 
	 */
	public void reset()
	{
		compmap.clear();
		links.clear();
		components.clear();
		pendingLinks.clear();
   	}

	/**
	 * Enables/disables showing ports that have no connection. 
	 * Default is off; 
	 */
	public void setShowBarePortEnabled(boolean s)
	{ showBarePort = s; }

	public boolean isShowBarePortEnabled()
	{ return showBarePort; }

	/**
	 * Enables/disables showing the root component.
	 * Default is off; 
	 */
	public void setShowRootEnabled(boolean s)
	{ showRoot = s; }

	public boolean isShowRootEnabled()
	{ return showRoot; }

	/**
	 * Dumps a graph description sutiable for the graphviz package of all components 
	 * to the given PrintStream.
	 */
    public void dumpComponents(PrintStream out) {
		reset();
        out.println("digraph G {");
        out.print(_graphAttributes(INDENT));

		Collection top = new java.util.LinkedList();

		if (showRoot)
        	top.add(dumpComponents(INDENT, root));
		else {
        	Component[] cc = root.getAllComponents();
        	for (int i = 0; i < cc.length; i++)
				if (showing(cc[i]))
        			top.add(dumpComponents(INDENT, cc[i]));
		}


		processPending();

        // we must print all connections in the end, because otherwise
        // all ports (graph nodes) are shown as part of the component 
		// (subgraph) they first appear in
		// HT: components as well if we consider not printing ports without
		// any connection 

		// print components/ports
        for (Iterator iter = top.iterator(); iter.hasNext();) {
			ComponentRecord r = (ComponentRecord)iter.next();
			dump(r, out);
		}

		// print links
        for (Iterator iter = links.iterator(); iter.hasNext();) {
            Object element = (Object) iter.next();
            out.println(element);
        }

        out.println("}");
		out.close();
    }

	// process pending ports and connections
	private void processPending()
	{
        for (Iterator iter = pendingLinks.iterator(); iter.hasNext();) {
            Port[] pp = (Port[]) iter.next();
			Port from = pp[0];
			Port to = pp[1];
			// if port's host is not showing, the port is not showing
			if (!components.containsKey(from.host)
						   	|| !components.containsKey(to.host)) continue;

			String color = from.host.isDirectlyRelatedTo(to.host)?
				  	SHADOW_CONNECTION_COLOR: REAL_CONNECTION_COLOR;
			links.add(_connect(from, to, color));

			// if link is pending, add to component record

			for (int i=0; i<2; i++) {
				Port p = pp[i];
				if (!pendingPorts.containsKey(p)) continue;
				ComponentRecord r = (ComponentRecord)components.get(p.host);
				r.print(_port(r.prefix + INDENT, p));
				pendingPorts.remove(p);
			}
		}
	}

	private void dump(ComponentRecord r, PrintStream out)
	{
		out.print(r);
		ComponentRecord[] rr = r.getChildRecords();
		for (int i=0; i<rr.length; i++)
			dump(rr[i], out);
		out.println(r.prefix + "}");
	}

    private ComponentRecord dumpComponents(String prefix_, Component c) {
		ComponentRecord r = new ComponentRecord(c, prefix_);
		components.put(c, r);

        // create a subgraph for this component
		r.println(prefix_ + "subgraph \"" + _getComponentClusterName(c)
					   	+ "\" {");
		String newPrefix_ = prefix_ + INDENT;
        r.print(_component(newPrefix_, c));

        Port[] ports = c.getAllPorts();

        for (int j = 0; j < ports.length; j++) {
			if (!showing(ports[j])) continue;

			boolean anyConnect_ = false;
			boolean anyPendingConnect_ = false;

            Port[] shadowsOut = ports[j].getOutShadows();

            for (int i = 0; i < shadowsOut.length; i++)
				if (showing(shadowsOut[i])) {
					if (compmap.containsKey(shadowsOut[i].host)) {
                		links.add(_connect(ports[j], shadowsOut[i], 
												SHADOW_CONNECTION_COLOR));
						anyConnect_ = true;
					}
					else {
						pendingPorts.put(shadowsOut[i], this);
						pendingLinks.add(new Port[]{ports[j], shadowsOut[i]});
						anyPendingConnect_ = true;
					}
				}

            Port[] connectedTo = ports[j].getConceptualInPeers();

            for (int i = 0; i < connectedTo.length; i++)
				if (showing(connectedTo[i])) {
					if (compmap.containsKey(connectedTo[i].host)) {
                		links.add(_connect(ports[j], connectedTo[i],
												REAL_CONNECTION_COLOR));
						anyConnect_ = true;
					}
					else {
						pendingPorts.put(connectedTo[i], this);
						pendingLinks.add(new Port[]{ports[j], connectedTo[i]});
						anyPendingConnect_ = true;
					}
				}

            Port[] shadowsIn = ports[j].getInShadows();

            for (int i = 0; i < shadowsIn.length; i++)
				if (showing(shadowsIn[i])) {
					if (compmap.containsKey(shadowsIn[i].host)) {
                		links.add(_connect(shadowsIn[i], ports[j],
										 SHADOW_CONNECTION_COLOR ));
						anyConnect_ = true;
					}
					else {
						pendingPorts.put(shadowsIn[i], this);
						pendingLinks.add(new Port[]{shadowsIn[i], ports[j]});
						anyPendingConnect_ = true;
					}
				}

			if (showBarePort || anyConnect_)
				r.print(_port(newPrefix_, ports[j]));
			else if (anyPendingConnect_)
				pendingPorts.put(ports[j], this);
        }

        Component[] components = c.getAllComponents();

        for (int i = 0; i < components.length; i++)
			if (showing(components[i])) {
            	ComponentRecord child =
					   dumpComponents(newPrefix_, components[i]);
				r.add(child);
			}

		return r;
    }

	protected final String _getComponentClusterName(Component c)
	{
		return "cluster_" + c;
	}

	protected String _graphAttributes(String prefix_)
	{ return ""; }

	/** This method calls {@link #_showing(Component)} and takes care of
	 * bookkeeping blocked components. */
	public final boolean showing(Component c)
	{
		if (compmap.containsKey(c)) return false;
		boolean show_ = _showing(c);
		if (!show_) compmap.put(c, this);
		return show_;
	}

	/** This method returns true if the host component of the port
	 * is not blocked and {@link #_showing(Port)} returns true.
	 */ 
	public final boolean showing(Port p)
	{
		Component host_ = p.getHost();
		if (host_ != null && !_showing(host_))
			return false;
		else
			return _showing(p);
   	}

	/** Returns true if desired to show the component and its inside structure.
	 * By default, it always returns true. */
	protected boolean _showing(Component c)
	{ return true; }

	/** Returns true if desired to show the port and its connections.
	 * By default, it returns false for "hidden" ports. */
	protected boolean _showing(Port p)
	{ return p.getID().charAt(0) != '.'; }

	/**
	 * Returns the "dot" description for the component. 
	 */
	protected String _component(String prefix_, Component c)
	{
        return prefix_ + "graph [label=\"" + c.getID()
			   	+ "\",color=\"#93A4BF\"];\n";
	}

	/**
	 * Returns the "dot" description for the port. 
	 */
	protected String _port(String prefix_, Port p)
	{
		String portColor = p.isShadow()? "#319E41": "#356ABF";
		return prefix_ + "\"" + p + "\" [label=\""
			   	+ p.getID() + "@" + p.getGroupID()
			   	+ "\",color=\"" + portColor + "\"];\n";
	}

	/**
	 * Returns the "dot" description for the connection. 
	 */
	protected String _connect(Port p1, Port p2, String color_)
	{
		return "\"" + p1 + "\" -> \"" + p2 + "\" [color=\"" + color_ + "\"];";
	}

	/**
	 * Calls the dot program with the description generated by the 
	 * {@link #dumpComponents(PrintStream)} method as input and creates
	 * a Java image object from its output.
	 */
    public Image createComponentImage() throws IOException {
        final Process dot = Runtime.getRuntime().exec(new String[] {
                                                          "dot", "-Tpng"
                                                      });
        new Thread() {
                public void run() {
                    dumpComponents(new PrintStream(dot.getOutputStream()));

                    try {
                        dot.getOutputStream().close();
                    } catch (IOException e) {
                        throw new RuntimeException("unexpected exception", e);
                    }
                }
            }.start();

        return ImageIO.read(dot.getInputStream());
    }

	/**
	 * A simplistic image viewer for the image returned by 
	 * {@link #createComponentImage()}. 
	 */
    void _showComponents() throws IOException {
        JFrame frame = new JFrame("JavaSim Components");
        JLabel label = new JLabel(new ImageIcon(createComponentImage()));
        JScrollPane pane = new JScrollPane(label);
        frame.getContentPane().add(pane, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

	public void run()
   	{
		try {
			_showComponents();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs _showComponents() in a separate thread.
	 */
	public void show()
	{
		new Thread(this).start();
	}

	public void toFile(String fileName_)
	{
		try {
			dumpComponents(new PrintStream(new FileOutputStream(fileName_)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void example()
	{
    	// example with shadowing
        Component test = new Component("test");
        Component.Root.addComponent(test);
        Component intest = new Component("intest");
        test.addComponent( intest);
        intest.addPort( "real").connectTo( test.addPort("shadow"));
		Component test2 = new Component("test2");
		Component.Root.addComponent(test2);
		test.getPort( "shadow").connectTo( test2.addPort( "stuff"));
		
		// example with (incorrectly?) displayed port loops and wire joining 
		Component blupps = new Component("blupps");
		Component.Root.addComponent(blupps);
		Component blupps2 = new Component("blupps2");
		Component.Root.addComponent(blupps2);
		blupps.addPort("stuff").connect( blupps2.addPort("stuff"));	
		/*	
		blupps.getPort("stuff").connect( blupps2.addPort("stuff2"));
		// indirectly connects to both ports of blupps2! wire joining!
		blupps.addPort("stuff2").connectTo( blupps2.getPort("stuff"));
		*/
	}

	public String info()
	{
		StringBuffer sb = new StringBuffer("blocked components: "
					   	+ compmap.size() + "\n");
		for (Iterator it_ = compmap.keySet().iterator(); it_.hasNext(); )
			sb.append("\t" + it_.next() + "\n");

		return "Root: " + root
			   	+ "\nshowRoot: " + showRoot
			   	+ "\nshowBarePort: " + showBarePort
				+ sb.toString();
   	}

    public static void main(String[] args) throws IOException {
		example();
        new DotDump().show();
    }

	class ComponentRecord {
		Component c;
		String prefix;
		StringBuffer lines = new StringBuffer();
		Collection childlist = new java.util.LinkedList();

		ComponentRecord(Component c, String prefix_) {
			this.c = c;
			prefix = prefix_;
		}

		public void print(String line_)
		{ lines.append(line_); }

		public void println(String line_)
		{ lines.append(line_ + "\n"); }

		public void add(ComponentRecord child)
		{
			childlist.add(child);
		}

		public ComponentRecord[] getChildRecords()
		{
			return (ComponentRecord[])
					childlist.toArray(new ComponentRecord[childlist.size()]);
		}

		// append ending "}"
		public String toString()
		{ return lines.toString(); }
	}
}
