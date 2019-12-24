// @(#)TopologyParser.java   7/2003
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

/** Converts an adjacency matrix to topology data structure.
 * Assume symmetric link.
 */
public class TopologyParser
{
	public Node[] nodes;
	public Link[] links;
	public int[][] adjMatrix; // adjacency matrix

	public void reset()
	{
		nodes = null;
		links = null;
	}

	/** Assumes bidirectional links. */
	public Graph parse(int[][] adjMatrix_)
	{
		adjMatrix = adjMatrix_;
		nodes = new Node[adjMatrix.length];
		int nlinks_ = 0;
		for (int i=0; i<nodes.length; i++) {
			nodes[i] = new Node(i, 0, 0);
			nlinks_ += adjMatrix[i].length;
		}
		links = new Link[nlinks_/2]; // link symmetry

		int k = 0;
		for (int i=0; i<nodes.length; i++) {
			for (int j=0; j<adjMatrix[i].length; j++) {
				int to_ = adjMatrix[i][j];
				if (i < to_)
					links[k] = new Link(k++, nodes[i], nodes[to_]);
			}
		}

		Graph g = new Graph(nodes, links);
		return g;
	}

	public String info()
	{
		if (nodes == null || nodes.length == 0)
			return "Nothing is read.\n";
		if (links == null) links = new Link[0];
		StringBuffer sb_ = new StringBuffer(nodes.length + " nodes, "
						+ links.length + " links\n");
		sb_.append("\nNodes: " + nodes.length + "\n");
		for (int i=0; i<nodes.length; i++)
			sb_.append(nodes[i] + "\n");
		sb_.append("\nLinks: " + links.length + "\n");
		for (int i=0; i<links.length; i++)
			sb_.append(links[i] + "\n");
		sb_.append("Adjacency matrix: "
					   	+ drcl.util.StringUtil.toString(adjMatrix) + "\n");
		return sb_.toString();
	}
}
