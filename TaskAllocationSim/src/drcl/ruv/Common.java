// @(#)Common.java   9/2002
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

import java.util.*;
import drcl.comp.*;

/**
 * Class that provides common utility methods that facilitate implementing file-system 
 * commands in different shells.
 */
public class Common
{
	public static boolean debug = false;
	public static final String ESCAPE_CHAR_SET = "/\\ *?[]";
	/**
	 * Given path_ and start component of the path, returns the corresponding
	 * component/port/wrapped object.
	 * If error occurs, a message of String type is returned instead.
	 * In addition to the path defined in drcl.comp.IDUtil, the method 
	 * also accepts WrapperComponent character '@' which is used to 
	 * refers to the object being wrapped in the Wrapper, it must appear
	 * at the end of the path, unless the wrapped object is still
	 * a component.
	 * 
	 * If resolveAlias flag is on, the alias is resolved, otherwise the alias's
	 * parent is resolved and the alias as well as the parent are returned in
	 * an array (array[0]: parent, array[1]: alias (String)).
	 * 
	 * The method ignores obvious errors such as addtional separator
	 * character '/' and out of bound parent reference '..'.
	 */
	public static Directory[] resolvePaths(Paths[] pathss_, CommandOption option_,
										   Shell shell_)
	{
		Vector v_ = new Vector();
		for (int i=0; i<pathss_.length; i++) {
			Object tmp_ = resolveOnePaths(pathss_[i], option_, shell_);
			__mergeDirectory(v_, tmp_);
		}
		Directory[] result_ = new Directory[v_.size()];
		v_.copyInto(result_);
		return result_;
	}
	
	static void __mergeDirectory(Vector v_, Object dir_)
	{
		if (dir_ instanceof Directory)
			v_.addElement(dir_);
		else {
			Directory[] tmp_ = (Directory[])dir_;
		
			if (tmp_ != null)
				for (int j=0; j<tmp_.length; j++)
					v_.addElement(tmp_[j]);
		}
	}

	// called by resolvePaths()
	// returns null/Directory[]
	static Directory[] resolveOnePaths(Paths paths_, CommandOption option_, Shell shell_)
	{
		if (paths_.base == null || paths_.paths == null || paths_.paths.length == 0) {
			if (paths_.object != null) {
				if (paths_.paths == null) // pure object reference
					return new Directory[]{new Directory(paths_.object, "", false)};
			}
			else
				return null;
		}
		Vector v_ = new Vector();
		try {
			for (int i=0; i<paths_.paths.length; i++) {
				Object tmp_ = null;
				if (paths_.base != null) {
					tmp_ = resolveOnePath(paths_.base, paths_.paths[i], paths_.base,
										  option_, paths_.paths[i], shell_);
				}
				else
					tmp_ = _resolveOnePath((Object[])paths_.object, paths_.paths[i], paths_.base,
										  option_, paths_.paths[i], shell_);
				__mergeDirectory(v_, tmp_);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
		Directory[] result_ = new Directory[v_.size()];
		v_.copyInto(result_);
		return result_;
	}

	// an irregular signal to distinguish wrapped object and component/port id's
	// used in resolveOnePath()
	static final String STOP_RESOLUTION_PREFIX = "?!$%#";
	static final String STOP_RESOLUTION_PORT = "?!$%#port";
	static final String STOP_RESOLUTION_COMPONENT = "?!$%#component";
	static final Object NULL_WRAPPED_OBJECT = "<no wrapped object>";
	static final Object EMPTY_WILDCARD_EXPANSION = "<empty wildcard expansion>";
	static final Object EXHAUST = "<exhaust search>";
	
	// called by resolveOnePath()
	// returns null/Directory/Directory[]
	static Object _resolveOnePath(Object[] targets_, String path_, Component ref_, 
								 CommandOption option_, String originalPath_, Shell shell_)
	{
		if (debug) java.lang.System.out.println("resolving multiple: " + drcl.util.StringUtil.toString(targets_) + " at " + path_);
		boolean hide_ = option_.hide;
		Vector v_ = new Vector();
		for (int k=0; k<targets_.length; k++) {
			Object tmp_ = null;
			if (targets_[k] instanceof Port) {
				Port p_ = (Port)targets_[k];
				if (hide_ && (p_.getID().startsWith(".") || p_.getGroupID().startsWith(".")))
					continue;
				if (path_ == null || path_.length() == 0)
					tmp_ = resolveOnePath(p_.host, p_.getID() + "@" + p_.getGroupID(),
										  ref_, option_, originalPath_, shell_);
				else
					tmp_ = resolveOnePath(p_.host, p_.getID() + "@" + p_.getGroupID() + "/" + path_,
										  ref_, option_, originalPath_, shell_);
			} else {
				Component c_ = (Component)targets_[k];
				if (hide_ && c_.getID().startsWith(".")) continue;
				if (debug) java.lang.System.out.println("resolving from " + c_ + ": " + path_);
				tmp_ = resolveOnePath(c_, path_, ref_, option_, originalPath_, shell_);
			}
			if (tmp_ == null) continue;
			__mergeDirectory(v_, tmp_);
		}
		if (v_.size() == 0) return null;
		else {
			Directory[] result_ = new Directory[v_.size()];
			v_.copyInto(result_);
			return result_;
		}
	}

	// called by resolveOnePaths()
	// returns null/Directory/Directory[]
	static Object resolveOnePath(Component start_, String path_, Component ref_, 
								 CommandOption option_, String originalPath_, Shell shell_)
	{
		int i = 0;
		if (path_.length() > 0 && path_.charAt(0) == '/') {
			start_ = start_.getRoot();
			i = 1;
		}
		
		boolean trailingSlash_ = path_.endsWith("/");
			
		//// special treat for not resolving alias
		//Component aliasParent_ = null;
		//String aliasID_ = null;
		
		Object wrapped = null;
		char[] cc_ = path_.toCharArray();
		//java.lang.System.out.println(path_ + ": " + cc_.length);
		int len_ = cc_.length;
		while (true) {
			// skip additional '/'
			for (; i<len_ && cc_[i] == '/'; i++);
			if (i >= len_) break;
			//net0_16/n8, <net0_4/n0>/dvmrp
			// search for next '/' and '<'
			// skip "escape" char, not precise (XX)
			int j;
			boolean escape_ = false;
			int insideItem_ = 0, leaveItem_ = 0; // count number of entering, and leaving, an item
			for (j=i; j<cc_.length; j++) {
				if (escape_)
					escape_ = false;
				else if (cc_[j] == '\\')
					escape_ = true;
				else if (cc_[j] == '<')
					insideItem_ ++;
				else if (cc_[j] == '>')
					leaveItem_ ++;
				else if (cc_[j] == '/' && insideItem_ <= leaveItem_) break;
			}
			String sub_ = path_.substring(i, j);
			
			// handle wrapped object
			if (wrapped != null) {
				if (wrapped == EXHAUST) {
					return _exhaust(start_, path_.substring(i), ref_, option_, originalPath_,
									option_.sort, shell_);
				}
				else if (!(wrapped instanceof String[]) && wrapped instanceof Object[]) {
					// component/port
					Object[] targets_ = (Object[])wrapped;
					return _resolveOnePath(targets_, path_.substring(i), ref_, option_,
										   originalPath_, shell_);
				}
				else if (wrapped instanceof Port) {
					if (sub_.equals("-")) {
						Port[] pp_ = ((Port)wrapped).getPeers();
						if (pp_ == null || pp_.length == 0) return null;
						return _resolveOnePath(pp_, path_.substring(j), ref_, option_,
											   originalPath_, shell_);
					}
					else if (sub_.equals("..")) {
						start_ = ((Port)wrapped).host;
						wrapped = null;
						i = j;
						continue; // continue parse next subpath
					}
					else if (sub_.equals(".")) {
						i = j;
						continue; // continue parse next subpath
					}
					else {
						// error: path after a port path
						if (!option_.quiet) 
							shell_.println("Cannot expand a path under a port: " 
									   + Util.getPortID((Port)wrapped, ref_) + "/@.\n");
						return null;
					}
				}
				else if (wrapped instanceof String[] && ((String[])wrapped).length > 0
						 && ((String[])wrapped)[0].startsWith(STOP_RESOLUTION_PREFIX)) {
					// component id/port id's
					// means the path does not exist
					// keep silence here, caller decides whether or not to issue warnings
					return null;
				}
				else {
					// error: path after a wrapped object spec
					if (!option_.quiet) 
						shell_.println("Cannot expand a path under a wrapped noncomponent object: " 
								   + Util.getID(start_, ref_) + "@ (wrapped: " + wrapped + ").\n");
					return null;
				}
			}
			
			if (sub_.equals("@")) {
				// error!
				if (!(start_ instanceof Wrapper)) {
					if (!option_.quiet) 
						shell_.println("Expect " + Util.getID(start_, ref_) + " to be a Wrapper but got "
								   + start_.getClass().getName() + "!\n");
					return null;
				}
				wrapped = ((Wrapper)start_).getObject();
				if (wrapped instanceof Component) {
					start_ = (Component)wrapped;
					wrapped = null; 
				}
				else if (wrapped == null)
					wrapped = NULL_WRAPPED_OBJECT;
			}
			else if (sub_.equals("..")) {
				if (start_.parent != null) start_ = start_.parent;
			}
			else if (sub_.equals("...")) { // search the whole subtree at start_
				wrapped = EXHAUST;
			}
			else if (insideItem_ > 0 || sub_.indexOf(",") >= 0) { // list
				wrapped = _parseList(start_, sub_, ref_, option_, originalPath_, shell_);
				if (wrapped == null) wrapped = EMPTY_WILDCARD_EXPANSION;
				if (j>=len_) {
					if (wrapped == EMPTY_WILDCARD_EXPANSION)
						return null;
					else
						return new Directory(start_, (Object[])wrapped, originalPath_, trailingSlash_);
				}
			}
			else {
				// check for matching characters *?[]
				i = sub_.indexOf('*');
				int count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
				boolean wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
				if (!wildcard_) {
					i = sub_.indexOf('?');
					count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
					wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
					if (!wildcard_) {
						i = sub_.indexOf('[');
						count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
						wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
					}
				}
				// check for range spec +-
				if (!wildcard_) {
					char symbol_ = '+';
					i = -1;
					while (true) {
						i = sub_.indexOf(symbol_, i+1);
						if (i>0 && i<sub_.length()-1) {
							// check if numbers are before and after symbol_
							// check before
							count_ = 0;
							for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
							if (count_ <= 1 && Character.isDigit(sub_.charAt(i-1-count_))) {
								// check after
								count_ = 0;
								for (int k=i+1; k<sub_.length() && sub_.charAt(k) == '\\'; k++) count_++;
								if (count_ <= 1 && Character.isDigit(sub_.charAt(i+1+count_))) {
									wildcard_ = true; break;
								}
							}
						}
						else if (symbol_ == '+') {
							symbol_ = '-';
							i = -1;
						}
						else
							break;
					}
				}
				// check for class name
				if (!wildcard_ && sub_.indexOf('.') > 0) {
					try {
						Class class_ = Class.forName(sub_);
						wildcard_ = true;
					}
					catch (Exception e_) {}
				}
				
				// handle wildcard expansion
				if (wildcard_) {
					Object[] tmp_ = _expand(start_, sub_, option_, shell_);
					if (tmp_ != null && tmp_.length == 0) tmp_ = null;
					String[] rangeSpecResult_ = null;
					if (tmp_ == null) {
						rangeSpecResult_ = _parseRangeSpec(sub_);
						if (rangeSpecResult_ != null && rangeSpecResult_.length == 0) rangeSpecResult_ = null;
						if (rangeSpecResult_ != null) {
							String[] resulttmp_ = new String[rangeSpecResult_.length + 2];
							java.lang.System.arraycopy(rangeSpecResult_, 0, resulttmp_, 2, rangeSpecResult_.length);
							resulttmp_[0] = sub_.indexOf('@') >= 0? STOP_RESOLUTION_PORT: STOP_RESOLUTION_COMPONENT;
							resulttmp_[1] = sub_;
							rangeSpecResult_ = resulttmp_;
						}
					}
					wrapped = rangeSpecResult_ == null? tmp_: rangeSpecResult_;
					if (wrapped == null) wrapped = EMPTY_WILDCARD_EXPANSION;
					if (j>=len_) {
						if (wrapped == EMPTY_WILDCARD_EXPANSION)
							return null;
						else if (rangeSpecResult_ == null)
							return new Directory(start_, tmp_, originalPath_, trailingSlash_);
						else
							return new Directory(start_, rangeSpecResult_, originalPath_, trailingSlash_);
					}
				}
				else if (sub_.indexOf('@') >= 0) {
					sub_ = drcl.util.StringUtil.removeEscape(sub_);
					// a port
					int k = sub_.indexOf('@');
					String id_ = sub_.substring(0, k);
					String gid_ = sub_.substring(k+1);
					if (gid_ == null) gid_ = "";
					// put port in wrapped
					wrapped = start_.getPort(gid_, id_);
					if (wrapped == null) {
						if (option_.createPort || option_.recordID)
							wrapped = new String[]{STOP_RESOLUTION_PORT, sub_, id_ + "@" + gid_};
						else
							return null;
					}
				}
				else if (!sub_.equals(".")) {
					if (debug) java.lang.System.out.println("resolve '" + sub_ + "'");
															
					sub_ = drcl.util.StringUtil.removeEscape(sub_);
					Component child_ = start_.getComponent(sub_);
					if (child_ == null) {
						if (option_.recordID)
							wrapped = new String[]{STOP_RESOLUTION_COMPONENT, sub_, sub_};
						else
							return null;
					}
					else 
						start_ = child_;
					/*
					if (child_ == null) {
						child_ = start_.getAlias(sub_);
						if (!resolveAlias_) {
							// special treat for not resolving alias
							// don't know if it's last one resolved, so save the info
							aliasParent_ = start_;
							aliasID_ = sub_;
						}
					}
					else if (!resolveAlias_) {
						// special treat for not resolving alias
						aliasParent_ = null; aliasID_ = null;
					}
					*/
					
					//if (child_ == null) return path_.substring(0, j) + " does not exist.";
				}
			} // last else
			i = j;
			//if (start_ == null) break;
		}

		if (start_ == null) return null;
		if (wrapped == null) {
			if (option_.expand) {
				return new Directory(start_, _expand(start_, "*", option_, shell_), originalPath_, trailingSlash_);
			}
			else {
				//java.lang.System.out.println("Matches " + start_);
				return new Directory(start_.getParent(), new Object[]{start_}, originalPath_, trailingSlash_);
			}
		}
		else if (wrapped == EMPTY_WILDCARD_EXPANSION)
			return null;
		else if (wrapped == EXHAUST)
			return _exhaust(start_, "", ref_, option_, originalPath_, option_.sort, shell_);
			//return new Directory(start_, expand(start_, "*", option_), originalPath_, trailingSlash_);
		else {
			if (wrapped instanceof Port) 
				return new Directory(start_, new Object[]{wrapped}, originalPath_, trailingSlash_);
			else if (start_ instanceof Wrapper
				&& (((Wrapper)start_).getObject() == wrapped || wrapped == NULL_WRAPPED_OBJECT))
				// wrapped object
				return new Directory(start_, new Object[]{wrapped}, originalPath_, trailingSlash_);
			else if (!(wrapped instanceof String[]) && wrapped instanceof Object[]) 
				// component/port
				return new Directory(start_, (Object[])wrapped, originalPath_, trailingSlash_);
			else {
				// component id/port id's
				return new Directory(start_, (String[])wrapped, originalPath_, trailingSlash_);
			}
		}
	}

	// list: <item>,<item>,<item>
	// <item>: no "/" in it
	static Object[] _parseList(Component start_, String path_, Component ref_, 
						  CommandOption option_, String originalPath_, Shell shell_)
	{
		if (debug) java.lang.System.out.println("LIST: " + path_);
		char[] cc_ = path_.toCharArray();
		int len_ = cc_.length;
		Vector v_ = new Vector();
		int i = 0;
		while (true) {
			// skip additional ','
			for (; i<len_ && (cc_[i] == ',' || cc_[i] == ' '); i++);
			if (i >= len_) break;

			int j;
			boolean escape_ = false;
			int insideItem_ = 0, leaveItem_ = 0; // count number of entering, and leaving, an item
			boolean sublist_ = false; // true if a sublist exists in a list
			for (j=i; j<cc_.length; j++) {
				if (escape_)
					escape_ = false;
				else if (cc_[j] == '\\')
					escape_ = true;
				else if (cc_[j] == '<')
					insideItem_ ++;
				else if (cc_[j] == '>')
					leaveItem_ ++;
				else if (cc_[j] == ',') {
					if (insideItem_ <= leaveItem_) break;
					else sublist_ = true;
				}
			}
			
			String sub_ = path_.substring(i, j);
			if (cc_[i] == '<') {
				// remove "<>" pair at the beginning and the end
				int k;
				for (k=j-1; k>i; k--)
					if (cc_[k] == '>') break;
				if (k == i) // error occurs
					k = j;
				sub_ = sub_.substring(1, k-i);
			}
			
			if (debug) java.lang.System.out.println("LIST sub: " + sub_ + ", sublist: " + sublist_);
			
			// don't expand items in a list
			boolean expand_ = option_.expand;
			option_.expand = false;
			Object tmp_ = resolveOnePath(start_, sub_, ref_, option_, originalPath_, shell_);
			option_.expand = expand_;
			
			if (tmp_ == null) ;
			else if (tmp_ instanceof Directory[]) {
				Directory[] result_ = (Directory[])tmp_;
				for (int l=0; l<result_.length; l++)
					__saveDirectory(v_, result_[l]);
			}
			else if (tmp_ instanceof Directory)
				__saveDirectory(v_, (Directory)tmp_);
			else if (tmp_ != null) {
				Object[] oo_ = (Object[])tmp_;
				for (int k=0; k<oo_.length; k++)
					v_.addElement(oo_[k]);
			}
			
			i = j;
		}
		
		if (v_.size() == 0) return null;
		Object result_[] = new Object[v_.size()];
		v_.copyInto(result_);
		return result_;
	}
	
	// called by _parseList()
	static void __saveDirectory(Vector v_, Directory d_)
	{
		Object[] oo_ = d_.child;
		if (oo_ == null || oo_.length == 0) return;
		for (int i=0; i<oo_.length; i++)
			v_.addElement(oo_[i]);
	}
	
	// called by resolveOnePath(), it in turn invokes resolveOnePath().
	// returns null/Directory/Directory[]
	static Object _exhaust(Component start_, String path_, Component ref_, 
						  CommandOption option_, String originalPath_,
						  boolean sort_, Shell shell_)
	{
		//java.lang.System.out.println("EXHAUST " + start_ + " for path '" + path_ + "'");
		Vector v_ = new Vector();
		boolean hide_ = option_.hide;
		
		option_.quiet = true; // turn on the quiet flag
		// resolve under start_
		Object tmp_ = resolveOnePath(start_, path_, ref_, option_, originalPath_, shell_);
		//if (tmp_ == null) return null;
		__mergeDirectory(v_, tmp_);
		
		// resolve under all children of start_
		Component[] children_ = start_.getAllComponents();
		if (children_ != null && children_.length > 0) {
			for (int i=0; i<children_.length; i++) {
				if (hide_ && children_[i].getID().startsWith(".")) continue;
				tmp_ = _exhaust(children_[i], path_, ref_, option_, originalPath_, false, shell_);
				if (tmp_ == null) continue;
				__mergeDirectory(v_, tmp_);
			}
		}
		
		//java.lang.System.out.println("RESULT: " + v_);
		if (v_.size() == 0) return null;
		else {
			Directory[] result_ = new Directory[v_.size()];
			v_.copyInto(result_);
			
			if (sort_ && result_.length > 1) {
				int count_ = 0; // for noncomponent/nonport object
				Hashtable ht_ = new Hashtable();
				String[] names_ = new String[result_.length];
				for (int i=0; i<result_.length; i++) {
					Object o_ = result_[i].parent;
					if (o_ instanceof Component || o_ instanceof Port)
						names_[i] = o_.toString();
					else
						names_[i] = "@@" + (count_++);
					ht_.put(names_[i], result_[i]);
				}
				drcl.util.StringUtil.sort(names_, true);
				for (int i=0; i<result_.length; i++) {
					result_[i] = (Directory)ht_.get(names_[i]);
				}
			}
			
			return result_;
		}
	}
	
	// expand wildcard path to components/ports
	static Object[] _expand(Component target_, String wildcardPath_, CommandOption option_,
							Shell shell_)
	{
		// FIXME: hardcoded port class
		if (wildcardPath_.equals("drcl.comp.Port")) wildcardPath_ = "*@*";
		
		boolean hide_ = option_.hide;
		boolean includePort_ = option_.includePort;
		boolean portOnly_ = wildcardPath_.indexOf('@') >= 0; // port only
		boolean all_ = wildcardPath_.equals("*");
		Vector v_ = new Vector();
		if (!portOnly_) {
			Component[] children_ = target_.getAllComponents();
			if (all_) {
				for (int i=0; i<children_.length; i++)
					if (hide_ && !wildcardPath_.startsWith(".") && children_[i].getID().startsWith(".")) continue;
					else v_.addElement(children_[i]);
			}
			else {
				Class class_ = null;
				try {
					class_ = Class.forName(wildcardPath_);
				}
				catch (Exception e_) {}
				for (int i=0; i<children_.length; i++)
					if (hide_ && !wildcardPath_.startsWith(".") && children_[i].getID().startsWith("."))
						continue;
					else if (class_ != null && class_.isAssignableFrom(children_[i].getClass()))
						v_.addElement(children_[i]);
					else if (drcl.util.StringUtil.match2(children_[i].getID(), wildcardPath_))
						v_.addElement(children_[i]);
			}
		}
		
		if (portOnly_ || includePort_) {
			all_ = all_ || wildcardPath_.equals("*@*");
			Port[] ports_ = target_.getAllPorts();
			if (all_) {
				for (int i=0; i<ports_.length; i++)
					// FIXME: the condition !wildcardPath_.startsWith(".") is not exact, but works in most cases
					if (hide_ && !wildcardPath_.startsWith(".") && (ports_[i].getID().startsWith(".")
									|| ports_[i].getGroupID().startsWith("."))) continue;
					else v_.addElement(ports_[i]);
			}
			else
				for (int i=0; i<ports_.length; i++)
					// FIXME: the condition !wildcardPath_.startsWith(".") is not exact, but works in most cases
					if (hide_ && !wildcardPath_.startsWith(".") && (ports_[i].getID().startsWith(".")
									|| ports_[i].getGroupID().startsWith("."))) continue;
					else if (drcl.util.StringUtil.match2(ports_[i].getID() + "@" + ports_[i].getGroupID(),
														wildcardPath_))
						v_.addElement(ports_[i]);
		}
		Object[] result_ = new Object[v_.size()];
		v_.copyInto(result_);
		if (option_.sort && result_.length > 1) {
			int count_ = 0; // for noncomponent/nonport object
			Hashtable ht_ = new Hashtable();
			String[] names_ = new String[result_.length];
			for (int i=0; i<result_.length; i++) {
				Object o_ = result_[i];
				if (o_ instanceof Component || o_ instanceof Port)
					names_[i] = o_.toString();
				else
					names_[i] = "@@" + (count_++);
				ht_.put(names_[i], o_);
			}
			drcl.util.StringUtil.sort(names_, true);
			for (int i=0; i<result_.length; i++) {
				result_[i] = ht_.get(names_[i]);
			}
		}
		return result_;
	}
	
	// parse range spec
	// returns null/component ids/port ids
	static String[] _parseRangeSpec(String sub_)
	{
		//java.lang.System.out.println("parse range spec: '" + sub_ + "'");
		// check for matching characters *?[]
		int i = sub_.indexOf('*');
		int count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
		boolean wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
		if (!wildcard_) {
			i = sub_.indexOf('?');
			count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
			wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
			if (!wildcard_) {
				i = sub_.indexOf('[');
				count_ = 0; for (int k=i-1; k>=0 && sub_.charAt(k) == '\\'; k--) count_++;
				wildcard_ = i>=0 && count_%2 == 0; // even number of '\\'
			}
		}
		if (wildcard_) return null;
		
	    char[] carray_ = sub_.toCharArray();
		
		// check for unnecessary '\' in sub_
		for (i=carray_.length-1; i>=0; i--) {
			if (carray_[i] != '\\') continue;
			// count number of \ 
			count_ = 1; int j=i-1;
			for (; j>=0 && carray_[j] == '\\'; j--) count_++;
			char c = carray_[i];
			if (count_++%2 > 0
				&& i < carray_.length-1
				&& c != '+' && c != '-')
					sub_ = sub_.substring(0,j) + sub_.substring(j+2);
			i = j;
		}
		
		// scan in 'sub_' for +- range spec
		StringBuffer sb_ = new StringBuffer(sub_);
		for (i=carray_.length-1; i>=0; i--) { // must scan backwards...
			if (carray_[i] != '+' && carray_[i] != '-') continue;
			// count number of \ before this +-
			// continue if odd number of '\' is encountered
			count_ = 0;
			for (int j=i-1; j>=0 && carray_[j] == '\\'; j--) count_++;
			if (count_++%2 > 0) continue;
			
			// check the second number
			int k=i+1;
			for (; k<carray_.length && carray_[k]>='0' && carray_[k]<='9'; k++);
			if (k == i+1) { // no 2nd number, not a range spec
				sb_.insert(i, '\\');
				continue;
			}
			
			// back track the first number in the range spec
			k=i-1;
			for (; k>=0 && carray_[k]>='0' && carray_[k]<='9'; k--);
			if (k == i-1)  // no first number, not a range spec
				sb_.insert(i, '\\');
			else { // insert +/- before the first number
				sb_.insert(k+1, carray_[i]);
				i = k+1;
			}
		}
		if (sb_.length() > carray_.length) {
			sub_ = sb_.toString();
		}
		return __parseRangeSpec(sub_);
	}
	
	// called by parseRangeSpec()
	// assume spec_ contains no error
	// not a good implementation if spec contains more than one range spec
	static String[] __parseRangeSpec(String spec_)
	{
		Vector v_ = new Vector();
		v_.addElement(spec_);
		
		int i = 0;
		while (i < v_.size()) {
			String s_ = (String)v_.elementAt(i);
			int j = __find1stRangeSpecIndex(s_, 0);
			if (j < 0) { i++; continue; }
			
			// strip off numbers from s_
			int pati = j + 1;
			char[] cc_ = s_.toCharArray();
			for (; cc_[pati] >= '0' && cc_[pati] <= '9'; pati++);
			int patj = pati + 1;
			for (; patj <cc_.length && cc_[patj] >= '0' && cc_[patj] <= '9'; patj++);
			v_.removeElementAt(i);
			try {
				int firstnum = Integer.parseInt(new String(cc_, j+1, pati-j-1));
				int secondnum = Integer.parseInt(new String(cc_, pati+1, patj-pati-1));
				if (cc_[j] == '-') {
					for (int k=Math.min(firstnum, secondnum);
						 k<=Math.max(firstnum, secondnum); k++)
						v_.addElement(s_.substring(0, j) + k + s_.substring(patj));
				}
				else {
					for (int k=0; k<secondnum; k++)
						v_.addElement(s_.substring(0, j) + (k+firstnum) + s_.substring(patj));
				}
			}
			catch (Exception e_) {
				return null;
			}
		}
		
		//java.lang.System.out.println("parse result: " + v_);
		String[] result_ = new String[v_.size()];
		v_.copyInto(result_);
		return result_;
	}
	
	// called by __parseRangeSpec()
	static int __find1stRangeSpecIndex(String spec_, int startIndex_)
	{
		int i = startIndex_;
		while (i < spec_.length()) {
			i = spec_.indexOf("-", i);
			if (i < 0) break;
			int count_ = 0; for (int k=i-1; k>=0 && spec_.charAt(k) == '\\'; k--) count_++;
			if (count_%2 == 0) return i; // found it!
			i++;
		}
		i = startIndex_;
		while (i < spec_.length()) {
			i = spec_.indexOf("+", i);
			if (i < 0) break;
			int count_ = 0; for (int k=i-1; k>=0 && spec_.charAt(k) == '\\'; k--) count_++;
			if (count_%2 == 0) return i; // found it!
			i++;
		}
		return -1;
	}
		
	
	/**
	 * Returns the ID's of the components.
	 */
	public static String[] getID(Component[] cc_)
	{
		if (cc_ == null) return new String[0];
		String[] ids_ = new String[cc_.length];
		//for (int i=0; i<cc_.length; i++) ids_[i] = drcl.util.StringUtil.addEscape(cc_[i].id, "/\\ ");
		for (int i=0; i<cc_.length; i++) ids_[i] = cc_[i].id;
		return ids_;
	}
	
	/**
	 * Returns the ID's of the ports.
	 */
	public static String[] getID(Port[] pp_)
	{
		if (pp_ == null) return new String[0];
		String[] ids_ = new String[pp_.length];
		for (int i=0; i<pp_.length; i++) ids_[i] = pp_[i].id + "@" + pp_[i].groupID;
		return ids_;
	}
}
