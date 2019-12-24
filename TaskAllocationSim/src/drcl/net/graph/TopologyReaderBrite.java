// @(#)TopologyReaderBrite.java   7/2003
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
import java.util.regex.Pattern;

public class TopologyReaderBrite extends TopologyReader
{
	static boolean DEBUG = false;
	String parameter = null;

	public String parameter()
	{ return parameter; }

	public void parse(Reader r_) throws Exception
	{
		BufferedReader r = new BufferedReader(r_);

		StringBuffer sbParameter = new StringBuffer();
		String line_ = null;
		while (true) {
			line_ = r.readLine();
			if (line_.trim().length() == 0) continue;
			if (line_.startsWith("Nodes:")) break;
			sbParameter.append(line_ + "\n");
		}
		parameter = sbParameter.toString();

		// read nodes
		try {
		Pattern pattern_ = Pattern.compile("[ \t]");
		int end_ = line_.indexOf(")");
		nodes = new Node[Integer.parseInt(line_.substring(8, end_).trim())];
			// "Nodes: (###)"
		if (DEBUG)
			System.out.println(nodes.length + " nodes to be read....");
		for (int i=0; i<nodes.length; i++) {
			while (true) {
				line_ = r.readLine();
				if (line_.trim().length() > 0) break;
			}
			String[] ll_ = pattern_.split(line_);
			double x = Double.valueOf(ll_[1]).doubleValue();
			double y = Double.valueOf(ll_[2]).doubleValue();
			// suppose index to be from 0
			nodes[i] = new Node(i, x, y);
		}

		// skip empty lines
		while (true) {
			line_ = r.readLine().trim();
			if (line_.length() > 0) break;
		}

		// read lines
		end_ = line_.indexOf(")");
		links = new Link[Integer.parseInt(line_.substring(8, end_).trim())];
			// "Edges: (###):"
		if (DEBUG)
			System.out.println(links.length + " links to be read....");
		for (int i=0; i<links.length; i++) {
			while (true) {
				line_ = r.readLine();
				if (line_.trim().length() > 0) break;
			}
			String[] ll_ = pattern_.split(line_);
			int i1 = Integer.parseInt(ll_[1]);
			int i2 = Integer.parseInt(ll_[2]);
			// suppose from 0
			links[i] = new Link(i, nodes[i1], nodes[i2]);
		}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			System.err.println(parameter);
			System.err.println("Line: " + line_);
			System.exit(1);
		}
	}
}
