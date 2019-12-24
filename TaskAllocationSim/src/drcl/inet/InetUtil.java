// @(#)InetUtil.java   2/2004
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

import java.io.*;
import java.util.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.Address;
import drcl.net.Module;
import drcl.net.Packet;
import drcl.net.FooPacket;
import drcl.net.traffic.TrafficModel;
import drcl.net.traffic.TrafficSourceComponent;
import drcl.net.traffic.TrafficAssistant;
import drcl.net.graph.TopologyReader;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.inet.tool.LinkCost;

/**
The utility class that offers tools for building a network topology and so on.

@author Hung-ying Tyan
 */
public class InetUtil implements InetConstants
{
	/** Returns the result of trace route in the format of
	 * "(hop): (hop_address), if=(outgoing_interface)", a line for each hop.
	 * @see #traceRouteInObj(Node, Node) */
	public static String traceRoute(Node from_, Node to_)
	{
		Object[] oo = traceRouteInObj(from_, to_);
		if (oo == null || oo.length == 0)
			return "Error: destinatioin not reachable";

		StringBuffer sb = new StringBuffer();
		for (int i=0; i<oo.length; i+=2)
			sb.append((i/2) + ": " + oo[i] + ", if=" + oo[i+1] + "\n");
		return sb.toString();
	}

	/** Returns the result of trace route in an array.
	 * The result is based on the routing table in the nodes. 
	 * Every two elements in the array represent address (Long)
	 * and interface (Integer) for the corresponding hop along the route.
	 * Returns null if destination is not reachable for any reason. */
	public static Object[] traceRouteInObj(Node from_, Node to_)
	{
		try {

		long dest_ = to_.getDefaultAddress();
		Node n = from_;
		LinkedList result_ = new LinkedList();
		long nextHop_ = Address.NULL_ADDR;
		while (n != to_) {
			int if_ = -1;
			if (n.hasRoutingCapability()) {
				RTKey key_ = new RTKey(-1, dest_, -1);
				RTEntry e = (RTEntry)n.retrieveRTEntry(key_,
						drcl.data.Map.MATCH_LONGEST);
				if (e == null) return null;
				nextHop_ = e.getNextHop();
				int[] out_ = e._getOutIfs();
				if (out_ == null) return null;
				for (int i=0; i<out_.length; i++)
					if (out_[i] >= 0) {
						if_ = out_[i];
						break;
					}
				if (if_ < 0) return null;
			}
			else
				if_ = 0;
			result_.addLast(new Long(n.getDefaultAddress()));
			result_.addLast(new Integer(if_));

			// move on to next node
	
			// look for next "valid" Node
			// - destination itself
			// - has routing capability and has route entry to dest_ 
			Util.Link[] ll = Util.getLinks(n.getPort(String.valueOf(if_)));

			Node next_ = null; 
			for (int i = 0; i<ll.length; i++) {
				Component c = ll[i].to.host;
				if (c instanceof Node &&
					(((Node)c).getDefaultAddress() == nextHop_
					 || _validNextHop((Node)c, dest_))) {
					next_ = (Node)c;
					break;
				}
				else if (c instanceof Link) {
					Port[] pp = c.getAllPorts("");
					for (int j = 0; j<pp.length; j++) {
						if (pp[j] == ll[i].to) continue;
						Util.Link[] ll2 = Util.getLinks(pp[j]);
						for (int k=0; k<ll2.length; k++) {
							Component c2 = ll2[k].to.host;
							if (c2 instanceof Node &&
								(((Node)c2).getDefaultAddress() == nextHop_
								|| _validNextHop((Node)c2, dest_))){
								next_ = (Node)c2;
								break;
							}
						}
						if (next_ != null) break;
					}
					if (next_ != null) break;
				}
			}
			if (next_ == null) return null;
			n = next_;
		}
		result_.addLast(new Long(to_.getDefaultAddress()));
		result_.addLast(new Integer(-1));
		return result_.toArray();

		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// return true if n is a valid next hop for dest_
	// used by traceRouteInObj() 
	static private boolean _validNextHop(Node n, long dest_)
	{
		if (n.getDefaultAddress() == dest_) return true;
		if (!n.hasRoutingCapability()) return false;
		RTKey key_ = new RTKey(-1, dest_, -1);
		RTEntry e = (RTEntry)n.retrieveRTEntry(key_,
						drcl.data.Map.MATCH_LONGEST);
		if (e == null) return false;
		int[] out_ = e._getOutIfs();
		if (out_ == null) return false;
		for (int i=0; i<out_.length; i++)
			if (out_[i] >= 0) return true;
		return false;
	}

	/**
	 Creates and returns the adjacency matrix for a regular mesh topology
	 of the given size.
	 @param nc_ # of columns in the mesh.
	 @param nr_ # of rows in the mesh.
	 */
	public static int[][] createMeshAdjMatrix(int nc_, int nr_)
	{
		if (nc_ <= 0 || nr_ <= 0)
			throw new IllegalArgumentException(
						"negative # of columns or rows: " + nc_ + "x" + nr_);
		int nn_ = nc_*nr_;
		int i = 0;
		int[][] adjMatrix_ = new int[nn_][];
		int[] tmp_ = new int[4];
		for (int r_ = 0; r_<nr_; r_++) {
			for (int c_=0; c_<nc_; c_++) {
				int j=0;
				// check up
				if (i >= nc_) tmp_[j++] = i-nc_;
				// check left
				if (c_ % nc_ > 0) tmp_[j++] = i-1;
				// check right
				if ((c_+1) % nc_ > 0) tmp_[j++] = i+1;
				// check down
				if (i + nc_ < nn_) tmp_[j++] = i+nc_;

				// sum up
				int[] new_ = new int[j];
				for (int k=0; k<j; k++) new_[k] = tmp_[k];
				adjMatrix_[i++] = new_;
			}
		}
		return adjMatrix_;
	}

	/**
	 Creates nodes.
	 @param endIndex_ exclusive.
	 */
	public static void createNodes(Component network_, int startIndex_, 
					int endIndex_)
	{ createNodes(network_, "n", startIndex_, endIndex_); }
	
	/**
	 Creates nodes.
	 @param endIndex_ exclusive.
	 */
	public static void createNodes(Component network_, String idPrefix_, 
					int startIndex_, int endIndex_)
	{
		if (network_ == null) return;
		if (idPrefix_ == null) idPrefix_ = "n";
		for (int i=startIndex_; i<endIndex_; i++)
			network_.addComponent(new Node(idPrefix_ + i));
	}
	
	/** Creates nodes.  */
	public static void createNodes(Component network_, int nNodes_)
	{ createNodes(network_, "n", 0, nNodes_); }
	
	/** Creates nodes.  */
	public static void createNodes(Component network_, String idPrefix_, 
					int nNodes_)
	{ createNodes(network_, idPrefix_, 0, nNodes_); }
	
	public static int[][] getAdjMatrixFromFile(String filename_) 
		throws Exception
	{
		int index_ = filename_.lastIndexOf(".");
		if (index_ < 0) {
			System.err.println("No file extension to tell the format");
			return null;
		}

		String format_ = filename_.substring(index_+1);
		format_ = format_.substring(0, 1).toUpperCase() + format_.substring(1);
		//System.out.println("Format: " + format_);
		String className_ = "drcl.net.graph.TopologyReader" + format_;

		try {
		drcl.net.graph.TopologyReader tr_ = (drcl.net.graph.TopologyReader)
				Class.forName(className_).newInstance();

		Reader r_ = new FileReader(filename_);
		tr_.parse(r_);
		//return tr_.getGraph().getAdjMatrix();
		int[][] adjMatrix_ = tr_.getGraph().getAdjMatrix();
		return adjMatrix_;
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException("Unrecognized format: " + format_);
		}
		catch (InstantiationException e) {
			throw new InstantiationException("cannot create reader class: "
							+ className_);
		}
		catch (FileNotFoundException e) {
			throw new FileNotFoundException("Cannot open file: " + filename_);
		}
		catch (Exception e) {
			throw e;
		}
	}

	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, null, null, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, null, null, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, null, null, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_,
									  boolean assignAddress_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, null, null, 
					assignAddress_); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, ids_, null, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_, 
					long[] ids_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, ids_, null, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, null, null, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_, 
					int[][] adjMatrix_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, null, null, 
					true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, null, null, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_, 
					int[][] adjMatrix_,
									  boolean assignAddress_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, null, null,
					assignAddress_); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, ids_, null, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_, 
					int[][] adjMatrix_, long[] ids_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, ids_, null, 
					true); }

	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, null, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_, 
					Link link_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, null, link_, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, null, link_, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_, 
					Link link_, boolean assignAddress_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, null, link_, 
					assignAddress_); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", null, 
	 adjMatrix_, ids_, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, int[][] adjMatrix_, 
					long[] ids_, Link link_)
	{ createTopology(network_, "n", "h", null, adjMatrix_, ids_, link_, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, null, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_, 
					int[][] adjMatrix_, Link link_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, null, link_, 
					true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, null, link_, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_,
					int[][] adjMatrix_, Link link_, boolean assignAddress_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, null, link_, 
					assignAddress_); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, "n", "h", existing_, 
	 adjMatrix_, ids_, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, Object[] existing_, 
					int[][] adjMatrix_, long[] ids_, Link link_)
	{ createTopology(network_, "n", "h", existing_, adjMatrix_, ids_, link_, 
					true); }
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, null, adjMatrix_, null, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_,
					int[][] adjMatrix_, Link link_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, null, 
					adjMatrix_, null, link_, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, null, adjMatrix_, ids_, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[],
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_,
					int[][] adjMatrix_, long[] ids_, Link link_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, null, adjMatrix_,
					ids_, link_, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, null, adjMatrix_, null, link_, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[], 
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_,
					int[][] adjMatrix_, Link link_, boolean assignAddress_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, null, adjMatrix_,
					null, link_, assignAddress_); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, existing_, adjMatrix_, null, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[], 
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_, 
					Object[] existing_, int[][] adjMatrix_, Link link_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, existing_, 
					adjMatrix_, null, link_, true); }

	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, existing_, adjMatrix_, null, link_, assignAddress_)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[], 
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_,
					Object[] existing_, int[][] adjMatrix_, Link link_,
					boolean assignAddress_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, existing_, 
					adjMatrix_, null, link_, assignAddress_); }

	/**
	 Creates a network topology based on an adjacency matrix.
	 The method calls <code>createTopology(network_, nodeIDPrefix_, 
	 nodeIDPrefix_, existing_, adjMatrix_, ids_, link_, true)</code>.
	 @see #createTopology(Component, String, String, Object[], int[][], long[], 
	 		Link, boolean)
	 */
	public static void createTopology(Component network_, 
					String routerIDPrefix_, String hostIDPrefix_, 
					Object[] existing_, int[][] adjMatrix_, long[] ids_,
					Link link_)
	{ createTopology(network_, routerIDPrefix_, hostIDPrefix_, existing_, 
					adjMatrix_, ids_, link_, true); }
	
	/**
	 Creates a network topology based on an adjacency matrix.
	 The adjacency matrix is a two-dimensional array.
	 The length of the first dimension, i.e., <code>adjMatrix_.length</code>,
	 is the number of nodes. Each element in the first dimension is a 
	 one-dimensional array, which represents the neighbors of the node.
	 The position of a neighbor in this one-dimensional array is the ID of
	 the port that the node uses to connect to the neighbor. Nodes are indexed 
	 as 0, 1,..., (<code>adjMatrix_.length</code>-1). The neighbors are 
	 represented by their indices. Each node may have a different number
	 of neighbors. As a result, the adjacency matrix looks like below:</p>
	 <pre>	<i>Port index</i> --&gt;	(port 0@)          (port 1@)
	  <i>node</i>	(node 0)        neighbor(0,0)      neighbor(0,1)     ... neighbor(0, <em>nb<sub>0</sub></em>)
	  <i>index</i>	(node 1)        neighbor(1,0)      neighbor(1,1)     ... neighbor(1, <em>nb<sub>1</sub></em>)
	   |	...
	   V	(node (<i>n-1</i>))    neighbor((<i>n-1</i>),0)  neighbor((<i>n-1</i>),1) ... neighbor((<i>n-1</i>), <em>nb<sub>n-1</sub></em>)
	 </pre>
	 <p>where <i>n</i> is the number of nodes, <i>nb<sub>i</sub></i> is the number of
	 neighbors of node <i>i</i>.
	 
	 The method distiguishes hosts and routers when specifying node IDs.
	 Hosts are nodes with exactly one neighbor.
	 Routers are nodes with more than one neighbor.
	 Nodes are not created if it has no neighbor.
	 
	 @param network_ the component to put all the created nodes and links in.
	 @param routerIDPrefix_ ID prefix for routers.
	 	The component ID of a node created is set to the concatenation of the 
		ID prefix and the node index in the adjacency matrix. If a node has 
		more than one connection, then it is considered as a router and this 
		argument is used as its ID prefix to construct its component ID.
	 @param hostIDPrefix_ ID prefix for hosts.  If a node has none or one 
	 	connection, then it is considered as a host, and this argument is used 
		as its ID prefix to construct its component ID. 
		See <code>routerIDPrefix_</code>.
	 @param existing_ Array of the existing nodes/networks that are used in 
	 	constructing the topology One may use existing components in creating 
		the topology. The existing components are put in an array. The indices 
		to this array are the node indices as to <code>adjMatrix_</code>. When
		creating the topology, a node is retrieved from this array, instead of
		created anew, only if the index does not excess the size of the array
		and the corresponding element in this array is not null.
	 @param adjMatrix_ The adjacency matrix.  See above.
	 @param ids_	One may explicitly specify the ID's for the nodes. 
	 	The indices to this array are the node indices as to 
		<code>adjMatrix_</code>.  If the array is not null, then its values 
		are used, instead of the node indices, to construct the component ID's.
	 	See <code>routerIDPrefix_</code>.
	 @param link_ the link component used to connect nodes.  Nodes are directly
	 	connected if <code>null</code> is specified.
	 @param assignAddress_ if true, this method will assign network addresses 
	 	to all newly created nodes based on the component ID's.
	 */
	public static void createTopology(
					Component network_, 
					String routerIDPrefix_,
					String hostIDPrefix_, 
					Object[] existing_,
					int[][] adjMatrix_,
					long[] ids_,
					Link link_,
					boolean assignAddress_)
	{
		if (network_ == null || adjMatrix_ == null) return;
		if (routerIDPrefix_ == null) routerIDPrefix_ = "n";
		if (hostIDPrefix_ == null) hostIDPrefix_ = "h";
		int nNodes_ = adjMatrix_.length;
		
		// create nodes
		Component[] nodes_ = new Component[nNodes_];
		for (int i=0; i<nNodes_; i++) {
			if (existing_ != null
				&& i < existing_.length && existing_[i] != null) {
				nodes_[i] = (Component)existing_[i];
				continue;
			}
			if (adjMatrix_[i] == null || adjMatrix_[i].length == 0)
				continue;
			Node node_ = new Node();
			long addr_ = ids_ == null? i: ids_[i];
			if (adjMatrix_[i].length == 1) // host
				node_.setID(hostIDPrefix_ + addr_);
			else // router
				node_.setID(routerIDPrefix_ + addr_);
			if (assignAddress_)
				node_.addAddress(addr_);
			nodes_[i] = node_;
			network_.addComponent(node_);
		}
		
		// create links according to adjMatrix_
		int nlinks_ = 0;
		for (int i=0; i<adjMatrix_.length; i++) {
			if (adjMatrix_[i] == null) continue;
			Component node1_ = nodes_[i];
			for (int j=0; j<adjMatrix_[i].length; j++) {
				int k = adjMatrix_[i][j]; // neighbor node id
				if (k < i) continue;
				if (adjMatrix_[k] == null) continue;
				int m = -1; 
				for (int l=0; l<adjMatrix_[k].length; l++)
					if (adjMatrix_[k][l] == i) { m = l; break; }
				if (m < 0) {
					drcl.Debug.error("InetUtil.createTopology(): " + k
									+ " is in " + i + "'s list but "
									+ i + " is not in " + k + "'s\n");
					continue;
				}

				Component node2_ = nodes_[k];
				Port p1_ = node1_.addPort(String.valueOf(j));
				Port p2_ = node2_.addPort(String.valueOf(m));
				if (link_ == null || node1_ == network_ || node2_ == network_) {
					p1_.connectTo(p2_);
					p2_.connectTo(p1_);
				}
				else {
					Link linkclone_ = (Link)link_.clone();
					linkclone_.setID(".link" + (nlinks_++));
					while (network_.containsComponent(linkclone_.getID())) {
						linkclone_.setID(".link" + (nlinks_++));
					}
					network_.addComponent(linkclone_);
					
					Port p_ = linkclone_.addPort("0");
					p_.connectTo(p1_);
					p1_.connectTo(p_);
					
					p_ = linkclone_.addPort("1");
					p_.connectTo(p2_);
					p2_.connectTo(p_);
				}
			}
		}
	}
	
	/**
	 Assigns network addresses to all nodes in the network by extracting from 
	 component IDs.
	 The method calls <code>setAddressByID(network_.getAllComponents())</code>.
	 @param network_ the network component.
	 @see #setAddressByID(Object[])
	 */
	public static void setAddressByID(Network network_)
	{
		setAddressByID(network_.getAllComponents());
	}
	
	/**
	 Assigns a network address to the argument node by extracting from its 
	 component ID.
	 */
	public static void setAddressByID(Node node_)
	{
		setAddressByID(new Node[]{node_});
	}

	/**
	 Assigns network addresses to nodes by extracting from component IDs.
	 @param nodes_ array of components.  Components that are not 
	 	<code>drcl.inet.Node</code> are omitted processed.
	 */
	public static void setAddressByID(Object[] nodes_)
	{
		int idPrefixLength_ = -1;
		
		for (int i=0; i<nodes_.length; i++) {
			if (!(nodes_[i] instanceof Node)) continue;
			Node node_ = (Node)nodes_[i];
			if (idPrefixLength_ < 0) {
				idPrefixLength_ = _getIDPrefixLength(node_.getID());
				if (idPrefixLength_ < 0) continue;
			}
			try {
				node_.addAddress(Long.valueOf(node_.getID().substring(
												idPrefixLength_)).longValue());
			}
			catch (Exception e_) {
				int tmp_ = _getIDPrefixLength(node_.getID());
				if (tmp_ >= 0) {
					idPrefixLength_ = tmp_;
					node_.addAddress(Long.valueOf(node_.getID().substring(
												idPrefixLength_)).longValue());
				}
			}
		}
	}
	
	public static void setIDByAddress(Component network_)
	{
		setIDByAddress(network_.getAllComponents());
	}
	
	public static void setIDByAddress(Object[] nodes_)
	{
		try {
			for (int i=0; i<nodes_.length; i++) {
				if (!(nodes_[i] instanceof Node)
					&& !(nodes_[i] instanceof Network)) continue;
				Component comp_ = (Component)nodes_[i];
				comp_.setID(comp_.getID() + "###");
			}
			for (int i=0; i<nodes_.length; i++) {
				if (!(nodes_[i] instanceof Node)
					&& !(nodes_[i] instanceof Network)) continue;
				Component comp_ = (Component)nodes_[i];
				String id_ = comp_.getID();
				int tmp_ = _getIDPrefixLength(id_);
				String idtail_ = null;
				if (comp_ instanceof Node) {
					idtail_ = String.valueOf(((Node)comp_).getDefaultAddress());
				}
				else {
					idtail_ = ((Network)comp_).getNetworkAddr() + "_"
							+ (-((Network)comp_).getNetworkMask());
				}
				comp_.setID(id_.substring(0, tmp_) + idtail_);
				
				if (comp_ instanceof Network)
					setIDByAddress(comp_);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}
	
	
	// returns the index of the first digit character
	static int _getIDPrefixLength(String id_)
	{
		for (int j=0; j<id_.length(); j++)
			if (Character.isDigit(id_.charAt(j))) return j;
		return -1;
	}
	
	public static void setAddressByCIDR(Network network_)
	{	setAddressByCIDR(network_, 0L);	}
	
	public static void setAddressByCIDR(Network network_, long baseAddress_)
	{
		_setCIDRSize(network_);
		_setAddressByCIDR(network_.getAllComponents(), baseAddress_, false);
		network_.setNetworkAddr(baseAddress_);
		network_.setNetworkMask(-network_.getNetworkMask());
	}
	
	// returns the exponent of power of 2's
	static int __getExp(long n_)
	{
		int exp_ = 0; // get exponent
		long tmp_ = 1;
		for (; tmp_ < n_; tmp_ <<= 1) exp_++;
		return exp_;
	}
	
	// the network mask of each subnetwork (including network_) will be set to
	// the number of addresses allocated to the subnetwork
	static long _setCIDRSize(Network network_)
	{	
		long numNodes_ = 0;
		Component[] comp_ = network_.getAllComponents();
		for (int i=0; i<comp_.length; i++) {
			if (comp_[i] instanceof Node) {
				numNodes_++;
			}
			else if (comp_[i] instanceof Network) {
				Network net_ = (Network)comp_[i];
				numNodes_ += _setCIDRSize(net_);;
			}
		}
		long tmp_;
		for (tmp_ = 1; tmp_ < numNodes_; tmp_ <<= 1);
		network_.setNetworkMask(tmp_);
		return tmp_;
	}
	
	public static void setAddressByCIDR(Object[] nodes_)
	{ _setAddressByCIDR(nodes_, 0L, true); }

	public static void setAddressByCIDR(Object[] nodes_, long baseAddress_)
	{ _setAddressByCIDR(nodes_, 0L, true); }
	
	static void _setAddressByCIDR(Object[] nodes_, long baseAddress_,
					boolean setCIDRSize_)
	{
		// put subnets in the vector indexed by the CIDR "size"
		Vector vnetworks_ = new Vector(64); // i'th, networks of 2^^i 
		vnetworks_.setSize(64);
		Vector vnodes_ = new Vector();
		for (int i=0; i<nodes_.length; i++) {
			Vector v_ = vnodes_;
			
			if (nodes_[i] instanceof Node) {
				// just put it in vnodes_, see below
			}
			else if (nodes_[i] instanceof Network) {
				Network net_ = (Network)nodes_[i];
				long size_ = setCIDRSize_?
						_setCIDRSize(net_): net_.getNetworkMask();
				int exp_ = __getExp(size_);
				v_ = (Vector)vnetworks_.elementAt(exp_);
				if (v_ == null) {
					v_ = new Vector();
					vnetworks_.setElementAt(v_, exp_);
				}
			}
			else
				continue;
			
			// put nodes/networks in the ascending order of ID's
			Component comp_ = (Component)nodes_[i];
			int place_ = 0;
			String id_ = comp_.getID();
			for (; place_ < v_.size(); place_++) {
				String thatid_ = ((Component)v_.elementAt(place_)).getID();
				if (id_.compareTo(thatid_) < 0) break;
			}
			if (place_ >= v_.size())
				v_.addElement(comp_);
			else
				v_.insertElementAt(comp_, place_);
		}
		
		// assign addresses from the largest subnetworks
		for (int i=vnetworks_.size()-1; i>=0; i--) {
			Vector v_ = (Vector)vnetworks_.elementAt(i);
			if (v_ == null) continue;
			for (int j=0; j<v_.size(); j++) {
				Network net_ = (Network)v_.elementAt(j);
				long size_ = net_.getNetworkMask();
				_setAddressByCIDR(net_.getAllComponents(), baseAddress_, false);
				net_.setNetworkMask(-size_);
				net_.setNetworkAddr(baseAddress_);
				baseAddress_ += size_;
			}
		}
		
		// assign addresses to nodes
		for (int i=0; i<vnodes_.size(); i++) {
			Node node_ = (Node)vnodes_.elementAt(i);
			node_.removeAddress(node_.getDefaultAddress());
			node_.addAddress(baseAddress_++);
		}
	}
	
	/** Returns a traffic generator component that can be attached to core 
	 * service layer. */
	public static Component createTrafficSource(TrafficModel trafficModel_, 
					Node dest_)
	{ return createTrafficSource(trafficModel_, dest_.getDefaultAddress()); }
	
	/** Returns a traffic generator component that can be attached to core 
	 * service layer. */
	public static Component createTrafficSource(TrafficModel trafficModel_, 
					long dest_)
	{
		Component src_ = TrafficAssistant.getTrafficComponent(trafficModel_);
		
		if (src_ instanceof TrafficSourceComponent)
			((TrafficSourceComponent)src_).setPacketWrapper(
				PktSending.getForwardPack(null, 0, Address.NULL_ADDR,
						dest_, false, 255/*TTL*/, InetPacket.DATA/*ToS*/));
		
		return src_;
	}

	/** Returns a ``shaped'' traffic source component that can be attached to 
	 * core service layer. */
	public static TrafficSourceComponent createTrafficSource(
					TrafficModel trafficSourceModel_, 
					TrafficModel trafficShaperModel_,
					long dest_)
	{
		TrafficSourceComponent src_ = TrafficAssistant.getTrafficSource(
						trafficSourceModel_, trafficShaperModel_);
		
		src_.setPacketWrapper(PktSending.getForwardPack(null, 0,
			Address.NULL_ADDR, dest_, false, 255/*TTL*/, 
			InetPacket.DATA/*ToS*/));
		
		return src_;
	}

	/** Returns a traffic generator component that generates Inet packets. */
	public static Component createTrafficSource(
					TrafficModel trafficModel_,
					long src_,
					long dest_, 
					long tos_)
	{
		Component srccomp_ = TrafficAssistant.getTrafficComponent(
						trafficModel_);
		
		if (srccomp_ instanceof TrafficSourceComponent) {
			((TrafficSourceComponent)srccomp_).setPacketWrapper(
				PktSending.getForwardPack(null, 0,
				src_, dest_, false, 255/*TTL*/, tos_));
		}
		
		return srccomp_;
	}
	
	/** Returns a ``shaped'' traffic source component that generates Inet 
	 * packets. */
	public static TrafficSourceComponent createTrafficSource(
					TrafficModel trafficSourceModel_, 
					TrafficModel trafficShaperModel_,
					long src_, long dest_, long tos_)
	{
		TrafficSourceComponent srccomp_ =
				TrafficAssistant.getTrafficSource(trafficSourceModel_,
						trafficShaperModel_);
		
		srccomp_.setPacketWrapper(PktSending.getForwardPack(null, 0, src_, 
								dest_, false, 255/*TTL*/, tos_));
		
		return srccomp_;
	}
	
	/** Creates a traffic generator component that generates Inet packets 
	 and installs it on the source node.
	 @param id_ the component ID of the generator.
	 @param protocol_ the protocol ID for the generator.
	*/
	public static Component createTrafficSource(
					TrafficModel trafficModel_,
					String id_,
					Node src_,
					Node dest_,
					long tos_,
					int protocol_)
	{
		Component srccomp_ = TrafficAssistant.getTrafficComponent(
						trafficModel_);
		
		if (srccomp_ instanceof TrafficSourceComponent) {
			((TrafficSourceComponent)srccomp_).setPacketWrapper(
				PktSending.getForwardPack(
						null, 0,
						src_.getDefaultAddress(),
						dest_.getDefaultAddress(),
						false, 255/*TTL*/, tos_));
			srccomp_.setID(id_);
			src_.addComponent(srccomp_);
			src_.add(srccomp_, ID_CSL, protocol_);
		}
		
		return srccomp_;
	}
	
	/** Creates a ``shaped'' traffic source component that generates Inet 
	 * packets
	 * and installs it on the source node.
	 * @param id_ the component ID of the generator.
	 * @param protocol_ the protocol ID for the generator.
	 */
	public static TrafficSourceComponent createTrafficSource(
					TrafficModel trafficSourceModel_,
					TrafficModel trafficShaperModel_,
					String id_, Node src_, Node dest_,
					long tos_, int protocol_)
	{
		TrafficSourceComponent srccomp_ =
				TrafficAssistant.getTrafficSource(trafficSourceModel_,
						trafficShaperModel_);
		
		srccomp_.setPacketWrapper(PktSending.getForwardPack(
								null, 0,
								src_.getDefaultAddress(),
								dest_.getDefaultAddress(),
								false, 255/*TTL*/, tos_));
		srccomp_.setID(id_);
		src_.addComponent(srccomp_);
		src_.add(srccomp_, ID_CSL, protocol_);
		
		return srccomp_;
	}
	
	/** Prints the network address and mask of the specified network.  */
	public static String toString(Network network_)
	{
		String saddr_ = __toBinary(network_.getNetworkAddr());
		String smask_ = __toBinary(network_.getNetworkMask());
		int nb_ = __getNB(network_);
		return "addr = " + saddr_.substring(nb_)
				+ "\nmask = " + smask_.substring(nb_) + "\n";
	}
	
	static int __getNB(Network network_)
	{
		for(; network_.parent instanceof Network; )
			network_ = (Network)network_.parent;
		String saddr_ = __toBinary(network_.getNetworkAddr());
		String smask_ = __toBinary(network_.getNetworkMask());
		int nb_ = Math.max(saddr_.indexOf("1"), smask_.indexOf("0")) - 1;
		if (nb_ <= 0) nb_ = 60;
		nb_ = nb_ >> 2 << 2;
		return nb_;
	}
	
    static String __toBinary(long n_) 
	{
		StringBuffer sb_ = new StringBuffer();

		long p_ = 1;
		for (int i = 0 ; i < 64; i++) {
			sb_.insert(0, ((p_ & n_) != 0)? "1": "0");
			p_ <<= 1;
		}
		return sb_.toString();
    }
	
	public static void setupVIF(Node n1_, int vifindex1_,
								Node n2_, int vifindex2_)
	{ setupVIF(n1_, vifindex1_, -1, n2_, vifindex2_, -1);	}
	
	public static void setupVIF(Node n1_, int vifindex1_,
								Node n2_, int vifindex2_, int mtu_)
	{ setupVIF(n1_, vifindex1_, mtu_, n2_, vifindex2_, mtu_);	}
	
	/**
	 Sets up virtual interfaces between two nodes.
	 @param mtu_ may be less than zero if the interface has the maximum MTU.
	 */
	public static void setupVIF(Node n1_, int vifindex1_, int mtu1_, 
								Node n2_, int vifindex2_, int mtu2_)
	{
		long nid1_ = n1_.getDefaultAddress();
		long nid2_ = n2_.getDefaultAddress();
		
		CoreServiceLayer csl1_ =
				(CoreServiceLayer)n1_.getComponent(Node.ID_CSL);
		if (csl1_ == null) {
			drcl.Debug.error("No CSL in " + n1_
							+ " to set up the VIF component", false);
			return;
		}
		CoreServiceLayer csl2_ =
				(CoreServiceLayer)n2_.getComponent(Node.ID_CSL);
		if (csl2_ == null) {
			drcl.Debug.error("No CSL in " + n2_
							+ " to set up the VIF component", false);
			return;
		}
		csl1_.setupVIF(vifindex1_, nid2_, mtu1_);
		csl2_.setupVIF(vifindex2_, nid1_, mtu2_);
	}
	
	
	/**
	 Configures the "host" routing entries and the interface info
	 at the area boundary routers.  In the INET framework, an area
	 is encapsulated in a {@link Network} component. 
	 An area boundary router is defined
	 as the one that has one or more connections (to other hosts/routers)
	 that either cross its parent (Network) component's boundary or
	 are connected to hosts.  A host is a node that has only one connection
	 AND does not contain routing table in it.
	 */
	public static void configure(Component network_)
	{
		if (network_ == null) return;
		configure(network_.getAllComponents());
	}
	
	/**
	 Configures the "host" routing entries and the interface info
	 at the area boundary routers.  In the INET framework, an area
	 is encapsulated in a {@link Network} component. 
	 An area boundary router is defined
	 as the one that has one or more connections (to other hosts/routers)
	 that either cross its parent (Network) component's boundary or
	 are connected to hosts.  A host is a node that has only one connection
	 AND does not contain routing table in it.
	 */
	public static void configure(Object[] nodes_)
	{
		if (nodes_ == null) return;
		try {
		for (int i=0; i<nodes_.length; i++) {
			if (nodes_[i] instanceof Node) {
				Node node_ = (Node)nodes_[i];
				if (node_.hasRoutingCapability()) {
					//System.out.println(node_ + " has routing: setup Interface");
					_setupInterfaceInfo(node_);
				}
				else {
					_setupHostRTEntry(node_);
				}
			}
			else if (nodes_[i] instanceof Network) {
				Network net_ = (Network)nodes_[i];
				configure(net_);
			}
			else
				continue;
		}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/**
	 * Calls {@link #configureFlat(Object[], boolean, boolean)
	 * configureFlat(network_.getAllComponents(), true, true)}. 
	 */
	public static void configureFlat(Component network_)
	{ configureFlat(network_, true, true); }

	/**
	 * Calls {@link #configureFlat(Object[], boolean, boolean)
	 * configureFlat(network_.getAllComponents(), reassignNodeID_,
	 * setupInterface_)}. 
	 */
	public static void configureFlat(Component network_,
					boolean reassignNodeID_, boolean setupInterface_)
	{
		if (network_ == null) return;
		configureFlat(network_.getAllComponents(), reassignNodeID_,
						setupInterface_);
	}
	
	/**
	 * Calls {@link #configureFlat(Object[], boolean, boolean)
	 * configureFlat(nodes_, true, true)}. 
	 */
	public static void configureFlat(Object[] nodes_)
	{ configureFlat(nodes_, true, true); }

	/**
	 Sets up the "host" routing entries and the interface info
	 at the area boundary routers in the flat network.
	 In a flat network, an area (or subnet) consists of a router and
	 one or more hosts.
	 The router is called the area boundary router (ABR).
	 ABR may connect to one or more routers outside the area.
	 A host is a node that has only one connection
	 AND does not contain routing table in it.

	 <p>The area can be represented by an area address in the format of
	 a base address and a mask.

	 <p>The interfaces of an ABR connected to other routers (outside of the
	 area) are on the boundary of the area and called boundary interfaces.

	 The method performs three tasks (two are optional and controlled by
	 two flags in the arguments):
	 (1) set up routing table entries in each ABR for the ABR to reach
	     the hosts that are connected to it;
	 (2) re-assign node IDs (addresses) to ABRs and hosts (optional);
	 (3) put the area/subnet address to the boundary interfaces of ABRs
	     (optional).
		This information can be used
		by a dynamic routing protocol for distributing route entries in
		a more compact fashion.

	 @param nodes_ contains all the ABRs to be configured.
	 @param reassignNodeID_ set to true if one wishes to re-assign node
	  	IDs (addresses) automatically; set to false to keep current node
		IDs.
	 @param setupInterface_ set to true if one wishes to set up area/subnet
	    address on the boundary interfaces.
	 */
	public static void configureFlat(Object[] nodes_,
					boolean reassignNodeID_, boolean setupInterface_)
	{
		if (nodes_ == null) return;
		try {
			long baseAddr_ = 0;
			for (int i=0; i<nodes_.length; i++) {
				if (nodes_[i] instanceof Node) {
					Node node_ = (Node)nodes_[i];
					if (node_.hasRoutingCapability())
						// if reassignNodeID_ is false, baseAddr_ is ignored
						baseAddr_ = _setupFlat(baseAddr_, node_, 
										reassignNodeID_, setupInterface_);
				}
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}
	
	// set up address and interfaces for flat network
	// return new base address
	static long _setupFlat(long baseAddr_, Node router_,
					boolean reassignNodeID_, boolean setupInterface_)
	{
		//System.out.println("set up interface info for " + router_);
		try {
			Port[] pp_ = router_.getAllPorts(Component.PortGroup_DEFAULT_GROUP);
			int nhosts_ = 0;
			HashMap vneighbor_ = new HashMap(); // if --> neighbors (vector)

			// the following is for finding subnet mask when reassignNodeID_ is
			// off
			long maxAddrDiff_ = 0;
			if (!reassignNodeID_)
				baseAddr_ = router_.getDefaultAddress();

			// check each router port
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				int if_ = -1;
				try {
					if_ = Integer.parseInt(p_.getID());
				} catch (Exception e_) {
					continue;
				}
				// vector of hosts that are on subnet reached by p_
				Vector vtmp_ = new Vector();
				vneighbor_.put(new Integer(if_), vtmp_);
				// count # of hosts
				Port[] peers_ = __getPeers(p_);
				for (int j=0; j<peers_.length; j++) {
					Port peer_ = peers_[j];
					Component peerHost_ = peer_.getHost();
					if (!(peerHost_ instanceof Node)) continue;
					Node node_ = (Node)peerHost_;
					if (node_.hasRoutingCapability()) {
						// FIXME:
						// to simplify, we assume no hosts on this interface
						// (no host exists on subnet containing two or more
						// routers) 
						break;
					}
					else {
						nhosts_ ++;
						vtmp_.addElement(node_);
						if (!reassignNodeID_) {
							long xor_ = node_.getDefaultAddress() ^ baseAddr_;
							if (xor_ > maxAddrDiff_) maxAddrDiff_ = xor_;
						}
					}
				}
			}

			if (nhosts_ == 0) {
				if (reassignNodeID_) {
					// complaint: should have method to change default address
					router_.removeAddress(router_.getDefaultAddress());
					router_.addAddress(baseAddr_++);
				}
				return baseAddr_;
			}

			long tmp_;

			if (reassignNodeID_) {
				// get the subnet address by counting #of hosts in the subnet
				for (tmp_ = 1; tmp_ <= nhosts_; tmp_ <<= 1);
				// allocate a block of addresses in power of 2
				if ((baseAddr_ % tmp_) > 0)
					baseAddr_ = (baseAddr_ / tmp_ + 1) * tmp_;
				// complaint: should have method to change default address
				router_.removeAddress(router_.getDefaultAddress());
				router_.addAddress(baseAddr_);
			}
			else { 
				for (tmp_ = 1; tmp_ <= maxAddrDiff_; tmp_ <<= 1);
			}

			long addr_ = baseAddr_+1;
			for (Iterator it_ = vneighbor_.keySet().iterator();
							it_.hasNext(); ) {
				Integer I = (Integer)it_.next();
				int i = I.intValue();
				// check neighbors at interface i
				Vector vtmp_ = (Vector)vneighbor_.get(I);
				if (vtmp_.size() == 0) {
					// interface i is connected to a router
					if (setupInterface_) {
						InterfaceInfo iinfo_ = router_.getInterfaceInfo(i);
						if (iinfo_ != null) {
							NetAddress local_ = iinfo_.getLocalNetAddress();
							if (local_ == null
								|| local_.getAddress() == Address.NULL_ADDR) {
								iinfo_.setLocalNetAddress(
											new NetAddress(baseAddr_, -tmp_));
								router_.setInterfaceInfo(i, iinfo_);
								//System.out.println("   modify local: "
								//	+ iinfo_.getLocalNetAddress());
							}
						}
						else {
							iinfo_ = new InterfaceInfo(
											new NetAddress(baseAddr_, -tmp_));
							router_.setInterfaceInfo(i, iinfo_);
							//System.out.println("   set local: "
							//	+ iinfo_.getLocalNetAddress());
						}
					}
				}
				else {
					// interface i is connected to hosts
					for (int j=0; j<vtmp_.size(); j++) {
						Node host_ = (Node)vtmp_.elementAt(j);
						if (reassignNodeID_) {
							host_.removeAddress(host_.getDefaultAddress());
							host_.addAddress(addr_);
						}
						else
							addr_ = host_.getDefaultAddress();
						RTKey rtkey_ = new RTKey(0, 0, addr_, -1, 0, 0);
						RTEntry rtentry_ = (RTEntry)router_.retrieveRTEntry(
										rtkey_, RTConfig.MATCH_LONGEST);
						if (rtentry_ == null || !rtentry_.getOutIf().get(i)) {
							rtentry_ = new RTEntry(
										addr_,
										new drcl.data.BitSet(new int[]{i}),
										HOST_ENTRY_EXT);
							//System.out.println("Adds rt entry to "
							//	+ router_ + ": " + rtkey_ + ", " + rtentry_);
							router_.addRTEntry(rtkey_, rtentry_, Double.NaN);
						}
						addr_++;
					}
				}
			}
			baseAddr_ += tmp_;
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
		return baseAddr_;
	}
	
	static void _setupInterfaceInfo(Node router_)
	{
		//System.out.println("set up interface info for " + router_);
		Port[] pp_ = router_.getAllPorts(Component.PortGroup_DEFAULT_GROUP);
		for (int i=0; i<pp_.length; i++) {
			try {
				Port p_ = pp_[i];
				int if_ = Integer.parseInt(p_.getID());
				// determine what area boundary crossed at this interface (port)
				int level_ = 0;
				Network outest_ = null;
				//Port[] peers_ = p_.getPeers();
				Port[] peers_ = p_.getOutAncestors();
				//System.out.println("  " + p_.getID() + "@: "
				//				+ drcl.util.StringUtil.toString(peers_));
				for (int j=0; j<peers_.length; j++) {
					Port peer_ = peers_[j];
					Component peerHost_ = peer_.getHost();
					if (!(peerHost_ instanceof Network)
						|| !drcl.comp.Util.contains(peerHost_, router_)) {
						//System.out.println("     " + peerHost_);
						continue;
					}
					// count the level
					Component tmp_ = router_;
					for (int k=0; true; k++) {
						if (tmp_ == peerHost_) {
							if (k > level_)
							{ level_ = k; outest_ = (Network)peerHost_; }
							break;
						}
						tmp_ = tmp_.parent;
					}
				}
			
				//System.out.println("     find outest nework " + outest_
				//	+ " at if " + p_.getID());
				if (outest_ == null) continue;
				
				InterfaceInfo iinfo_ = router_.getInterfaceInfo(if_);
				if (iinfo_ != null) {
					NetAddress local_ = iinfo_.getLocalNetAddress();
					if (local_.getAddress() == Address.NULL_ADDR) {
						iinfo_.setLocalNetAddress(new NetAddress(
										router_.getDefaultAddress(),
										outest_.getNetworkMask()));
						router_.setInterfaceInfo(if_, iinfo_);
						//System.out.println("   modify local: "
						//	+ iinfo_.getLocalNetAddress());
					}
				}
				else {
					iinfo_ = new InterfaceInfo(new NetAddress(
											router_.getDefaultAddress(), 
											outest_.getNetworkMask()));
					router_.setInterfaceInfo(if_, iinfo_);
					//System.out.println("   set local: "
					//	+ iinfo_.getLocalNetAddress());
				}
			}
			catch (NumberFormatException e_) {} // not a standard interface port
		}
	}
	
	static void _setupHostRTEntry(Node host_)
	{
		//System.out.println("set up host rt entry for " + host_);
		Port out_ = host_.getPort("0");
		if (out_ == null) return;
		
		Port[] pp_ = __getPeers(out_);
		Port peer_ = null;
		Node router_ = null;
		// XX: use the first router in the list, should seek for
		// 		"designated router"
		for (int i=0; i<pp_.length; i++) {
			Node tmp_ = (Node)pp_[i].getHost();
			if (tmp_.parent == host_.parent	&& tmp_.hasRoutingCapability()) {
				router_ = tmp_;
				peer_ = pp_[i];
				break;
			}
		}
		if (router_ == null) return;
		
		// add the host RT entry
		RTKey rtkey_ = new RTKey(0, 0, host_.getDefaultAddress(), -1, 0, 0);
		int outif_ = Integer.parseInt(peer_.getID());
		RTEntry rtentry_ = (RTEntry)router_.retrieveRTEntry(rtkey_, 
						RTConfig.MATCH_LONGEST);
		if (rtentry_ == null || !rtentry_.getOutIf().get(outif_)) {
			rtentry_ = new RTEntry(
							host_.getDefaultAddress(),
							new drcl.data.BitSet(new int[]{outif_}),
							HOST_ENTRY_EXT);
			//System.out.println("Adds rt entry to " + router_
			//	+ ": " + rtkey_ + ", " + rtentry_);
			router_.addRTEntry(rtkey_, rtentry_, Double.NaN);
		}
	}
	
	// used by _setupHostRTEntry()
	// out_ is a port of a Node's
	// returns array of ports, hosts of which are Node.
	static Port[] __getPeers(Port out_)
	{
		Port[] pp_ = out_.getPeers();
		Vector vpeers_ = new Vector();
		for (int i=0; i<pp_.length; i++) {
			Component tmp_ = pp_[i].getHost();
			if (tmp_ instanceof Node) {
				if (vpeers_.indexOf(pp_[i]) < 0)
					vpeers_.addElement(pp_[i]);
			}
			else if (tmp_ instanceof Link) {
				___getPeers(vpeers_, tmp_, pp_[i]);
			}
		}
		
		Port[] result_ = new Port[vpeers_.size()];
		vpeers_.copyInto(result_);
		return result_;
	}
	
	// a recursive function used by __getPeers(Port)
	static void ___getPeers(Vector v_, Component link_, Port oneLinkPort_)
	{
		Port[] all_ = link_.getAllPorts();
		for (int j=0; j<all_.length; j++) {
			if (all_[j] == oneLinkPort_) continue;
			Port[] allpeers_ = all_[j].getPeers();
			for (int k=0; k<allpeers_.length; k++) {
				Component host_ = allpeers_[k].getHost();
				if (host_ instanceof Node) {
					if (v_.indexOf(allpeers_[k]) < 0)
						v_.addElement(allpeers_[k]);
				}
				else if (host_ instanceof Link)
					___getPeers(v_, host_, allpeers_[k]);
			}
		}
	}
	
	static drcl.inet.tool.routing_msp msp = null;
	static drcl.inet.tool.routing_msp2 msp2 = null;

	/**
	 * Sets up unicast routes (min-hop) in the network in one shot. 
	 * It sets up routes bi-directionally for every pair of nodes in the
	 * network.   Assumes that nodes are addressed at 0, 1, 2 and so on.
	 * It runs <code>n</code> times of the shortest path algorithm to
	 * compute a shortest path tree for each node as source. 
	 * This may take a long time for large networks. 
	 *
	 * @param network_ contains all the nodes to be configured. 
	 * @param adjMatrix_ adjacency matrix describing network topology. 
	 */
	public static void setupRoutes(Component network_, int[][] adjMatrix_)
	{
		if (network_ == null) throw new NullPointerException();

		if (msp2 == null) msp2 = new drcl.inet.tool.routing_msp2();
		msp2.setup(network_, adjMatrix_);
	}

	/**
	 * Sets up unicast routes (min-cost) in the network in one shot. 
	 * It sets up routes bi-directionally for every pair of nodes in the
	 * network.   Assumes that nodes are addressed at 0, 1, 2 and so on.
	 * It runs <code>n</code> times of the shortest path algorithm to
	 * This may take a long time for large networks. 
	 *
	 * @param nodes_ array of nodes to be configured. 
	 * @param adjMatrix_ adjacency matrix describing network topology. 
	 * @param linkcost_ cost matrix for network links; use downstream node
	 * 		index as the first argument and the interface index of the
	 * 		downstream node (to the upstream node) as the 2nd
	 * 		argument.  The interface index should be consistent with that
	 * 		in <code>adjMatrix_</code>.  For the format of adjMatrix,
	 * 		see {@link #createTopology(Component,String,
	 * 		String,Object[],int[][],long[],drcl.inet.Link,boolean)}
	 */
	public static void setupRoutes(Node[] nodes_, int[][] adjMatrix_,
					LinkCost linkcost_)
	{
		if (nodes_ == null || adjMatrix_ == null)
			throw new NullPointerException();

		if (msp2 == null) msp2 = new drcl.inet.tool.routing_msp2();
		msp2.setup(nodes_, adjMatrix_, linkcost_, false, true);
	}

	/**
	 * Same as {@link #setupRoutes(Component, int[][])} except that
	 * this method allows node addresses to be arbitrary as specified
	 * in <code>addr_</code>. 
	 *
	 * @param network_ contains all the nodes to be configured. 
	 * @param adjMatrix_ adjacency matrix describing network topology. 
	 * @param addr_ node addresses.  Indexing is matched between
	 * 		<code>addr_<code> and <code>adjMatrix_<code>.
	 */
	public static void setupRoutes(Component network_, int[][] adjMatrix_,
					long[] addr_)
	{
		if (network_ == null || adjMatrix_ == null || addr_ == null)
			throw new NullPointerException();

		if (msp2 == null) msp2 = new drcl.inet.tool.routing_msp2();
		msp2.setup(network_, adjMatrix_, addr_, null, false, true);
	}

	/**
	 * Same as {@link #setupRoutes(Component, int[][], LinkCost)}
	 * except that
	 * this method allows node addresses to be arbitrary as specified
	 * in <code>addr_</code>. 
	 *
	 * @param network_ contains all the nodes to be configured. 
	 * @param adjMatrix_ adjacency matrix describing network topology. 
	 * @param addr_ node addresses.  Indexing is matched between
	 * 		<code>addr_<code> and <code>adjMatrix_<code>.
	 * @param linkcost_ see {@link #setupRoutes(Node[],int[][],LinkCost)}.
	 */
	public static void setupRoutes(Component network_, int[][] adjMatrix_,
					long[] addr_, LinkCost linkcost_)
	{
		if (network_ == null || adjMatrix_ == null || addr_ == null)
			throw new NullPointerException();

		if (msp2 == null) msp2 = new drcl.inet.tool.routing_msp2();
		msp2.setup(network_, adjMatrix_, addr_, linkcost_, false, true);
	}

	public static void setupRoutes(Node src_, Node dest_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dest_);
	}
	
	public static void setupRoutes(Node src_, Node dest_, String bidirect_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dest_, bidirect_);
	}
	
	/**
	 Sets up routing table entries between two nodes.
	 Useful for hierarchical network where <code>dest_</code> could be a network
	 component with (<code>destAddr_</code>, <code>destAddrMask_</code>) as 
	 its network address.
	 @param destAddr_ destination address, could be a multicast address.
	 */
	public static void setupRoutes(Component src_, Component dest_, 
					long destAddr_, long destAddrMask_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dest_, destAddr_, destAddrMask_);
	}
	
	/**
	 Sets up routing table entries between the source node and multiple 
	 destination nodes.
	 @param destAddr_ destination address, could be a multicast address.
	 */
	public static void setupRoutes(Component src_, Object[] dest_, 
					long destAddr_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dest_, destAddr_);
	}
	
	/**
	 Sets up routing table entries between the source node and multiple 
	 destination nodes.
	 @param destAddr_ destination address, could be a multicast address.
	 */
	public static void setupRoutes(Component src_, Object[] dest_, 
					long destAddr_, long destAddrMask_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dest_, 0, 0, destAddr_, destAddrMask_);
	}
	
	/**
	 Sets up routing table entries between the source node and multiple 
	 destination nodes.
	 Useful for hierarchical network where <code>dest_</code> could be a network
	 component with (<code>destAddr_</code>, <code>destAddrMask_</code>) as 
	 its network address.
	 @param destAddr_ destination address, could be a multicast address.
	 */
	public static void setupRoutes(Component src_, Object[] dests_, 
					long srcAddr_, long srcAddrMask_,
					long destAddr_, long destAddrMask_)
	{
		if (msp == null) msp = new drcl.inet.tool.routing_msp();
		msp.setup(src_, dests_, srcAddr_, srcAddrMask_, destAddr_, 
						destAddrMask_);
	}

	/** Setup a route between two directly connected nodes.
	(c) 2002, Infonet Group, University of Namur, Belgium
	@author Bruno Quoitin (bqu@infonet.fundp.ac.be)
	@author Hung-ying Tyan
	 */
	public static void setupRoute(Node sourceNode, Node destNode)
	{
		// List of links connected to the source node
		HashMap sourceLinks= new HashMap();
		// Interface index to find
		int interfaceIndex= -1;

		//System.out.println("setupRoute: "+sourceNode+" -> "+destNode);

		// ---------------------------------------------------------
		// Find links connected to the source node and save the port
		// connected to each link.
		// ---------------------------------------------------------
		//System.out.println("Wires:");
		Port[] sourcePorts_= sourceNode.getAllPorts("");
		for (int i= 0; i < sourcePorts_.length; i++) {
			Port p = sourcePorts_[i];
			Wire outwire_ = p.getOutWire();
			if (outwire_ == null) continue;
			Port[] neighborPorts_ = outwire_.getInPorts();
			for (int j= 0; j < neighborPorts_.length; j++) {
				Port neighborPort_ = neighborPorts_[j];
				if ((neighborPort_.host instanceof Link) &&
					(!sourceLinks.containsKey(neighborPort_.host))) {
					sourceLinks.put(neighborPort_.host, p);
				}
			}
		}

		// --------------------------------------------------------
		// For links connected to the dest node, find the one that 
		// is also connected to the source node. The ID of the port
		// connected to the link on the source node.
		// --------------------------------------------------------
		Port[] destPorts_= destNode.getAllPorts("");
		for (int i= 0; i < destPorts_.length; i++) {
			Port p = destPorts_[i];
			Wire inwire_ = p.getInWire();
			if (inwire_ == null) continue;
			Port[] neighborPorts_ = inwire_.getOutPorts();
			for (int j= 0; j < neighborPorts_.length; j++) {
				Port neighborPort_ = neighborPorts_[j];
				if (neighborPort_.host instanceof Link) {
					Port srcPort_ = (Port)sourceLinks.get(neighborPort_.host);
					if (srcPort_ != null) {
						interfaceIndex = Integer.parseInt(srcPort_.getID());
						break;
					}
				}
			}
			if (interfaceIndex >= 0) break;
		}
	
		// --------------------------------------------------------
		// If the interface index has been found, set up the route.
		// --------------------------------------------------------
		//System.out.println("Interface:");
		if (interfaceIndex >= 0) {
			//System.out.println("\tindex: "+interfaceIndex);
			// Add entry in routing-table
			RTKey key= new RTKey(
							0, 0,
							destNode.getDefaultAddress(), -1,
							0, 0);
			drcl.data.BitSet bitset= new drcl.data.BitSet(sourceLinks.size());
			bitset.set(interfaceIndex);
			sourceNode.addRTEntry(
							key,
							new RTEntry(bitset, "neighbor route"),
							-1.0); // no timeout
		} else {
			System.out.println("Could not set up route from "
							+ sourceNode + " to " + destNode);
		}
	}

	/** Setup a bidiretional route between two directly connected
	nodes.
	(c) 2002, Infonet Group, University of Namur, Belgium
	@author Bruno Quoitin (bqu@infonet.fundp.ac.be)
	@version 15/07/2002
	*/
	public static void setupBRoute(Node sourceNode, Node destNode)
	{
		setupRoute(sourceNode, destNode);
		setupRoute(destNode, sourceNode);
	}

	// returns the first Link in p_'s peers' hosts.
	// returns the link's port that connects to p_
	static Port _getLinkPort(Port p_)
	{
		Port[] pp_ = p_.getPeers();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i].host instanceof Link) return pp_[i];
		return null;
	}

	// returns the first Node in p_'s peers' hosts.
	// returns the node's port that connects to p_
	static Port _getNodePort(Port p_)
	{
		Port[] pp_ = p_.getPeers();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i].host instanceof Node) return pp_[i];
		return null;
	}

	/**
	 Sets up a {@link drcl.inet.tool.NamTrace} on a network component.
	 This method creates a NamTrace component and hooks it up with all the 
	 nodes, links and queues in the network component.
	 Currently, this method only works for flat point-to-point network topology.
	 */
	public static drcl.inet.tool.NamTrace setNamTraceOn(
					Component net_,
					String filename_,
					String[] colors_)
	{
		drcl.inet.tool.NamTrace nam_ = new drcl.inet.tool.NamTrace("._nam_");
		setNamTraceOn(net_, nam_, filename_, colors_);
		return nam_;
	}

	/**
	 Sets up a {@link drcl.inet.tool.NamTrace} on a network component.
	 This method creates a NamTrace component and hooks it up with all the 
	 nodes, links and queues in the network component.
	 If <code>nam_</code> is not added to a component hierarchy, it is
	 added to <code>net_</code>.  A FileComponent is added to <code>net_</code>
	 and connected to <code>nam_</code> to save the NAM trace.
	 Currently, this method only works for flat point-to-point network topology.
	 */
	public static void setNamTraceOn(
					Component net_,
					drcl.net.tool.NamTrace nam_,
					String filename_,
					String[] colors_)
	{
		drcl.comp.io.FileComponent file_ = 
				new drcl.comp.io.FileComponent("._file_");
		file_.open(filename_);
		if (nam_.getParent() == null) net_.addComponent(nam_);
		net_.addComponent(file_);
		nam_.getPort("output").connectTo(file_.addPort("in"));
		if (colors_ != null && colors_.length > 0)
			nam_.addColors(colors_);
		setNamTraceOn(net_, nam_);
	}

	/**
	 Sets up a {@link drcl.net.tool.NamTrace} on a network component.
	 This method hooks up all the nodes, links and queues in the network 
	 component.  Currently, this method only works for flat point-to-point 
	 network topology.
	 */
	public static void setNamTraceOn(Component net_, 
					drcl.net.tool.NamTrace nam_)
	{
		try {
		Component[] comp_ = net_.getAllComponents();
		Hashtable ht_ = null;
		int i;
		// add node events and collect links
		Vector vlink_ = new Vector();
		for (i=0; i<comp_.length; i++) {
			if (comp_[i] instanceof Node)
				nam_.addNode(((Node)comp_[i]).getDefaultAddress(), "UP", 
								"circle", "blue", comp_[i].id);
			else if (comp_[i] instanceof Link)
				vlink_.addElement(comp_[i]);
		}
		// add link and queue events
		for (i=0; i<vlink_.size(); i++) {
			try {
				Link link_ = (Link)vlink_.elementAt(i);
				Port p1_ = _getNodePort(link_.getPort("0"));
				Node n1_ = (Node)p1_.host;
				Port p2_ = _getNodePort(link_.getPort("1"));
				Node n2_ = (Node)p2_.host;
				nam_.addLink(n1_.getDefaultAddress(),
					n2_.getDefaultAddress(), "UP",
					String.valueOf(n1_.getBandwidth(Integer.parseInt(p1_.id))),
					String.valueOf(link_.getPropDelay()), null);
				nam_.addQueue(n1_.getDefaultAddress(),
								n2_.getDefaultAddress(), "0.5");
				nam_.addQueue(n2_.getDefaultAddress(),
								n1_.getDefaultAddress(), "0.5");
			}
			catch (Exception e_) {
				// ignore this link, just continue
			}
		}
		// and set up nam_ ports
		for (i=0; i<comp_.length; i++) {
			try {
				if (comp_[i] instanceof Node) {
					Node n1_ = (Node)comp_[i];
					Port[][] ppp_ = n1_.getCSL().getNAMPacketEventPorts();
					for (int j=0; j<ppp_.length; j++) { // j: interface index
						Port p_ = null, p2_ = null;
						try {
							p_ = n1_.getPort(String.valueOf(j));
							p2_ = _getLinkPort(p_);
							Link link_ = (Link)p2_.host;
							if (link_.getPort("0") == p2_)
								p2_ = _getNodePort(link_.getPort("1"));
							else
								p2_ = _getNodePort(link_.getPort("0"));
							Node n2_ = (Node)p2_.host;
							long a1_ = n1_.getDefaultAddress();
							long a2_ = n2_.getDefaultAddress();
							// "enqueue" port
							ppp_[j][0].attachIn(nam_.addPort("+ -s " + a1_
													+ " -d " + a2_));
							// "dequeue" port
							// "hop" port
							ppp_[j][1].attachOut(new Port[]{
								nam_.addPort("- -s " + a1_ + " -d " + a2_),
								nam_.addPort("h -s " + a1_ + " -d " + a2_)
							});
							// "drop" port
							ppp_[j][2].disconnectOutWire();
								// because this is infoPort
							ppp_[j][2].attachOut(nam_.addPort("d -s " + a1_
													+ " -d " + a2_));
							ppp_[j][2].host.setGarbageEnabled(true);
							// "receive" port
							p2_.attachIn(nam_.addPort("r -s " + a1_ + " -d "
													+ a2_));
						}
						catch (Exception e_) {
							System.err.println(n1_);
							System.err.println(p_);
							System.err.println(p2_);
							e_.printStackTrace();
							// ignore this interface, just continue
						}
					}
				}
			}
			catch (Exception e_) {
				// ignore this component, just continue
			}
		}
		nam_.setComponentMessageFeedbackEnabled(true);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	static Hashtable htPID = null;

	/** Returns the protocol ID of the component if
	the component is a well-defined protocol.
	The protocol name is obtained from <code>Component.getName()</code>.
	@return -1 if the protocol ID is not defined. */
	public static int getPID(Component c_)
	{
		if (htPID == null) {
			htPID = new Hashtable();
			// "hello" is standard service in INET
			String[] protocols_ = {
				"icmp", "igmp", "hello", "tcp",
				"tcpsink", "egp", "pup", "udp",
				"idp", "rsvp", "pim", "esp",
				"ah", "ospf", "comp", "dv",
				"dvmrp"};
			int[] pids_ = {
				1, 2, PID_HELLO, PID_TCP,
				PID_TCP, 8, 12, PID_UDP,
				22, PID_RSVP, 103, 50,
				51, PID_OSPF, 108, PID_DV,
				PID_DVMRP};
			for (int i=0; i<protocols_.length; i++)
				htPID.put(protocols_[i], new Integer(pids_[i]));
		}
		Integer pid_ = (Integer)htPID.get(c_.getName());
		if (pid_ == null) return -1;
		else return ((Integer)pid_).intValue();
	}

	/* Propagates the address scheme through the modules.
	 @see Module
	public static void propagates(Address addr_, Object[] modules_)
	{
		if (modules_ == null) return;
		for (int i=0; i<modules_.length; i++) {
			if (modules_[i] instanceof Module) {
				((Module)modules_[i]).setAddress(addr_);
				propagates(addr_, ((Component)modules_[i]).getAllComponents());
			}
			else if (modules_[i] instanceof Component) {
				propagates(addr_, ((Component)modules_[i]).getAllComponents());
			}
		}
	}
	*/

	/** Connects two TCP components (TCP-TCPSink or TCPb's) with protocol ID
	 * automatically assigned.  * The TCP components must be added to a 
	 * {@link Node} before one can call this method. */
	public static void connectTCP(Component p1_, Component p2_)
	{
		for (int i=0; !connectTCP(p1_, p2_, i); i++);
	}

	/** Connects two TCP components (TCP-TCPSink or TCPb's) with the specified 
	 * protocol ID.  The TCP components must be added to a {@link Node} before
	 * one can call this method.  Returns false if the protcol ID is already
	 * in use. */
	public static boolean connectTCP(Component p1_, Component p2_, 
					int protocolID_)
	{
		if (!connect(p1_, p2_, protocolID_))
			return false;
		Component n1_ = p1_.getParent();
		if (n1_ instanceof Node) {
			if (p2_ instanceof drcl.inet.transport.TCP)
				((drcl.inet.transport.TCP)p2_).setPeer(
											((Node)n1_).getDefaultAddress());
		}
		Component n2_ = p2_.getParent();
		if (n2_ instanceof Node) {
			if (p1_ instanceof drcl.inet.transport.TCP)
				((drcl.inet.transport.TCP)p1_).setPeer(
											((Node)n2_).getDefaultAddress());
		}
		return true;
	}

	/** Connects two protocol components.  It assigns protocol ID 
	 * automatically. */
	public static void connect(Component p1_, Component p2_)
	{
		for (int i=0; !connect(p1_, p2_, i); i++);
	}

	/** Connects two protocol components with the specified protocol ID.  
	 * Returns false if the protcol ID is already in use. */
	public static boolean connect(Component p1_, Component p2_, int protocolID_)
	{
		try {
			String pID_ = String.valueOf(protocolID_);
			Port[] pp1_ = p1_.getPort(Module.PortGroup_DOWN).getPeers();
			Port[] pp2_ = p2_.getPort(Module.PortGroup_DOWN).getPeers();
			// check if protocolID_ is available
			for (int i=0; i<pp1_.length; i++) {
				Port p_ = pp1_[i];
				if (!p_.getID().equals(pID_)
					&& p_.getHost().getPort(p_.getGroupID(), pID_) != null)
					return false;
			}
			for (int i=0; i<pp2_.length; i++) {
				Port p_ = pp2_[i];
				if (!p_.getID().equals(pID_) 
					&& p_.getHost().getPort(p_.getGroupID(), pID_) != null)
					return false;
			}
			for (int i=0; i<pp1_.length; i++) {
				Port p_ = pp1_[i];
				if (p_.getID().equals(pID_))
					continue;
				Component host_ = p_.getHost();
				long previous_ = host_.getComponentFlag(
								Component.FLAG_PORT_NOTIFICATION);
				if (previous_ > 0)
					host_.setComponentFlag(
								Component.FLAG_PORT_NOTIFICATION, false);
				p_.setID(pID_);
				if (previous_ > 0)
					host_.setComponentFlag(
								Component.FLAG_PORT_NOTIFICATION, true);
			}
			for (int i=0; i<pp2_.length; i++) {
				Port p_ = pp2_[i];
				if (p_.getID().equals(pID_))
					continue;
				Component host_ = p_.getHost();
				long previous_ = host_.getComponentFlag(
								Component.FLAG_PORT_NOTIFICATION);
				if (previous_ > 0)
					host_.setComponentFlag(
								Component.FLAG_PORT_NOTIFICATION, false);
				p_.setID(pID_);
				if (previous_ > 0)
					host_.setComponentFlag(
								Component.FLAG_PORT_NOTIFICATION, true);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return false;
		}
		return true;
	}
}
