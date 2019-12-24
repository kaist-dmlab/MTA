// @(#)FileComponent.java   2/2004
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

package drcl.comp.io;

import java.io.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.comp.lib.bytestream.*;
import drcl.comp.contract.EventContract;
import drcl.util.StringUtil;

/**
 * Writes incoming data to a file in text format. 
 */
public class FileComponent extends Extension
{
	static final long FLAG_APPEND_NEW_LINE_TO_OBJECT_ENABLED  = 1L << FLAG_UNDEFINED_START;
	static final long FLAG_AUTO_FLUSH_ENABLED  = 1L << FLAG_UNDEFINED_START << 1;
	static final long FLAG_TIMESTAMP_ENABLED  = 1L << FLAG_UNDEFINED_START << 2;
	static final long FLAG_EVENT_FILTERING_ENABLED  = 1L << FLAG_UNDEFINED_START << 3;
	static final long FLAG_WRITE_BINARY_ENABLED  = 1L << FLAG_UNDEFINED_START << 4;
	{ setComponentFlag(FLAG_APPEND_NEW_LINE_TO_OBJECT_ENABLED | FLAG_AUTO_FLUSH_ENABLED, true); }
	
	public FileComponent()
	{ super(); }
	
	public FileComponent(String id_)
	{ super(id_); }
	
	OutputStream out = null;

	public void open(String fname_)
	{
		try {
			if (out != null) try { out.close(); } catch (Exception e_) {}
			out = new FileOutputStream(fname_);
		}
		catch (Exception e_) {
			error("open()", e_);
		}
	}

	public void close()
	{
		try {
			if (out != null) out.close();
		}
		catch (Exception e_) {
			error("close()", e_);
		}
	}

	protected synchronized void process(Object data_, Port inPort_) 
	{
		try {
			if (out == null) {
				return;
			}
			if (data_ instanceof ByteStreamContract.Message) {
				ByteStreamContract.Message s_ = (ByteStreamContract.Message)data_;
				// suppose FileComponent is just a listener,
				// does not send reply to the send request
				byte[] bytes_ = s_.getByteArray();
				int offset_ = s_.getOffset();
				int len_ = s_.getLength();
				out.write(bytes_, offset_, len_);
				if (getComponentFlag(FLAG_AUTO_FLUSH_ENABLED) != 0)
					out.flush();
			}
			else if (getComponentFlag(FLAG_EVENT_FILTERING_ENABLED) != 0
				&& data_ instanceof EventContract.Message) {
				EventContract.Message s_ = (EventContract.Message)data_;
				out.write((s_.getTime() + "\t" + s_.getEvent() + "\n").getBytes());
				if (getComponentFlag(FLAG_AUTO_FLUSH_ENABLED) != 0)
					out.flush();
			}
			else if (data_ != null) {
				if (getComponentFlag(FLAG_TIMESTAMP_ENABLED) != 0) {
					if (data_ instanceof byte[]) {
						out.write((getTime() + "\t").getBytes());
						out.write((byte[])data_);
						out.write("\n".getBytes());
					}
					else
						out.write((getTime() + "\t"
							+ StringUtil.toString(data_) + "\n").getBytes());
				}
				else {
					if (data_ instanceof byte[])
						out.write((byte[])data_);
					else if (getComponentFlag(
									FLAG_APPEND_NEW_LINE_TO_OBJECT_ENABLED) == 0
						|| data_ instanceof String)
						out.write(StringUtil.toString(data_).getBytes());
					else 
						out.write((StringUtil.toString(data_)
												+ "\n").getBytes());
				}
				if (getComponentFlag(FLAG_AUTO_FLUSH_ENABLED) != 0)
					out.flush();
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error("close()", e_);
		}
	}
	
	/*
	public void setWriteBinaryEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_WRITE_BINARY_ENABLED, enabled_); }
	
	public boolean isWriteBinaryEnabled()
	{ return getComponentFlag(FLAG_WRITE_BINARY_ENABLED) != 0; }
	*/

	public void setAppendNewLineToObjectEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_APPEND_NEW_LINE_TO_OBJECT_ENABLED, enabled_); }
	
	public boolean isAppendNewLineToObjectEnabled()
	{ return getComponentFlag(FLAG_APPEND_NEW_LINE_TO_OBJECT_ENABLED) != 0; }
	
	public void setAutoFlushEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_AUTO_FLUSH_ENABLED, enabled_); }
	
	public boolean isAutoFlushEnabled()
	{ return getComponentFlag(FLAG_AUTO_FLUSH_ENABLED) != 0; }
	
	public void setTimestampEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_TIMESTAMP_ENABLED, enabled_); }
	
	public boolean isTimestampEnabled()
	{ return getComponentFlag(FLAG_TIMESTAMP_ENABLED) != 0; }
	
	public void setEventFilteringEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_EVENT_FILTERING_ENABLED, enabled_); }
	
	public boolean isEventFilteringEnabled()
	{ return getComponentFlag(FLAG_EVENT_FILTERING_ENABLED) != 0; }
}
