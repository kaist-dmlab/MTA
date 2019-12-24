// @(#)MemoryMonitor.java   7/2003
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

package drcl.util;
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

/*
 * @(#)MemoryMonitor.java	1.32 03/01/23
 */

//package java2d;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class MemoryMonitor extends JPanel implements ActionListener, MouseListener
{

    //static JCheckBox dateStampCB = new JCheckBox("Output Date Stamp");
    public Surface surf;
    JPanel controls;
	boolean controlHidden = true;
    JTextField tf;
	JButton btnGC = new JButton("GC");
	JToggleButton btnStop = new JToggleButton("Stop");
	Runtime runtime = Runtime.getRuntime();

    public MemoryMonitor() {
        setLayout(new BorderLayout());
        //setBorder(new TitledBorder(new EtchedBorder(), "Memory Monitor"));
        add(surf = new Surface());
        controls = new JPanel();
		//add(BorderLayout.SOUTH, controls);
        //controls.setPreferredSize(new Dimension(135,80));
        Font font = new Font("serif", Font.PLAIN, 10);
        JLabel label = new JLabel("Sample Rate");
        label.setFont(font);
        label.setForeground(Color.black);
        controls.add(label);
        tf = new JTextField("1000");
        tf.setPreferredSize(new Dimension(45,20));
		tf.addActionListener(this);
        controls.add(tf);
        controls.add(label = new JLabel("ms"));
		controls.add(btnGC);
		controls.add(btnStop);
		btnGC.addActionListener(this);
		btnStop.addActionListener(this);
        label.setFont(font);
        label.setForeground(Color.black);
        //controls.add(dateStampCB);
        //dateStampCB.setFont(font);
        addMouseListener(this);
        surf.addMouseListener(this);
    }

	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		if (o == tf)
			setUpdateInterval(Long.parseLong(tf.getText().trim()));
		else if (o == btnGC)
			runtime.gc();
		else if (o == btnStop) {
			if (btnStop.isSelected())
				surf.stop();
			else
				surf.start();
		}
	}

	public void setUpdateInterval(long intervalMS_)
	{
		tf.setText(intervalMS_+"");
		surf.sleepAmount = intervalMS_;
   	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void mouseClicked(MouseEvent e)
   	{
		//System.out.println(e);
		int clickCount_ = e.getClickCount();
		if (clickCount_ >= 2) {
			controlHidden = !controlHidden;
			//System.out.println("control " + (controlHidden? "hidden": "showing"));
			if (controlHidden) {
				remove(controls);
			}
			else {
				add(BorderLayout.SOUTH, controls);
			}
		   	revalidate();
			surf.repaint();
		}
	}

    public class Surface extends JPanel implements Runnable, ComponentListener {

        public Thread thread;
        public long sleepAmount = 1000;
        private int w, h;
        private BufferedImage bimg;
        private Graphics2D big;
        private Font font = new Font("Times New Roman", Font.PLAIN, 11);
        private Runtime r = Runtime.getRuntime();
        private int columnInc;
        private int pts[];
		private float values[];
        private int ptNum;
        private int ascent, descent;
        private Rectangle graphOutlineRect = new Rectangle();
        private Rectangle2D mfRect = new Rectangle2D.Float();
        private Rectangle2D muRect = new Rectangle2D.Float();
        private Line2D graphLine = new Line2D.Float();
        private Color graphColor = new Color(46, 139, 87);
        private Color mfColor = new Color(0, 100, 0);
        private String usedStr;
		private boolean updatePts = false;
      

        public Surface() {
			super();
            setBackground(Color.black);
			addComponentListener(this);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

		/*
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
		*/

        public Dimension getPreferredSize() {
            return new Dimension(controls.getPreferredSize().width,135);
        }

		float totalMemoryPrev = 0.0f;
            
        public synchronized void paintComponent(Graphics g)
	   	{
            //if (big == null) {
            //    return;
            //}
			big = (Graphics2D)g;
                    FontMetrics fm = big.getFontMetrics(font);
                    ascent = (int) fm.getAscent();
                    descent = (int) fm.getDescent();

            big.setBackground(getBackground());
            big.clearRect(0,0,w,h);

            float totalMemory = (float) r.totalMemory();
			float freeMemory = (float)r.freeMemory();
            float usedMemory = (float) totalMemory - freeMemory;
			if (totalMemoryPrev != totalMemory) updatePts = true;

            // .. Draw allocated and used strings ..
            big.setColor(Color.green);
            big.drawString(String.valueOf((int) totalMemory/1024) + "K allocated",  4.0f, (float) ascent+0.5f);
            usedStr = String.valueOf(((int) (usedMemory))/1024) 
                + "K used";
            big.drawString(usedStr, 4, h-descent);

            // Calculate remaining size
            float ssH = ascent + descent;
            float remainingHeight = (float) (h - (ssH*2) - 0.5f);
            float blockHeight = remainingHeight/10;
            float blockWidth = 20.0f;
            float remainingWidth = (float) (w - blockWidth - 10);

            // .. Memory Free ..
            big.setColor(mfColor);
            int MemUsage = (int) ((freeMemory / totalMemory) * 10);
            int i = 0;
            for ( ; i < MemUsage ; i++) { 
                mfRect.setRect(5,(float) ssH+i*blockHeight,
                                blockWidth,(float) blockHeight-1);
                big.fill(mfRect);
            }

            // .. Memory Used ..
            big.setColor(Color.green);
            for ( ; i < 10; i++)  {
                muRect.setRect(5,(float) ssH+i*blockHeight,
                                blockWidth,(float) blockHeight-1);
                big.fill(muRect);
            }

            // .. Draw History Graph ..
            big.setColor(graphColor);
            int graphX = 30;
            int graphY = (int) ssH;
            int graphW = w - graphX - 5;
            int graphH = (int) remainingHeight;
			if (graphW <= 0 || graphH <= 0) {
				System.out.println("size = " + getSize());
				System.out.println("w = " + w + ", h = " + h);
				System.out.println("graphW = " + graphW + ", graphH = " + graphH);
				return;
			}
            graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
            big.draw(graphOutlineRect);

            int graphRow = graphH/10;

            // .. Draw row ..
            for (int j = graphY; j <= graphH+graphY; j += graphRow) {
                graphLine.setLine(graphX,j,graphX+graphW,j);
                big.draw(graphLine);
            }
        
            // .. Draw animated column movement ..
            int graphColumn = graphW/15;

			if (animating()) {
            if (columnInc == 0) {
                columnInc = graphColumn;
            }
            --columnInc;
			}

            for (int j = graphX+columnInc; j < graphW+graphX; j+=graphColumn) {
                graphLine.setLine(j,graphY,j,graphY+graphH);
                big.draw(graphLine);
            }

            if (pts == null) {
                pts = new int[graphW];
                values = new float[graphW];
                ptNum = 0;
            } else if (pts.length != graphW) {
                int tmp[] = null;
				float tmpf[] = null;
                if (ptNum < graphW) {     
                    tmp = new int[ptNum];
                    System.arraycopy(pts, 0, tmp, 0, ptNum);
                    tmpf = new float[ptNum];
                    System.arraycopy(values, 0, tmpf, 0, ptNum);
                } else {        
                    tmp = new int[graphW];
                    System.arraycopy(pts, ptNum-graphW, tmp, 0, graphW);
                    tmpf = new float[graphW];
                    System.arraycopy(values, ptNum-graphW, tmpf, 0, graphW);
                    ptNum = graphW;
                }
                pts = new int[graphW];
                System.arraycopy(tmp, 0, pts, 0, tmp.length);
                values = new float[graphW];
                System.arraycopy(tmpf, 0, values, 0, tmpf.length);
            } else {
				if (ptNum > values.length) {
					System.out.println("size = " + getSize());
					System.out.println("w = " + w + ", h = " + h);
					System.out.println("graphW = " + graphW + ", graphH = " + graphH);
					System.out.println("ptNum = " + ptNum);
					return;
				}
                big.setColor(Color.yellow);
				if (animating()) {
                	if (ptNum == graphW) {
						ptNum--;
					   	// throw out oldest point
						for (int j = 0;j < ptNum; j++) {
							pts[j] = pts[j+1];
						   	values[j] = values[j+1];
					   	}
                	}
                	values[ptNum] = usedMemory;
                	pts[ptNum] = (int)(graphY+graphH
									*(totalMemory-values[ptNum])/totalMemory);
                    ptNum++;
                }
			}

			if (updatePts) { // due to resize
				for (int k=0; k<ptNum; k++)
               		pts[k] = (int)(graphY+graphH
									*(totalMemory-values[k])/totalMemory);
				updatePts = false;
			} 
			
			for (int j=graphX+graphW-ptNum+1, k=1;k < ptNum; k++, j++) {
				//if (pts[k] != pts[k-1]) {
					big.drawLine(j-1, pts[k-1], j, pts[k]);
			   	//} else {
				//	big.fillRect(j, pts[k], 1, 1);
			   	//}
		   	}
        }

		boolean animating()
		{ return thread != null; }

        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("MemoryMonitor");
            thread.start();
        }


        public synchronized void stop() {
            thread = null;
            notify();
        }


        public void run() {

            Thread me = Thread.currentThread();

            while (thread == me && !isShowing() || getSize().width == 0) {
                try {
                    thread.sleep(500);
                } catch (InterruptedException e) { return; }
            }
    
            while (thread == me && isShowing()) {
					/*
                Dimension d = getSize();
                if (d.width != w || d.height != h) {
                    w = d.width;
                    h = d.height;
                    bimg = (BufferedImage) createImage(w, h);
                    big = bimg.createGraphics();
                    big.setFont(font);
                    FontMetrics fm = big.getFontMetrics(font);
                    ascent = (int) fm.getAscent();
                    descent = (int) fm.getDescent();
                }
				*/
                repaint();
                try {
                    thread.sleep(sleepAmount);
                } catch (InterruptedException e) { break; }
                //if (MemoryMonitor.dateStampCB.isSelected()) {
                //    System.out.println(new Date().toString() + " " + usedStr);
                //}
            }
            thread = null;
        }

		public void componentHidden(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentShown(ComponentEvent e) {}

		public synchronized void componentResized(ComponentEvent e)
		{
			Dimension d = getSize();
		   	if (d.width == w && d.height == h) return;
			if (d.height != h) updatePts = true;
			w = d.width; h = d.height;
			repaint();
		}
    }


    public static void main(String s[]) {
		run(500);
	}

	public static void run()
	{ run(1000); }

	public static void run(long sleepAmount)
	{
        final MemoryMonitor mm = new MemoryMonitor();
		mm.setUpdateInterval(sleepAmount);
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
				//System.exit(0);
				JFrame f = (JFrame)e.getSource();
				f.dispose();
			}
            public void windowDeiconified(WindowEvent e) { mm.surf.start(); }
            public void windowIconified(WindowEvent e) { mm.surf.stop(); }
        };
        JFrame f = new JFrame("MemoryMonitor");
        f.addWindowListener(l);
        f.getContentPane().add("Center", mm);
        f.pack();
        //f.setSize(new Dimension(200,200));
        f.setVisible(true);
        mm.surf.start();
    }
}
