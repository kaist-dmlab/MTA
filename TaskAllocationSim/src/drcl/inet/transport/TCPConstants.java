// @(#)TCPConstants.java   1/2004
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

package drcl.inet.transport;

public interface TCPConstants
{
	public static final int DEFAULT_PID = 6;
	
	/** Port ID of the congestion window changed event port. */
	public static final String CWND_PORT_ID = "cwnd";
	/** Port ID of the slow start threshold changed event port. */
	public static final String SST_PORT_ID = "sst";
	/** Port ID of the SRTT event port. */
	public static final String SRTT_PORT_ID = "srtt";
	/** Port ID of the RTTVAR event port. */
	public static final String RTTVAR_PORT_ID = "rttvar";
	/** Port ID of the sequence number sent/received event port. */
	public static final String SEQNO_PORT_ID = "seqno";
	/** Port ID of the sequence number received event port. */
	public static final String ACK_PORT_ID = "ack";
	/** Port ID of the sequence number sent event port. */
	public static final String SEQNO_SENT_PORT_ID = "seqno_sent";
	/** Port ID of the sequence number received event port. */
	public static final String SEQNO_RCV_PORT_ID = "seqno_rcv";

	/** Name of the congestion window changed event. */
	public static final String CWND_EVENT = "CWND";
	/** Name of the slow start threshold changed event. */
	public static final String SST_EVENT = "S. S. Threshold";
	/** Name of the SRTT event. */
	public static final String SRTT_EVENT = "SRTT";
	/** Name of the RTTVAR event. */
	public static final String RTTVAR_EVENT = "RTTVAR";
	/** Name of the sequence number sent/received event. */
	public static final String SEQNO_EVENT = "Seq#";
	/** Name of the sequence number sent event. */
	public static final String SEQNO_SENT_EVENT = "Seq# Sent";
	/** Name of the sequence number received event. */
	public static final String SEQNO_RCV_EVENT = "Seq# Received";
	/** Name of the sequence number received event. */
	public static final String ACK_EVENT = "Ack#";

	/** Retransmission timeout event ID. */
	public static final int RXT_EVT = 0;
	/** Vegas RTT timeout event ID. */
	public static final int RTT_EVT = 1;
	/** The delay ACK timeout event ID. */
	public static final int DELAY_ACK = 2;
	public static final String[] TIMEOUT_TYPES =
		{"rxt", "vegas_rtt", "delay_ack"};
	
	/* * Number of duplicate ACKs to trigger fast RXT as specified in [RFC2581]
	 * (for Reno, Tahoe). * /
	public static int NUMDUPACKS = 3;
	*/

	public static final String RENO = "RENO";
	public static final String TAHOE = "TAHOE";
	public static final String VEGAS = "VEGAS";
	public static final String NEW_RENO = "NEW-RENO";

	/* * Default advertisement window size (unit of MSS bytes). * /
	public static int AWND_DEFAULT = 128;
	/** Default maximum congestion window size (unit of MSS bytes). * /
	public static int MAXCWND_DEFAULT = 128;
	/** Initial slow-start threshold (unit of MSS bytes). * /
	public static int INIT_SS_THRESHOLD = 20;
	*/

	public static int CLOSED = 0;
	public static int LISTEN = 1;
	public static int SYN_RCVD = 2;
	public static int SYN_SENT = 3;
	public static int ESTABLISHED = 4;
	public static int FIN_WAIT_1 = 5;
	public static int CLOSING = 6;
	public static int FIN_WAIT_2 = 7;
    public static int TIME_WAIT = 8;
	public static int CLOSE_WAIT = 9;
	public static int LAST_ACK = 10;
	public static int PRE_SYN_SENT = 11;
	public static int PRE_CLOSED = 12;
	public static int PRE_LAST_ACK = 13;
	public static int SEND = 14;
	public static int RECEIVE = 15;
	public static int LISTEN1 = 16;
	public static int ESTABLISHED_OVER = 17;
	public static int LISTEN2 = 18;
	public static int ESTABLISHED_FIN = 19;

	public static String[] STATES = {
		"closed",
		"listen",
		"syn_rcvd",
		"syn_sent",
		"established",
		"fin_wait_1",
		"closing",
		"fin_wait_2",
		"time_wait",
		"close_wait",
		"last_ack",
		"pre_syn_sent",
		"pre_closed",
		"pre_last_ack",
		"send",
		"receive",
		"listen1",
		"established_over",
		"listen2",
		"established_fin"
	};
}
