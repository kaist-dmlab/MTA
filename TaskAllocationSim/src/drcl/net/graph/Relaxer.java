// @(#)Relaxer.java   7/2003
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

// This class is based on Graph.java in the demo package included in Sun's
// J2SDK distribution and the following is the copyright notice and disclaimer
// in the original Graph.java. 
/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

import java.util.*;

/** Given a graph, iterates to get to the equilibrium point where forces
 * by edge length and "push" by nodes are balanced. */
public class Relaxer
{
	public static final int VERTICAL = 1;
	public static final int HORIZONTAL = 0;
	static final int[] PREF_MASK = {
			0xFFFF0000, 0xFFFF0000};

	static final boolean TESTING = true;

	class MyNode
	{
		double dx, dy;
		boolean fixed;
		boolean showing = true;
		int wallPreference = 0x00000000; //vertical:horizontal

		int getVPref()
		{ return wallPreference >> 16; }

		int getHPref()
		{ return wallPreference & 0x0ffff; }

		int getPref(int wall_)
		{ return (wallPreference >> (wall_<<4)) & 0x0ffff; }

		public String toString()
		{
			return dx + "," + dy + (fixed? ",fixed":"")
					+ ",pref=" + getHPref() + "-" + getVPref();
		}
	}

	/** Sets wall preference for nodeID_.
	 * Larger value, higher preference. Valid values: 0-16383.
	 * wall_ must be one of {0, 1}, corresponding to
	 * horizontal, vertical.
	 */
	public void setWallPref(int nodeID_, int wall_, int pref_)
	{
		if (wall_ < 0) wall_ = -wall_;
		if (wall_ > 2) wall_ = wall_ % 2;
		if (pref_ > 16383) pref_ = 16383;
		else if (pref_ < 0) pref_ = 0;
		mynodes[nodeID_].wallPreference &= PREF_MASK[wall_];
		mynodes[nodeID_].wallPreference |= (pref_ << (wall_<<4));
		maxPref[wall_] = _calculateMaxPref(wall_);
	}

	// calculates the maximum preference in 'mynodes'
	int _calculateMaxPref(int wall_)
	{
		if (wall_ < 0) wall_ = -wall_;
		if (wall_ > 2) wall_ = wall_ % 2;
		wall_ = wall_ << 4;
		int max_ = 0;
		for (int i=0; i<mynodes.length; i++) {
			int pref_ = (mynodes[i].wallPreference >> wall_) & 0x0ffff;
			if (pref_ > max_) max_ = pref_;
		}
		return max_+1;
	}

	// max preference
	int[] maxPref = new int[2];

	Graph graph;
	int width, height;
	int times;
	long seed;

	int nnodes, nedges;
	Node[] nodes;
	Link[] edges;
	MyNode[] mynodes;
	Random rand = new Random();

	boolean random;

	public Relaxer()
	{}

	public Relaxer(Graph g_, int width_, int height_, int times_,
					long seed_)
	{
		config(g_, width_, height_, times_, seed_);
	}

	public void config(Graph graph_, int width_, int height_, int times_,
					long seed_)
	{
		if (graph_ != graph) {
			graph = graph_;
			nodes = graph.nodes();
			edges = graph.links();
			nnodes = nodes.length;
			nedges = edges.length;
		}

		mynodes = new MyNode[nnodes];
		for (int i=0; i<nnodes; i++)
			mynodes[i] = new MyNode();

		width = width_;
		height = height_;
		times = times_;
		seed = seed_;
		rand.setSeed(seed);

		// testing preference
		if (TESTING) { 
			if (nnodes == 9 || nnodes == 12 || nnodes == 15
							|| nnodes == 18) {
				setWallPref(0, VERTICAL, 1);
				setWallPref(2, VERTICAL, 2);
				setWallPref(3, VERTICAL, 3);
				setWallPref(4, VERTICAL, 3);
				setWallPref(5, VERTICAL, 4);
				setWallPref(6, VERTICAL, 4);
				setWallPref(7, VERTICAL, 5);
				setWallPref(8, VERTICAL, 5);
				if (nnodes > 9) {
					setWallPref(9, VERTICAL, 3);
					setWallPref(10, VERTICAL, 4);
					setWallPref(11, VERTICAL, 5);
				}
				if (nnodes > 12) {
					setWallPref(12, VERTICAL, 3);
					setWallPref(13, VERTICAL, 4);
					setWallPref(14, VERTICAL, 5);
				}
				if (nnodes > 15) {
					setWallPref(15, VERTICAL, 3);
					setWallPref(16, VERTICAL, 4);
					setWallPref(17, VERTICAL, 5);
				}
			}

			for (int i =0; i<nnodes; i++)
				System.out.println(mynodes[i]);
			System.out.print("PREF:");
			for (int i=0; i<2; i++)
				System.out.print(maxPref[i] + "-");
			System.out.println();
		}
	}

	public void config(int width_, int height_)
	{
		width = width_;
		height = height_;
	}

	public void excludeStub()
	{
		for (int i=0; i<nnodes; i++) {
			Node n = nodes[i];
			if (n.neighbors().length <= 1)
				mynodes[n.id].showing = false;
		}
	}

	public void includeStub()
	{
		for (int i=0; i<nnodes; i++) {
			Node n = nodes[i];
			if (n.neighbors().length <= 1)
				mynodes[n.id].showing = true;
		}
	}


	public void reset()
	{
		rand.setSeed(seed);
		// XX: how about mynodes?
	}

	public String parameter()
	{
		return "width=" + width + " height=" + height + " times=" + times
				+ " seed=" + seed + " random=" + random;
	}

	public void setRandom(boolean random_)
	{ random = random_; }

	public void doit()
	{
		for (int i=0; i<times; i++) {
			relax();
			if (random && (Math.random() < 0.03)) {
				Node n = nodes[(int)(Math.random() * nnodes)];
				if (!mynodes[n.id].fixed) {
					n.x += 100*Math.random() - 50;
					n.y += 100*Math.random() - 50;
				}
			}
		}
	}

	double MAX_STRING_LENGTH = 50; // corresponds to max force
	double MAX_STRING_FORCE = 10; // in terms of "distance"
	double STRING_FORCE_RATIO = MAX_STRING_FORCE / MAX_STRING_LENGTH;

	double MAX_NODE_DIST = 200; // corresponds to min force
	double MAX_NODE_FORCE = 10; // in terms of "distance"
	double NODE_FORCE_RATIO = MAX_NODE_FORCE / MAX_NODE_DIST;

	double MAX_WALL_DIST = 500; // corresponds to min force
	double MAX_WALL_FORCE = 5; // in terms of "distance"
	double WALL_FORCE_RATIO = MAX_WALL_FORCE / MAX_WALL_DIST;

	double MAX_PREF_DIST = 500; // corresponds to min force
	double MAX_PREF_FORCE = 50; // in terms of "distance"
	double PREF_FORCE_RATIO = MAX_PREF_FORCE / MAX_PREF_DIST;

	double STEP = 1.5;

	double DAMPING_RATIO = 2;

	public double getStep()
	{ return STEP; }

	public void setStep(double step_)
	{ STEP = step_; }

	public double getDampingRatio()
	{ return DAMPING_RATIO; }

	public void setDampingRatio(double ratio_)
	{ DAMPING_RATIO = ratio_; }

	synchronized void relax()
	{
		// force by the edge length constraint
		for (int i = 0 ; i < nedges ; i++) {
			Link e = edges[i];
			MyNode nn1 = mynodes[e.n1.id];
			MyNode nn2 = mynodes[e.n1.id];
			if (!nn1.showing || !nn2.showing) continue;
			double vx = e.n1.x - e.n2.x;
			double vy = e.n1.y - e.n2.y;
			double len = Math.sqrt(vx * vx + vy * vy);
			if (len > MAX_STRING_LENGTH) {
				vx *= MAX_STRING_LENGTH / len;
				vy *= MAX_STRING_LENGTH / len;
			}
			double dx = vx * STRING_FORCE_RATIO;
			double dy = vy * STRING_FORCE_RATIO;

			nn1.dx += -dx;
			nn1.dy += -dy;
			nn2.dx += dx;
			nn2.dy += dy;
		}

		// "push" by nodes and walls and pull by preferences
		for (int i = 0 ; i < nnodes ; i++) {
			Node n1 = nodes[i];
			MyNode nn = mynodes[i];
			if (!nn.showing) continue;
			double dx = 0;
			double dy = 0;

			for (int j = 0 ; j < nnodes ; j++) {
				if (i == j) continue;
				Node n2 = nodes[j];
				if (!mynodes[n2.id].showing) continue;
				double vx = n1.x - n2.x;
				double vy = n1.y - n2.y;
				double len = Math.sqrt(vx * vx + vy * vy);
				if (len > MAX_NODE_DIST) {
					vx *= MAX_NODE_DIST / len;
					vy *= MAX_NODE_DIST / len;
					len = MAX_NODE_DIST;
				}
				if (len == 0.0) {
					vx = Math.random() * MAX_NODE_DIST; // randomized
					vy = Math.random() * MAX_NODE_DIST; // randomized
				}
				else {
					vx *= (MAX_NODE_DIST - len) / len;
					vy *= (MAX_NODE_DIST - len) / len;
				}
				dx += vx * NODE_FORCE_RATIO;
				dy += vy * NODE_FORCE_RATIO;
			}

			// horizontal preference pulling
			int pref_ = nn.getHPref();
			if (pref_ > 0)
				dx += _pullx(n1.x, width * pref_ / maxPref[HORIZONTAL]);
			// vertical preference pulling
			pref_ = nn.getVPref();
			if (pref_ > 0)
				dy += _pully(n1.y, height * pref_ / maxPref[VERTICAL]);

			dx += _pushx(n1.x, 0);
			dx += _pushx(n1.x, width);
			dy += _pushy(n1.y, 0);
			dy += _pushy(n1.y, height);
			if (dx == 0) dx += Math.random();
			else if (Math.random() > .75)
				dx += Math.random()*2;
			if (dy == 0) dy += Math.random();
			else if (Math.random() > .75)
				dy += Math.random()*2;
	   		nn.dx += dx;
	   		nn.dy += dy;
		}

		//System.out.println(nodes[2] +": "+ mynodes[2]);
		for (int i = 0 ; i < nnodes ; i++) {
			Node n = nodes[i];
			MyNode nn = mynodes[i];
			if (!nn.showing) continue;
			if (!nn.fixed) {
				//n.x += Math.max(-5, Math.min(5, nn.dx));
				//n.y += Math.max(-5, Math.min(5, nn.dy));
				n.x += nn.dx * STEP;
				n.y += nn.dy * STEP;
			}
			if (n.x < 0)
				n.x = 0;
			else if (n.x > width)
				n.x = width;
			if (n.y < 0)
				n.y = 0;
			else if (n.y > height)
				n.y = height;
			nn.dx /= DAMPING_RATIO;
			nn.dy /= DAMPING_RATIO;
		}
	}

	double _pushx(double nx, double n2x)
	{
		double maxDist_ = MAX_WALL_DIST;
		double vx = nx - n2x;
		double len = Math.abs(vx);
		if (len > maxDist_) return 0.0;
		if (len == 0.0)
			vx = n2x==0.0? maxDist_: -maxDist_;
		else
			vx *= (maxDist_ - len) / len;
		return vx * WALL_FORCE_RATIO;
	}

	double _pushy(double ny, double n2y)
	{
		double maxDist_ = MAX_WALL_DIST;
		double vy = ny - n2y;
		double len = Math.abs(vy);
		if (len > maxDist_) return 0.0;
		if (len == 0.0)
			vy = n2y==0.0? maxDist_: -maxDist_;
		else
			vy *= (maxDist_ - len) / len;
		return vy * WALL_FORCE_RATIO;
	}

	double _pullx(double nx, double n2x)
	{
		double vx = nx - n2x;
		double len = Math.abs(vx);
		if (len > MAX_STRING_LENGTH)
			return vx > 0? -MAX_PREF_FORCE: MAX_PREF_FORCE;
		else
			return -vx * PREF_FORCE_RATIO;
	}

	double _pully(double ny, double n2y)
	{
		double vy = ny - n2y;
		double len = Math.abs(vy);
		if (len > MAX_STRING_LENGTH)
			return vy > 0? -MAX_PREF_FORCE: MAX_PREF_FORCE;
		else
			return -vy * PREF_FORCE_RATIO;
	}
}

