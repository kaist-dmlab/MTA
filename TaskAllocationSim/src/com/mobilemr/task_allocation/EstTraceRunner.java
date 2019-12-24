package com.mobilemr.task_allocation;

import java.util.ArrayList;
import java.util.HashMap;

import com.mobilemr.task_allocation.heuristic.Heuristic;
import com.mobilemr.task_allocation.heuristic.HeuristicMtaS;
import com.mobilemr.task_allocation.platform.EstJobTracker;
import com.mobilemr.task_allocation.platform.JobResult;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;
import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;
import drcl.inet.protocol.aodv.struct.RoutableNode;

public class EstTraceRunner {

	private Heuristic h;
	private MultihopTrace trace;
	private int realModeTime;
	private int estModeTime;

	private TaskPhaseProfile curTpp;
	private TaskPhaseProfile nextTpp;
	private HashMap<Integer, Task> srcTid2Task;
	private ArrayList<OrderedNumPair<Integer>> conns;

	public EstTraceRunner(Heuristic h, MultihopTrace trace, int realModeTime, TaskPhaseProfile curTpp,
			TaskPhaseProfile nextTpp, HashMap<Integer, Task> srcTid2Task, ArrayList<OrderedNumPair<Integer>> conns) {
		this.h = h;
		this.trace = trace;
		this.realModeTime = realModeTime;
		estModeTime = realModeTime;

		this.curTpp = curTpp;
		this.nextTpp = nextTpp;
		this.srcTid2Task = srcTid2Task;
		this.conns = conns;
	}

	public JobResult start() {
		Double mapReliability = null;
		Double redReliability = null;
		Double distContention = null;
		Double mapContention = null;
		HashMap<Integer, Float> dstNid2CommDuration = new HashMap<>();

		// JobTracker 생성
		EstJobTracker jt = new EstJobTracker(curTpp, nextTpp, srcTid2Task, conns, realModeTime);

		// 초기 RoutableSnapshot 생성
		RoutableMultihopSnapshot curRoutableSnapshot = trace.getClosestRoutableMultihopSnapshot(estModeTime);

		// TaskPhase 가 끝날때까지 시간 증가
		while (!jt.isCurTaskPhaseFinished(curRoutableSnapshot)) {
			// 실제 또는 추정시간으로 NewRoutableMultihopSnapshot Topology 정보 갱신
			MultihopSnapshot curSnapshot = h instanceof HeuristicMtaS ? trace.getClosestSnapshot(estModeTime)
					: trace.getClosestSnapshot(realModeTime);
			ArrayList<Integer> departedNids = curRoutableSnapshot.updateTopology(curSnapshot);

			// JobTracker 에 Topology 갱신정보 반영
			jt.updateTaskDepartures(departedNids);

			// 현재 TaskPhase 실패검사
			if (jt.isCurTaskPhaseFailed(false)) {
				// 실패했을 경우 실패 결과 반환
				return JobResult.failed(jt.getCurTaskPhaseType(), mapReliability, redReliability, distContention,
						mapContention, estModeTime);
			}

			// 현재 Job 의 TaskPhase 에 대해 단위시간만큼 작업 + 전송 수행
			jt.updateByUnitTime(curRoutableSnapshot, estModeTime);

			// 추정시간 갱신
			estModeTime++;
		}

		// Dst Node 통신시간 기록
		ArrayList<Integer> dstNids = Heuristic.getDstNids(conns);
		for (int dstNid : dstNids) {
			RoutableNode dstNode = curRoutableSnapshot.nid2Node.get(dstNid);
			if (dstNode != null) {
				dstNid2CommDuration.put(dstNid, (float) dstNode.getCommDuration(curTpp.tpType));
			}
		}

		// 결과 반환
		double traffic = jt.getTraffic();
		return JobResult.succeeded(mapReliability, redReliability, distContention, mapContention, traffic, estModeTime,
				null, null, null, null, dstNid2CommDuration, jt.getReplicateDuration(), jt.getShuffleDuration());
	}

}
