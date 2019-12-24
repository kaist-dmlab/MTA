package drcl.inet.protocol.aodv.struct;

import drcl.inet.protocol.aodv.AODV_Packet;

public class Packet extends AODV_Packet implements Comparable<Packet> {

	public int srcNid;
	public int dstNid;
	public int srcPid;
	public int dstPid;
	public double size;

	public Packet(int type, int srcNid, int dstNid, int srcPid, int dstPid,
			double size) {
		super(type, srcNid);
		this.srcNid = srcNid;
		this.dstNid = dstNid;
		this.srcPid = srcPid;
		this.dstPid = dstPid;
		this.size = size;
	}

	@Override
	public int compareTo(Packet that) {
		if (this.srcNid < that.srcNid) {
			return -1;
		} else if (this.srcNid > that.srcNid) {
			return 1;
		}
		if (this.dstNid < that.dstNid) {
			return -1;
		} else if (this.dstNid > that.dstNid) {
			return 1;
		}
		if (this.srcPid < that.srcPid) {
			return -1;
		} else if (this.srcPid > that.srcPid) {
			return 1;
		}
		if (this.dstPid < that.dstPid) {
			return -1;
		} else if (this.dstPid > that.dstPid) {
			return 1;
		}
		return Double.compare(this.size, that.size);
	}

}
