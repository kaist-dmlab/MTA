// @(#)EventContract.java   1/2004
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
 * Defines the format of the <i>event</i> message exported by a
 * {@link drcl.comp.Component component} and provides utility methods
 * to retrieve individual fields in such a message.
 *
 * <p>An event message contains the following fields:
 * <table border=1>
 * <tr> <td align=center> FIELD <td align=center> TYPE <td align=center> DESCRIPTION </tr>
 * <tr>
 *     <td valign=top> Time
 *     <td valign=top> <code>double</code>
 *     <td > Time when the event occurs and this message is exported.
 * </tr>
 * <tr>
 *     <td valign=top> Port
 *     <td valign=top> <code>String</code>
 *     <td > Path of the port where this message is originated.
 * </tr>
 * <tr>
 *     <td valign=top nowrap> Event Name
 *     <td valign=top> <code>String</code>
 *     <td > Name of the event.
 * </tr>
 * <tr>
 *     <td valign=top nowrap> Event Object
 *     <td valign=top> <code>Object</code>
 *     <td > Object of the event.
 * </tr>
 * <tr>
 *     <td valign=top> Description
 *     <td valign=top> <code>Object</code>
 *     <td > Description of the event.
 * </tr>
 * </table>
 */
public class EventContract extends Contract
{
	public static final EventContract INSTANCE = new EventContract();
 
	public String getName()
	{ return "ACA Event Message Format"; }

	public Object getContractContent()
	{ return null; }
	
	public static class Message extends ComponentMessage
	{
		double time;
		String evtName;
		Object event;
		String portPath;
		Object description;

		public Message (double time_, String evtName_, String portPath_, 
			Object event_, Object description_)
		{
			time = time_;
			portPath = portPath_;
			evtName = evtName_;
			event = event_;
			description = description_;
		}
	
		public double getTime()
		{ return time; }
	
		public String getPortPath()
		{ return portPath; }
	
		public String getEventName()
		{ return evtName; }
	
		public Object getEvent()
		{ return event; }
	
		public Object getDescription()
		{ return description; }

		public Contract getContract()
		{ return INSTANCE; }

		public Object clone()
		{ return new Message(time, evtName, portPath, event, description); }

		public String toString(String separator_)
		{
			return "EVENT" + separator_ + time + separator_ + portPath + separator_
				+ evtName + separator_ + drcl.util.StringUtil.toString(event)
				+ (description == null? "": separator_ + description);
		}
	}
}
