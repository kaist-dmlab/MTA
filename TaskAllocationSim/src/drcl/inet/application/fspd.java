// @(#)fspd.java   9/2002
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
 * A simple FSP (File Service Protocol) server.
 * This implementation does not limit client sending rate.
 * To use this component, simply start it as {@link drcl.comp.ActiveComponent}.
 * @see fsp
 */
public class fspd extends SUDPApplication implements ActiveComponent
{
	public fspd ()
	{ super(); }
	
	public fspd(String id_)
	{ super(id_); }

	public synchronized String info()
	{
		if (clients == null || clients.size() == 0)
			return super.info() + "No client is being served.\n";
		StringBuffer sb_ = new StringBuffer(super.info() + "Client(s):\n");
		for (Enumeration e_ = clients.keys(); e_.hasMoreElements(); ) {
			Object key_ = e_.nextElement();
			Object entry_ = clients.get(key_);
			sb_.append("   Client '" + key_ + "': " + entry_ + "\n");
		}
		return sb_.toString();
	}
	
	Hashtable clients = null; // String: file name -> RandomAccessFile
	static final double CLIENT_TIMEOUT = 30; // seconds
		
	protected void _start()
	{
		while (true)
		{
			// wait indefinitely for client requests
			DatagramContract.Message requestPack_ = recvmsg();
			if (isDebugEnabled()) debug("Got request: " + requestPack_);
			
			try {
				long peer_ = requestPack_.getSource();
				int peerPort_ = requestPack_.getSourcePort();
				FSPMessage request_ = (FSPMessage)requestPack_.getContent();
				String fileName_ = request_.getFileName();
				
				try {
					if (clients == null) clients = new Hashtable();
					ClientData client_ = null;
					RandomAccessFile raf_ = null;
					synchronized (clients) {
						client_ = (ClientData)clients.get(fileName_);
					}
					if (client_ == null) {
						raf_ = new RandomAccessFile(fileName_, "r");
						client_ = new ClientData(fileName_, raf_);
						if (raf_ == null) {
							sendmsg(new FSPMessage(fileName_, FSPMessage.FILE_NOT_EXIST, null),
								fileName_.length() + 8, peer_, peerPort_);
							error("_start()", "'" + fileName_ + "' does not exist");
							continue;
						}
						else synchronized (clients) {
							clients.put(fileName_, client_);
						}
					}
					else
						raf_ = client_.raf;
					synchronized (client_) {
						client_.timeout = setTimeout(fileName_, CLIENT_TIMEOUT).getTime();
					}
					
					long startIndex_ = request_.getStartIndex();
					long endIndex_ = request_.getEndIndex();
					int code_ = (int) (Integer.MAX_VALUE * Math.random());
					raf_.seek(startIndex_);
					byte[] buf_ = new byte[(int)(endIndex_ - startIndex_)];
					int nbytes_ = raf_.read(buf_);
					if (nbytes_ < buf_.length) {
						byte[] tmp_ = new byte[nbytes_ < 0? 0: nbytes_];
						System.arraycopy(buf_, 0, tmp_, 0, tmp_.length);
						buf_ = tmp_;
					}
					if (isDebugEnabled()) debug("Send back reply");
					sendmsg(new FSPMessage(fileName_, code_, startIndex_, endIndex_, raf_.length(), buf_),
						fileName_.length() + buf_.length + 28/* 3 int's and 2 long's */,
						peer_, peerPort_);
				}
				catch (Exception e_) {
					e_.printStackTrace();
					sendmsg(new FSPMessage(fileName_, FSPMessage.EXCEPTION, e_.toString()),
						fileName_.length() + 12 + e_.toString().length(), peer_, peerPort_);
					error("_start()", "Error occurred when handling client: " + e_);
				}
			}
			catch (Exception e_) {
				e_.printStackTrace();
				error("_start()", e_);
			}
		} // end while (true)
	}
	
	/**
	 */
	protected void timeout(Object data_)
	{
		ClientData client_ = null;
		synchronized (clients) {
			client_ = (ClientData)clients.remove(data_);
		}
		if (client_ != null)
			synchronized (client_) {
				if (client_.timeout > getTime())
					// reset timer
					setTimeoutAt(client_.fileName, client_.timeout);
				else
					try { client_.raf.close(); } catch (Exception e_) {} // ignored
			}
	}

	class ClientData {
		String fileName;
		RandomAccessFile raf;
		double timeout; // time when the timer expires

		ClientData(String fn_, RandomAccessFile raf_)
		{
			fileName = fn_;
			raf = raf_;
		}
	}
}
