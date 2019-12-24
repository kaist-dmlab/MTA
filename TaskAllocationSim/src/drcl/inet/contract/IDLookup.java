// @(#)IDLookup.java   9/2002
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

package drcl.inet.contract;

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;

/**
The IdentityLookup contract.

This contract defines three services at the reactor:
<dl>
<dt> <code>DefaultIdentityRetrieval</code>
<dd> The initiator sends a <code>drcl.data.IntObj</code>
	of value 0 and the reactor returns the default identity 
	(<code>drcl.data.LongObj</code>) stored at the reactor.
<dt> <code>AllIdentitiesRetrieval</code>
<dd> The initiator sends a <code>drcl.data.IntObj</code>
	of value 1 and the reactor returns all the identities 
	(<code>long[]</code>) stored at the reactor.
<dt> <code>IdentitiesQuery</code>
<dd> The initiator sends identities in question (<code>long[]</code>)
	and the reactor sends back the corresponding answers in
	the <code>boolean[]</code> array of the same size.
	The value <code>true</code> indicates the corresponding identity
	exists in the reactor.
</dl>
This class also provides a set of static methods to facilitate conducting
the above services
({@link #getDefaultID(drcl.comp.Port) getDefaultID(Port)},
 {@link #getAllIDs(drcl.comp.Port) getAllIDs(Port)} and <code>query(..., Port)</code>)
from the specified port.  These methods are particularly useful in implementing
a protocol that needs to be aware of the identities of the node.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
*/
public class IDLookup extends Contract
{
	public IDLookup()
	{ super(); }
	
	public IDLookup(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "IdentityLookup Contract"; }

	public Object getContractContent()
	{ return null; }

	public static final int GET_DEFAULT = 0;
	public static final int GET_ALL = 1;
	
	/**
	 */
	public static long getDefaultID(Port out_)
	{
		LongObj o_ = (LongObj)out_.sendReceive(new IntObj(GET_DEFAULT));
		return o_.value;
	}
	
	/**
	 */
	public static long[] getAllIDs(Port out_)
	{ return (long[])out_.sendReceive(new IntObj(GET_ALL)); }
	
	/**
	 */
	public static boolean query(long id_, Port out_)
	{
		boolean[] o_ = (boolean[])out_.sendReceive(new long[]{id_});
		return o_[0];
	}
	
	/**
	 */
	public static boolean[] query(long[] ids_, Port out_)
	{ return (boolean[])out_.sendReceive(ids_); }
	
	/**
	 */
	public static Object createGetDefaultRequest()
	{	return new IntObj(GET_DEFAULT);	}
	
	/**
	 */
	public static Object createGetAllRequest()
	{	return new IntObj(GET_ALL);	}
	
	/**
	 */
	public static Object createQueryRequest(long id_)
	{	return new long[]{id_}; }
	
	/**
	 */
	public static Object createQueryRequest(long[] id_)
	{	return id_; }
}




