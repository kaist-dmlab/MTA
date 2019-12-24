package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.Params;
import com.mobilemr.trace.struct.GeneralPair;

import drcl.inet.protocol.aodv.AODV;

public class RoutableMultihopSnapshot extends AODV {
	private static final long serialVersionUID = 5744016027518020580L;

	public static final double MAX_QUEUE_PACKET_SIZE = 1;

	public HashMap<Integer, RoutableNode> nid2Node = new HashMap<Integer, RoutableNode>();

	public RoutableMultihopSnapshot(MultihopSnapshot curSnapshot) {
		for (int nid : curSnapshot.getNids()) {
			if (this instanceof DsdvMultihopSnapshot) {
				nid2Node.put(nid, new DsdvNode(nid));
			} else {
				throw new IllegalStateException();
			}
		}
		updateTopology(curSnapshot);
	}

	public int getNodeCount() {
		return nid2Node.size();
	}

	private MultihopSnapshot curSnapshot;

	public ArrayList<Integer> updateTopology(MultihopSnapshot curSnapshot) {
		this.curSnapshot = curSnapshot;

		ArrayList<Integer> prevNids = new ArrayList<Integer>(nid2Node.keySet());
		ArrayList<Integer> curNids = curSnapshot.getNids();
		ArrayList<Integer> departedNids = new ArrayList<Integer>();

		for (int curNid : curNids) {
			if (!prevNids.contains(curNid)) {
				RoutableNode arrivedNode;
				if (this instanceof DsdvMultihopSnapshot) {
					arrivedNode = new DsdvNode(curNid);
				} else {
					throw new IllegalStateException();
				}
				nid2Node.put(curNid, arrivedNode);
			}
		}

		for (int prevNid : prevNids) {
			if (!curNids.contains(prevNid)) {
				nid2Node.remove(prevNid);
				departedNids.add(prevNid);
			}
		}

		for (int srcNid : curNids) {
			RoutableNode srcNode = nid2Node.get(srcNid);
			srcNode.clearRoutingTable();

			for (int dstNid : curNids) {

				if (srcNid == dstNid) {
					srcNode.setNextDstNode(srcNid, srcNode);

				} else {
					ArrayList<Integer> nextDstNids = curSnapshot.getNextNids(
							srcNid, dstNid);
					ArrayList<RoutableNode> nextDstNodes = new ArrayList<RoutableNode>();
					for (int nextDstNid : nextDstNids) {
						RoutableNode nextDstNode = nid2Node.get(nextDstNid);
						nextDstNodes.add(nextDstNode);
					}
					srcNode.setNextDstNodes(dstNid, nextDstNodes);
				}
			}
		}

		return departedNids;
	}

	public void queuePackets(int srcNid, int dstNid, int srcPid, int dstPid,
			double dataSize) {
		RoutableNode srcNode = nid2Node.get(srcNid);

		if (srcNid == dstNid) {
			srcNode.loopbackBuffer.offer(new Packet(AODV.AODVTYPE_RREQ, srcNid,
					dstNid, srcPid, dstPid, dataSize));
		} else {
			double remainingDataSize = dataSize;
			while (remainingDataSize > 0) {
				double curPacketSize = remainingDataSize < MAX_QUEUE_PACKET_SIZE ? remainingDataSize
						: MAX_QUEUE_PACKET_SIZE;
				remainingDataSize -= curPacketSize;
				srcNode.outBuffer.offer(new Packet(AODV.AODVTYPE_RREQ, srcNid,
						dstNid, srcPid, dstPid, curPacketSize));
			}
		}
	}

	public GeneralPair<Double, Integer> sendByUnitTime() {
		ArrayList<RoutableNode> nodes = new ArrayList<RoutableNode>(
				nid2Node.values());
		for (RoutableNode node : nodes) {
			node.throughputInUnitTime = 0;
		}

		while (true) {
			boolean anySentInThisIteration = false;
			for (RoutableNode srcNode : nodes) {
				if (!srcNode.isOutBufferEmpty()) {
					Packet nextP = srcNode.outBuffer.peek();
					RoutableNode nextDstNode = srcNode
							.getNextDstNode(nextP.dstNid);
					if (nextDstNode == null) {
						srcNode.outBuffer.poll();
						continue;
					}
					float curLinkBandwidth = Params.MAX_LINK_BANDWIDTH
							* curSnapshot.getLinkDeliveryRatio(srcNode.id,
									nextDstNode.id);
					if (srcNode.throughputInUnitTime + nextP.size <= curLinkBandwidth
							&& nextDstNode.throughputInUnitTime + nextP.size <= curLinkBandwidth) {
						srcNode.outBuffer.poll();
						if (nextP.dstNid != nextDstNode.id) {
							nextDstNode.outBuffer.offer(nextP);
						} else {
							nextDstNode.completedBuffer.offer(nextP);
						}
						srcNode.throughputInUnitTime += nextP.size;
						nextDstNode.throughputInUnitTime += nextP.size;
						anySentInThisIteration = true;
					}
				}
			}
			if (!anySentInThisIteration) {
				break;
			}
		}
		double sumThroughputInUnitTime = 0;
		int cntNodesInTransmission = 0;
		for (RoutableNode node : nodes) {
			if (node.throughputInUnitTime > 0) {
				sumThroughputInUnitTime += node.throughputInUnitTime;
				cntNodesInTransmission++;
			}
		}
		return new GeneralPair<Double, Integer>(sumThroughputInUnitTime,
				cntNodesInTransmission);
	}

	public double getLoopbackData(int dstNid, int dstPid) {
		double sumLoopbackDataSize = 0;
		RoutableNode dstNode = nid2Node.get(dstNid);
		ArrayList<Packet> dstPidPackets = dstNode.loopbackBuffer
				.getPacketsByPid(dstPid);
		for (Packet dstPidPacket : dstPidPackets) {
			sumLoopbackDataSize += dstPidPacket.size;
		}
		dstNode.loopbackBuffer.removeAll(dstPidPackets);
		return sumLoopbackDataSize;
	}

	public double getCompletedData(int dstNid, int dstPid) {
		double sumCompletedDataSize = 0;
		RoutableNode dstNode = nid2Node.get(dstNid);
		ArrayList<Packet> dstPidPackets = dstNode.completedBuffer
				.getPacketsByPid(dstPid);
		for (Packet dstPidPacket : dstPidPackets) {
			sumCompletedDataSize += dstPidPacket.size;
		}
		dstNode.completedBuffer.removeAll(dstPidPackets);
		return sumCompletedDataSize;
	}

	public boolean anyPendingPackets() {
		for (RoutableNode curNode : nid2Node.values()) {
			if (!curNode.isOutBufferEmpty()) {
				return true;
			}
		}
		return false;
	}

	public double getCommDataSize() {
		double sumAllDataSize = 0;
		for (RoutableNode curNode : nid2Node.values()) {
			for (Packet p : curNode.outBuffer) {
				sumAllDataSize += p.size;
			}
			for (Packet p : curNode.loopbackBuffer) {
				sumAllDataSize += p.size;
			}
			for (Packet p : curNode.completedBuffer) {
				sumAllDataSize += p.size;
			}
		}
		return sumAllDataSize;
	}

	public String toDebugString(int... targetNids) {
		String ret = "";
		for (int targetNid : targetNids) {
			RoutableNode targetNode = nid2Node.get(targetNid);
			ret += targetNode + " , ";
		}
		return ret;
	}

	@Override
	public String toString() {
		return getCommDataSize() + "";
	}

}
