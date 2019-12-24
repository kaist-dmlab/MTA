// @(#)Stdout.java   9/2002
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

package drcl.comp.io;

import java.io.*;
import drcl.comp.*;
import drcl.data.*;

public class Stdout extends Extension
{
	boolean appendNewLineToObject = true;

	{ Port p_ = addPort("port"); }

	public Stdout()
	{ super(); }
	
	public Stdout(String id_)
	{ super(id_); }
	
	protected synchronized void process(Object data_, Port inPort_) 
	{
		if (!appendNewLineToObject || data_ instanceof String)
			System.out.print(data_);
		else
			System.out.println(data_);
	}
	
	public void reset()
	{ super.reset(); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		appendNewLineToObject = ((Stdout)source_).appendNewLineToObject;
	}
	
	public void setAppendNewLineToObjectEnabled(boolean enabled_)
	{ appendNewLineToObject = enabled_; }
	
	public boolean isAppendNewLineToObjectEnabled()
	{ return appendNewLineToObject;}
}
