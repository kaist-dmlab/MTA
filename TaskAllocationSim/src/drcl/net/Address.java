// @(#)Address.java   11/2002
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

package drcl.net;

/**
<code>Address</code> implements an address scheme. 
An address scheme divides the address space (in <code>long</code>) into
unicast addresses, multicast addresses, broadcast addresses(<i>any</i> addresses),
null addresses.  Also it provides translation between an
address and <code>String</code> representation of the address.

<p>All the address schemes should follow the following rules:
<ul>
<li> {@link #NULL_ADDR} must be in the null address space.
<li> {@link #ANY_ADDR} must be in the broadcast address space.
<li> The maximum value of an address must not exceed {@link #MAX_ADDR}.
</ul>

<p>The default address scheme implemented by this class
(an instance is at {@link #DEFAULT_ADDRESS}) is as follows:
<ul>
<li> Unicast address: 0 and positive long integers up to {@link #MAX_ADDR}.
<li> Multicast address: all negative long integers.
<li> Null address: {@link #NULL_ADDR} only.
<li> Broadcast address: {@link #ANY_ADDR} only.
</ul>
 */
public class Address implements java.io.Serializable
{
	/** An instance of the default address scheme. */
	public static final Address DEFAULT_ADDRESS = new Address();

	/** The default null address. */
	public static final long NULL_ADDR = Long.MAX_VALUE;

	/** The default broadcast address. */
	public static final long ANY_ADDR = Long.MAX_VALUE - 1;

	/** The maximum address value that can be used in any address scheme. */
	public static final long MAX_ADDR = Long.MAX_VALUE - 8;
															 
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE0 = Long.MAX_VALUE - 7;
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE1 = Long.MAX_VALUE - 6;
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE2 = Long.MAX_VALUE - 5;
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE3 = Long.MAX_VALUE - 4;
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE4 = Long.MAX_VALUE - 3;
	/** One of the long integers that should not be used in an address scheme. */
	public static final long NO_USE5 = Long.MAX_VALUE - 2;
															 
	/** When translating an address (long integer) to String, 
	 * the address will be displayed as a heximal if it is larger than
	 * this value. */
	public static long ADDRESS_DISPLAY_THRESH = 1000000;

	/** Returns true if the argument is the default null address. */
	public static boolean _isNull(long addr_) 
	{ return addr_ == NULL_ADDR; }
	
	/** Returns true if the argument is the default broadcast address. */
	public static boolean _isAny(long addr_) 
	{ return addr_ == ANY_ADDR; }
	
	/** Returns the default <code>String</code> representation of the address.*/
	public static String _ltos(long addr_)
	{
		return Address._isNull(addr_)?  "--":
				Address._isAny(addr_)? "*":
				addr_ >= -ADDRESS_DISPLAY_THRESH
					&& addr_ < ADDRESS_DISPLAY_THRESH?
				Long.toString(addr_):
				addr_ >= 0 && addr_ < 0x0FFFFFFFFL?
				drcl.util.StringUtil.toDottedDecimal(addr_, 4):
				"#" + 
				drcl.util.StringUtil.toHex(addr_, true/*skip leading zeros*/);
	}
	
	/** Returns the <code>long</code> representation of the address.
	 * The format is assumed to be either a decimal or a heximal starting
	 * with "#". */
	public static long _stol(String addr_) 
	{
		try {
			return Long.valueOf(addr_).longValue(); 
		}
		catch (Exception ee_) {
			if (addr_.startsWith("#"))
				return drcl.util.StringUtil.hexToLong(addr_);
			else
				try {
					return drcl.util.StringUtil.dottedDecimalToLong(addr_);
				}
				catch (Exception e_) {
					drcl.Debug.error(Address.class, e_);
					return NULL_ADDR;
				}
		}
	}

	/** Returns true if the argument is a multicast address. */
	public boolean isMcast(long addr_) 
	{ return addr_ < 0; }
	
	/** Returns true if the argument is a unicast address. */
	public boolean isUnicast(long addr_) 
	{ return addr_ >= 0 && addr_ < ANY_ADDR; }
	
	/** Returns true if the argument is a broadcast address. */
	public boolean isAny(long addr_) 
	{ return addr_ == ANY_ADDR; }
	
	/** Returns true if the argument is a null address. */
	public boolean isNull(long addr_) 
	{ return addr_ == NULL_ADDR; }
	
	/** Returns the <code>String</code> representation of the address. */
	public String ltos(long addr_)
	{ return _ltos(addr_); }
	
	/** Returns the <code>long</code> representation of the address. */
	public long stol(String addr_) 
	{ return _stol(addr_); }
}
