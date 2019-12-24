package com.mobilemr.task_allocation.heuristic;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.CommType;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;

public class HeuristicHadoop extends Heuristic {

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
			// Data Local Dst Node ID 결정
			ArrayList<Integer> dstNids = curSnapshot.getDataLocalNids(srcNids,
					maxNodesToSelect);

			// Src:Dst = Utilization 에 따라 1:1 or M:1 or 1:M 모두 가능
			return new GeneralPair<>(createGreedyClosestPaths(curSnapshot,
					srcNids, srcNid2Tid, dstNids), 1.0);

		} else if (tpp.commType == CommType.SHUFFLE) {
			// Random Dst Nid 목록 생성
			ArrayList<Integer> dstNids = curSnapshot
					.getRandomNids(maxNodesToSelect);

			// Src:Dst = N:M 연결
			return new GeneralPair<>(createShufflePaths(srcTids, dstNids), 1.0);

		} else {
			throw new IllegalStateException(tpp.commType + "");
		}
	}

}
