// @(#)PlotPlain.java   11/2003
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

package drcl.comp.tool;

import java.io.*;
import java.util.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.comp.contract.*;

/** 
 * The class works similar to {@link Plotter} except that the results
 * are outputted to files in a simple format of "x-value{ValueSeparator}y-value"
 * per line.  By default, {ValueSeparator} is a whitespace.  One can change
 * this by {@link #setValueSeparator(String)}.
 *
 * By default, the result files are named with 
 * {prefix}{port_group}{separator}{port_id}.  One can change these by
 * {@link #setFilePrefix(String)} and {@link #setFileSeparator(String)}.
 * One can also associate incoming ports with files of specific names using
 * {@link #associates(Port, String)}. 
 */
public class PlotPlain extends drcl.comp.Extension
{
	String VALUE_SEPARATOR = " ";
	String FILE_SEPARATOR = "_";
	String FILE_PREFIX = "data";
	
	HashMap htWriter = new HashMap(); // port -> PrintWriter

	public PlotPlain()
	{ super(); }
	
	public PlotPlain(String id_)
	{ super(id_); }

	public void setValueSeparator(String s)
	{ VALUE_SEPARATOR = s; }

	public String getValueSeparator()
	{ return VALUE_SEPARATOR; }

	public void setFilePrefix(String s)
	{ FILE_PREFIX = s; }

	public void setFileSeparator(String s)
	{ FILE_SEPARATOR = s; }

	public String getFileSeparator()
	{ return FILE_SEPARATOR; }

	public String getFilePrefix()
	{ return FILE_PREFIX; }

	public synchronized void flush()
	{
		for (Iterator it_ = htWriter.values().iterator(); it_.hasNext(); ) {
			Object o = it_.next();
			try {
			PrintWriter w = (PrintWriter)o;
				w.flush();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println(o);
			}
		}
	}

	public synchronized void reset()
	{
		super.reset();
		for (Iterator it_ = htWriter.values().iterator(); it_.hasNext(); ) {
			PrintWriter w = (PrintWriter)it_.next();
			try {
				w.close();
			}
			catch (Exception e)
			{}
		}
		htWriter.clear();
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof PlotPlain)) return;
		FILE_PREFIX = ((PlotPlain)source_).FILE_PREFIX;
		FILE_SEPARATOR = ((PlotPlain)source_).FILE_SEPARATOR;
		VALUE_SEPARATOR = ((PlotPlain)source_).VALUE_SEPARATOR;
	}
	
	protected void process(Object data_, Port inPort_) 
	{
		int figID_=0, setID_=0;
		try {
			figID_ = Integer.parseInt(inPort_.groupID);
			setID_ = Integer.parseInt(inPort_.id);
		}
		catch (Exception e_) {
			// ignored
		}
		if (data_ instanceof DoubleEventContract.Message) {
			DoubleEventContract.Message s_ = (DoubleEventContract.Message)data_;
			_plot(inPort_, s_.getTime(), s_.getValue());
		}
		else if (data_ instanceof EventContract.Message) {
			EventContract.Message s_ = (EventContract.Message)data_;
			Object evt_ = s_.getEvent();
			double x_ = 0.0, y_ = 0.0;
			if (evt_ instanceof Double) {
				x_ = s_.getTime();
				y_ = ((Double)evt_).doubleValue();
			}
			else if (evt_ instanceof DoubleObj) {
				x_ = s_.getTime();
				y_ = ((DoubleObj)evt_).value;
			}
			else if (evt_ instanceof double[]) {
				double[] xy_ = (double[])evt_;
				if (xy_.length >= 2) {
					x_ = xy_[0];  y_ = xy_[1];
				}
				else if (xy_.length == 1) {
					x_ = s_.getTime();  y_ = xy_[0];
				}
				else {
					error(data_, "process()", inPort_, "zero-length double array");
					return;
				}
			}
			else {
				error(data_, "process()", inPort_, "unrecognized event object: " + evt_);
				return;
			}
			_plot(inPort_, x_, y_);
		}
		else if (data_ instanceof String) {
			_plot(inPort_, (String)data_);
		}
		else if (data_ instanceof double[]) {
			double[] xy_ = (double[])data_;
			double x_ = 0.0, y_ = 0.0;
			if (xy_.length == 0) {
				error(data_, "process()", inPort_, "zero-length double array");
				return;
			}
			else if (xy_.length == 1) {
				x_ = getTime();  y_ = xy_[0];
			}
			else {
				x_ = xy_[0];  y_ = xy_[1];
			}
			_plot(inPort_, x_, y_);
		}
		else if (data_ instanceof Double || data_ instanceof DoubleObj) {
			double y_ = data_ instanceof Double?
										((Double)data_).doubleValue():
										((DoubleObj)data_).value;
			_plot(inPort_, getTime(), y_);
		}
		else {
			error(data_, "process()", inPort_, "unrecognized data");
		}
	}
	
	synchronized void _plot(Port inPort_, double x_, double y_)
	{
		try {
			PrintWriter w = (PrintWriter)htWriter.get(inPort_);
			if (w == null) {
				w = new PrintWriter(new FileWriter(
									FILE_PREFIX + inPort_.getGroupID()
									+ FILE_SEPARATOR + inPort_.getID()));
				htWriter.put(inPort_, w);
			}
			w.println(x_ + VALUE_SEPARATOR + y_);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	synchronized void _plot(Port inPort_, String line_)
	{
		try {
			PrintWriter w = (PrintWriter)htWriter.get(inPort_);
			if (w == null) {
				w = new PrintWriter(new FileWriter(
									FILE_PREFIX + inPort_.getGroupID()
									+ FILE_SEPARATOR + inPort_.getID()));
				htWriter.put(inPort_, w);
			}
			w.println(line_);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Associates the incoming port with the result file. */
	public synchronized void associates(Port p, String fileName_)
	{
		try {
			htWriter.put(p, new PrintWriter(new FileWriter(fileName_)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
