// @(#)DrclPlotFrame.java   9/2002
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

import ptolemy.plot.*;
import java.awt.event.*;
import java.awt.MenuBar;
import javax.swing.JOptionPane;
import javax.swing.JMenuBar;

/**
 * @author Hung-ying Tyan
 */
public class DrclPlotFrame extends ptolemy.plot.plotml.PlotMLFrame
{
    /** Construct a plot frame with a default title and by default contains
     *  an instance of Plot. After constructing this, it is necessary
     *  to call setVisible(true) to make the plot appear.
     */
    public DrclPlotFrame() {
        this("Drcl Plot Frame");
    }

    /** Construct a plot frame with the specified title and by default
     *  contains an instance of Plot. After constructing this, it is necessary
     *  to call setVisible(true) to make the plot appear.
     *  @param title The title to put on the window.
     */
    public DrclPlotFrame(String title) {
        this(title, null);
    }

    /** Construct a plot frame with the specified title and the specified
     *  instance of PlotBox.  After constructing this, it is necessary
     *  to call setVisible(true) to make the plot appear.
     *  @param title The title to put on the window.
     *  @param plotArg the plot object to put in the frame, or null to create
     *   an instance of Plot.
     */
    public DrclPlotFrame(String title, PlotBox plotArg) {
        super(title, plotArg);
        setJMenuBar(null);
		_init();
		pack();
    }

	boolean menuUp = false;
	boolean buttonEnabled = false;

	JMenuBar _getMenuBar()
	{ return _menubar; }

	void _init()
	{
		plot.removeKeyListener(plot.myKeyListener);
		plot.myKeyListener = new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				switch(e.getKeyChar()) {
				case 'C':
					// The "null" sends the output to the clipboard.
					plot.export(null);
					String message = "Encapsulated PostScript (EPS) exported to clipboard.";
					JOptionPane.showMessageDialog(plot, message, "Ptolemy Plot Message",
						JOptionPane.INFORMATION_MESSAGE);
					break;
				case 'D':
					plot.write(System.out);
					message = "Plot data sent to standard out.";
					JOptionPane.showMessageDialog(plot, message, "Ptolemy Plot Message",
						JOptionPane.INFORMATION_MESSAGE);
					break;
				case 'E':
					plot.export(System.out);
					message = "Encapsulated PostScript (EPS) exported to standard out.";
					JOptionPane.showMessageDialog(plot, message, "Ptolemy Plot Message",
						JOptionPane.INFORMATION_MESSAGE);
					break;
				case 'F':
					plot.fillPlot();
					break;
				case 'M':
					setJMenuBar(menuUp? null: _getMenuBar());
					DrclPlotFrame.this.validate();
					menuUp = !menuUp;
					break;
				case 'T':
					buttonEnabled = !buttonEnabled;
					plot.setButtons(buttonEnabled);
					plot.validate();
					break;
				case '?':
				case 'H':
					_help();
					break;
				default:
					// None
					break;
				}
			}
		};
		plot.addKeyListener(plot.myKeyListener);
	}

   protected void _help() {
        String message = 
                "Ptolemy Plot\n" +
				"Modified By: Hung-ying Tyan, tyanh@ee.eng.ohio-state.edu\n" +
                "Originally By: Edward A. Lee, eal@eecs.berkeley.edu\n" +
                "and Christopher Hylands, cxh@eecs.berkeley.edu\n" +
				"Version " + PlotBox.PTPLOT_RELEASE +
				", Build: $Id: DrclPlotFrame.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $\n\n" +
                "Key bindings:\n" +
                "   Shift-D: dump plot data to standard out (PlotML)\n" +
                "   Shift-E: export plot to standard out (EPS format)\n" +
                "   Shift-F: fill plot\n" +
                "   Shift-H or ?: print help message (this message)\n" +
                "   Shift-M: toggle menu\n" +
                "   Shift-T: toggle toolbar\n" +
                "For more information, see\n" +
                "http://ptolemy.eecs.berkeley.edu/java/ptplot\n";
         JOptionPane.showMessageDialog(plot, message,
                "Ptolemy Plot Help Window", JOptionPane.INFORMATION_MESSAGE);
    }
}
