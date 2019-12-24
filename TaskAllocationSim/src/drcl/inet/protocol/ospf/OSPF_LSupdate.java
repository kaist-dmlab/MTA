// @(#)OSPF_LSupdate.java   9/2002
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
 * Link State Update Packet Body.
 * This request pkt can carry several LSAs one hop further from their origin.
 * <p>Ref: A.3.5.
 * Note : type = 4 
 * @author Wei-peng Chen
 */
public class OSPF_LSupdate extends drcl.DrclObj
{
	public int lsupdate_num;
	Vector	LSA_list = new Vector();
	
	public OSPF_LSupdate()
	{}

	public Object clone()
	{
		OSPF_LSupdate new_ = new OSPF_LSupdate();
		new_.duplicate(this);
		return new_;
	}

	public void duplicate(Object source_)
	{
		OSPF_LSupdate that_ = (OSPF_LSupdate)source_;
		lsupdate_num = that_.lsupdate_num;
		LSA_list.removeAllElements();
		LSA_list.setSize(lsupdate_num);
		for (int i=0; i<lsupdate_num; i++)
			LSA_list.setElementAt(((OSPF_LSA)that_.LSA_list.elementAt(i)).clone(), i); // clone LSA
	}

  	public OSPF_LSupdate ( int no )
	{
		/* create a OPSF_LSupdate object through 
		   RecycleManager.reproduce() to improve the performance
		   of simulator. For more details, refer to the RecycleManager 
		   class. 
		 */
		/*OSPF_LSupdate h_ = (OSPF_LSupdate)drcl.RecycleManager.reproduce(OSPF_LSupdate.class);*/
		setlsa(no);
	}

	public OSPF_LSupdate ( OSPF_LSA lsa )
	{
		/* create a OPSF_LSupdate object through 
		   RecycleManager.reproduce() to improve the performance
		   of simulator. For more details, refer to the RecycleManager 
		   class. 
		 */
		/*OSPF_LSupdate h_ = (OSPF_LSupdate)drcl.RecycleManager.reproduce(OSPF_LSupdate.class);*/
		setlsa(lsa);
	}
	
	// create new LSAs for a recycle object
	private void setlsa( int no )
	{
		if ( LSA_list == null) {
			LSA_list = new Vector(no);
		} else {
			LSA_list.removeAllElements();
			LSA_list = null;
			LSA_list = new Vector(no);
		}
		lsupdate_num = 0;
	}

	private void setlsa( OSPF_LSA lsa )
	{
		if ( LSA_list == null) {
			LSA_list = new Vector(1);
		} else {
			LSA_list.removeAllElements();
		}
		LSA_list.addElement(lsa);
		lsupdate_num = 1;
	}
	
	public void addlsa( OSPF_LSA lsa )
	{
		LSA_list.addElement(lsa);
		lsupdate_num++ ;
	}

	public String toString()
	{
		return "#lsas:" + lsupdate_num + (lsupdate_num>0 && lsupdate_num<5? ",lsas:" + LSA_list: "");
	}
}
