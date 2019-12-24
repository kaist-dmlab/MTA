// @(#)routing_msp.java   1/2004
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

package drcl.inet.tool;

import java.util.*;

import drcl.util.queue.*;
import drcl.util.queue.Queue;
import drcl.comp.*;
import drcl.inet.Node;
import drcl.inet.Network;
import drcl.inet.data.*;
import drcl.inet.contract.*;
import drcl.net.Address;

/**
This class sets up static routes between a source node and one or more 
destination nodes.
The routes being set up form a shortest path tree.

<p>To use this class, simply invoke one of the <code>setup(...)</code> methods.

<p>Specifically, this class uses {@link Util#explore(Component, Object[], 
Object[]) Util.explore()} to explore the topology from the source node. 
And then from that topology, it builds a shortest path tree rooted at the
source node.  The appropriate routing table entries are then added to
involved nodes by {@link Node#addRTEntry(RTKey, RTEntry, double) 
Node.addRTEntry()}.
*/
public class routing_msp extends drcl.net.Module
{	
	public routing_msp()
	{ super(); }
	
	public routing_msp(String id_)
	{ super(id_); }
	
	/** 
	 * Reset the routing protocol.
	 */
	public void reset()
	{ super.reset();}
		
	/** 
	 * Duplicate the fields of a routing protocol object given a source routing
	 * protocol object.
	 * 
	 * @param source_ an existing routing protocol object.
	 */
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		routing_msp that_ = (routing_msp)source_;
	}
	
	/**
	 * 
	 */
	public void setup(Node src_, Node dest_)
	{
		setup(src_, dest_, null);
	}
	
	/**
	 * 
	 */
	public void setup(Node src_, Node dest_, String bidirect_)
	{
		//System.out.println("set up path between " + src_ + " and " + dest_);
		long srcAddr_, destAddr_;
		srcAddr_ = src_.getDefaultAddress();
		destAddr_ = dest_.getDefaultAddress();
		Address address_ = drcl.inet.InetConfig.Addr;
		
		if (address_.isUnicast(destAddr_))
			_setup(src_, new Component[]{src_, dest_}, 0, 0, destAddr_, -1);
		else
			_setup(src_, new Component[]{src_, dest_}, srcAddr_, -1, destAddr_,
						-1);
		
		if (bidirect_ != null && bidirect_.length() > 0
						&& address_.isUnicast(destAddr_)) {
			if (address_.isUnicast(srcAddr_))
				_setup(dest_, new Component[]{src_, dest_}, 0, 0, srcAddr_, -1);
			else
				_setup(dest_, new Component[]{src_, dest_}, destAddr_, -1,
						srcAddr_, -1);
		}
	}
	
	/**
	 * Useful for hierarchical network where <code>dest_</code> could be a 
	 * network * component with (<code>destAddr_</code>, 
	 * <code>destAddrMask_</code>) as its network address.
	 * @param destAddr_ destination address, could be a multicast address.
	 */
	public void setup(Component src_, Component dest_, long destAddr_, 
					long destAddrMask_)
	{ _setup(src_, new Component[]{src_, dest_}, 0, 0, destAddr_,
				destAddrMask_);	}
	
	/**
	 * @param destAddr_ destination address, could be a multicast address.
	 */
	public void setup(Component src_, Object[] dest_, long destAddr_)
	{ setup(src_, dest_, 0, 0, destAddr_, -1);	}
	
	/**
	 * @param destAddr_ destination address, could be a multicast address.
	 */
	public void setup(Component src_, Object[] dest_, long destAddr_, 
					long destAddrMask_)
	{ setup(src_, dest_, 0, 0, destAddr_, destAddrMask_);	}
	
	/**
	 * Useful for hierarchical network where <code>dest_</code> could be a 
	 * network component with (<code>destAddr_</code>, 
	 * <code>destAddrMask_</code>) as its network address.
	 * @param destAddr_ destination address, could be a multicast address.
	 */
	public void setup2(Component src_, Object[] dests_, long srcAddr_, 
					long srcAddrMask_, long destAddr_, long destAddrMask_)
	{
		// use src node as the starting node to explore the topology
		Component startNode_ = src_;
		//System.out.println("set up path between " + source + " and "
		//		+ destination);
		
		//starting from current node
		Hashtable topology = drcl.comp.Util.explore(startNode_, 
						new Object[]{drcl.inet.Link.class}, null);
		
		//System.out.println("Number of nodes "+topology.size());
		Component[] nodes = new Component[topology.size()];
		int i = 0;
		for (Enumeration e = topology.keys(); e.hasMoreElements(); )
			 nodes[i++] = (Component)e.nextElement();
		
		// construct ajacency matrix: neighbor
		// nodes[i] use ports[i][j] to connect to nodes[neighbor[i][j]]
		int[][] neighbor = new int[nodes.length][];
		Port[][] ports = new Port[nodes.length][];
		for(i=0; i<neighbor.length; i++) {
			Util.Link[] links_ = (Util.Link[]) topology.get(nodes[i]);
			neighbor[i] = new int[links_.length];
			ports[i] = new Port[links_.length];
			for(int j=0; j<links_.length; j++)
				for(int k=0; k<nodes.length; k++) 
					if (i != k && links_[j].to.host == nodes[k]) {
						neighbor[i][j] = k;
						ports[i][j] = links_[j].from;
						break;
					}
		}
		
		if (isDebugEnabled()) {
			StringBuffer sb_ = new StringBuffer("Ajacency matrix:\n");
			for(i=0; i<neighbor.length; i++) {
				sb_.append(nodes[i] + ": ");
				for (int j=0; j<neighbor[i].length; j++)
					sb_.append(Util.getPortID(ports[i][j], nodes[i]) + "-->"
									+ nodes[neighbor[i][j]] + " ");
				sb_.append("\n");
			}
			debug(sb_);
		}
		
		// 
		int isrc_ = Integer.MAX_VALUE;
		int[] idest_ = new int[dests_.length];
		i = 0;
		for(int k=0; k<nodes.length; k++)	{
			Component n_ = nodes[k];
			if (n_ == src_) isrc_ = k;
			else {
				for (int j=0; j<dests_.length; j++)
					if (n_ == dests_[j]) {
						idest_[i++] = k;
						break;
					}
			}
		}
		if (isrc_ == Integer.MAX_VALUE || i < idest_.length) {
			if (isrc_ == Integer.MAX_VALUE)
				error("setup()", src_ + " is not in the topology");
			else
				error("setup()",
						"not all destination nodes are in the topology");
			return;
		}
		
		if (isDebugEnabled()) debug("current: " + nodes[isrc_].getID()
						+ ", dests: " + drcl.util.StringUtil.toString(idest_));
		
		// run the algorithm!
		// XXX: should recursively process Network components
		// by far, this only works for flat networks...
		ConfigureRT(srcAddr_, srcAddrMask_, destAddr_, destAddrMask_,
					mspf(isrc_, idest_, neighbor), nodes, ports, neighbor);
	}

	/**
	 * Configure forwarding cache entries.
	 */
	void ConfigureRT(long src_, long srcMask_, long destination, long destMask_,
								  PathStruct[] path, Component[] nodes,
								  Port[][] ports, int[][] neighbor)
	{
		if(isDebugEnabled()) {
			StringBuffer sb_ = new StringBuffer(
							"Configuring forwarding cache:\n");
			for (int i=0; i<path.length; i++)	{
				if (path[i] == null) continue;
				sb_.append(nodes[i] + " : parent " + nodes[path[i].parentIndex]
								+ " : children ");
				for(int j=0; j<path[i].link_id.size(); j++)
					sb_.append(nodes[((Integer)
							(path[i].link_id.elementAt(j))).intValue()]+" ");
				sb_.append("\n");
			}
			debug(sb_);
		}
		
		try {
			for(int i=0; i<nodes.length; i++) {
				if (isDebugEnabled()) debug("check " + nodes[i] + ": ");
				if (path[i] == null || path[i].link_id.size() == 0) continue;
				if (!(nodes[i] instanceof Node)) continue;
				
				Node n_ = (Node)nodes[i];
				drcl.data.BitSet bitset = new drcl.data.BitSet(
								neighbor[i].length);
				for(int j=0; j<path[i].link_id.size(); j++)
				{
					int neighbor_ = ((Integer)
									path[i].link_id.elementAt(j)).intValue();
					// find corresponding interface index
					for (int k=0; k<neighbor[i].length; k++)
						if (neighbor[i][k] == neighbor_) {
							try {
								bitset.set(
									Integer.parseInt(ports[i][k].getID()));
							}
							catch (Exception e_) {} // ignored
							break;
						}
				}
				RTKey key_ = new RTKey(src_, srcMask_, destination, destMask_,
								0, 0); 
				if (isDebugEnabled())
					debug("add fc entry (" + key_ + ", " + bitset + ")");
				n_.addRTEntry(key_, new RTEntry(bitset, null), -1.0);
			}
		}
		catch (Throwable e_) {
			e_.printStackTrace();
			error(path, "ConfigureRT()", null, e_);
		}
	}

	/**
	 * Assumes that each link costs 1, i.e., find a min hop count path tree.
	 * Method to select the shortest path tree given a 
	 * source and a set of destinations.
	 * 
	 * This method does not use node address.
	 */
	public PathStruct[] mspf(int source_id, int[] dest_id, int[][] neighbor)
	{
		int NumNode = neighbor.length;
		NodeStruct [] nodestruct	= new NodeStruct[NumNode];
			// For constructing a SPF tree.
		int i, j, k, nodecount, mincostnode;

		// Initialize NodeStruct.
		for (i=0; i< NumNode; i++) {
			nodestruct[i]       = new NodeStruct();
			nodestruct[i].cost	= Integer.MAX_VALUE; 
			nodestruct[i].parentIndex= -1;
			nodestruct[i].intree= false;
		}

		nodecount	= 0;

		// Source node cost and parent.
		nodestruct[source_id].cost	= 0;
		nodestruct[source_id].parentIndex= source_id;
		
		while (nodecount < NumNode) {
			// Get the node with the minimum cost.
			mincostnode	= mincost(nodestruct);
			nodestruct[mincostnode].intree = true;	// Add the node to the tree.
			nodecount++;

			// Update the cost of the neighbouring nodes.
			for (i=0; i< neighbor[mincostnode].length; i++) {
				double cost = 1.0;
				
				if((nodestruct[mincostnode].cost + cost)
					< nodestruct[neighbor[mincostnode][i]].cost) {
					nodestruct[neighbor[mincostnode][i]].cost =
						nodestruct[mincostnode].cost + cost;
					nodestruct[neighbor[mincostnode][i]].parentIndex =
						mincostnode;
				}
			}
		}

		// Found the ontree nodes and links.
		Vector nodevector	= new Vector();
		PathStruct tmppathstruct;

		int tmpindex, temp = -1;

		//
		// Search backwards from each destination node.
		//
		PathStruct[] paths = new PathStruct[NumNode];

		for (i=0; i< dest_id.length; i++)
		{
			if (paths[dest_id[i]] != null) continue; // 5/4, Tyan

			tmpindex = dest_id[i];

			tmppathstruct				= new PathStruct();
			tmppathstruct.nodeIndex		= tmpindex;
			tmppathstruct.parentIndex	= nodestruct[tmpindex].parentIndex;

			paths[tmpindex] = tmppathstruct;
			
			while (tmpindex != source_id)
			{
				temp	 = tmpindex;
				tmpindex = nodestruct[tmpindex].parentIndex;	// Parent.
				
				// If parent is in the nodevector, add link if not there already
				boolean done_ = paths[tmpindex] != null;
				
				if (done_) {
					paths[tmpindex].link_id.addElement(new Integer(temp));
					break; // no need to go up further, 5/4, Tyan
				}
				else { // If parent is not in the nodevector, add node and link.
					PathStruct newpathstruct = new PathStruct();
					newpathstruct.nodeIndex	 = tmpindex;
					newpathstruct.parentIndex= nodestruct[tmpindex].parentIndex;
					if (temp == -1)
					{
						System.out.println("incorrect link added.");
						System.exit(1);
					}
					newpathstruct.link_id.addElement(new Integer(temp));
					paths[tmpindex] = newpathstruct;
				}
			}
		} // for loop.
		return paths;
	}

	/** 
	 * Method used by SPF for finding the minimum cost node.
	 */
	public int mincost (NodeStruct[] nodestruct)
	{
		int i, tmpnode;
		double cost;

		cost = Integer.MAX_VALUE;

		tmpnode = 0;
		for (i = 0; i< nodestruct.length; i++) {
			if ((nodestruct[i].intree == false) && (nodestruct[i].cost < cost))
			{
				cost = nodestruct[i].cost;
				tmpnode = i;
			}
		}

		return tmpnode; 
	}
	
	
	/**
	 * Structures for finding a shortest path tree from a source
	 * to all nodes.
	 */
	class NodeStruct {
		double cost;
		int parentIndex;
		boolean intree;
	}

	class PathStruct {
		int nodeIndex; // for debug
		int parentIndex;	 // parent node id
		Vector link_id;
			// actually, it's a vector of neighbor node ids, not link id

		public PathStruct ()
		{
			link_id = new Vector();
		}
		
		public String toString()
		{
			return "(Path for node " + nodeIndex + ": parent=" + parentIndex
				+ ", neighbors=" + link_id + ")";
		}
	}

	// there is a big array of nodes, node indices used in this class
	// are the ones to this array
	class NodeStruct2 {
		int nodeIndex; // node index of this node
		Component comp;

		// data structure for network topology
		int[] toNeighbors; // neighbor node indices
		Object toNeighborPorts;

		// data structure for SPT
		int parentIndex = -1;
		Port parentPort;
			// index of interface of parent to this node,
			// for creating route entry
		int numChild = -1;
			// first used to count number of neighbors
			// then decremented to fill 'children'
		double cost = Double.POSITIVE_INFINITY;
		int[] children; // intree child node indices

		// true if this node is in tree
		boolean intree()
		{ return numChild >= 0; }

		String _topologyInfo()
		{
			return "neighbors="
				+ drcl.util.StringUtil.toString(toNeighbors)
				+ ", If_to_neighbors="
				+ drcl.util.StringUtil.toString(toNeighborPorts) + ")";
		}

		String _treeInfo()
		{
			return "parent=" + parentIndex + ", parentPort=" + parentPort
				+ ", cost=" + cost + ", neighbors="
				+ (children != null && children.length > 0? 
					drcl.util.StringUtil.toString(children): "none");
		}

		public String toString()
		{
			return "(Node " + nodeIndex + ": " + _topologyInfo()
				+ ", " + _treeInfo() + ")";
		}

		public String topologyInfo()
		{
			return "(Node " + nodeIndex + ": " + _topologyInfo() + ")";
		}

		public String treeInfo()
		{
			return "(Node " + nodeIndex + ": " + _treeInfo() + ")";
		}
	}

	/**
	 * Useful for hierarchical network where <code>comp_</code> could be 
	 * the set of components (networks and nodes) that constitute
	 * the network address <code>destAddr_</code>/
	 * <code>destAddrMask_</code>).
	 * @param destAddr_ destination address, could be a multicast address.
	 */
	public void setup(Component src_, Object[] dests_, long srcAddr_, 
					long srcAddrMask_, long destAddr_, long destAddrMask_)
	{
		if (src_ == null) {
			error("setup()", "src_ cannot be null");
			return;
		}
		Component[] comp_ = null;
		{
			Vector tmp_ = new Vector();
			tmp_.addElement(src_);
			if (dests_ != null)
				for (int i=0; i < dests_.length; i++)
					if (dests_[i] instanceof Component)
						tmp_.addElement(dests_[i]);
			comp_ = new Component[tmp_.size()];
			tmp_.copyInto(comp_);
		}
		_setup(src_, comp_, srcAddr_, srcAddrMask_, destAddr_, destAddrMask_);
	}

	// comp_ must include src_
	void _setup(Component src_, Component[] comp_, long srcAddr_, 
					long srcAddrMask_, long destAddr_, long destAddrMask_)
	{
		try {

		Hashtable topology = drcl.comp.Util.explore(comp_,
						new Object[]{drcl.inet.Link.class}, null, false);
		
		// XXX: if comp_ is null, fill it with nodes explored

		//System.out.println("Number of nodes "+topology.size());
		NodeStruct2[] nodes = new NodeStruct2[topology.size()];
		int i = 0;
		for (Enumeration e = topology.keys(); e.hasMoreElements(); ) {
			NodeStruct2 node_ = new NodeStruct2();
			node_.comp = (Component)e.nextElement();
			if (!(node_.comp instanceof Node)
				&& !(node_.comp instanceof Network)) continue;
			node_.nodeIndex = i;
			// temporary setting:
			// - put Util.Link[] to 'node_.toNeighborPorts' from 'topology'
			// - and then replace in 'topology' with 'node_'
			// the new 'topology' and Util.Link[] will be used to construct
			// 'node_.toNeighbors' and 'node_.toNeighborPorts'
			node_.toNeighborPorts = (Util.Link[]) topology.get(node_.comp);
			topology.put(node_.comp, node_);
			nodes[i++] = node_;
		}

		// construct ajacency matrix: toNeighbors and toNeighborPorts
		// the j'th neighbor of nodes[i]: 
		//   nodes[i] use nodes[i].toNeighborPorts[j] to connect to 
		//   nodes[nodes[i].toNeighbors[j]]
		for (i=0; i<nodes.length; i++) {
			NodeStruct2 node_ = nodes[i];
			if (node_ == null) break;
			Util.Link[] links_ = (Util.Link[]) node_.toNeighborPorts; //c above
			int[] toNeighbors_ = new int[links_.length];
			Port[] toNeighborPorts_ = new Port[links_.length];
			for(int j=0; j<links_.length; j++) {
				Util.Link link_ = links_[j];
				try {
					NodeStruct2 neighbor_ = (NodeStruct2)topology.get(
								link_.to.host);
					toNeighborPorts_[j] = link_.from;
					toNeighbors_[j] = neighbor_.nodeIndex;
				} catch (Exception e_) {
					toNeighbors_[j] = -1;
				}
			}
			node_.toNeighbors = toNeighbors_;
			node_.toNeighborPorts = toNeighborPorts_;
		}
		
		if (isDebugEnabled()) {
			StringBuffer sb_ = new StringBuffer("Ajacency matrix:\n");
			for(i=0; i<nodes.length; i++) {
				NodeStruct2 node_ = nodes[i];
				if (node_ == null) break;
				sb_.append("node " + i + "(" + node_.comp + "): ");
				for (int j=0; j<node_.toNeighbors.length; j++)
					if (node_.toNeighbors[j] >= 0)
						sb_.append(Util.getPortID((
							(Port[])node_.toNeighborPorts)[j], node_.comp)
							+ "--> node " + node_.toNeighbors[j] + ", ");
				sb_.append("\n");
			}
			debug(sb_);
		}
		
		Hashtable htDest_ = new Hashtable();
		for (int j=0; j<comp_.length; j++)
			if (src_ != comp_[j])
				htDest_.put(comp_[j], htDest_); // dont care 2nd argument
		int isrc_ = Integer.MAX_VALUE;
		int[] idest_ = new int[htDest_.size()];
		i = 0;
		for(int k=0; k<nodes.length; k++)	{
			if (nodes[k] == null) break;
			Component n_ = nodes[k].comp;
			if (n_ == src_)
				isrc_ = k;
			else if (htDest_.containsKey(n_))
				idest_[i++] = k;
		}
		if (isrc_ == Integer.MAX_VALUE || i < htDest_.size()) {
			if (isrc_ == Integer.MAX_VALUE)
				error("setup()", src_ + " is not in the topology");
			else
				error("setup()",
						"not all destination nodes are in the topology");
			return;
		}

		if (isDebugEnabled())
			debug("current: " + nodes[isrc_].comp.getID()
					+ ", dests: " + drcl.util.StringUtil.toString(idest_));
		
		// run the algorithm!
		// XXX: should recursively process Network components
		// by far, this only works for flat networks...
		mspf2(isrc_, idest_, nodes, htDest_);
		ConfigureRT2(srcAddr_, srcAddrMask_, destAddr_, destAddrMask_, nodes);

		} catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	void mspf2(int sourceID_, int[] destIDs_, NodeStruct2[] nodes_,
				Hashtable htDest_)
	{
		// Source node cost and parent.
		nodes_[sourceID_].cost	= 0;
		nodes_[sourceID_].parentIndex = sourceID_;
		
		Queue candidates_ = QueueAssistant.getBest();
		candidates_.enqueue(0.0, nodes_[sourceID_]);
		while (!candidates_.isEmpty() && htDest_.size() > 0) {
			// Get the node with the minimum cost.
			NodeStruct2 minCostNode_ = (NodeStruct2)candidates_.dequeue();
			if (minCostNode_.intree()) continue;
				// a node may appear in candidate more than once if cost
				// was updated
			if (htDest_.containsKey(minCostNode_.comp))
				htDest_.remove(minCostNode_.comp);
			minCostNode_.numChild = 0; // become intree

			// Update the cost of the neighbouring nodes.
			for (int i=0; i< minCostNode_.toNeighbors.length; i++) {
				if (minCostNode_.toNeighbors[i] < 0) continue;
				NodeStruct2 neighbor_ = nodes_[minCostNode_.toNeighbors[i]];
				if (neighbor_.intree()) continue;
				
				double linkcost_ = 1.0;
				if(neighbor_.cost > minCostNode_.cost + linkcost_) {
					neighbor_.cost = minCostNode_.cost + linkcost_;
					neighbor_.parentIndex = minCostNode_.nodeIndex;
					neighbor_.parentPort =
						((Port[])minCostNode_.toNeighborPorts)[i];
					candidates_.enqueue(neighbor_.cost, neighbor_);
				}
			}
		}

		// Cut off unused branches:
		// Search backwards from each destination node.
		// First get 'numChild', then build 'children'
		for (int i=0; i< destIDs_.length; i++) {
			NodeStruct2 node_ = nodes_[destIDs_[i]];

			if (node_.numChild > 0) continue;
				// node_ has been processed, on the way back to source
				// from another destination node

			while (node_.nodeIndex != sourceID_) {
				if (node_.parentIndex < 0) {
					// network is partitioned?
					if (isDebugEnabled())
						debug("-- warning -- cannot find path to "
								+ node_.comp);
					break;
				}
				NodeStruct2 parent_ = nodes_[node_.parentIndex];
				parent_.numChild++;
				
				// If parent has been processed before, no need to go up further
				if (parent_.numChild > 1) break;
				node_ = parent_;
			}
		}

		for (int i=0; i< destIDs_.length; i++) {
			NodeStruct2 node_ = nodes_[destIDs_[i]];

			if (node_.children != null) continue;
				// node_ has been processed, on the way back to source
				// from another destination node

			while (node_.nodeIndex != sourceID_) {
				if (node_.parentIndex < 0)
					// network is partitioned? gave warning above
					break;
				NodeStruct2 parent_ = nodes_[node_.parentIndex];

				if (parent_.children == null)
					parent_.children = new int[parent_.numChild];
				int numChild_ = parent_.children.length;
				parent_.children[numChild_ - parent_.numChild] =
					node_.nodeIndex;
				parent_.numChild --;

				// If parent has been processed before, no need to go up further
				if (parent_.children.length > parent_.numChild +1)
					break;
				node_ = parent_;
			}
		}
	}

	/**
	 * Configure forwarding cache entries.
	 */
	void ConfigureRT2(long src_, long srcMask_, long destination_,
					long destMask_, NodeStruct2[] nodes_)
	{
		if(isDebugEnabled()) {
			StringBuffer sb_ = new StringBuffer(
							"Configuring forwarding cache:\n");
			for (int i=0; i<nodes_.length; i++)	{
				if (nodes_[i] == null) break;
				if (!nodes_[i].intree()) continue;
				sb_.append(nodes_[i].treeInfo() + "\n");
			}
			debug(sb_);
		}
		
		try {
			for(int i=0; i<nodes_.length; i++) {
				if (nodes_[i] == null) break;
				if (nodes_[i].children == null ||
					!(nodes_[i].comp instanceof Node)) continue;
				if (isDebugEnabled()) debug("check node " + i + ": ");
				
				NodeStruct2 node_ = nodes_[i];
				Node n_ = (Node)nodes_[i].comp;
				drcl.data.BitSet bitset_ = new drcl.data.BitSet(
								node_.toNeighbors.length);

				for(int j=0; j<node_.children.length; j++) {
					int child_ = node_.children[j];
					// set corresponding interface index
					bitset_.set(Integer.parseInt(
						nodes_[child_].parentPort.getID()));
				}
				RTKey key_ = new RTKey(src_, srcMask_, destination_, destMask_,
								0, 0); 
				if (isDebugEnabled())
					debug("add fc entry (" + key_ + ", " + bitset_ + ")");
				n_.addRTEntry(key_, new RTEntry(bitset_, null), -1.0);
			}
		}
		catch (Throwable e_) {
			e_.printStackTrace();
		}
	}
}


