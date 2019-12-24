// @(#)CommandTask.java   1/2004
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

package drcl.ruv;

import drcl.comp.*;

public class CommandTask extends drcl.DrclObj implements Runnable
{
	static final String INACTIVE = "INACTIVE";
	static final String IN_EXECUTION = "IN_EXECUTION";
	static final String DONE = "DONE";
	String cmd;
	Shell shell;
	drcl.comp.ACARuntime runtime;
	double period;
	boolean enabled = true;
	String state = INACTIVE;
	
	public static CommandTask add(String cmd_, double later_, double period_, drcl.comp.ACARuntime runtime_,
								  Shell shell_)
	{
		CommandTask ct_ = new CommandTask(); //(CommandTask) drcl.RecycleManager.reproduce(CommandTask.class);
		ct_.cmd = cmd_;
		ct_.period = period_;
		ct_.runtime = runtime_;
		ct_.shell = shell_;
		runtime_.addRunnable(later_, ct_);
		return ct_;
	}
	
	public static CommandTask addAt(String cmd_, double time_, double period_, drcl.comp.ACARuntime runtime_,
								  Shell shell_)
	{
		CommandTask ct_ = new CommandTask(); //(CommandTask) drcl.RecycleManager.reproduce(CommandTask.class);
		ct_.cmd = cmd_;
		ct_.period = period_;
		ct_.runtime = runtime_;
		ct_.shell = shell_;
		runtime_.addRunnableAt(time_, ct_);
		return ct_;
	}
	
	public void run()
	{
		//java.lang.System.out.println("--------CommandTask------");
		state = IN_EXECUTION;
		if (!enabled) return;
		try {
			//java.lang.System.out.println("---------------- Execute: " + cmd);
			synchronized (shell) {
				Object result_ = shell.eval(cmd);
				//java.lang.System.out.println("--------------- ENd execution");
				if (result_ != null) shell.print(result_.toString());
			}
			
			if (period > 0.0 && runtime != null) {
				runtime.addRunnable(period, this);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			String msg_ = "Script Error| " + e_ + "| executing '" + this;
			shell.println(msg_);
		}
		state = DONE;
	}
	
	public void setEnabled(boolean v_) { enabled = v_; }
	public boolean isEnabled() {return enabled; }

	public void stop()
	{ enabled = false; }
	
	public String toString()
	{
		return state + ",script='" + cmd + "',period=" + period + ",shell=" + shell + ",enabled=" + enabled;
	}
	
	public String info()
	{ return toString() + "\n"; }
}
