// @(#)SRED.java   9/2002
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

package drcl.inet.core.queue;

import drcl.data.*;
import drcl.util.random.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.InetPacket;
import java.util.*;

/**
 * The <i>Stabalized Random Early Detection</i> queue logic.
 * The codes are ported from Parks'(parks@ee.eng.ohio-state.edu) <i>ns-2</i> codes,
 * which were based on T.J. Ott, T.V. Lakshman, L.H. Wong, "SRED: Stabilized RED", IEEE INFOCOM, 1999.
 * 
 * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
 * @version 2.0 03/27/2001, revised and ported to QLogic by Hung-ying Tyan.
 */
public class SRED extends drcl.comp.queue.QLogic implements drcl.net.PktClassifier
{
	static final String	DTYPE_NONE	= null;	/* ok, no drop */
	static final String	DTYPE_FORCED =	"forced drop";	/* a "forced" drop */
	static final String DTYPE_UNFORCED = "random drop";	/* an "unforced" (random) drop */
	static final int	MAXZOMBIE = 200;    /* max length of zombie list */
	
	/* fixed parameters */
	/** The mark flag, true for marking CE bit instead of early dropping. */
	protected boolean mark = false;
	double	p_sred_max = 0.15;
	double  rprob = 0.8; // probability of replacement

	//parameters for zombie 
	int		hit = 0;
	int		zindex = 0;
	int		zsize = 100; // recommended to be 2 ~ 3 times the queue size
	double	p_sred = 0.0;
	double	p_hit = 0.0;
	double	p_hit_old = 0.0;
	double  lastp; // for debug
		
	RandomNumberGenerator ug;

	ZOMBIEList zombie[] = new ZOMBIEList[MAXZOMBIE];
	{ for (int i=0; i<zombie.length; i++) zombie[i] = new ZOMBIEList(); }

	PktClassifier classifier = this;
	
	public SRED()
	{ super(); }
	
	public SRED(Component host_)
	{ super(host_); }
	
	public SRED(Component host_, String qpid_)
	{ super(host_, qpid_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		SRED that_ = (SRED)source_;
		p_sred_max = that_.p_sred_max;
		rprob = that_.rprob;
		mark = that_.mark;
		if (that_.ug != null)
			setRandomNumberGenerator((RandomNumberGenerator)that_.ug.clone());
		else
			setSeed(that_.getSeed());
	}
	
	public void reset()
	{
		super.reset();
		p_sred = 0.0;
		hit = 0;
		p_hit = 0.0;
		p_hit_old = 0.0;
		zindex = 0;
	
		if(zombie != null)
			for (int i = 0; i < zombie.length; i++) zombie[i].reset();
		if (ug != null) ug.reset();
	}

	public String info(String prefix_)
	{
		return super.info(prefix_) + prefix_ + "RNG: " + (ug == null? "<none>": ug.oneline()) + "\n"
			+ prefix_ + "Flow history:\n" + zombieInfo(prefix_ + "   ")
			+ prefix_ + "1/replacement prob. = " + (1.0/rprob) + "\n"
			+ prefix_ + "1/max. dropping prob. = " + (1.0/p_sred_max) + "\n"
			+ prefix_ + "hit = " + hit + "\n"
			+ prefix_ + "p_sred = " + p_sred + "\n"
			+ prefix_ + "p_hit = " + p_hit + "\n"
			+ prefix_ + "p_hit_old = " + p_hit_old + "\n"
			+ prefix_ + "lastp = " + lastp + "\n";
	}

	// SRED probabilistic drop
	boolean drop_early(Packet pkt_, int pktsize_)
	{
		// compute hit frequency/prob P(pkt)
		//double alpha_ = 1./double(capacity*2);
		double alpha_ = 0.001;
		p_hit_old = p_hit;
		p_hit = p_hit_old * (1.0 - alpha_) + hit * alpha_;

		// compute P_zap
		// As paper
		if (qsize >= capacity/3 && qsize < capacity) {
		 	p_sred = p_sred_max;
		} else if (qsize >= capacity/6 && qsize < capacity/3) {
		 	p_sred = p_sred_max*0.25;
		} else if (qsize >= 0 && qsize < capacity/6) {
		 	p_sred = 0.0;
		}

		//Full SRED
		// Tyan: add pktsize_ for byte mode
		lastp = p_sred*Math.min(1, 1/(Math.pow(((double)capacity/pktsize_/3.)*p_hit,2)))*(1+hit/(p_hit));

		if (lastp > 1.0) lastp = 1.0;
		if (lastp < 0.0) lastp = 0.0;

		// drop probability is computed, pick random number and act
		if (ug == null) ug = new UniformDistribution(0L);
		if (ug.nextDouble() <= lastp) {
			// DROP or MARK
			if (mark && ((InetPacket)pkt_).isECT()) {
				((InetPacket)pkt_).setCE(true);
				return false;	// no drop
			}
			else return true;	// drop
		}
		return false;			// no DROP/mark
	}

	public String adviceOn(Object obj_, int psize_)
	{
		Packet pkt_ = (Packet)obj_;
		int flowId = classifier.classify(pkt_);

		// Since SRED use instantaneous queue size, no averaging needed 

		// added for SRED from here - ZOMBIES
		ZOMBIEList zombie_;
		if (zindex < zombie.length) { // zombie is not full							
			zombie_ = zombie[zindex];
			zombie_.id = flowId;
			zombie_.count = 0;
			zindex++;			
		} else {
			if (ug == null) ug = new UniformDistribution(0L);
			zombie_ = zombie[(int) (ug.nextDouble()*zombie.length)];

			if (zombie_.id == flowId) { // one hit
				zombie_.count++;
				hit = 1; 
			} else { // no hit??
				hit = 0;				
				if (ug.nextDouble() <= rprob) { // with prob replace it
					zombie_.count = 0;
					zombie_.id = flowId;
				}
			}
		} // up to here for zombie

		String droptype = DTYPE_NONE;

		if(drop_early(pkt_, psize_))
			droptype = DTYPE_UNFORCED;
				
		if (droptype == DTYPE_NONE) {
			if (qsize + psize_ > capacity) {
				if (host.isGarbageEnabled() && host.isDebugEnabled())
					return "exceeds capacity:" + qsize + "+" + psize_ + ">" + capacity;
				else
					return "exceeds capacity";
			}
			else
				return null;
		}
		else { // droptype == DTYPE_UNFORCED || droptype == DTYPE_FORCED
			if (host.isGarbageEnabled() && host.isDebugEnabled())
				droptype += ": lastp=" + lastp;
			return droptype;
		}
	}

	// a hash function compute the flowID from packet header information
	public int classify(Packet pkt_) {
		return (int)((((InetPacket)pkt_).getSource() + (((InetPacket)pkt_).getDestination()<<8)
					+((long)((InetPacket)pkt_).getProtocol())<<16) % (Integer.MAX_VALUE-1));
	}

	public void setClassifier(PktClassifier c_)
	{ classifier = c_; }

	public PktClassifier getClassifier()
	{ return classifier; }

	String zombieInfo(String prefix_)
	{
		int total_ = 0;
		Hashtable ht_ = new Hashtable();
		for (int i=0; i<zombie.length; i++) {
			if (zombie[i].count == 0) continue;
			total_ += zombie[i].count;
			IntObj key_ = new IntObj(zombie[i].id);
			IntObj count_ = (IntObj)ht_.get(key_);
			if (count_ == null) {
				count_ = new IntObj(zombie[i].count);
				ht_.put(key_, count_);
			}
			else
				count_.value+= zombie[i].count;
		}
		StringBuffer sb_ = new StringBuffer();
		for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
			IntObj key_ = (IntObj)e_.nextElement();
			IntObj count_ = (IntObj)ht_.get(key_);
			int v_ = count_.value * 10000 / total_;
			sb_.append(prefix_ + "flow " + key_ + ": " + (v_/100) + "." + (v_%100) + "%"
					+ "(" + count_ + "/" + total_ + ")\n");
		}
		return sb_.toString();
	}
	
	/* Scripting */
	
	/** Sets the replacement probability. */
	public void setReplaceProb(double p)
	{ rprob = p; }
	
	/** Returns the replacement probability. */
	public double getReplaceProb()
	{ return rprob; }
	
	/** Sets the maximum dropping probability. */
	public void setSREDMaxProb(double p)
	{ p_sred_max = p; }
	
	/** Returns the maximum dropping probability. */
	public double getSREDMaxProb()
	{ return p_sred_max; }

	public void setSREDParam(double replacep_, double maxp_)
	{ rprob = replacep_; p_sred_max = maxp_; }
	
	/** If enabled, SRED marks the congestion bit of, instead of advising to drop, packets.  */
	public void setMarkEnabled(boolean enabled_)
	{mark = enabled_;} //congestion bit?

	public boolean isMarkEnabled()
	{ return mark; }

	public void setSeed(long seed_)
	{
		if (ug == null) ug = new UniformDistribution(seed_);
		else ug.setSeed(seed_);
	}

	public long getSeed()
	{ return ug == null? 0L: ug.getSeed(); }

	public void setRandomNumberGenerator(RandomNumberGenerator ug_)
	{
		if (ug != null && ug_ != null) ug_.setSeed(ug.getSeed());
		ug = ug_;
	}

	public void setRandomNumberGenerator(RandomNumberGenerator ug_, long seed_)
	{ ug = ug_;  ug.setSeed(seed_); }

	class ZOMBIEList
	{
		int id = 0;
		int count = 0;
		public void reset()
		{ id = 0; count = 0; }
	}		
}
