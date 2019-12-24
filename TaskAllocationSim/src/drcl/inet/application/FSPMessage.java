// @(#)FSPMessage.java   9/2002
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

package drcl.inet.application;

import drcl.comp.*;
import drcl.net.*;
import drcl.util.ObjectUtil;

/** The file service protocol message. */
class FSPMessage extends drcl.DrclObj
{
	String fileName;
	int code;
	long startIndex, endIndex;
	long size;
	Object content; // could be null (request), byte[], (reply) or String (error)
	int msgSize;

	// errors
	public static final int EXCEPTION = 0;
	public static final int FILE_NOT_EXIST = 1;
	
	public FSPMessage()
	{ super(); }
	
	/**
	 * Constructor for the FSP request.
	 * 
	 * @param code_ the code sent by server in the previous reply.
	 * @param endIndex_ the end byte index, exclusive.
	 */
	public FSPMessage(String fileName_, int code_, long startIndex_,
										long endIndex_)
	{
		fileName = fileName_;
		code = code_;
		startIndex = startIndex_;
		endIndex = endIndex_;
	}
	
	/**
	 * Constructor for the FSP reply.
	 * 
	 * @param code_ the code for next request.
	 * @param endIndex_ the end byte index, exclusive.
	 */
	public FSPMessage(String fileName_, int code_, long startIndex_,
									   long endIndex_, long size_, byte[] content_)
	{
		fileName = fileName_;
		code = code_;
		startIndex = startIndex_;
		endIndex = endIndex_;
		size = size_;
		content = content_;
	}
	
	/** Constructor for the FSP error reply. */
	public FSPMessage(String fileName_, int errorCode_, String error_)
	{
		fileName = fileName_;
		code = errorCode_;
		content = error_;
	}
	
	public String getFileName()
	{ return fileName; }
	
	public long getStartIndex()
	{ return startIndex; }
	
	public long getEndIndex()
	{ return endIndex; }
	
	public int getCode()
	{ return code; }
	
	public int getErrorCode()
	{ return code; }
	
	public byte[] getFileContent()
	{ return (byte[])content; }

	public long getFileSize()
	{ return size; }
	
	public String getError()
	{ return (String)content; }

	public boolean isError()
	{ return content instanceof String; }

	public boolean isRequest()
	{ return content == null; }

	public boolean isReply()
	{ return content != null; }

	public String toString()
	{
		if (content == null) {
			// a request
			return "FSP_REQUEST:" + fileName + "(" + startIndex + "-" + endIndex + "),code=" + code;
		}
		else if (content instanceof String) {
			// an error
			return "FSP_ERROR:" + fileName + "," + code + ",'" + content + "'";
		}
		else {
			// a reply
			return "FSP_REPLY:" + fileName + "(" + size + "," + startIndex + "-" + endIndex
				+ "),content=" + drcl.util.StringUtil.toString(content) + ",code=" + code;
		}
	}
}




