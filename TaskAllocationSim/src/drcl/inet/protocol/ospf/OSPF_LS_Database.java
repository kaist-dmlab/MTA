// @(#)OSPF_LS_Database.java   9/2002
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

package drcl.inet.protocol.ospf;

import java.util.*;

// xxx: support AS scope

/**
 *  The link state database within one area.
 * 
 *  <p>Ref: sec. 12.2.
 *  RFC 2328 does not clearly define the data structure of LS database
 *  We use Vector to implement the database.
 *  
 * @author Wei-peng Chen
 * @see OSPF_Area 
 */
public class OSPF_LS_Database
{
	protected static final int		SCOPE_AREA       = 2;
	protected static final int		SCOPE_AS         = 4;
  
	/** the list which stores the LSA in the database */
	protected Vector ls_list = null ;
	
	/**
	 * Constructor
	 * @param ospf_: backward OSPF reference
	 */
	public OSPF_LS_Database()
	{
		ls_list = new Vector();
	}
	
	public void reset()
	{
		ls_list.removeAllElements();
	}
	
	/** return the # of LSA in this database	 */
	protected int size() { return ls_list.size(); }
    
/*
void ospf6_lsa_flood_as (struct ospf6_lsa *lsa, struct ospf6 *ospf6)
{
  listnode n;
  struct area *area;

  assert (lsa && lsa->lsa_hdr && ospf6);
  o6log.dbex ("flooding %s in AS", print_lsahdr (lsa->lsa_hdr));

  // for each attached area 
  for (n = listhead (ospf6->area_list); n; nextnode (n))
    {
      area = (struct area *) getdata (n);
      ospf6_lsa_flood_area (lsa, area);
    }

  return;
}
*/
	/* flood ospf6_lsa within appropriate scope */
	/*protected void ospf_lsa_flood ( OSPF_LSA lsa, Object scope )
	{
		switch ( Util.translate_scope(lsa.header.lsh_type) ) {
			//case SCOPE_LINKLOCAL:
			//	OSPF_Interface oif = (OSPF_Interface) scope;
			//	ospf_lsa_flood_interface (lsa, oif);
			//	return;

			case OSPF_LS_Database.SCOPE_AREA:
				OSPF_Area area = (OSPF_Area) scope;
				ospf_lsa_flood_area (lsa, area);
		        return;

//	  struct ospf6 *ospf6;
//    case OSPF_LS_Database.SCOPE_AS:
//        ospf6 = (struct ospf6 *) lsa->scope;
//        assert (ospf6);
//        ospf6_lsa_flood_as (lsa, ospf6);
//        return;

			//case OSPF_LS_Database.SCOPE_RESERVED:
			default:
				//o6log.dbex ("unsupported scope, can't flood");
				break;
		}
		return;
	}
*/	

	OSPF_LSA ospf_lsdb_lookup (OSPF_LSA l)
	{
		return ospf_lsdb_lookup(l.header.lsh_type, l.header.lsh_id, l.header.lsh_advtr);
	}

	OSPF_LSA ospf_lsdb_lookup (OSPF_LSA_Header h)
	{
		return ospf_lsdb_lookup(h.lsh_type, h.lsh_id, h.lsh_advtr);
	}

	/**
	 * lookup the LSA in this database with type, id, and advrtr
	 * @param type: LSA type
	 * @param id: Link State id
	 * @param advrtr: the ip address of the originator
	 */
	OSPF_LSA ospf_lsdb_lookup ( int type, int id, int advrtr)
	{
		int no = size();
		for (int i = 0; i < no; i++) {
			OSPF_LSA lsa = (OSPF_LSA) ls_list.elementAt(i);
			if ( (lsa.header.lsh_type == type) && (lsa.header.lsh_id == id) 
			&& (lsa.header.lsh_advtr == advrtr) )
				return lsa;
		}
		return null;
	}

	void ospf_update_age ( int now_)
	{
		int no = size();
		for (int i = 0; i < no; i++)
			((OSPF_LSA) ls_list.elementAt(i)).header.ospf_age_current(now_);
	}

	// Returns true if the lsa (the exact reference) is in the database
	boolean ospf_lsdb_search (OSPF_LSA lsa)
	{ return ls_list.indexOf(lsa) >= 0; }

	/** Removes the LSA from the area database */
	protected void ospf_lsdb_remove ( OSPF_LSA lsa )
	{ ls_list.removeElement(lsa); }
 
	/** Adds the LSA to the area database */
	protected void ospf_lsdb_add ( OSPF_LSA lsa )
	{ ls_list.addElement(lsa); }
 
	/**
	 * Replaces the old LSA with the new one.
	 * If new_ is null, then it removes the old one.
	 * If old_ is null or not in database, then it adds the new one.
	 */
	protected void ospf_lsdb_replace ( OSPF_LSA new_, OSPF_LSA old_ )
	{
		int index_ = old_ == null? -1: ls_list.indexOf(old_);
		if (index_ < 0) {
			if (new_ != null) ls_list.addElement(new_);
		}
		else if (new_ == null)
			ls_list.removeElementAt(index_);
		else
			ls_list.setElementAt(new_, index_);
	}
}
