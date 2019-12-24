// @(#)OSPF_DBdesc.java   9/2002
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

import drcl.comp.*;

/**
 * OSPF Database Description packet body.
 * 
 * <p>ref: A.3.3.
 * Note: type = 2
 * @author Wei-peng Chen
 */
public class OSPF_DBdesc extends drcl.DrclObj
{
	/* the packet body size exccluding the size of LSA header */
	protected final static int OSPF_DBDESC_FIX_SIZE	= 8;
	protected int		ifmtu;
  	protected int		dd_msbit;
	protected int		dd_mbit;
	protected int		dd_ibit;
	protected int		dd_seqnum;     /* DD sequence number */
	/* The OSPF Options field is present in OSPF Hello packets, Database
           Description packets and all LSAs. */
	private int     options;

	/* this field does not exist in the specification, for implementation simplicity */
	protected int			LSA_no;

	//protected OSPF_LSA_Header[]	LSA_hdr;
	protected Vector	LSA_hdr_list;
	
	public Object clone()
	{
		OSPF_DBdesc new_ = new OSPF_DBdesc(dd_ibit, dd_mbit, dd_msbit, dd_seqnum);
		new_.duplicate(this);
		return new_;
	}

	public void duplicate(Object source_)
	{
		OSPF_DBdesc that_ = (OSPF_DBdesc)source_;
		LSA_no = that_.LSA_no;
		LSA_hdr_list.removeAllElements();
		LSA_hdr_list.setSize(LSA_no);
		for (int i=0; i<LSA_no; i++)
			LSA_hdr_list.setElementAt(((OSPF_LSA_Header)that_.LSA_hdr_list.elementAt(i)).clone(), i); // clone header
	}

	public String toString()
	{
		return (dd_ibit > 0? "I": "-") + (dd_mbit > 0? "M": "-") + (dd_msbit > 0? "MS": "-")
			+ ",seq#:" + dd_seqnum
			+ ",#lshs:" + LSA_no + (LSA_no>0 && LSA_no<5? ",lshs:" + LSA_hdr_list:"");
	}
	
	/**
	 * Constructor
	 */
	protected OSPF_DBdesc( int ibit, int mbit, int msbit, int seqnum)
	{
		LSA_hdr_list = new Vector();
		dd_msbit = msbit;
		dd_mbit  = mbit;
		dd_ibit  = ibit;
		dd_seqnum = seqnum;
	}

	public void reset()
	{
		LSA_hdr_list.removeAllElements();
	}
	
	protected static int isduplicate( OSPF_DBdesc db1, OSPF_DBdesc db2) 
	{
		if( db1.dd_ibit == db2.dd_ibit && db1.dd_mbit == db2.dd_mbit &&
			db1.dd_msbit == db2.dd_msbit && db1.dd_seqnum == db2.dd_seqnum )
			return 1;
		return 0;
	}
	
	protected void addlsaheader( OSPF_LSA_Header hdr) {
		LSA_hdr_list.addElement(hdr);
		/*OSPF_LSA_Header[] tmphdr_ = new OSPF_LSA_HEADER[ LSA_no+1 ]; 
		System.arraycopy(LSA_hdr, 0, tmphdr_, 0, LSA_no);
		LSA_hdr = tmpkey_;
		LSA_hdr[LSA_no] = hdr;*/
		LSA_no ++;
	}

	public int size()
	{
		int size_ = OSPF_DBDESC_FIX_SIZE;
		for (int i=0; i<LSA_no; i++)
			size_ += ((OSPF_LSA_Header)LSA_hdr_list.elementAt(i)).size();
		return size_;
	}

	void ospf_age_update_to_send(int transdelay_, int now_)
	{
		for (int i=0; i<LSA_no; i++)
			((OSPF_LSA_Header)LSA_hdr_list.elementAt(i)).ospf_age_update_to_send(transdelay_, now_);
	}
	
	protected void setMbit( int mbit_) {
		dd_mbit = mbit_;
	}
}


