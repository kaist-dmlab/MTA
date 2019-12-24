// @(#)Graph.java   7/2003
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

package drcl.net.graph;

import java.util.Iterator;
import java.util.BitSet;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedList;

public class Graph
{
	Area area; // root area
	LinkedList llNodes = new LinkedList(),
		llLinks = new LinkedList(),
		llAreas = new LinkedList();

	double maxX = -1, minX = -1, maxY = -1, minY = -1;
	double avgNodeDegree = -1;
	int minNodeDegree = -1, maxNodeDegree = -1;
	int connectivity = -1;

	public Graph()
	{}

	public Graph(int numNodes_)
	{
		for (int i=0; i<numNodes_; i++)
			llNodes.add(new Node(i));
	}

	public Graph(Node[] nodes_, Link[] links_)
	{
		llNodes.clear();
		llLinks.clear();
		for (int i=0; i<nodes_.length; i++)
			llNodes.add(nodes_[i]);
		for (int i=0; i<links_.length; i++)
			llLinks.add(links_[i]);
	}

	public void addNode(Node n)
	{
		llNodes.add(n);
	}

	public void addLink(Link l)
	{
		llLinks.add(l);
	}

	public void addArea(Area a)
	{
		llAreas.add(a);
	}

	public int numNodes()
	{ return llNodes.size(); }

	public Node[] nodes()
	{ return (Node[])llNodes.toArray(new Node[0]); }

	public Link[] links()
	{ return (Link[])llLinks.toArray(new Link[0]); }

	public String info()
	{
		Node[] nodes_ = nodes();
		Link[] links_ = links();
		if (nodes_ == null || nodes_.length == 0)
			return "Nothing in the graph.\n";
		if (links_ == null) links_ = new Link[0];
		StringBuffer sb_ = new StringBuffer(" links="
						+ links_.length + "  avgDegree="
						+ (links_.length * 2.0 / nodes_.length) + "\n");
		sb_.append("\nNodes: " + nodes_.length + "\n");
		for (int i=0; i<nodes_.length; i++)
			sb_.append(nodes_[i] + "\n");
		sb_.append("\nLinks: " + links_.length + "\n");
		for (int i=0; i<links_.length; i++)
			sb_.append(links_[i] + "\n");
		return sb_.toString();
	}

	public double avgDegree()
	{ 
		if (llNodes.size() == 0) return 0.0;
		return (double)llLinks.size() * 2 / llNodes.size();
	}

	/** Returns the adjacency matrix of the graph. */
	public int[][] getAdjMatrix()
	{
		int[][] adjMatrix_ = new int[llNodes.size()][];
		int count_ = 0;
		for (Iterator it_ = llNodes.iterator(); it_.hasNext(); ) {
			Node n = (Node)it_.next();
			int[] neighbors_ = new int[n.llLinks.size()];
			int count2_ = 0;
			for (Iterator it2_ = n.llLinks.iterator(); it2_.hasNext(); ) {
				Link l = (Link)it2_.next();
				Node neighbor_ = l.neighbor(n);
				neighbors_[count2_++] = neighbor_.getID();
			}
			adjMatrix_[count_++] = neighbors_;
		}
		return adjMatrix_;
	}
}

