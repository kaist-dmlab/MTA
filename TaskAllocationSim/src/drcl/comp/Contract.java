// @(#)Contract.java   9/2002
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

package drcl.comp;

/**
 * The base class for describing a contract.
 */
public abstract class Contract
{
	public static final int Role_INITIATOR = 0;
	public static final int Role_PEER = 1;
	public static final int Role_REACTOR = 2;
	
	int role = Role_PEER;
	
	public Contract()
	{}
	
	public Contract(int role_)
	{ setRole(role_); }
	
	public void setRole(int role_)
	{
		if (role_ < 0 || role_ > 2) return;
		role = role_;
	}
	
	public int getRole()
	{ return role; }
	
	/**
	 * Returns true if this contract matches <code>that_</code>.
	 */
	public boolean match(Contract that_)
	{
		if (this == that_) return true;
		if (this instanceof ContractAny || that_ instanceof ContractAny) return true;
		if (getClass().isAssignableFrom(that_.getClass()) && (that_.role+role) == 2)
			return true;
		else if (that_.role + role == 2) {
			// XXX: compare content
			// XXX: content format?
			return false;
		}
		else
			return false;
	}

	public abstract String getName();
	
	/** Returns the content of this contract (format?). */
	public abstract Object getContractContent();
}
