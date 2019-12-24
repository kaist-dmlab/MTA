// @(#)UIUCInjector.java   10/2003
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

package drcl.inet.protocol.aodv;

import drcl.inet.*;
import drcl.net.*;
import drcl.comp.*;

/**
 * UIUCInjector.java: The fault injection component for AODV packets.
 * The code is ported from the ns-2 code written by Yury A. Perzov.
 * This program accept three parameters, SINGLE|CONSTANT RREQ|RREP|RERR Bit.
 * It can flip one bit in each AODV packets at each node.
 * This componet is supposed to be placed below pktDispatcher and above LL.
 * It should also be able to be placed in other positions but we haven't tested it yet.
 *
 * @author Wei-peng Chen
 */
public class UIUCInjector extends drcl.net.Module
{
  /* Injection Type */
  final static int INJTYPE_NONE     = 0;
  final static int INJTYPE_SINGLE   = 1; 
  final static int INJTYPE_CONSTANT = 2; 
  final static String[] INJTYPE_NAME = { "NONE", "SINGLE", "CONSTANT" };
  
  // should be changed when the packet headers changed
  final static int AODV_RREQ_PKT_SIZE = 24; /*byte*/ 
  final static int AODV_RREP_PKT_SIZE = 20; /*byte*/ 
  final static int AODV_RERR_PKT_SIZE = 12; /*byte*/ 
 
  public static final int DEBUG_ALL     = 0; 
  public static final int DEBUG_INJ     = 1; 
  public static final int DEBUG_RREQ    = 2; 
  public static final int DEBUG_RREP    = 3; 
  public static final int DEBUG_RERR    = 4; 
  static final String[] DEBUG_LEVELS = {
                "all", "inj", "rreq", "rrep", "rerr"
  };

  public String[] getDebugLevelNames()
  { return DEBUG_LEVELS; }

  //injector status variables
  protected int	e_inj_type;   // case be INJTYPE_NONE(0), INJTYPE_SINGLE(1), INJTYPE_CONSTANT(2)
  protected int inj_target_ptype; //target packet type AODV_UU, IP ...
  protected int inj_target_msg_type; //target msg type within packet type
  protected int inj_target_bit;
  protected int inj_mask;
  protected int inj_target_byte; 

  public String getName() { return "UIUCInjector"; } 

  public UIUCInjector() {
    super();
    e_inj_type = INJTYPE_NONE; 
    inj_target_ptype = -1; /* equivalent to PT_NTYPE in ns-2 */
    inj_target_msg_type = 0;
    inj_mask = 0x00;
    inj_target_byte = 0;
 }

  public String toString() {
    return "mode: " + e_inj_type + " pkt_type: " + inj_target_msg_type + " bit: " + inj_target_bit;
  }

  /**
   * Inject a bit flip to the packet header of AODV packet.
   * It can be extented to other types of packets.
   */
  public void inject( String inj_type_, String pkt_type_, String bit_) {
    /*inject INJTYPE_SINGLE|INJTYPE_CONSTANT TYPE BIT*/
    /*inject "injection type" "packet type" "bit"*/
      
    //set the injection type
    inj_target_ptype = InetConstants.PID_AODV; /* XXX: can also be given by script file */

    if ( inj_type_.equalsIgnoreCase("SINGLE") ) {
      e_inj_type = INJTYPE_SINGLE;
    } else if ( inj_type_.equalsIgnoreCase("CONSTANT") ) {
      e_inj_type = INJTYPE_CONSTANT;
    } else {
      e_inj_type = INJTYPE_NONE;
    }
      
    //set packet type
    // hello message should have the same type as RREP but not RREQ
    if ( pkt_type_.equalsIgnoreCase("RREQ") ){
      inj_target_msg_type = AODV.AODVTYPE_RREQ;
    } else if ( pkt_type_.equalsIgnoreCase("RREP") || pkt_type_.equalsIgnoreCase("HELLO") ) {
      inj_target_msg_type = AODV.AODVTYPE_RREP;
    } else if ( pkt_type_.equalsIgnoreCase("RERR") ) {
      inj_target_msg_type = AODV.AODVTYPE_RERR;
    } else if ( pkt_type_.equalsIgnoreCase("ACK") ) {
      inj_target_msg_type = AODV.AODVTYPE_RREP_ACK;
    } else { 
      error("inject()", "UIUCInjector: " + pkt_type_ + ": unknown packet type, shold be (RREQ, RREP, RERR, ACK");
      return;
    }

    //set the bit
    inj_target_bit = Integer.parseInt(bit_);
    inj_target_byte = inj_target_bit / 8;
      
    if (validateInjIndx(inj_target_byte,inj_target_msg_type) == false) {
      error( "inject()", "UIUCInjector: inject beyond packet boundary." );
      return;
    };

    inj_mask = 0x01;
    inj_mask = inj_mask << (7-(inj_target_bit % 8));

    if (isDebugEnabled()&& (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_ALL))) {
      debug("The target byte,mask is " + inj_target_byte + ", " +  inj_mask);      
    }
    return;
  }

  /** Test whether the byte number is correct.
   * index shoube be from 0 to PKT_SIZE -1, 
   * Note there is an error in the original program 
   * if index == PKT_SIZE, it is incorrect 
   */
  protected boolean validateInjIndx(int index, int type){
    boolean status = false;
   
    if (index < 0) return false;
    switch (type) {
      case AODV.AODVTYPE_RREQ:
	if (index < AODV_RREQ_PKT_SIZE ) status = true;
	break;
      
      case AODV.AODVTYPE_HELLO:
      case AODV.AODVTYPE_RREP:
	if (index < AODV_RREP_PKT_SIZE ) status = true;
	break;
  
      case AODV.AODVTYPE_RERR:
	//since the rerr packet has various packet size, we only support to flip the fields till the first pair
	if (index < AODV_RERR_PKT_SIZE ) status = true;
	break;

      case AODV.AODVTYPE_RREP_ACK:
	error("validateInjIndx()", "RREP_ACK not implemented in J-Sim yet!");
	break;
    }
    if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ALL)) {
      debug("Validation status "  + status);
    }
    return status;
  }


  /**
   * Handle data arriving at the upPort from the component PktDispatcher.
   * Note the data_ should be the type: InetPacket from PktDispatcher.
   *
   * @param data_ message body arriving at the down port.
   * @param upPort_ up port at which messages arrive.
   */
  public void dataArriveAtUpPort(Object data_, drcl.comp.Port upPort_)
  {
    if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ALL)) {
      debug("FaultInject dataArriveAtUpPort " + data_);
    }

    InetPacket ipkt_ = (InetPacket)data_; 
     
    /* Warning: the method below may not be applied to the case of packet-in-packet */ 
    int pkt_type_ = ipkt_.getProtocol();
    if (pkt_type_ == inj_target_ptype)  {
      if (e_inj_type != INJTYPE_NONE) {
        AODV_Packet aodv_pkt_ = (AODV_Packet)ipkt_.getBody(); 
        if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_INJ) || isDebugEnabledAt(DEBUG_ALL))) {
	  debug("Before Injecting! " + aodv_pkt_ + " bit " + inj_target_bit);
        }
	injectAODVUU(aodv_pkt_);
        if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_INJ) || isDebugEnabledAt(DEBUG_ALL))) {
	  debug("After Injecting! " + aodv_pkt_ + " bit " + inj_target_bit);
        }
      
        if (e_inj_type == INJTYPE_SINGLE) {
	  e_inj_type = INJTYPE_NONE;
        }
      }
    }
    downPort.doSending(data_);
    return;
  }

  protected void dataArriveAtDownPort(Object data_, Port downPort_) {
    upPort.doSending(data_);
  }

  public void injectAODVUU(AODV_Packet pkt_) {
    int pkt_type_ = pkt_.getType();
    
    if (inj_target_msg_type == pkt_type_) {
      /* 
      // the type of packet is the first byte
      if (inj_target_byte > -1) {
        char * p_ptr = (char *) target_hdr;
        printf("Before injection %2x \n",target_hdr->type);
        p_ptr[inj_target_byte]=p_ptr[inj_target_byte]^inj_mask;
        printf("After injection %2x \n",target_hdr->type);
      }*/
      /* XXX: It is quite inefficient to map each bit into the field of pkt header in Javasim,
	 any better idea?? */
      switch (pkt_type_) {
        case AODV.AODVTYPE_RREQ:
          flipBitRREQ(pkt_, inj_target_bit);
          break;
        case AODV.AODVTYPE_HELLO:
        case AODV.AODVTYPE_RREP:
          flipBitRREP(pkt_, inj_target_bit);
          break;
        case AODV.AODVTYPE_RERR:
          flipBitRERR(pkt_, inj_target_bit);
          break;
        case AODV.AODVTYPE_RREP_ACK:
          if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ALL)) {
	    debug("RREP_ACK not implemented yet !");
	  }
          break;
        default:
          if (isDebugEnabled()) {
            debug(" Invalid AODV type" + pkt_type_);
          }
          System.exit(1);
      }
    }
  }

  /** please refer to draft sec. 5 */
  protected void flipBitRREQ(AODV_Packet pkt_, int bit) {
    AODV_RREQ req = (AODV_RREQ) pkt_.getBody();

    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" before flipBitRREQ " + req);
    }
    if ( bit <= 7 ) {
      req.rq_type = (req.rq_type & 0xff) ^ inj_mask;
      // if we change the value of type, we should also change the type value in AODV_Packet
      pkt_.setType(req.rq_type);      
    } else if (bit <= 8) {
      req.rq_j_flag = (req.rq_j_flag & 0x01) ^ 0x01;
    } else if (bit <= 9) {
      req.rq_r_flag = (req.rq_r_flag & 0x01) ^ 0x01;
    } else if (bit <= 10) {
      req.rq_g_flag = (req.rq_g_flag & 0x01) ^ 0x01;
    } else if (bit <= 11) {
      req.rq_d_flag = (req.rq_d_flag & 0x01) ^ 0x01;
    } else if (bit <= 12) {
      req.rq_u_flag = (req.rq_u_flag & 0x01) ^ 0x01;
    } else if (bit <= 23) {
      ; // rezserved bit, do nothing
    } else if (bit <= 31) {
      req.rq_hop_count = (req.rq_hop_count & 0xff) ^ inj_mask;
    } else if (bit <= 63) {
      req.rq_bcast_id = (req.rq_bcast_id ^ (0x00000001 << (31-(bit - 32))));
    } else if (bit <= 95) {
      req.rq_dst = (req.rq_dst ^ (0x00000001 << (31-(bit - 64))));
    } else if (bit <= 127) {
      req.rq_dst_seqno = (req.rq_dst_seqno ^ (0x00000001 << (31-(bit - 96))));
    } else if (bit <= 159) {
      req.rq_src = (req.rq_src ^ (0x00000001 << (31-(bit - 128))));
    } else if (bit <= 191) {
      req.rq_src_seqno = (req.rq_src_seqno ^ (0x00000001 << (31-(bit - 160))));
    }
    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" after flipBitRREQ " + req);
    }
  }
  
  protected void flipBitRREP(AODV_Packet pkt_, int bit) {
    AODV_RREP rep = (AODV_RREP) pkt_.getBody();
    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" before flipBitRREP " + rep);
    }
    if ( bit <= 7 ) {
      rep.rp_type = (rep.rp_type & 0xff) ^ inj_mask;
      // if we change the value of type, we should also change the type value in AODV_Packet
      pkt_.setType(rep.rp_type);      
    } else if (bit <= 8) {
      ; // do nothing, R flag, not implemented in J-Sim
      //rep.rp_r_flag = (rep.rp_r_flag & 0x01) ^ 0x01;
    } else if (bit <= 9) {
      ; // do nothing, A flag, not implemented in J-Sim
      //rep.rp_a_flag = (rep.rp_a_flag & 0x01) ^ 0x01;
    } else if (bit <= 18) {
      ; // rezserved bit, do nothing
    } else if (bit <= 23) {
      ; // do nothing, prefix size, not implemented in J-sim    
    } else if (bit <= 31) {
      rep.rp_hop_count = (rep.rp_hop_count & 0xff) ^ inj_mask;
    } else if (bit <= 63) {
      rep.rp_dst = (rep.rp_dst ^ (0x00000001 << (31-(bit - 32))));
    } else if (bit <= 95) {
      rep.rp_dst_seqno = (rep.rp_dst_seqno ^ (0x00000001 << (31-(bit - 64))));
    } else if (bit <= 127) {
      rep.rp_src = (rep.rp_src ^ (0x00000001 << (31-(bit - 96))));
    } else if (bit <= 159) {
      rep.rp_lifetime = (((int) rep.rp_lifetime) ^ (0x00000001 << (31-(bit - 128))));
    }
    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" after flipBitRREP " + rep);
    }
  }

  protected void flipBitRERR(AODV_Packet pkt_, int bit) {
    AODV_RERR err = (AODV_RERR) pkt_.getBody();
    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" before flipBitRERR " + err);
    }
    // check whether bit consists with DestCount
    int pair = (bit-32) / 64 + 1;
    if (pair > err.DestCount) {
      error("flipBitRERR()", "Mismatch DestCount: " + err.DestCount + " with intended flipped bit: " + bit);
      return;
    }

    if ( bit <= 7 ) {
      err.re_type = (err.re_type & 0xff) ^ inj_mask;
      // if we change the value of type, we should also change the type value in AODV_Packet
      pkt_.setType(err.re_type);      
    } else if (bit <= 8) {
      ; // do nothing, N flag, not implemented in J-Sim
      //err.re_n_flag = (err.re_n_flag & 0x01) ^ 0x01;
    } else if (bit <= 23) {
      ; // rezserved bit, do nothing
    } else if (bit <= 31) {
      err.DestCount = (err.DestCount & 0xff) ^ inj_mask;
    } 
    int odd_or_even = ((bit-32) / 32) % 2;
    int bit_shift = 31-((bit-32) % 32);

    if (odd_or_even == 0) {   
      err.unreachable_dst[pair-1] = (err.unreachable_dst[pair-1] ^ (0x00000001 << bit_shift));
    } else {
      err.unreachable_dst_seqno[pair-1] = (err.unreachable_dst_seqno[pair-1] ^ (0x00000001 << bit_shift));
    }
    if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_ALL))) {
      debug(" after flipBitRERR " + err);
    }
  }
}
