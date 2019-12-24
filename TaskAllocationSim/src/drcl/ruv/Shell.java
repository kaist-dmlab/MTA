// @(#)Shell.java   8/2003
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
import java.util.*;
import drcl.comp.*;
import drcl.data.*;

/**
 * The base class for all shell classes.
 * 
 * Properties
 * <ul>
 *	<li> non-duplicable
 *  <li> non-disposable
 * </ul>
 * 
 * Ports (in addition to those defined in <code>drcl.comp.Component</code>):
 * <ul>
 *	<li> "file" port (input): receives the file name of a script (String) to execute.
 *  <li> "result" port (output): sends out results of executions.  At the end of execution,
 *		 it sends a null.
 *	<li> command port: all the other ports are considered as command ports;
 *		 it receives commands (String) to execute and replies complete/incomplete (boolean).
 * </ul>
 * The class defines a default "command" port to receives commands.
 */
public abstract class Shell extends Component implements StackTraceFilter
{
	protected static final String COMMAND_QUIT = "quit";
	protected static final String COMMAND_EXIT = "exit";

	static final String WARNING1 = "Shell is occupied by previous command.  Use ^C to reclaim the control.";
	Thread currentThread = null;
	
	protected Port port = addPort("cmd", false/* not removable*/);
	protected Port result = addPort("result", false/* not removable*/);
	{ result.setType(Port.PortType_OUT); }
	String cmd = null;
	
	public Shell() throws Exception
	{	super();	init(); }

	public Shell(String id_) throws Exception
	{	super(id_);	init(); }

	public void reset()
	{
		super.reset();
		cmd = null;
	}
	
	public String info()
	{
		return "Current thread: " + currentThread + "\n" +
			   "Partial command: " + cmd + "\n";
	}
	
	String prompt = ">";
	
	protected void setPrompt(String prompt_)
	{ prompt = prompt_; }
	
	protected String getPrompt()
	{ return prompt; }
	
	/**
	 */
	public void process(Object data_, Port inPort_)
	{
		if (!(data_ instanceof ShellContract.Message))
		{ error(data_, "process()", inPort_, "unrecongnized data"); return; }
		
		ShellContract.Message request_ = (ShellContract.Message)data_;

		// XX: codes are messy here, only works if commands are not overlap
		// or overlap only with out-of-bound commands or with file processing.
		// file processing cannot be overlapped with a command processing.
		switch (request_.getType()) {
		case ShellContract.EXECUTE_FILE:
			currentThread = Thread.currentThread();
			try {
				evalFile(request_.getFileName(), request_.getArguments());
			}
			catch (InterruptedException e_) {
			}
			catch (ShellEvalException e_) {
				//e_.printStackTrace();
				//_println(e_.toString(), true);
				_printException(e_);
			}
			finally {
				currentThread = null;
			}
			break;
		case ShellContract.EXECUTE_COMMANDS:
			String cmd_ = request_.getCommand();
			if (cmd_ == null) {// || cmd_.length() == 0) {
				ShellContract.commandFinished(null, inPort_, true);
				return;
			}
			
			boolean complete_ = true;
			if (cmd == null) cmd = cmd_;
			else cmd += "\n" + cmd_;
			try {
				if (!isCommandComplete(cmd)) {
					ShellContract.commandFinished(getIncompletePrompt(), inPort_, true);
					return;
				}
			}
			catch (Exception e_) {
				//_println(e_.toString(), true);
				_printException(new ShellEvalException(e_));
				ShellContract.commandFinished(null, inPort_, true);
				cmd = null;
				return;
			}
			
			String result_ = null;
			Object oresult_ = null;
			
			// make sure no two threads end up with currentThread == null
			boolean prevousCommandFinished_ = true;
			synchronized (this) {
				if (currentThread != null) prevousCommandFinished_ = false;
				else currentThread = Thread.currentThread();
			}
			
			if (!prevousCommandFinished_) {
				if (cmd_.equals(COMMAND_EXIT) || cmd_.equals(COMMAND_QUIT)) {
					currentThread.interrupt();
					//cmd_ = "exit";
					try {
						Thread.currentThread().sleep(100);
					}
					catch (Exception e_) {}
					cmd = cmd_;
				}
				else result_ = WARNING1; // should not be happening
			}
			if (result_ == null) { // not out of bound commands and no command is being executed
				try {
					oresult_ = eval(cmd);
					//if (result_ != null) result_ += "\n";
					if (oresult_ != null) result_ = oresult_.toString();
				}
				catch (InterruptedException e_) {
				}
				catch (ShellEvalException e_) {
					//result_ = "Command: '" + cmd + "'\n" + e_;
					_printException(e_);
				}
				finally {
					currentThread = null;
				}
			}
			if (result_ != null && result_.length() > 0)
				_println(result_, true);
			ShellContract.commandFinished(getPrompt(), inPort_, true);
			cmd = null;
			break;
		case ShellContract.AUTOCOMPLETE:
			cmd_ = request_.getCommand();
			int pos_ = request_.getPosition();
			result_ = null;
			try {
				result_ = _autocomplete(cmd_, pos_);
			}
			catch (ShellEvalException e_) {
				//e_.printStackTrace();
				//_println(e_.toString(), true);
				_printException(e_);
			}
			if (result_ != null) {
				cmd_ = cmd_.substring(0, pos_) + result_ + cmd_.substring(pos_);
				ShellContract.setCommand(cmd_, pos_ + result_.length(), inPort_, true);
			}
			ShellContract.commandFinished(null, inPort_, true);
			break;
		case ShellContract.INTERRUPT:
			if (currentThread != null) {
				currentThread.interrupt();
				try {
					Thread.currentThread().sleep(100);
				}
				catch (Exception e_) {}
			}
			else {
				cmd = null;
			}
			ShellContract.commandFinished(getPrompt(), inPort_, true);
			break;
		}
	}

	public void duplicate(Object source_)
	{
		if (!(source_ instanceof Shell)) return;
		super.duplicate(source_); // Let super class copy its fields.
		Shell that_ = (Shell)source_;
	}

	// return null if error occurs
	protected abstract void init() throws ShellEvalException;
	protected abstract Object eval(String cmd_)
			throws ShellEvalException, InterruptedException;
	protected abstract boolean isCommandComplete(String cmd_);
	protected abstract void setArguments(String[] args_)
			throws ShellEvalException;
	protected String getIncompletePrompt()
	{ return "... "; }

	public final Object evalFile(String script_, String[] args_)
			throws ShellEvalException, InterruptedException
	{
		try {
			setArguments(args_);
			BufferedReader r_ = new BufferedReader(new FileReader(
									new File(script_)));
			Object result_ = eval(r_);
			r_.close();
			return result_;
		}
		catch (ShellEvalException e_) {
			throw e_;
		}
		catch (Exception e_) {
			throw new ShellEvalException(e_);
		}
	}
	
	/**
	 * Script is from resource.
	 */
	public final void evalResource(String resource_) throws ShellEvalException
	{
		try {
			BufferedReader r_ = new BufferedReader(new InputStreamReader(
								getClass().getResourceAsStream(resource_)));
			eval(r_);
			r_.close();
		}
		catch (ShellEvalException e_) {
			throw e_;
		}
		catch (Exception e_) {
			throw new ShellEvalException(e_);
		}
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
				//line_.trim();
				//if (line_.length() == 0) continue;
				sb_.append(line_);
				if (isCommandComplete(sb_.toString())) {
					try {
						result_ = eval(sb_.toString());
					}
					catch (InterruptedException e_) {
						throw e_;
					}
					catch (ShellEvalException e_) {
						throw e_;
					}
					sb_.setLength(0);
				}
				else
					sb_.append("\n");
			}
			r_.close();
			return result_;
		}
		catch (Exception e_) {
			if (counter_ > 0)
				throw new ShellEvalException(line_.trim(), e_, counter_);
			else
				throw new ShellEvalException(line_.trim(), e_);
		}
	}
	
	Hashtable ht_waitUntilThreads = null;
					
	public void print(String msg_)
	{
		result.doSending(msg_);
	}
	
	public void println(String msg_)
	{
		result.doSending(msg_ + "\n");
	}
	
	void _println(String msg_, boolean lastSend_)
	{
		if (lastSend_)
			result.doLastSending(msg_ + "\n");
		else
			result.doSending(msg_ + "\n");
	}

	void _printException(ShellEvalException e_)
	{
		result.doSending(e_.toString(this) + "\n");
	}

	/** Returns true if the result is affirmative for this shell language. */
	public boolean isResultAffirmative(Object result_)
	{
		//java.lang.System.out.println("result: " + result_);
		return result_ != null && result_.toString().equals("1");
	}
	
	/**
	 * Subclasses should override this method to provide autocomplete function.
	 * Basically this method finds the partial path from the particial command 
	 * that the user has typed and then execute autocomplete commands in 
	 * Commands or a wrap-up version of which to return the common prefix
	 * string of all matched components or ports.
	 * @see Commands#autocomplete(String, Paths[], Shell)
	 */
	protected abstract String _autocomplete(String cmd_, int pos_)
			throws ShellEvalException;

	// StaceTraceFilter
	public String filter(Exception e_)
	{
		StringWriter sw_ = new StringWriter();
		e_.printStackTrace(new PrintWriter(sw_));
		String trace_ = sw_.toString();
		int index_ = trace_.indexOf("at sun.reflect");
		if (index_ > 0)
			return trace_.substring(0, index_);
		index_ = trace_.indexOf("at java.lang.reflect");
		if (index_ > 0)
			return trace_.substring(0, index_);
		else
			return trace_;
	}
}
