// @(#)TopologyVisRelaxer.java   7/2003
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

package drcl.net.graph;

// This class is based on Graph.java in the demo package included in Sun's
// J2SDK distribution and the following is the copyright notice and disclaimer
// in the original Graph.java.
/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Plots the graph changed by {@link Relaxer}. */
public class TopologyVisRelaxer implements ActionListener
{
	public static boolean DEBUG = false;

	class GraphPanel extends JPanel
		implements Runnable, MouseListener, MouseMotionListener
	{
		TopologyVisRelaxer tr;
		Relaxer relaxer;
		Thread thread;

		GraphPanel(TopologyVisRelaxer tr)
		{
			this.tr = tr;
			addMouseListener(this);
		}

		public void run()
		{
			Thread current_ = Thread.currentThread();

			while (thread == current_) {
				relaxer.doit();
				repaint();
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e_) {
					break;
				}
			}
		}


		Node pick;
		boolean pickfixed;
		Image offscreen;
		Dimension offscreensize;
		boolean showLength;

		final Color fixedColor = Color.red;
		final Color selectColor = Color.pink;
		final Color edgeColor = Color.black;
		final Color nodeColor = new Color(250, 220, 100);
		final Color stressColor = Color.darkGray;
		final Color arcColor1 = Color.black;
		final Color arcColor2 = Color.pink;
		final Color arcColor3 = Color.red;

		public void paintComponent(Graphics g)
		{
			if (relaxer == null) return;
			super.paintComponent(g);
			Dimension d = getSize();
	
			for (int i = 0 ; i < relaxer.nedges ; i++) {
				Link e = relaxer.edges[i];
				Relaxer.MyNode nn1 = relaxer.mynodes[e.n1.id];
				Relaxer.MyNode nn2 = relaxer.mynodes[e.n2.id];
				if (!nn1.showing || !nn2.showing) continue;
				int x1 = (int)e.n1.x;
				int y1 = (int)e.n1.y;
				int x2 = (int)e.n2.x;
				int y2 = (int)e.n2.y;
				int len = (int)Math.abs(Math.sqrt((x1-x2)*(x1-x2)
										+ (y1-y2)*(y1-y2)));
				Color color_ = len < 10 ? arcColor1:
						len < 20 ? arcColor2: arcColor3;
				g.setColor(color_);
				g.drawLine(x1, y1, x2, y2);
				if (showLength && color_ != arcColor1) {
					String lbl = String.valueOf(len);
					g.setColor(stressColor);
					g.drawString(lbl, x1 + (x2-x1)/2, y1 + (y2-y1)/2);
					g.setColor(edgeColor);
				}
			}

			FontMetrics fm = g.getFontMetrics();
			for (int i = 0 ; i < relaxer.nnodes ; i++) {
				Node n = relaxer.nodes[i];
				if (!relaxer.mynodes[n.id].showing) continue;
				int x = (int)n.x;
				int y = (int)n.y;
				g.setColor((n == pick) ? selectColor :
						(relaxer.mynodes[n.id].fixed ? fixedColor : nodeColor));
				String lbl = n.getName();
				int w = fm.stringWidth(lbl) + 10;
				int h = fm.getHeight() + 4;
				g.fillRect(x - w/2, y - h / 2, w, h);
				g.setColor(Color.black);
				g.drawRect(x - w/2, y - h / 2, w-1, h-1);
				g.drawString(lbl, x - (w-10)/2, (y - (h-4)/2) + fm.getAscent());
			}
		}

		// adjust edges' length
		//public void respondsToResize(double SCALE_RATIO)
		public void respondsToResize()
		{
			Dimension d = getSize();
			if (DEBUG) System.out.println("resize: " + d);
			if (relaxer == null) return;
			relaxer.config(d.width, d.height);
			/*
			double maxLen_ = 0;
			for (int i = 0 ; i < relaxer.nedges ; i++) {
				if (relaxer.edges[i].len > maxLen_)
					maxLen_ = relaxer.edges[i].len;
			}

			double scale_ = Math.min(d.width, d.height) / maxLen_ * SCALE_RATIO;
			for (int i = 0 ; i < relaxer.nedges ; i++)
				relaxer.edges[i].len = relaxer.edges[i].len * scale_;
			*/
			repaint();
		}

		public void setStep(double step_)
		{
			relaxer.setStep(step_);
		}

		// best pick is put in 'pick'
		double bestPick(int x, int y)
		{
			double shortest_ = Double.POSITIVE_INFINITY;
			for (int i = 0 ; i < relaxer.nnodes ; i++) {
				Node n = relaxer.nodes[i];
				double dist_ = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
				if (dist_ < shortest_) {
					pick = n;
					shortest_ = dist_;
				}
			}
			return shortest_;
		}

		public void mouseClicked(MouseEvent e)
		{
			if (relaxer == null) return;
			if (bestPick(e.getX(), e.getY()) < 100) {
				if (DEBUG) System.out.println("clicked: " + pick);
				relaxer.mynodes[pick.id].fixed = !relaxer.mynodes[pick.id].fixed;
			}
			else
				pick = null;
			repaint();
			e.consume();
		}

		public void mousePressed(MouseEvent e)
		{
			if (relaxer == null) return;
			int x = e.getX();
			int y = e.getY();
			if (bestPick(x, y) < 100) {
				if (DEBUG) System.out.println("pressed: " + pick);
				pickfixed = relaxer.mynodes[pick.id].fixed;
				relaxer.mynodes[pick.id].fixed = true;
				pick.x = x;
				pick.y = y;
				addMouseMotionListener(this);
			}
			else
				pick = null;
			repaint();
			e.consume();
		}

		public void mouseReleased(MouseEvent e)
		{
			if (relaxer == null) return;
			if (pick != null) {
				removeMouseMotionListener(this);
				pick.x = e.getX();
				pick.y = e.getY();
				relaxer.mynodes[pick.id].fixed = pickfixed;
				if (DEBUG) System.out.println("released: " + pick);
				pick = null;
			}
			repaint();
			e.consume();
		}

		public void mouseEntered(MouseEvent e)
		{}

		public void mouseExited(MouseEvent e)
		{}

		public void mouseDragged(MouseEvent e)
		{
			if (pick != null) {
				pick.x = e.getX();
				pick.y = e.getY();
				repaint();
			}
			e.consume();
		}

		public void mouseMoved(MouseEvent e)
		{}

		public void start()
		{
			thread = new Thread(this);
			thread.start();
		}

		public void stop()
		{
			thread = null;
		}
	}


	GraphPanel panel;
	JPanel controlPanel;

	double prevStep = 1.0;
	JButton scramble = new JButton("Scramble");
	JToggleButton shake = new JToggleButton("Shake");
	JToggleButton exec = new JToggleButton("Stop");
	JToggleButton showLength = new JToggleButton("Show Length");
	JToggleButton random = new JToggleButton("Random");
	JToggleButton stabilize = new JToggleButton("Stabilize");
	JTextField tfStep = new JTextField(prevStep+"", 5);

	public TopologyVisRelaxer(int width_, int height_)
	{
		panel = new GraphPanel(this);
		controlPanel = new JPanel();

		controlPanel.add(scramble); scramble.addActionListener(this);
		controlPanel.add(exec); exec.addActionListener(this);
		controlPanel.add(showLength); showLength.addActionListener(this);
		controlPanel.add(random); random.addActionListener(this);
		controlPanel.add(shake); shake.addActionListener(this);
		controlPanel.add(stabilize); stabilize.addActionListener(this);
		controlPanel.add(new Label("Step"));
		controlPanel.add(tfStep); tfStep.addActionListener(this);

		panel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				panel.respondsToResize();
							//Double.valueOf(tfRatio.getText()).doubleValue());
			}
		});

		JFrame f_ = new JFrame("Topology Visualization");
		f_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f_.getContentPane().add(panel, BorderLayout.CENTER);
		f_.getContentPane().add(controlPanel, BorderLayout.SOUTH);
		f_.pack();
		f_.setSize(width_, height_);
		f_.show();
	}

	public int getWidth()
	{ return panel.getWidth(); }

	public int getHeight()
	{ return panel.getHeight(); }

	public void visualize(Relaxer relaxer_)
	{
		panel.relaxer = relaxer_;
		//panel.respondsToResize(0.3);
		panel.setStep(prevStep);
		panel.start();
	}

	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();

		if (src == scramble) {
			Dimension d = panel.getSize();
			for (int i = 0 ; i < panel.relaxer.nnodes ; i++) {
				Node n = panel.relaxer.nodes[i];
				Relaxer.MyNode nn = panel.relaxer.mynodes[i];
				if (!nn.fixed) {
					n.x = 10 + (d.width-20)*Math.random();
					n.y = 10 + (d.height-20)*Math.random();
				}
			}
		}
		else if (src == shake) {
			if (shake.isSelected() && stabilize.isSelected()) {
				stabilize.setSelected(false);
				tfStep.setText(prevStep+"");
			}
			tfStep.setEnabled(!shake.isSelected());
			if (shake.isSelected()) {
				prevStep = Double.valueOf(tfStep.getText()).doubleValue();
				tfStep.setText("6");
			}
			else 
				tfStep.setText(prevStep+"");
			panel.setStep(Double.valueOf(tfStep.getText()).doubleValue());
			/*
			double w = getWidth() * 0.2;
			double h = getHeight() * 0.2;
			double w2 = w/2;
			double h2 = h/2;
			for (int i = 0 ; i < panel.relaxer.nnodes ; i++) {
				Node n = panel.relaxer.nodes[i];
				Relaxer.MyNode nn = panel.relaxer.mynodes[i];
				if (!nn.fixed) {
					n.x += w*Math.random() - w2;
					n.y += h*Math.random() - h2;
				}
			}
			*/
		}
		else if (src == exec) {
			if (exec.isSelected())
				panel.stop();
			else
				panel.start();
		}
		else if (src == showLength)
			panel.showLength = showLength.isSelected();
		else if (src == random)
			panel.relaxer.setRandom(random.isSelected());
		else if (src == stabilize) {
			if (shake.isSelected() && stabilize.isSelected()) {
				shake.setSelected(false);
				tfStep.setText(prevStep+"");
			}
			tfStep.setEnabled(!stabilize.isSelected());
			if (stabilize.isSelected()) {
				prevStep = Double.valueOf(tfStep.getText()).doubleValue();
				tfStep.setText("0.05");
			}
			else 
				tfStep.setText(prevStep+"");
			panel.setStep(Double.valueOf(tfStep.getText()).doubleValue());
		}
		else if (src == tfStep) {
			//panel.respondsToResize(
			//				Double.valueOf(tfRatio.getText()).doubleValue());
			panel.setStep(Double.valueOf(tfStep.getText()).doubleValue());
		}
	}
}
