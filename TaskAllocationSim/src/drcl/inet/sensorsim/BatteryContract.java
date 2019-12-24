// @(#)BatteryContract.java   1/2004
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

package drcl.inet.sensorsim;

import drcl.comp.*;
import drcl.data.DoubleObj;
import drcl.net.*;

/** This class implements the contract between the battery model and the CPU and radio models.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class BatteryContract extends Contract
{
	public static final BatteryContract INSTANCE = new BatteryContract();

	public BatteryContract()
	{ super(); }
	
	public BatteryContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "BatteryContract Contract"; }

	public Object getContractContent()
	{ return null; }

	public static final int GET_REMAINING_ENERGY = 0;
	public static final int SET_CONSUMER_CURRENT = 1;

	/** Reports the current consumed by a power consumer. */
	public static void setConsumerCurrent(int consumer_id, double current, Port out_)
	{
		out_.doSending(createSetConsumerCurrentRequest(consumer_id,current));
	}

	/** Builds the message that is used to report the consumed current. */
	public static Object createSetConsumerCurrentRequest(int consumer_id,double current)
	{	
		return new Message(SET_CONSUMER_CURRENT, consumer_id, current);	
	}

	/** Gets the remaining energy. */
	public static double getRemainingEnergy(Port out_)
	{
		DoubleObj e_ = (DoubleObj)out_.sendReceive(createGetRemainingEnergyRequest());
		return e_.value;
	}
	
	/** Builds the message that is used to get the remaining energy. */
	public static Object createGetRemainingEnergyRequest()
	{	return new Message(GET_REMAINING_ENERGY);	}

	/** This class implements the underlying message of the contract. */
	public static class Message extends drcl.comp.Message
	{
		int type;
		int consumer_id;
		double current;
		
		public Message ()
		{}

		// for GET_REMAINING_ENERGY
		public Message (int type_)
		{
			type = type_;
		}
		
		// for SET_CONSUMER_CURRENT
		public Message(int type_, int consumer_id_, double current_)
		{
			type = type_;
			consumer_id = consumer_id_;
			current = current_;
		}

		public int getType()
		{ return type; }

		public int getConsumerID()
		{ return consumer_id; }

		public double getCurrent()
		{ return current; }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			consumer_id = that_.consumer_id;
			current = that_.current;
		}
		*/
	
		public Object clone()
		{ return new Message(type, consumer_id, current); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return ("BatteryContract: " + type);
		}
	} // end class BatteryContract.Message
} // end class BatteryContract
