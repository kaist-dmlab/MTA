// @(#)OSPF_LSack.java   9/2002
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

/**
 * Link State ack packet body.
 * <p>Ref: sec. A.3.6.
 * Note: type = 5
 * 
 * @author Wei-peng Chen
 */
public class OSPF_LSack extends drcl.DrclObj
{
	protected final static int OSPF_LS_ACK_PKT_SIZE	= OSPF_LSA_Header.OSPF_LSA_HEADER_SIZE;

	protected int lsack_num;
	protected Vector	LSA_hdr_list = new Vector();

	public OSPF_LSack()
	{}

	public Object clone()
	{
		OSPF_LSack new_ = new OSPF_LSack();
		new_.duplicate(this);
		return new_;
	}

	public void duplicate(Object source_)
	{
		OSPF_LSack that_ = (OSPF_LSack)source_;
		lsack_num = that_.lsack_num;
		LSA_hdr_list.removeAllElements();
		LSA_hdr_list.setSize(lsack_num);
		for (int i=0; i<lsack_num; i++)
			LSA_hdr_list.setElementAt(((OSPF_LSA_Header)that_.LSA_hdr_list.elementAt(i)).clone(), i); // clone header
	}

	protected OSPF_LSack ( int no )
	{
		/* create a OPSF_LSack object through 
		   RecycleManager.reproduce() to improve the performance
		   of simulator. For more details, refer to the RecycleManager 
		   class. 
		 */
		// OSPF_LSack h_ = (OSPF_LSack)drcl.RecycleManager.reproduce(OSPF_LSack.class);
		setlsahdr(no);
	}

  	protected OSPF_LSack ( OSPF_LSA_Header lsh )
	{
		/* create a OPSF_LSack object through 
		   RecycleManager.reproduce() to improve the performance
		   of simulator. For more details, refer to the RecycleManager 
		   class. 
		 */
		//OSPF_LSack h_ = (OSPF_LSack)drcl.RecycleManager.reproduce(OSPF_LSack.class);
		this();
		setlsahdr(lsh);
	}

	public String toString()
	{ return "#lshs:" + lsack_num + (lsack_num>0 && lsack_num < 5? ",lshs:" + LSA_hdr_list: ""); }
	
	/**
	 * create new LSAs for a recycle object
	 */
	private void setlsahdr( int no )
	{
		if ( LSA_hdr_list == null) {
			LSA_hdr_list = new Vector(no);
		} else {
			LSA_hdr_list.removeAllElements();
			LSA_hdr_list = null;
			LSA_hdr_list = new Vector(no);
		}
		lsack_num = 0;
	}
	
	private void setlsahdr( OSPF_LSA_Header lsh )
	{
		if ( LSA_hdr_list == null) {
			LSA_hdr_list = new Vector(1);
		} else {
			LSA_hdr_list.removeAllElements();
		}
		LSA_hdr_list.addElement(lsh);
		lsack_num = 1;
	}
	
	protected void addlsahdr( OSPF_LSA_Header lsh )
	{
		LSA_hdr_list.addElement(lsh);
		lsack_num++ ;
	}
}
