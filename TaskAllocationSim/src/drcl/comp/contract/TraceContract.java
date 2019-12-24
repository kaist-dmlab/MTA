// @(#)TraceContract.java   1/2004
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

package drcl.comp.contract;

import drcl.comp.Contract;

/**
 * Defines the format of the <i>trace</i> message exported by a
 * {@link drcl.comp.Component component} and provides utility methods
 * to retrieve individual fields in such a message.
 *
 * <p>A trace message is exported by a component when data is sent from or
 * arrives at the component.
 *
 * <p>Specifically, a trace message contains the following fields:
 * <table border=1>
 * <tr> <td align=center> FIELD <td align=center> TYPE <td align=center> DESCRIPTION </tr>
 * <tr>
 *     <td valign=top> Trace
 *     <td valign=top> <code>String</code>
 *     <td > Two types of traces are defined.
 *           A "SEND" trace message is exported for data sent.
 *           A "DATA" trace message is exported for arriving data.
 * </tr>
 * <tr>
 *     <td valign=top> Time
 *     <td valign=top> <code>double</code>
 *     <td > Time when this message is exported.
 * </tr>
 * <tr>
 *     <td valign=top> Where
 *     <td valign=top> <code>String</code>
 *     <td > Path of the port where the sending or receiving takes place.
 * </tr>
 * <tr>
 *     <td valign=top> Description
 *     <td valign=top> <code>Object</code>
 *     <td > Content of the data being sent/received.
 * </tr>
 * </table>
 */
public class TraceContract extends Contract
{
	public static final TraceContract INSTANCE = new TraceContract();

	public String getName()
	{ return "ACA Trace Message Format"; }

	public Object getContractContent()
	{ return null; }

	public static class Message extends ComponentMessage
	{
		String trace;
		double time;
		Object description;
		String where;
	
		public Message (String trace_, double time_, Object where_, Object description_)
		{
			trace = trace_;
			time = time_;
			description = description_;
			where = where_ == null? null: where_.toString();
		}
	
		public String getTrace()
		{ return trace; }
	
		public double getTime()
		{ return time; }
	
		public String getWhere()
		{ return where; }
	
		public Object getDescription()
		{ return description; }

		public Contract getContract()
		{ return INSTANCE; }

		public Object clone()
		{ return new Message(trace, time, where, description); }

		public String toString(String separator_)
		{
			return trace + separator_ + time + separator_ + where + separator_ + drcl.util.StringUtil.toString(description);
		}
	}
}
