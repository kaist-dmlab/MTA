// @(#)ftpd.java   2/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.application;

import java.io.*;
import drcl.comp.*;

/**
 * A simple FTP server.
 * It can only accept one file at one run.  Use {@link #setup(String, int)} to
 * set up the uploaded file and then start the component to make one run.
 * At the end of transfer, the component sends a "done" signal via
 * the notify@ port. 
 * @see ftp
 */
public class ftpd extends SApplication
	implements drcl.comp.ActiveComponent
{
	File file;
	int bufferSize;
	long progress, fileSize;
	Port notifyPort = addEventPort("notify");

	public ftpd ()
	{ super(); }

	public ftpd(String id_)
	{ super(id_); }

	public void setup(String outfile_, int bufferSize_) throws IOException
	{
		bufferSize = bufferSize_;
		file = new File(outfile_);
	}

	public void reset()
	{
		super.reset();
		fileSize = progress = 0;
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		bufferSize = ((ftpd)source_).bufferSize;
	}

	protected void _start ()
	{
		if (file == null) {
			error("_start()", "no file is set up to receive");
			return;
		}
		progress = 0;
		byte[] buf_ = new byte[bufferSize];
		try	{
			FileOutputStream out_ = new FileOutputStream(file);
			DataInputStream in_ = new DataInputStream(getInputStream());

			fileSize = in_.readLong();
			if (isDebugEnabled()) debug("Start receiving file size " + fileSize);
			while (progress < fileSize) {
				int len_ = in_.read(buf_, 0, -1);// -1: read whatever available in the buffer
				if (len_ > 0) out_.write(buf_, 0, len_);
				progress += len_;
				if (isStopped())
					wait(this);
			}
			out_.close();
			in_.close();
			if (isDebugEnabled()) debug("Done with '" + file.getName() + "'");

			notifyPort.doSending("done");
		}
		catch (IOException ioe)	{
			ioe.printStackTrace();
			error("_start()", ioe);
		}
	}

	protected void _resume()
	{ notify(this); }

	public String info()
	{
	    return "Write to file: " + file + "\n"
			+ "BufferSize = " + bufferSize + "\n"
			+ "Progress: " + progress + "/" + fileSize + "\n"
			+ super.info();
	}
}
