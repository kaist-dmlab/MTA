// @(#)MiscUtil.java   7/2003
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

package drcl.util;

/**
 * Containing miscellaneous utility methods.
 * For utility methods regarding numbers, object cloning and comparison,
 * and string manipulation, see {@link NumberUtil}, {@link ObjectUtil} and
 * {@link StringUtil} respectively.
 */
public class MiscUtil
{
	/**
	 * Returns the wall time (in second) elapsed since <code>pastReferencePoint_</code>.
	 * This method uses <code>java.lang.System.currentTimeMillis()</code> to obtain the current time.
	 * @param pastReferencePoint_ past time instance in ms.
	 */
	public static double timeElapsed(long pastReferencePoint_)
	{
		return (double)(System.currentTimeMillis() - pastReferencePoint_)/1000.0;
	}

	static long lastTimeMarked;

	/** Marks the current time for calculate time elapsed in {@link #timeElapsed()}. */
	public static void markTime()
	{ lastTimeMarked = System.currentTimeMillis(); }

	/**
	 * Returns the wall time (in second) elapsed since last time marked.
	 * This method uses <code>java.lang.System.currentTimeMillis()</code> to obtain the current time.
	 */
	public static double timeElapsed()
	{ return (double)(System.currentTimeMillis() - lastTimeMarked)/1000.0; }

	static Runtime runtime = Runtime.getRuntime();

	public static void gc()
	{ runtime.gc(); }

	public static long totalMemory()
	{ return runtime.totalMemory(); }

	public static long freeMemory()
	{ return runtime.freeMemory(); }

	public static long maxMemory()
	{ return runtime.maxMemory(); }

	public static long allocatedMemory()
	{ return runtime.totalMemory() - runtime.freeMemory(); }
}
