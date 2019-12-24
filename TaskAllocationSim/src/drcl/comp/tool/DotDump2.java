// @(#)DotDump2.java   7/2003
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

import drcl.comp.Component;
import drcl.comp.Port;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class adds "blocking" capability to DotDump.
 * The internal structure of the classes being blocked are not shown
 * when the graph is rendered.
 */
public class DotDump2 extends DotDump
{
	HashMap blockmap = new HashMap(); // Class -> dont care

	// "pass" list
	HashMap passmap = new HashMap(); // Class -> dont care

	boolean showHidden = false;
	boolean showInfoPort = false;
	
	public DotDump2()
	{ super(); }

	public DotDump2(Component root_)
	{ super(root_); }

	/**
	 * Enables/disables showing the hidden components/ports. 
	 * This flag does not apply to "InfoPort". 
	 * Default is off. 
	 */
	public void setShowHiddenEnabled(boolean s)
	{ showHidden = s; }

	public boolean isShowHiddenEnabled()
	{ return showHidden; }

	/**
	 * Enables/disables showing the InfoPorts.
	 * Default is off. 
	 */
	public void setShowInfoPortEnabled(boolean s)
	{ showInfoPort = s; }

	public boolean isShowInfoPortEnabled()
	{ return showInfoPort; }

	/** 
	 * Don't show the internal structure of the components of the same class. 
	 */
	public void block(Object o)
	{ if (o != null) blockmap.put(o.getClass(), this); }

	/** 
	 * Show the internal structure of the components of the same class. 
	 */
	public void unblock(Object o)
	{ if (o != null) blockmap.remove(o.getClass()); }

	/** 
	 * Don't show the internal structure of the components of the same class. 
	 */
	public void block(String className_)
	{
		try {
			Class c = Class.forName(className_); 
			blockmap.put(c, this);
		}
		catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/** 
	 * Show the internal structure of the components of the same class. 
	 */
	public void unblock(String className_)
	{
		try {
			Class c = Class.forName(className_);
			blockmap.remove(c);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Removes any blocked classes. */
	public void unblockAll()
	{
		blockmap.clear();
	}

	/** Adds the object to the "pass" list.
	 * The components in the list are shown no matter what flags are set. */
	public void pass(Object o)
	{ passmap.put(o, this); }

	/** Adds the objects to the "pass" list.
	 * The components in the list are shown no matter what flags are set. */
	public void pass(Object[] oo)
	{
		for (int i=0; i<oo.length; i++)
			passmap.put(oo[i], this);
	}

	/** Removes the object from the "pass" list. */
	public void removePass(Object o)
	{ passmap.remove(o); }

	/** Removes the objects from the "pass" list. */
	public void removePass(Object[] oo)
	{
		for (int i=0; i<oo.length; i++)
			passmap.remove(oo[i]);
	}

	/** Clears the "pass" list. */
	public void resetPass()
	{ passmap.clear(); }

	protected boolean _showing(Component c)
	{
		// in the "pass" list?
		if (passmap.containsKey(c)) return true;

		// show hidden component?
		boolean show_ = showHidden || c.getID().charAt(0) != '.';

		Component parent_ = c.getParent();

		if (show_ && parent_ != null)
			show_ = !blockmap.containsKey(parent_.getClass());

		return show_;
	}

	protected boolean _showing(Port p)
	{
		Component host_ = p.getHost();

		// show infoPort?
		if (!showInfoPort && host_ != null && p == host_.infoPort)
			return false;

		// show hidden port?
		return showHidden || super._showing(p);
	}

	protected String _graphAttributes(String prefix_)
	{
		 return super._graphAttributes(prefix_)
				 + prefix_ + "concentrate=true;\n";
   	}

	protected String _component(String prefix_, Component c)
	{
		return prefix_ + "graph [color=\"#93A4BF\"];\n"
				+ prefix_ + "\"" + c + "\" [label=\"" + c.getID()
			   	+ "\",shape=plaintext];\n"
				+ prefix_ + "node ["
			// style for port
			+ "style=filled,fontsize=10,height=0,width=0,fontcolor=white];\n";

	}

	protected String _port(String prefix_, Port p)
	{
		String portColor = p.isShadow()? "#319E41": "#356ABF";
		return prefix_ + "\"" + p + "\" [label=\""
			   	+ p.getID() + "@" + p.getGroupID()
			   	+ "\",color=\"" + portColor + "\"];\n";
			   	//+ "\",color=\"" + portColor + "\","
				//+"style=filled,fontsize=10];\n";
	}

	protected String _connect(Port p1, Port p2, String color_)
	{
		return "\"" + p1 + "\" -> \"" + p2 + "\" [color=\"" + color_ + "\"];";
	}

	public String info()
	{
		StringBuffer sb = new StringBuffer("showInfoPort: " + showInfoPort
						+ "\nshowHidden: " + showHidden
						+ "\nblocked classes: " + blockmap.size()
						+ "\n");
		for (Iterator it_ = blockmap.keySet().iterator(); it_.hasNext(); )
			sb.append("\t" + it_.next() + "\n");

		sb.append("passed components: " + passmap.size() + "\n");
		for (Iterator it_ = passmap.keySet().iterator(); it_.hasNext(); )
			sb.append("\t" + it_.next() + "\n");

		return super.info() + sb.toString();
	}

    public static void main(String[] args) {
		example();
        new DotDump2().show();
        new DotDump2().toFile("DotDump2_test.dot");
    }
}

// Below is the disclaimer from DumpJS.java which is the origin of DotDump.java
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
