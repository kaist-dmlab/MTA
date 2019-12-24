// @(#)ShellPython.java   8/2003
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

import java.io.*;
import org.python.util.*;
import org.python.core.*;

/**
 * The Python shell class which wraps around the JPython interpreter.
 */
public class ShellPython extends Shell
{
	static String name = "Python";
	
	public String getName() { return name; }
	
	RUVPythonInterpreter it;
	PythonWriter outChan;
	
	public ShellPython() throws Exception
	{ super(); }
	
	public ShellPython(String id_) throws Exception
	{ super(id_); }
	
	protected void init() throws ShellEvalException
	{
		it = new RUVPythonInterpreter(result);
		/* here to read init script 
		BufferedReader r_ = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("drcl.tcl")));
		StringBuffer sb_ = new StringBuffer();
		while (true) {
			String line_ = r_.readLine();
			if (line_ == null) break;
			sb_.append(line_);  sb_.append("\n");
			if (it.commandComplete(sb_.toString())) {
				it.eval(sb_.toString());
				sb_.setLength(0);
			}
		}
		r_.close();
		*/
			
		outChan = new PythonWriter();
		it.setOut(outChan);
		it.set("__current_shell__", this);
	}
	
	protected synchronized void setArguments(String[] args_)
			throws ShellEvalException
	{
		if (args_ == null || args_.length == 0) {
			it.runcode("import sys\n");
			it.runcode("sys.argv = []\n");
		}
		else {
			it.runcode("import sys\n");
			StringBuffer sb_ = new StringBuffer("sys.argv = [\"\"\"" + args_[0] + "\"\"\"");
			for (int i=1; i<args_.length; i++)
				sb_.append(", \"\"\"" + args_[i] + "\"\"\"");
			sb_.append("]\n");
			it.runcode(sb_.toString());
		}
	}

	public synchronized Object eval(String cmd_) throws ShellEvalException
	{
		if (cmd_.equals(COMMAND_EXIT) || cmd_.equals(COMMAND_QUIT))
			java.lang.System.exit(0); // XX: just intercept...
			
		//org.python.core.PyObject o_ = it.eval(cmd_);
		//return o_ == null? "": o_.toString();
		//java.lang.System.out.println("EVALUATE? " + cmd_);
		it.runcode(cmd_);
		return null;
	}

	// XXX: not implemented
	protected String _autocomplete(String cmd_, int pos_)
			throws ShellEvalException
	{
		return "";
	}
	
	public boolean isCommandComplete(String cmd_)
	{ 
		//java.lang.System.out.println("COMPLETE? " + cmd_);
        return !it.runsource(cmd_, "<stdin>");
	}
	
	// wrap "result" port inside
	class PythonWriter extends java.io.Writer
	{
		public void write(char cbuf[], int off, int len) throws IOException
		{ result.doSending(new String(cbuf, off, len)); }
		
		public void flush() throws IOException
		{}
											
		public void close() throws IOException
		{}
	}
}
