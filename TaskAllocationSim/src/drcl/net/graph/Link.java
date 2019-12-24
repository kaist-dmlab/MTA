// @(#)Link.java   1/2004
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

/** The class defines data structure that describes an "edge" of a graph.
 * It can be directional or un-directional. 
 * A link may be "marked" for general purpose.
 */
public class Link 
{
	int id;
	Node n1, n2;
	int n1IfId, n2IfId;
	double len; // length
	double cost;
	boolean marked; // for general use
	boolean directional; // undirectional by default

	public Link()
	{}

	public Link(int id_, Node n1_, Node n2_)
	{
		this(id_, n1_, n1_.getNumLinks(), n2_, n2_.getNumLinks(),
						1.0, false);
	}

	public Link(int id_, Node n1_, Node n2_, boolean directional_)
	{
		this(id_, n1_, n1_.getNumLinks(), n2_, n2_.getNumLinks(),
						1.0, directional_);
	}

	public Link(int id_, Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_)
	{
		this(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						1.0, false);
	}

	public Link(int id_, Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_, boolean directional_)
	{
		this(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						1.0, directional_);
	}

	public Link(int id_,
					Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_,
					double cost_)
	{
		this(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						cost_, false);
	}

	public Link(int id_,
					Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_,
					double cost_, boolean directional_)
	{
		double vx = n1_.x - n2_.x;
		double vy = n1_.y - n2_.y;
		double len_ = Math.sqrt(vx * vx + vy * vy);
		len_ = (len_ == 0) ? .0001 : len_;
	
		_set(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						cost_, len_, directional_);
	}

	public Link(int id_, Node n1_, Node n2_, double cost_, double len_)
	{
		_set(id_, n1_, n1_.getNumLinks(), n2_, n2_.getNumLinks(),
						cost_, len_, false);
	}

	public Link(int id_, Node n1_, Node n2_, double cost_, double len_,
					boolean directional_)
	{
		_set(id_, n1_, n1_.getNumLinks(), n2_, n2_.getNumLinks(),
						cost_, len_, directional_);
	}

	public Link(int id_,
					Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_,
					double cost_, double len_)
	{
		_set(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						cost_, len_, false);
	}

	public Link(int id_,
					Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_,
					double cost_, double len_, boolean directional_)
	{
		_set(id_, n1_, n1InterfaceId_, n2_, n2InterfaceId_,
						cost_, len_, directional_);
	}

	void _set(int id_,
					Node n1_, int n1InterfaceId_,
					Node n2_, int n2InterfaceId_,
					double cost_, double len_, boolean directional_)
	{
		id = id_;
		n1 = n1_;
		n2 = n2_;
		n1IfId = n1InterfaceId_;
		n2IfId = n2InterfaceId_;
		cost   = cost_;
		len = len_;
		directional = directional_;
		n1.addLink(this);
		if (!directional_)
			n2.addLink(this);
	}

	public int getID()
	{ return id; }

	/** Returns the end nodes of this link. */
	public Node[] nodes()
	{ return new Node[]{n1, n2}; }

	/** Returns n's neighbor node thru this link. 
	 * n must be one of the end nodes. */
	public Node neighbor(Node n)
	{ return n == n1? n2: n1; }

	/** Returns the interface Id of this link on node n.
	 * n must be one of the end nodes. */
	public int getInterfaceId(Node n)
	{ return n == n1? n1IfId: n2IfId; }

	/** Returns the link cost. */
	public double getCost()
	{ return cost;  }

	/** Sets the link cost. */
	public void setCost(double cost_)
	{ cost = cost_; }

	/** Returns the length of the link. */
	public double getLength()
	{ return len; }

	/** Sets the length of the link. */
	public void setLength(double len_)
	{ len = len_; }

	/** Removes itself from both end nodes. */
	public void disconnect()
	{
		n1.removeLink(this); n2.removeLink(this);
		n1 = n2 = null;
	}

	/** Mark/unmark this link. */
	public void setMarked(boolean marked_)
	{ marked = marked_; }

	/** Returns true if this link is marked. */
	public boolean isMarked()
	{ return marked; }

	/** Returns true if it is a directional link. */
	public boolean isDirectional()
	{ return directional; }

	public String toString()
	{
		return id + "/" + n1.id + "." + n1IfId
				+ (directional? "-->":"---")
				+ n2.id + "." + n2IfId + "/" + cost
				+ (marked? "/marked": "");
	}

	// used in-package only
	int cost()
	{ return 1; }
}
