// @(#)Directory.java   9/2002
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

import drcl.comp.*;

// store results of a resovled path
// as intermediate data structure in implementing file commands
public class Directory
{
	public Directory (Component parent_, Object[] child_, String path_, boolean trailingSlash_)
	{
		parent = parent_;
		child = child_;
		ids = null;
		path = path_;
		trailingSlash = trailingSlash_;
	}
	
	public Directory (Component parent_, String[] ids_, String path_, boolean trailingSlash_)
	{
		parent = parent_;
		child = null;
		ids = ids_;
		path = path_;
		trailingSlash = trailingSlash_;
	}
	
	public Directory (Object object_, String path_, boolean trailingSlash_)
	{
		object = object_;
		path = path_;
		trailingSlash = trailingSlash_;
	}
	
	Component parent;
	Object[] child; // could be component/port/wrapped object
	String[] ids;  // component id/port group id/port id...
	// first element is Common.STOP_RESOLUTION
	Object object; // pure object reference, other fields must be null

	// in mkdir, cp and mv, trailing slash in a path makes sutle differences
	// between "under" and "onto" the component <-- component only
	boolean trailingSlash = false;
	
	String path; // the path from which this directory is derived
		// right now, it's only useful for "ls"
	
	public String toString()
	{
		return parent + ": " + drcl.util.StringUtil.toString(child)
			   + ", " + drcl.util.StringUtil.toString(ids)
			   + ", " + drcl.util.StringUtil.toString(object)
			   + (trailingSlash? "trailing /": "");
	}
}
