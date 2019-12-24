// @(#)OSPF_LSrequest.java   9/2002
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
 * Link State Request packet body.
 * <p>Ref: A.3.4.
 * Note: type = 3 
 * @author Wei-peng Chen
 */
public class OSPF_LSrequest extends drcl.DrclObj
{
	public final static int OSPF_LS_REQUEST_PKT_SIZE	= 12;
	
	public class LSrequest_body {
		public int lsreq_type;         /* LS type */
		public int lsreq_id;           /* Link State ID */
		public int lsreq_advrtr;       /* Advertising Router */
		
		public LSrequest_body (int type, int id, int adv )
		{
			lsreq_type		 = type;
			lsreq_id		 = id;
			lsreq_advrtr	 = adv;
		}
		
		public String toString()
		{ return "type:" + lsreq_type + ",id:" + lsreq_id + ",adv:" + lsreq_advrtr; }
	}
	
	public int ls_req_num;
	public Vector req_list = new Vector();

	public OSPF_LSrequest()
	{}

	public Object clone()
	{
		OSPF_LSrequest new_ = new OSPF_LSrequest();
		new_.duplicate(this);
		return new_;
	}

	public void duplicate(Object source_)
	{
		OSPF_LSrequest that_ = (OSPF_LSrequest)source_;
		ls_req_num = that_.ls_req_num;
		req_list.removeAllElements();
		req_list.setSize(ls_req_num);
		for (int i=0; i<ls_req_num; i++)
			req_list.setElementAt(that_.req_list.elementAt(i), i); // FIXME: dont clone
	}

	public OSPF_LSrequest ( int no )
	{
		/*OSPF_LSrequest h_ = (OSPF_LSrequest)drcl.RecycleManager.reproduce(OSPF_LSrequest.class);*/
		setreq(no);
	}
	
  	public OSPF_LSrequest ( int type, int id, int adv )
	{
		/* create a OPSF_LSupdate object through 
		   RecycleManager.reproduce() to improve the performance
		   of simulator. For more details, refer to the RecycleManager 
		   class. 
		 */
		/*OSPF_LSrequest h_ = (OSPF_LSrequest)drcl.RecycleManager.reproduce(OSPF_LSrequest.class);*/
		setreq(type, id, adv);
	}
	
	// create new requests for a recycle object
	private void setreq(int no )
	{
		if ( req_list == null) {
			req_list = new Vector(no);
		} else {
			req_list.removeAllElements();
			req_list = null;
			req_list = new Vector(no);
		}
		ls_req_num = 0;
	}

	// create a new request for a recycle object
	private void setreq(int type, int id, int adv )
	{
		if ( req_list == null) {
			req_list = new Vector(1);
		} else {
			req_list.removeAllElements();
		}
		LSrequest_body req = new LSrequest_body(type, id, adv);
		req_list.addElement(req);
		ls_req_num = 1;
	}
	
	public void addreq(int type, int id, int adv )
	{
		LSrequest_body req = new LSrequest_body(type, id, adv);
		req_list.addElement(req);
		ls_req_num ++;
	}

	public String toString()
	{
		return "#reqs:" + ls_req_num + (ls_req_num > 0 && ls_req_num < 5? ",reqs:" + req_list: "");
	}
}
