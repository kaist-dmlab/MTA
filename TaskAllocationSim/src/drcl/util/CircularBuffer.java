// @(#)CircularBuffer.java   9/2002
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

package drcl.util;

/**
A fixed-capacity circular buffer class for byte-stream data.
This class is thread-safe.
@author Yung-ching Hsiao, Hung-ying Tyan
*/
public class CircularBuffer extends drcl.DrclObj
{
    byte[] buf;
    int begin, end, capacity;

    /**
     * @param capacity size of the circular buffer.
     */
    public CircularBuffer(int capacity_)
	{
        begin = end = 0;
		capacity = capacity_;
    }

	public synchronized void reset()
	{
		buf = null;
		begin = end = 0;
	}

	public synchronized void duplicate(Object source_)
	{
		CircularBuffer that_ = (CircularBuffer)source_;
		begin = that_.begin;
		end = that_.end;
		capacity = that_.capacity;
		if (that_.buf == null) buf = null;
		else {
			buf = new byte[that_.buf.length];
			System.arraycopy(that_.buf, 0, buf, 0, buf.length);
		}
	}

    /**
     * Appends data to circular buffers.
     * @return number of bytes that are actually moved to this buffer.
     */
    public int append(byte[] data_)
	{
		if (data_ == null) return -1;
		else return append(data_, 0, data_.length);
    }

    /**
     * Appends data to circular buffers.
     * @return number of bytes that are actually moved to this buffer.
     */
    public int append(byte[] data_, int length_)
	{ return append(data_, 0, length_);	}
	
    /**
     * Appends data to circular buffers.
     * Note that <code>data_</code> may be null and the buffer still 
     * allocates <code>length_</code> bytes but
     * no data is actually written to the buffer.
     * Nothing is allocated if the buffer capacity would be exceeded or
     * <code>data_</code> is not null but <code>length_</code> is greater
     * than the length of <code>data_</code>.
     * @param offset_ starting position in <code>data_</code>.
     * @return number of bytes that are actually moved to this buffer.
     */
    public synchronized int append(byte[] data_, int offset_, int length_)
	{
		int occupy_ = getSize();
		int bufLength_ = capacity + 1;
		if (occupy_ + length_ > capacity)
			length_ = capacity - occupy_;
		if (data_ != null && buf == null) // real buffer
			buf = new byte[capacity + 1];
		
		if (end + length_ < bufLength_) {
            if (data_ != null) System.arraycopy(data_, offset_, buf, end, length_);
            end += length_;
		}
		else {
			int len_ = bufLength_ - end;
            if (data_ != null) {
				System.arraycopy(data_, offset_, buf, end, len_);
				System.arraycopy(data_, offset_ + len_, buf, 0, length_-len_);
			}
			end = length_ - len_;
		}
        return length_;
    }

	
    /**
     * Inserts data to circular buffers.
     * @param pos_ starting position in <code>data_</code>.
     * @return number of bytes that are actually moved to this buffer.
     */
    public int insert(byte[] data_, int pos_, int length_)
	{ return insert(data_, pos_, 0, length_); }
	
    /**
     * Inserts data to circular buffers.
     * Note that <code>data_</code> may be null and the buffer still 
     * allocates <code>length_</code> bytes but
     * no data is actually written to the buffer.
     * Nothing is allocated if the buffer capacity would be exceeded or
     * <code>data_</code> is not null but <code>length_</code> is greater
     * than the length of <code>data_</code>.
     * @param pos_ starting position in <code>data_</code>.
	 * @param dataOffset_ offset in <code>data_</code>.
     * @return number of bytes that are actually moved to this buffer.
     */
    public synchronized int insert(byte[] data_, int pos_, int dataOffset_, int length_)
	{
		int bufLength_ = capacity + 1; // if buffer is real
		if (pos_ + length_ > capacity)
			length_ = capacity - pos_;
		if (data_ != null && buf == null) // real buffer
			buf = new byte[capacity + 1];
		
		pos_ = (pos_ + begin) % bufLength_;
		if (pos_ + length_ < bufLength_) {
            if (data_ != null) System.arraycopy(data_, dataOffset_, buf, pos_, length_);
		}
		else {
			int len_ = bufLength_ - pos_;
            if (data_ != null) {
				System.arraycopy(data_, dataOffset_, buf, pos_, len_);
				System.arraycopy(data_, dataOffset_ + len_, buf, 0, length_-len_);
			}
		}

		// update "end" if necessary
		int end_ = end >= begin? end: end + bufLength_;
		int thisEnd_ = pos_ + length_;
		thisEnd_ = thisEnd_ >= begin? thisEnd_: thisEnd_ + bufLength_;
		if (thisEnd_ > end_) end = thisEnd_ % bufLength_;
        return length_;
    }

    /**
     * Removes data from the circular buffer.
     *
     * @param size_ # of bytes to be removed from the buffer; removes all if size is
     *   less than 0.
     * @return the removed data.
     */
    public synchronized byte[] remove(int size_)
	{
		int occupy_ = getSize();
		if (size_ > occupy_) return null;
		if (size_ <= 0) size_ = occupy_;
		byte[] data_ = buf == null? null: new byte[size_];
		remove(data_, 0, size_);
		return data_;
    }

    /**
     * Removes bytes from the circular buffer to the specified buffer.
     *
     * @param size_ # of bytes to be removed from the buffer; removes all if size is
     *   less than 0.
     * @return the # of bytes being removed.
     */
    public int remove(byte[] buf_, int size_)
	{ return remove(buf_, 0, size_); }
	
    /**
     * Removes bytes from the circular buffer to the specified buffer.
     *
     * @param size_ # of bytes to be removed from the buffer; removes all if size is
     *   less than 0.
     * @param pos_ starting position in <code>buf_</code>.
     * @return the # of bytes being removed.
     */
    public synchronized int remove(byte[] buf_, int pos_, int size_)
	{
		int occupy_ = getSize();
		int bufLength_ = capacity + 1;
		if (size_ <= 0 || size_ > occupy_) size_ = occupy_; // remove all
		if (buf_ != null && pos_ + size_ > buf_.length) {// but constrained by the buffer
			size_ = buf_.length - pos_; 
			if (size_ <= 0) return 0;
		}
		
		if (begin + size_ < bufLength_) {
        	if (buf_ != null) System.arraycopy(buf, begin, buf_, pos_, size_);
			begin += size_;
		}
		else {
            int len_ = bufLength_ - begin;
        	if (buf_ != null) {
				System.arraycopy(buf, begin, buf_, pos_, len_);
				System.arraycopy(buf, 0, buf_, pos_ + len_, size_-len_);
			}
			begin = size_-len_;
		}
        return size_;
    }

	/**
	 * Reads but not removes data from the buffer
	 */
	public synchronized byte[] read(int start_, int len_)
	{
		if (buf == null) return null;
		
		int bufLength_ = capacity + 1;
		start_ = (start_ + begin) % bufLength_;
		int size_ = end >= start_? end - start_: end - start_ + bufLength_;
		if (len_ > size_) return null;
		if (len_ < 0) len_ = size_;
		byte[] data_ = new byte[len_];
		if (start_ + len_ > bufLength_) {
			int firstSegment_ = bufLength_ - start_;
			System.arraycopy(buf, start_, data_, 0, firstSegment_);
			System.arraycopy(buf, 0, data_, firstSegment_, start_ + len_ - bufLength_);
		}
		else {
			System.arraycopy(buf, start_, data_, 0, len_);
		}
		return data_;
	}

    /** returns the (occupied) size of the circular buffer */
    public synchronized int getSize()
	{ return end >= begin? end - begin: end + capacity + 1 - begin;   }
	
	public synchronized int getCapacity()
	{ return capacity; }

	public synchronized int getAvailableSpace()
	{ return capacity - getSize(); }

	public synchronized String toString()
	{
		return getSize() + "/" + getCapacity() + ", begin=" + begin + ", end=" + end;
	}

	public synchronized void resize(int newSize_, boolean shrink_)
	{
		if (capacity > newSize_ && !shrink_) return;
		int bufLength_ = capacity + 1;
		
		byte[] tmp_ = null;

		if (begin <= end) {
			if (end-begin > newSize_) end = begin + newSize_;
			if (buf != null) {
				tmp_ = new byte[newSize_ + 1];
				System.arraycopy(buf, begin, tmp_, 0, end-begin);
			}
		}
		else {
			if (getSize() > newSize_) {
				end = (begin + newSize_) % bufLength_;
				resize(newSize_, shrink_);
				return;
			}
			if (buf != null) {
				tmp_ = new byte[newSize_ + 1];
				System.arraycopy(buf, begin, tmp_, 0, bufLength_-begin);
				System.arraycopy(buf, 0, tmp_, bufLength_-begin, end);
			}
		}
		int size_ = getSize();
		buf = tmp_;
		capacity = newSize_;
		begin = 0; end = size_;
	}
}
