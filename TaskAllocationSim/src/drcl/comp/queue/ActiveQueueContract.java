// @(#)ActiveQueueContract.java   9/2002
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

package drcl.comp.queue;

import drcl.comp.*;
import drcl.data.*;

/**
 * Defines the commands used in the contract of queue component.
 */
public class ActiveQueueContract extends Contract
{
	//public static final String ENQUEUE	= "ENQUEUE";
	public static final String DEQUEUE	= "dequeue";
	public static final String PEEK		= "peek";
	public static final String IS_FULL	= "isfull";
	public static final String IS_EMPTY	= "isempty";
	public static final String GET_SIZE	= "getsize";
	public static final String GET_CAPACITY	= "getcapacity";
	//public static final String SET_CAPACITY	= "SETSIZE";

	public static final String OUTPUT_PORT_ID = "output";
	
	/** */
	public static void enqueue(Object data_, Port out_)
	{	out_.doSending(data_);	}
	
	/** */
	public static Object dequeue(Port out_)
	{	return out_.sendReceive(DEQUEUE);	}
	
	/** */
	public static Object peek(Port out_)
	{	return out_.sendReceive(PEEK);	}
	
	/** */
	public static boolean isFull(Port out_)
	{
		BooleanObj o_ = (BooleanObj)out_.sendReceive(IS_FULL);
		return o_.value;
	}
	
	/** */
	public static boolean isEmpty(Port out_)
	{
		BooleanObj o_ = (BooleanObj)out_.sendReceive(IS_EMPTY);
		return o_.value;
	}
	
	/** */
	public static int getSize(Port out_)
	{
		IntObj o_ = (IntObj)out_.sendReceive(GET_SIZE);
		return o_.value;
	}
	
	/** */
	public static int getCapacity(Port out_)
	{
		IntObj o_ = (IntObj)out_.sendReceive(GET_CAPACITY);
		return o_.value;
	}
	
	/** */
	public static void setCapacity(int size_, Port out_)
	{	out_.doSending(new IntObj(size_));	}
	
	/** */
	public static Object getEnqueueRequest(Object data_)
	{	return data_;	}
	
	/** */
	public static Object getDequeueRequest()
	{	return DEQUEUE;	}
	
	/** */
	public static Object getPeekRequest()
	{	return PEEK;	}
	
	/** */
	public static Object getIsFullRequest()
	{	return IS_FULL;	}
	
	/** */
	public static Object getIsEmptyRequest()
	{	return IS_EMPTY; }
	
	/** */
	public static Object getGetSizeRequest()
	{	return GET_SIZE; }
	
	/** */
	public static Object getGetCapacityRequest()
	{	return GET_CAPACITY; }
	
	/** */
	public static Object getSetCapacityRequest(int size_)
	{	return new IntObj(size_); }
	
	/** */
	public static Object pull(Port out_)
	{	return out_.sendReceive(null);	}
	
	/** */
	public static Object getPullRequest()
	{	return null; }
	
	public ActiveQueueContract()
	{ super(); }
	
	public ActiveQueueContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "ActiveQueue Contract"; }
	
	public Object getContractContent()
	{ return null; }
}
