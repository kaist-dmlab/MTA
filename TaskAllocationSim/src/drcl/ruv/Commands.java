// @(#)Commands.java   1/2004
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

import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import drcl.comp.*;
import drcl.util.StringUtil;

public class Commands
{
	// FIXME: shell should get the following info from terminal...
	static int ts = 8; // tab size
	static int sw = 80; // screen width
	static boolean debug = false;
	public static void setTabsize(int ts_) { ts = ts_; }
	public static int getTabsize() { return ts; }
	public static void setScreenwidth(int sw_) { sw = sw_; }
	public static int getScreenwidth() { return sw; }
	
	public static void setDebug(boolean v_)
	{ debug = v_; }
	
	static String getWarningMsg(Paths[] pathss_)
	{
		if (pathss_ == null || pathss_.length == 0) return "<no path>";
		StringBuffer sb_ = new StringBuffer(StringUtil.toString(
								pathss_[0].paths, ",", 2));
		for (int i=1; i<pathss_.length; i++)
			sb_.append(", " + StringUtil.toString(pathss_[i].paths, ",", 2));
		return sb_.toString();
	}
	
	// Internal use: common code block for most of the file commands
	// Returns Vector of Directory
	static Vector __common__(CommandOption option_, Paths[] pathss_,
					boolean sort_, Shell shell_)
	{
		option_.sort = sort_;
		Vector v_ = new Vector(); // store Directory
		for (int i=0; i<pathss_.length; i++) {
			// append "*" if a path ends with "/"
			if (pathss_[i].paths != null && option_.expand)
				// expand used here??
				for (int j=0; j<pathss_[i].paths.length; j++) {
					String path_ = pathss_[i].paths[j];
					if (path_.length() > 1 && path_.charAt(path_.length()-1)
						== '/')
						pathss_[i].paths[j] = path_ + "*";
				}
			Directory[] tmp_ = Common.resolveOnePaths(pathss_[i], option_,
							shell_);
		
			if (tmp_ == null) continue;
			for (int j=0; j<tmp_.length; j++) {
				v_.addElement(tmp_[j]);
			}
		}
		return v_;
	}
	
	static ShellFoo SHELL_FOO = null;
	static {
		try {
			SHELL_FOO = new ShellFoo();
		}
		catch (Exception e_)
		{}
	}

	public static Object[] cp(String srcpath_, String destpath_)
	{
		return cpmv("copy", "", new Paths[]{new Paths(Component.Root, srcpath_),
			new Paths((Component)null, "-d"), new Paths(Component.Root,
					destpath_)}, SHELL_FOO);
	}

	// Returns null/Object[]
	public static Object[] cpmv(String operation_, String soption_,
					Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		if (operation_ == null) {
			if (!option_.quiet)
				shell_.println("Error: no operation ('move' or 'copy')" 
								+ " is given.");
			return null;
		}
		String operation2_ = operation_.toLowerCase();
		if (!operation2_.equals("move") && !operation2_.equals("copy")) {
			if (!option_.quiet)
				shell_.println("Error: unrecognized operation, " + operation_
								+ ".\nOnly 'move' or 'copy' is accepted.");
			return null;
		}
		boolean isMove_ = operation2_.equals("move");
		operation_ = operation2_;
		
		if (isMove_) { // keep runtime
			soption_ += "k";
			option_.keepRuntime = true;
		}

		if (pathss_ == null || pathss_.length < 2) {
			if (!option_.quiet)
				shell_.println("Error: no source/dest is given.");
			return null;
		}
		
		// look for "-d" option and set up src and dest paths
		Paths[] srcpathss_ = null;
		Paths[] destpathss_ = null;
		for (int i=0; i<pathss_.length; i++) {
			String[] paths_ = pathss_[i].paths;
			if (paths_ == null || paths_.length == 0) continue;
			if (paths_[0].equals("-d")) {
				if (i == pathss_.length-1) {
					// error
					if (!option_.quiet)
						shell_.println("Error: no dest is given.");
					return null;
				}
				srcpathss_ = new Paths[i];
				java.lang.System.arraycopy(pathss_, 0, srcpathss_, 0,
								srcpathss_.length);
				destpathss_ = new Paths[pathss_.length-i-1];
				java.lang.System.arraycopy(pathss_, i+1, destpathss_, 0,
								destpathss_.length);
				break;
			}
		}
		
		// no option "-d"
		if (srcpathss_ == null) {
			int lastIndex_ = pathss_.length-1;
			srcpathss_ = new Paths[lastIndex_];
			java.lang.System.arraycopy(pathss_, 0, srcpathss_, 0,
							srcpathss_.length);
			destpathss_ = new Paths[]{pathss_[lastIndex_]};
		}
		
		// get the sources
		Object[] srcs_ = toRef(soption_, srcpathss_, false, shell_);
		if (srcs_ == null || srcs_.length == 0) {
			if (!option_.quiet) {
				shell_.println("Warning: no source is matched in "
								+ getWarningMsg(srcpathss_) + ".");
			}
			return null;
		}
		
		Vector vresult_ = new Vector();
		for (int i=0; i<srcs_.length; i++) {
			Object src_ = isMove_? srcs_[i]:
							drcl.util.ObjectUtil.clone(srcs_[i]);
			Component parent_ = null;
			if (isMove_) {
				if (src_ instanceof Port) {
					parent_ = ((Port)src_).getHost();
					parent_.removePort((Port)src_);
				}
				else if (src_ instanceof Component) {
					parent_ = ((Component)src_).getParent();
					parent_.removeComponent((Component)src_);
				}
			}
			Object result_ = mkdir(soption_, src_, destpathss_, true, shell_);
				// if cp, then mkdir here actually does the work of copying
			int size_ = vresult_.size();
			_cpmvMergeResult(result_, vresult_);
			// if operation is "move" and there's no change,
			// then restore the source
			if (isMove_ && size_ == vresult_.size() && parent_ != null) { 
				if (src_ instanceof Port) {
					Port p_ = (Port)src_;
					parent_.addPort(p_, p_.getGroupID(), p_.getID());
				}
				else if (src_ instanceof Component) {
					parent_.addComponent((Component)src_, false);
						//'false' to keep runtime
				}
			}
		}
		if (vresult_.size() == 0) {
			if (!option_.quiet) 
				shell_.println("Nothing is " + (isMove_? "moved": "copied")
								+ ".");
			return null;
		}
		
		Object[] result_ = new Object[vresult_.size()];
		vresult_.copyInto(result_);
		return result_;
	}
	
	
	// used by cpmv
	// result_ is Component/Port/Object[]
	static void _cpmvMergeResult(Object result_, Vector vresult_)
	{
		if (result_ == null) return;
		if (result_ instanceof Component || result_ instanceof Port)
			vresult_.addElement(result_);
		else {
			Object[] tmp_ = (Object[])result_;
			for (int j=0; j<tmp_.length; j++) vresult_.addElement(tmp_[j]);
		}
	}
	
	public static final Object NULL_OBJECT = new Object();
												  
	public static Object[] toRef(Component start_, String path_)
	{ return toRef("", new Paths[]{new Paths(start_, path_)}, true,
						SHELL_FOO); }

	public static Object[] toRef(Component start_, String[] paths_)
	{ return toRef("", new Paths[]{new Paths(start_, paths_)}, true,
					SHELL_FOO); }

	public static Object[] toRef(String path_)
	{ return toRef("", new Paths[]{new Paths(Component.Root, path_)}, true,
					SHELL_FOO); }

	public static Object[] toRef(String[] paths_)
	{ return toRef("", new Paths[]{new Paths(Component.Root, paths_)}, true,
					SHELL_FOO); }

	// converts paths to object references
	// returns Object[]
	public static Object[] toRef(String soption_, Paths[] pathss_,
					boolean sort_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);

		Vector v_ = __common__(option_, pathss_, sort_, shell_);

		//java.lang.System.out.println("RESULT OF __COMMON__: " + v_);
		
		Vector vresult_ = new Vector();
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			//if (d_ == null || (d_.child == null && d_.object == null))
			//	continue;
			if (d_ == null) continue;
			if (d_.ids != null) {
				if (!option_.createPort) continue;
				// make port(s)
				for (int j=2; j<d_.ids.length; j++) {
					Port new_ = new Port();
					String tmp_ = d_.ids[j];
					int index_ = tmp_.indexOf('@');
					String gid_ = index_ >= tmp_.length() -1?
								  "": tmp_.substring(index_+1);
					d_.parent.addPort(new_, gid_, tmp_.substring(0, index_));
					vresult_.addElement(new_);
				}
			}
			else if (d_.child != null) {//(d_.object == null) {
				Object[] oo_ = d_.child;
				if (oo_.length == 0) continue;
				for (int j=0; j<oo_.length; j++)
					if (oo_[j] != null && oo_[j] != Common.NULL_WRAPPED_OBJECT)
						vresult_.addElement(oo_[j]);
			}
			else if (d_.object != null) {// pure object reference
				if (d_.object == NULL_OBJECT)
					vresult_.addElement(null);
				else
					vresult_.addElement(d_.object);
			}
		}
		if (vresult_.size() == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no object was matched in "
								+ getWarningMsg(pathss_) + ".");
			return null;
		}
		//if (vresult_.size() == 1) return vresult_.firstElement();
		Object[] result_ = new Object[vresult_.size()];
		vresult_.copyInto(result_);
		return result_;
	}
	
	public static Object mkdir(Component start_, String class_, String path_)
	{
		Object[] all_ = mkdir("", class_, new Paths[]{new Paths(start_, path_)},
						false, SHELL_FOO);
		if (all_ != null && all_.length == 1) return all_[0];
		else return all_;
	}

	public static Object mkdir(Component start_, String class_, String[] paths_)
	{
		Object[] all_ = mkdir("", class_,
						new Paths[]{new Paths(start_, paths_)},
						false, SHELL_FOO);
		if (all_ != null && all_.length == 1) return all_[0];
		else return all_;
	}

	public static Object mkdir(String class_, String path_)
	{
		Object[] all_ = mkdir("", class_,
						new Paths[]{new Paths(Component.Root, path_)},
						false, SHELL_FOO);
		if (all_ != null && all_.length == 1) return all_[0];
		else return all_;
	}

	public static Object mkdir(String class_, String[] paths_)
	{
		Object[] all_ = mkdir("", class_,
						new Paths[]{new Paths(Component.Root, paths_)},
						false, SHELL_FOO);
		if (all_ != null && all_.length == 1) return all_[0];
		else return all_;
	}

	// return array
	// classInfo_ could be a class name or already a Java object
	public static Object[] mkdir(String soption_, Object classInfo_,
					Paths[] pathss_, boolean calledByOtherCmds_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);
		if (pathss_ == null || pathss_.length == 0) {
			if (!option_.quiet) {
				if (classInfo_ instanceof String
						&& ((String)classInfo_).indexOf('/') > 0)
					// classInfo_ is a path!
					shell_.println("Error: no class name or Java object"
									+ " is given.");
				else
					shell_.println("Error: no path is specified.");
			}
			return null;
		}
		if (classInfo_ == null) {
			if (!option_.quiet)
				shell_.println("Error: no class class name or Java object"
								+ " is given.");
			return null;
		}

		// get the class information from classInfo_
		Class class_ = null;
		Component comp_ = null; // component or component wrapper
		Port port_ = null;
		Object object_ = null; // wrapped object
		try {
			if (classInfo_ instanceof String) {
				// class name
				class_ = Class.forName((String)classInfo_);
				object_ = class_.newInstance();
			}
			else {
				object_ = classInfo_;
				class_ = object_.getClass();
			}
			
			if (object_ instanceof Component) {
				comp_ = (Component)object_;
				object_ = null;
			}
			else if (object_ instanceof Port) {
				port_ = (Port)object_;
				object_ = null;
			}
			else {
				comp_ = new WrapperComponent();
				((WrapperComponent)comp_).setObject(object_);
			}
		}
		catch (Exception e_) {
			shell_.println("classInfo: " + classInfo_);
			if (!option_.quiet) shell_.println(e_.toString());
			return null;
		}
		
		option_.recordID = true;
		Vector v_ = __common__(option_, pathss_, false, shell_);
		
		Vector vresult_ = new Vector();
		boolean firstTime_ = true;
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			if (d_ == null
				|| ((d_.child == null || d_.child.length == 0)
				&& d_.ids == null)) continue;
			
			if (d_.ids != null) { // have id information
				// error
				if (d_.ids[0] == Common.STOP_RESOLUTION_COMPONENT
					&& port_ != null) {
					if (!option_.quiet)
						shell_.println("Warning: cannot make a port with"
										+ " a component id (" + d_.parent
										+ "/" + d_.ids[1] + ").");
					continue;
				}
				if (d_.ids[0] == Common.STOP_RESOLUTION_PORT
						&& comp_ != null) {
					if (!option_.quiet)
						shell_.println("Warning: cannot make a component"
										+ " with a port id (" + d_.parent
										+ "/" + d_.ids[1] + ").");
					continue;
				}
				if (d_.ids[0] == Common.STOP_RESOLUTION_COMPONENT
						&& d_.trailingSlash) {
					if (!option_.quiet) {
						shell_.println("Warning: invalid path '" + d_.parent
										+ "/" + d_.ids[1] + "/.");
						shell_.println("Remove the trailing slash maybe?");
					}
					continue;
				}
				
				// everything is normal
				if (comp_ != null) {
					// make component(s)
					for (int j=2; j<d_.ids.length; j++) {
						Component new_ = firstTime_? comp_:
							(Component)comp_.clone();
						new_.setID(d_.ids[j]);
						d_.parent.addComponent(new_, !option_.keepRuntime);
						vresult_.addElement(new_);
						firstTime_ = false;
					}
				}
				else {
					// make port(s)
					for (int j=2; j<d_.ids.length; j++) {
						Port new_ = firstTime_? port_: (Port)port_.clone();
						String tmp_ = d_.ids[j];
						int index_ = tmp_.indexOf('@');
						String gid_ = index_ >= tmp_.length() -1?
									  "": tmp_.substring(index_+1);
						d_.parent.addPort(new_, gid_,
										tmp_.substring(0, index_));
						vresult_.addElement(new_);
						firstTime_ = false;
					}
				}
				continue;
			} // end if (d_.ids != null)
			
			// else we have no id info unless object_ has id info in it

			Object[] oo_ = d_.child;
			for (int j=0; j<oo_.length; j++) {
				if (oo_[j] instanceof Component) {
					// handling wrapper component
					if (object_ != null) {
						if (oo_[j] instanceof WrapperComponent) {
							WrapperComponent parent_ = (WrapperComponent)oo_[j];
							if (parent_.getObject() != null) {
								if (!option_.quiet)
									shell_.println("Warning: " + parent_
													+ " contains other"
													+ " wrapped object.");
								continue;
							}
							Component new_ = firstTime_? comp_:
								(Component)comp_.clone();
							parent_.setObject(
										((WrapperComponent)new_).getObject());
							vresult_.addElement(parent_.getObject());
							firstTime_ = false;
							continue;
						}
						else {
							// error no matter whether there's a trailing slash
							if (!option_.quiet) {
								if (d_.trailingSlash)
									shell_.println("Warning: component ID"
											+ " should be given to make a"
											+ " wrapper component under "
											+ oo_[j] + ".");
								else
									shell_.println("Warning: " + oo_[j]
											+ " is not a wrapper component.");
							}
							continue;
						}
					}
					
					// check no id error
					if (comp_ != null) {
						if (!d_.trailingSlash) {
							if (!option_.quiet) {
								// XX: write onto the component?
								shell_.println("Warning: " + oo_[j]
												+ " already exists.");
								shell_.println("Add a trailing slash maybe?");
							}
							continue;
						}
						else if (comp_.getID() == null) {
							if (!option_.quiet)
								shell_.println("Warning: component ID should"
										+ " be given to make a component"
										+ " under " + oo_[j] + ".");
							continue;
						}
					}
					if (port_ != null && port_.getID() == null) {
						if (!option_.quiet)
							shell_.println("Warning: port ID should be given"
									+ " to make a port under " + oo_[j] + ".");
						continue;
					}
					
					if (comp_ != null) {
						// make a component
						Component new_ = firstTime_? comp_:
								(Component)comp_.clone();
						Component parent_ = (Component)oo_[j];
						if (!parent_.containsComponent(new_.getID())) {
							parent_.addComponent(new_, !option_.keepRuntime);
							vresult_.addElement(new_);
							firstTime_ = false;
						}
						else {
							if (new_.getParent() != null) {
								if (!option_.quiet) {
									shell_.println("Warning: source '" + new_ 
											   + "' is not a standalone"
											   + " component.");
								}
								break;
							}
							if (!option_.quiet) {
								// error
								if (parent_.getComponent(new_.getID()) != null){
									shell_.println("Warning: '" + parent_ 
											   + "' already contains a child"
											   + " with ID '"
											   + new_.getID() + "'.");
								}
								else {
									shell_.println("Unknown error when adding "
											+ new_ + " to " + parent_ + ".");
								}
							}
						}
					}
					else {
						// make a port
						Port new_ = firstTime_? port_: (Port)port_.clone();
						Component parent_ = (Component)oo_[j];
						if (parent_.addPort(new_, new_.getGroupID(),
												new_.getID()) != null) {
							vresult_.addElement(new_);
							firstTime_ = false;
						}
						else {
							if (new_.getHost() != null) {
								if (!option_.quiet) {
									shell_.println("Warning: source '" + new_ 
											   + "' is not a standalone port.");
								}
								break;
							}
							if (!option_.quiet) {
								// error
								if (parent_.getPort(new_.getGroupID(),
													new_.getID()) != null) {
									shell_.println("Warning: '" + parent_ 
											   + "' already contains a port '"
											   + new_.getID() + "@"
											   + new_.getGroupID() + "'.");
								}
								else {
									shell_.println("Unknown error when adding "
											+ new_ + " to " + parent_ + ".");
								}
							}
						}
					}
				} // end if (oo_[j] is component)
				else {
					// error
					if (!option_.quiet)
						shell_.println("Warning: cannot make things under "
										+ oo_[j] + "!");
				}
			} // end loop i, index to v_
		}
		if (vresult_.size() == 0) {
			if (!option_.quiet && !calledByOtherCmds_)
				shell_.println("Nothing is created.");
			return null;
		}
		//if (vresult_.size() == 1) return vresult_.firstElement();
		Object[] result_ = new Object[vresult_.size()];
		vresult_.copyInto(result_);
		return result_;
	}
		
	public static String cat(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);

		Vector v_ = __common__(option_, pathss_, true/*sort*/, shell_);
		boolean found_ = false;
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			Object[] oo_ = d_.child;
			if (oo_ == null && d_.object == null) continue;
			if (oo_ == null) oo_ = new Object[]{d_.object};
			if (debug) java.lang.System.out.println("cat objects: "
							+ drcl.util.StringUtil.toString(oo_));
			for (int j=0; j<oo_.length; j++) {
				if (oo_[j] == null) continue;
				found_ = true;
				if (oo_[j] instanceof Component) {
					Component c_ = (Component)oo_[j];
					sb_.append("----- " + c_ + " -----:\n");
					if (option_.showConn)
						sb_.append(Util.showConnections(c_,
												option_.showNoConnPort,
												option_.showPortType,
												option_.showHidden,
												option_.showOutside));
					if (option_.showInfo)
						sb_.append(c_.info());
					sb_.append("\n");
				}
				else if (oo_[j] instanceof Port) {
					Port p_ = (Port)oo_[j];
					sb_.append("----- " + p_ + " -----:\n");
					sb_.append(p_.info() + "\n");
				}
				else {
					if (d_.parent instanceof Wrapper) {
						if (((Wrapper)d_.parent).getObject() == oo_[j]
							|| oo_[j] == Common.NULL_WRAPPED_OBJECT) {
							sb_.append("----- " + d_.parent + "@ -----:\n"
									   + getObjectInfo(oo_[j], "     ")
									   + "\n");
							continue;
						}
					}
					sb_.append("----- " + oo_[j].getClass() + " -----:\n"
							   + getObjectInfo(oo_[j], "     ") + "\n");
				}
			}
		}
		//if (!found_) shell_.println("Error: invalid path.");
		//else shell_.println(sb_.toString());
		//return ""; // must return something
		if (!found_) return "Error: invalid path.  Forgot '-a' maybe?\n";
		else  return sb_.toString();
	}
	
	// display detail info if is an array object
	static String getObjectInfo(Object o_, String prefix_)
	{
		if (!(o_ instanceof Object[]))
			return prefix_ + drcl.util.StringUtil.toString(o_) + "\n";
		Object[] oo_ = (Object[])o_;
		StringBuffer sb_ = new StringBuffer(prefix_ + o_.getClass().getName()
						+ " of length " + oo_.length + "\n");
		for (int i=0; i<oo_.length; i++) {
			sb_.append(getObjectInfo(oo_[i], prefix_ + "     "));
		}
		return sb_.toString();
	}
	
	
	public static Object[] rm(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);

		Vector v_ = __common__(option_, pathss_, false/*sort*/, shell_);
		
		Vector vresult_ = new Vector();
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			if (d_ == null || d_.child == null) continue;
			Object[] oo_ = d_.child;
			if (oo_ == null || oo_.length == 0) continue;
			//java.lang.System.out.println("rm check: " + d_);
			for (int j=0; j<oo_.length; j++) {
				if (oo_[j] instanceof Component) {
					if (d_.parent == null) {
						if (!option_.quiet)
							shell_.println("Warning: " + oo_[j]
										+ " has no parent to be removed from.");
						continue;
					}
					if (d_.parent.removeComponent((Component)oo_[j]) == null)
						continue;
				}
				else if (oo_[j] instanceof Port) {
					if (d_.parent == null) {
						if (!option_.quiet)
							shell_.println("Warning: " + oo_[j]
										+ " has no parent to be removed from.");
						continue;
					}
					if (d_.parent.removePort((Port)oo_[j]) == null) continue;
				}
				else if (d_.parent instanceof WrapperComponent) {
					if (((WrapperComponent)d_.parent).getObject() != oo_[j])
						continue;
					((WrapperComponent)d_.parent).setObject(null);
				}
				else continue;
				vresult_.addElement(oo_[j]);
			}
		}
		if (vresult_.size() == 0) return null;
		//if (vresult_.size() == 1) return vresult_.firstElement();
		Object[] result_ = new Object[vresult_.size()];
		vresult_.copyInto(result_);
		return result_;
	}
		
	public static Component cd(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);

		Vector v_ = __common__(option_, pathss_, true/*sort*/, shell_);
		
		Object found_ = null;
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			if (d_ == null || d_.child == null) continue;
			Object[] oo_ = d_.child;
			if (oo_ == null || oo_.length == 0) continue;
			if (found_ == null) found_ = oo_[0];
			for (int j=0; j<oo_.length; j++)
				if (oo_[j] instanceof Component) return (Component)oo_[j];
					// return the first found
		}
		if (found_ != null)
			shell_.println("Error: invalid path, " + found_
							+ " is not a component.");
		else
			shell_.println("Error, the path does not exist.");
		return null;
	}
	
	public static String ls(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, true/*expand*/);

		Vector v_ = __common__(option_, pathss_, true/*sort*/, shell_);
		
		StringBuffer sb_ = new StringBuffer();
		boolean pathValid_ = false;
		for (int i=0; i<v_.size(); i++) {
			Directory d_ = (Directory)v_.elementAt(i);
			if (d_.parent == null || d_.child == null) continue;
			//java.lang.System.out.println("Directory: " + d_);
			boolean multipathSpec_ = true;
			// determine if d_.path is a multipath spec
			// FIXME: not exact but works for most cases
			while (i == 0) {
				multipathSpec_ = v_.size() > 1;
				int lastSlash_ = d_.path.lastIndexOf('/');
				if (!multipathSpec_ && lastSlash_ >= 0) {
					int index_ = d_.path.indexOf('*');
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }

					index_ = d_.path.indexOf('?');
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }

					index_ = d_.path.indexOf('+');
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }

					index_ = d_.path.indexOf('-');
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }

					index_ = d_.path.indexOf('[');
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }

					index_ = d_.path.indexOf("...");
					if (index_ >= 0 && index_ < lastSlash_)
					{ multipathSpec_ = true; break; }
				}
				break;
			}
			if (multipathSpec_)
				sb_.append("----- " + d_.parent + " -----:\n");
			Object[] oo_ = d_.child;
			sb_.append(_ls(oo_, option_.list));
			pathValid_ = true;
			if (!option_.quiet) {
				if (!option_.list && oo_.length > 0) sb_.append("\n");
				sb_.append(oo_.length + " object" + (oo_.length > 1? "s": "")
						   + " listed.\n");
			}
			else sb_.append("\n");
		}
		if (sb_.length() == 0) {
			if (pathValid_) return "";
			else return "Error: invalid path.\n";
		}
		return sb_.toString();
	}
	
	static String _ls(Object[] oo_, boolean list_) 
	{
		if (oo_ == null) return "";
		
		// IDs
		StringBuffer[] ids_ = new StringBuffer[oo_.length];
		int max_ = ts;
		for (int i=0; i<oo_.length; i++) {
			if (oo_[i] instanceof Component) ids_[i] =
				new StringBuffer(drcl.util.StringUtil.addEscape(
									((Component)oo_[i]).id, "/\\ ") + " ");
			else if (oo_[i] instanceof Port)
				ids_[i] = new StringBuffer(drcl.util.StringUtil.addEscape(
							((Port)oo_[i]).id, "/\\ ") + "@"
							+ drcl.util.StringUtil.addEscape(
									((Port)oo_[i]).groupID, "/\\ ") + " ");
			else {// String, alias
				if (oo_[i] == Common.NULL_WRAPPED_OBJECT || oo_[i] == null)
					ids_[i] = new StringBuffer("<null> ");
				else
					ids_[i] = new StringBuffer(oo_[i] + " ");
			}
			int len_ = ids_[i].length();
			if (len_ > max_) max_ = len_;
		}
		
		int maxnt_ = (max_ + ts - 1) / ts;
		for (int i=0; i<oo_.length; i++) {
			int len_ = ids_[i].length();
			int nt_ =  maxnt_ - len_ / ts;
			for (int j=0; j<nt_; j++) ids_[i].append('\t');
		}
		
		// list only id's
		if (!list_) {
			int nc_ = sw / (maxnt_ * ts);
			int j=0;
			StringBuffer sb_ = new StringBuffer();
			drcl.util.StringUtil.sort(ids_, true/*accending order*/);
			for (int i=0; i<ids_.length; i++) {
				sb_.append(ids_[i]);
				if (++j == nc_) { sb_.append('\n'); j=0; }
			}
			return sb_.toString();
		}
		
		// column 2: name
		StringBuffer[] names_ = new StringBuffer[oo_.length];
		max_ = ts;
		for (int i=0; i<oo_.length; i++) {
			//if (oo_[i] instanceof Component)
			//	names_[i] = new StringBuffer(((Component)oo_[i]).getName()
			//	+ " ");
			//else {//if (oo_[i] instanceof Port) {
				/*
				String gid_ = ((Port)oo_[i]).groupID;
				if (gid_.equals(Component.PortGroup.DEFAULT_GROUP))
					names_[i] = new StringBuffer("<default> ");
				else names_[i] = new StringBuffer(gid_ + " ");
				*/
			//	names_[i] = new StringBuffer(
			//		drcl.util.StringUtil.finalPortionClassName(
			//		oo_[i].getClass()) + " ");
			//}
			//else { // String, alias
				//Component c_ = start_.getAlias((String)oo_[i]);
				//if (c_ == null) names_[i] = new StringBuffer("?(alias) ");
				//else names_[i] = new StringBuffer(c_.getName() + "(alias) ");
			//}
			if (oo_[i] == null || oo_[i] == Common.NULL_WRAPPED_OBJECT)
				names_[i] = new StringBuffer("<null> ");
			else
				names_[i] = new StringBuffer(oo_[i].getClass().getName() + " ");
			int len_ = names_[i].length();
			if (len_ > max_) max_ = len_;
		}
		
		maxnt_ = (max_ + ts - 1) / ts;
		for (int i=0; i<oo_.length; i++) {
			int len_ = names_[i].length();
			int nt_ = maxnt_ - len_ / ts;
			for (int j=0; j<nt_; j++) names_[i].append('\t');
		}
		
		// column 3: additional info
		for (int i=0; i<oo_.length; i++) {
			ids_[i].append(names_[i]);
			if (oo_[i] instanceof Component) {
				if (oo_[i] instanceof Wrapper) {
					Object o_ = ((Wrapper)oo_[i]).getObject();
					if (o_ instanceof Component || o_ instanceof Port)
						ids_[i].append("obj:" + o_ + ", ");
					else if (o_ == null)
						ids_[i].append("obj:<null>, ");
					else
						ids_[i].append("obj:" + o_ + ", ");
				}
				Component c_ = (Component)oo_[i];
				ids_[i].append("child:" + c_.getAllComponents().length
							   + ", port:" + c_.getAllPorts().length);
			}
			else if (oo_[i] instanceof Port) {
				Port p_ = (Port)oo_[i];
				ids_[i].append(p_.getTypeInString());
				ids_[i].append(p_.anyPeer()? ", connected": ", unconnected");
				if (p_.isShadow()) ids_[i].append(", shadow");
				if (p_.isExecutionBoundary()) ids_[i].append(", exe_boundary");
				if (p_.isDataTraceEnabled()) ids_[i].append(", data_trace_on");
				if (p_.isSendTraceEnabled()) ids_[i].append(", send_trace_on");
			}
			else { // String, alias
				//String path_ = start_.getAliasPath((String)oo_[i]);
				//if (path_ != null) ids_[i].append("--> " + path_);
				//else ids_[i].append("--> null");
				if (oo_[i] == null)
					ids_[i].append("<null>");
				else
					ids_[i].append(oo_[i].toString());
			}
		}
		
		// final result:
		StringBuffer sb_ = new StringBuffer();
		//drcl.util.StringUtil.sort(ids_, true/*accending order*/);
		for (int i=0; i<ids_.length; i++)
			sb_.append(ids_[i] + "\n");
		
		return sb_.toString();
	}
	
	private void __OTHER_COMMANDS___() {}

	public static void connect(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		String separator_ = "-to";
		String separator2_ = "-and";

		if (pathss_ == null || pathss_.length < 2) {
			if (!option_.quiet)
				shell_.println("Error: no source/dest is given.");
			return;
		}
		
		// look for "-to" or "-from" option and set up src and dest paths
		Paths[] srcpathss_ = null;
		Paths[] destpathss_ = null;
		for (int i=0; i<pathss_.length; i++) {
			String[] paths_ = pathss_[i].paths;
			if (paths_ == null || paths_.length == 0) continue;
			if (paths_[0].equals(separator_) || paths_[0].equals(separator2_)) {
				if (i == pathss_.length-1) {
					// error
					if (!option_.quiet)
						shell_.println("Error: no dest is given.");
					return;
				}
				srcpathss_ = new Paths[i];
				java.lang.System.arraycopy(pathss_, 0, srcpathss_, 0,
								srcpathss_.length);
				destpathss_ = new Paths[pathss_.length-i-1];
				java.lang.System.arraycopy(pathss_, i+1, destpathss_, 0,
								destpathss_.length);
				
				option_.unidirectional = paths_[0].equals(separator_);
				break;
			}
		}
		
		// error: no option "to" or "from"
		if (srcpathss_ == null) {
			if (pathss_.length > 2) {
				shell_.println("Error: incorrect syntax, '" + separator_
								+ "' or '" + separator2_ + "' is missing.");
				shell_.println(man("connect"));
				return;
			}
			else { // 2 paths: -and by default, 1st one is source, 2nd is dest
				srcpathss_ = new Paths[]{pathss_[0]};
				destpathss_ = new Paths[]{pathss_[1]};
			}
		}
		
		// get src/dest ports
		Object[] src_ = toRef(soption_ + "q", srcpathss_, false, shell_);
		if (src_ == null || src_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no source port was matched in "
								+ getWarningMsg(srcpathss_) + ".");
			return;
		}
		Object[] dest_ = toRef(soption_ + "q", destpathss_, false, shell_);
		if (dest_ == null || dest_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no destination port was matched in "
							+ getWarningMsg(destpathss_) + ".");
			return;
		}
		
		// connect
		Port[] srcports_ = new Port[src_.length];
		for (int i=0; i<src_.length; i++)
			if (src_[i] instanceof Port) srcports_[i] = (Port)src_[i];
			else srcports_[i] = ((Component)src_[i]).findAvailable();
		Port[] destports_ = new Port[dest_.length];
		for (int i=0; i<dest_.length; i++)
			if (dest_[i] instanceof Port) destports_[i] = (Port)dest_[i];
			else destports_[i] = ((Component)dest_[i]).findAvailable();
		if (option_.unidirectional || !option_.sharedWire) {
			for (int i=0; i<srcports_.length; i++)
				srcports_[i].connectTo(destports_);
			if (!option_.unidirectional)
				for (int i=0; i<destports_.length; i++)
					destports_[i].connectTo(srcports_);
		}
		else {
			for (int i=0; i<srcports_.length; i++) {
				srcports_[i].connect(destports_);
			}
		}
	}
	
	public static void disconnect(String soption_, Paths[] pathss_,
					Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		if (pathss_ == null || pathss_.length == 0) {
			if (!option_.quiet) shell_.println("Error: no port is given.");
			return;
		}
		
		Object[] oo_ = toRef(soption_ + "q", pathss_, false, shell_);
		if (oo_ == null || oo_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no port was matched in "
								+ getWarningMsg(pathss_) + ".");
			return;
		}
		
		// disconnect
		for (int i=0; i<oo_.length; i++)
			if (oo_[i] instanceof Port) ((Port)oo_[i]).disconnect();
			else ((Component)oo_[i]).disconnectAllPeers();
	}
	
	public static void attach(String cmd_, String soption_, Paths[] pathss_,
					Shell shell_)
	{
		try {
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		String separator_ = "-to";
		String separator2_ = "-with";

		if (pathss_ == null || pathss_.length < 2) {
			if (!option_.quiet)
				shell_.println("Error: no source/dest is given.");
			return;
		}
		
		// look for "-to" or "-with" option and set up src and dest paths
		Paths[] srcpathss_ = null;
		Paths[] destpathss_ = null;
		for (int i=0; i<pathss_.length; i++) {
			String[] paths_ = pathss_[i].paths;
			if (paths_ == null || paths_.length == 0) continue;
			if (paths_[0].equals(separator_) || paths_[0].equals(separator2_)) {
				if (i == pathss_.length-1) {
					// error
					if (!option_.quiet)
						shell_.println("Error: no dest is given.");
					return;
				}
				srcpathss_ = new Paths[i];
				java.lang.System.arraycopy(pathss_, 0, srcpathss_, 0, 
								srcpathss_.length);
				destpathss_ = new Paths[pathss_.length-i-1];
				java.lang.System.arraycopy(pathss_, i+1, destpathss_, 0,
								destpathss_.length);
				
				option_.input = !paths_[0].equals(separator_);
				break;
			}
		}
		
		// error: no option "-to" or "-with"
		if (srcpathss_ == null) {
			if (pathss_.length > 2) {
				shell_.println("Error: incorrect syntax, '" + separator_
								+ "' or '" + separator2_ + "' is missing.");
				shell_.println(man("attach"));
				return;
			}
			else { // 2 paths: -and by default, 1st one is source, 2nd is dest
				srcpathss_ = new Paths[]{pathss_[0]};
				destpathss_ = new Paths[]{pathss_[1]};
			}
		}
		
		// get src/dest ports
		Object[] src_ = toRef(soption_ + "q", srcpathss_, false, shell_);
		if (src_ == null || src_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no source port was matched in "
								+ getWarningMsg(srcpathss_) + ".");
			return;
		}
		Object[] dest_ = toRef(soption_ + "q", destpathss_, false, shell_);
		if (dest_ == null || dest_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no destination port was matched in "
								+ getWarningMsg(destpathss_) + ".");
			return;
		}
		
		// attach
		Port[] srcports_ = new Port[src_.length];
		for (int i=0; i<src_.length; i++)
			if (src_[i] instanceof Port) srcports_[i] = (Port)src_[i];
			else srcports_[i] = ((Component)src_[i]).findAvailable();
		Port[] destports_ = new Port[dest_.length];
		for (int i=0; i<dest_.length; i++)
			if (dest_[i] instanceof Port) destports_[i] = (Port)dest_[i];
			else destports_[i] = ((Component)dest_[i]).findAvailable();
		
		if (option_.input) {
			if (cmd_.equals("attach"))
				for (int i=0; i<destports_.length; i++)
					destports_[i].attachIn(srcports_);
			else
				for (int i=0; i<destports_.length; i++)
					destports_[i].detachIn(srcports_);
		}
		else {
			if (cmd_.equals("attach"))
				for (int i=0; i<destports_.length; i++)
					destports_[i].attachOut(srcports_);
			else
				for (int i=0; i<destports_.length; i++)
					destports_[i].detachOut(srcports_);
		}
		} catch (Exception e_) {
			e_.printStackTrace();
			Thread.currentThread().getThreadGroup().uncaughtException(
							Thread.currentThread(), e_);
		}
	}
	
	static final String PIPE_CONNECT = "-connect";
	static final String PIPE_BREAK = "-break";
	
	public static void pipe(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		if (pathss_ == null || pathss_.length == 0) {
			if (!option_.quiet) shell_.println("Error: no path is given.");
			return;
		}
		
		String[] paths_ = pathss_[0].paths;
		String cmd_ = PIPE_CONNECT;
		if (paths_ != null && paths_.length > 0
			&& (paths_[0].equals(PIPE_BREAK)
					|| paths_[0].equals(PIPE_CONNECT))) {
			cmd_ = paths_[0];
			Paths[] tmp_ = new Paths[pathss_.length-1];
			java.lang.System.arraycopy(pathss_, 1, tmp_, 0, tmp_.length);
			pathss_ = tmp_;
			if (pathss_.length == 0) {
				if (!option_.quiet) shell_.println("Error: no path is given.");
				return;
			}
		}
		
		Object[] oo_ = toRef(soption_, pathss_, true, shell_);
		if (oo_ == null || oo_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no component was matched in "
							+ getWarningMsg(pathss_) + ".");
			return;
		}

		Port[] ports_ = new Port[oo_.length];
		for (int i=0; i<oo_.length; i++)
			if (oo_[i] instanceof Port) ports_[i] = (Port)oo_[i];
			else ports_[i] = ((Component)oo_[i]).findAvailable();
		
		if (cmd_.equals(PIPE_CONNECT)) {
			if (option_.attach) { // ATTACH
				Port prev_ = ports_[0];
				for (int i=1; i<ports_.length; i++) {
					Port this_ = ports_[i];
					Wire wire_ = prev_.getOutWire();
					if (wire_ == null)
						wire_ = new Wire().joinOut(prev_);
					wire_.attach(this_);
					prev_ = this_;
				}
			}
			else { // CONNECT TO
				Port prev_ = ports_[0];
				for (int i=1; i<ports_.length; i++) {
					Port this_ = ports_[i];
					prev_.connectTo(this_);
					prev_ = this_;
				}
			}
		}
		else { // BREAK
			Port prev_ = ports_[0];
			for (int i=1; i<ports_.length; i++) {
				Port this_ = ports_[i];
				Wire wire_ = prev_.getOutWire();
				if (wire_ == null || !wire_.isAttachedToInBy(this_)) {
					if (!option_.quiet)
						shell_.print(prev_
									+ " is not connected to, or attached by, "
									+ this_ + "\n");
				}
				else {
					wire_.disconnect(this_);
					if (!wire_.anyPortExcept(prev_)) wire_.disconnect(prev_);
				}
				prev_ = this_;
			}
		}
	}
	
	public static void getflag(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		Object[] oo_ = toRef(soption_, pathss_, true, shell_);
		if (oo_ == null || oo_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no component was matched in " 
								+ getWarningMsg(pathss_) + ".");
			return;
		}
		
		boolean ever_ = false;
		StringBuffer all_ = new StringBuffer();
		for (int i=0; i<oo_.length; i++) {
			StringBuffer sb_ = new StringBuffer();
			StringBuffer sb2_ = new StringBuffer(); // for disabled flags
			Object o_ = oo_[i];
			if (oo_.length > 5) shell_.print(".");
			ever_ = true;
			Component c_ = o_ instanceof Component? (Component)o_: null;
			Port p_ = o_ instanceof Port? (Port)o_: null;
			if (c_ != null) {
				String[] debugLevelNames_ = c_.getDebugLevelNames();
				if (c_.isEnabled()) sb_.append("Enabled ");
				else if (option_.showAllDetails) sb2_.append("Enabled ");
				if (c_.isTraceEnabled()) sb_.append("Trace ");
				else if (option_.showAllDetails) sb2_.append("Trace ");
				if (c_.isGarbageDisplayEnabled())
					sb_.append("Garbage(Display) ");
				else if (c_.isGarbageEnabled())
					sb_.append("Garbage ");
				else if (option_.showAllDetails)
					sb2_.append("Garbage(Display) ");
				if (c_.isStarted()) {
					if (c_.isStopped()) sb_.append("Stopped ");
					else sb_.append("Started ");
				}
				if (c_.isDebugEnabled()) {
					sb_.append("Debug ");
					if (debugLevelNames_ != null
							&& debugLevelNames_.length > 0) {
						StringBuffer tmp_ = new StringBuffer();
						for (int j=0; j<debugLevelNames_.length; j++) {
							if (c_.isDebugEnabledAt(j)) {
								if (tmp_.length() == 0) 
									tmp_.append(debugLevelNames_[j]);
								else
									tmp_.append(" " + debugLevelNames_[j]);
							}
						}
						if (tmp_.length() > 0) sb_.append("(" + tmp_ + ") ");
					}
				}
				else if (option_.showAllDetails) sb2_.append("Debug ");
				if (c_.isErrorNoticeEnabled()) sb_.append("ErrorNotice ");
				else if (option_.showAllDetails) sb2_.append("ErrorNotice ");
				if (c_.isEventExportEnabled()) sb_.append("EventExport ");
				else if (option_.showAllDetails) sb2_.append("EventExport ");
				if (c_.isDirectOutputEnabled()) sb_.append("DirectOutput ");
				else if (option_.showAllDetails) sb2_.append("DirectOutput ");
			}
			else if (p_ != null) {
				if (p_.isExecutionBoundary()) sb_.append("ExecutionBoundary ");
				else if (option_.showAllDetails)
					sb2_.append("ExecutionBoundary ");
				if (p_.isEventExportEnabled()) sb_.append("EventExport ");
				else if (option_.showAllDetails) sb2_.append("EventExport ");
				if (p_.isDataTraceEnabled()) sb_.append("DataTrace ");
				else if (option_.showAllDetails) sb2_.append("DataTrace ");
				if (p_.isSendTraceEnabled()) sb_.append("SendTrace ");
				else if (option_.showAllDetails) sb2_.append("SendTrace ");
				if (p_.isShadow()) sb_.append("Shadow ");
				else if (option_.showAllDetails) sb2_.append("Shadow ");
			}
			
			if (option_.list) {
				if (c_ != null) {
					if (c_.getComponentFlag(
								Component.FLAG_COMPONENT_NOTIFICATION) != 0)
						sb_.append("ComponentNotification ");
					else if (option_.showAllDetails)
						sb2_.append("ComponentNotification ");
					if (c_.getComponentFlag(
								Component.FLAG_PORT_NOTIFICATION) != 0)
						sb_.append("PorNotification ");
					else if (option_.showAllDetails)
					sb2_.append("PorNotification ");
				}
				else if (p_ != null) {
					if (p_.isRemovable()) sb_.append("Removable ");
					else if (option_.showAllDetails) sb2_.append("Removable ");
				}

				_printAllFlagNames(o_, sb_, option_.showAllDetails? sb2_: null);
			}
			
			if (c_ != null) {
				String[] debugLevelNames_ = c_.getDebugLevelNames();
				if (debugLevelNames_ != null && debugLevelNames_.length > 0) {
					sb_.append(" (available debug levels:");
					for (int j=0; j<debugLevelNames_.length; j++)
						sb_.append(" " + debugLevelNames_[j]);
					sb_.append(") ");
				}

				// Trace: go thru all ports
				StringBuffer tmp_ = new StringBuffer();
				Port[] pp_ = c_.getAllPorts();
				for (int j=0; j<pp_.length; j++)
					if (pp_[j].isDataTraceEnabled())
						tmp_.append(Util.getPortID(pp_[j]) + " ");
				if (tmp_.length() > 0)
					sb_.append("DataTrace: " + tmp_);
				tmp_.delete(0, tmp_.length());
				for (int j=0; j<pp_.length; j++)
					if (pp_[j].isSendTraceEnabled())
						tmp_.append(Util.getPortID(pp_[j]) + " ");
				if (tmp_.length() > 0)
					sb_.append("SendTrace: " + tmp_);
			}
			if (sb_.length() == 0 && sb2_.length() == 0) continue;
			else {
				all_.append(o_ + ": ");
				if (sb_.length() == 0)
					all_.append("(disabled flags)" + sb2_ + "\n");
				else if (sb2_.length() == 0)
					all_.append(sb_ + "\n");
				else
					all_.append(sb_ + "\n\tDisabled flags: " + sb2_ + "\n");
			}
		}

		if (!ever_) {
			if (!option_.quiet)
				shell_.println((oo_.length > 5? "\n": "")
					+ "Warning: no component was matched in "
					+ getWarningMsg(pathss_) + ".");
		}
		else 
			shell_.print((oo_.length > 5? "\n": "") + all_.toString());
	}

	static Hashtable htComponentFlags;
	static Hashtable htPortFlags;

	// get names of all enabled flags
	static void _printAllFlagNames(Object c_, StringBuffer sb_,
					StringBuffer sb2_)
	{
		if (c_ instanceof Component) {
			if (htComponentFlags == null) {
				htComponentFlags = new Hashtable();
				htComponentFlags.put("Enabled", htComponentFlags);
				htComponentFlags.put("Garbage", htComponentFlags);
				htComponentFlags.put("GarbageDisplay", htComponentFlags);
				htComponentFlags.put("Trace", htComponentFlags);
				htComponentFlags.put("Debug", htComponentFlags);
				htComponentFlags.put("EventExport", htComponentFlags);
				htComponentFlags.put("ErrorNotice", htComponentFlags);
			}
		}
		else if (c_ instanceof Port) {
			if (htPortFlags == null) {
				htPortFlags = new Hashtable();
				htPortFlags.put("EventExport", htPortFlags);
				htPortFlags.put("SendTrace", htPortFlags);
				htPortFlags.put("DataTrace", htPortFlags);
			}
		}
		PropertyDescriptor[] pds_ =
			drcl.comp.Util.getAllPropertyDescriptors(c_);
		if (pds_ == null) return;
		Vector v_ = new Vector();
		for (int i=0; i<pds_.length; i++) {
			PropertyDescriptor pd_ = pds_[i];
			if (pd_.getWriteMethod() == null) continue;
			Method read_ = pd_.getReadMethod();
			String name_ = pd_.getDisplayName();
			if (read_ == null) continue;
			if (!name_.endsWith("Enabled")) continue;
			name_ = name_.substring(0,1).toUpperCase() + name_.substring(1);
			if (!read_.getName().equals("is" + name_)) continue;
			if (!read_.getReturnType().equals(boolean.class)) continue;
			if (!name_.equals("Enabled"))
				name_ = name_.substring(0, name_.length()-7);
			if (c_ instanceof Component && htComponentFlags.containsKey(name_)
				|| c_ instanceof Port && htPortFlags.containsKey(name_))
				continue;
			try {
				if (((Boolean)read_.invoke(c_, null)).booleanValue())
					sb_.append(name_ + " ");
				else if (sb2_ != null)
					sb2_.append(name_ + " ");
			}
			catch (Exception e_)
			{}
		}
	}

	/**
	 * @param overwrite_ true if to overwrite the current debug level setting.
	 */
	public static void setflag(String soption_, String[] flagName_,
					boolean enabled_, Object debugFlags_, boolean overwrite_,
					Paths[] pathss_, Shell shell_)
	{ _setflag(soption_, flagName_, enabled_, false, false, debugFlags_,
					overwrite_, pathss_, shell_); }
	
	/**
	 * @param overwrite_ true if to overwrite the current debug level setting.
	 */
	public static void setflag(String soption_, String[] flagName_,
					boolean enabled_, boolean recursive_, Object debugFlags_,
					boolean overwrite_, Paths[] pathss_, Shell shell_)
	{ _setflag(soption_, flagName_, enabled_, true, recursive_, debugFlags_,
					overwrite_, pathss_, shell_); }
	
	/**
	 * @param overwrite_ true if to overwrite the current debug level setting.
	 */
	static void _setflag(String soption_, String[] flagNames_, boolean enabled_,
					boolean recursiveEnabled_, boolean recursive_,
					Object debugFlags_, boolean overwrite_, Paths[] pathss_,
					Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		Object[] oo_ = toRef(soption_, pathss_, false, shell_);
		if (oo_ == null || oo_.length == 0) {
			if (!option_.quiet)
				shell_.println("Warning: no object was matched in "
								+ getWarningMsg(pathss_) + ".");
			return;
		}

		for (int j=0; j<flagNames_.length; j++) {
			String flagName_ = flagNames_[j].toLowerCase();
			boolean isDebug_ = flagName_.equals("debug");
			boolean isTrace_ = !isDebug_ && flagName_.equals("trace");
			if (isDebug_) flagName_ = "Debug";
			else if (isTrace_) flagName_ = "Trace";
			else if (flagName_.equals("errornotice")) flagName_ = "ErrorNotice";
			else if (flagName_.equals("eventexport")) flagName_ = "EventExport";
			else if (flagName_.equals("directoutput"))
				flagName_ = "DirectOutput";
			else if (flagName_.equals("garbagedisplay"))
				flagName_ = "GarbageDisplay";
			else if (flagName_.equals("component")) flagName_ = "";
			else flagName_ = flagName_.substring(0,1).toUpperCase()
					+ flagNames_[j].substring(1);
			
			Hashtable ht_ = new Hashtable();
			for (int i=0; i<oo_.length; i++) {
				Object o_ = oo_[i];
				if (isDebug_ && o_ instanceof Component) {
					Component c_ = (Component)o_;
					if (debugFlags_ == null || enabled_)
						c_.setDebugEnabled(enabled_, recursive_);

					// turn on/off all debug levels
					if (debugFlags_ == null) {
						if (!overwrite_)
							c_.setDebugEnabledAt(enabled_, -1);
					}
					else {
						if (overwrite_)
							c_.setDebugEnabledAt(!enabled_, -1);
								// turn on/off all levels first
						if (debugFlags_ instanceof int[])
							c_.setDebugEnabledAt(enabled_, (int[])debugFlags_);
						else
							c_.setDebugEnabledAt(enabled_, (String[])debugFlags_);
					}
					continue;
				}
				else if (isTrace_ && o_ instanceof Port) {
					Port p_ = (Port)o_;
					// turn on/off both traces
					if (debugFlags_ == null)
						p_.setTraceEnabled(enabled_);
					else {
						if (overwrite_)
							p_.setTraceEnabled(!enabled_);
								// turn on/off all traces first
						String[] traces_ = (String[])debugFlags_;
						for (int k=0; k<traces_.length; k++) {
							if (traces_[k].equalsIgnoreCase(
											Component.Trace_DATA))
								p_.setDataTraceEnabled(enabled_);
							else if (traces_[k].equalsIgnoreCase(
											Component.Trace_SEND))
								p_.setSendTraceEnabled(enabled_);
						}
					}
					continue;
				}
				Class class_ = o_.getClass();
				Class[] argClasses_ = null;
				Object[] arg_ = null;
				if (recursiveEnabled_) {
					argClasses_ = new Class[]{boolean.class, boolean.class};
					arg_ = new Object[]{new Boolean(enabled_),
							new Boolean(recursive_)};
				}
				else {
					argClasses_ = new Class[]{boolean.class};
					arg_ = new Object[]{new Boolean(enabled_)};
				}
				try {
					Method method_ = class_.getMethod("set" + flagName_
									+ "Enabled", argClasses_);
					method_.invoke(o_, arg_);
				}
				catch (Exception e_) {
					if (!option_.quiet && ht_.get(class_) == null) {
						shell_.println("Error: no '" + flagName_
									+ "' flag for " + class_.getName() + ".");
						ht_.put(class_, class_);
					}
				}
			} // end loop i, object index
		} // end loop j, flagName index
	}
	
	// displays candidates by "ls" and returns the common prefix
	public static String autocomplete(String soption_, Paths[] pathss_,
					Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_,
						false/*dont expand*/);

		// set up paths
		if (pathss_ == null || pathss_.length == 0) return "";
		Paths p_ = pathss_[pathss_.length-1];
		if (p_.paths == null || p_.paths.length == 0) return "";
		String last_ = p_.paths[p_.paths.length-1];
		p_.paths = new String[] {last_ + "*"};
		pathss_ = new Paths[]{p_};
		
		Object[] oo_ = toRef(soption_+'q', pathss_, true, shell_);
		if (oo_ == null || oo_.length == 0) {
			shell_.println("no object was matched.");
			return "";
		}

		String[] ids_ = new String[oo_.length];
		for (int i=0; i<oo_.length; i++) {
			Object o_ = oo_[i];
			String id_ = null;
			if (o_ instanceof Component) id_ = ((Component)o_).getID();
			else if (o_ instanceof Port) id_ = Util.getPortID((Port)o_);
			else continue;
			ids_[i] = id_;
		}
		
		String result_ = drcl.util.StringUtil.findCommonPrefix(ids_);
		//java.lang.System.out.println("common prefix: " + result_
		//	+ " for " + drcl.util.StringUtil.toString(ids_));
		if (result_ == null) {
			shell_.println("no object was matched.");
			return "";
		}
		else {
			int index_ = last_.lastIndexOf('/');
			String path_ = index_ >= 0? last_.substring(index_+1): last_;
			result_ = result_.substring(path_.length());
			shell_.print(ls(soption_+'q', pathss_, shell_));
		}
		
		return result_;
	}
	
	// explores the neighboring information
	public static void explore(String soption_, Paths[] pathss_, Shell shell_)
	{
		try {

		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		if (pathss_ == null || pathss_.length < 1) {
			shell_.println("Error: no staring component is given.");
			return;
		}
		
		// exploration starting component
		Component[] startingPoint_ = null;
		Object[] tmp_ = toRef(soption_+"q", new Paths[]{pathss_[0]},
						true/*sort*/, shell_);
		if (tmp_ != null) {
			Vector vtmp_ = new Vector();
			for (int i=0; i<tmp_.length; i++) {
				Object o_ = tmp_[i];
				if (o_ instanceof Component)
					vtmp_.addElement(o_);
			}
			if (vtmp_.size() > 0) {
				startingPoint_ = new Component[vtmp_.size()];
				vtmp_.copyInto(startingPoint_);
			}
		}
		if (startingPoint_ == null) {
			if (!option_.quiet)
				shell_.println("Error: can't find the specified starting"
								+ " component.");
			return;
		}
		
		// look for "-transparent", "-excluded" and "stop" option
		int len_ = pathss_.length;
		int j=len_, k=len_, m = len_; // mark where transparent, excluded and stop begin
		for (int i=1; i<len_; i++) {
			String[] paths_ = pathss_[i].paths;
			if (paths_ == null || paths_.length == 0) continue;
			if (paths_[0].equals("-transparent")) {
				j = i;		if (k < len_ && m < len_) break;
			}
			else if (paths_[0].equals("-excluded")) {
				k = i;		if (j < len_ && m < len_) break;
			}
			else if (paths_[0].equals("-stop")) {
				if (!option_.flat) option_.flat = true;
				m = i;		if (j < len_ && k < len_) break;
			}
		}
		
		// make transparent and exclude paths
		Component[] transparent_ = null;
		Component[] excluded_ = null;
		Component[] stop_ = null;
		if (j<len_) {
			int end_ = k>j? k: len_; // exclusive
			if (m > j && m < end_) end_ = m;
			Paths[] tmppathss_ = new Paths[end_-j];
			java.lang.System.arraycopy(pathss_, j, tmppathss_, 0, end_-j);
			tmp_ = toRef(soption_, tmppathss_, false/*sort*/, shell_);
			LinkedList v_ = new LinkedList();
			if (tmp_ != null) {
				for (int i=0; i<tmp_.length; i++)
					if (tmp_[i] instanceof Component) v_.add(tmp_[i]);
			}
			transparent_ = (Component[])v_.toArray(new Component[v_.size()]);
		}
		if (k<len_) {
			int end_ = j>k? j: len_; // exclusive
			if (m > k && m < end_) end_ = m;
			Paths[] tmppathss_ = new Paths[end_-k];
			java.lang.System.arraycopy(pathss_, k, tmppathss_, 0, end_-k);
			tmp_ = toRef(soption_, tmppathss_, false/*sort*/, shell_);
			LinkedList v_ = new LinkedList();
			if (tmp_ != null) {
				for (int i=0; i<tmp_.length; i++)
					if (tmp_[i] instanceof Component) v_.add(tmp_[i]);
			}
			excluded_ = (Component[])v_.toArray(new Component[v_.size()]);
		}
		if (m<len_) {
			int end_ = j>m? j: len_; // exclusive
			if (k > m && k < end_) end_ = k;
			Paths[] tmppathss_ = new Paths[end_-m];
			java.lang.System.arraycopy(pathss_, m, tmppathss_, 0, end_-m);
			tmp_ = toRef(soption_, tmppathss_, false/*sort*/, shell_);
			LinkedList v_ = new LinkedList();
			if (tmp_ != null) {
				for (int i=0; i<tmp_.length; i++)
					if (tmp_[i] instanceof Component) v_.add(tmp_[i]);
			}
			stop_ = (Component[])v_.toArray(new Component[v_.size()]);
		}
		
		// call Util.explore()
		Hashtable ht_ = null;
		if (option_.flat) {
			ht_ = drcl.comp.Util.exploreFlat(startingPoint_[0], transparent_,
							excluded_, stop_, !option_.quiet);
		}
		else {
			if (startingPoint_.length == 1)
				ht_ = drcl.comp.Util.explore(startingPoint_[0], transparent_,
							excluded_, !option_.quiet);
			else
				ht_ = drcl.comp.Util.explore(startingPoint_, transparent_,
							excluded_, !option_.quiet);
		}
		StringBuffer sb_ = new StringBuffer();
		Component[] cc_ = new Component[ht_.size()];
		j=0;
		for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
			cc_[j++] = (Component)e_.nextElement();
		}
		drcl.util.StringUtil.sort(cc_, true/*accending order*/);
		sb_.append("-----------------\n");
		for (j=0; j<cc_.length; j++) {
			Component c_ = cc_[j];
			Util.Link[] ll_ = (Util.Link[])ht_.get(c_);
			sb_.append(c_ + ":\n");
			for (int i=0; i<ll_.length; i++)
				sb_.append("     neighbor " + (i+1) + ": " + ll_[i] + "\n");
		}
		
		if (sb_.length() == 0) {
			shell_.println(startingPoint_ + " has no neighbor.");
		}
		else
			shell_.println(sb_.toString());

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// verifies connections
	public static void verify(String soption_, Paths[] pathss_, Shell shell_)
	{
		CommandOption option_ = new CommandOption(soption_, false/*expand*/);
		if (pathss_ == null || pathss_.length < 1) {
			shell_.println("Error: no component is given to verify.");
			return;
		}
		
		Object[] tmp_ = toRef(soption_+"q", pathss_, true/*sort*/, shell_);
		Vector v_ = new Vector();
		if (tmp_ != null) {
			for (int i=0; i<tmp_.length; i++) {
				Object o_ = tmp_[i];
				if (o_ instanceof Component)
					v_.addElement(o_);
			}
		}
		Component[] comp_ = new Component[v_.size()];
		v_.copyInto(comp_);
		
		if (comp_.length == 0) {
			shell_.println("Warning: no component was matched in "
							+ getWarningMsg(pathss_) + ".");
			return;
		}
		
		StringBuffer event_ = new StringBuffer();
		StringBuffer error_ = new StringBuffer();
		StringBuffer warning_ = new StringBuffer();
		int nevent_ = 0, nerror_ = 0, nwarning_ = 0;
		
		Hashtable ht_ = drcl.comp.Util.verify(comp_, option_.recursive);
		for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
			Port p_ = (Port)e_.nextElement();
			if (p_.getGroupID() == Component.PortGroup_EVENT) {
				event_.append(p_ + " " + ht_.get(p_) + "\n");
				nevent_ ++;
			}
			// XXX: no error so far
			else {
				warning_.append("   " + p_ + " " + ht_.get(p_) + "\n");
				nwarning_ ++;
			}
		}
		
		if (event_.length() + warning_.length() + error_.length() == 0) {
			shell_.println("Verification is done.  No error was found.");
		}
		else {
			shell_.println("");
			if (option_.error && error_.length() > 0)
				shell_.print("Error:\n" + error_);
			if (option_.warning && warning_.length() > 0)
				shell_.print("Unconnected (nonevent) port:\n" + warning_);
			if (option_.event && event_.length() > 0)
				shell_.print("Unconnected event port:\n" + event_);
			shell_.println("Summary:");

			if (option_.error)
				shell_.println("   " + nerror_
								+ (nerror_ > 1? " errors": " error"));
			if (option_.warning)
				shell_.println("   " + nwarning_ + " unconnected"
								+ (nwarning_ > 1? " ports": " port"));
			if (option_.event)
				shell_.println("   " + nevent_ + " unconnected event"
								+ (nevent_ > 1? " ports": " port"));
		}
	}
	
	public static String getAllCommands()
	{
		return "UNIX-like commands: ! !!! cat cd cp ls man mkdir mv pwd rm.\n"
			+ "attach_simulator     attach a simulation runtime to components.\n"
			+ "connect/disconnect   connect/disconnect components/ports.\n"
			+ "attach/detach        attach/detach an instrument port.\n"
			+ "pipe                 connect/break a series of components/ports in a pipe manner.\n"
			+ "watch                add/remove a system watcher.\n"
			+ "setflag/getflag      set/display component flags.\n"
			+ "run/stop/reumse      start/stop/resume components.\n"
			+ "reset                reset components.\n"
			+ "reboot               reset components and runtime.\n"
			+ "rt                   access runtime.\n"
			+ "inject               inject data to ports.\n"
			+ "set_default_class    set the default class for 'mkdir'.\n"
			+ "whats_default_class  return the name of the default class for 'mkdir'.\n"
			+ "term                 create a new terminal.\n"
			+ "exit                 exit the terminal.\n"
			+ "quit                 quit the program.\n"
			+ "\nExecution control:\n"
			+ "   script            schedule a script.\n"
			+ "   wait_until        block the terminal until a condition is met.\n"
			+ "\nTopology construction:\n"
			+ "   explore           explore the component topology from a component.\n"
			+ "\nVerification:\n"
			+ "   verify            verify connections within a container component.\n"
			+ "\nOther utility commands:\n"
			+ "   grep              search a pattern in a big chunk of text and prints the result.\n"
			+ "   subtext           extract lines of text from text.\n"
			+ "   nlines            counts the number of lines in a chunk of text.\n"
			+ "   _to_string_array  convert a Tcl list to a Java array of String.\n"
			+ "\nFor more help on a particular command, use man <command>.\n"; 
	}
	
	public static String man(String cmd_)
	{
		if ("!".equals(cmd_)) {
			return "! ?-apq? <path>|<paths> ?<arg0> <arg1> ...?\n"
				+ "     Convert the path to the reference to the Java object\n"
				+ "     and then invoke the method specified in the argument list.\n";
		} else if ("!!!".equals(cmd_)) {
			return  "!!! <obj_ref>\n"
				+ "     Cast the object reference to its most specific class.\n";
		} else if ("attach".equals(cmd_)) {
			return "attach ?-c? <src path>... -to|-with <dest path>...\n"
				+ "     Attaches components/ports in <src path>s to/with components/ports in <dest path>.\n"
				+ "     If components are specified in the paths, the command finds available ports\n"
				+ "     in the components to do the attachment.\n"
				+ "Options:\n"
				+ "     -c    Creates port if not existent.\n"
				+ "     -to   Attached to listen to the outputs of the destination ports.\n"
				+ "     -with Attached to listen to whatever the destination ports listen to.\n"
				+ "See Also:\n"
				+ "     detach, connect.\n";
		} else if ("attach_simulator".equals(cmd_)) {
			return "attach_simulator ?-aq? ?<#threads allowed> | event? <path1> ?<path2>...?\n"
				+ "     Attaches a default simulation runtime to components specified in the path(s).\n"
				+ "     If # of threads allowed is not specified, the runtime creates as many threads\n"
				+ "     as possible if needed.\n"
				+ "Options:\n"
				+ "     -a      Match all hidden components.\n"
				+ "     -q      Suppress verbose warnings such as \"invalid path\".\n"
				+ "  event      Use sequential event simulation runtime rather than the real-time one.\n";
		} else if ("cat".equals(cmd_)) {
			return "cat ?-acdhnpqt? ?<path>\n"
			    + "cat <java_object_reference>\n"
				+ "     Print out the information of the component or object.\n"
				+ "     -a      Match all hidden components (and ports if -p is specified).\n"
				+ "     -c      Display only the connections for a component.\n"
				+ "     -d      Display both the internal states and the connections in details\n"
				+ "             for a component.\n"
				+ "     -h      Display hidden ports when displaying connections (-c).\n"
				+ "     -n      Display ports that do not connect to other ports when displaying\n"
				+ "             connections (-c).\n"
				+ "     -p      Match ports as well.\n"
				+ "     -q      Suppress verbose warnings such as \"invalid path\".\n"
				+ "     -t      Display the port type when displaying connections (-c).\n"
				+ "Note:\n"
				+ "     The second form is particularly useful in examining the contents of a Java array.\n";
		} else if ("cd".equals(cmd_)) {
			return "cd ?-aq? ?<path?>\n"
				+ "     Change the current directory.\n"
				+ "     If multiple components are matched to the path, the first matched component\n"
				+ "     becomes the current working directory. No specific rule is defined in\n"
				+ "     determining the first match.\n"
				+ "Options:\n"
				+ "     -a      Match all hidden components.\n"
				+ "     -q      Suppress verbose warnings such as \"invalid path\".\n";
		} else if ("connect".equals(cmd_)) {
			return "connect ?-acpqs? <src path>... -to|-and <dest path>...\n"
				+ "     Connects components/ports in <src path>s to components/ports in <dest path>.\n"
				+ "     If components are specified in the paths, the command finds available ports\n"
				+ "     in the components in order to make the connections.\n\n"
				+ "     A bidirectional connection either consists of two unidirectional connections\n"
				+ "     or one shared connection. By default (no -s option), the command creates two\n"
				+ "     unidirectional connections in response to the -and option.\n\n"
				+ "     The original connections (if exist) of the ports will be joined all together\n"
				+ "     on the same wire after connect.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -c    Create ports if the ports do not exist.\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "     -s	  Set up one shared bidirectional connection (with -and).\n"
				+ "     -and  Sets up bidirectional connection.\n"
				+ "     -to   Sets up unidirectional connection (destination ports are set as IN type).\n"
				+ "See Also:\n"
				+ "     disconnect.\n";
		} else if ("cp".equals(cmd_)) {
			return "cp ?-apq? <source path>... <dest path>\n"
				+ "cp ?-apq? <source path>... -d <dest path>...\n"
				+ "     Copy components/ports.  This command returns the references of the object(s)\n"
				+ "     being copied.\n";
		} else if ("detach".equals(cmd_)) {
			return "detach ?-apq? <instrument_port_path>... -to|-with <destination_path>...\n"
				+ "     Detaches an instrument port.\n"
				+ "Options:\n"
				+ "     -a     Match all hidden components (and ports if -p is specified).\n"
				+ "     -p     Match ports as well.\n"
				+ "     -q     Suppress verbose warnings such as \"invalid path\".\n"
				+ "     -to    Remove the instrument port from listening to what comes out of the\n"
				+ "            specified ports.\n"
				+ "     -with  Remove the instrument port from listening to what the specified ports\n"
				+ "            receive.\n"
				+ "See Also:\n"
				+ "     attach.\n";
		} else if ("disconnect".equals(cmd_)) {
			return "disconnect ?-apq? <path>...\n"
				+ "     Disconnects components/ports specified in <path>s.  If components are specified\n"
				+ "     in the path(s), all the ports of the components are disconnected from the rest\n"
				+ "     of the system.\n"
				+ "Options:\n"
				+ "     -a     Match all hidden components (and ports if -p is specified).\n"
				+ "     -p     Match ports as well.\n"
				+ "     -q     Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     connect.\n";
		} else if ("exit".equals(cmd_)) {
			return "exit\n"
				+ "     Close the current terminal.  If the current terminal is the last one, then\n"
				+ "     the command exits Java.\n";
		} else if ("explore".equals(cmd_)) {
			return "explore ?-aq? <path of starting component> ?-transparent <path>...? ?-excluded <path>...? ?-stop <path>...?\n"
				+ "     Explore and print the component topology.\n\n"
				+ "     This command explores the component topology by tracing from the starting\n"
				+ "     component, all the way to the root component. During the exploration process,\n"
				+ "     if the -f flag is not on (default), the command does not trace into a containing\n"
				+ "     component. As a result, the components that can be traced from the starting\n"
				+ "     component are at the same (component hierarchy) layer as, or higher layer than,\n"
				+ "     the starting component.\n\n"
				+ "     One may specify components (in the -transparent option) as transparent during\n"
				+ "     the course of exploration. Also components may be (in the -excluded option)\n"
				+ "     excluded from the topology exploration.\n"
				+ "Options:\n"
				+ "     -a                  Match all hidden components.\n"
				+ "     -excluded <...>     Exclude the components from being explored.\n"
				+ "     -f                  Discover the \"flat\" topology of all \"leaf\" components reached.\n"
				+ "     -q                  Suppress verbose warnings such as \"invalid path\".\n"
				+ "     -stop <...>         Implies the \"-f\" option.  It stops exploring further inside the \"stop\" components.\n"
				+ "     -transparent <...>  Make the components transparent in the exploration.\n";
		} else if ("getflag".equals(cmd_)) {
			return "getflag ?-adlpq? <path>...\n"
				+ "     Displays the flags of the given components.\n"
				+ "     By default, this command only examines the following standard component flags\n"
				+ "     and display enabled ones: Trace, Garbage, GarbageDisplay, Debug, ErrorNotice\n"
				+ "     and EventExport. The option \"-l\" makes the command examine all the programmable\n"
				+ "     flags.  A programmable flag is found by discovering the pair of the \"read\" method,\n"
				+ "     is<flag>Enabled(), and the \"write\" method, set<flag>Enabled(boolean), in the\n"
				+ "     component (or more precisely, in the component's JavaBeans properties).\n"
				+ "     With the \"-d\" option, the command displays not only the enabled flags but also\n"
				+ "     the disabled ones, for reference purpose.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -d    Displays disabled flags as well.\n"
				+ "     -l    Displays all the programmable flags in addition to the standard ones.\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     setflag.\n";
		} else if ("grep".equals(cmd_)) {
			return "grep <pattern> <text>\n"
				+ "     Searches a pattern in a big chunk of text and prints the result.\n";
		} else if ("inject".equals(cmd_)) {
			return "inject ?-apq? <data> <port path>...\n"
				+ "     Inject data to port(s).\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n";
		} else if ("ls".equals(cmd_)) {
			return "ls ?-apql? ?<path>...?\n"
				+ "     List the child components/ports in the paths specified.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -l    List in the long format (more detailed information).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n";
		} else if ("mkdir".equals(cmd_)) {
			return "mkdir ?-aq? <Java class name> <path>...\n"
				+ "mkdir ?-aq? <Java object ref> <path>...\n"
				+ "mkdir ?-aq? <port path> ?<port path>...?\n"
				+ "mkdir ?-aq? <component path>\n"
				+ "     Create components/ports.  This command returns the reference of the\n"
				+ "     object(s) being created.  If the class name or the Java object is not\n"
				+ "     a component/port, a wrapper component (drcl.comp.WrapperComponent) is\n"
				+ "     created to encapsulate the object.\n\n"
				+ "     The fourth form uses the default class to create component(s) at the\n"
				+ "     specified path.  It is equivalent to\n\n"
				+ "          mkdir ?-aq? <default_class> <component_path>\n\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     set_default_class, whats_default_class\n";
		} else if ("mv".equals(cmd_)) {
			return "mv ?-apq? <source path>... <dest path>\n"
				+ "mv ?-apq? <source path>... -d <dest path>...\n"
				+ "     Move components/ports.  This command returns the reference of the\n"
				+ "     object(s) being moved.  Note that the command does not change the\n"
				+ "     connections of the moved components/ports.\n\n"
				+ "     The second form of this command is the same as \"cp\" except that the\n"
				+ "     source components are removed from the original directories and moved\n"
				+ "     to the first directory component specified in the destination path(s).\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     disconnect\n";
		} else if ("nlines".equals(cmd_)) {
			return "nlines <text>\n"
				+ "     Count and print the number of lines in a chunk of text.\n";
		} else if ("pipe".equals(cmd_)) {
			return "pipe ?-a? ?-break|-connect? <path1> ?<path2>...?\n"
				+ "     Connect/break a series of components/ports in a pipe manner.\n"
				+ "Options:\n"
				+ "     -a         Form the pipe by \"attach\" instead of \"connect\".\n"
				+ "     -connect   Connect a pipe, the default action if none is specified.\n"
				+ "     -break     Break a pipe.\n"
				+ "See Also:\n"
				+ "     attach, connect.\n";
		} else if ("pwd".equals(cmd_)) {
			return "pwd\n"
				+ "     Print the current working path.\n";
		} else if ("reboot".equals(cmd_)) {
			return "reboot ?-apq? <path1> ?<path2>...?\n"
				+ "     Reset components and the runtime(s) behind components.  The command\n"
				+ "     calls drcl.comp.Component.reboot() for all the components specified\n"
				+ "     in the path(s).\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     reset\n";
		} else if ("reset".equals(cmd_)) {
			return "reset ?-apq? <path1> ?<path2>...?\n"
				+ "     Reset components.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     reboot\n";
		} else if ("resume".equals(cmd_)) {
			return "resume ?-apq? <path1> ?<path2>...?\n"
				+ "     Resume (stopped) components and all the components within.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     run, stop\n";
		} else if ("rm".equals(cmd_)) {
			return "rm ?-apq? <path1> ?<path2>...?\n"
				+ "     Remove components/ports.  This command returns the object(s) being removed.\n"
				+ "     The removed components/ports are not disconnected.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     disconnect\n";
		} else if ("rt".equals(cmd_)) {
			return "rt ?-apq? <path> ?<method> ?<arg>...??\n"
				+ "     Access runtime and its information.  The command retrieves the runtime\n"
				+ "     behind the component specified, and does a method call on the runtime if\n"
				+ "     a method is specified.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n";
		} else if ("run".equals(cmd_)) {
			return "run ?-apq? <path1> ?<path2>...?\n"
				+ "     Start (active) components and all the components within.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     stop, resume\n";
		} else if ("script".equals(cmd_)) {
			return "script <script> ?-at <time> ?later?? ?-period <period>? ?-to <runtime instance>?\n"
				+ "     ?-shell <shell instance>?\n"
				+ "     Execute a script at a future time instant (recursively).\n"
				+ "Options:\n"
				+ "     -at <...>       The time instant to execute this script the first time\n"
				+ "        ?later?      (default: 0.0).  The time instant is relative if \"later\" is\n"
				+ "                     present.\n"
				+ "     -period <...>   Execute this script periodically (default: 0.0, execute only one time).\n"
				+ "     -shell <...>    Associate execution of this script to the shell (default:\n"
				+ "                     the current shell instance).\n"
				+ "     -on <...>       Associate execution of this script to the runtime instance\n"
				+ "                     (default: the default runtime instance).\n";
		} else if ("setflag".equals(cmd_)) {
			return "setflag ?-apq? <flag>... <enabled> ?-at <list_of_debug_levels>?\n"
				+ "     ?<recursively>? <path1> ?<path2>...?\n"
				+ "     Turn on/off the given flags on the components/objects.\n\n"
				+ "     The command enables/disables the given flag(s) on the matched components/objects.\n"
				+ "     Six flags are defined for drcl.comp.Component: Trace, Garbage, GarbageDisplay, Debug,\n"
				+ "     ErrorNotice and EventExport. Additional flags that are defined in sub-components or\n"
				+ "     objects may be enabled/disabled by this command as well.\n\n"
				+ "     A component may define debug levels to further classify debug messages. Each level\n"
				+ "     can be enabled/disabled individually with the \"-at\" option. Enabling the debug flag\n"
				+ "     of a component without specifying debug levels enables the debug flag and all the\n"
				+ "     debug levels if defined. Available debug levels can be shown with the getflag\n"
				+ "     command.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -at   Enable/disable debug levels.\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     getflag.\n";
		} else if ("set_default_class".equals(cmd_)) {
			return "set_default_class ?class_name?\n"
				+ "     Set the name of the default class for 'mkdir' a component without the class\n"
				+ "     or object argument.  If class_name is not specified, 'drcl.comp.Component'\n"
				+ "     is used instead.\n"
				+ "See Also:\n"
				+ "     mkdir, whats_default_class.\n";
		} else if ("stop".equals(cmd_)) {
			return "stop ?-apq? <path1> ?<path2>...?\n"
				+ "     Stop (active) components and all the components within.\n"
				+ "Options:\n"
				+ "     -a    Match all hidden components (and ports if -p is specified).\n"
				+ "     -p    Match ports as well.\n"
				+ "     -q    Suppress verbose warnings such as \"invalid path\".\n"
				+ "See Also:\n"
				+ "     run, resume\n";
		} else if ("subtext".equals(cmd_)) {
			return "subtext <text> <start line #> <end line #>\n"
				+ "     Extracts lines of text from text.\n";
		} else if ("term".equals(cmd_)) {
			return "term title ?-t <term_class>? ?-s <shell_class>|<shell_object>? ?<init script>?\n"
				+ "     Open a new terminal.\n"
				+ "     Default <term_class> is drcl.ruv.Dterm.\n"
				+ "     Default <shell_class> is drcl.ruv.ShellTcl.\n";
		} else if ("wait_until".equals(cmd_)) {
			return "wait_until <condition>\n"
				+ "     Blocks the terminal until the specified condition is met.\n"
				+ "     <condition> is a script that returns a boolean value.\n"
				+ "     This command can only be used as a standalone command. The result will not\n"
				+ "     be predictable if it is used in other commands such as \"if\"\n";
		} else if ("watch".equals(cmd_)) {
			return "watch ?-acpq? ?-label <label>? -add|-remove <path>...\n"
				+ "     Add/remove a system watcher at the system monitor /.system/monitor.\n"
				+ "     The events observed by the watcher are output to whichever components\n"
				+ "     that are connected to the system monitor.\n\n"
				+ "     The command attaches a port of the RUV system watcher component to the ports\n"
				+ "     specified in the destination path(s). Then the watcher port starts to listen\n"
				+ "     to what comes out of the specified ports and prints whatever it receives on\n"
				+ "     the terminal. One may add and remove multiple watchers in a command. For example,\n\n"
				+ "         watch -label L1 -add x@p1 y@p2 -remove z@p3 -label none -add w@p4 u@p5\n\n"
				+ "     adds x@p1 and y@p2 to the L1 watcher and removes z@p3 from the L1 watcher,\n"
				+ "     and adds w@p4 and u@p5 to the default watcher (with no label).\n"
				+ "Options:\n"
				+ "     -a      Match all hidden components (and ports if -p is specified).\n"
				+ "     -c      Create ports if the specified ports do not exist.\n"
				+ "     -label  The watcher label\n"
				+ "     -p      Match ports as well.\n"
				+ "     -q      Suppress verbose warnings such as \"invalid path\".\n";
		} else if ("whats_default_class".equals(cmd_)) {
			return "whats_default_class\n"
				+ "     Prints the name of the default class for 'mkdir'.\n"
				+ "See Also:\n"
				+ "     mkdir, set_default_class\n";
		} else {
			return "Unrecognized command.\n";
		}
	}
}
