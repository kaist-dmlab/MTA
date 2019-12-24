// @(#)DtermAWT.java   7/2003
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
import java.io.*;
import java.util.*;
import drcl.comp.*;

/**
 * Implements a (AWT) terminal.
 */
public class DtermAWT extends  Term
{
	int ScrWidth = 80, ScrHeight = 24;
	Frame frame;
	MenuBar mBar;
	Menu mFile, mEdit, mHelp;
	TextArea ta;
	TextField tf;
	
	// for file dialog
	String currentDir = ".";
	String currentFile = "";

	
	MyKeyAdapter tt = new MyKeyAdapter();
	
	public DtermAWT()
	{ super(); setID("dterm"); init(); }
	
	public DtermAWT(String id_)
	{ super(id_); init(); }
	
	public String info()
	{
		return super.info() +
			   " currentDir: " + currentDir + "\n" +
			   "currentFile: " + currentFile + "\n";
	}
	
	void init() 
	{
		frame = new Frame(title);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e_)
			{
				write(Shell.COMMAND_EXIT + "\n");
				//evalCommand(Shell.COMMAND_EXIT);
				exit();
			}

			public void windowActivated(WindowEvent e_) 
			{ isfocused = true;	 }
			
			public void windowDeactivated(WindowEvent e_) 
			{ isfocused = false; }
		});

		mBar = new MenuBar();
		mFile = new Menu("File");
		mFile.add(new MenuItem("Open..."));
		mFile.addSeparator();
		mFile.add(new MenuItem("Save"));
		mFile.add(new MenuItem("Save As..."));
		mFile.add(new MenuItem("Append To..."));
		mFile.addSeparator();
		mFile.add(new MenuItem("Exit"));
		mFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt)
			{
				String arg = evt.getActionCommand();
				if (arg.equals("Open..."))	open();
				else if (arg.equals("Save")) save(false);
				else if (arg.equals("Save...")) save(false);
				else if (arg.equals("Save As...")) saveas();
				else if (arg.equals("Append To...")) save(true);
				else if (arg.equals("Exit")) {
					write(Shell.COMMAND_EXIT + "\n");
					//evalCommand(Shell.COMMAND_EXIT);
					exit();
				}
			}
		});
		mBar.add(mFile);
		/*
		mEdit = new Menu("Edit");
		mEdit.add(new MenuItem("Copy"));
		mEdit.add(new MenuItem("Paste"));
		mBar.add(mEdit);
		*/
		ta = new TextArea(ScrHeight, ScrWidth);
		ta.setEditable(false);
		ta.setFont(new Font("Courier", Font.PLAIN, 12));
		ta.addKeyListener(tt);
		tf = new TextField("", ScrWidth);
		tf.addKeyListener(tt);
		frame.add(tf, BorderLayout.SOUTH);
		frame.add(ta, BorderLayout.CENTER);			
		frame.setMenuBar(mBar);
		frame.pack();
	}
	
	protected void setPrompt(String prompt_) 
	{
		super.setPrompt(prompt_);
		if (ta.getText().length() == 0)
			write(prompt_);
	}
	
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
			String old_ = ta.getText();
			if (maxlength == Integer.MAX_VALUE) {
				ta.append(msg_);
				String new_ = ta.getText();
				int diff_ = old_.length() + msg_.length() - new_.length();
				if (diff_ != 0) {
					// detect a maxlength
					ta.setEnabled(false);
					maxlength = new_.length();
					StringBuffer sb_ = new StringBuffer();
					for (int i=0; i<diff_/2; i++) sb_.append(" ");
					if (sb_.length() > 0)
						for (;;) {
							for (;;) {
								ta.append(sb_.toString());
								if (maxlength == ta.getText().length()) break;
								maxlength = ta.getText().length();
							}
							int len_ = sb_.length();
							if (len_ == 1) break;
							sb_.setLength(len_/2);
						}
					maxlength -= 400; /* some slack */
					ta.setText(old_);
					ta.setEnabled(true);
					//System.out.println(getClass() + " old length: " + old_.length() + ", msg length: " + msg_.length());
					//System.out.println(getClass() + " max length: " + maxlength);
				}
				else {
					return;
				}
			} 
		
			int msglen_ = msg_.length();
			int oldlen_ = old_.length();
		
			if (msglen_ + oldlen_ + 400 <= maxlength) {
				//System.out.println(getClass() + " old length: " + old_.length() + ", msg length: " + msg_.length());
				//System.out.println(getClass() + " max length: " + maxlength);
				ta.append(msg_);
			}
			else {
				String new_ = old_ + msg_;
				
				int over_ = new_.length() - maxlength + 400/*slack*/;
				int i = 0;
				while (i < over_) {
					int j = new_.indexOf("\n", i);
					if (j < 0) { i = j; break; }
					else i = j+1;
				}
				//System.out.println(getClass() + " old length: " + old_.length() + ", msg length: " + msg_.length());
				//System.out.println(getClass() + " max length: " + maxlength);
				//System.out.println("i = " + i + ", over=" + over_);
				if (i < 0 || i > over_ + 1000) { // long line 
					i = over_;
					ta.setText("--snip--snip--snip--" + new_.substring(i));
				}
				else
					ta.setText(new_.substring(i));
			}
			ta.setCaretPosition(ta.getText().length());
		}
	}
	
	public void show() { frame.show(); }
	
	public void hide() { frame.setVisible(false); }
	
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
		FileDialog fd_ = new FileDialog(frame, title_);
		fd_.setDirectory(currentDir);
		fd_.setFile(currentFile);
		fd_.show();
		String tmp  = fd_.getFile();
		if (tmp != null) {
			currentDir = fd_.getDirectory();
			currentFile = tmp;
			return true;
		}
		else
			return false;
	}
	
	void open()
	{
		if (_getFile("open"))	readfile();
	}
	
	void save(boolean append_)
	{
		if (currentFile.equals("") || append_) {
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
			BufferedWriter bw_ = new BufferedWriter(new FileWriter(currentDir + currentFile, append_));
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
			setTitle(getTitle() + " - " + currentFile);
		else
			setTitle(getTitle().substring(0, i) + " - " + currentFile);
	}
	
	void readfile()
	{
		try	{
			evalFile(currentDir + currentFile);
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
			//c = evt_.getKeyChar();
			//System.out.println("kep pressed: " + ((int)c));
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
					synchronized (DtermAWT.this) {
						if (evalCommand(cmd_, true)) {
							addCmdToHistory(cmd_);
							setCommand("", 0);
						}
						else
							// suppose it was "bee" sound
							java.lang.System.out.print(new String(new byte[]{7}));
					}
				//}
				//else {
				//	write("\n" + getPrompt());
				//}
				break;
			case KeyEvent.VK_TAB:
				evt_.consume();
				cmd_ = getCommand();
				if (cmd_.trim().length() > 0)
					autocomplete(cmd_, tf.getCaretPosition());
				else
					setCommand(cmd_ + "    ", cmd_.length() + 4);
				break;
			case KeyEvent.VK_C:
				if (evt_.isControlDown()) {
					Object source_ = evt_.getSource();
					//java.lang.System.out.println("Event source: " + source_);
					String stext_ = source_ == ta? ta.getSelectedText(): tf.getSelectedText();
					//java.lang.System.out.println("Selected text: " + stext_);
					if (stext_.length() > 0) return;
					interrupt();
					partialCmd = null;
					setCommand("", 0);
					write("^C\n");
				}
				// go to default
			default:
				partialCmd = null;
				if (!evt_.isControlDown()) {
					//java.lang.System.out.println("Transfer focus to tf.");
					tf.requestFocus();
				}
			}
		}
	}
}
