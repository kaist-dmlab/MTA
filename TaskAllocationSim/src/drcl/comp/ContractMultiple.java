// @(#)ContractMultiple.java   9/2002
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

import drcl.comp.Contract;

/**
 * The class which wraps multiple contracts into one.
 */
public class ContractMultiple extends Contract
{
	public ContractMultiple (Contract c1_, Contract c2_)
	{ this(new Contract[]{c1_, c2_}); }
	
	public ContractMultiple (Contract c1_, Contract c2_, Contract c3_)
	{ this(new Contract[]{c1_, c2_, c3_}); }
	
	public ContractMultiple (Contract c1_, Contract c2_, Contract c3_, Contract c4_)
	{ this(new Contract[]{c1_, c2_, c3_, c4_}); }
	
	public ContractMultiple (Contract[] cc_)
	{
		add(cc_);
	}
	
	Contract[] cc;
	
	public void add(Contract c_)
	{
		if (cc == null) cc = new Contract[]{c_};
		else expand(new Contract[]{c_});
	}
	
	public void add(Contract[] cc_)
	{
		if (cc == null) cc = cc_;
		else {
			// XXX: expand multiple contracts?
			expand(cc_);
		}
	}
	
	void expand(Contract[] cc_)
	{
		if (cc == null) {
			cc = cc_; return;
		}
		
		Contract[] new_ = new Contract[cc.length + cc_.length];
		System.arraycopy(cc, 0, new_, 0, cc.length);
		System.arraycopy(cc_, 0, new_, cc.length, cc_.length);
		cc = new_;
	}
	
	/**
	 * Returns true if this contract matches <code>that_</code>.
	 */
	public boolean match(Contract that_)
	{
		if (cc == null || cc.length == 0) return true;
		
		for (int i=0; i<cc.length; i++)
			if (cc[i].match(that_)) return true;
		return false;
	}
	
	public Object getContractContent()
	{
		if (cc == null || cc.length == 0) return "";
		Object[] oo_ = new Object[cc.length];
		for (int i=0; i<cc.length; i++)
			oo_[i] = cc[i].getContractContent();
		return oo_;
	}

	public String getName()
	{
		if (cc == null || cc.length == 0) return "N/A";
		StringBuffer sb_ = new StringBuffer();
		sb_.append(cc[0].getName());
		for (int i=1; i<cc.length; i++)
			sb_.append(":" + cc[i].getName());
		return sb_.toString();
	}
}
