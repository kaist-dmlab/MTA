// @(#)DrclObj.java   8/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

package drcl;

/**
The class defines the objects that are duplicable and serializable,
properties that are desirable in many class implementations.
 */
public class DrclObj implements java.io.Serializable, ObjectDuplicable
{
	/**
	 opies the content of the <code>source_</code> object to this object.
	 he subclass must implement this method to realize {@link #clone()}.
	 */
	public void duplicate(Object source_)
	{}

	/**
	 Returns a clone of this object.
	 By default, this method creates an object of the same class and 
	 calls {@link #duplicate(Object)} to duplicate the content of this object to
	 the newly-created one.
	  
	 <p>This method uses <code>getClass().newInstance()</code> to create 
	 new instance.  Hence,
	 subclasses need to override this method only if the subclass is not
	 declared as public or does not have explicit no-argument constructor.
	 A subclass may override this method for performance reason.
	 */
	public Object clone()
	{
		try {
			DrclObj o_ = (DrclObj)getClass().newInstance(); //(DrclObj)drcl.RecycleManager.reproduce(this.getClass());
			o_.duplicate(this);
			return o_;
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return null;
		}
	}
	
	public String toString()
	{ return drcl.util.StringUtil.lastSubstring(super.toString(), "."); }
}
