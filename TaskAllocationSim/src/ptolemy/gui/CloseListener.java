/* An interface for listeners that need to be informed when a window closes.

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
@ProposedRating Green (eal@eecs.berkeley.edu)
@AcceptedRating Green (janneck@eecs.berkeley.edu)
*/

package ptolemy.gui;

import java.awt.Window;

//////////////////////////////////////////////////////////////////////////
//// CloseListener
/**
This is an interface for listeners that need to be informed when a
window closes.  Note that this is a very small subset of what Java's
WindowListener interface does.  This class is a workaround for a bug
in Java's AWT, where components are not informed in any way when the
window that contains them is closed, even though they can have
registered listeners.  The listeners are never called, unless the
component is a top-level window. A listener that implements this
interface, by contrast, is informed regardless of whether it is
at the top level. This is used, for example, by the ComponentDialog
class.
@see ComponentDialog

@author Edward A. Lee
@version $Id: CloseListener.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $
*/
public interface CloseListener {

    /** Notify that the specified window has closed.  The second argument,
     *  if non-null, gives the name of the button that was used to close
     *  the window.
     *  @param window The window that closed.
     *  @param button The name of the button that was used to close the window.
     */
    public void windowClosed(Window window, String button);
}
