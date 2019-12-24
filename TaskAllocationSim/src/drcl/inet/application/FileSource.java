// @(#)FileSource.java   11/2002
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

import drcl.comp.*;
import drcl.comp.lib.bytestream.ByteStreamConstants;
import drcl.comp.lib.bytestream.ByteStreamContract;

/** A byte stream source which simulates sending a file of limited size.
This component follows {@link drcl.comp.lib.bytestream.ByteStreamContract}.
This component does not receive bytes. 
@see drcl.comp.lib.bytestream.ByteStreamContract
*/
public class FileSource extends Component 
	implements ActiveComponent, ByteStreamConstants
{
	Port downPort = addPort("down", false);

	int dataUnit = 512;
	long progress, size = Long.MAX_VALUE;

	public FileSource ()
	{ super(); }

	public FileSource (String id_)
	{ super(id_); }

	public void reset()
	{
		super.reset();
		progress = 0;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		dataUnit = ((FileSource)source_).dataUnit;
		size = ((FileSource)source_).size;
	}
	
	/**
	 * The source sends a byte array of size <code>dataUnit_</code> indefinitely.
	 * @param dataUnit_ size of the byte array; default is 512.
	 */
	public void setDataUnit(int dataUnit_)
	{ dataUnit = dataUnit_; }

	public int getDataUnit()
	{ return dataUnit; }

	public void setSize(long size_)
	{ size = size_; }
	
	public long getSize()
	{ return size; }
	
	protected void _start()
	{
		progress = 0;
		downPort.doLastSending(new ByteStreamContract.Message(QUERY));
	}

	protected void _resume()
	{
		downPort.doLastSending(new ByteStreamContract.Message(QUERY));
	}
	
	public String info()
	{
		return "Progress: " + (progress/dataUnit)
				+ "/" + progress + "/" + size
				+ " = " + (progress*100/size) + "%\n";
	}

	protected void process(Object data_, Port inPort_)
	{
		if (isStopped()) return;

		int len_ = 0;
		if (data_ instanceof Integer)
			len_ = ((Integer)data_).intValue();
		else if (data_ instanceof ByteStreamContract.Message) {
			ByteStreamContract.Message msg_ = (ByteStreamContract.Message)data_;
			if (msg_.isReport())
				len_ = msg_.getLength();
			else
				return;
		}
		if (len_ > 0) {
			if (progress < size) {
				progress += len_;
				if (progress > size) {
					len_ -= (progress - size);
					progress = size;
				}
				downPort.doLastSending(new ByteStreamContract.Message(SEND,
										null, 0, len_));
			}
		}
		else if (len_ < 0) // peer's buffer is shrinked compared to last report
			progress += len_;
	}
}
