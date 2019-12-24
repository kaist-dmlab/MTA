// @(#)HistogramPlotter.java   9/2002
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

import java.io.*;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ptolemy.plot.Histogram;
import ptolemy.plot.plotml.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.comp.contract.*;
import drcl.util.StringUtil;

public class HistogramPlotter extends drcl.comp.Extension
{
	static final String SEPARATOR = "--";
	
	Histogram[] plots = null;//new Histogram[] {new Histogram(), new Histogram()};
	int[] firstPoint = null;
	
	boolean plotEnabled = true, outEnabled = true;
	Port outport = addPort(".output", false/*not removable*/);
	//BufferedWriter out = null;
	
	double binWidth = 1.0;

	public HistogramPlotter()
	{ super(); }
	
	public HistogramPlotter(String id_)
	{ super(id_); }
	
	public synchronized void reset()
	{
		super.reset();
		if (firstPoint != null)
			for (int i=0; i<firstPoint.length; i++)
				firstPoint[i] = 0;
		if (isEnabled() && plotEnabled && plots != null) {
			for (int i=0; i<plots.length; i++)
				if (plots[i] != null) {
					//plots[i].clear(false);
					java.awt.Component c_ = plots[i].getParent();
					while (c_ != null && !(c_ instanceof DrclPlotFrame))
						c_ = c_.getParent();
					if (c_ != null) ((DrclPlotFrame)c_).dispose();
					plots[i] = null;
				}
		}
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof HistogramPlotter)) return;
	}
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		if (plots != null)
			for (int i=0; i<plots.length; i++)
				if (plots[i] != null) {
					int sum_ = 0;
					for (int j=0; j<plots[i].getNumDataSets(); j++)
						sum_ += plots[i].getNumPoints(j);
					sb_.append(plots[i].getTitle() + ": " + sum_ + " points.\n");
				}
		if (sb_.length() == 0)
			return "No plot is created.\n";
		return sb_.toString();
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
			_plot(figID_, setID_, s_.getValue(), s_.getEventName(), s_.getPortPath());
		}
		else if (data_ instanceof EventContract.Message) {
			EventContract.Message s_ = (EventContract.Message)data_;
			Object evt_ = s_.getEvent();
			double y_ = 0.0;
			if (evt_ instanceof Double) {
				y_ = ((Double)evt_).doubleValue();
			}
			else if (evt_ instanceof DoubleObj) {
				y_ = ((DoubleObj)evt_).value;
			}
			else if (evt_ instanceof double[]) {
				double[] yy_ = (double[])evt_;
				for (int j=0; j<yy_.length; j++)
					_plot(figID_, setID_, yy_[j], s_.getEventName(), s_.getPortPath());
				return;
			}
			else {
				error(data_, "process()", inPort_, "unrecognized event object: " + evt_);
				return;
			}
			_plot(figID_, setID_, y_, s_.getEventName(), s_.getPortPath());
		}
		else if (data_ instanceof String) {
			String s_ = (String)data_;
			if (s_.indexOf("\n") >= 0) {
				StringReader sr_ = new StringReader(s_);
				plotsLoad(sr_);
			}
			else {
				double y_ = _rawPlotsParseLine(s_);
				_plot(figID_, setID_, y_, null, null);
			}
		}
		else if (data_ instanceof double[]) {
			double[] yy_ = (double[])data_;
			for (int j=0; j<yy_.length; j++)
				_plot(figID_, setID_, yy_[j], null, null);
		}
		else if (data_ instanceof Double || data_ instanceof DoubleObj) {
			double y_ = data_ instanceof Double?
				((Double)data_).doubleValue(): ((DoubleObj)data_).value;
			_plot(figID_, setID_, y_, null, null);
		}
		else {
			error(data_, "process()", inPort_, "unrecognized data");
		}
	}
	
	synchronized void _plot(int figID_, int setID_, double y_, Object titleObj_, Object legendObj_)
	{
		// check if the plot is available
		boolean justCreated_ = false;
		String title_ = titleObj_ == null? "<No Title>": titleObj_.toString();
		String legend_ = legendObj_ == null? "???": legendObj_.toString();
		synchronized(this) {
			if (plotEnabled
				&& (plots == null || plots.length <= figID_ || plots[figID_] == null)) {
				addPlot(figID_, title_);
				justCreated_ = true;
			}
			if (firstPoint == null || firstPoint.length <= figID_) {
				int[] btmp_ = new int[figID_+1];
				if (firstPoint != null)
					System.arraycopy(firstPoint, 0, btmp_, 0, firstPoint.length);
				firstPoint = btmp_;
			}
				
			if (firstPoint[figID_] == 0) {// new plot
				firstPoint[figID_] = -1; // all one's 
				if (outEnabled)
					write("###   NEW PLOT" + SEPARATOR + figID_ + SEPARATOR
						  + title_ + "\n");
			}
		}
		
		Object syncObj_ = plots != null && plots.length > figID_ && plots[figID_] != null?
						  (Object)plots[figID_]: (Object)this;
		synchronized (syncObj_) {
			boolean firstPoint_ = (firstPoint[figID_] & (1<<setID_)) > 0;
			Histogram plot_ = plotEnabled? plots[figID_]: null;
			if (firstPoint_) {
				if (plotEnabled && plot_ != null) {
					// plot legend
					if (plot_.getLegend(setID_) == null) {
						plot_.addLegend(setID_, legend_);
						/*
						String path_ = legend_;
						if (path_.endsWith("/")) path_ = path_.substring(0, path_.length()-1);
						if (path_.indexOf("@") >= 0) {
							int index1_ = path_.lastIndexOf("/");
							if (index1_ < 0)
								plot_.addLegend(setID_, path_);
							else {
								int index2_ = path_.substring(0, index1_).lastIndexOf("/");
								if (index2_ < 0)
									plot_.addLegend(setID_, path_);
								else
									plot_.addLegend(setID_, path_.substring(index2_+1));
							}
						}
						else
							plot_.addLegend(setID_, StringUtil.lastSubstring(path_, "/"));
							*/
					}
				}
				if (outEnabled) {
					// legend
					write("#####  NEW SET" + SEPARATOR + figID_ + SEPARATOR
						  + setID_ + SEPARATOR + legend_ + "\n");
				}
				firstPoint[figID_] &= ~(1<<setID_);
			} // end if (firstPoint_)
				
			if (plotEnabled && plot_ != null) {
				plot_.addPoint(setID_, y_);
				if (Math.random() > .75) plot_.repaint();
			}
				
			if (outEnabled) {
				write(figID_ + SEPARATOR + setID_ 
					  + SEPARATOR + y_ + SEPARATOR + legend_ + "\n");
			}
		}
	}
	
	// write to out and flush
	void write(String line_)
	{
		try {
			outport.doSending(line_);
		}
		catch (Exception e_) {
			error(line_, "write()", null, e_);
		}
	}
	
	//
	private void ___SCRIPT___() {}
	//
	
	public boolean isPlotEnabled()
	{ return plotEnabled; }
	
	public void setPlotEnabled(boolean enabled_)
	{ plotEnabled = enabled_; }
	
	public boolean isOutputEnabled()
	{ return outEnabled; }
	
	public void setOutputEnabled(boolean enabled_)
	{ outEnabled = enabled_; }
	
	/*
	public void setOutput(Writer out_)
	{ out = new BufferedWriter(out_); }
	
	public Writer setOutFile(String fname_)
	{
		try {
			FileWriter out_ = new FileWriter(fname_);
			setOutput(out_);
		}
		catch (Exception e_) {
			if (isErrorNoticeEnabled())
				error("setOutFile()", e_);
		}
		return out;
	}
	*/
	
	// plot_: plot id, automatically allocate if negative
	// return plot id
	public int addPlot(int plot_, String title_)
	{
		if (plots == null) plots = new Histogram[3];
		if (plot_ < 0)
			for (int i=0; i<plots.length; i++)
				if (plots[i] == null) { plot_ = i; break; }
		if (plots.length <= plot_) {
			Histogram[] tmp_ = new Histogram[Math.max(plot_ + 1, plots.length + 3)];
			System.arraycopy(plots, 0, tmp_, 0, plots.length);
			plots = tmp_;
		}
		if (plots[plot_] == null) {
			Histogram tmp_ = new Histogram();
			tmp_.setBinWidth(binWidth);
			tmp_.setTitle(title_ == null? "No Title": title_);
			tmp_.setXLabel("- Bin - "); // let users change this later
			DrclPlotFrame f_ = new DrclPlotFrame(this + " -- Histogram " + plot_, tmp_);
			f_.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt_) {
					Frame frame_ = (Frame)evt_.getSource();
					for (int i=0; i<plots.length; i++)
						if (plots[i] != null && plots[i].getParent() == frame_) {
							firstPoint[i] = 0;
							plots[i] = null;
							frame_.dispose();
						}
				}
				});
			f_.show();
			plots[plot_] = tmp_;
		}
		
		return plot_;
	}
	
	public Histogram getPlot(int plot_)
	{
		if (plots == null || plot_ >= plots.length || plots[plot_] == null)
			return plots[addPlot(plot_, "Histogram " + plot_)];
		else
			return plots[plot_];
	}
	
	public void fill(int plot_)
	{
		getPlot(plot_).fillPlot();
	}
	
	public void repaint(int plot_)
	{
		getPlot(plot_).repaint();
	}

	/** Re-displays the plot if it is not shown on the screen. */
	public void show(int plot_)
	{
		if (plots == null || plot_ >= plots.length)
			return;
		java.awt.Component c_ = plots[plot_];
		while (c_ != null && !(c_ instanceof Frame))
			c_ = c_.getParent();
		if (c_ != null) c_.setVisible(true);
	}

	/** Re-displays all the plots if they are not shown on the screen. */
	public void showAll()
	{
		if (plots == null) return;
		for (int i=0; i<plots.length; i++)
			show(i);
	}	

	public void addLegend(int plot_, int dataset_, String legend_)
	{
		Histogram p_ = getPlot(plot_);
		p_.addLegend(dataset_, legend_);
		p_.repaint();
	}
	
	public void setLegend(int plot_, int dataset_, String legend_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setLegend(dataset_, legend_);
		p_.repaint();
	}
	
	public void exportEPS(int plot_, String fileName_)
	{
		if (plots == null || plot_ >= plots.length || plots[plot_] == null)
			return; // XXX: warning
		try {
			java.io.FileOutputStream file_ = new java.io.FileOutputStream(fileName_);
			plots[plot_].export(file_);
			file_.close();
		}
		catch (Exception e_) {
			error("exportEPS()", e_);
		}
	}
	
	public void setTitle(int plot_, String title_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setTitle(title_);
		p_.repaint();
	}
	
	public void setXLabel(int plot_, String label_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setXLabel(label_);
		p_.repaint();
	}
	
	public void setXLog(int plot_, boolean enabled_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setXLog(enabled_);
		p_.repaint();
	}
	
	public void setXRange(int plot_, double min_, double max_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setXRange(min_, max_);
		p_.repaint();
	}
	
	public void setYLabel(int plot_, String label_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setYLabel(label_);
		p_.repaint();
	}
	
	public void setYLog(int plot_, boolean enabled_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setYLog(enabled_);
		p_.repaint();
	}
	
	public void setYRange(int plot_, double min_, double max_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setYRange(min_, max_);
		p_.repaint();
	}

	public double getBinWidth()
	{ return binWidth; }

	public void setBinWidth(double width_)
	{
		binWidth = width_;
		if (plots == null) return;
		for (int i=0; i<plots.length; i++)
			setBinWidthAt(i, width_);
	}

	public void setBinWidthAt(int plot_, double width_)
	{
		Histogram p_ = getPlot(plot_);
		p_.setBinWidth(width_);
		if (p_.getNumDataSets() > 0)
			p_.repaint();
	}


	/*
	public void output(String fname_)
	{
		try {
			output(new FileWriter(fname_));
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("FileName:" + fname_, "output(String)", null, e_);
		}
	}
	*/
	
	public void plotMLOutput(int plot_, String fname_)
	{
		try {
			plotMLOutput(plot_, new FileWriter(fname_));
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	public synchronized void plotMLOutput(int plot_, java.io.Writer writer_)
	{
		if (plots == null || plots.length <= plot_ || plots[plot_] == null) return;
		plots[plot_].write(writer_, null);
	}

	public synchronized void plotMLOutput(int plot_)
	{
		if (plots == null || plots.length <= plot_ || plots[plot_] == null) return;
		plots[plot_].write(new java.io.Writer() {
			public void write(char[] cc_, int offset_, int len_) {
				HistogramPlotter.this.write(new String(cc_, offset_, len_));
			}

			public void write(String msg_) {
				HistogramPlotter.this.write(msg_);
			}

			public void close() {}
			public void flush() {}
		}, null);
	}

	public synchronized void plotsOutput()
	{
		if (plots == null || plots.length == 0) return;
		
		try {
			//BufferedWriter bw_ = new BufferedWriter(out_);
			for (int i=0; i<plots.length; i++) {
				if (plots[i] == null) continue;
				Histogram plot_ = plots[i];
				String title_ = plot_.getTitle();
				write("###   NEW PLOT" + SEPARATOR + i + SEPARATOR
						  + title_ + SEPARATOR
						  + "'" + plot_.getXLabel() + "'\n");
				int ns_ = plot_.getNumDataSets();
				for (int j=0; j<ns_; j++) {
					String legend_ = plot_.getLegend(j);
					if (legend_ == null) continue;
					write("#####  NEW SET" + SEPARATOR + i + SEPARATOR
							+ j + SEPARATOR + legend_ + "\n");
					int np_ = plot_.getNumPoints(j);
					for (int k=0; k<np_; k+=2) {
						double y_ = plot_.getPoint(j, k);
						write(i + SEPARATOR + j + SEPARATOR + y_ + SEPARATOR + legend_ + "\n");
					}
				}
			}
			//bw_.close();
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("plotsOutput()", e_);
		}
	}
	
	public void load(String fname_)
	{ load(fname_, -1, 0); }

	// figID and setID are only good for raw data format
	public void load(String fname_, int figID_, int setID_)
	{
		try {
			BufferedReader bin_ = new BufferedReader(new FileReader(fname_));
			String line_;
			for (;;) {
				line_ = bin_.readLine();
				if (line_ == null || line_.length() > 0) break;
			}
			if (line_ == null) return;
			bin_.close();

			// check the first line 
			if (line_.startsWith("###")) {
				System.out.println("Drcl Histogram Format");
				plotsLoad(new FileReader(fname_));
			}
			else if (line_.startsWith("<?xml version")) {
				System.out.println("PlotML");
				plotMLLoad(fname_);
			}
			else {
				System.out.println("Raw data");
				rawPlotLoad(new FileReader(fname_), figID_, setID_);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	/**
	 * Creates a new plot from a <i>PlotML</i> file.
	 */
	public void plotMLLoad(String fname_)
	{
		// find an ID for the new plot
		int plotID_ = 0;
		if (plots != null) {
			for (; plotID_ < plots.length; plotID_++)
				if (plots[plotID_] == null) break;
		}
		plotMLLoad(fname_, plotID_);
	}

	/**
	 * Creates a new plot of the specified ID from a <i>PlotML</i> file.
	 */
	public void plotMLLoad(String fname_, int plotID_)
	{
		try {
			File file_ = new File(fname_);
			URL base_ = new URL("file", null, file_.getAbsolutePath());
			plotMLLoad(base_, new FileInputStream(file_), plotID_);
		}
		catch (Exception ex_) {
			System.err.println(ex_.toString());
			ex_.printStackTrace();
		}
	}

	/**
	 * Creates a new plot of the specified ID from a <i>PlotML</i> input stream.
	 */
	public synchronized void plotMLLoad(URL base_, InputStream in_, int plotID_)
	{
		Histogram plot_ = plots[addPlot(plotID_, "Histogram " + plotID_)];
		PlotBoxMLParser parser_ = new PlotBoxMLParser(plot_);
		try {
			parser_.parse(base_, in_);
			plots[plotID_].repaint();
		} catch (Exception ex_) {
			System.err.println(ex_.toString());
			ex_.printStackTrace();
		}
	}

	/**
	 * Creates new plots from a <i>Plots</i> reader.
	 */
	public synchronized void plotsLoad(Reader in_)
	{
		String line_ = null;
		try {
			BufferedReader bin_ = new BufferedReader(in_);
			for (;;) {
				line_ = bin_.readLine();
				if (line_ != null) {
					_plotsParseLine(line_, null);
				}
				else { // end of file
					// repaint
					if (plots != null)
						for (int i=0; i<plots.length; i++)
							if (plots[i] != null) plots[i].repaint();
					bin_.close();
					return;
				}
				
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("'" + line_ + "'", "plotsLoad(Reader)", null, e_);
		}
	}

	public synchronized void rawPlotLoad(Reader in_, int figID_, int setID_)
	{
		String line_ = null;
		try {
			BufferedReader bin_ = new BufferedReader(in_);
			double y_ = 0.0;
			figID_ = addPlot(figID_, null); 
			Histogram plot_ = plots[figID_];
			if (setID_ < 0) {
				setID_ = 0; // XX: should locate one for it
			}
			for (;;) {
				line_ = bin_.readLine();
				if (line_ != null) {
					y_ = _rawPlotsParseLine(line_);
					_plot(figID_, setID_, y_, null, null);
				}
				else { // end of file
					plot_.repaint();
					bin_.close();
					return;
				}
				
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("'" + line_ + "'", "rawPlotLoad(Reader)", null, e_);
		}
	}
	
	// parse a line in "Plots" format
	void _plotsParseLine(String line_, double[] xy_)
	{
		String[] ss_ = substrings(line_);
		if (ss_[0].startsWith("#")) {
			if (ss_[0].startsWith("####")) { // new data set
				int figID_ = Integer.parseInt(ss_[1]);
				int setID_ = Integer.parseInt(ss_[2]);
				setLegend(figID_, setID_, ss_[3]);
			}
			else { // new plot
				int figID_ = addPlot(Integer.parseInt(ss_[1]), ss_[2]);
				Histogram plot_ = plots[figID_];
				// x label
				if (ss_.length > 3) {
					String x_ = ss_[3];
					if (x_.startsWith("'") && x_.endsWith("'"))
						x_ = x_.substring(1, x_.length()-1);
					plot_.setXLabel(x_);
				}
					
				if (firstPoint == null || firstPoint.length <= figID_) {
					int[] btmp_ = new int[figID_+1];
					if (firstPoint != null)
						System.arraycopy(firstPoint, 0, btmp_, 0, firstPoint.length);
					firstPoint = btmp_;
				}
				firstPoint[figID_] = -1; // all one's 
			}
			return;
		}
		else if (ss_.length >= 4) { // add point
			int figID_ = Integer.parseInt(ss_[0]);
			int setID_ = Integer.parseInt(ss_[1]);
			double x_ = Double.valueOf(ss_[2]).doubleValue();
			double y_ = Double.valueOf(ss_[3]).doubleValue();
			Histogram plot_ = getPlot(figID_);
			
			boolean firstPoint_ = (firstPoint[figID_] & (1<<setID_)) > 0;
			if (firstPoint_)
				plot_.setLegend(setID_, ss_.length > 4? ss_[4]: "");
			plot_.addPoint(setID_, x_, y_, !firstPoint_/*connected*/);
			firstPoint[figID_] &= ~(1<<setID_);
			return;
		}
		error(line_, "_plotsParseLine()", null, "unrecognized format");
	}

	double _rawPlotsParseLine(String line_)
	{
		String[] ss_ = StringUtil.substrings(line_);

		if (ss_ == null)
			return Double.NaN;
		else if (ss_.length == 1) { // one double
			return Double.valueOf(ss_[0]).doubleValue();
		}
		else if (ss_.length == 2) { // two doubles
			// ignore first one
			return Double.valueOf(ss_[1]).doubleValue();
		}
		else
			return Double.NaN;
	}
	
	// utility method for parsing "Plots" format
	String[] substrings(String line_)
	{
		Vector v_ = new Vector();
		int i = 0;
		for (;;) {
			int j = line_.indexOf(SEPARATOR, i);
			if (j < 0) {
				String s_ = line_.substring(i);
				if (s_.length() > 0) v_.addElement(s_);
				break;
			}
			v_.addElement(line_.substring(i, j));
			i = j+2;
		}
		String[] ss_ = new String[v_.size()];
		v_.copyInto(ss_);
		return ss_;
	}
	
	public static void main(String[] args)
	{
		HistogramPlotter p_ = new HistogramPlotter("");
		boolean argument_ = true;
		int figID_ = -1, setID_ = 0;
		boolean oneFigure_ = false;
		for (int i=0; i<args.length; i++) {
			if (argument_ && args[i].startsWith("-")) {
				if (args[i].indexOf("1") > 0) {
					oneFigure_ = true;
					figID_ = 0;
					setID_ = 0;
				}
			}
			else {
				argument_ = false;
				p_.load(args[i], figID_, setID_);
				if (oneFigure_) setID_++;
			}
		}
	}
}
