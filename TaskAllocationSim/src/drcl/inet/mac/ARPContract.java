// @(#)ARPContract.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.util.ObjectUtil;
import drcl.inet.contract.*;

/**
 * The contract between <code>LL</code> and <code>ARP</code>.
 *
 * @author Ye Ge
 */
public class ARPContract extends Contract
{
	public static final ARPContract INSTANCE = new ARPContract();

    /**
     *  Constructor
     */
	public ARPContract()
	{ super(); }
	
    
    /**
     * Constructor
     */
	public ARPContract(int role_)
	{ super(role_); }
	
    
    /**
     * Return "ARP Contract" as its name.
     */
	public String getName()
	{ return "ARP Contract"; }
	
    /**
     * Return null
     */
	public Object getContractContent()
	{ return null; }
	
    
    /**
     * A Message class defined for carrying information between ARP components
     * and LL components.
     */
	public static class Message extends drcl.comp.Message
	{
        
        static final int ARP_Resolve      = 0;    // to arp
        static final int ARP_ResolveReply = 1;    // to ll
        static final int ARP_Send_Request = 2;    // to ll
        static final int ARP_Input        = 3;    // to arp
        static final int ARP_Send_Reply   = 4;    // to ll
        static final int ARP_Send_Hold    = 5;    // to ll
        
        static final int ARP_Config       = 6;    // this is not used yet because ARP table configuration feature is not implemented yet.
        
        /**
         * An opration code which is used to decide the corresponding operation
         * after an ARP or LL component receives this message.
         */
		int opCode;
        
        
        long dst;
        Object pkt;
        
        /**
         * Constructor.
         */
        public Message ()
		{}

        /**
         * Constructor. 
         *
         * @param opCode_ operation code.
         * @param data_   the packet being exchanged between ARP and LL.
         */
		public Message (int opCode_, Object data_ )
		{
			opCode = opCode_;
            dst = 0;
            pkt = data_;
		}

        /**
         * Constructor. This is used while contructing an ARP request message.
         *
         * @param opCode_ operation code.
         * @param dst_    the destination IP address whose MAC address to be resolved.
         * @param data_   the packet being exchanged between ARP and LL.
         *
         */
		public Message (int opCode_, long dst_, Object data_ )
		{
			opCode = opCode_;
            dst = dst_;
            pkt = data_;
		}
		
		protected Object getPkt()
		{ return pkt; }
        
	    protected int getOpCode() 
        { return opCode; }
        
        protected long getDst() 
        { return dst; }
        
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			pkt = that_.pkt instanceof drcl.ObjectCloneable?
				((drcl.ObjectCloneable)that_.pkt).clone(): that_.pkt;
			opCode = that_.opCode;
            dst = that_.dst;
		}
		*/
	
        /**
         * Clones itself.
         */
		public Object clone()
		{
			// the contract is only between two components; don't clone pkt
			return new Message(opCode, dst, pkt);
		}

        
        /**
         * Gets an instance of ARPContract class, which is a static field
         * defined in ARPContract class.
         */
		public Contract getContract()
		{ return INSTANCE; }


        /**
         * Converts the message content to a String object.
         */
		public String toString(String separator_)
		{
			return "ARPContract.Message" + separator_ + "OpCode:" + opCode + separator_ + "dst:" + dst + separator_ + "," + drcl.util.StringUtil.toString(pkt);
		}
	}
}
