// @(#)ShellContract.java   1/2004
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
import drcl.data.*;
import drcl.net.*;
import drcl.util.ObjectUtil;

/**
The Shell contract.
This contract defines the following services at the reactor:
<dl>
<dt> <code>ExecuteFile</code>
<dd> The initiator sends a request that consists of:
	<ol>
	<li> an integer of value 0 (the "execute file" command),
	<li> the name of the script file (String).
	<li> arguments (String[]), may be <code>null</code>.
	</ol>
	In response, the reactor opens the file and executes the scripts in the file.
<dt> <code>ExecuteCommands</code>
<dd> The initiator sends a request that consists of:
	<ol>
	<li> an integer of value 1 (the "execute commands" command) and
	<li> the commands (String).
	</ol>
	In response, the reactor executes the command and replies "CommandFinished".
<dt> <code>Autocomplete</code>
<dd> The initiator sends a request that consists of:
	<ol>
	<li> an integer of value 2 (the "autocomplete" command),
	<li> The command in construction (String) and
	<li> The position in the command for autocomplete (int).
	</ol>
	In response, the reactor sends back the result by "SetCommand" and "CommandFinished".
<dt> <code>Interrupt</code>
<dd> The initiator sends a request that consists of only
	a field of integer 3 (the "interrupt" command).  In response, the reactor interrupts
	the command that is currently being executed or the partial command that is 
	currently being constructed, and replies with "CommandFinished".
</dl>

The reactor may send back the following replies in response of a request:
<dl>
<dt> <code>CommandFinished</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 3 (the "command finished" command), and
	<li> a new prompt (String), may be null.
	</ol>
<dt> <code>SetCommand</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 4 (the "set command" command),
	<li> The command in construction (String) and
	<li> The current position in the command (int).
	</ol>
<dt> <code>PrintResult</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 5 (the "result" command), and
	<li> The result (String).
</dl>

@author Hung-ying Tyan
@version 1.0, 1/25/2001
 */
public class ShellContract extends Contract
{
	public static final ShellContract INSTANCE = new ShellContract();

	public String getName()
	{ return "RUV System Shell Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	/**
	 */
	public static Message createExecuteFileRequest(String fname_, String[] args_)
	{	return new Message(EXECUTE_FILE, fname_, args_);	}
	
	public static Message createExecuteCommandsRequest(String cmd_)
	{	return new Message(EXECUTE_COMMANDS, cmd_);	}
	
	public static Message createInterruptRequest()
	{	return new Message(INTERRUPT, null);	}
	
	public static Message createAutocompleteRequest(String cmd_, int pos_)
	{	return new Message(AUTOCOMPLETE, cmd_, pos_);	}

	public static Message createSetCommandReply(String cmd_, int pos_)
	{	return new Message(SET_COMMAND, cmd_, pos_);	}

	public static Message createCommandFinishedReply(String prompt_)
	{	return new Message(COMMAND_FINISHED, prompt_);	}
	
	public static Message createPrintResultReply(String result_)
	{	return new Message(PRINT_RESULT, result_);	}
	
	public static void setCommand(String cmd_, int pos_, Port p_, boolean lastSend_)
	{
		if (lastSend_)
			p_.doLastSending(new Message(SET_COMMAND, cmd_, pos_));
		else
			p_.doSending(new Message(SET_COMMAND, cmd_, pos_));
	}
	
	public static void commandFinished(String prompt_, Port p_, boolean lastSend_)
	{
		if (lastSend_)
			p_.doLastSending(new Message(COMMAND_FINISHED, prompt_));
		else
			p_.doSending(new Message(COMMAND_FINISHED, prompt_));
	}
	
	public static void printResult(String result_, Port p_, boolean lastSend_)
	{
		if (lastSend_)
			p_.doLastSending(new Message(PRINT_RESULT, result_));
		else
			p_.doSending(new Message(PRINT_RESULT, result_));
	}
	
	// name
	public static final String REQUEST = "ShellContract_REQ";
	public static final String REPLY = "ShellContract_REPLY";
	
	// type
	public static final int EXECUTE_FILE = 0; // or modify/replace
	public static final int EXECUTE_COMMANDS = 1;
	public static final int AUTOCOMPLETE = 2;
	public static final int INTERRUPT = 3;
	public static final int COMMAND_FINISHED = 4;
	public static final int SET_COMMAND = 5;
	public static final int PRINT_RESULT = 6;
	static final String[] TYPES = {"exec_file", "exec_cmd", "autocomplete",
									"interrupt", "cmd_finish", "set_cmd", "result"};
	
	public static class Message extends drcl.comp.Message
	{
		int type;
		String content;
		int pos;
		String[] args;
		
		public Message ()
		{}

		// for EXECUTE_FILE, EXECUTE_COMMANDS, INTERRUPT, COMMAND_FINISHED, PRINT_RESULT
		public Message (int type_, String ss_)
		{
			type = type_;
			content = ss_;
		}
			
		// for AUTOCOMPLETE AND SET_COMMAND
		public Message (int type_, String ss_, int pos_)
		{
			type = type_;
			content = ss_;
			pos = pos_;
		}

		// for EXECUTE_FILE
		public Message (int type_, String ss_, String[] args_)
		{
			type = type_;
			content = ss_;
			args = args_;
		}

		private Message (int type_, String ss_, int pos_, String[] args_)
		{
			type = type_;
			content = ss_;
			pos = pos_;
			args = args_;
		}

		public int getType()
		{ return type; }
	
		public String getContent()
		{ return content; }
	
		public String getCommand()
		{ return content; }
	
		public String getFileName()
		{ return content; }
	
		public String getResult()
		{ return content; }
	
		public String getPrompt()
		{ return content; }
	
		public int getPosition()
		{ return pos; }

		public String[] getArguments()
		{ return args; }
	
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			content = that_.content;
			pos = that_.pos;
			args = that_.args;
		}
		*/
	
		public Object clone()
		{ return new Message(type, content, pos, args); }

		public String getName()
		{
			if (type <= INTERRUPT) return REQUEST;
			else return REPLY;
		}

		public Contract getContract()
		{ return INSTANCE; }
	
		public String toString(String separator_)
		{
			if (type == INTERRUPT)
				return getName() + ":" + TYPES[type];
			else if (type == AUTOCOMPLETE || type == SET_COMMAND)
				return getName() + ":" + TYPES[type] + separator_ + "cmd:" + content
					+ separator_ + "pos:" + pos;
			else if (type == EXECUTE_FILE)
				return getName() + ":" + TYPES[type] + separator_ + "cmd:" + content
					+ separator_ + "args:" + drcl.util.StringUtil.toString(args);
			else
				return getName() + ":" + TYPES[type] + separator_ + "content:" + content;
		}
	}
}




