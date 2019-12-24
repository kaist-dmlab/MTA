// @(#)fsp.java   11/2002
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

package drcl.inet.application;

import java.util.*;
import java.io.*;
import drcl.comp.*;
import drcl.inet.contract.DatagramContract;

/**
 * A simple FSP (file service protocol) client.
 * Use {@link #get(String, String, int, long, int) get()} to retrieve a file at a fsp server.
 * @see fspd
 */
public class fsp extends SUDPApplication
{
	Port forkPort = addPort(new Port(Port.PortType_FORK), ".fork", false/*not removable*/);
	
    public fsp ()
	{ super(); }
	
	public fsp(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		
		synchronized (this) {
			if (vUR != null) vUR.removeAllElements();
		}
	}
	
	Vector vUR = null; // store user requests
						   
	public synchronized String info()
	{
		if (vUR == null || vUR.size() == 0)
			return super.info() + "No request is issued.\n";
		StringBuffer sb_ = new StringBuffer(super.info() + "User request(s):\n");
		for (int i=0; i<vUR.size(); i++)
			sb_.append("   Request " + i + ": " + vUR.elementAt(i) + "\n");
		return sb_.toString();
	}
	
	/** Retrieves a file from the remote node. */
	public void get(String remoteFileName_, String localFileName_, int blockSize_,
					long dest_, int destPort_)
	{
		fork(forkPort, new UserRequest(remoteFileName_, localFileName_, blockSize_, dest_, destPort_),
			 0.0);
	}
	
	/** Stops a file transfer. */
	public synchronized void stop(int index_)
	{
		if (vUR != null)
			vUR.removeElementAt(index_);
	}
	
	static final int NTRY = 3; // seconds
	static final double REPLY_TIMEOUT = 30.0; // seconds
	
	/**
	 */
	protected void processOther(Object data_, Port inPort_)
	{
		if (inPort_ != forkPort) {
			super.processOther(data_, inPort_);
			return;
		}
		
		UserRequest ur_ = null;
		
		try {
			ur_ = (UserRequest)data_;
		}
		catch (Exception e_) {
			if (isErrorNoticeEnabled())
				error(data_, "processOther()", inPort_, "unrecognized data type: " + e_);
			return;
		}
	
		synchronized (this) {
			if (vUR == null) vUR = new Vector();
			vUR.addElement(ur_);
		}
		try {
			RandomAccessFile rfile_ = new RandomAccessFile(ur_.localFileName, "rw");	
			int code_ = 0;
			while (true) {
				DatagramContract.Message replyPack_ = null;
				for (int i=0; i<NTRY; i++) {
					FSPMessage s_ = new FSPMessage(ur_.remoteFileName, code_, ur_.index,
						ur_.index + ur_.blockSize);
					if (i>0) {
						if (isDebugEnabled()) debug("RESEND " + i + ": " + s_);
					}
					sendmsg(s_, ur_.remoteFileName.length() + 24
						/* 2 int's (code and length of file name) and 2 long's */,
						ur_.dest, ur_.destPort);

					replyPack_ = recvmsg(ur_.dest, ur_.destPort, REPLY_TIMEOUT);
					if (replyPack_ != null) break;
				}
				if (replyPack_ == null) {
					if (isDebugEnabled()) debug("GIVEUP after " + NTRY + " tries for " + ur_);
					break;
				}
				
				if (vUR.indexOf(ur_) < 0) return; // stop the connection
				
				FSPMessage reply_ = (FSPMessage)replyPack_.getContent();
				String fileName_ = reply_.getFileName();
				if (fileName_ == null || !fileName_.equals(ur_.remoteFileName)) {
					if (isGarbageEnabled()) drop(reply_, "not for " + ur_.remoteFileName);
					continue;
				}
				if (reply_.isError()) {
					// XXX: notify user?
					error(reply_, "processOther()", inPort_, reply_.getError());
					break;
				}
				
				// ignored if index does not match
				if (reply_.getStartIndex() != ur_.index) {
					drop(reply_, "already got it");
					continue;
				}
				
				ur_.size = reply_.getFileSize();
				byte[] buf_ = reply_.getFileContent();
				rfile_.write(buf_);
				code_ = reply_.getCode();
				ur_.index += buf_.length;
				if (ur_.index == ur_.size) break; // end of file
			}
			rfile_.close();
        }
		catch (IOException e_) {
			e_.printStackTrace();
		}
		
		synchronized (this) {
			if (isDebugEnabled()) debug("Done with " + ur_);
			vUR.removeElement(ur_);
		}
    }
	
	static class UserRequest
	{
		String remoteFileName, localFileName;
		int blockSize, destPort;
		long dest;
		long index = 0; // progress
		long size = -1;
		
		public UserRequest (String remoteFileName_, String localFileName_,
										 int blockSize_, long dest_, int destPort_)
		{
			remoteFileName = remoteFileName_;
			localFileName = localFileName_;
			blockSize = blockSize_;
			dest = dest_;
			destPort = destPort_;
		}
		
		public String toString()
		{
			return "remote=" + dest + ":" + destPort + "/" + remoteFileName
				   + ", local=" + localFileName + ", block size=" + blockSize
				+ ", progress=" + index + "/" + size;
		}
	}
}
