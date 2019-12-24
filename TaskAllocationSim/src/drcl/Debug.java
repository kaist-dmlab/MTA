// @(#)Debug.java   2/2004
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

package drcl;

import java.io.PrintStream;

/** This class provides static methods for other classes to output debug
 * and error messages. */
public class Debug
{
	static String BUG_REPORT_EMAIL = "j-sim@cs.uiuc.edu";
	
	/** Outputs an error message and exits the program. */
	public static void fatalError(String msg_)
	{
		msg_ += "\nProgram exits.";
		error(null, msg_, true);
	}
	
	/** Outputs a system-error message and exits the program. */
	public static void systemFatalError(String msg_)
	{
		msg_ += "\nPlease report the bug to " + BUG_REPORT_EMAIL
				+ ".\nProgram exits.";
		error(null, msg_, true);
	}
	
	/** Outputs an error message. */
	public static void error(Object msg_)
	{ error(null, msg_, false); }
	
	/** Outputs an error message. */
	public static void error(Object where_, Object msg_)
	{ error(where_, msg_, false); }
	
	/** Outputs an error message.
	@param exit_ true to exit the program after printing out the message. */
	public static void error(Object msg_, boolean exit_)
	{ error(null, msg_, exit_); }
	
	/** Outputs an error message.
	@param exit_ true to exit the program after printing out the message. */
	public static void error(Object where_, Object msg_, boolean exit_)
	{
		String s_ = (where_ == null? "": where_ + "| ") + msg_ + "\n";
		debug(s_);
		if (exit_) System.exit(1);
	}

	/** Outputs a debug message. */
	public static void debug(String msg_)
	{
		System.err.print(msg_);
	}
}
