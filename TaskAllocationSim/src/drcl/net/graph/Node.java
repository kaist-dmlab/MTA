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

package drcl.net.graph;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

public class Node
{
	int id;
	double x, y;
	LinkedList llLinks;
	Area parent;

	String name;

	public Node()
	{}

	public Node(int id_)
	{ id = id_; }

	public Node(int id_, double x_, double y_)
	{
		id = id_;
		x = x_;
		y = y_;
	}

	public Node(int id_, double x_, double y_, Area parent_)
	{
		this(id_, x_, y_);
		parent = parent_;
	}

	public Node(int id_, double x_, double y_, String name_)
	{
		this(id_, x_, y_);
		name = name_;
	}

	public Node(int id_, double x_, double y_, String name_, Area parent_)
	{
		this(id_, x_, y_, name_);
		parent = parent_;
	}

	public int getID()
	{ return id; }

	public void setID(int id_)
	{ id = id_; }

	public double getX()
	{ return x; }

	public double getY()
	{ return y; }

	public Link[] getLinks()
	{
		return llLinks == null? new Link[0]:
				(Link[])llLinks.toArray(new Link[llLinks.size()]);
   	}

	public int getNumLinks()
	{ return llLinks == null? 0: llLinks.size(); }

	public Area getParent()
	{ return parent; }

	public void set(double x_, double y_)
	{ x = x_; y = y_; }

	public void addLink(Link l_)
	{
		if (llLinks == null) llLinks = new LinkedList();
		llLinks.add(l_);
	}

	/** Only removes the link from this end. */
	public void removeLink(Link l_)
	{
		if (llLinks != null) llLinks.remove(l_);
	}

	public String getName()
	{ return name == null? String.valueOf(id): name; }

	public void setName(String name_)
	{ name = name_; }

	public String toString()
	{
		if (name == null)
			return id + ": " + x + "  " + y;
		else
			return id + ": " + name + ": " + x + "  " + y;
	}

	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(toString());
		if (llLinks != null && llLinks.size() > 0) {
			sb_.append(", links: ");
			Link[] links_ = links();
			for (int i=0; i<links_.length; i++)
				sb_.append(links_[i].id + " ");
		}
		else
			sb_.append(", no_links");

		return sb_.toString();
	}

	public String areaID()
	{
		if (parent == null) return "" + id;
		else return parent.areaID() + "." + id;
	}

	public Link[] links()
	{
		return llLinks == null? new Link[0]:
				(Link[])llLinks.toArray(new Link[llLinks.size()]);
	}

	public List linksInList()
	{ return llLinks == null? new LinkedList(): llLinks; }

	public Node[] neighbors()
	{
		if (llLinks == null) return new Node[0];
		// XX: assume p2p links
		Node[] nn = new Node[llLinks.size()];
		int i = 0;
		for (Iterator it_ = llLinks.iterator(); it_.hasNext(); ) {
			Link l = (Link)it_.next();
			nn[i++] = l.neighbor(this);
		}
		return nn;
	}

	public LinkedList getLinksLinkedList()
	{ return llLinks; }

	public int degree()
	{ return llLinks == null? 0: llLinks.size(); }
}
