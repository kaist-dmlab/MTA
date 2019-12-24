// @(#)GarbageContract.java   1/2004
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
 * Defines the format of the <i>garbage</i> message exported by a
 * {@link drcl.comp.Component component} and provides utility methods
 * to retrieve individual fields in such a message.
 *
 * A garbage message describes unwanted data being discarded by 
 * a component.
 * It is equipped with a <i>displayable</i> flag for users to control
 * display of garbage messages from selected components.
 *
 * <p>Specifically, a garbage message contains the following fields:
 * <table border=1>
 * <tr> <td align=center> FIELD <td align=center> TYPE <td align=center> DESCRIPTION </tr>
 * <tr>
 *     <td valign=top> Time
 *     <td valign=top> <code>double</code>
 *     <td > Time when the data is discarded and this message is exported.
 * </tr>
 * <tr>
 *     <td valign=top> Port
 *     <td valign=top> <code>String</code>
 *     <td > Path of the port where unwanted data arrives at the component.
 * </tr>
 * <tr>
 *     <td valign=top> Data
 *     <td valign=top> <code>Object</code>
 *     <td > Data being discarded.
 * </tr>
 * <tr>
 *     <td valign=top> Description
 *     <td valign=top> <code>Object</code>
 *     <td > Explanation regarding this discarding.
 * </tr>
 * <tr>
 *     <td valign=top> Displayable
 *     <td valign=top> <code>boolean</code>
 *     <td > True if this garbage message is allowed to be displayed on an
 *           instrument component with a display. 
 * </tr>
 * </table>
 */
public class GarbageContract extends Contract
{
	public static final GarbageContract INSTANCE = new GarbageContract();

	public String getName()
	{ return "ACA Garbage Message Format"; }

	public Object getContractContent()
	{ return null; }

	public static class Message extends ComponentMessage
	{
		double time;
		String portPath;
		Object data;
		Object description;
		boolean displayable;
	
		public Message (double time_, String portPath_, Object data_, boolean displayable_)
		{
			time = time_;
			portPath = portPath_;
			data = data_;
			description = "";
			displayable = displayable_;
		}
	
		public Message (double time_, String portPath_, Object data_,
			Object description_, boolean displayable_)
		{
			time = time_;
			portPath = portPath_;
			data = data_;
			description = description_;
			displayable = displayable_;
		}
	
		public double getTime()
		{ return time; }
	
		public String getPortPath()
		{ return portPath; }
	
		public Object getData()
		{ return data; }
	
		public Object getDescription()
		{ return description; }
	
		public boolean isDisplayable()
		{ return displayable; }

		public Contract getContract()
		{ return INSTANCE; }

		public Object clone()
		{ return new Message(time, portPath, data, description, displayable); }

		public String toString(String separator_)
		{
			return "GARBAGE" + separator_ + time + separator_ + portPath + separator_
				+ data + (description == null? "": separator_ + description);
		}
	}
}
