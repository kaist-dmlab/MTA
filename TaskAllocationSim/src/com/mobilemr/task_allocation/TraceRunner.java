package com.mobilemr.task_allocation;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.heuristic.Heuristic;
import com.mobilemr.task_allocation.platform.AllocationResult;
import com.mobilemr.task_allocation.platform.JobResult;
import com.mobilemr.task_allocation.platform.JobTracker;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.platform.TaskPhaseType;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.history.HistoryStat;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;
import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;

public class TraceRunner {

	private static final boolean DEBUG = false;

	private MultihopTrace trace;
	private JobProfile jp;
	private Class<? extends Heuristic> clsHeuristic;
	private String inputTraceId;
	private HistoryStat historyStat;
	private HashMap<Integer, Integer> zb2OrgNid;

	private int realModeTime;
	private String indent;

	public TraceRunner(MultihopTrace trace, JobProfile jp,
			Class<? extends Heuristic> clsHeuristic, String inputTraceId,
			HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid) {
		Logger.println(clsHeuristic.getSimpleName());

		this.trace = trace;
		this.jp = jp;
		this.clsHeuristic = clsHeuristic;
		this.inputTraceId = inputTraceId;
		this.historyStat = historyStat;
		this.zb2OrgNid = zb2OrgNid;

		realModeTime = 0;
		indent = "";

		Task.resetId();
	}

	public JobResult start() {
		Double mapReliability = null;
		Double redReliability = null;
		Double mapContention = null;
		Double redContention = null;
		Double sumOptimalIdx1 = 0.0;
		Integer cntOptimalIdx1 = 0;
		Double sumOptimalIdx2 = 0.0;
		Integer cntOptimalIdx2 = 0;
		Double sumClusterUtilization = 0.0;
		Integer cntClusterUtilization = 0;
		Double sumEvals = 0.0;

		JobTracker jt = new JobTracker(trace, jp, clsHeuristic, historyStat,
				zb2OrgNid);

		AllocationResult allocationResult = jt.allocateNextTasks(inputTraceId,
				0);

		mapReliability = allocationResult.reliability;
		mapContention = allocationResult.contention;

		if (allocationResult.optimalIdx1 != null) {
			sumOptimalIdx1 += allocationResult.optimalIdx1;
			cntOptimalIdx1++;
		}
		if (allocationResult.optimalIdx2 != null) {
			sumOptimalIdx2 += allocationResult.optimalIdx2;
			cntOptimalIdx2++;
		}
		if (allocationResult.clusterUtilization != null) {
			sumClusterUtilization += allocationResult.clusterUtilization;
			cntClusterUtilization++;
		}
		if (allocationResult.numEvals != null) {
			sumEvals += allocationResult.numEvals;
		}

		RoutableMultihopSnapshot curRoutableSnapshot = trace
				.getClosestRoutableMultihopSnapshot(realModeTime);

		while (!jt.isJobFinished(curRoutableSnapshot)) {
			if (DEBUG) {
				Logger.println(indent + "TR - 현재 Time : " + realModeTime);
			}

			MultihopSnapshot curSnapshot = trace
					.getClosestSnapshot(realModeTime);
			ArrayList<Integer> departedNids = curRoutableSnapshot
					.updateTopology(curSnapshot);

			jt.updateTaskDepartures(departedNids);

			if (jt.isCurTaskPhaseFailed(true)) {
				return JobResult.failed(jt.getCurTaskPhaseType(),
						mapReliability, redReliability, mapContention,
						redContention, realModeTime);
			}

			if (jt.isJobFinished(curRoutableSnapshot)) {
				break;
			}

			if (jt.isCurTaskPhaseFinished(curRoutableSnapshot)) {
				if (DEBUG) {
					Logger.println(indent + "TR - 현재 Time : " + realModeTime
							+ " | Placing Tasks | AvgThroughput : "
							+ jt.getTraffic());
				}

				allocationResult = jt.allocateNextTasks(inputTraceId,
						realModeTime);

				TaskPhaseType curTpType = jt.getCurTaskPhaseType();
				if (curTpType == TaskPhaseType.MAP) {
					redReliability = allocationResult.reliability;
					redContention = allocationResult.contention;
				}
				if (allocationResult.clusterUtilization != null) {
					sumClusterUtilization += allocationResult.clusterUtilization;
					cntClusterUtilization++;
				}
				if (allocationResult.numEvals != null) {
					sumEvals += allocationResult.numEvals;
				}
			}

			jt.updateByUnitTime(curRoutableSnapshot, realModeTime);

			realModeTime++;
		}

		Logger.println("Job 종료");
		Logger.println("realModeTime : " + realModeTime);
		jt.printStatus(curRoutableSnapshot);
		// jt.printResult();
		Logger.println();

		double traffic = jt.getTraffic();
		double optimalIdx1 = sumOptimalIdx1 / cntOptimalIdx1;
		double optimalIdx2 = sumOptimalIdx2 / cntOptimalIdx2;
		double avgClusterUtilization = sumClusterUtilization
				/ cntClusterUtilization;
		return JobResult.succeeded(mapReliability, redReliability,
				mapContention, redContention, traffic, realModeTime,
				// cdfRecordMap,
				optimalIdx1, optimalIdx2, avgClusterUtilization, sumEvals,
				null, jt.getReplicateDuration(), jt.getShuffleDuration());
	}

}
