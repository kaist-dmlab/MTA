// @(#)CommandOption.java   9/2002
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

public class CommandOption
{
	CommandOption() {}
		
	CommandOption(String option_, boolean expand_)
	{
		for (int i=0; i<option_.length(); i++) {
			char c_ = option_.charAt(i);
			if (c_ == 'a') {
				hide = false;
				attach = true;
			}
			else if (c_ == 'p') includePort = true;
			else if (c_ == 'l') list = true;
			else if (c_ == 'q') quiet = true;
			else if (c_ == 'f') forced = flat = true;
			else if (c_ == 't') showPortType = true;
			else if (c_ == 'n') showNoConnPort = true;
			else if (c_ == 's') showShadow = sharedWire = true;
			else if (c_ == 'd') showAllDetails = true;
			else if (c_ == 'h') showHidden = true;
			//else if (c_ == 'i') showINPort = true;
			else if (c_ == 'u') unidirectional = true;
			else if (c_ == 'c') {
				showConn = createPort = true;
			}
			else if (c_ == 'i') showInfo = true;
			else if (c_ == 'e') error = true;
			else if (c_ == 'w') warning = true;
			else if (c_ == 'v') event = true;
			else if (c_ == 'r') recursive = true;
			else if (c_ == 'o') showOutside = true;
			else if (c_ == 'k') keepRuntime = true;
		}
		
		if (showAllDetails) {
			showHidden = showPortType = showNoConnPort = showShadow = true;
			showOutside = true;
		}
		if (!showConn && !showInfo) showInfo = true;
		expand = expand_;
		
		// for verify:
		// if is not turned on individually then all on
		if (!error && !warning && !event) {
			error = warning = event = true;
		}
	}
		
	// general
	boolean hide = true; // don't expand hidden objects
	boolean includePort = false;
	boolean list = false;
	boolean quiet = false;
	
	boolean recordID = false; // true if record ID in Directory if path cannot be resolved (turned on by mkdir)
	
	// expand all children of a path
	// set by individual command
	boolean expand;
		
	// mv/cp:
	boolean forced = false; // XX: not used anymore?
		
	// cat:
	boolean showAllDetails = false; // override the following options if set true
	boolean showHidden = false; // show hidden ports
	boolean showPortType = false;
	boolean showNoConnPort = false;
	boolean showShadow = false; // show child's shadow connection
	boolean showInfo = false; // show info()
	boolean showConn = false; // show connections
	boolean showOutside = false; // show out-of-scope ports
	
	// connect:
	boolean createPort = false;
	boolean unidirectional = false; 
	boolean sharedWire = false;
	
	// attach:
	boolean input;
	
	// verify:
	boolean error = false;
	boolean warning = false;
	boolean event = false;
	boolean recursive = false;

	// explore:
	boolean flat = false; // flat or hierarchical
	
	// internally used by Common.expand()
	boolean sort = false;
	
	// pipe:
	boolean attach = false;
	
	// mv:
	boolean keepRuntime = false;

	// getflag:
	// "list" for listing all enabled the programmed flags
	// "showAllDetails" for listing all the programmed flags
}
