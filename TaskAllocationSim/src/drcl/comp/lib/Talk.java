// @(#)Talk.java   9/2002
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

package drcl.comp.lib;

import java.awt.*;
import java.awt.event.*;
import drcl.comp.*;

/**
 * A simple chat client component.  It is a frame with two areas inside: a display area
 * and an input area.  Whenever a line is formed in the input area, the line is sent out
 * from the "inout" port.  And whenever a message is received from the port, it is displayed
 * in the display area.
 * 
 * Ports (in addition to those defined in <code>drcl.comp.Component</code>):
 * <ul>
 *	<li> "inout" port: First, it sends out one line of messages when a line is inputed
 *		from the user.  The line is prepended with the ID + ": ".  Second, the component
 *		displays any message comes in from this port.
 * </ul>
 */
public class Talk extends drcl.comp.Component implements drcl.comp.ActiveComponent
{
	public static final String INOUT_PORT_ID = "inout";
	
	protected Port port = addPort(INOUT_PORT_ID);
	Frame frame = new Frame();
	protected TextArea taDisplay = new TextArea(30, 80);
	protected TextArea taInput = new TextArea(5, 80);
	boolean echo = true;
	boolean anonymous = false;
	
	{
		taDisplay.setEditable(false);
		taInput.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e_)
//			public void keyReleased(KeyEvent e_)
			{
				switch(e_.getKeyCode())
				{
				case KeyEvent.VK_ENTER:
					String msg_ = (anonymous? "": getID() + ": ") + taInput.getText().trim();
					if (echo) taDisplay.append(msg_);
					processLine(msg_);
					taInput.setText("");
					e_.consume();
					break;
				//default:
				//	if (!filter(e_))
				//		e_.consume();
				}
			}
		});
		frame.add(taDisplay, BorderLayout.CENTER);
		frame.add(taInput, BorderLayout.SOUTH);
		
		frame.pack();
	}
	
						
	/**
	 * Constructor.
	 */
	public Talk()
	{	super();	}

	/**
	 * Constructor.
	 */
	public Talk(String id_)
	{	super(id_);	}

	/**
	 * Invoked when <code>data_</code> arrives this component at the <code>inPort_</code> port.
	 */
	public void process(Object data_, drcl.comp.Port inPort_)
	{
		if (data_ == null) return;
		
		synchronized (taDisplay) {
			taDisplay.append(data_.toString());
		}
	}

	/**
	 * Resets this component to the initial state for use anew.
	 * Must call <code>super.reset()</code> in the beginning.
	 */
	public void reset()
	{
		super.reset(); // Let super class reset its fields.
		taDisplay.setText("");
		taInput.setText("");
	}

	/**
	 * Copies the content from the <code>source_</code> to this component.
	 * Must call <code>super.duplicate()</code> in the beginning.
	 */
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof Talk)) return;
		super.duplicate(source_); // Let super class copy its fields.
		Talk that_ = (Talk)source_;
		taDisplay.setRows(that_.taDisplay.getRows());
		taDisplay.setColumns(that_.taDisplay.getColumns());
		taInput.setRows(that_.taInput.getRows());
		taInput.setColumns(that_.taInput.getColumns());
		taDisplay.setText(that_.taDisplay.getText());
	}

	/**
	 * Invoked when the component is <code>run()</code>ed.
	 */
	protected void _start()
	{
		frame.setTitle(getID());
		frame.show();
	}

	/**
	 * Script interface which reveals the internal states of the component.
	 * It is for debugging and demonstration purpose.
	 */
	public String info()
	{
		return "My ID: " + getID() + "\n";
	}

	public void setAnonymous(boolean v_)
	{ anonymous = v_; }
	
	public boolean isAnonymous()
	{ return anonymous; }
	
	public void setEchoEnabled(boolean v_)
	{ echo = v_; }
	
	public boolean isEchoEnabled()
	{ return echo; }
	
	/**
	 * Sets the number of rows and columns of the display text area.
	 * The number of columns also affects that of the input text area.
	 * @param rows_		number of rows; no effect if nonpositive value is given.
	 * @param cols_		number of columns; no effect if nonpositive value is given.
	 */
	public void setDisplaySize(int rows_, int cols_)
	{
		if (rows_ > 0) taDisplay.setRows(rows_);
		if (cols_ > 0) {
			taDisplay.setColumns(cols_);
			taInput.setColumns(cols_);
		}
		frame.pack();
	}
	
	/**
	 * Sets the number of rows and columns of the input text area.
	 * The number of columns also affects that of the display text area.
	 * @param rows_		number of rows; no effect if nonpositive value is given.
	 * @param cols_		number of columns; no effect if nonpositive value is given.
	 */
	public void setInputSize(int rows_, int cols_)
	{
		if (rows_ > 0) taInput.setRows(rows_ + 1);
		if (cols_ > 0) {
			taInput.setColumns(cols_);
			taDisplay.setColumns(cols_);
		}
		frame.pack();
	}
	
	protected void processLine(String line_)
	{
		port.doSending(line_);
	}
	
	/**
	 * @return true if the key is ok to be processed by the default behavior.
	protected boolean filter(KeyEvent key_)
	{
		return true;
	}
	 */
}
