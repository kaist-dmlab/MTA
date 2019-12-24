// @(#)PropertyContract.java   1/2004
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
 * Defines the format of the <i>property</i> message exported by a
 * {@link drcl.comp.Component component} and provides utility methods
 * to retrieve individual fields in such a message.
 *
 * <p>A property message contains the following fields:
 * <table border=1>
 * <tr> <td align=center> FIELD <td align=center> TYPE <td align=center> DESCRIPTION </tr>
 * <tr>
 *     <td valign=top> Property
 *     <td valign=top> <code>Object</code>
 *     <td > The property object.
 * </tr>
 * </table>
 */
public class PropertyContract extends Contract
{
	public static final PropertyContract INSTANCE = new PropertyContract();

	public String getName()
	{ return "ACA Property Message Format"; }

	public Object getContractContent()
	{ return null; }

	public static class Message extends ComponentMessage
	{
		Object property;
	
		public Message(Object property_)
		{
			property = property_;
		}
	
		public Object getProperty()
		{ return property; }
	
		public Contract getContract()
		{ return INSTANCE; }

		public Object clone()
		{
			return new Message(property instanceof drcl.ObjectCloneable?
							((drcl.ObjectCloneable)property).clone():
							property);
		}

		public String toString(String separator_)
		{ return "PROPERTY" + separator_ + property; }
	}
}
