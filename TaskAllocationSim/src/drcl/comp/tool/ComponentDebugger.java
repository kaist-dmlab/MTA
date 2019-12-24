// @(#)ComponentDebugger.java   9/2002
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

package drcl.comp.tool;

import java.util.*;

/**
 * Can specify a set of keyword combinations to filter out unrelated events.
 * Use addKeyword(String) to specify a keyword combination.
 * Match occurs when at least one keyword combination is matched.
 * All the keywords in a keyword combination must be matched for it to be
 * matched.  For example, consider the following keyword combinations:
 * 1. session 1, node 3
 * 2. requester 5, session 1
 * 3. session 2
 * 
 * It is equivalent to say that this debugger is interested in events of
 * "session 1 and node 3" or "requester 5 and session 1" or "node 3, session 1
 * and requester 5".  So event "session 1, node3, requester 5" is considered 
 * matched (to 1 and 2), event "session 2, requester 3" matches 3.
 * Event "session 1" does not match to any keyword combination.
 */
public class ComponentDebugger extends ComponentTester
{
	boolean isStepByStep = false;
	String[][] keywords = new String[3][];
								  
	public boolean isStepByStep() { return isStepByStep; }
	public void setStepByStep(boolean v_) { isStepByStep = v_; }
	
	public void addKeyword(String keyword_)
	{
		String[] keywords_ = form(keyword_);
		
		if (indexOf(keywords_) < 0) {
			// find empty spot
			for (int i=0; i<keywords.length; i++)
				if (keywords[i] == null) {
					keywords[i] = keywords_;
					return;
				}
			String[][] tmp = new String[keywords.length + 3][];
			System.arraycopy(keywords, 0, tmp, 0, keywords.length);
			tmp[keywords.length] = keywords_;
			keywords = tmp;
		}
	}
	
	String[] form(String keyword_)
	{
		String[] tmp_ = drcl.util.StringUtil.substrings(keyword_, ",");
		StringBuffer all_ = new StringBuffer();
		for (int i=0; i<tmp_.length; i++) {
			tmp_[i] = tmp_[i].toLowerCase().trim();
			all_.append(tmp_[i]); 
			if (i < tmp_.length-1) all_.append(", ");
		}
		String[] keywords_ = new String[tmp_.length + 1];
		keywords_[0] = all_.toString();
		System.arraycopy(tmp_, 0, keywords_, 1, tmp_.length);
		return keywords_;
	}
	
	public void removeKeyword(String keyword_)
	{
		int i = indexOf(keyword_);
		if (i >= 0) keywords[i] = null;
	}
	
	int indexOf(String[] keywords_)
	{
		for (int i=0; i<keywords.length; i++)
			if (keywords[i] != null && keywords[i].length == keywords_.length) {
				boolean matched = true;
				for (int j=1; j<keywords_.length; j++)
					if (keywords[i][0].indexOf(keywords_[j]) < 0) {
						matched = false; break;
					}
				if (matched) return i;
			}
		return -1;
	}
	
	public int indexOf(String keyword_)
	{ return indexOf(form(keyword_)); }
	
	public boolean contains(String keyword_)
	{ return indexOf(keyword_) >= 0; }
	
	public String[] getKeywords()
	{
		int n = 0;
		for (int i=0; i<keywords.length; i++) if (keywords[i] != null) n++;
		String[] tmp_ = new String[n];
		n = 0;
		for (int i=0; i<keywords.length; i++) if (keywords[i] != null) tmp_[n++] = keywords[i][0];
		return tmp_;
	}
	
	/**
	 * Result is back from the monitored component.
	 * If it's non-string, use getData() to retrieve the result.
	 */
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		if (data_ != null && !(data_ instanceof String) ) 
			super.process(data_, inPort_);
		else if (data_ != null) {
			int matchedIndex_ = -1;
			String target_ = ((String)data_).toLowerCase();
			//out.write("Debugger: receive " + target_ + "\n");
			
			for (int i=0; i<keywords.length && matchedIndex_ < 0; i++)
				if (keywords[i] != null) {
					//out.write("keywords " + i + ": " + keywords[i][0] + "\n");
					matchedIndex_ = i;
					for (int j=1; j<keywords[i].length; j++) {
						//out.write("keyword " + i + ", " + (j-1) + ": " + keywords[i][j]);
						if (target_.indexOf(keywords[i][j]) < 0) {
							//out.write(" -- not matched\n");
							matchedIndex_ = -1; break;
						}
						//out.write(" -- matched\n");
					}
				}
			
			if (matchedIndex_ >= 0) {
				post("Matched \"" + keywords[matchedIndex_][0] + "\": simulation stopped here.\n");
				try {
					synchronized (this) { wait(); }
				} catch (Exception e_) {
					drcl.Debug.error("Debugger", "Can't stop the simulator, simulation goes on!");
				}
			}
			else post("Not matched: simulation goes on!\n");
		}
	}
	
	public void go()
	{
		synchronized(this) {
			notify();
		}
	}
}
