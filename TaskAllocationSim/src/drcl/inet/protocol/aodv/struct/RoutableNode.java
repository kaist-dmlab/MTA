package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.platform.TaskPhaseType;

public abstract class RoutableNode implements Comparable<RoutableNode> {

	public int id;
	public float throughputInUnitTime;
	public PacketBuffer outBuffer = new PacketBuffer();

	public PacketBuffer loopbackBuffer = new PacketBuffer();
	public PacketBuffer completedBuffer = new PacketBuffer();

	public RoutableNode(int id) {
		this.id = id;
	}

	protected HashMap<Integer, ArrayList<? extends RoutableNode>> id2Node = new HashMap<>();

	public void clearRoutingTable() {
		id2Node.clear();
	}

	public void setNextDstNode(int dstNid, RoutableNode nextDstNode) {
		ArrayList<RoutableNode> nextDstNodes = new ArrayList<>();
		nextDstNodes.add(nextDstNode);
		id2Node.put(dstNid, nextDstNodes);
	}

	public void setNextDstNodes(int dstNid, ArrayList<RoutableNode> nextDstNodes) {
		id2Node.put(dstNid, nextDstNodes);
	}

	public abstract RoutableNode getNextDstNode(int dstNid);

	public boolean isOutBufferEmpty() {
		return outBuffer.isEmpty();
	}

	public int timeDistStarted = -1;
	public int timeDistFinished = -1;
	public int timeMapStarted = -1;
	public int timeMapFinished = -1;
	public int timeReduceStarted = -1;
	public int timeReduceFinished = -1;

	public void markCommStarted(TaskPhaseType tpType, int timeUnitStarted) {
		// 처음 시작 시간만 한 번 기록
		if (tpType == TaskPhaseType.DIST) {
			if (timeDistStarted == -1) {
				timeDistStarted = timeUnitStarted;
			}
		} else if (tpType == TaskPhaseType.MAP) {
			if (timeMapStarted == -1) {
				timeMapStarted = timeUnitStarted;
			}
		} else if (tpType == TaskPhaseType.REDUCE) {
			if (timeReduceStarted == -1) {
				timeReduceStarted = timeUnitStarted;
			}
		} else {
			throw new IllegalStateException(tpType + "");
		}
	}

	public void markCommFinished(TaskPhaseType tpType, int timeUnitFinished) {
		if (tpType == TaskPhaseType.DIST) {
			timeDistFinished = timeUnitFinished;
		} else if (tpType == TaskPhaseType.MAP) {
			timeMapFinished = timeUnitFinished;
		} else if (tpType == TaskPhaseType.REDUCE) {
			timeReduceFinished = timeUnitFinished;
		} else {
			throw new IllegalStateException(tpType + "");
		}
	}

	public int getCommDuration(TaskPhaseType tpType) {
		if (tpType == TaskPhaseType.DIST) {
			if ((timeDistStarted == -1 && timeDistFinished != -1)
					|| (timeDistStarted != -1 && timeDistFinished == -1)) {
				throw new IllegalStateException(timeDistStarted + " "
						+ timeDistFinished);
			}
			return timeDistFinished - timeDistStarted;

		} else if (tpType == TaskPhaseType.MAP) {
			if ((timeMapStarted == -1 && timeMapFinished != -1)
					|| (timeMapStarted != -1 && timeMapFinished == -1)) {
				throw new IllegalStateException(timeMapStarted + " "
						+ timeMapFinished);
			}
			return timeMapFinished - timeMapStarted;

		} else if (tpType == TaskPhaseType.REDUCE) {
			if ((timeReduceStarted == -1 && timeReduceFinished != -1)
					|| (timeReduceStarted != -1 && timeReduceFinished == -1)) {
				throw new IllegalStateException(timeReduceStarted + " "
						+ timeReduceFinished);
			}
			return timeReduceFinished - timeReduceStarted;

		} else {
			throw new IllegalStateException(tpType + "");
		}
	}

	@Override
	public int compareTo(RoutableNode that) {
		return Integer.compare(this.id, that.id);
	}

	@Override
	public String toString() {
		String ret = "";
		ret += "id: " + id + " _ ";
		ret += "out: " + outBuffer + " _ ";
		ret += "cp: " + completedBuffer;
		return ret;
	}

}
