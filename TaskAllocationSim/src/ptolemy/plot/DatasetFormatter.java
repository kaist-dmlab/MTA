// DRCL: modified from PlotFormatter.java

/* A panel for controlling the format of a dataset.

 Copyright (c) 1998-2001 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY
@ProposedRating Yellow (eal@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/

package ptolemy.plot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import ptolemy.gui.ComponentDialog;

//////////////////////////////////////////////////////////////////////////
//// DatasetFormatter
/**

DatasetFormatter is a panel that controls the format of a plotter object
passed to the constructor.

@see Plot
@see PlotBox
@author Edward A. Lee
@version $Id: DatasetFormatter.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $
*/
public class DatasetFormatter extends JPanel
	implements ActionListener, ItemListener, FocusListener
{
    /** Construct a plot formatter for the specified plot object.
     */
    public DatasetFormatter(PlotBox plot_)
	{
        super();
        _plot = (Plot)plot_;

		int n = _plot.getNumDataSets();
		String[] marks_ = {"none", "points", "dots", "various", "pixels"};
		Box box_ = Box.createVerticalBox();
		for (int i=0; i<n; i++) {
			String id_ = String.valueOf(i);
			JPanel panel_ = new JPanel();
			panel_.add(new JLabel(i + ": Legend"));
			JTextField legend_ = new JTextField(_plot.getLegend(i), 12);
			legend_.setName(id_);
			legend_.addFocusListener(this);
			legend_.addActionListener(this);
			panel_.add(legend_);

			panel_.add(new JLabel(" Mark"));
			JComboBox mark_ = new JComboBox(marks_);
			mark_.setName(id_);
			mark_.addActionListener(this);
			mark_.setSelectedItem(_plot.getMarksStyle(i));
			panel_.add(mark_);

			JCheckBox connect_ = new JCheckBox("connect", _plot.getConnected(i));
			connect_.setName(id_);
			connect_.addItemListener(this);
			panel_.add(connect_);

			JCheckBox stepwise_ = new JCheckBox("stepwise", _plot.getStepwise(i));
			stepwise_.setName(id_);
			stepwise_.addItemListener(this);
			panel_.add(stepwise_);
			box_.add(panel_);
		}

		add(box_);
    }

    public void focusGained(FocusEvent e_)
	{/*do nothing, see focusLost()*/}

    public void focusLost(FocusEvent e_)
	{
		JTextField legend_ = (JTextField)e_.getSource();
		_setLegend(legend_);
	}

    public void actionPerformed(ActionEvent e_)
	{
        Component c_ = (Component)e_.getSource();
		if (c_ instanceof JComboBox) {
			JComboBox cb_ = (JComboBox)c_;
			int dataset_ = Integer.parseInt(cb_.getName());
			String mark_ = (String)cb_.getSelectedItem();
			_plot.setMarksStyle(mark_, dataset_);
			_plot.repaint();
		}
		else if (c_ instanceof JTextField) {
			JTextField legend_ = (JTextField)c_;
			_setLegend(legend_);
		}
    }

	void _setLegend(JTextField legend_)
	{
		int dataset_ = Integer.parseInt(legend_.getName());
		_plot.setLegend(dataset_, legend_.getText());
	}

	public void itemStateChanged(ItemEvent e_)
	{
        Component c_ = (Component)e_.getSource();
		if (c_ instanceof JCheckBox) {
			JCheckBox cb_ = (JCheckBox)c_;
			int dataset_ = Integer.parseInt(cb_.getName());
			boolean state_ = cb_.isSelected();
			if (cb_.getText().equals("connect"))
				_setConnected(state_, dataset_);
			else
				_plot.setStepwise(state_, dataset_);
			_plot.repaint();
		}
	}

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Open a format control window as a top-level, modal dialog.
     */
    public void openModal() {
        String[] buttons = {"OK"};
        // NOTE: If the plot is in a top-level container that is a Frame
        // (as opposed to an applet), then tell the dialog that the Frame
        // owns the dialog.
        Container toplevel = _plot.getTopLevelAncestor();
        Frame frame = null;
        if (toplevel instanceof Frame) frame = (Frame)toplevel;
        ComponentDialog dialog =
            new ComponentDialog(frame, "Set DataSet Format", this, buttons);

        if (dialog.buttonPressed().equals("OK")) {
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /** @serial The plot object controlled by this formatter. */
    protected final Plot _plot;

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // Set the current connected state of all the point in the
    // plot.  NOTE: This method reaches into the protected members of
    // the Plot class, taking advantage of the fact that this class is
    // in the same package.
	private void _setConnected(boolean value, int dataset)
	{
		// Make sure the default matches.
		_plot.setConnected(value, dataset);
		PlotPoint[] pts_ = _plot.getPoints(dataset);
		boolean first = true;
		for (int i = 0; i < pts_.length; i++) {
			pts_[i].connected = value && !first;
			first = false;
        }
    }
}
