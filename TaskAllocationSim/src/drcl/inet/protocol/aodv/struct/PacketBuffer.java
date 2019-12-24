package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class PacketBuffer extends LinkedList<Packet> {
	private static final long serialVersionUID = -3808446337893728753L;

	private double sumDataSize = 0;

	@Override
	public boolean offer(Packet p) {
		boolean ret = super.offer(p);
		sumDataSize += p.size;
		return ret;
	}

	@Override
	public Packet poll() {
		Packet p = super.poll();
		sumDataSize -= p.size;
		return p;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		@SuppressWarnings("unchecked")
		Collection<Packet> packetC = (Collection<Packet>) c;
		packetC.forEach(p -> sumDataSize -= p.size);
		return super.removeAll(c);
	}

	public ArrayList<Packet> getPacketsByPid(int dstPid) {
		ArrayList<Packet> packetsByPid = new ArrayList<Packet>();
		for (Packet p : this) {
			if (p.dstPid == dstPid) {
				packetsByPid.add(p);
			}
		}
		return packetsByPid;
	}

	public double getDataSize() {
		return sumDataSize;
	}

	@Override
	public String toString() {
		return sumDataSize + "";
	}

}
