/* HistogramApplet

@Copyright (c) 1997-2001 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

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

@ProposedRating Yellow (cxh@eecs.berkeley.edu)
@AcceptedRating Yellow (cxh@eecs.berkeley.edu)
*/

package ptolemy.plot;

import ptolemy.plot.*;

//////////////////////////////////////////////////////////////////////////
//// HistogramApplet
/**
A Histogram.  Data can be given in ASCII format at a URL.
If none is given, then a sample histogram is generated.

@author Edward A. Lee
@version $Id: HistogramApplet.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $
 */
public class HistogramApplet extends PlotApplet {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a string describing this applet.
     *  @return A string describing the applet.
     */
    public String getAppletInfo() {
        return "Histogram 1.0: Demo of PlotApplet.\n" +
            "By: Edward A. Lee, eal@eecs.berkeley.edu\n " +
            "($Id: HistogramApplet.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $)";
    }

    /** Create a new Histogram object for the applet.
     *  @return A new instance of Histogram.
     */
    public PlotBox newPlot() {
        return new Histogram();
    }
}
