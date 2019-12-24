// @(#)TCPTrace.java   9/2002
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

package drcl.net.traffic;

import java.io.Reader;
import java.io.*;
import drcl.comp.*;
import drcl.data.*;

/**
 */
public class TCPTrace extends TraceInput
{
	public TCPTrace()
	{ super(); }

	public TCPTrace(String id_)
	{ super(id_); }

	BufferedReader breader;

	long src_id;
	long dest_id;
	int  src_port;
	int  dest_port;
	
	public void setParam(long src, long dest, int s_port, int d_port)
	{
		src_id		= src;
		dest_id		= dest;
		src_port	= s_port;
		dest_port	= d_port;
	}
	
	protected double setNextPacket(drcl.net.FooPacket nextpkt_) 
	{
		double	time_;
		int	size_;
		
		try{
			if (breader == null)
				breader = reader instanceof BufferedReader?
					(BufferedReader)reader: new BufferedReader(reader);
			do {
				String line;
				if ((line = breader.readLine()) == null) return Double.NaN;
				java.util.StringTokenizer st = new java.util.StringTokenizer(line);
				String element [] = new String[6];
				int i;
				for(i=0; i<6; i++)
					if (st.hasMoreTokens())
						element[i] = st.nextToken();
				if(i!=6) return Double.NaN;
				time_	= Double.valueOf(element[0]).doubleValue();
				long s_host	= Long.valueOf(element[1]).longValue();
				long d_host	= Long.valueOf(element[2]).longValue();
				long s_port	= Long.valueOf(element[3]).longValue();
				long d_port	= Long.valueOf(element[4]).longValue();
				size_	= Integer.parseInt(element[5]);
				if( (s_host==src_id) && (d_host==dest_id) && (s_port==src_port) && (d_port==dest_port))
					break;
			}while(true);
			nextpkt_.setPacketSize(size_);
			return time_;
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return Double.NaN;
		}
	}
	
	public void reset()
	{
		super.reset();
/*		try {
			if (reader != null) reader.reset();
		}
		catch (Exception e_) {
			drcl.Debug.error(this, "reset()| reader cannot be reset");
		}
*/
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TraceInput that_ = (TraceInput) source_;
	}
	
	public String info()
	{ return super.info(); }
}
