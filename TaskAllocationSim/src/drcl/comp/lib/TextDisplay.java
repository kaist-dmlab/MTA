// @(#)TextDisplay.java   9/2002
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
 * A simple text display component.
 * Whenever a message is received from the port, it is displayed
 * in the display area.
 */
public class TextDisplay extends Extension
{
	Port port = addPort("in");
	TextArea taDisplay = new TextArea(30, 80);
	boolean isPaste = true;
	
	{ taDisplay.setEditable(false); }
						
	public TextDisplay()
	{	super();	}

	public TextDisplay(String id_)
	{	super(id_);}

	protected void process(Object data_, drcl.comp.Port inPort_)
	{
		if (data_ == null) return;
		
		synchronized (taDisplay) {
			if (isPaste) taDisplay.setText(data_.toString());
			else taDisplay.append(data_.toString());
		}
	}

	public void reset()
	{
		super.reset(); // Let super class reset its fields.
		taDisplay.setText("");
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_); // Let super class copy its fields.
		TextDisplay that_ = (TextDisplay)source_;
		taDisplay.setRows(that_.taDisplay.getRows());
		taDisplay.setColumns(that_.taDisplay.getColumns());
		taDisplay.setText(that_.taDisplay.getText());
	}

	public String info()
	{
		return this + ":\n" + taDisplay.getText();
	}

	/**
	 * Sets the number of rows and columns of the display text area.
	 * The number of columns also affects that of the input text area.
	 */
	public void setDisplaySize(int rows_, int cols_)
	{
		taDisplay.setRows(rows_);
		taDisplay.setColumns(cols_);
		taDisplay.validate();
	}
	
	public java.awt.Component getDisplay() 
	{ return taDisplay; }
	
	public boolean isPaste() { return isPaste; }
	public void setPaste(boolean v_) { isPaste = v_; }
}
