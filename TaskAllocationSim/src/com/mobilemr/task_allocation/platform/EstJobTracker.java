package com.mobilemr.task_allocation.platform;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.ComparisonUtil;

import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.Task.Status;
import com.mobilemr.task_allocation.profile.CommType;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.task_allocation.util.Indenter;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;
import drcl.inet.protocol.aodv.struct.RoutableNode;

public class EstJobTracker {

	private static final boolean DEBUG = false;

	protected String indent;

	public TaskPhase curTaskPhase;

	public EstJobTracker() {
		indent = "";
	}

	public EstJobTracker(TaskPhaseProfile curTpp, TaskPhaseProfile nextTpp,
			HashMap<Integer, Task> srcTid2Task,
			ArrayList<OrderedNumPair<Integer>> conns, int curTime) {
		indent = Indenter.get(1);

		curTaskPhase = new TaskPhase(curTpp, nextTpp, srcTid2Task, conns,
				indent);

		curTaskPhase.start();
	}

	public TaskPhaseType getCurTaskPhaseType() {
		return curTaskPhase.tpp.tpType;
	}

	public boolean isCurTaskPhaseFailed(boolean realMode) {
		if (curTaskPhase.srcTid2Task.isEmpty()
				|| (!curTaskPhase.isLastTaskPhase() && curTaskPhase.dstTid2Task
						.isEmpty())) {
			return true;
		}

		int numSrcTasks = curTaskPhase.srcTid2Task.size();
		int numDstTasks = curTaskPhase.dstTid2Task.size();
		int numFailedTasks = curTaskPhase.failedTid2Task.size();
		// if (realMode) {
		// System.out.println(curTaskPhase.srcTid2Task);
		// System.out.println(curTaskPhase.tpp.commType + " "
		// + numSrcTasks + " " + numDstTasks + " "
		// + numFailedTasks);
		// }
		double curAliveTaskFraction = (double) (numSrcTasks + numDstTasks)
				/ (numSrcTasks + numDstTasks + numFailedTasks);
		return ComparisonUtil.lessThan(curAliveTaskFraction, 1);
	}

	public boolean isCurTaskPhaseFinished(
			RoutableMultihopSnapshot curRoutableSnapshot) {
		return curTaskPhase.isFinished(curRoutableSnapshot);
	}

	private double sumThroughput = 0;
	private double sumBandwidthUtilization = 0;
	private int commDuration = 0;
	private int replicateDuration = 0;
	private int shuffleDuration = 0;

	public void updateByUnitTime(RoutableMultihopSnapshot curRoutableSnapshot,
			int curTime) {
		if (!curTaskPhase.isProcessingFinished()) {
			ArrayList<Task> srcTasks = new ArrayList<Task>(
					curTaskPhase.srcTid2Task.values());
			for (Task srcTask : srcTasks) {
				srcTask.processByUnitTime(curRoutableSnapshot);

				if (srcTask.pendingInputSize == 0) {
					srcTask.status = Status.PROC_FINISHED;
				}
			}

		} else {
			if (!curTaskPhase.isLastTaskPhase()) {
				GeneralPair<Double, Integer> result = curRoutableSnapshot
						.sendByUnitTime();
				double sumThroughputInUnitTime = result.getFirst();
				sumThroughput += sumThroughputInUnitTime;
				int cntNodesInTransmission = result.getSecond();
				if (sumThroughputInUnitTime > 0) {
					if (cntNodesInTransmission == 0) {
						throw new IllegalStateException(sumThroughputInUnitTime
								+ " " + cntNodesInTransmission);
					}
					double maxNetworkBandwidth = curRoutableSnapshot
							.getNodeCount() * Params.MAX_LINK_BANDWIDTH;
					double bandwidthUtilizationInUnitTime = sumThroughputInUnitTime
							/ maxNetworkBandwidth;
					sumBandwidthUtilization += bandwidthUtilizationInUnitTime;
					commDuration++;
					if (curTaskPhase.tpp.commType == CommType.REPLICATE) {
						replicateDuration++;
					} else if (curTaskPhase.tpp.commType == CommType.SHUFFLE) {
						shuffleDuration++;
					}
				}
			}
		}

		for (Task dstTask : curTaskPhase.dstTid2Task.values()) {
			dstTask.onReceive(curRoutableSnapshot, curTaskPhase.tpp.tpType,
					curTime);
		}
	}

	private HashMap<Integer, Task> totalFailedTid2Task = new HashMap<Integer, Task>();

	public void updateTaskDepartures(ArrayList<Integer> departedNids) {
		if (departedNids.isEmpty()) {
			return;
		}

		ArrayList<Integer> curSrcTids = new ArrayList<Integer>(
				curTaskPhase.srcTid2Task.keySet());
		for (int curSrcTid : curSrcTids) {
			Task curSrcTask = curTaskPhase.srcTid2Task.get(curSrcTid);
			if (departedNids.contains(curSrcTask.nid)) {
				curTaskPhase.failedTid2Task.put(curSrcTid, curSrcTask);
				totalFailedTid2Task.put(curSrcTid, curSrcTask);
				curTaskPhase.srcTid2Task.remove(curSrcTid);
			}

			curSrcTask.dstTasks.removeIf(curDstTask -> departedNids
					.contains(curDstTask.nid));
		}

		ArrayList<Integer> curDstTids = new ArrayList<Integer>(
				curTaskPhase.dstTid2Task.keySet());
		for (int curDstTid : curDstTids) {
			Task curDstTask = curTaskPhase.dstTid2Task.get(curDstTid);
			if (departedNids.contains(curDstTask.nid)) {
				curTaskPhase.failedTid2Task.put(curDstTid, curDstTask);
				totalFailedTid2Task.put(curDstTid, curDstTask);
				curTaskPhase.dstTid2Task.remove(curDstTid);
			}
		}
	}

	public void printStatus(RoutableMultihopSnapshot curRoutableSnapshot) {
		Logger.println(indent + "Current Cluster Data Size : "
				+ curTaskPhase.getPendingInputSize() + " _ "
				+ curTaskPhase.getOutputBufferSize() + " _ "
				+ curRoutableSnapshot.getCommDataSize());

		double sumInputDataSize = 0;
		double sumOutputDataSize = 0;
		for (Task t : totalFailedTid2Task.values()) {
			sumInputDataSize += t.pendingInputSize;
			sumOutputDataSize += t.outputBuffer;
		}
		Logger.println(indent + "Failed Data Size : " + sumInputDataSize
				+ " _ " + sumOutputDataSize);
		double sumDataSize = curTaskPhase.getPendingInputSize()
				+ curTaskPhase.getOutputBufferSize()
				+ curRoutableSnapshot.getCommDataSize() + sumInputDataSize
				+ sumOutputDataSize;
		Logger.println(indent + "Sum Data Size : " + sumDataSize);

		if (DEBUG) {
			for (RoutableNode node : curRoutableSnapshot.nid2Node.values()) {
				Logger.println(indent + node.id);
				Logger.println(indent + "Dist : " + node.timeDistStarted
						+ " _ " + node.timeDistFinished);
				Logger.println(indent + "Map : " + node.timeMapStarted + " _ "
						+ node.timeMapFinished);
				Logger.println(indent + "Red : " + node.timeReduceStarted
						+ " _ " + node.timeReduceFinished);
			}
		}
	}

	public void printResult() {
		for (Task activeDstTask : curTaskPhase.srcTid2Task.values()) {
			Logger.println(indent + activeDstTask);
		}
	}

	public double getTraffic() {
		return sumThroughput;
	}

	public double getAvgBandwidthUtilization() {
		return sumBandwidthUtilization / commDuration;
	}

	public int getCommDuration() {
		return commDuration;
	}

	public int getReplicateDuration() {
		return replicateDuration;
	}

	public int getShuffleDuration() {
		return shuffleDuration;
	}

}
