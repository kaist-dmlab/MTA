// @(#)Term.java   8/2003
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
 * The base class for all terminal classes.
 * 
 * Ports
 * <ul>
 *	<li> "port": sends out commands and receives replies; bound with {@link ShellContract}.
 * </ul>
 */
public abstract class Term extends Component implements RUVOutput
{
	Port port = addPort("cmd", false/* not removable*/);
	Port result = addPort("result", false/* not removable*/);
	Port output = addPort(new Port(Port.PortType_OUT), ".output", false/* not removable*/);
	{
		port.setExecutionBoundary(false);
		result.setExecutionBoundary(false);
	}
	
	public Term()
	{	super();	}

	public Term(String id_)
	{	super(id_);	}

	public void reset()
	{
		super.reset();
		cmdFinished = true;
		historyPos = history.size();
	}
	
	public String info()
	{
		return " Title = '" + title + "'\n" +
			   "Prompt = '" + prompt + "'\n" +
			   "cmdFinished = " + cmdFinished + "\n";
	}
	
	/**
	 */
	public synchronized void process(Object data_, drcl.comp.Port inPort_)
	{
		if (inPort_ == port) {
			if (data_ instanceof ShellContract.Message) {
				ShellContract.Message reply_ = (ShellContract.Message)data_;
				switch (reply_.getType()) {
				case ShellContract.COMMAND_FINISHED:
					String prompt_ = reply_.getPrompt();
					if (prompt_ != null) setPrompt(prompt_);
					cmdFinished();
					break;
				case ShellContract.SET_COMMAND:
					String cmd_ = reply_.getCommand();
					int pos_ = reply_.getPosition();
					setCommand(cmd_, pos_);
					break;
				case ShellContract.PRINT_RESULT:
					String result_ = reply_.getResult();
					write(result_);
					break;
				default:
					write(data_ + "\n");
				}
			}
		}
		else if (data_ != null) {
			write(data_.toString());
			/*
			if (data_ instanceof String)
				write(data_.toString());
			else
				write(data_ + "\n");
				*/
		}
	}

	public void duplicate(Object source_)
	{
		if (!(source_ instanceof Term)) return;
		super.duplicate(source_); // Let super class copy its fields.
		Term that_ = (Term)source_;
		title = that_.title;
		prompt = that_.prompt;
	}

	boolean stdoutEnabled = true;
	boolean outputEnabled = false;
	boolean termDisplayEnabled = true;

	/** Returns true if the terminal is enabled to output to
	the {@link RUVOutputManager#SYSTEM_OUT stdout} device. */
	public boolean isStdoutEnabled()
	{ return stdoutEnabled; }

	/** Enables/disables output to the {@link RUVOutputManager#SYSTEM_OUT stdout} device. */
	public void setStdoutEnabled(boolean enabled_)
	{ stdoutEnabled = enabled_; }

	/** Returns true if the terminal is enabled to output via the "output" port. */
	public boolean isOutputEnabled()
	{ return outputEnabled; }

	/** Enables/disables output via the "output" port. */
	public void setOutputEnabled(boolean enabled_)
	{ outputEnabled = enabled_; }

	/** Returns true if the terminal is enabled to display output. */
	public boolean isTerminalDisplayEnabled()
	{ return termDisplayEnabled; }

	/** Enables/disables terminal output display. */
	public void setTerminalDisplayEnabled(boolean enabled_)
	{ termDisplayEnabled = enabled_; }

	/** Puts the message on the terminal.
	 * Subclasses should override {@link #_write(String)} to implement
	 * their own display mechanism. */
	public final void write(String msg_)
	{
		if (stdoutEnabled) RUVOutputManager.SYSTEM_OUT.print(msg_);
		if (outputEnabled) output.doSending(msg_);
		if (termDisplayEnabled) _write(msg_);
	}
	
	// abstract methods
	
	/** Puts the message on the terminal.
	 * Subclasses should override this method to implement their own
	 * display mechanism. */
	protected abstract void _write(String msg_);

	/** Implements the {@link RUVOutput} interface. */
	public final void RUVOutput(String msg_)
	{ _write(msg_); }

	public abstract void show();
	public abstract void hide();
	public abstract boolean isFocused();
	
	protected abstract void setCommand(String newCmd_, int pos_);
	
	
	boolean cmdFinished = true;
	String title = "Term";
	String prompt = ">";
	
	public void setTitle(String title_)
	{ title = title_; }
	
	public String getTitle()
	{ return title; }
	
	protected void setPrompt(String prompt_)
	{ prompt = prompt_; }
	
	protected String getPrompt()
	{ return prompt; }
	
	/**
	 * Return false if the previous command is not finished yet.
	 * @param write_ if true, the command is outputted on the terminal
	 * 	with the {@link #write(String)} method. 
	 */
	protected final synchronized boolean evalCommand(String cmd_,
				   	boolean write_)
	{
		if (!cmdFinished && !isOutBoundCommand(cmd_)) return false;
		if (write_) write(cmd_ + "\n");
		port.doSending(ShellContract.createExecuteCommandsRequest(cmd_));
		cmdFinished = false;
		return true;
	}
	
	protected final void evalFile(String filename_)
	{
		port.doSending(ShellContract.createExecuteFileRequest(filename_, null));
	}
	
	final void cmdFinished()
	{
		cmdFinished = true;
		write(prompt);
	}
	
	protected synchronized final boolean isPrevousCommandFinished()
	{ return cmdFinished; }
	
	public static final boolean isOutBoundCommand(String cmd_)
	{
		return cmd_.equals(Shell.COMMAND_EXIT)
			   || cmd_.equals(Shell.COMMAND_QUIT);
	}

	protected synchronized final void autocomplete(String cmd_, int pos_)
	{
		if (!cmdFinished) return;
		port.doSending(ShellContract.createAutocompleteRequest(cmd_, pos_));
		cmdFinished = false;
	}
	
	protected synchronized final void interrupt()
	{
		port.doSending(ShellContract.createInterruptRequest());
	}
	
	Vector listener = null;
	
	void addExitListener(TermExitListener l_)
	{
		if (listener == null) listener = new Vector();
		if (listener.indexOf((Object)l_) < 0) 
			listener.addElement((Object)l_);
	}
	
	void removeExitListener(TermExitListener l_)
	{
		if (listener != null)
			listener.removeElement((Object)l_);
	}
	
	/**
	 * Exits the terminal.  Subclasses must call super if overriding this method.
	 */
	public void exit()
	{ _exit("exit"); }
	
	void _exit(String cmd_)
	{
		disconnectAllPeers();
		hide();
		if (listener != null) {
			for (int i=0; i<listener.size(); i++)
				((TermExitListener)listener.elementAt(i)).termExit(this, cmd_);
		}
	}
	
	/**
	 * Quits the application.
	 * Subclasses must call super if overriding this method.
	 */
	public void quit()
	{ _exit("quit"); }

	//
	private void ___HISTORY___() {}
	//
	
	Vector history = new Vector(100);
	int historyPos = 0; // current position in the command history
		
	protected synchronized int getHistorySize()
	{ return history.size(); }
		
	protected synchronized void addCmdToHistory(String cmd_)
	{
		// share command copy 
		int index_ = history.indexOf(cmd_);
		if (index_ >= 0) cmd_ = (String)history.elementAt(index_);
		history.addElement(cmd_);
		historyPos = history.size();
	}
		
	protected synchronized String getHistory(int increment_)
	{
		historyPos += increment_;
		if (historyPos < 0) historyPos = 0;
		if (historyPos > history.size()) historyPos = history.size();
		if (historyPos == history.size()) return "";
		return (String)history.elementAt(historyPos);
	}

	/**
	 * Returns the most recent command in history from the current position up that starts with <code>partial_</code>.
	 * @return <code>null</code> if no such command is found.
	 */
	protected synchronized String getHistoryUp(String partial_)
	{
		for (int i=historyPos-1; i>=0; i--) {
			String cmd_ = (String)history.elementAt(i);
			if (cmd_.startsWith(partial_)) {
				historyPos = i;
				return cmd_;
			}
		}
		//historyPos = -1;
		return null;
	}
		
	/**
	 * Returns the most early command in history from the current position down that starts with <code>partial_</code>.
	 * @return <code>null</code> if no such command is found.
	 */
	protected synchronized String getHistoryDown(String partial_)
	{
		for (int i=historyPos+1; i<history.size(); i++) {
			String cmd_ = (String)history.elementAt(i);
			if (cmd_.startsWith(partial_)) {
				historyPos = i;
				return cmd_;
			}
		}
		//historyPos = history.size();
		return null;
	}
		
	protected synchronized String historySubList(int start_)
	{ return historySubList(start_, history.size()); }
		
	protected synchronized String historySubList(int start_, int end_)
	{
		StringBuffer bs_ = new StringBuffer();
			
		if ((end_ < start_) || (start_*end_ <= 0))
			return null;
		for (int i=start_; i<end_; i++)
			bs_.append(history.elementAt(i) + "\n");
		return bs_.toString();
	}		
		
	protected synchronized void saveHistory(BufferedWriter bw_)
	{
		try	{
			for (int i=0; i<history.size(); i++) {
				String cmd_ = history.elementAt(i) + "\n";
				bw_.write(cmd_, 0, cmd_.length());
			}
			bw_.close();
		}
		catch(Exception e_) {
			e_.printStackTrace();
			drcl.Debug.error("Term " + getTitle(), "savelist() |" + e_);
		}
	}
	
	protected synchronized void diagnoze(String cmd_)
	{
		try {
			Shell shell_ = System.getBackupShell();
			if (!shell_.port.isConnectedWith(port)) {
				shell_.port.connectTo(port);
				shell_.result.connectTo(result);
			}
			Object result_ = shell_.eval(cmd_);
			if (result_ != null)
				process(result_, null);
		}
		catch (Exception e_) {
			e_.printStackTrace();
			write(e_.toString() + "\n");
			write("backup shell = " + System.getBackupShell());
		}
	}
}
