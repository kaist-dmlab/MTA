// @(#)RadixMap.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.data;

import java.util.*;

/**
 * A map keeps (<code>MapKey</code>, entry object) pairs in it.
 * Given a <code>MapKey</code>, an entry/entries can be retrieved by 
 * exact match, longest match, match, or wildcard match.
 * 
 * @see MapKey
 */
public class RadixMap extends drcl.data.Map
{
	private Radix_node head;
	
	public RadixMap()
	{ rn_inithead(0); }
		
	/** Removes all the entries. */
	public void reset()
	{
		super.reset();
		rn_inithead(0);
	}

	protected Object getLongestMatch(drcl.data.BitSet key_)
	{
		Radix_node t = head;
		Radix_mask mk;
		long  cp = key_.getSubset(1); // check if want to use whole key to compare
		
		// First, check if exact match
		Radix_node leave = rn_search(cp, t);
		
		int index_ = leave.rn_mklist.key_index;
		if(index_ >= 0 && key[index_].match(key_)) 
			return entry[index_];
				
		// If not, check if it match the left with mask
		mk = leave.rn_mklist; 
		while ( (mk = mk.rm_mklist) != null) {
			index_ = mk.key_index;
			if (index_ >=0 && key[index_].match(key_)) 
				return entry[index_];
		}

		if (leave != null) 
			t = leave;
		else t = head;

		/* If still not match, start searching up the tree */
		do {
			Radix_node child = t;
			t = t.rn_p;
			// if come from left child, it is uncessary to do mask 
			// because it will get the same result after masking and cause endless loop 
//			if (child == t.rn_l)
//				continue;	/*skip masking*/
			
			Radix_mask m = t.rn_mklist;
			if( m != null ) {
				do {
					long cp2 = cp & m.rmu_mask; // long cp2 = cp.mask(m);
					if ( cp2 == cp)
						continue; // will get the same result, so skip the following steps
					Radix_node x = rn_search(cp2, t);
					mk = x.rn_mklist;
					do {
						index_ = mk.key_index;
						if (index_ >= 0 && key[index_].match(key_))
						return entry[index_];
					} while ( (mk = mk.rm_mklist) != null );
				} while ((m = m.rm_mklist) != null);
			}
		} while (t != head);
		return null;
	};

	protected Object getExactMatch(MapKey key_)
	{
		long  cp = key_.value.getSubset(1);
		
		Radix_node leave = rn_search( cp, head);
		if (leave != null) {
			int index_ = leave.rn_mklist.key_index;
			if (index_ >=0 && key[index_].exactMatch(key_))
				return entry[index_];
		}
		return null;
	}

	/**
	 * Removes the longest match.
	 * Repeate the action done in getLongestMatch, but in the final step
	 * free the entry and delete the node from radix-tree
	 * @param key_ the key to match.
	 * @return the entry with the key matching the argument with largest number of bits.
	 */
	protected Object removeLongestMatch(drcl.data.BitSet key_)
	{
		Radix_node t = head;
		Radix_mask mk;
		long  cp = key_.getSubset(1); // check if want to use whole key to compare
		
		// First, check if exact match
		Radix_node leave = rn_search(cp, t);
		
		int index_ = leave.rn_mklist.key_index;
		if(index_ >= 0 && key[index_].match(key_)) {
			rn_delete(leave.rn_mklist);
			Object e_ = entry[index_];
			key[index_] = null; 
			entry[index_]= null;
			return e_;
		}	
		
		// If not, check if it match the left with mask
		mk = leave.rn_mklist; 
		while ( (mk = mk.rm_mklist) != null) {
			index_ = mk.key_index;
			if (index_ >= 0 && key[index_].match(key_)) {
				// Tyan: should delete mk
				//rn_delete(mk.rm_mklist);
				rn_delete(mk);
				Object e_ = entry[index_];
				key[index_] = null; 
				entry[index_]= null;
				return e_;
			}
		}

		if (leave != null) 
			t = leave;
		else t = head;

		/* If still not match, start searching up the tree */
		do {
			Radix_node child = t;
			t = t.rn_p;
			// if come from left child, it is uncessary to do mask 
			// because it will get the same result after masking and cause endless loop 
//			if (child == t.rn_l)
//				continue;	/*skip masking*/
			
			Radix_mask m = t.rn_mklist;
			if( m != null ) {
				do {
					long cp2 = cp & m.rmu_mask; // long cp2 = cp.mask(m);
					if ( cp2 == cp)
						continue; // will get the same result, so skip the following steps
					Radix_node x = rn_search(cp2, t);
					mk = x.rn_mklist;
					do {
						index_ = mk.key_index;
						if (index_ >= 0 && key[index_].match(key_)) {
							// Tyan: should delete mk
							//rn_delete(mk.rm_mklist);
							rn_delete(mk);
							Object e_ = entry[index_];
							key[index_] = null; 
							entry[index_]= null;
							return e_;
						}
					} while ( (mk = mk.rm_mklist) != null );
				} while ((m = m.rm_mklist) != null);
			}
		} while (t != head);
		return null;
	}

	protected Object removeExactMatch(MapKey key_)
	{
		Radix_node t = head;
		long  cp = key_.value.getSubset(1);
		
		Radix_node leave = rn_search( cp, t);

		int index_ = leave.rn_mklist.key_index;
		
		if (index_ >= 0 && key[index_].exactMatch(key_)){
			rn_delete(leave.rn_mklist);
			Object e_ = entry[index_];
			key[index_] = null; 
			entry[index_]= null;
			return e_;
		}
		return null;
	}

	/**
	 * Add the key-entry pair to the Map.  No check is done
	 * for duplicate keys.
	 * 
	 * @param key_	    the key.
	 * @param entry_	the entry.
	 */
	public void addEntry(MapKey key_, Object entry_)
	{
		try {
		super.addEntry(key_, entry_);
		// XX: ugly
		int i=0;
		for (; i<key.length; i++)
			if (key[i] == key_) break;
		
		// add this entry to radix-tree
		long  ip     = key_.getValue().getSubset(1); // check if want to use whole key to compare
		long  ip_mask = key_.getMask().getSubset(1);
		
		rn_insert(ip, ip_mask, head, i);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	// rn_search: 
	// given the key and the root of the tree, find the leave that might comply with the conditions
	private Radix_node rn_search(long key_, Radix_node h_)
	{
		Radix_node x;
		byte v;
		
		for (x = h_; x != null && x.rn_b >= 0;) {
			if ( ((1 << (31 - x.rn_b)) & key_)!=0) {
				x = x.rn_r;	// bit is set on
			} else {
				x = x.rn_l;	// bit is set off
			}
		}
		return (x);
	};

	private Radix_node rn_insert(long v_arg, long mask_, Radix_node h_, int index)
	{
		long cp = v_arg;
		long ip_mask = mask_;
		Radix_node top = h_;
		Radix_mask mk, new_mk;

		if( top == null) {  // error
	
		}
		/* First, trace to leave of the current tree */
		Radix_node end = rn_search(cp, top);
		int b,i;
		
		/* Second, find first bit at which cp and t->rn_key differ */
		if(end.rn_key == cp){
			// if ip the same, compare the mask value
			mk = end.rn_mklist;
			while ( mk != null ) {
				if (mk.rmu_mask == ip_mask)
					return end;
				mk = mk.rm_mklist;
			};
			// add the new mask to the end node
			new_mk = rn_new_radix_mask(end, ip_mask, index);
			rn_traceup_new_mask(end, new_mk);
			return end;
		}
		long diff = end.rn_key ^ cp;
		for( i=31, b=0; i >=0; i--)	{	// can do via compare each byte and then each bit
			if( (((diff>>i) & 0x01))!=0) {
				b = 31-i;	// bit count is from zero
				break;
			}
		}
	
		/*
		 * Third, find the node under which we should insert the node (node p)
		 * That is, the node one upper layer of the node on which difference happens (node x)
		 */
		Radix_node p;			/* node which the new node will be insert under */
		Radix_node t  = new Radix_node();	/* new branch node */
		Radix_node tt = new Radix_node();	/* new leave node */
		Radix_node x = top;		/* node that difference happens at */
		do {
			p = x;
//		if ( ( (0x80000000 >> x.rn_b) & cp) != 0)
		if ( ( (1 << (31 - x.rn_b)) & cp) != 0)
				x = x.rn_r;
			else x = x.rn_l;
		} while ((b > x.rn_b) && (x.rn_b>=0) ); /* x->rn_b < b && x->rn_b >= 0 */
		
		/* insert the new node */
		rn_newpair(cp, b, t, tt);
		// Here we first assume the new node is on the left side
		tt = t.rn_l;

		// link from node p down to the new node
//		if ( ( (0x80000000 >> p.rn_b) & cp) == 0)
		if ( ( (1 << (31- p.rn_b)) & cp) == 0)
			p.rn_l = t;
		else
			p.rn_r = t;
		// link from the difference node up to branch node and up to node p
		x.rn_p = t; t.rn_p = p; /* frees x, p as temp vars below */
//		if ( ( (0x80000000 >> t.rn_b) & cp) == 0) {
		if ( ( (1 << (31- t.rn_b)) & cp) == 0) {
			t.rn_r = x;
		} else {
			t.rn_r = tt; t.rn_l = x;
		}
		new_mk = rn_new_radix_mask(tt, ip_mask, index);
		rn_traceup_new_mask(tt, new_mk);		
		return (tt);
	}

	private void rn_newpair(long v, int b, Radix_node t, Radix_node tt)
	{		
		t.rn_b = b;
		t.rn_l = tt;
		tt.rn_b = -1;
		tt.rn_key = (long)v; tt.rn_p = t;
	}
	
	// create a root (starting at the offset bit) and two "end" nodes
	private int rn_inithead(int offset)
	{
		
		if (head != null)
			return (1);
		head = new Radix_node();
		Radix_node left  = new Radix_node();	// new left node
		Radix_node right = new Radix_node();	// new right node
		rn_newpair(0x0, offset, head, left);
 		head.rn_r = right;
		right.rn_p = head;
		right.rn_key = -1; // 0xFFFF...
		left = head.rn_l;
		left.rn_b = -1 - offset;
		// assign default mask to left most node and root node
		rn_new_radix_mask(head, 0, -1);
		rn_new_radix_mask(left, 0, -1);
		rn_new_radix_mask(right, -1, -1);
		// assgin header node
		return (1);
	}
	
	// add one mask into the mask_list of node tt
	private Radix_mask rn_new_radix_mask(Radix_node tt, long mask_, int index /*Radix_mask next*/) {
		Radix_mask mk_ = new Radix_mask(mask_, tt, index);
		Radix_mask m, p;
		if( mk_ == null) {
			//To-do: error handling
		}
		// If no masklist or the first element of the masklist is coaser than the new mask,
		// then add the new mask to the top of masklist
		if((tt.rn_mklist == null) || (mk_.rm_b < tt.rn_mklist.rm_b)) { 
			// put the new mask into the top of mask list
			mk_.rm_mklist = tt.rn_mklist;
			tt.rn_mklist  = mk_;
			return mk_;
		}
		for( m = tt.rn_mklist; m != null; m = m.rm_mklist) {
			if(m.rm_mklist != null) {
				if(mk_.rm_b < m.rm_mklist.rm_b) {
					// the new node is more specified than the existed node
					mk_.rm_mklist = m.rm_mklist;
					m.rm_mklist   = mk_;
					return mk_;
				}
			} else { // insert the new mask into the end of the mask list
				mk_.rm_mklist = m.rm_mklist;
				m.rm_mklist	  = mk_;
				return mk_;
			}
		}
		return null;
	}
	
	// opposition action of rn_new_radix_mask
	private void rn_delete_radix_mask(Radix_node tt, long mask_, int index /*Radix_mask next*/) {
		Radix_mask pmk = tt.rn_mklist;
		Radix_mask mk  = pmk.rm_mklist; 

		if ((pmk.rmu_mask == mask_) && (pmk.key_index == index)) {
			// delete mask is in the first element of mask-list
			tt.rn_mklist = mk;
		} else {
			for( ; mk != null; pmk = pmk.rm_mklist, mk = mk.rm_mklist) {
				if( (mk.rmu_mask == mask_) && (mk.key_index == index)) {
					// Got it, delete it
					pmk = mk.rm_mklist;			
				}
			}
		}		
	}

	// When a new mask is added into the leave node, it is also necessary to 
	// be added into the upper node until the mask is invalid in that upper node 
	private void rn_traceup_new_mask(Radix_node leave, Radix_mask new_mk){
		int cmp;
		Radix_node tt = leave;
		Radix_node t = tt.rn_p;

		cmp = -new_mk.rm_b - 1;
		// If tt is right child, it is unnecessary to add mask to its parent
		while( (t != null) && (t.rn_b > cmp) && (tt == t.rn_l) ) {
			rn_new_radix_mask(t, new_mk.rmu_mask, new_mk.key_index);
			t = t.rn_p;
			tt = tt.rn_p;
		}
	}

	// The opposite action of rn_traceup_new_mask()
	private void rn_traceup_delete_mask(Radix_mask delete_mk){
		int cmp = -delete_mk.rm_b - 1;
		Radix_node tt = delete_mk.rmu_leaf;
		Radix_node t = tt.rn_p;

		// If tt is right child, it is unnecessary to delete mask to its parent
		while( (t != null) && (t.rn_b > cmp) && (tt == t.rn_l) ) {
			rn_delete_radix_mask(t, delete_mk.rmu_mask, delete_mk.key_index);
			t = t.rn_p;
			tt = tt.rn_p;
		}
	}

	private void rn_delete(Radix_mask mk_delete) {
		
		// First, find the upper mask that point to mk_delete
		/* Radix_mask in the one way list
		   Therefore, we need to find this entry from the head */
		
		Radix_node tt	= mk_delete.rmu_leaf; /* deleted leave node */
		Radix_mask mk = tt.rn_mklist;
		
		if( mk == mk_delete) {
			// mk_delete is the first element in the mk_list
			tt.rn_mklist = mk_delete.rm_mklist;
			
			// Second, tear down the mask if it exists in the upper immediate node
			rn_traceup_delete_mask(mk_delete);
			
			// Third, check if the Radix_mask is null, 
			// if it is, delete this Radix_node, and its parent node
			// (because we add the pair of immediate node and leave node in the phase of add entry
			if( tt.rn_mklist == null) {
				Radix_node t	= tt.rn_p;		/* deleted branch node */
				Radix_node p	= t.rn_p;		/* parent node of the deleted branch node*/
				Radix_node an   = (tt == t.rn_l) ? t.rn_r : t.rn_l ;	/* another child of the branch node*/
				
				if( p.rn_l == t) {
					// left node
					p.rn_l = an;
					an.rn_p = p;
				} else {
					// right node	
					p.rn_r = an;
					an.rn_p = p;
				}
			}
			
		} else {
			// mk_delete is not the first element in the mk_list
			while( (mk.rm_mklist != mk_delete) && (mk.rm_mklist != null) ) {
				mk = mk.rm_mklist;
			}
			if( (mk.rm_mklist != null) && (mk.rm_mklist == mk_delete)) {
				mk.rm_mklist = mk_delete.rm_mklist;	
			} else {
				// error!!, it should not happen
				
			}
			rn_traceup_delete_mask(mk_delete);
		}
	}

	public String diag()
	{ return head.diag("", "Root:"); }

	/* Class Radix_mask: record the mask inside the Radix-tree node */
	protected class Radix_mask {
		int 	rm_b;			/* bit offset; -1-index(netmask) */
		long	rmu_mask;		/* the mask */
		int		key_index;		// index for key, -1 if "end" node
		Radix_mask rm_mklist;	/* more masks to try */
		Radix_node rmu_leaf;	/* for normal routes */
		
		Radix_mask() {}
		Radix_mask(long mask_, Radix_node t_ , int index_) {
			int i,b;
			
			rmu_mask = mask_;
			rm_mklist = null;
			// find the first bit that is zero
			for( i=0; i < 32; i++)	{
				if( (mask_ << i) == 0) {
					break;		// bit count is from zero
				}
			}
			rm_b = -1-i;
			rmu_leaf = t_;
			key_index = index_;
		}
/*		Radix_mask(long mask_) {
			Radix_mask(mask_, null, 0);
		}
		Radix_mask(long mask_, Radix_node t_) {
			Radix_mask(mask_, t_, 0);
		}

*/
		public String toString()
		{
			return "mask:" + rmu_mask + ", index:" + key_index + " --> " + rm_mklist;
		}
	}
	
	/*
	 * Class Radix_mask: record the mask inside the Radix-tree node
	 */
	protected class Radix_node extends drcl.DrclObj
	{
		Radix_mask rn_mklist;	/* list of masks contained in subtree */
		Radix_node rn_p;		/* parent */
		Radix_node rn_l;		/* progeny */
		Radix_node rn_r;		/* progeny */

		/* internal mode: the bit # to test
		leave mode:       bit offset; -1-index(netmask) */
		int		rn_b;		/* which bit to be tested */
		long	rn_key;		/* object of search */

//		private Radix_node	rn_dupedkey;
//		char	rn_bmask;		/* node: mask for bit test*/
//		int		rn_off;		/* where to start compare */
//		public Radix_node  get_dupedkey() { return rn_dupedkey; }
		public String toString()
		{ return "bit:" + rn_b + ", rnkey=" + rn_key + ", masklist=" + rn_mklist; }

		public String diag(String prefix_, String pos_)
		{
			return prefix_ + pos_ + this + "\n"
				+ (rn_l == null? prefix_ + "   L:null\n": rn_l.diag(prefix_ + "   ", "L:"))
				+ (rn_r == null? prefix_ + "   R:null\n": rn_r.diag(prefix_ + "   ", "R:"));
		}
	}
}


