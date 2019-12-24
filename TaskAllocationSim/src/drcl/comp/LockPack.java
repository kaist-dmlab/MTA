// @(#)LockPack.java   9/2002
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

package drcl.comp;

/** Data structure of a lock.  
 * Multiple locks can be chained to form a linked list.*/
public class LockPack implements Component.Locks
{
	/** The target object being locked or waited on. */
	public Object target;
	/** Thread that holds the lock of the target object. */
	public WorkerThread holder;
	/** The number of times the holder thread grabs the lock. */
	public int counter; 
	/** The number of threads competing for the lock of the target object. */
	public int lockReqCount;
	/** The number of threads waiting on the target object. */
	public int waitCount;
	/** Next lock pack in the chain. */
	public LockPack next;
		
	public LockPack(Object target_)
	{
		target = target_;
	}

	public String printAll()
	{
		LockPack p_ = this;
		StringBuffer sb_ = new StringBuffer();
		while (p_ != null) {
			sb_.append("   " + p_.target + ": " + p_.holder + ", count="
				+ p_.counter + ", waitCount=" + p_.waitCount + "\n");
			p_ = p_.next;
		}
		return sb_.toString();
	}

	public String toString()
	{ return "LockPack: " + target + ", holder=" + ", " + counter
				+ ", " + lockReqCount + ", " + waitCount; }
}
