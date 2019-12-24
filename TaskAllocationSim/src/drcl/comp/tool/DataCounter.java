// @(#)DataCounter.java   9/2002
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

package drcl.comp.tool;

import java.util.*;
import drcl.comp.*;
import drcl.data.*;

/**
 * This component counts the number of data coming in at any of its ports.
 * The counting is performed on a per-port basis.
 */
public class DataCounter extends Extension
{
	Hashtable htcount = new Hashtable();
	
	public DataCounter() { super(); }
	
	public DataCounter(String id_) { super(id_); }
	
	protected synchronized void process(Object data_, Port inPort_) 
	{
		LongObj count_ = (LongObj)htcount.get(inPort_);
		if (count_ == null) {
			count_ = new LongObj(0);
			htcount.put(inPort_, count_);
		}
		count_.value ++;
	}
	
	public void reset()
	{
		if (htcount != null) htcount.clear();
		else htcount = new Hashtable();
		super.reset();
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof DataCounter)) return;
		super.duplicate(source_);
		DataCounter that_ = (DataCounter)source_;
	}
	
	public String info()
	{
		if (htcount == null || htcount.size() == 0) 
			return "Nothing being counted.\n";
		StringBuffer sb_ = new StringBuffer("#Ports being monitored: " + htcount.size() + "\n");
		Enumeration counts_ = htcount.elements();
		for (Enumeration e_ = htcount.keys(); e_.hasMoreElements(); ) {
			Object port_ = e_.nextElement();
			Object count_ = counts_.nextElement();
			sb_.append(port_ + ": " + count_ + "\n");
		}
		return sb_.toString();
	}
}
