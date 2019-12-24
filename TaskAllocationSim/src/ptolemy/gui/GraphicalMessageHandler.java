/* Singleton class for displaying exceptions, warnings, and messages.

 Copyright (c) 1999-2001 The Regents of the University of California.
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
@AcceptedRating Red (reviewmoderator@eecs.berkeley.edu)
*/

package ptolemy.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


//////////////////////////////////////////////////////////////////////////
//// GraphicalMessageHandler
/**
This is a message handler that reports errors in a graphical dialog box.
When an applet or application starts up, it should call setContext()
to specify a component with respect to which the display window
should be created.  This ensures that if the application is iconfied
or deiconified, that the display window goes with it. If the context
is not specified, then the display window is centered on the screen,
but iconifying and deiconifying may not work as desired.
<p>
This class is based on (and contains code from) the diva GUIUtilities
class.

@author  Edward A. Lee, Steve Neuendorffer, and John Reekie
@version $Id: GraphicalMessageHandler.java,v 1.1.1.1 2004/01/26 21:52:02 hyuklim Exp $
*/
public class GraphicalMessageHandler extends MessageHandler {

    /** Set the component with respect to which the display window
     *  should be created.  This ensures that if the application is
     *  iconified or deiconified, that the display window goes with it.
     *  @param context The component context.
     */
    public static void setContext(Component context) {
        _context = context;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Show the specified error message.
     *  @param info The message.
     */
    protected void _error(String info) {
        Object[] message = new Object[1];
	String string = info;
	message[0] = _ellipsis(string, 400);

        Object[] options = {"Dismiss"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);
    }

    /** Show the specified message and exception information.
     *  If the exception is an instance of CancelException, then it
     *  is not shown.  By default, only the message of the exception
     *  is thrown.  The stack trace information is only shown if the
     *  user clicks on the "Display Stack Trace" button.
     *
     *  @param info The message.
     *  @param exception The exception.
     *  @see CancelException
     */
    protected void _error(String info, Exception exception) {
        if (exception instanceof CancelException) return;

        // Sometimes you find that errors are reported multiple times.
        // To find out who is calling this method, uncomment the following.
        // System.out.println("------ reporting error:");
        // (new Exception()).printStackTrace();

        Object[] message = new Object[1];
	String string;
	if(info != null) {
	    string = info + "\n" + exception.getMessage();
	} else {
	    string = exception.getMessage();
	}
	message[0] = _ellipsis(string, 400);

        Object[] options = {"Dismiss", "Display Stack Trace"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Exception",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);

        if(selected == 1) {
            _showStackTrace(exception, info);
        }
    }

    /** Show the specified message in a modal dialog.
     *  @param info The message.
     */
    protected void _message(String info) {
        Object[] message = new Object[1];
        message[0] = info;
        Object[] options = {"OK"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Message",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
    }

    /** Show the specified message in a modal dialog.  If the user
     *  clicks on the "Cancel" button, then throw an exception.
     *  This gives the user the option of not continuing the
     *  execution, something that is particularly useful if continuing
     *  execution will result in repeated warnings.
     *  @param info The message.
     *  @exception CancelException If the user clicks on the "Cancel" button.
     */
    protected void _warning(String info) throws CancelException {
        Object[] message = new Object[1];
        message[0] = info;
        Object[] options = {"OK", "Cancel"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if(selected == 1) {
            throw new CancelException();
        }
    }

    /** Show the specified message and exception information
     *  in a modal dialog.  If the user
     *  clicks on the "Cancel" button, then throw an exception.
     *  This gives the user the option of not continuing the
     *  execution, something that is particularly useful if continuing
     *  execution will result in repeated warnings.
     *  By default, only the message of the exception
     *  is thrown.  The stack trace information is only shown if the
     *  user clicks on the "Display Stack Trace" button.
     *  @param info The message.
     *  @exception CancelException If the user clicks on the "Cancel" button.
     */
    protected void _warning(String info, Exception exception)
            throws CancelException {
        Object[] message = new Object[1];
        message[0] = info;
        Object[] options = {"OK", "Display Stack Trace", "Cancel"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if(selected == 1) {
            _showStackTrace(exception, info);
        } else if(selected == 2) {
            throw new CancelException();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                       protected variables                 ////

    // The context.
    protected static Component _context = null;

    ///////////////////////////////////////////////////////////////////
    ////                        private methods                    ////

    /** Return a string that contains the original string, limited to the
     *  given number of characters.  If the string is truncated, an ellipsis
     *  will be appended to the end of the string.
     *  @param string The string to truncate.
     *  @param length The length to which to truncate the string.
     */
    private String _ellipsis(String string, int length) {
	if(string.length() > length) {
	    return string.substring(0, length-3) + "...";
	}
	return string;
    }

    /** Display a stack trace dialog. The "info" argument is a
     *  string printed at the top of the dialog instead of the Exception
     *  message.
     *  @param exception The exception.
     *  @param info A message.
     */
    private void _showStackTrace(Exception exception, String info) {
        // FIXME: Eventually, the dialog should
        // be able to email us a bug report.
        // Show the stack trace in a scrollable text area.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        JTextArea text = new JTextArea(sw.toString(), 60, 80);
        JScrollPane stext = new JScrollPane(text);
        stext.setPreferredSize(new Dimension(600, 300));
        text.setCaretPosition(0);
        text.setEditable(false);

        // We want to stack the text area with another message
        Object[] message = new Object[2];
        String string;
        if(info != null) {
            string = info + "\n" + exception.getMessage();
        } else {
            string = exception.getMessage();
        }
        message[0] = _ellipsis(string, 400);
        message[1] = stext;

        // Show the MODAL dialog
        JOptionPane.showMessageDialog(
                _context,
                message,
                "Stack trace",
                JOptionPane.ERROR_MESSAGE);
    }
}
