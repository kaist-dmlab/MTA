// @(#)ComponentMonitor.java   9/2002
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

import java.awt.*;
import java.awt.event.*;
import drcl.comp.Port;
import drcl.comp.contract.*;
import drcl.data.*;

/**
 * A simple text display component.
 * Whenever a message is received from the port, it is displayed
 * in the display area.
 */
public class ComponentMonitor extends drcl.comp.lib.TextDisplay
{
	public ComponentMonitor()
	{	super();	}

	public ComponentMonitor(String id_)
	{	super(id_);		}

	Button btnCont = new Button("Continue");
	Panel panelMsg = new Panel(); // panel that consists message checkboxes
	Checkbox cbMsgTrace = new Checkbox("Trace", false);
	Checkbox cbMsgEvent = new Checkbox("Event", true);
	Checkbox cbMsgDebug = new Checkbox("Debug", false);
	Checkbox cbMsgGarbage = new Checkbox("Garbage", false);
	Checkbox cbMsgError = new Checkbox("Error", true);
	Panel panelExe = new Panel(); // panel that consists execution control checkboxes
	Checkbox cbExeTrace = new Checkbox("Trace", false);
	Checkbox cbExeEvent = new Checkbox("Event", false);
	Checkbox cbExeDebug = new Checkbox("Debug", false);
	Checkbox cbExeGarbage = new Checkbox("Garbage", false);
	Checkbox cbExeError = new Checkbox("Error", false);
	
	ItemListener myItemListener = new ItemListener() {
		public void itemStateChanged(ItemEvent evt_) {
			Object o_ = evt_.getSource();
			if (o_ == cbMsgTrace) {
				setTraceEnabled(cbMsgTrace.getState());
			}
			else if (o_ == cbMsgEvent) {
				setEventExportEnabled(cbMsgTrace.getState());
			}
			else if (o_ == cbMsgDebug) {
				setDebugEnabled(cbMsgTrace.getState());
			}
			else if (o_ == cbMsgGarbage) {
				setGarbageEnabled(cbMsgTrace.getState());
			}
			else if (o_ == cbMsgError) {
				setErrorNoticeEnabled(cbMsgTrace.getState());
			}
			else if (o_ == cbExeTrace) {
				if (cbExeTrace.getState() && !cbMsgTrace.getState())
					cbMsgTrace.setState(true);
			}
			else if (o_ == cbExeEvent) {
				if (cbExeEvent.getState() && !cbMsgEvent.getState())
					cbMsgEvent.setState(true);
			}
			else if (o_ == cbExeDebug) {
				if (cbExeDebug.getState() && !cbMsgDebug.getState())
					cbMsgDebug.setState(true);
			}
			else if (o_ == cbExeGarbage) {
				if (cbExeGarbage.getState() && !cbMsgGarbage.getState())
					cbMsgGarbage.setState(true);
			}
			else if (o_ == cbExeError) {
				if (cbExeError.getState() && !cbMsgError.getState())
					cbMsgError.setState(true);
			}
		}
	};
	
	Panel main = new Panel();
	
	{
		btnCont.setEnabled(false);
		btnCont.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e_) {
				System.out.println("Notify " + ComponentMonitor.this);
				synchronized (ComponentMonitor.this) {
					ComponentMonitor.this.notifyAll();
				}
			}
		});
		Checkbox cb_ = new Checkbox("Overwrite", true);
		cb_.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e_) {
				setPaste(((Checkbox)e_.getSource()).getState());
			}
		});
		
		// west panel: switches
		panelMsg.setLayout(new GridLayout(5,1));
		panelMsg.add(cbMsgTrace);
		panelMsg.add(cbMsgDebug); 
		panelMsg.add(cbMsgGarbage); 
		panelMsg.add(cbMsgEvent);
		panelMsg.add(cbMsgError); 
		panelExe.setLayout(new GridLayout(5,1));
		panelExe.add(cbExeTrace); cbExeTrace.addItemListener(myItemListener);
		panelExe.add(cbExeDebug); cbExeDebug.addItemListener(myItemListener);
		panelExe.add(cbExeGarbage); cbExeGarbage.addItemListener(myItemListener);
		panelExe.add(cbExeEvent); cbExeEvent.addItemListener(myItemListener);
		panelExe.add(cbExeError); cbExeError.addItemListener(myItemListener);
		Panel wp_ = new Panel();
		wp_.setLayout(new BorderLayout());
		wp_.add(panelMsg, BorderLayout.NORTH);
		wp_.add(panelExe, BorderLayout.SOUTH);
		
		// south panel: buttons
		Panel p_ = new Panel();
		p_.add(cb_);
		p_.add(btnCont);
		
		main.setLayout(new BorderLayout());
		main.add(super.getDisplay(), BorderLayout.CENTER);
		main.add(p_, BorderLayout.SOUTH);
		main.add(wp_, BorderLayout.WEST);
	}
	
	public void process(Object data_, drcl.comp.Port inPort_)
	{
		if (data_ == null) return;
		try {
			if (data_ instanceof EventContract.Message && cbMsgEvent.getState()
				|| data_ instanceof DebugContract.Message && cbMsgDebug.getState()
				|| data_ instanceof GarbageContract.Message && cbMsgGarbage.getState()
				|| data_ instanceof ErrorContract.Message && cbMsgError.getState()) {
				super.process(data_, inPort_);
				
				// hold the execution
				if (data_ instanceof EventContract.Message && cbExeEvent.getState()
					|| data_ instanceof DebugContract.Message && cbExeDebug.getState()
					|| data_ instanceof GarbageContract.Message && cbExeGarbage.getState()
					|| data_ instanceof ErrorContract.Message && cbExeError.getState()) {
					synchronized (this) {
						btnCont.setEnabled(true);
						wait();
						btnCont.setEnabled(false);
					}
				}
			}
		}
		catch (Exception e_) {
			// discarded silently
		}
	}
	
	public java.awt.Component getDisplay() 
	{ return main; }
	
	/** Extends {@link drcl.comp.Component#setComponentFlag(long)} to reflect the change to GUI. */
	public void setComponentFlag(int flag_)
	{
		super.setComponentFlag(flag_);
		//cbMsgTrace.setState((flag_ & drcl.comp.Component.FLAG_TRACE_ENABLED) > 0);
		cbMsgEvent.setState((flag_ & drcl.comp.Component.FLAG_EVENT_ENABLED) > 0);
		cbMsgDebug.setState((flag_ & drcl.comp.Component.FLAG_DEBUG_ENABLED) > 0);
		cbMsgGarbage.setState((flag_ & drcl.comp.Component.FLAG_GARBAGE_ENABLED) > 0);
		cbMsgError.setState((flag_ & drcl.comp.Component.FLAG_ERROR_ENABLED) > 0);
	}
	
	/** Extends {@link drcl.comp.Component#setComponentFlag(long, boolean)} to reflect the change to GUI. */
	public void setComponentFlag(long mask_, boolean v_)
	{
		super.setComponentFlag(mask_, v_);
		long flag_ = getComponentFlag();
		//cbMsgTrace.setState((flag_ & drcl.comp.Component.FLAG_TRACE_ENABLED) > 0);
		cbMsgEvent.setState((flag_ & drcl.comp.Component.FLAG_EVENT_ENABLED) > 0);
		cbMsgDebug.setState((flag_ & drcl.comp.Component.FLAG_DEBUG_ENABLED) > 0);
		cbMsgGarbage.setState((flag_ & drcl.comp.Component.FLAG_GARBAGE_ENABLED) > 0);
		cbMsgError.setState((flag_ & drcl.comp.Component.FLAG_ERROR_ENABLED) > 0);
	}
	
	public void setTraceExeEnabled(boolean v_)
	{ cbExeTrace.setState(v_);	}
	
	public boolean isTraceExeEnabled()
	{ return cbExeTrace.getState(); }
	
	public void setEventExeEnabled(boolean v_)
	{ cbMsgEvent.setState(v_);	}
	
	public boolean isEventExeEnabled()
	{ return cbMsgEvent.getState(); }
	
	public void setDebugExeEnabled(boolean v_)
	{ cbMsgDebug.setState(v_);	}
	
	public boolean isDebugExeEnabled()
	{ return cbMsgDebug.getState(); }
	
	public void setGarbageExeEnabled(boolean v_)
	{ cbMsgGarbage.setState(v_);	}
	
	public boolean isGarbageExeEnabled()
	{ return cbMsgGarbage.getState(); }
	
	public void setErrorExeEnabled(boolean v_)
	{ cbMsgError.setState(v_);	}
	
	public boolean isErrorExeEnabled()
	{ return cbMsgError.getState(); }
}
