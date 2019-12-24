// @(#)routing_msp2.java   1/2004
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
import drcl.comp.*;
import drcl.inet.Node;
import drcl.inet.data.*;
import drcl.inet.contract.*;
import drcl.net.Address;

/**
This class sets up static routes for a network.
The routes being set up form a shortest path tree rooted at each node
as destination.
<p>This class takes the adjacency matrix as the network topology
and runs the Dijkstra algorithm to find the shortest path tree.

<p>To use this class, invoke one of the <code>setup(...)</code> methods
to install appropriate routing entries to nodes (this is done by 
{@link Node#addRTEntry(RTKey, RTEntry, double) Node.addRTEntry()}).
Or one can choose not to install routing entries but only return the result
of the shortest path tree.
*/
public class routing_msp2 extends drcl.net.graph.ShortestPathTree
{
	final boolean memory = false;
	boolean profiling = false;
	boolean profilingDetail = false;
	boolean profilingDetailPrint = false;
	long timeSetup = 0;
	long timeSPF = 0;
	long timeRT = 0;
	long timeTotal = 0;
	Runtime runtime = Runtime.getRuntime();
	boolean stopped = false;

	public void setProfilingEnabled(boolean e)
	{ profiling = e; }

	public boolean isProfilingEnabled()
	{ return profiling; }

	public void setProfilingDetailEnabled(boolean e)
	{ profilingDetail = e; }

	public boolean isProfilingDetailEnabled()
	{ return profilingDetail; }

	public void setProfilingDetailPrintEnabled(boolean e)
	{ profilingDetailPrint = e; }

	public boolean isProfilingDetailPrintEnabled()
	{ return profilingDetailPrint; }

	public void stopit()
	{ stopped = true; }

	/**
	 * Sets up unicast routes for the network_. 
	 * Assumes that nodes in the network are addressed at 0, 1, ..., and so on. 
	 * @param adjMatrix_ adjacency matrix that describes network topology. 
	 */
	public void setup(Component network_, int[][] adjMatrix_)
	{ setup(network_, adjMatrix_, false, true); }

	/**
	 * Sets up unicast routes for the network_. 
	 * @param adjMatrix_ adjacency matrix that describes network topology. 
	 * @param ids_ real node addresses.  ids_[0] is the node address
	 * 		of node 0.  The indexing matches between ids_ and adjMatrix_.
	 */
	public void setup(Component network_, int[][] adjMatrix_, long[] ids_)
	{ setup(network_, adjMatrix_, ids_, null, false, true); }

	/**
	 * Sets up unicast routes for the network_. 
	 * @param retainResult_ if true, the result is stored in
	 * 	{@link #childIndices} and {@link #routes} which are
	 * (n x n) array where n is number of nodes. 
	 * The first dimension is the destination node index.  For the 
	 * shortest path tree of each destination node, the second dimension
	 * records the child node index and interface index to child,
	 * respectively, in that tree.
	 * @param installRoutes_ true if results are installed to the nodes. 
	 */
	public void setup(Component network_, int[][] adjMatrix_,
				   	boolean retainResult_, boolean installRoutes_)
	{
		Component[] cc_ = network_.getAllComponents();
		ArrayList ll_ = new ArrayList();
		for (int i=0; i<cc_.length; i++)
			if (cc_[i] instanceof Node) ll_.add(cc_[i]);
		int nnodes_ = ll_.size();
		Node[] realNodes_ = new Node[nnodes_];
		for (int i=0; i<nnodes_; i++) {
			Node n = (Node)ll_.get(i);
			int index_ = (int)n.getDefaultAddress();
			realNodes_[index_] = n;
		}

		setup(realNodes_, adjMatrix_, null, retainResult_, installRoutes_);
	}

	/** 
	 * See {@link #setup(Component, int[][], boolean, boolean)}.
	 * @param ids_ real node addresses.  ids_[0] is the node address
	 * of node 0.  The indexing matches between ids_ and adjMatrix_.
	 * @param linkcost_ cost matrix for network links; use downstream node
	 * 		index as the first argument and the interface index of the
	 * 		downstream node (to the upstream node) as the 2nd
	 * 		argument.  The interface index should be consistent with that
	 * 		in <code>adjMatrix_</code>.  For the format of adjMatrix,
	 * 		see {@link drcl.inet.InetUtil.createTopology(Component,String,
	 * 		String,Object[],int[][],long[],drcl.inet.Link,boolean)}
	 */
	public void setup(Component network_, int[][] adjMatrix_, long[] ids_,
					LinkCost linkcost_,
				   	boolean retainResult_, boolean installRoutes_)
	{
		HashMap hmID_ = new HashMap(); // node address -> index (0,1,2...)
		for (int i=0; i<ids_.length; i++)
			hmID_.put(new Long(ids_[i]), new Integer(i));

		Component[] cc_ = network_.getAllComponents();
		ArrayList ll_ = new ArrayList();
		for (int i=0; i<cc_.length; i++)
			if (cc_[i] instanceof Node) ll_.add(cc_[i]);
		int nnodes_ = ll_.size();
		Node[] realNodes_ = new Node[nnodes_];
		for (int i=0; i<nnodes_; i++) {
			Node n = (Node)ll_.get(i);

			int index_ = ((Integer)hmID_.get(
								new Long(n.getDefaultAddress()))).intValue();
			realNodes_[index_] = n;
		}

		setup(realNodes_, adjMatrix_, linkcost_, retainResult_, installRoutes_);
	}

	public Node[] realNodes;
	public int[][] adjMatrix;
	public LinkCost linkcost;
	public int[][] childIndices;
	public int[][] routes;

	drcl.net.graph.Node[] nodes; // for debugging

	public void reset()
	{
		realNodes = null;
		nodes = null;
		adjMatrix = null;
		linkcost = null;
		childIndices = null;
		routes = null;
	}

	/**
	 * Sets up unicast routes for the network_. 
	 * The indices in realNodes_ must match those in adjMatrix_. 
	 * The values in adjMatrix_ are indices to realNodes_. 
	 *
	 * <p>If 'retainResult' is turned on, the result is returned
	 * in a (n x n) array where n is number of nodes. 
	 * The first dimension is the destination node index.  For the 
	 * shortest path tree of each destination node, the second dimension
	 * records the parent node index in that tree.
	 * @param linkcost_ cost matrix for network links; use downstream node
	 * 		index as the first argument and the interface index of the
	 * 		downstream node (to the upstream node) as the 2nd
	 * 		argument.  The interface index should be consistent with that
	 * 		in <code>adjMatrix_</code>.  For the format of adjMatrix,
	 * 		see {@link drcl.inet.InetUtil.createTopology(Component,String,
	 * 		String,Object[],int[][],long[],drcl.inet.Link,boolean)}
	 */ 
	public void setup(Node[] realNodes_, int[][] adjMatrix_,
				   	LinkCost linkcost_, boolean retainResult_,
					boolean installRoutes_)
	{
		realNodes = realNodes_;
		adjMatrix = adjMatrix_;
		linkcost = linkcost_;

		stopped = false;
		if (profiling)
			timeSetup = timeTotal = System.currentTimeMillis();

		childIndices = null;

		try {
		int nnodes_ = realNodes_.length;
		if (!installRoutes_) routes = new int[nnodes_][nnodes_];
		if (debug) {
			System.out.println(nnodes_ + " nodes...");
			for (int i=0; i<nnodes_; i++)
				System.out.println("  " + realNodes_[i]);
		}

		// create topology data structures on
		// drcl.net.graph.Node/Link
		nodes = new drcl.net.graph.Node[nnodes_];
		for (int i=0; i<nnodes_; i++)
			nodes[i] = new drcl.net.graph.Node(i);
		int linkcount_ = 0;
		for (int i=0; i<nnodes_; i++) {
			int[] neighbors_ = adjMatrix_[i]; // indices
			drcl.net.graph.Node n = nodes[i];
			for (int j=0; j<neighbors_.length; j++) {
				if (neighbors_[j] <= i) continue; // dont double count
				drcl.net.graph.Node neighbor_ = nodes[neighbors_[j]];

				// find out the interface index at neighbor
				int k = 0; 
				int[] indices_ = adjMatrix[neighbors_[j]];
				for (; k< indices_.length; k++)
					if (indices_[k] == i) break;

				double cost1_ = 1.0, cost2_ = 1.0;
				if (linkcost_ != null) {
					cost1_ = linkcost_.cost(i, j);
					cost2_ = linkcost_.cost(neighbors_[j], k);
				}

				if (cost1_ == cost2_) {
					// bidirectional
					drcl.net.graph.Link l = new drcl.net.graph.Link(
							linkcount_++, n, j, neighbor_, k, cost1_, 0.0,
							false/*undirectional*/);
					n.addLink(l);
					neighbor_.addLink(l);
					if (debug) System.out.println(l);
				}
				else {
					// two unidirectional links
					drcl.net.graph.Link l = new drcl.net.graph.Link(
							linkcount_++, n, j, neighbor_, k, cost1_, 0.0,
							true/*directional*/);
					n.addLink(l);
					if (debug) System.out.println(l);
					l = new drcl.net.graph.Link(
							linkcount_++, neighbor_, k, n, j, cost2_, 0.0,
							true);
					if (debug) System.out.println(l);
				}
			}
		}

		int[] srcIDs_ = new int[nnodes_ - 1];
		for (int i=1; i<nnodes_; i++) srcIDs_[i-1] = i;

		if (retainResult_)
			childIndices = new int[nnodes_][];

		if (profiling) {
			timeSetup = System.currentTimeMillis() - timeSetup;
			System.out.println("Time to set up = "
							   	+ (timeSetup/1000.0) + " seconds");
		}

		long timeSPFtmp_ = 0, timeRTtmp_ = 0;

		// iterates on destination index
		for (int i=0; i<nnodes_; i++) {
			if (stopped) break;
			if (debug) System.out.println("node " + i + "...");
			if (profiling) timeSPFtmp_ = System.currentTimeMillis();

			int destID_ = i;
			// notice that destID_ is used as "source" and
			// srcIDs_ are used as "destinations" in run()
			 
			int[][] result_ = run(destID_, srcIDs_, nodes,
							false/*dont trim the tree*/);
			// result_[1] is child interface index, irrelevant here
			int[] childIndices_ = result_[0];
			int[] ifIndices_ = result_[2];

			if (profiling)
				timeSPFtmp_ = System.currentTimeMillis() - timeSPFtmp_;
			
			if (debug)
				for (int j=0; j<childIndices_.length; j++)
					if (childIndices_[j] >= 0)
						System.out.println("  " + j + " --> "
									   	+ childIndices_[j]);

			if (installRoutes_) {
				if (profiling) timeRTtmp_ = System.currentTimeMillis();
				_configureRT(realNodes_[i].getDefaultAddress(),
								ifIndices_, childIndices_);
				if (profiling)
		   			timeRTtmp_ = System.currentTimeMillis() - timeRTtmp_;

				if (profiling) {
					timeSPF += timeSPFtmp_;
					timeRT += timeRTtmp_;
				}
			}

			if (memory) {
				System.out.println(i + ": MEMORY="
							   	+ drcl.util.MiscUtil.allocatedMemory());
			}

			if (i < nnodes_-1) srcIDs_[i] = i;

			if (retainResult_) {
				childIndices[i] = childIndices_;
				routes[i] = ifIndices_;
			}

			if (profilingDetailPrint)
				System.out.println(i + ": spf = "
							   	+ (timeSPFtmp_/1000.0) + " seconds, rt = "
								+ (timeRTtmp_/1000.0) + " seconds.");
		}

		}
		catch (Exception e_) {
			e_.printStackTrace();
		}

		if (profiling) {
			timeTotal = System.currentTimeMillis() - timeTotal;
			if (profiling) {
				System.out.println("SPF time = "
							   	+ (timeSPF/1000.0) + " seconds");
				System.out.println("RT time = "
								+ (timeRT/1000.0) + " seconds");
			}
			System.out.println("Total time = "
							   	+ (timeTotal/1000.0) + " seconds");
		}
	}


	/**
	 * Configure forwarding cache entries.
	 */
	void _configureRT(long destination_, int[] ifIndices_, int[] childIndices_)
			throws Exception
	{
		long src_ = 0, srcMask_ = 0, destMask_ = -1;
		int nnodes_ = nodes.length;
		RTKey key_ = new RTKey(src_, srcMask_, destination_, destMask_, 0, 0);

		for(int i=0; i<nnodes_; i++) {
			if (ifIndices_[i] < 0) continue;
			int ifindex_ = ifIndices_[i];
			int[] neighbors_ = adjMatrix[i]; // indices
			drcl.data.BitSet bitset_ = new drcl.data.BitSet(neighbors_.length);
			bitset_.set(ifindex_);
			if (debug)
				System.out.println("add fc entry (" + key_ + ", "
							   	+ bitset_ + ")" + " to " + realNodes[i]);

			RTEntry entry_ = new RTEntry(
							realNodes[childIndices_[i]].getDefaultAddress(),
							bitset_);
			realNodes[i].addRTEntry(key_, entry_, -1.0);
		}
	}
}


