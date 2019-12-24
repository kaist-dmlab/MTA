// @(#)RUVOutputManager.java   11/2002
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

/**
 */
public class RUVOutputManager extends OutputStream
{
	public static final PrintStream SYSTEM_OUT = java.lang.System.out;
	public static final PrintStream SYSTEM_ERR = java.lang.System.err;

	static RUVOutput[] rout = null;
	static OutputStream[] sout= new OutputStream[]{SYSTEM_OUT, null};

	public static final RUVOutputManager onlyManager = new RUVOutputManager();

	public static void activate()
	{
		PrintStream ps_ = new PrintStream(onlyManager);
		java.lang.System.setOut(ps_);
		java.lang.System.setErr(ps_);
	}

	public static void deactivate()
	{
		java.lang.System.setOut(SYSTEM_OUT);
		java.lang.System.setErr(SYSTEM_ERR);
	}

	private RUVOutputManager()
	{ super(); }
	
    public void write(int b) throws IOException
	{
		out(new String(new byte[]{(byte)b}));
	}

    public void write(byte b[]) throws IOException
	{
		out(new String(b));
    }

    public void write(byte b[], int off, int len) throws IOException
	{
		if (b == null) {
			throw new NullPointerException();
		}
		else if ((off < 0) || (off > b.length) || (len < 0) ||
			((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		}
		else if (len == 0)
	    	return;
		out(new String(b, off, len));
    }
	
	void out(String msg_)
	{
		if (rout != null)
			for (int i=0; i<rout.length; i++)
				if (rout[i] != null)
					rout[i].RUVOutput(msg_);
		if (sout!= null) {
			byte[] bytes_ = msg_.getBytes();
			for (int i=0; i<sout.length; i++)
				if (sout[i] != null)
					try {
						sout[i].write(bytes_);
					}
					catch (Exception e_) {}
		}
	}
	
	public static void addOutput(RUVOutput o_)
	{
		if (rout == null)
			rout = new RUVOutput[2];
		
		for (int i=0; i<rout.length; i++)
			if (rout[i] == null) {
				rout[i] = o_;
				return;
			}
		
		RUVOutput[] tmp_ = new RUVOutput[rout.length + 2];
		java.lang.System.arraycopy(rout, 0, tmp_, 0, rout.length);
		rout = tmp_;
		rout[rout.length-2] = o_;
	}
	
	public static void addOutput(OutputStream o_)
	{
		if (sout == null)
			sout = new OutputStream[2];
		
		for (int i=0; i<sout.length; i++)
			if (sout[i] == null) {
				sout[i] = o_;
				return;
			}
		
		OutputStream[] tmp_ = new OutputStream[sout.length + 2];
		java.lang.System.arraycopy(sout, 0, tmp_, 0, sout.length);
		sout = tmp_;
		sout[sout.length-2] = o_;
	}

	public static void removeOutput(OutputStream o_)
	{
		if (sout== null) return;
		for (int i=0; i<sout.length; i++)
			if (sout[i] == o_) {
				sout[i] = null; return;
			}
	}

	public static void removeOutput(RUVOutput o_)
	{
		if (rout == null) return;
		for (int i=0; i<rout.length; i++)
			if (rout[i] == o_) {
				rout[i] = null; return;
			}
	}

	/** Prints out the message on the standard output. */
	public static void print(String s_)
	{ SYSTEM_OUT.print(s_); }

	/** Prints out the message on the standard output. */
	public static void println(String s_)
	{ SYSTEM_OUT.println(s_); }

	/** Prints out the message on the standard err. */
	public static void eprint(String s_)
	{ SYSTEM_ERR.print(s_); }

	/** Prints out the message on the standard err. */
	public static void eprintln(String s_)
	{ SYSTEM_ERR.println(s_); }
}
