// @(#)TraceManager.java   9/2002
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

package drcl.util;

import java.util.*;
import drcl.data.*;

/**
 * Manages a set of traces and each trace can be individually turned on or off.
 * There is also a global switch.
 * All traces are off when the global switch is off.
 * The individual switch is only effective when the global switch is on.
 */
public class TraceManager extends drcl.DrclObj implements java.io.Serializable
{
	Hashtable htTraces = new Hashtable();
	boolean allDisabled = false;
	
	public TraceManager()
	{}
	
	public TraceManager(String[] traces_)
	{ setTraces(traces_); }
	
	public void setTraces(String[] traces_)
	{
		for (int i=0; i<traces_.length; i++)
			htTraces.put(traces_[i], new BooleanObj(false));
	}
	
	public void addTrace(String trace_)
	{
		htTraces.put(trace_, new BooleanObj(false));
	}
	
	public void removeTrace(String trace_)
	{
		if (trace_ != null) htTraces.remove(trace_);
	}
	
	public void setTraceEnabledAt(String which_, boolean enabled_)
	{
		BooleanObj t_ = (BooleanObj)htTraces.get(which_);
		if (t_ != null) t_.value = enabled_;
	}
	
	public boolean isTraceEnabledAt(String which_)
	{
		BooleanObj t_ = (BooleanObj)htTraces.get(which_);
		if (t_ != null) return t_.value;
		else {
			addTrace(which_);
			return true;
		}
	}
	
	public void setTraceEnabled(boolean enabled_) { allDisabled = !enabled_; }
	public boolean isTraceEnabled() { return !allDisabled; }
	
	public String[] getAllTraces()
	{
		String[] traces_ = new String[htTraces.size()];
		int i = 0;
		for (Enumeration e_ = htTraces.keys(); e_.hasMoreElements();) 
			traces_[i++] = (String)e_.nextElement();
		return traces_;
	}
	
	public boolean containsTrace(String trace_)
	{
		return htTraces.get(trace_) != null;
	}
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer("Trace " + (allDisabled? "disabled\n": "enabled\n"));
		int i = 0;
		for (Enumeration e_ = htTraces.keys(); e_.hasMoreElements();)  {
			Object key_ = e_.nextElement();
			BooleanObj t_ = (BooleanObj)htTraces.get(key_);
			sb_.append("Trace " + i + ": " + key_ + " " + (t_.value? "enabled": "disabled") + "\n");
			i++;
		}
		return sb_.toString();
	}

	public void duplicate(Object source_)
	{
		TraceManager that_ = (TraceManager)source_;
		allDisabled = that_.allDisabled;
		htTraces = (Hashtable)that_.htTraces.clone();
	}
}
