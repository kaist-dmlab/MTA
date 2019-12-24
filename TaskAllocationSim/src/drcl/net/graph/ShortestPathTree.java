// @(#)ShortestPathTree.java   1/2004
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

import java.util.*;

import drcl.util.queue.*;
import drcl.util.queue.Queue;

/**
This class implements the Dijkstra algorithm and
calculates the minimum shortest path tree for a graph.
*/
public class ShortestPathTree
{	
	protected boolean debug = false;

	// result of path cost
	double[] cost;

	public void setDebugEnabled(boolean enabled_)
	{ debug = enabled_; }

	public boolean isDebugEnabled()
	{ return debug; }

	/**
	 * Runs the Dijkstra algorithm.  
	 * The implementation excludes "marked" links
	 * (see {@link Link.setMarked(boolean)}). 
	 * @param sourceID_ index of the source node, in the node array. 
	 * @param destIDs_ indices of the destination node, in the node array. 
	 * @param nodes_ the node array. 
	 * @param trim_ set to trim the branches that do not contain destinations.
	 * @return three integer arrays.
	 * 		1st array: parent indices of nodes.  An index can be -1
	 * 		if the node has no parent (i.e., the node is either the source or
	 * 		is not intree).
	 * 		2nd array: parent node interface Id.
	 * 		3rd array:  interface Id of interface to the parent node.
	 */
	public int[][] run(int sourceID_, int[] destIDs_, Node[] nodes_,
					boolean trim_)
	{
		int nnodes = nodes_.length;

		// save original IDs
		int[] originalIDs_ = new int[nnodes];
		for (int i=0; i<nnodes; i++) {
			originalIDs_[i] = nodes_[i].getID();
			nodes_[i].setID(i);
		}

		// neighbor indices
		int[][] neighborIndices_ = new int[nnodes][]; 
		for (int i=0; i<nnodes; i++) {
			Node[] neighbors_ = nodes_[i].neighbors();
			if (neighbors_ == null)
				neighborIndices_[i] = new int[0];
			else {
				int[] tmp_ = new int[neighbors_.length];
				for (int j=0; j<neighbors_.length; j++)
					tmp_[j] = neighbors_[j].getID();
				neighborIndices_[i] = tmp_;
			}
		}

		cost = new double[nnodes]; // cost from src to the node
		int[] parentIndices = new int[nnodes];
		int[] parentIfs = new int[nnodes]; // parent node interface Id
		int[] ifs = new int[nnodes];       // node interface Id
		boolean[] intree = new boolean[nnodes];
		for (int i=0; i<nnodes; i++) {
			parentIndices[i] = parentIfs[i] = ifs[i] = -1;
			cost[i] = Double.POSITIVE_INFINITY;
		}

		// Source node cost
		cost[sourceID_] = 0.0;
		
		// set to true in the beginning, meaning destination node *not* reached
		boolean[] destArray_ = new boolean[nnodes];
		for (int i=0; i<destIDs_.length; i++)
			destArray_[destIDs_[i]] = true;
		int ndests_ = destIDs_.length;

		Queue candidates_ = QueueAssistant.getBest();
			// cost --> candidate node
		candidates_.enqueue(0.0, nodes_[sourceID_]);
		while (!candidates_.isEmpty()) {
			// Get the node with the minimum cost.
			Node minCostNode_ = (Node)candidates_.dequeue();
			if (debug) System.out.println("check " + minCostNode_.getID());
			int from_ = minCostNode_.getID();
			if (intree[from_]) continue;
				// a node may appear in candidate more than once if cost
				// was updated
			intree[from_] = true;
			if (destArray_[from_]) {
				destArray_[from_] = false;
				ndests_ --;
				if (ndests_ == 0) break; // done: all destinations are reached
			}

			// Update the cost of the neighbouring nodes.
			List links_ = minCostNode_.linksInList();
			for (Iterator it = links_.iterator(); it.hasNext(); ) {
				Link l = (Link)it.next();
				if (l.isMarked()) continue; // dont consider marked links
				Node neighbor_ = l.neighbor(minCostNode_);
				int to_ = neighbor_.getID();
				if (intree[to_]) continue;

				int fromIf_ = l.getInterfaceId(minCostNode_);
				double lcost_ = l.getCost();
				if(cost[to_] > cost[from_] + lcost_) {
					cost[to_] = cost[from_] + lcost_;
					parentIndices[to_] = from_;
					parentIfs[to_] = fromIf_;
					ifs[to_] = l.getInterfaceId(neighbor_);
					candidates_.enqueue(cost[to_], neighbor_);
				}
			}
		}

		if (trim_) {

			// Cut off unused branches:
			// Search backwards from each destination node.
			for (int i=0; i<nnodes; i++)
				intree[i] = false;

			for (int i=0; i< destIDs_.length; i++) {
				int index_ = destIDs_[i];

				while (!intree[index_]) {
					intree[index_] = true;
					if (index_ == sourceID_) break;
					else if (parentIndices[index_] < 0) {
						// network is partitioned?
						System.err.println("-- warning -- cannot find path to "
								+ nodes_[index_]);
						break;
					}
					index_ = parentIndices[index_];
				}
			}

			// erase parent indices for those who are not intree
			for (int i=0; i<nnodes; i++)
				if (!intree[i])
					parentIndices[i] = parentIfs[i] = ifs[i] = -1;
		}

		// restore node IDs
		for (int i=0; i<nnodes; i++)
			nodes_[i].setID(originalIDs_[i]);

		originalIDs_ = null;
		destArray_ = null;
		intree = null;
		neighborIndices_ = null;
		candidates_ = null;

		return new int[][]{parentIndices, parentIfs, ifs};
	}

	/** Returns the result cost from source from the previous run.
	 * The indices match the node array in the previous run. */
	public double[] getCost()
	{ return cost; }
}

