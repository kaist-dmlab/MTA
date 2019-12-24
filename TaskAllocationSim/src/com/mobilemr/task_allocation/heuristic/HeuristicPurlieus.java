package com.mobilemr.task_allocation.heuristic;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.CommType;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.JobWeightType;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;

public class HeuristicPurlieus extends Heuristic {

	@Override
	public HashMap<Integer, Task> allocateData(JobProfile jp,
			MultihopSnapshot initSnapshot) {
		int numInitNodes = (int) (initSnapshot.getNodeCount() * Params.MAX_CLUSTER_UTILIZATION);
		double inputSize_Per_Task = jp.inputSize / numInitNodes;

		// JobWeightType 에 따라 다른 Dst Nid 목록 선택
		ArrayList<Integer> initNids;
		if (jp.weightType == JobWeightType.MAP_INPUT_HEAVY) {
			// 아무데나
			initNids = initSnapshot.getRandomNids(numInitNodes);

		} else if (jp.weightType == JobWeightType.RED_INPUT_HEAVY) {
			// 아무데나
			initNids = initSnapshot.getRandomNids(numInitNodes);

		} else if (jp.weightType == JobWeightType.MR_INPUT_HEAVY) {
			// K-Club
			initNids = initSnapshot.getKClubNids(numInitNodes);

		} else {
			throw new IllegalStateException(jp.weightType + "");
		}

		HashMap<Integer, Task> initTid2Task = new HashMap<>();
		for (int initNid : initNids) {
			int tid = Task.generateId();
			initTid2Task.put(tid, new Task(tid, initNid, inputSize_Per_Task));
		}
		return initTid2Task;
	}

	@Override
	public GeneralPair<ArrayList<OrderedNumPair<Integer>>, Double> allocateTasks(
			String inputTraceId, JobProfile jp, TaskPhaseProfile tpp,
			TaskPhaseProfile nextTpp, MultihopTrace trace, int realModeTime,
			HashMap<Integer, Task> srcTid2Task, ArrayList<Integer> srcNids,
			HashMap<Integer, Integer> srcNid2Tid, ArrayList<Integer> srcTids,
			HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid) {
		// 현재 Snapshot 추출
		MultihopSnapshot curSnapshot = trace.getClosestSnapshot(realModeTime);
		int maxNodesToSelect = (int) (trace.getClosestSnapshot(0)
				.getNodeCount() * Params.MAX_CLUSTER_UTILIZATION);
		int curNumNodes = curSnapshot.getNodeCount();
		if (curNumNodes < maxNodesToSelect) {
			maxNodesToSelect = curNumNodes;
		}

		// CommType 에 따라 다른 방식으로 Src-Dst 결합
		if (tpp.commType == CommType.REPLICATE) {
			// JobWeightType 에 따라 다른 Dst Nid 목록 선택
			ArrayList<Integer> dstNids;
			if (jp.weightType == JobWeightType.MAP_INPUT_HEAVY) {
				// Data Local Dst Node ID 결정
				dstNids = curSnapshot.getDataLocalNids(srcNids,
						maxNodesToSelect);

			} else if (jp.weightType == JobWeightType.RED_INPUT_HEAVY) {
				// K-Club
				dstNids = curSnapshot.getKClubNids(maxNodesToSelect);

			} else if (jp.weightType == JobWeightType.MR_INPUT_HEAVY) {
				// Data Local Dst Node ID 결정
				dstNids = curSnapshot.getDataLocalNids(srcNids,
						maxNodesToSelect);

			} else {
				throw new IllegalStateException(jp.weightType + "");
			}

			// Src:Dst = Utilization 에 따라 1:1 or M:1 or 1:M 모두 가능
			return new GeneralPair<>(createGreedyClosestPaths(curSnapshot,
					srcNids, srcNid2Tid, dstNids), 1.0);

		} else if (tpp.commType == CommType.SHUFFLE) {
			// Src:Dst = N:M 연결
			// Map 과 같은 Node 에 Task 할당
			return new GeneralPair<>(createShufflePaths(srcTids, srcNids), 1.0);

		} else {
			throw new IllegalStateException(tpp.commType + "");
		}
	}

}
