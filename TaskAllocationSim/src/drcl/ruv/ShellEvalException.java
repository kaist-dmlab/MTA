// @(#)ShellEvalException.java   9/2003
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

package drcl.ruv;

public class ShellEvalException extends Exception
{
	Exception e;
	String msg; // msg does not co-exist with cmd and lineNum
	String cmd, stacktrace;
	int lineNum = -1; // line number

	private ShellEvalException()
	{ super(); }
	
	public ShellEvalException(String msg_)
	{ super(msg_); msg = msg_; }

	public ShellEvalException(Exception e_)
	{ e = e_; }
	
	public ShellEvalException(String cmd_, Exception e_)
	{ cmd = cmd_; e = e_; }

	public ShellEvalException(String cmd_, Exception e_, String stacktrace_)
	{ cmd = cmd_; e = e_; stacktrace = stacktrace_; }
	
	public ShellEvalException(String cmd_, Exception e_, int lineNum_)
	{ cmd = cmd_; e = e_; lineNum = lineNum_; }

	public ShellEvalException(String cmd_, Exception e_, String stacktrace_,
					int lineNum_)
	{ cmd = cmd_; e = e_; stacktrace = stacktrace_; lineNum = lineNum_; }

	public static ShellEvalException msg(String msg_, Exception e_)
	{
		ShellEvalException e = new ShellEvalException(msg_);
		e.e = e_;
		return e;
	}
	
	public String toString()
	{ return toString(null); }

	public String toString(StackTraceFilter f)
	{
		if (cmd == null) {
			if (msg != null)
				return msg + "\n" + _stackTrace(f);
			else
				return _stackTrace(f);
		}

		StringBuffer sb_ = new StringBuffer();
		if (lineNum >= 0)
			sb_.append("Line:" + lineNum + "  ");
		sb_.append("Cmd:" + cmd + "\n");

		if (msg != null) sb_.append(msg + "\n");

		return sb_ + _stackTrace(f);
	}

	String _stackTrace(StackTraceFilter f)
	{
		if (stacktrace != null) return stacktrace;
		if (f == null) return "";
		Exception e_ = e == null? this: e;
		return f.filter(e_);
	}

	public void printStackTrace()
	{ printStackTrace(java.lang.System.err); }

	public void printStackTrace(java.io.PrintStream ps)
	{
		if (stacktrace != null)
			ps.println(stacktrace);
		Exception e_ = e == null? this: e;
		e_.printStackTrace(ps);
	}

	public void printStackTrace(java.io.PrintWriter ps)
	{
		if (stacktrace != null)
			ps.println(stacktrace);
		Exception e_ = e == null? this: e;
		e_.printStackTrace(ps);
	}
}
