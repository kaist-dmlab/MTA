// @(#)ErrorContract.java   1/2004
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

import drcl.comp.*;

/**
 * Defines the format of the <i>error</i> message exported by a
 * {@link drcl.comp.Component component} and provides utility methods
 * to retrieve individual fields in such a message.
 *
 * <p>An error message is exported by a component when an error or a warning
 * of processing arriving data occurs.
 *
 * <p>Specifically, an error message contains the following fields:
 * <table border=1>
 * <tr> <td align=center> FIELD <td align=center> TYPE <td align=center> DESCRIPTION </tr>
 * <tr>
 *     <td valign=top> Time
 *     <td valing=top> <code>double</code>
 *     <td > Time when this message is exported.
 * </tr>
 * <tr>
 *     <td valign=top> Port
 *     <td valing=top> <code>String</code>
 *     <td > Path of the port where the data arrives.
 * </tr>
 * <tr>
 *     <td valign=top> Data
 *     <td valing=top> <code>Object</code>
 *     <td > Data in process when the error is discovered.
 * </tr>
 * <tr>
 *     <td valign=top> Where
 *     <td valing=top> <code>String</code>
 *     <td > Name of the method in which the data is processed.
 * </tr>
 * <tr>
 *     <td valign=top> Description
 *     <td valing=top> <code>Object</code>
 *     <td > Description of this error.
 * </tr>
 * </table>
 */
public class ErrorContract extends Contract
{
	public static final ErrorContract INSTANCE = new ErrorContract();

	public String getName()
	{ return "ACA Error Message Format"; }

	public Object getContractContent()
	{ return null; }

	public static class Message extends ComponentMessage
	{
		double time;
		String portPath;
		Object data;
		String where;
		Object description;
	
		public Message (double time_, Port p_, Object data_,
			String where_, Object description_)
		{
			time = time_;
			portPath = p_ == null? null: p_.toString();
			data = data_;
			where = where_;
			description = description_;
		}

		private Message (double time_, String portPath_, Object data_,
			String where_, Object description_)
		{
			time = time_;
			portPath = portPath_;
			data = data_;
			where = where_;
			description = description_;
		}
	
		public double getTime()
		{ return time; }
	
		public String getPortPath()
		{ return portPath; }
	
		public Object getData()
		{ return data; }
	
		public String getSpot()
		{ return where; }
	
		public Object getDescription()
		{ return description; }

		public Contract getContract()
		{ return INSTANCE; }

		public Object clone()
		{ return new Message(time, portPath, data, where, description); }

		public String toString(String separator_)
		{
			return time + (portPath == null? "": separator_ + portPath)
				+ (where == null? "": separator_ + where) + separator_ + "data:" + data
				+ (description == null? "": separator_ + description);
		}
	}
}
