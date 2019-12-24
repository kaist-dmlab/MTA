// @(#)AdjMatrixVis.java   7/2003
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

import java.io.*;

// Visualize an adjacency matrix, use TopologyParser to convert adjacency
// matrix to topology data structure.
public class AdjMatrixVis
{
	public static void visualize(int[][] adjMatrix_)
	{ visualize(adjMatrix_, false); }

	public static void visualize(int[][] adjMatrix_, boolean excludeStub_)
	{
		TopologyParser parser_ = new TopologyParser();
		Graph g = parser_.parse(adjMatrix_);

		if (parser_.nodes.length < 20)
			System.out.println(parser_.info());
		else
			System.out.println("visualize it...");

		int width_ = 400, height_ = 300;
		long seed_ = 0;
		TopologyVisRelaxer t_ = new TopologyVisRelaxer(width_, height_);
		Relaxer relaxer_ = new Relaxer(g , t_.getWidth(), t_.getHeight(),
					   	1, seed_);
		if (excludeStub_) relaxer_.excludeStub();
		t_.visualize(relaxer_);
	}
}
