// @(#)System.java   1/2004
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

import java.util.*;
import drcl.comp.*;
import drcl.comp.tool.ComponentTester;

/**
 * The entry class of the DRCL RUntime Virtual (RUV) system.
 * It will set up a terminal along with a shell and several system "directories":
 * <ul>
 * <li>/.system: The <code>System</code> object itself.
 * <li>/.system/monitor: the system monitor ({@link SystemMonitor}).
 * <li>/.term: stores all the terminals and shells.
 * </ul>
 * One may specify the shell class, the terminal class and/or the initial script to execute.
 * By default, <code>Dterm</code> and <code>ShellTcl</code> will be used to start the system.
 * The complete usage is as follows:<br>
 * <pre>drcl.ruv.System ?-auenh? ?-s &ltshell_class&gt? ?-t &ltterminal_class&gt? ?&ltinit_script&gt?
 *      -a	use the RUV system as part of other programs (e.g. GUI), where "exit" or
 *          "quit" only terminates the terminal instead of the Java program.
 *      -u	uninteractive execution; no terminal is created.
 *      -e	end the Java program when there is no activity in the system; used with the "-u" option.
 *      -h	display help.
 *      -n	no terminal display.
 * </pre>
 */
public class System extends Component
{
	// dedicated ACARuntime for shells and terms
	static drcl.comp.ACARuntime RUNTIME = new ARuntime("ruv");
	
	static System system = null;
	static Shell backupShell = null; // for diagnosis when something goes wrong with user's shell
	boolean exit2quit = true; // command "exit" to quit?
	Component termDir = null; // directory that stores all terminals and shells
	SystemMonitor systemMonitor = null; // system monitor
	static Hashtable htRuntime; // monitoring runtimes for -e option
	
	/** Creates a new terminal in the RUV system. */
	public static void newTerminal()
	{	if (system != null) system._newTerminal();	}
	
	/** Creates a new terminal in the RUV system and executes the script in the terminal. */
	public static void newTerminal(String initScript_, String[] args_)
	{	if (system != null) system._newTerminal(initScript_, args_);	}
	
	/** Resets all the components in the RUV system. */
	public static void resetSystem()
	{
		if (system == null) return;
		Component[] cc_ = Component.Root.getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (cc_[i] == system) continue;
			if (cc_[i] == system.termDir) continue;
			cc_[i].reset();
		}
	}
												   
	/** Removes all the components, except system objects, in the RUV system. */
	public static void cleanUpSystem()
	{
		if (system == null) return;
		Component[] cc_ = Component.Root.getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (cc_[i] == system) continue;
			if (cc_[i] == system.termDir) continue;
			cc_[i].reset();
			Component.Root.removeComponent(cc_[i]);
		}
	}
	
	public static void main(String[] args)
	{
		// -s Shell class
		// -t Term class
		// -w wish executable
		// -h help
		// -a use ruv as auxiliary in other programs (e.g. in GUI)
		// -u uninteractive, no terminal
		// -e end program if no activity in all runtimes
		// -d debug
		// -n no terminal display
		// init script

		String shellClass_ = null, termClass_ = null, initScript_ = null;
		boolean auxiliary_ = false; // true if called by other Java program
		boolean debug_ = false;
		boolean uninteractive_ = false;
		boolean end_ = false;
		boolean noTerminalDisplay_ = false;
		String[] scriptArgs_ = null;
		
		if (args != null) {
			// look for help first
			for (int i=0; i<args.length; i++) {
				if (args[i].startsWith("-") && args[i].indexOf("h") >0) {
					usage();
				}
			}
			
			for (int i=0; i<args.length; i++) {
				if (args[i].startsWith("-s")) shellClass_ = args[++i];
				else if (args[i].startsWith("-t")) termClass_ = args[++i];
				else if (args[i].startsWith("-")) {
					if (args[i].indexOf("a") > 0) auxiliary_ = true;
					if (args[i].indexOf("u") > 0) uninteractive_ = true;
					if (args[i].indexOf("e") > 0) end_ = true;
					if (args[i].indexOf("d") > 0) debug_ = true;
					if (args[i].indexOf("n") > 0) noTerminalDisplay_ = true;
					if (args[i].indexOf("h") > 0) { usage(); java.lang.System.exit(0); }
					if (args[i].indexOf("w") > 0)
						try {
							java.lang.Runtime.getRuntime().exec(args[++i]);
							return;
						}
						catch (Exception e_) {
							java.lang.System.out.println(e_.toString());
						}
				}
				else {
					initScript_ = args[i];
					scriptArgs_ = new String[args.length - i - 1];
					java.lang.System.arraycopy(args, i+1, scriptArgs_, 0, scriptArgs_.length);
					break;
				}
			}
		}

		if (system != null) {
			system.exit2quit = !auxiliary_;
			if (!uninteractive_)
				newTerminal(initScript_, scriptArgs_);
			else
				system.__execute(initScript_, scriptArgs_);
			return;
		}
		
		Shell shell_ = null;
		Term term_ = null;
		
		if (debug_) java.lang.System.out.println("Creating terminal...");
		if (uninteractive_) {
			term_ = new DummyTerm();
		}
		else {
			RUVOutputManager.activate();
		
			try {
				if (termClass_ != null)
					term_ = (Term)Class.forName(termClass_).newInstance();
			} catch (Exception e_) {
				e_.printStackTrace();
				drcl.Debug.error("RUV System", "Startup error: " + e_ + "\n");
				drcl.Debug.error(null, "Use Dterm instead.\n");
			}
			if (term_ == null) {
				// X: bug in Swing?
				try {
					term_ = new Dterm();
				}
				catch (Exception e2_) {
					try {
						term_ = new Dterm();
					}
					catch (Exception e3_) {
						e3_.printStackTrace();
						drcl.Debug.error("RUV System", "Cannot create Dterm: " + e3_ + "\n");
					}
				}
			}
		}
		
		if (debug_) java.lang.System.out.println("Creating shell...");
		
		// Try the shell class assigned by user
		if (shellClass_ != null)
			try {
				shell_ = (Shell)Class.forName(shellClass_).newInstance();
			} catch (Exception e_) {
				try {
					if (shellClass_.indexOf('.') < 0) {
						e_ = null;
						shell_ = (Shell)Class.forName("drcl.ruv." + shellClass_).newInstance();
					}
				}
				catch (Exception e2_) {
					e_ = e2_;
				}
				if (e_ != null) {
					drcl.Debug.error("RUV System", "Startup error: " + e_ + "\n");
					drcl.Debug.error(null, "Use ShellTcl instead.\n");
				}
			}
		
		// use ShellTcl
		if (shell_ == null && (shellClass_ == null || !shellClass_.equals("drcl.ruv.ShellTcl")))
			try {
				shell_ = new ShellTcl();
			}
			catch (Exception e_) {
				//e_.printStackTrace();
				drcl.Debug.error("RUV System", 
								 (shellClass_ == null? "": "Even ") 
								 + "ShellTcl cannot be initiated: " + e_ + ".");
			}
		
		// can't go on 
		if (shell_ == null) java.lang.System.exit(1);
		shell_.setPrompt(shell_.getName().toUpperCase() + "0> ");
		if (!uninteractive_ && term_ != null) {
			term_.setPrompt(shell_.getPrompt());
			term_.setTerminalDisplayEnabled(!noTerminalDisplay_);
			term_.show(); // make term show as soon as possible
			term_.setTitle(shell_.getName().toUpperCase() + "0");
			term_.setID(shell_.getName().toLowerCase() + "0");
		}
		else if (term_ != null)
			term_.setID("uninteractive");
		
		if (debug_) java.lang.System.out.println("Creating RUV system components...");
		
		// /.term
		System termDir_ = new System(".term");
		Component.Root.addComponent(termDir_);
		termDir_.setRuntime(RUNTIME);
		Thread.currentThread().yield();
		
		if (uninteractive_)
			shell_.setID("uninteractive_" + shell_.getName().toLowerCase() + "_shell");
		else
			shell_.setID(shell_.getName().toLowerCase() + "0_shell");
		termDir_.addComponent(shell_);
		
		// connect shell_ and term_
		if (term_ != null) {
			termDir_.addComponent(term_);
			term_.port.connect(shell_.port);
			shell_.result.connectTo(term_.result);
		}
		
		// /.system, /.system/trace, /.system/rt
		System system_ = system = new System();
		system_.setID(".system");
		Component.Root.addComponent(system_);
		system_.setRuntime(RUNTIME);
		system_.exit2quit = !auxiliary_;
		system_.termDir = termDir_;
		if (term_ != null) term_.addExitListener(system_.termExitListener);
		RUVOutputManager.addOutput(term_);
		WrapperComponent javart_ = new WrapperComponent("rt");
		system_.addComponent(javart_);
		javart_.setObject(java.lang.Runtime.getRuntime());
		
		if (!uninteractive_) system_.background();
		
		if (debug_) java.lang.System.out.println("Setting up system monitor...");

		// /.system/monitor
		SystemMonitor monitor_ = system_.systemMonitor = new SystemMonitor("monitor");
		system_.addComponent(monitor_);
		monitor_.setTraceEnabled(true);
		monitor_.setGarbageDisplayEnabled(true);
		monitor_.setDebugEnabled(true);
		monitor_.setErrorNoticeEnabled(true);
		monitor_.setEventExportEnabled(true);
		monitor_.setRcvEnabled(true);
		
		if (initScript_ != null && initScript_.length() > 0) {
			if (debug_) java.lang.System.out.println("Run the script...");

			drcl.comp.Util.inject(ShellContract.createExecuteFileRequest(initScript_, scriptArgs_),
				shell_.port);
		}
		
		if (debug_) java.lang.System.out.println("RUV system is ready.");

		if (end_)
			// start a thread to probe system activities, quit the program if there isn't any
			new Thread() {
				public void run()
				{
					Thread current_ = Thread.currentThread();
					current_.setPriority(Thread.MIN_PRIORITY);
					htRuntime = new Hashtable();
					for (;;) {
						try {
							current_.sleep(1500);
						}
						catch (Exception e_) {
							e_.printStackTrace();
							java.lang.System.exit(0);
						}
						synchronized (htRuntime) { _scanSystem(Component.Root); }
						boolean alive_ = false;
						for (Enumeration e_ = htRuntime.keys(); e_.hasMoreElements(); ) {
							ACARuntime m_ = (ACARuntime)e_.nextElement();
							if (!m_.isIdle()) { alive_ = true; break; }
						}
						if (!alive_) {
							java.lang.System.exit(0);
						}
					}
				}
			}.start();
	}

	static void _scanSystem(Component root_)
	{
		htRuntime.clear();
		Vector v_ = new Vector(100, 100);
		v_.addElement(root_);
		while (v_.size() > 0) {
			root_ = (Component)v_.lastElement();
			v_.setSize(v_.size()-1);
			ACARuntime m_ = drcl.comp.Util.getRuntime(root_);
			if (m_ != null) htRuntime.put(m_, m_);
			Component[] cc_ = root_.getAllComponents();
			for (int j=0; j<cc_.length; j++)
				v_.addElement(cc_[j]);
		}
	}
	
	public System()
	{ super(); }
	
	public System(String id_)
	{ super(id_); }

	public String info()
	{
		if (htRuntime != null) {
			StringBuffer sb_ = new StringBuffer();
			synchronized (htRuntime) {
				for (Enumeration e_ = htRuntime.keys(); e_.hasMoreElements(); ) {
					ACARuntime m_ = (ACARuntime)e_.nextElement();
					if (!m_.isIdle())
						sb_.append(m_.diag() + "\n");
				}
			}
			if (sb_.length() == 0)
				return "All runtimes are idle.\n";
			else
				return sb_.toString();
		}
		else
			return "No info is available.";
	}
	
	// avoid accidentally changing this
	/** Overrides {@link Component#setRuntime(ACARuntime)} to prevent changing the runtime by accident. */
	protected void setRuntime(ACARuntime m_)
	{ if (m_ == RUNTIME) super.setRuntime(m_); }
	
	
	static Shell getBackupShell()
	{ return backupShell; }
	
	static void usage()
	{
		java.lang.System.out.println("System [-ubh] [-s shell_class] [-t term_class] [init script]");
		java.lang.System.out.println("Options:");
		java.lang.System.out.println("    -u: use the RUV system as part of other programs (e.g. GUI).");
		java.lang.System.out.println("    -b: execute a script uninteractively.");
		java.lang.System.out.println("    -e: end the program when simulation is stopped .");
		java.lang.System.out.println("    -h: display help.");
		java.lang.System.exit(1);
	}
	
	void background()
	{
		//Thread thread_ = new Thread() {
		//	// create things in background
		//	public void run()
		//	{
				try {
					synchronized (System.this) {
						if (backupShell == null) {
							backupShell = new ShellTcl("backup");
							if (termDir != null)
								termDir.addComponent(backupShell);
						}
					}
				}
				catch (Exception e_) {
					e_.printStackTrace();
				}
		//	}
		//};
		//thread_.setPriority(Thread.MIN_PRIORITY);
		//thread_.start();
	}
			
	/** Creates a new terminal in the RUV system.
	 */
	public void _newTerminal()
	{ _newTerminal(null, null); }
	
	/** Creates a new terminal in the RUV system and executes the script in the terminal.
	 */
	public void _newTerminal(String initScript_, String[] args_)
	{
		try {
			Term term_ = new Dterm();
			Shell shell_ = new ShellTcl();
			int i = 0;
			for (; termDir.containsComponent("tcl" + i + "_shell"); i++);
			term_.setID("tcl" + i);
			shell_.setID("tcl" + i + "_shell");
			addTerminal(term_, shell_, initScript_, args_);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}
	
	public void addTerminal(Term terminal_, Shell shell_, String initScript_, String[] args_)
	{
		termDir.addComponent(shell_);
		if (terminal_ != null) {
			terminal_.addExitListener(termExitListener);
			termDir.addComponent(terminal_);
			terminal_.port.connect(shell_.port);
			shell_.result.connectTo(terminal_.result);
			shell_.setPrompt(terminal_.getID().toUpperCase() + "> ");
			terminal_.setTitle(terminal_.getID().toUpperCase());
			terminal_.setPrompt(shell_.getPrompt());
			terminal_.show();
			RUVOutputManager.addOutput(terminal_);
		}
		
		
		if (initScript_ != null && initScript_.length() > 0)
			drcl.comp.Util.inject( ShellContract.createExecuteFileRequest(initScript_, args_), shell_.port);
	}
	
	// execute the script in background, create a shell if necessary
	void __execute(String initScript_, String[] args_)
	{
		try {
			Shell shell_ = (Shell)termDir.getComponent(".background");
			if (shell_ == null) shell_ = new ShellTcl(".background");
			addTerminal(null, shell_, initScript_, args_);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}
	
	void showThreads()
	{
	}
	
	TermExitListener termExitListener =  new TermExitListener() {
		public void termExit(Term which_, String exitOrQuit_)
		{
			termDir.removeComponent(which_);
			//java.lang.System.out.println("System.termExit(): '" + exitOrQuit_ + "'");
			RUVOutputManager.removeOutput(which_);
			if (!exit2quit) return;
			if (exitOrQuit_.equals(Shell.COMMAND_QUIT)) java.lang.System.exit(0);
		
			Component[] cc_ = termDir.getAllComponents();
			for (int i=0; i<cc_.length; i++) {
				Component c_ = cc_[i];
				if (c_ instanceof Term) {
					// XX: remove the corresponding shell
					return;
				}
			}
			java.lang.System.exit(0);
		}
	};

	public static Term getTerminal(Shell shell_)
	{
		Port[] pp_ = shell_.port.getPeers();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i].host instanceof Term)
				return (Term)pp_[i].host;
		return null;
	}
}
