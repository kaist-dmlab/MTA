// @(#)RUVPythonInterpreter.java   9/2002
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

package drcl.ruv;

import org.python.util.*;
import org.python.core.*;
import drcl.comp.Port;

// Based on org.python.util.InteractiveInterpreter

class RUVPythonInterpreter extends PythonInterpreter
{
	Port result; // for outputing result
	String cmd;
	PyObject cmdCode;

    public RUVPythonInterpreter(Port result_)
	{
        super();
		result = result_;
    }

    public RUVPythonInterpreter(Port result_, PyObject locals)
	{
        super(locals);
		result = result_;
    }

    /**
     * Compile and run some source in the interpreter.
     *
     * Arguments are as for compile_command().
     *
     * One several things can happen:
     *
     * 1) The input is incorrect; compile_command() raised an exception
     * (SyntaxError or OverflowError).  A syntax traceback will be printed
     * by calling the showsyntaxerror() method.
     *
     * 2) The input is incomplete, and more input is required;
     * compile_command() returned None.  Nothing happens.
     *
     * 3) The input is complete; compile_command() returned a code object.
     * The code is executed by calling self.runcode() (which also handles
     * run-time exceptions, except for SystemExit).
     *
     * The return value is 1 in case 2, 0 in the other cases (unless an
     * exception is raised).  The return value can be used to decide
     * whether to use sys.ps1 or sys.ps2 to prompt the next line.
     **/
    public boolean runsource(String source) {
        return runsource(source, "<input>", "single");
    }

    public boolean runsource(String source, String filename) {
        return runsource(source, filename, "single");
    }

    public boolean runsource(String source, String filename, String symbol) {
        PyObject code;
        try {
            code = org.python.modules.codeop.compile_command(
                source, filename, symbol);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SyntaxError)) {
                // Case 1
                showexception(exc);
                return false;
            } else if (Py.matchException(exc, Py.ValueError) ||
                       Py.matchException(exc, Py.OverflowError)) {
                // Should not print the stack trace, just the error.
                showexception(exc);
                return false;
            } else {
                throw exc;
            }
        }
        // Case 2
        if (code == Py.None) {
            return true;
		}
        // Case 3
        //runcode(code);
		cmd = source;
		cmdCode = code;
        return false;
    }

	/**
	 */
    public void runcode(String cmd_) {
        try {
			if (cmd != cmd_) {
				if (runsource(cmd_, "<stdin>")) // not complete
					return;
			}
            exec(cmdCode);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SystemExit)) throw exc;
            showexception(exc);
        }
    }

    public void showexception(PyException exc)
	{ if (result != null) result.doSending(exc); }

    public void write(String data)
	{
        //Py.stderr.write(data);
		if (result != null) result.doSending(data);
    }

    /** Pause the current code, sneak an exception raiser into
     * sys.trace_func, and then continue the code hoping that JPython will
     * get control to do the break;
     **/
    public void interrupt(ThreadState ts) throws InterruptedException {
        TraceFunction breaker = new BreakTraceFunction();
        TraceFunction oldTrace = ts.systemState.tracefunc;
        ts.systemState.tracefunc = breaker;
        if (ts.frame != null)
            ts.frame.tracefunc = breaker;
        ts.systemState.tracefunc = oldTrace;
        //ts.thread.join();
    }    
}

class BreakTraceFunction extends TraceFunction {
    private void doBreak() {
        throw new Error("Python interrupt");
        //Thread.currentThread().interrupt();
    }

    public TraceFunction traceCall(PyFrame frame) {
        doBreak();
        return null;
    }

    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        doBreak();
        return null;
    }

    public TraceFunction traceLine(PyFrame frame, int line) {
        doBreak();
        return null;
    }

    public TraceFunction traceException(PyFrame frame, PyException exc) {
        doBreak();
        return null;
    }
}
