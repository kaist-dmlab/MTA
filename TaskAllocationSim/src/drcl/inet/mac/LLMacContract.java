// @(#)LLMacContract.java   7/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.mac;

import drcl.data.*;
import drcl.net.*;
import drcl.comp.*;
import drcl.inet.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
 * Defines soem convenient methods which are used by the <code>LL</code> 
 * components and the <code>MAC</code> components.
 *
 * @author Ye Ge
 */ 
public class LLMacContract extends Contract
{
	public static final LLMacContract INSTANCE = new LLMacContract();

    /**
     * Constructor.
     */
	public LLMacContract()
	{ super(); }
	
    /**
     * Constructor.
     */
	public LLMacContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "LL Mac Contract"; }
	
	public Object getContractContent()
	{ return null; }
    
    /**
     * Creates a LLPacket.
     *
     *@param dst_macaddr_ the destination mac address.
     *@param src_macaddr_ the source mac address.
     *@param body_        the InetPacket object which is the payload of the constructed LLPacket object.
     */
    public LLPacket createLLPacket(long dst_macaddr_, long src_macaddr_, InetPacket body_) {
        LLPacket pkt = new LLPacket(dst_macaddr_, src_macaddr_, body_.size, body_);
        return pkt;
    }    

    /**
     * Creates a LLPacket.
     *
     *@param dst_macaddr_ the destination mac address.
     *@param src_macaddr_ the source mac address.
     *@param body_        the ARPPacket object which is the payload of the constructed LLPacket object.
     */
    public LLPacket createLLPacket(long dst_macaddr_, long src_macaddr_, ARPPacket body_) {
        LLPacket pkt = new LLPacket(dst_macaddr_, src_macaddr_, body_.size, body_); 
        return pkt;
    }    
    
    
}
