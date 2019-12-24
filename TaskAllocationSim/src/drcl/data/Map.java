// @(#)Map.java   10/2002
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

package drcl.data;

import java.util.*;

/**
 * A map keeps (<code>MapKey</code>, entry object) pairs in it.
 * Given a <code>MapKey</code>, an entry/entries can be retrieved by 
 * exact match, longest match, match, or wildcard match.
 * 
 * @see MapKey
 */
public class Map extends drcl.DrclObj
{
	public static Class BEST_CLASS_FOR_LONGEST_MATCH = Map.class;
	public final static Map getBestImplementationForLongestMatch()
	{
		if (BEST_CLASS_FOR_LONGEST_MATCH != null)
			try {
				return (Map)BEST_CLASS_FOR_LONGEST_MATCH.newInstance();
			}
			catch (Exception e_) {}
		return new RadixMap();
	}
	
	// match type
	public static final String MATCH_EXACT = "exact match";
	public static final String MATCH_LONGEST = "longest match";
	public static final String MATCH_ALL = "match all";
	public static final String MATCH_WILDCARD = "match *";
	
	protected MapKey[] key; // a simple hashtable
	protected Object[] entry;
	protected int size = 0; // actual size of the map
	
	{
		key = new MapKey[0];
		entry = new Object[0];
	}
	
	public Map() { }
	
	/** Removes all the entries.  */
	public void reset()
	{
		for (int i=0; i<key.length; i++) key[i] = null;
		key = new MapKey[0];
		for (int i=0; i<entry.length; i++) entry[i] = null;
		entry = new Object[0];
		size = 0;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof Map)) return;
		Map that_ = (Map)source_;
		if (that_.key != null) {
			key = new MapKey[that_.key.length];
			for (int i=0; i<key.length; i++)
				key[i] = (MapKey)drcl.util.ObjectUtil.clone(that_.key[i]);
		}
		else key = new MapKey[0];
		
		if (that_.entry != null) {
			entry = new Object[that_.entry.length];
			for (int i=0; i<entry.length; i++)
				entry[i] = drcl.util.ObjectUtil.clone(that_.entry[i]);
		}
		else entry = new Object[0];
		
		size = that_.size;
	}
	
	//
	private void ___SCRIPT_INTERFACE___() {}
	//
	
	/**
	 Add the key-entry pair to the Map.  No check is done
	 for duplicate keys.
	 
	 @param key_	    the key.
	 @param entry_	the entry.
	 */
	public void addEntry(MapKey key_, Object entry_)
	{
		int i;
		// find an empty spot
		for (i=0; i<key.length; i++)
			if (key[i] == null) break;
		
		if (i >= key.length) {
			MapKey[] tmpkey_ = new MapKey[key.length + 3]; // 3 is arbitrary
			Object[] tmpentry_ = new Object[key.length + 3];
			System.arraycopy(key, 0, tmpkey_, 0, key.length);
			System.arraycopy(entry, 0, tmpentry_, 0, key.length);
			key = tmpkey_;
			entry = tmpentry_;
		}
		
		// xx: map change
		key[i] = key_;
		entry[i] = entry_;
		size ++;
	}

	/**
	 For "exact match" and "wildcard match".
	 @see #get(drcl.data.BitSet, String) for "longest match" and "match all".
	 */
	public Object get(MapKey key_, String matchType_)
	{
		if (matchType_ == MATCH_LONGEST) return getLongestMatch(key_.value);
		if (matchType_ == MATCH_EXACT) return getExactMatch(key_);
		if (matchType_ == MATCH_WILDCARD) return getWildcardMatches(key_);
		if (matchType_ == MATCH_ALL) return getMatches(key_.value);
		if (matchType_.toLowerCase().equals(MATCH_LONGEST))
			return getLongestMatch(key_.value);
		if (matchType_.toLowerCase().equals(MATCH_EXACT)) 
			return getExactMatch(key_);
		if (matchType_.toLowerCase().equals(MATCH_WILDCARD)) 
			return getWildcardMatches(key_);
		if (matchType_.toLowerCase().equals(MATCH_ALL)) 
			return getMatches(key_.value);
		return null;
	}
	
	MapKey createMapKey(drcl.data.BitSet bs_)
	{
		drcl.data.BitSet mask_ = new drcl.data.BitSet(bs_.getSize());
		mask_.set(); // make it all one's
		return new MapKey(mask_, bs_);
	}
	
	/**
	 For "longest match" and "match all".
	 @see #get(drcl.data.BitSet, String) for "exact match" and "wildcard match".
	 */
	public Object get(drcl.data.BitSet bs_, String matchType_)
	{
		if (matchType_ == MATCH_LONGEST) return getLongestMatch(bs_);
		if (matchType_ == MATCH_EXACT) return getExactMatch(bs_);
		if (matchType_ == MATCH_WILDCARD) 
			return getWildcardMatches(createMapKey(bs_));
		if (matchType_ == MATCH_ALL) return getMatches(bs_);
		if (matchType_.toLowerCase().equals(MATCH_LONGEST)) 
			return getLongestMatch(bs_);
		if (matchType_.toLowerCase().equals(MATCH_EXACT)) 
			return getExactMatch(bs_);
		if (matchType_.toLowerCase().equals(MATCH_WILDCARD)) 
			return getWildcardMatches(createMapKey(bs_));
		if (matchType_.toLowerCase().equals(MATCH_ALL)) return getMatches(bs_);
		return null;
	}
	
	/**
	 Returns the longest match.
	 
	 @param key_ the key to match.
	 @return the entry with the key matching the argument with largest number 
	 	of bits.
	 */
	protected Object getLongestMatch(drcl.data.BitSet key_)
	{
		int nmatch_ = -1, best_ = -1;
		for (int i=0; i<key.length; i++) {
			MapKey k = key[i];
			if (k != null && k.match(key_) 
				&& k.mask.getNumSetBits() > nmatch_) {
				nmatch_ = k.mask.getNumSetBits();
				best_ = i;
			}
		}
		if (best_ >= 0) return entry[best_];
		return null;
	}
	
	/**
	 Returns the exact match.
	 
	 @param key_ the key to match.
	 @return the entry with the key exactly matching the argument.
	 */
	protected Object getExactMatch(MapKey key_)
	{
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].exactMatch(key_)) return entry[i];
		return null;
	}
	
	/**
	 Returns the exact match on key value only.
	 
	 @param bs_ the bit set to match.
	 @return the entry with the key value exactly matching the argument.
	 */
	protected Object getExactMatch(drcl.data.BitSet bs_)
	{
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].value.equals(bs_)) return entry[i];
		return null;
	}

	/**
	 @param key_ the key to match.
	 @return true if a key that matches the argument exists.
	 */
	public boolean anyMatch(drcl.data.BitSet key_)
	{
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].match(key_)) return true;
		return false;
	}
	
	/**
	 Returns all the matches
	 
	 @param key_ the key to match.
	 @return all the entries with the keys matching the argument.
	 */
	protected Object[] getMatches(drcl.data.BitSet key_)
	{
		LinkedList v = new LinkedList();
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].match(key_)) v.add(entry[i]);
		return v.toArray();
	}
	
	/**
	 Returns all the wildcard matches.  The mask used to match two keys is the union of
	 the two keys' masks.
	 
	 @param key_ the key to match.
	 @return all the entries with the keys matching the argument in the wildcard fashion.
	 */
	protected Object[] getWildcardMatches(MapKey key_)
	{
		LinkedList v = new LinkedList();
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].wildcardMatch(key_)) v.add(entry[i]);
		return v.toArray();
	}

	/**
	 For "exact match" and "wildcard match".
	 @see #remove(drcl.data.BitSet, String) for "longest match" and "match all".
	 */
	public Object remove(MapKey key_, String matchType_)
	{
		if (matchType_ == MATCH_LONGEST) return removeLongestMatch(key_.value);
		if (matchType_ == MATCH_EXACT) return removeExactMatch(key_);
		if (matchType_ == MATCH_WILDCARD) return removeWildcardMatches(key_);
		if (matchType_ == MATCH_ALL) return removeMatches(key_.value);
		if (matchType_.toLowerCase().equals(MATCH_LONGEST)) return removeLongestMatch(key_.value);
		if (matchType_.toLowerCase().equals(MATCH_EXACT)) return removeExactMatch(key_);
		if (matchType_.toLowerCase().equals(MATCH_WILDCARD)) return removeWildcardMatches(key_);
		if (matchType_.toLowerCase().equals(MATCH_ALL)) return removeMatches(key_.value);
		return null;
	}
	
	/**
	 For "longest match" and "match all".
	 @see #remove(MapKey, String) for "exact match" and "wildcard match".
	 */
	public Object remove(drcl.data.BitSet bs_, String matchType_)
	{
		if (matchType_ == MATCH_LONGEST) return removeLongestMatch(bs_);
		if (matchType_ == MATCH_EXACT) return removeExactMatch(bs_);
		if (matchType_ == MATCH_WILDCARD) return removeWildcardMatches(createMapKey(bs_));
		if (matchType_ == MATCH_ALL) return removeMatches(bs_);
		if (matchType_.toLowerCase().equals(MATCH_LONGEST)) return removeLongestMatch(bs_);
		if (matchType_.toLowerCase().equals(MATCH_EXACT)) return removeExactMatch(bs_);
		if (matchType_.toLowerCase().equals(MATCH_WILDCARD)) return removeWildcardMatches(createMapKey(bs_));
		if (matchType_.toLowerCase().equals(MATCH_ALL)) return removeMatches(bs_);
		return null;
	}
	
	/**
	 Removes the longest match.
	 
	 @param key_ the key to match.
	 @return the entry with the key matching the argument with largest number 
	 of bits.
	 */
	protected Object removeLongestMatch(drcl.data.BitSet key_)
	{
		int nmatch_ = -1, best_ = -1;
		for (int i=0; i<key.length; i++) {
			MapKey k = key[i];
			if (k != null && k.match(key_) && k.mask.getNumSetBits() > nmatch_){
				nmatch_ = k.mask.getNumSetBits();
				best_ = i;
			}
		}
		if (best_ >= 0) {
			Object e_ = entry[best_];
			key[best_] = null; entry[best_] = null;
			size --;
			return e_;
		}
		return null;
	}
	
	/**
	 Removes the exact match.
	 
	 @param key_ the key to match.
	 @return the entry with the key exactly matching the argument.
	 */
	protected Object removeExactMatch(MapKey key_)
	{
		int i;
		for (i=0; i<key.length; i++)
			if (key[i] != null && key[i].exactMatch(key_)) break;
		if (i < key.length) {
			Object e_ = entry[i];
			key[i] = null; entry[i] = null;
			size --;
			return e_;
		}
		return null;
	}
	
	/**
	 Removes the exact match on key value only.
	 
	 @param bs_ the bit set to match.
	 @return the entry with the key value exactly matching the argument.
	 */
	protected Object removeExactMatch(drcl.data.BitSet bs_)
	{
		int i;
		for (i=0; i<key.length; i++)
			if (key[i] != null && key[i].value.equals(bs_)) break;
		if (i < key.length) {
			Object e_ = entry[i];
			key[i] = null; entry[i] = null;
			size --;
			return e_;
		}
		return null;
	}
	
	/**
	 Removes all the matches
	 
	 @param key_ the key to match.
	 @return all the entries with the keys matching the argument.
	 */
	protected Object[] removeMatches(drcl.data.BitSet key_)
	{
		LinkedList v = new LinkedList();
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].match(key_)) {
				v.add(entry[i]);
				key[i] = null; entry[i] = null;
				size --;
			}
		return v.toArray();
	}
	
	/**
	 Removes all the wildcard matches.  The mask used to match two keys is the 
	 union of the two keys' masks.
	 
	 @param key_ the key to match.
	 @return all the entries with the keys matching the argument in the 
	 wildcard fashion.
	 */
	protected Object[] removeWildcardMatches(MapKey key_)
	{
		LinkedList v = new LinkedList();
		for (int i=0; i<key.length; i++)
			if (key[i] != null && key[i].wildcardMatch(key_)) {
				v.add(entry[i]);
				key[i] = null; entry[i] = null;
				size --;
			}
		return v.toArray();
	}
	
	public void removeEntry(Object entry_)
	{
		if (entry_ == null) return;
		for (int i=0; i<key.length; i++)
			if (entry[i] == entry_ || (entry[i] != null 
				&& entry[i].equals(entry_))) {
				key[i] = null; entry[i] = null;
				size --;
			}
	}

	public Object[] getAllEntries()
	{
		if (size == 0) return new Object[0];
		Object[] tmp_ = new Object[size];
		int j = 0;
		for (int i=0; i<key.length; i++)
			if (key[i] != null) tmp_[j++] = entry[i];
		return tmp_;
	}
	
	public MapKey[] getAllKeys()
	{
		if (size == 0) return new MapKey[0];
		MapKey[] tmp_ = new MapKey[size];
		int j = 0;
		for (int i=0; i<key.length; i++)
			if (key[i] != null) tmp_[j++] = key[i];
		return tmp_;
	}

	/** Returns the current size (number of entries) of the map.  */
	public int getSize()
	{ return size; }
	
	/**
	 Prints out the content of this map and represents the keys by
	 the indices of 1's.
	 */
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				if (entry[i] != null)
					sb_.append(key[i] + "\t" + entry[i] + "\n");
				else
					sb_.append(key[i] + "\tnull entry\n");
			
		return sb_.length() == 0? "No entry in the map\n": sb_.toString();
	}

	/** Printout for diagnosis. */
	public String diag()
	{ return info(); }
	
	/**
	 Prints out the content of this map and represents the keys in the form of 
	 long integers.
	 */
	public String numberRepresentation()
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				sb_.append(key[i].numberRepresentation() + "\t"
					+ (entry[i] != null? entry[i]: "null entry") + "\n");
			
		return sb_.length() == 0? "-0-\n": sb_.toString();
	}
		
	/** Prints out the content of this map and represents the keys in the 
	 * binary form.  */
    public String binaryRepresentation() 
	{ return binaryRepresentation(false); }

	/**
	 Prints out the content of this map and represents the keys in the binary 
	 form.
	 @see MapKey#binaryRepresentation(boolean).
	 */
    public String binaryRepresentation(boolean skipLeadingZeros_) 
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				sb_.append(key[i].binaryRepresentation(skipLeadingZeros_) + "\t"
					+ (entry[i] != null? entry[i]: "null entry") + "\n");
			
		return sb_.length() == 0? "-0-\n": sb_.toString();
	}
	
	/**
	 Prints out the content of this map and represents the keys in the binary 
	 form.
	 @see MapKey#binaryRepresentation(int).
	 */
    public String binaryRepresentation(int length_) 
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				sb_.append(key[i].binaryRepresentation(length_) + "\t"
					+ (entry[i] != null? entry[i]: "null entry") + "\n");
			
		return sb_.length() == 0? "-0-\n": sb_.toString();
	}

	/** Prints out the content of this map and represents the keys in the hex 
	 * form.  */
    public String hexRepresentation() 
	{ return hexRepresentation(false); }

	/**
	 Prints out the content of this map and represents the keys in the hex form.
	 @see MapKey#hexRepresentation(boolean).
	 */
    public String hexRepresentation(boolean skipLeadingZeros_) 
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				sb_.append(key[i].hexRepresentation(skipLeadingZeros_) + "\t"
					+ (entry[i] != null? entry[i]: "null entry") + "\n");
			
		return sb_.length() == 0? "-0-\n": sb_.toString();
	}

	/**
	 Prints out the content of this map and represents the keys in the hex form.
	 @see MapKey#hexRepresentation(int).
	 */
    public String hexRepresentation(int length_) 
	{
		StringBuffer sb_ = new StringBuffer();
			
		for (int i=0; i<key.length; i++)
			if (key[i] != null)
				sb_.append(key[i].hexRepresentation(length_) + "\t"
					+ (entry[i] != null? entry[i]: "null entry") + "\n");
			
		return sb_.length() == 0? "-0-\n": sb_.toString();
	}
}
