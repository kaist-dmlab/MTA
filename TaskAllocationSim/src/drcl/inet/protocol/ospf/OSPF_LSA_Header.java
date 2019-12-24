// @(#)OSPF_LSA_Header.java   9/2002
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

import drcl.comp.*;

/**
 * OSPF LSA Header (fixed length = 20 byte). Ref: sec. 12.1 &#38; A.4.1
 * contains fields: ls_age(2), options(1), type(1), LS_ID(4), Adv_router(4),
 * LS_seqno(4), checksum(2), length(2) 
 * 
 * <p>Ref: RFC 2328 sec. 12
 * Remark: ls_age, options, type, checksum, length in RFC is defined unsigned type
 * Here we use signed type for simplicity. Besides, we replace struct in_addr with long
 * 
 * @author Wei-peng Chen
 * @see OSPF_LSA
 */
public class OSPF_LSA_Header
{
	protected final static int OSPF_LSA_HEADER_SIZE    = 20;		  /* ref. from A.3.1 */
	
	/** Age of the LSA. An instance of age MaxAge is always accepted as most recent;
	 *  otherwise, if the ages differ by more	than MaxAgeDiff, 
	 *  the instance having the smaller age	is accepted as most recent.(sec. 12.1.1) 
	 */
	protected int		lsh_age;
	
	/* The OSPF Options field is present in OSPF Hello packets, Database
         Description packets and all LSAs. */
	private int     	options;
	
	protected int		lsh_type; 

	/**
	 * In type 1, link state type always represents router ID.
	 * ref: sec 12.1.4 table 16
	 */
	protected int		lsh_id; 
	
	/** specify router ID of the LSA's originator */
	protected int		lsh_advtr; 

	protected static final int INITIAL_SEQUENCE_NUMBER = 0x80000001; /* signed 32-bit integer */
	protected static final int MAX_SEQUENCE_NUMBER     = 0x7fffffff; /* signed 32-bit integer */
		
	/** increase one when the router send one new LSA */
	protected int		lsh_seqnum = 0; /* used to detect old and duplicate LSA */
		
	/* short	checksum; */ /* here we ignore the checksum */
	private int		length;

	/** 
	 * birth: the time when genarating the LSA
	 * although 'birth' is not defined in the header, it is necessary to record
	 * in the lsa. Record the birth time of this lsa. => also can record the time
	 * that this lsa has stayed */
	protected int	birth;		/* tv_sec when LS age 0 */
	protected int  installed;	/* tv_sec when installed */

	public String toString()
	{
		return "origin:" + lsh_advtr + ",seq#:" + Util.printLSHSeqNum(lsh_seqnum) + ",age:" + lsh_age + ",birth: " + birth + ",installed: " + installed ;
	}

	public Object clone()
	{
		OSPF_LSA_Header h = new OSPF_LSA_Header(lsh_seqnum, lsh_type, lsh_id, lsh_advtr);
		h.lsh_age = lsh_age;
		h.birth = birth;
		h.length = length;
		h.options = options;
		return h;
	}
	
	/**  Constructor */
	OSPF_LSA_Header () { super(); }
	
	/**  Constructor */
	OSPF_LSA_Header (int _seq,  int _type, int _ls_id, int _adv_id)
	{
		super();
		lsh_age		= 0;
		lsh_seqnum	= _seq;
		lsh_type	= _type;
		lsh_id		= _ls_id;
		lsh_advtr	= _adv_id;
	}

	/**  Constructor */
	OSPF_LSA_Header (int _type, int _ls_id, int _adv_id)
	{
		super();
		lsh_age		= 0;
		lsh_seqnum	= INITIAL_SEQUENCE_NUMBER;
		lsh_type	= _type;
		lsh_id		= _ls_id;
		lsh_advtr	= _adv_id;
	}
		
	/** duplicate the OSPF_LSA_Header */
	protected void duplicate(Object source_)
	{
		OSPF_LSA_Header that_ = (OSPF_LSA_Header)source_;
		lsh_age		= that_.lsh_age;
		lsh_seqnum	= that_.lsh_seqnum;
		lsh_type	= that_.lsh_type;
		lsh_id		= that_.lsh_id;
		lsh_advtr	= that_.lsh_advtr;
		
		length		= that_.length;
		birth		= that_.birth;
		installed	= that_.installed;
		
		options		= that_.options;
	}
	
	/**  return the header size */
	protected int size() {
		return OSPF_LSA_HEADER_SIZE;
	}
			
	/**
	 * Check the correctness of the header type
	 */
	protected int check_lsh_type() {
		if (lsh_type < 1 || lsh_type > 6 )
			lsh_type = 0;
		return lsh_type;
	}
	
	/** 
	 * update age field of lsa_hdr, add InfTransDelay, when sending out the packet 
	 * over the interface
	 */
	protected void ospf_age_update_to_send ( int transdelay, int now_ )
	{
		lsh_age = now_ - birth + transdelay;
		if (lsh_age > OSPF.LSA_MAXAGE) lsh_age = OSPF.LSA_MAXAGE;
	}

	/** get current age of the LSA */
	protected int ospf_age_current( int now_)
	{
		/* calculate age */
		lsh_age = now_ - birth;
		/* if over MAXAGE, set to it */
		if (lsh_age > OSPF.LSA_MAXAGE)
			lsh_age = OSPF.LSA_MAXAGE;
		return lsh_age;
	}

	/** test LSAs identity */
	protected static int ospf_lsa_issame (OSPF_LSA_Header lsh1, OSPF_LSA_Header lsh2)
	{
		if ((lsh1 == null) || (lsh2 == null)) {
			// error
			return -1;
		}
		
		if (lsh1.lsh_advtr != lsh2.lsh_advtr)
			return 0;
		if (lsh1.lsh_id != lsh2.lsh_id)
		    return 0;
		if (lsh1.lsh_type != lsh2.lsh_type)
		    return 0;
		return 1;
	}

	protected int  get_options() { return options ;};
	protected void set_options(int op) { options = op; };
	
} // End of class OSPF_LSA_Header

/* OSPF LSA header */
