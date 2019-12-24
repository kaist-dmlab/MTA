// @(#)Util.java   9/2002
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

// static utility methods
class Util
{
	/**
	 * Translate from the header type to the scope id to which this type corresponds
	 * @param type: LSA header type
	 */
	static int translate_scope(int type)
	{
		if( type == OSPF.OSPF_AS_EXTERNAL_LSA) 
			return OSPF.SCOPE_AS;
		else return OSPF.SCOPE_AREA;
	}

	/** check if lsa a and b are the same in the sense that is defined in sec. 13.2 */
	// returns true if same
	static boolean check_is_same (OSPF_LSA a, OSPF_LSA b, int now_)
	{
		if (a.header.lsh_type != b.header.lsh_type) return false;
		if (a.header.ospf_age_current(now_) != b.header.ospf_age_current(now_)
			&& (a.header.lsh_age == OSPF.LSA_MAXAGE || b.header.lsh_age == OSPF.LSA_MAXAGE))
			return false;

		switch (a.header.lsh_type) {
		case OSPF.OSPF_ROUTER_LSA:
			return ((Router_LSA)a).check_same_link((Router_LSA)b);
		}
		return false;
	}

	/**
	 * check if the two header is the same
	 * We compare age, header id, the orginator id, header type and seq. #
	 */
	// returns true if same
	static boolean check_is_same (OSPF_LSA_Header ha, OSPF_LSA_Header hb)
	{
		return ha.lsh_age == hb.lsh_age
			&& ha.lsh_id == hb.lsh_id
			&& ha.lsh_advtr == hb.lsh_advtr
			&& ha.lsh_type == hb.lsh_type
			&& ha.lsh_seqnum == hb.lsh_seqnum;
	}

	/**
	 * check which is more recent. if a is more recent, return -1;
	 * If the same, return 0; otherwise(b is more recent), return 1
	 * For the instances of the same LSA, determine which one is more recent by
	 * their seq no. age and checksum (ref. sec. 13.1)
	 */
	static int ospf_lsa_check_recent ( OSPF_LSA a, OSPF_LSA b, int now_)
	{
		// Tyan: should just check lsh_type and lsh_advtr,
		//		seqnum and age are checked below, how about lsh_id?
		//if (check_is_same (a.header, b.header) <=0 ) {
		//	// error : xxx
		//	return -2;
		//}
		if (a.header.lsh_type != b.header.lsh_type || a.header.lsh_advtr != b.header.lsh_advtr)
			return -2;

		long seqnuma = a.header.lsh_seqnum;
		long seqnumb = b.header.lsh_seqnum;

		/* compare by sequence number */
	    /* xxx, care about LS sequence number wrapping , see sec. 12.1.6*/
		if (seqnuma > seqnumb)
			return -1;
		else if (seqnuma < seqnumb)
			return 1;

		/* xxx, Checksum */

		/* MaxAge check */
		int agea = a.header.ospf_age_current( now_ );
		int ageb = b.header.ospf_age_current( now_ );
		if ( ( agea == OSPF.LSA_MAXAGE) && 
			 ( ageb != OSPF.LSA_MAXAGE) )
			return -1;
		else if ( (agea != OSPF.LSA_MAXAGE) && 
			 	  (ageb == OSPF.LSA_MAXAGE) )
			return 1;

		/* Age check */
		if (agea > ageb && agea - ageb >= OSPF.LSA_MAX_AGE_DIFF)
		    return 1;
		else if (agea < ageb && ageb - agea >= OSPF.LSA_MAX_AGE_DIFF)
		    return -1;
		// the same
		return 0;
	}

	static final int INITIAL_SEQUENCE_NUMBER = OSPF_LSA_Header.INITIAL_SEQUENCE_NUMBER;

	/** Prints LSA Header sequence number. */
	public static String printLSHSeqNum(int seqno_)
	{
		return seqno_ == INITIAL_SEQUENCE_NUMBER?
			"INIT": "INIT+" + (seqno_ - INITIAL_SEQUENCE_NUMBER);
	}
}
