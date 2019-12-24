// @(#)ShellTcl.java   1/2004
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

import java.io.*;
import tcl.lang.*;
import drcl.comp.*;

/**
 * The TCL shell class which wraps around the Jacl interpreter.
 */
public class ShellTcl extends Shell
{
	static String name = "Tcl";
	
	public String getName() { return name; }
	
	Interp it;
	//tcl.lang.DrclChannel outChan;
	
	public ShellTcl() throws Exception
	{ super(); }
	
	public ShellTcl(String id_) throws Exception
	{ super(id_); }
	
	protected void init() throws ShellEvalException
	{
		try {
			it = new Interp();	
			//outChan = new tcl.lang.DrclChannel(it, "file1", result);
		}
		catch (Exception e_) {
			throw new ShellEvalException(e_);
		}
		try {
			it.setVar("__shell", ReflectObject.newInstance(it,
						Shell.class, this), 0/*dunno how to set flags*/);
		}
		catch (Exception e_) {
			throw ShellEvalException.msg(this + ": cannot set __shell", e_);
		}

		evalResource("drcl.tcl");
	}
	
	protected synchronized void setArguments(String[] args_)
			throws ShellEvalException
	{
		try {
			if (args_ == null || args_.length == 0) {
				it.eval("set argc 0;");
			}
			else {
				it.eval("set argc " + args_.length);
				StringBuffer sb_ = new StringBuffer("set argv {");
				for (int i=0; i<args_.length; i++)
					sb_.append("{" + args_[i] + "} ");
				sb_.append("}");
				it.eval(sb_.toString());
			}
		}
		catch (Exception e_) {
			throw ShellEvalException.msg("failed setting arguments: "
							+ drcl.util.StringUtil.toString(args_), e_);
		}
	}

	public synchronized Object eval(String cmd_)
			throws ShellEvalException, InterruptedException
	{
		try {
			it.eval(cmd_);
		}
		catch (Exception e_) {
			if (e_ instanceof InterruptedException)
				throw (InterruptedException)e_;
			else
				throw new ShellEvalException(null, e_,
								it.getResult().toString());
		}
		// XX: not precise
		return cmd_.endsWith(";")? null: it.getResult();
	}
	
	public Object eval(BufferedReader r_)
			throws ShellEvalException, InterruptedException
	{
		StringBuffer sb_ = new StringBuffer();
		Object result_ = null;
		String line_ = null;
		int counter_ = 0;
		try {
			while (true) {
				line_ = r_.readLine();
				counter_++;
				if (line_ == null) break;
				line_.trim();
				if (line_.length() == 0 || line_.startsWith("#")) continue;
			
			
				if (line_.lastIndexOf('\\') == line_.length() -1) {
					// unfinished line
					sb_.append(line_.substring(0, line_.length() - 1));
					sb_.append(" ");
					continue;
				} else {
					sb_.append(line_);
					// NOTE: must add a trailing line,
					// otherwise the interpreter thinks of a complete command
					// with comment at the end as incomplete...
					synchronized (this) { 
						if (it.commandComplete(sb_ + "\n")) {
							String cmd_ = sb_.toString();
							it.eval(cmd_);
							// XX: not precise
							result_ = cmd_.endsWith(";")? 
									null: it.getResult();
							//result_ = eval(sb_.toString());
							sb_.setLength(0);
						}
						else {
							//java.lang.System.out.println(
							//	"command not complete: " + sb_);
							sb_.append("\n");
						}
					}
				}
			}
		}
		catch (Exception e_) {
			if (e_ instanceof InterruptedException)
				throw (InterruptedException)e_;
			else
				throw new ShellEvalException(sb_.toString(), e_, 
								it.getResult().toString(), counter_);
		}
		return result_;

	}
	
	public boolean isCommandComplete(String cmd_)
	{ 
		return Interp.commandComplete(cmd_); 
		//return cmd_.startsWith("#") || CountBracket(cmd_) == 0;
	}
	
	public Interp getInterp() 
	{ return it; }
	
	//public DrclChannel getDrclChannel() 
	//{ return outChan; }
	
	// find the partial path from the particial command and then execute
	// autocomplete commands
	protected String _autocomplete(String cmd_, int pos_)
			throws ShellEvalException
	{
		int i = cmd_.lastIndexOf(' ') + 1;
		if (i < 0 || i > pos_) i = 0;
		int j = i;
		for (; i<pos_; i++) {
			char c_ = cmd_.charAt(i);
			if (c_ == '\\') {
				i++; continue;
			}
			else if (c_ == '[' || c_ == ']' || c_ == '{' || c_ == '}'
					 || c_ == '\'' || c_ == '"')
				j = i+1;
		}
		String tmp_ = cmd_.substring(j, pos_);
		if (tmp_ == null || tmp_.length() == 0) tmp_ = "./";
		try {
			return eval("autocomplete -ap " + tmp_).toString();
		}
		catch (ShellEvalException e_) {
			throw e_;
		}
		catch (Exception e_) {
			throw new ShellEvalException(e_);
		}
	}
	
	int CountBracket(String str)
	{
		int no = 0;
			
		if (str.length()!=0) {
			for (int i=0; i<str.length(); i++)
				if (str.charAt(i)=='{') 
					no++;
				else if(str.charAt(i)=='}') 
					no--;
		}
		return no;		
	}

	// StaceTraceFilter
	public String filter(Exception e_)
	{
		String trace_ = super.filter(e_);
		int index_ = trace_.indexOf("at tcl.lang");
		if (index_ > 0)
			return trace_.substring(0, index_);
		else
			return trace_;
	}
}
