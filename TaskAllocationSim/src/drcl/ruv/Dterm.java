// @(#)Dterm.java   7/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import drcl.comp.*;

/**
 * Implemnets a terminal.
 */
public class Dterm extends  Term implements ActionListener, Wrapper
{
	int ScrWidth = 80, ScrHeight = 24;
	JFrame frame;
	JScrollPane scrollPane;
	JMenuBar mBar;
	JMenu mFile, mEdit, mHelp;
	JTextArea ta, tf;
	//JTextField tf;
	JFileChooser chooser;
	JMenuItem lineWrap, outputEnable;
	
	// for file dialog
	File currentFile = null;

	
	MyKeyAdapter tt = new MyKeyAdapter();
	
	public Dterm()
	{ super(); setID("dterm"); init(); }
	
	public Dterm(String id_)
	{ super(id_); init(); }
	
	public String info()
	{
		return super.info() +
			   "currentFile: " + currentFile + "\n";
	}
	
	void init() 
	{
		// JFileChooser is slow, so create it in advance and do it in low-priority background thread
		Thread backgroundTask_ = new Thread() {
			public void run() {
				synchronized (Dterm.this) {
					chooser = new JFileChooser();
					javax.swing.filechooser.FileFilter filter_ = new javax.swing.filechooser.FileFilter() {
						public boolean accept(File f_)
						{ return f_.isDirectory() || f_.getName().endsWith(".tcl"); }
						public String getDescription()
						{ return "TCL Script (*.tcl)"; }
					};
					chooser.setFileFilter(filter_);
					Dterm.this.notify();
				}
			}
		};
		backgroundTask_.setPriority(Thread.MIN_PRIORITY);
		backgroundTask_.start();

		frame = new JFrame(title);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e_)
			{
				write(Shell.COMMAND_EXIT + "\n");
				exit();
			}

			public void windowActivated(WindowEvent e_) 
			{ isfocused = true;	 }
			
			public void windowDeactivated(WindowEvent e_) 
			{ isfocused = false; }
		});

		mBar = new JMenuBar();
		mFile = new JMenu("File");
		mFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem mi_ = new JMenuItem("Open...");
		mi_.setMnemonic(KeyEvent.VK_O);
		mi_.addActionListener(this);
		mFile.add(mi_);
		mFile.addSeparator();

		mi_ = new JMenuItem("Save");
		mi_.setMnemonic(KeyEvent.VK_S);
		mi_.addActionListener(this);
		mFile.add(mi_);

		mi_ = new JMenuItem("Save As...");
		mi_.setMnemonic(KeyEvent.VK_V);
		mi_.addActionListener(this);
		mFile.add(mi_);

		mi_ = new JMenuItem("Append To...");
		mi_.setMnemonic(KeyEvent.VK_A);
		mi_.addActionListener(this);
		mFile.add(mi_);
		mFile.addSeparator();

		mi_ = new JMenuItem("Exit");
		mi_.setMnemonic(KeyEvent.VK_X);
		mi_.addActionListener(this);
		mFile.add(mi_);
		mBar.add(mFile);

		mEdit = new JMenu("Edit");
		mEdit.setMnemonic(KeyEvent.VK_E);
		lineWrap = new JMenuItem("Set Line Wrap");
		lineWrap.setMnemonic(KeyEvent.VK_L);
		lineWrap.addActionListener(this);
		mEdit.add(lineWrap);

		outputEnable = new JMenuItem("Disable Terminal Display");
		outputEnable.setMnemonic(KeyEvent.VK_T);
		outputEnable.addActionListener(this);
		mEdit.add(outputEnable);

		mi_ = new JMenuItem("Copy");
		mi_.setMnemonic(KeyEvent.VK_C);
		mi_.addActionListener(this);
		mEdit.add(mi_);

		mi_ = new JMenuItem("Paste");
		mi_.setMnemonic(KeyEvent.VK_P);
		mi_.addActionListener(this);
		mEdit.add(mi_);

		mBar.add(mEdit);

		ta = new JTextArea(ScrHeight, ScrWidth);
		//ta.setEditable(false);
		ta.setFont(new Font("Courier", Font.PLAIN, 12));
		//tf = new JTextField("", ScrWidth);
		tf = new JTextArea(1, ScrWidth);
		tf.setBorder(BorderFactory.createLineBorder(Color.gray));
		scrollPane = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.getContentPane().add(tf, BorderLayout.SOUTH);
		if (isTerminalDisplayEnabled())
			frame.getContentPane().add(scrollPane, BorderLayout.CENTER);			
		frame.setJMenuBar(mBar);
		frame.pack();
		ta.addKeyListener(tt);
		tf.addKeyListener(tt);
		tf.requestFocus();
	}

	public void setTerminalDisplayEnabled(boolean enabled_)
	{
		super.setTerminalDisplayEnabled(enabled_);
		//ta.setRows(isTerminalDisplayEnabled()? ScrHeight:0);
		if (isTerminalDisplayEnabled()) {
			frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
			if (frame.isShowing() && ta.getText().length() == 0)
				ta.append(getPrompt());
		}
		else
			frame.getContentPane().remove(scrollPane);
		frame.pack();
		outputEnable.setText(isTerminalDisplayEnabled()? "Disable Terminal Display": "Enable Terminal Display");
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object arg = evt.getSource();
		if (arg == lineWrap) {
			ta.setLineWrap(!ta.getLineWrap());
			lineWrap.setText(ta.getLineWrap()? "Unset Line Wrap": "Set Line Wrap");
		}
		else if (arg == outputEnable) {
			setTerminalDisplayEnabled(!isTerminalDisplayEnabled());
		}
		else {
			arg = evt.getActionCommand();
			if (arg.equals("Copy")) {
				if (ta.getSelectedText() != null && ta.getSelectedText().length() > 0) {
					ta.copy();
					//java.lang.System.out.println("selected text:'"+ta.getSelectedText()+"'");
					//java.lang.System.out.println("clipboard:" + java.awt.Toolkit.getDefaultToolkit().getSystemClipboard());
				}
				else if (tf.getSelectedText() != null && tf.getSelectedText().length() > 0) {
					tf.copy();
					//java.lang.System.out.println("selected text:'"+tf.getSelectedText()+"'");
					//java.lang.System.out.println("clipboard:" + java.awt.Toolkit.getDefaultToolkit().getSystemClipboard());
				}
				/*
				// send Ctrl-C to ta or tf
				if (ta.getSelectedText() != null && ta.getSelectedText().length() > 0) {
					ta.dispatchEvent(new KeyEvent(ta, KeyEvent.KEY_PRESSED,
						java.lang.System.currentTimeMillis(), InputEvent.CTRL_MASK, KeyEvent.VK_C));
				}
				else if (tf.getSelectedText() != null && ta.getSelectedText().length() > 0) {
					tf.dispatchEvent(new KeyEvent(tf, KeyEvent.KEY_PRESSED,
						java.lang.System.currentTimeMillis(), InputEvent.CTRL_MASK, KeyEvent.VK_C));
				}
				*/
			}
			else if (arg.equals("Paste")) {
				//java.lang.System.out.println("clipboard:" + java.awt.Toolkit.getDefaultToolkit().getSystemClipboard());
				tf.paste();
				/*
				// send Ctrl-V to tf
				tf.dispatchEvent(new KeyEvent(tf, KeyEvent.KEY_PRESSED,
					java.lang.System.currentTimeMillis(), InputEvent.CTRL_MASK, KeyEvent.VK_V));
					*/
			}
			else if (arg.equals("Open..."))	open();
			else if (arg.equals("Save")) save(false);
			else if (arg.equals("Save...")) save(false);
			else if (arg.equals("Save As...")) saveas();
			else if (arg.equals("Append To...")) save(true);
			else if (arg.equals("Exit")) {
				write(Shell.COMMAND_EXIT + "\n");
				exit();
			}
		}
	}

	// for debugging
	public Object getObject()
	{ return ta; }
	
	public void setTitle(String title_)
	{
		super.setTitle(title_);
		frame.setTitle(title_);
	}

	// maxlength and messy codes in write() are for limited buffer size in TextArea B(
	int maxlength = Integer.MAX_VALUE;
	
	protected void _write(String msg_)
	{
		synchronized (ta) {
			ta.append(msg_);
			ta.setCaretPosition(ta.getText().length());
		}
	}
	
	public void show()
	{
		write(getPrompt());
		frame.show();
	}
	
	public void hide()
	{ frame.setVisible(false); }
	
	boolean isfocused = false;
	
	public boolean isFocused()
	{ return isfocused; }
	
	public void exit()
	{
		super.exit();
		frame.dispose();
	}
	
	protected void setCommand(String cmd_, int pos_)
	{
		tf.setText(cmd_);
		tf.setCaretPosition(pos_);
	}
	
	String getCommand()
	{	return tf.getText();	}
	
	int getCommandPosition()
	{ return tf.getCaretPosition(); }
	
	String getPartialCommand()
	{	return tf.getText().substring(0,tf.getCaretPosition());	}

	// return false if cancelled
	boolean _getFile(String title_)
	{
		synchronized (this) {
			try {
				if (chooser == null) this.wait();
			}
			catch (Exception e_) {
				e_.printStackTrace();
			}
		}
		chooser.setCurrentDirectory(currentFile == null? new File("."): currentFile.getParentFile());
		if(chooser.showDialog(frame, title_) == JFileChooser.APPROVE_OPTION) {
			currentFile = chooser.getSelectedFile();
			return true;
		}
		else
			return false;
	}
	
	void open()
	{
		if (_getFile("open"))	readfile();
		currentFile = null;
	}
	
	void save(boolean append_)
	{
		if (currentFile == null || append_) {
			if (_getFile(append_? "Append To": "Save")) {
				if (!append_) {
					// XXX: give a warning
				}
				
				writefile(append_);
				_setTitle();
			}			
		}
		else 
			writefile(append_);
	}
	
	void saveas()
	{
		if (_getFile("SaveAs")) {
			writefile(false);
			_setTitle();
		}
	}
	
	void writefile(boolean append_)
	{
		try	{
			BufferedWriter bw_ = new BufferedWriter(new FileWriter(currentFile.getPath(), append_));
			saveHistory(bw_);
		}
		catch(Exception e_) {
			write(e_ + "\n");
		}
	}
	
	void _setTitle()
	{
		int i = getTitle().indexOf(" - ");
		if (i < 0)
			setTitle(getTitle() + " - " + currentFile.getName());
		else
			setTitle(getTitle().substring(0, i) + " - " + currentFile.getName());
	}
	
	void readfile()
	{
		try	{
			evalFile(currentFile.getPath());
		}
		catch(Exception e_) {
			write(e_ + "\n");
		}
	}
	
	String partialCmd = null; // for smart history similar to matlab

	public class MyKeyAdapter extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt_)
		{
			int priority_ = Thread.currentThread().getPriority();
			if (priority_ != Thread.MAX_PRIORITY) {
				//java.lang.System.out.println("changed from " + priority_ + " to " + Thread.MAX_PRIORITY);
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			}
			//java.lang.System.out.println("kep pressed: " + (int)evt_.getKeyChar() + ": " + evt_);
			switch(evt_.getKeyCode())
			{
			case KeyEvent.VK_UP:
				String cmd_ = getPartialCommand();
				if (partialCmd == null) partialCmd = cmd_;
				if (evt_.isControlDown()) partialCmd = "";
				cmd_ = partialCmd.length() == 0? getHistory(-1): getHistoryUp(partialCmd);
				if (cmd_ != null) setCommand(cmd_, cmd_.length());
				evt_.consume();
				break;
			case KeyEvent.VK_DOWN:
				cmd_ = getPartialCommand();
				if (partialCmd == null) partialCmd = cmd_;
				if (evt_.isControlDown()) partialCmd = "";
				cmd_ = partialCmd.length() == 0? getHistory(+1): getHistoryDown(partialCmd);
				if (cmd_ != null) setCommand(cmd_, cmd_.length());
				evt_.consume();
				break;
			case KeyEvent.VK_ENTER:
				partialCmd = null;
				evt_.consume();
				cmd_ = getCommand();//.trim();
				
				//if (cmd_.length() > 0) {
					if (evt_.isControlDown()) {
						addCmdToHistory(cmd_);
						setCommand("", 0);
						write(cmd_ + "\n");
						diagnoze(cmd_);
						return;
					}
					
					// XX: synchronized here to avoid result comes back sooner
					synchronized (Dterm.this) {
						if (evalCommand(cmd_, true)) {
							addCmdToHistory(cmd_);
							setCommand("", 0);
						}
						else
							// suppose it was "bee" sound
							RUVOutputManager.SYSTEM_OUT.print(
											new String(new byte[]{7}));
					}
				//}
				//else {
				//	write("\n" + getPrompt());
				//}
				break;
			case KeyEvent.VK_TAB:
				//java.lang.System.out.println("TAB..." + evt_.getSource());
				evt_.consume();
				cmd_ = getCommand();
				if (cmd_.trim().length() > 0)
					autocomplete(cmd_, tf.getCaretPosition());
				else
					setCommand(cmd_ + "    ", cmd_.length() + 4);
				break;
			// JDK for linux somehow does not respond to the following two events, so need to do it myself...
			case KeyEvent.VK_LEFT:
				if (tf.getCaretPosition() > 0)
					tf.setCaretPosition(tf.getCaretPosition() - 1);
				evt_.consume();
				break;
			case KeyEvent.VK_RIGHT:
				if (tf.getCaretPosition() < tf.getText().length())
					tf.setCaretPosition(tf.getCaretPosition() + 1);
				evt_.consume();
				break;
			}
		}

		boolean reentrance = false; // XX: bug in JDK1.4?
		public void keyTyped(KeyEvent evt_)
		{
			//java.lang.System.out.println("kep typed: " + (int)evt_.getKeyChar() + ": " + evt_);
			if (reentrance && evt_.getSource() == ta) {
				evt_.consume();
				return;
			}
			int c_ = (int)evt_.getKeyChar();
			switch(evt_.getKeyCode())
			{
			case KeyEvent.VK_V:
				// Windows: transfer CTRL-V from "ta" to "tf"
				if (evt_.isControlDown() && evt_.getSource() == ta) {
					tf.dispatchEvent(new KeyEvent(tf, evt_.getID(), evt_.getWhen(), evt_.getModifiers(),
						evt_.getKeyCode(), evt_.getKeyChar()));
					evt_.consume();
					break;
				}
			case KeyEvent.VK_C:
				if (!evt_.isControlDown()) break;
				else c_ = KeyEvent.VK_CANCEL;
			default:
				if (c_ == KeyEvent.VK_CANCEL) {
					// for IBM port/linux, CTRL-C is VK_CANCEL
					Object source_ = evt_.getSource();
					String stext_ = source_ == ta? ta.getSelectedText(): tf.getSelectedText();
					//java.lang.System.out.println("Selected text = '"+stext_+"'");
					// Delegate to CTRL-C default behavior if some text is selected
					if (stext_ != null && stext_.length() > 0) return;
					evt_.consume();
					write("^C\n");
					interrupt();
					partialCmd = null;
					setCommand("", 0);
					break;
				}
				partialCmd = null;
				if (!evt_.isConsumed() && !evt_.isControlDown()) {
					if (evt_.getSource() == ta) {
						//java.lang.System.out.println("Transfer focus to tf: " + evt_);
						evt_.consume();
						tf.requestFocus();
						reentrance = true;
						tf.dispatchEvent(new KeyEvent(tf, evt_.getID(), evt_.getWhen(), evt_.getModifiers(),
							evt_.getKeyCode(), evt_.getKeyChar()));
						reentrance = false;
					}
					else {
						//java.lang.System.out.println("tf got: " + evt_);
					}
				}
			}
		}
	}
}
