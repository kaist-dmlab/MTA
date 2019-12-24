// @(#)StringUtil.java   10/2002
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

package drcl.util;

import java.io.*;
import java.util.*;

/**
 *  Some commonly-used string utilities.
 */
public class StringUtil
{
	public static final String EMPTY_STRING = "";
	public static final String DEFAULT_DELIMITERS = " \t\n\r\f";
	public static final char[] HEX = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String[] substrings(String db_)
	{ return substrings(db_, DEFAULT_DELIMITERS); }
	
	public static String[] substrings(String s_, String delim_)
	{
		if (delim_ == null) delim_ = DEFAULT_DELIMITERS;
		if (s_ == null || s_.length() == 0 || delim_.length() == 0)
			return new String[]{s_};
		
		int len_ = s_.length();
		int currentPos_ = 0;
		LinkedList ll_ = new LinkedList();
		
		while (currentPos_ < len_) {
			if (delim_.length() > 0)
				currentPos_ = skipDelimiters(s_, currentPos_, delim_);
			if (currentPos_ >= len_) break;
			
			int start_ = currentPos_;
			while (++currentPos_ < len_
						&& delim_.indexOf(s_.charAt(currentPos_)) < 0);
			ll_.add(s_.substring(start_, currentPos_));
			if (currentPos_ >= len_) break;
		}
				
		//return (String[])ll_.toArray(new String[0]);
		String[] ss_ = new String[ll_.size()];
		for (int i=0; i<ss_.length; i++)
			ss_[i] = (String)ll_.removeFirst();
		return ss_;
	}
	
	public static String[] substrings(String db_, String delim_, String left_)
	{ return substrings(db_, delim_, left_, EMPTY_STRING); }
	
	private static int skipDelimiters(String s_, int currentPos_, String delim_)
	{
		int len_ = s_.length();
		while (currentPos_ < len_ && delim_.indexOf(s_.charAt(currentPos_)) >=0)
			currentPos_++;
		return currentPos_;
	}
	
	// s_.charAt(currentPos_) should be left_.charAt(which_),
	// the method returns the position where right_.charAt(which_) is found.
	private static int findMatch(String s_, int currentPos_, int which_,
					String left_, String right_)
	{
		char rightChar_ = right_.charAt(which_);
		int len_ = s_.length();
		currentPos_++; // skip the left char
		while (currentPos_ < len_) {
			char c_ = s_.charAt(currentPos_);
			if (c_ == rightChar_) return currentPos_;
			int index_ = left_.indexOf(c_);
			if (index_ >= 0)
				currentPos_ = findMatch(s_, currentPos_, index_, left_, right_);
			currentPos_++;
		}
		return currentPos_; // run out of s_!
	}
	
	/**
	 * Breaks the given string according to the delimiters.
	 * 
	 * @param s_ the string.
	 * @param delim_ the delimiters; use default set if null is given.
	 * @param left_ the "left" delimiters.
	 * @param right_ the "right" delimiters.
	 */
	public static String[] substrings(String s_, String delim_, String left_,
					String right_)
	{
		if (left_ == null || left_.length() == 0) left_ = EMPTY_STRING;
		if (right_ == null || right_.length() == 0) right_ = left_;
		if (delim_ == null) delim_ = DEFAULT_DELIMITERS;
		if (s_ == null || s_.length() == 0
			|| delim_.length() + left_.length() == 0
			|| left_.length() != right_.length()) return new String[]{s_};
		
		String delim2_ = delim_ + left_;
		int len_ = s_.length();
		int currentPos_ = 0;
		LinkedList ll_ = new LinkedList();
		
		while (currentPos_ < len_) {
			if (delim_.length() > 0)
				currentPos_ = skipDelimiters(s_, currentPos_, delim_);
			if (currentPos_ >= len_) break;
			
			char c_ = s_.charAt(currentPos_);
			int start_ = currentPos_;
			int leftIndex_ = left_.indexOf(c_);
			if (leftIndex_ < 0) { // start of a substring
				while (++currentPos_ < len_
						&& delim2_.indexOf(s_.charAt(currentPos_)) < 0);
				ll_.add(s_.substring(start_, currentPos_));
			}
			else { // start of a "quoted" string
				currentPos_ = findMatch(s_, currentPos_, leftIndex_, left_,
								right_);
				ll_.add(s_.substring(start_+1/*skip left char*/,
										currentPos_));
				currentPos_++; // skip right char
			}
		}
		
		//return (String[])ll_.toArray(new String[0]);
		String[] ss_ = new String[ll_.size()];
		for (int i=0; i<ss_.length; i++)
			ss_[i] = (String)ll_.removeFirst();
		return ss_;
	}
	
	/*
	public static String[] subStrings(String db)
	{
		StreamTokenizer tz = new StreamTokenizer(new StringBufferInputStream(db));
		tz.eolIsSignificant(false);
		tz.wordChars('_','_');

		try {
			Vector ncv = new Vector();

			boolean finished = false;
			while (!finished) {
				int token=tz.nextToken();
				switch (token) {
				case StreamTokenizer.TT_EOF:
					finished = true;
					break;
				case StreamTokenizer.TT_WORD:
					ncv.addElement(tz.sval);
					break;
				case StreamTokenizer.TT_NUMBER:
					ncv.addElement(tz.sval); //tz.nval +"");
					break;
				default:
					break;
				}
			}
			String[] s = new String[ncv.size()];
			ncv.copyInto(s);
			return s;
		} catch (IOException e) { // from tz.nextToken()
			return null;
		}
	}
	*/

	public static String getAcronym(String s)
	{
		String as = s.charAt(0) + ""; // assume first char is not ' '
		int i = 0;
		while (true)
		{
			s = s.substring(i+1);
			i = s.indexOf(' ');
			if (i<0) break;
			while (i < s.length() && s.charAt(i) == ' ') i++; // skip all ' '
			if (i < s.length()) as += s.charAt(i) + "";
			else break;
		}

		return as;
	}

	/**
	 * Format a double number.
	 * @param	f	# of digits in fraction
	 */
	public static  String toString(double d, int f)
	{
		return toString(d, -1, f);
	}

	/**
	 * Format a double number.
	 * @param	n	length of result
	 * @param	f	# of digits in fraction
	 */
	public static  String toString(double d, int nn, int ff)
	{
		String r_ = d + "";
		int ii_ = r_.indexOf(".");
		String head_ = null, tail_ = null;
		
		if (ii_ < 0) {
			head_ = r_;
			tail_ = "";
		}
		else {
			head_ = r_.substring(0, ii_);
			tail_ = r_.substring(ii_ + 1);
		} 
		
		int dot = ff > 0? 1:0;
		
		if (nn < 0) nn = head_.length() + dot + ff;
		
		if (head_.length() > nn) return head_;
		else if (head_.length() + ff + dot < nn) // prepends " " to head_
		{
			StringBuffer sb_ = new StringBuffer(nn - ff - dot - head_.length());
			for (int i=0; i<sb_.capacity(); i++)
				sb_.append(' ');
			head_ = sb_ + head_;
		}
		
		if (ff == 0) return head_;
		
		
		if (tail_.length() > ff) // truncate tail_
		{
			tail_ = tail_.substring(0, ff);
		}
		else // appends "0" to tail_
		{
			StringBuffer sb_ = new StringBuffer(ff - tail_.length());
			for (int i=0; i<sb_.capacity(); i++)
				sb_.append('0');
			tail_ += sb_.toString();
		}
		
		return head_ + "." + tail_;
		/*
		if (d == 0) return "0";

		double ten = Math.exp(Math.log(10)*ff);
		String sign = "";
		if (d < 0) { d = -d;  sign = "-"; }

		//System.out.println("double = " + d);
		//System.out.println("  sign = " + sign);
		//System.out.println("   ten = " + ten);

		if (d >= 1) 
		{
			long v = (long) (d*ten) / (long)ten;
			long f = (long) (d*ten) % (long)ten;

			return sign + v + "." + f;
		}
		else
		{
			double log = Math.log(d)/Math.log(10);
			int e = (int)log;
			if (log - e < 0) e--;
			log -= e;
			double fraction = Math.exp(Math.log(10)*log);

			//System.out.println("        log = " + log);
			//System.out.println("   fraction = " + fraction);
			//System.out.println("exponential = " + e);
			return sign + toString(fraction, n) + 
				(e==0? "": "e" + (e>0? "+":"") + e);
		}
		*/
	}
	
	static Hashtable htFinalPortionClassName = new Hashtable();
	/**
	 * Extract final portion of a full class name.
	 */
	public static String finalPortionClassName(Class class_)
	{
		if (class_ == null) return null;
		String name_ = (String) htFinalPortionClassName.get(class_);
		if (name_ == null) {
			name_ = class_.getName();
			int i = name_.lastIndexOf('.');
			if (i < 0) i = -1;
			name_ = name_.substring(i+1);
			htFinalPortionClassName.put(class_, name_);
		}
		return name_;
	}
	
	/**
	 * Extract substring after the last appearance of the separator string.
	 */
	public static String lastSubstring(String target_, String separator_)
	{
		if (target_ == null || separator_ == null || separator_.length() == 0)
			return target_;
		int i = target_.lastIndexOf(separator_);
		if (i < 0) i = -1;
		return target_.substring(i + separator_.length());
	}
	
	public static String toString(Hashtable ht_)
	{
		StringBuffer sb_ = new StringBuffer();
		Enumeration keys_ = ht_.keys(), elements_ = ht_.elements();
		while (keys_.hasMoreElements()) {
			sb_.append(toString(keys_.nextElement()) + ": "
					   + toString(elements_.nextElement()) + "\n");
		}
		return sb_.toString();
	}
	
	public static String toString(Object o_)
	{ return toString(o_, ",", 10); }
	
	public static String toString(Object o_, String separator_, int maxcount_)
	{
		if (o_ instanceof long[])
			return toString((long[])o_, separator_, maxcount_);
		if (o_ instanceof int[])
			return toString((int[])o_, separator_, maxcount_);
		if (o_ instanceof boolean[])
			return toString((boolean[])o_, separator_, maxcount_);
		if (o_ instanceof double[])
			return toString((double[])o_, separator_, maxcount_);
		if (o_ instanceof float[])
			return toString((float[])o_, separator_, maxcount_);
		if (o_ instanceof byte[])
			return toString((byte[])o_, separator_, maxcount_);
		if (o_ instanceof char[])
			return toString((char[])o_, separator_, maxcount_);
		if (o_ == null) return "<null>";
		if (!(o_ instanceof Object[])) return o_.toString();
		Object[] a_ = (Object[]) o_;
		if (a_.length == 0) return "Object[0]";
		StringBuffer sb_ = new StringBuffer("(" + toString(a_[0]));
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + toString(a_[i]));
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(double[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(double[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_double[]>";
		if (a_.length == 0) return "double[0]";
		StringBuffer sb_ = new StringBuffer("(" + a_[0]);
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + a_[i]);
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(float[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(float[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_float[]>";
		if (a_.length == 0) return "float[0]";
		StringBuffer sb_ = new StringBuffer("(" + a_[0]);
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + a_[i]);
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(long[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(long[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_long[]>";
		if (a_.length == 0) return "long[0]";
		StringBuffer sb_ = new StringBuffer("(" + a_[0]);
		for (int i=1; i<a_.length; i++)
			sb_.append(separator_ + a_[i]);
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(int[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(int[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_int[]>";
		if (a_.length == 0) return "int[0]";
		StringBuffer sb_ = new StringBuffer("(" + a_[0]);
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + a_[i]);
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(boolean[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(boolean[] a_, String separator_,
				int maxcount_)
	{
		if (a_ == null) return "<null_boolean[]>";
		if (a_.length == 0) return "boolean[0]";
		StringBuffer sb_ = new StringBuffer("(" + a_[0]);
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + a_[i]);
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(byte[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(byte[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_byte[]>";
		if (a_.length == 0) return "byte[0]";
		StringBuffer sb_ = new StringBuffer("(" + ((int)a_[0] & 0x0FF));
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + ((int)a_[i] & 0x0FF));
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(char[] a_)
	{ return toString(a_, ",", 10); }
	
	public static String toString(char[] a_, String separator_, int maxcount_)
	{
		if (a_ == null) return "<null_char[]>";
		if (a_.length == 0) return "char[0]";
		StringBuffer sb_ = new StringBuffer("(" + ((int)a_[0] & 0x0FFFF));
		maxcount_ = Math.min(maxcount_, a_.length);
		for (int i=1; i<maxcount_; i++)
			sb_.append(separator_ + ((int)a_[i] & 0x0FFFF));
		if (a_.length > maxcount_) sb_.append(separator_ + "...");
		sb_.append(")");
		return sb_.toString();
	}
	
	public static String toString(Class class_)
	{
		if (class_ == int[].class) return "int[]";
		else if (class_ == double[].class) return "double[]";
		else if (class_ == long[].class) return "long[]";
		else if (class_ == boolean[].class) return "boolean[]";
		else if (class_ == byte[].class) return "byte[]";
		else if (class_ == float[].class) return "float[]";
		else return class_.getName();
	}
	
	public static String removeEscape(String s_)
	{
		if (s_ == null) return null;
		
		StringBuffer sb_ = new StringBuffer();
		int i = 0;
		while (true) {
			int j = s_.indexOf('\\', i);
			if (j >= 0) {
				sb_.append(s_.substring(i, j));
				i = j+1;
				if (i < s_.length()-1 && s_.charAt(i) == '\\') i++;
			}
			else break;
		}
		if (sb_.length() == 0) return s_;
		return i >= 0? sb_ + s_.substring(i): sb_.toString();
	}
	
	public static String addEscape(String s_, String escapeSet_)
	{
		if (s_ == null) return null;
		
		StringBuffer sb_ = new StringBuffer();
		int j = 0;
		for (int i=0; i<s_.length(); i++) {
			char c_ = s_.charAt(i);
			if (escapeSet_.indexOf(c_) >= 0) {
				sb_.append(s_.substring(j, i) + '\\');
				j = i;
			}
		}
		
		if (sb_.length() == 0) return s_;
		sb_.append(s_.substring(j));
		return sb_.toString();
	}
	
	/**
	 * Returns true if the given string matches the given pattern.
	 * The matching operation permits the following special characters in
	 * the pattern: *?\[].
	 * 
	 * The codes are borrowed from the implementation of the 'string match'
	 * command in JACL.
	 */
	public static final boolean 
	match(
	    String str,			//String to compare pattern against.
	    String pat)			//Pattern which may contain special characters.
	{
	    char[] strArr = str.toCharArray();
	    char[] patArr = pat.toCharArray();
	    int    strLen = str.length();	// Cache the len of str.
	    int    patLen = pat.length();	// Cache the len of pat.
	    int    pIndex = 0;           	// Current index into patArr.
	    int    sIndex = 0;          	// Current index into patArr.
	    char   strch;                 	// Stores current char in string.
	    char   ch1;                 	// Stores char after '[' in pat.
	    char   ch2;                 	// Stores look ahead 2 char in pat.
	    boolean incrIndex = false;  	// If true it will incr both p/sIndex.

	    while (true) {
		
		if (incrIndex == true) {
		    pIndex++;
		    sIndex++;
		    incrIndex = false;
		}

		// See if we're at the end of both the pattern and the string.
		// If so, we succeeded.  If we're at the end of the pattern
		// but not at the end of the string, we failed.
		
		if (pIndex == patLen) {
		    if (sIndex == strLen) {
			return true;
		    } else {
			return false;
		    }
		}
		if ((sIndex == strLen) && (patArr[pIndex] != '*')) {
		    return false;
		}

		// Check for a "*" as the next pattern character.  It matches
		// any substring.  We handle this by calling ourselves
		// recursively for each postfix of string, until either we
		// match or we reach the end of the string.
		
		if (patArr[pIndex] == '*') {
		    pIndex++;
		    if (pIndex == patLen) {
			return true;
		    }
		    while (true) {
			if (match(str.substring(sIndex), 
				pat.substring(pIndex))) {
			    return true;
			}
			if (sIndex == strLen) {
			    return false;
			}
			sIndex++;
		    }
		}
		
		// Check for a "?" as the next pattern character.  It matches
		// any single character.
		  
		if (patArr[pIndex] == '?') {
		    incrIndex = true;
		    continue;
		}

		// Check for a "[" as the next pattern character.  It is followed
		// by a list of characters that are acceptable, or by a range
		// (two characters separated by "-").
		
		if (patArr[pIndex] == '[') {
		    pIndex++;
		    
		    while (true) {
			if ((pIndex == patLen) || (patArr[pIndex] == ']')) {
			    return false;
			}
			if (sIndex == strLen) {
			    return false;
			}
			ch1 = patArr[pIndex];
			strch = strArr[sIndex];
			if (((pIndex + 1) != patLen) && (patArr[pIndex + 1] == '-')) {
			    if ((pIndex += 2) == patLen) {
				return false;
			    }
			    ch2 = patArr[pIndex];
			    if (((ch1 <= strch) && (ch2 >= strch)) ||
				    ((ch1 >= strch) && (ch2 <= strch))) {
				break;
			    }
			} else if (ch1 == strch) {
			    break;
			}
			pIndex++;
		    }
		    
		    for (pIndex++; ((pIndex != patLen) && (patArr[pIndex] != ']'));
			 pIndex++) {
		    }
		    if (pIndex == patLen) {
			return false;
		    }
		    incrIndex = true;
		    continue;
		}
		
		// If the next pattern character is '/', just strip off the '/'
		// so we do exact matching on the character that follows.
		
		if (patArr[pIndex] == '\\') {
		    pIndex++;
		    if (pIndex == patLen) {
			return false;
		    }
		}

		// There's no special character.  Just make sure that the next
		// characters of each string match.
		
		if ((sIndex == strLen) || (patArr[pIndex] != strArr[sIndex])) {
		    return false;
		}
		incrIndex = true;
	    }
	}

	/**
	 * Returns true if the given string matches the given pattern.
	 * The matching operation permits the following special characters in
	 * the pattern: *?\[] and +-
	 * 
	 * Most of the codes are borrowed from the implementation of the
	 * 'string match' command * in JACL to handle *?\[].
	 * We add codes to handle +-.
	 */
	public static final boolean 
	match2(
	    String str,			//String to compare pattern against.
	    String pat)			//Pattern which may contain special characters.
	{
	    char[] strArr = str.toCharArray();
	    char[] patArr = pat.toCharArray();
		
		// check for unnecessary '\' in pat
		for (int i=patArr.length-1; i>=0; i--) {
			if (patArr[i] != '\\') continue;
			// count number of \ 
			int count_ = 1, j=i-1;
			for (; j>=0 && patArr[j] == '\\'; j--) count_++;
			char c = patArr[i];
			if (count_++%2 > 0
				&& i < patArr.length-1
				&& c != '*' && c != '?' && c != '[' && c != ']' && c != '+'
				&& c != '-')
					pat = pat.substring(0,j) + pat.substring(j+2);
			i = j;
		}
		
		// scan in 'pat' for +- range spec
		StringBuffer sb_ = new StringBuffer(pat);
		for (int i=patArr.length-1; i>=0; i--) { // must scan backwards...
			if (patArr[i] != '+' && patArr[i] != '-') continue;
			// count number of \ before this +-
			// continue if odd number of '\' is encountered
			int count_ = 0;
			for (int j=i-1; j>=0 && patArr[j] == '\\'; j--) count_++;
			if (count_++%2 > 0) continue;
			
			// check the second number
			int k=i+1;
			for (; k<patArr.length && patArr[k]>='0' && patArr[k]<='9'; k++);
			if (k == i+1) { // no 2nd number, not a range spec
				sb_.insert(i, '\\');
				continue;
			}
			
			// back track the first number in the range spec
			k=i-1;
			for (; k>=0 && patArr[k]>='0' && patArr[k]<='9'; k--);
			if (k == i-1)  // no first number, not a range spec
				sb_.insert(i, '\\');
			else { // insert +/- before the first number
				sb_.insert(k+1, patArr[i]);
				i = k+1;
			}
		}
		if (sb_.length() > patArr.length) {
			pat = sb_.toString();
		}
		return _match2(str, pat);
	}
	
	// internal use, called by match2()
	static final boolean 
	_match2(String str, String pat)
	{
		//System.out.println("str = '" + str + "'");
		//System.out.println("pat = '" + pat + "'");
		
	    char[] strArr = str.toCharArray();
	    char[] patArr = pat.toCharArray();
	    int    strLen = str.length();	// Cache the len of str.
	    int    patLen = pat.length();	// Cache the len of pat.
	    int    pIndex = 0;           	// Current index into patArr.
	    int    sIndex = 0;          	// Current index into patArr.
	    char   strch;                 	// Stores current char in string.
	    char   ch1;                 	// Stores char after '[' in pat.
	    char   ch2;                 	// Stores look ahead 2 char in pat.
	    boolean incrIndex = false;  	// If true it will incr both p/sIndex.

	    while (true) {
		
		if (incrIndex == true) {
		    pIndex++;
		    sIndex++;
		    incrIndex = false;
		}

		// See if we're at the end of both the pattern and the string.
		// If so, we succeeded.  If we're at the end of the pattern
		// but not at the end of the string, we failed.
		
		if (pIndex == patLen) {
		    if (sIndex == strLen) {
			return true;
		    } else {
			return false;
		    }
		}
		if ((sIndex == strLen) && (patArr[pIndex] != '*')) {
		    return false;
		}

		// Check for a "*" as the next pattern character.  It matches
		// any substring.  We handle this by calling ourselves
		// recursively for each postfix of string, until either we
		// match or we reach the end of the string.
		
		if (patArr[pIndex] == '*') {
		    pIndex++;
		    if (pIndex == patLen) {
			return true;
		    }
		    while (true) {
			if (_match2(str.substring(sIndex), 
				pat.substring(pIndex))) {
			    return true;
			}
			if (sIndex == strLen) {
			    return false;
			}
			sIndex++;
		    }
		}
		
		// Check for a "?" as the next pattern character.  It matches
		// any single character.
		  
		if (patArr[pIndex] == '?') {
		    incrIndex = true;
		    continue;
		}

		// Check for a "[" as the next pattern character.  It is followed
		// by a list of characters that are acceptable, or by a range
		// (two characters separated by "-").
		
		if (patArr[pIndex] == '[') {
		    pIndex++;
		    
		    while (true) {
			if ((pIndex == patLen) || (patArr[pIndex] == ']')) {
			    return false;
			}
			if (sIndex == strLen) {
			    return false;
			}
			ch1 = patArr[pIndex];
			strch = strArr[sIndex];
			if (((pIndex + 1) != patLen) && (patArr[pIndex + 1] == '-')) {
			    if ((pIndex += 2) == patLen) {
				return false;
			    }
			    ch2 = patArr[pIndex];
			    if (((ch1 <= strch) && (ch2 >= strch)) ||
				    ((ch1 >= strch) && (ch2 <= strch))) {
				break;
			    }
			} else if (ch1 == strch) {
			    break;
			}
			pIndex++;
		    }
		    
		    for (pIndex++; ((pIndex != patLen) && (patArr[pIndex] != ']'));
			 pIndex++) {
		    }
		    if (pIndex == patLen) {
			return false;
		    }
		    incrIndex = true;
		    continue;
		}
		
		// If the next pattern character is '\', just strip off the '\'
		// so we do exact matching on the character that follows.
		
		if (patArr[pIndex] == '\\') {
		    pIndex++;
		    if (pIndex == patLen) {
			return false;
		    }
			if ((sIndex == strLen) || (patArr[pIndex] != strArr[sIndex])) {
			    return false;
			}
			incrIndex = true;
			continue;
		}
		
		if (patArr[pIndex] == '+' || patArr[pIndex] == '-') {
			if (sIndex == strLen) return false;
			
			// strip off numbers from pat and str
			int stri = sIndex;
			for (; stri <strLen && strArr[stri] >= '0' && strArr[stri] <= '9';
							stri++);
			if (stri == sIndex) return false; // not a number
			int pati = pIndex + 1;
			for (; patArr[pati] >= '0' && patArr[pati] <= '9'; pati++);
			int patj = pati + 1;
			for (; patj <patLen && patArr[patj] >= '0' && patArr[patj] <= '9';
							patj++);
			
			try {
				int strnum = Integer.parseInt(new String(strArr, sIndex,
										stri-sIndex));
				int firstnum = Integer.parseInt(new String(patArr, pIndex+1,
										pati-pIndex-1));
				int secondnum = Integer.parseInt(new String(patArr, pati+1,
										patj-pati-1));
				if (patArr[pIndex] == '-') {
					if (strnum < Math.min(firstnum, secondnum)
						|| strnum > Math.max(firstnum, secondnum)) return false;
				}
				else {
					if (strnum < firstnum
						|| strnum > firstnum + secondnum-1) return false;
				}
			}
			catch (Exception e_) {
				return false;
			}
			pIndex = patj;
			sIndex = stri;
			continue;
		}

		// There's no special character.  Just make sure that the next
		// characters of each string match.
		
		if ((sIndex == strLen) || (patArr[pIndex] != strArr[sIndex])) {
		    return false;
		}
		incrIndex = true;
	    }
	}

	public static void sort(Object[] array_, boolean accendingOrder_)
	{
		// bubble sort
		String[] ids_ = new String[array_.length];
		for (int i=0; i<array_.length; i++)
			ids_[i] = array_[i].toString();
		for(;;) {
			boolean changed_ = false;
			for (int i=0; i<array_.length-1; i++) {
				String s = ids_[i];
				String t = ids_[i+1];
				int result_ = compare(s,t);
				if (result_ == 0) continue;
				if (!((result_ > 0) ^ accendingOrder_)) {
					//exchange
					Object tmp_ = array_[i];
					array_[i] = array_[i+1];
					array_[i+1] = tmp_;
					ids_[i] = t;
					ids_[i+1] = s;
					changed_ = true;
				}
			}
			if (!changed_) break;
		}
	}
	
	public static void sort(String[] array_, boolean accendingOrder_)
	{
		// bubble sort
		for(;;) {
			boolean changed_ = false;
			for (int i=0; i<array_.length-1; i++) {
				String s = array_[i];
				String t = array_[i+1];
				int result_ = compare(s,t);
				if (result_ == 0) continue;
				if (!((result_ > 0) ^ accendingOrder_)) {
					//exchange
					array_[i] = t;
					array_[i+1] = s;
					changed_ = true;
				}
			}
			if (!changed_) break;
		}
	}
	
	public static void sort(StringBuffer[] array_, boolean accendingOrder_)
	{
		// bubble sort
		for(;;) {
			boolean changed_ = false;
			for (int i=0; i<array_.length-1; i++) {
				StringBuffer s = array_[i];
				StringBuffer t = array_[i+1];
				int result_ = compare(s,t);
				if (result_ == 0) continue;
				if (!((result_ > 0) ^ accendingOrder_)) {
					//exchange
					array_[i] = t;
					array_[i+1] = s;
					changed_ = true;
				}
			}
			if (!changed_) break;
		}
	}
	
	/**
	 * Returns 1 if s &gt; t, -1 if s < t, 0 otherwise.
	 */
	public static int compare(String s, String t)
	{
		int slen_ = s.length();
		int tlen_ = t.length();
		int len_ = Math.min(slen_, tlen_);
		for (int i=0; i<len_; i++) {
			char a=s.charAt(i), b=t.charAt(i);
			if (a > b) return 1;
			else if (a < b) return -1;
		}
		if (slen_ > tlen_) return 1;
		else if (slen_ < tlen_) return -1;
		else return 0;
	}
	
	/**
	 * Returns 1 if s &gt; t, -1 if s < t, 0 otherwise.
	 */
	public static int compare(StringBuffer s, StringBuffer t)
	{
		int slen_ = s.length();
		int tlen_ = t.length();
		int len_ = Math.min(slen_, tlen_);
		for (int i=0; i<len_; i++) {
			char a=s.charAt(i), b=t.charAt(i);
			if (a > b) return 1;
			else if (a < b) return -1;
		}
		if (slen_ > tlen_) return 1;
		else if (slen_ < tlen_) return -1;
		else return 0;
	}
	
	/**
	 * Returns null if nothing to do.
	 */
	public static String findCommonPrefix(String[] ss_)
	{
		if (ss_ == null || ss_.length == 0) return null;
		String result_ = null;
		for (int i=0; i<ss_.length; i++) {
			if (ss_[i] == null) continue;
			if (result_ == null) {
				result_ = ss_[i]; continue;
			}
			
			String s_ = ss_[i];
			int j=result_.length();
			for (; j>=0; j--) {
				String sub_ = result_.substring(0, j);
				if (s_.startsWith(sub_)) {
					result_ = sub_; break;
				}
			}
			if (result_.length() == 0) return result_;
		}
		
		return result_;
	}

	/** Represents an integer in the binary representation.  */
    public static String toBinary(int n_)
	{ return toBinary(n_, 0, 32); }

	/**
	 * Represents an integer in the binary form.
	 * @param skipLeadingZeros_ if true, leading zeros are not printed.
	 */
    public static String toBinary(int n_, boolean skipLeadingZeros_)
	{
		if (!skipLeadingZeros_)
			return toBinary(n_, 0, 32);

		String result_ = toBinary(n_, 0, 32);
		for (int i=0; i<result_.length(); i++)
			if (result_.charAt(i) != '0') return result_.substring(i);
		return "0";
	}

	/**
	 * Represents a range of bits in an integer in the binary form.
	 * The range is specified by the index of the least significant bit and
	 * the number of bits in the range.
	 * @param lsb_ index of least significant bit.
	 * @param nb_ number of bits.
	 */
    public static String toBinary(int n_, int lsb_, int nb_)
	{
		if (lsb_ >= 32) return "";
		else if (lsb_ + nb_ > 32) nb_ = 32 - lsb_;
		StringBuffer sb_ = new StringBuffer(nb_);

		int p_ = 1 << lsb_;
		for (int i = 0 ; i < nb_; i++) {
			sb_.append(((p_ & n_) != 0)? "1": "0");
			p_ <<= 1;
		}
		sb_.reverse();
		return sb_.toString();
    }

	/** Represents a long integer in the binary form.  */
    public static String toBinary(long n_)
	{ return toBinary(n_, 0, 64); }

	/**
	 * Represents a long integer in the binary form.
	 * @param skipLeadingZeros_ if true, leading zeros are not printed.
	 */
    public static String toBinary(long n_, boolean skipLeadingZeros_)
	{
		if (!skipLeadingZeros_)
			return toBinary(n_, 0, 64);

		String result_ = toBinary(n_, 0, 64);
		for (int i=0; i<result_.length(); i++)
			if (result_.charAt(i) != '0') return result_.substring(i);
		return "0";
	}

	/**
	 * Represents a range of bits in a long integer in the binary form.
	 * The range is specified by the index of the least significant bit and
	 * the number of bits in the range.
	 * @param lsb_ index of least significant bit.
	 * @param nb_ number of bits.
	 */
    public static String toBinary(long n_, int lsb_, int nb_)
	{
		if (lsb_ >= 64) return "";
		else if (lsb_ + nb_ > 64) nb_ = 64 - lsb_;
		StringBuffer sb_ = new StringBuffer(nb_);

		long p_ = 1L << lsb_;
		for (int i = 0 ; i < nb_; i++) {
			sb_.append(((p_ & n_) != 0)? "1": "0");
			p_ <<= 1;
		}
		sb_.reverse();
		return sb_.toString();
    }

	/** Represents an integer in the hex form.  */
    public static String toHex(int n_)
	{ return toHex(n_, 0, 32); }

	/**
	 * Represents an integer in the hex form.
	 * @param skipLeadingZeros_ if true, leading zeros are not printed.
	 */
    public static String toHex(int n_, boolean skipLeadingZeros_)
	{
		if (!skipLeadingZeros_)
			return toHex(n_, 0, 32);

		String result_ = toHex(n_, 0, 32);
		for (int i=0; i<result_.length(); i++)
			if (result_.charAt(i) != '0') return result_.substring(i);
		return "0";
	}

	/**
	 * Represents a range of bits in an integer in the hex form.
	 * The range is specified by the index of the least significant bit and
	 * the number of bits in the range.
	 * @param lsb_ index of least significant bit.
	 * @param nb_ number of bits.
	 */
    public static String toHex(int n_, int lsb_, int nb_)
	{
		if (lsb_ >= 32) return "";
		else if (lsb_ + nb_ > 32) nb_ = 32 - lsb_;
		StringBuffer sb_ = new StringBuffer(nb_/4 + 1);

		int mask_ = nb_ == 32? -1: ((1 << nb_) - 1) << lsb_;
		int v_ = n_ & mask_;
		if (v_ < 0 && lsb_ > 0) {
			v_ = (v_ >> 1) - (1 << 31); // should become positive
			v_ >>= (lsb_-1);
		}
		else v_ >>= lsb_;
		int probe_ = 0xf;
		nb_ = nb_/4 + (nb_%4 > 0? 1: 0);
		for (int i = 0 ; i < nb_; i++) {
			sb_.append(HEX[v_ & probe_]);
			if (v_ < 0) {
				v_ = (v_ >> 1) - (1 << 31); // should become positive
				v_ >>= 3;
			}
			else
				v_ >>= 4;
		}
		sb_.reverse();
		return sb_.toString();
    }

	/** Represents a long integer in the hex form.  */
    public static String toHex(long n_)
	{ return toHex(n_, 0, 64); }

	/**
	 * Represents a long integer in the hex form.
	 * @param skipLeadingZeros_ if true, leading zeros are not printed.
	 */
    public static String toHex(long n_, boolean skipLeadingZeros_)
	{
		if (!skipLeadingZeros_)
			return toHex(n_, 0, 64);

		String result_ = toHex(n_, 0, 64);
		for (int i=0; i<result_.length(); i++)
			if (result_.charAt(i) != '0') return result_.substring(i);
		return "0";
	}

	/**
	 * Represents a range of bits in a long integer in the hex form.
	 * The range is specified by the index of the least significant bit and
	 * the number of bits in the range.
	 * @param lsb_ index of least significant bit.
	 * @param nb_ number of bits.
	 */
    public static String toHex(long n_, int lsb_, int nb_)
	{
		if (lsb_ >= 64) return "";
		else if (lsb_ + nb_ > 64) nb_ = 64 - lsb_;
		StringBuffer sb_ = new StringBuffer(nb_/4 + 1);

		long mask_ = nb_ == 64? -1: ((1L << nb_) - 1) << lsb_;
		long v_ = n_ & mask_;
		if (v_ < 0 && lsb_ > 0) {
			v_ = (v_ >> 1) - (1L << 63); // should become positive
			v_ >>= (lsb_-1);
		}
		else v_ >>= lsb_;
		long probe_ = 0xf;
		nb_ = nb_/4 + (nb_%4 > 0? 1: 0);
		for (int i = 0 ; i < nb_; i++) {
			sb_.append(HEX[(int)(v_ & probe_)]);
			if (v_ < 0) {
				v_ = (v_ >> 1) - (1L << 63); // should become positive
				v_ >>= 3;
			}
			else
				v_ >>= 4;
		}
		sb_.reverse();
		return sb_.toString();
    }

	/** Converts a heximal to a long integer. 
	 * The method does not check if the heximal is in valid format. */
	public static long hexToLong(String hex_)
	{
		long value_ = 0;
		for (int i=0; i<hex_.length(); i++) {
			char c_ = hex_.charAt(i);
			if (c_ <= '9') c_ -= '0';
			else if (c_ <= 'F') c_ += 10 - 'A';
			else c_ += 10 - 'a';
			value_ = (value_ << 4) + c_;
		}
		return value_;
	}
	
	public static String toDottedDecimal(long value_)
	{ return toDottedDecimal(value_, 4); }

	public static String toDottedDecimal(long value_, int nDecimals_)
	{
		StringBuffer sb_ = new StringBuffer(4*nDecimals_-1); // xxx.xxx.xxx.xxx
		long mask_ = 0x0FFL << ((nDecimals_ - 1) << 3);
		for (int i=nDecimals_-1; i>=0; i--) {
			if (sb_.length() == 0)
				sb_.append((value_ & mask_) >> (i << 3));
			else {
				sb_.append('.');
				sb_.append((value_ & mask_) >> (i << 3));
			}
			mask_ = (mask_ >> 8) & 0x7FFFFFFFFFFFFFFFL;
		}
		return sb_.toString();
	}

	public static long dottedDecimalToLong(String dd_)
	{
		long value_ = 0;
		int index_ = 0;
		while (index_ < dd_.length()) {
			int nextIndex_ = dd_.indexOf('.', index_);
			if (nextIndex_ == index_) {
				index_++;
				continue;
			}
			if (nextIndex_ < 0)
				nextIndex_ = dd_.length();
			value_ = (value_ << 8)
					+ Integer.parseInt(dd_.substring(index_, nextIndex_));
			index_ = nextIndex_ + 1;
		}
		return value_;
	}

	public static void main(String[] arg)
	{
		try {
			BufferedReader r_ = new BufferedReader(
							new InputStreamReader(System.in));
			
			while (true)
			{
				String line_ = r_.readLine();
				if (line_.equals("exit")) break;
				
				String[] ss_ = substrings(line_);
				if (ss_.length <= 1) continue;
				
				try {
					double value_ = Double.valueOf(ss_[0]).doubleValue();
					int nn_ = (int) Double.valueOf(ss_[1]).doubleValue();
					
					if (ss_.length == 2) {
						System.out.println("Result: '" + toString(value_, nn_)
										+ "'");
					}
					else {
						int ff_ = (int)Double.valueOf(ss_[2]).doubleValue();
						System.out.println("Result: '" + toString(value_, nn_,
												ff_) + "'");
					}
				}
				catch (Exception e_) {
					System.out.println(e_);
				}
			}
		}
		catch (Exception e_) {
			System.out.println(e_);
		}
		finally {
			System.out.println("System exited.");
		}
	}
}
