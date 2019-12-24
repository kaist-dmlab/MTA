package com.mobilemr.task_allocation.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import com.mobilemr.task_allocation.heuristic.Heuristic;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.history.HistoryStat;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;
import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;

public class JobTracker extends EstJobTracker {

	private static final boolean DEBUG = false;

	private MultihopTrace trace;
	private JobProfile jp;
	private Class<? extends Heuristic> clsHeuristic;
	private HistoryStat historyStat;
	private HashMap<Integer, Integer> zb2OrgNid;

	private LinkedList<TaskPhaseProfile> tppQ;
	private HashMap<Integer, Task> initTid2Task;

	public JobTracker(MultihopTrace trace, JobProfile jp,
			Class<? extends Heuristic> clsHeuristic, HistoryStat historyStat,
			HashMap<Integer, Integer> zb2OrgNid) {
		super();

		this.trace = trace;
		this.jp = jp;
		this.clsHeuristic = clsHeuristic;
		this.historyStat = historyStat;
		this.zb2OrgNid = zb2OrgNid;

		tppQ = new LinkedList<>(jp.tpps);

		// 목표 Node 개수만큼 Random Input 분배
		MultihopSnapshot initSnapshot = trace.getClosestSnapshot(0);

		// Heuristic h = Heuristic.of(clsHeuristic);
		// initTid2Task = h.placeData(jp, initSnapshot);

		// Trace 마다 같은 Random Node Set 을 선택하기 위해 Trace 의 ID 를 seed 로 사용
		int numInitNodes = (int) (initSnapshot.getNodeCount());
		double inputSize_Per_Task = jp.inputSize / numInitNodes;
		ArrayList<Integer> nids = initSnapshot.getNids();
		Random traceRandom = new Random(trace.id);
		Collections.shuffle(nids, traceRandom);
		initTid2Task = new HashMap<>();
		for (int i = 0; i < numInitNodes; i++) {
			int nid = nids.get(i);
			int tid = Task.generateId();
			initTid2Task.put(tid, new Task(tid, nid, inputSize_Per_Task));
		}
	}

	public boolean isJobFinished(RoutableMultihopSnapshot curRoutableSnapshot) {
		return tppQ.isEmpty() && isCurTaskPhaseFinished(curRoutableSnapshot);
	}

	public AllocationResult allocateNextTasks(String inputTraceId,
			int realModeTime) {
		if (DEBUG) {
			Logger.println(indent
					+ "JT - allocateNextTasks() | realModeTime : "
					+ realModeTime);
		}
		Heuristic h = Heuristic.of(clsHeuristic);

		// Src Task 결정
		HashMap<Integer, Task> srcTid2Task = null;
		if (curTaskPhase == null) {
			// 처음일 경우 처음 Task 사용
			srcTid2Task = initTid2Task;
		} else {
			// 처음이 아닐 경우 이전 TaskPhase 의 Dst Task 를 사용
			HashMap<Integer, Task> prevDstTid2Task = curTaskPhase.dstTid2Task;
			srcTid2Task = new HashMap<>();
			srcTid2Task.putAll(prevDstTid2Task);
		}

		// 현재 TaskPhaseProfile 추출
		TaskPhaseProfile curTpp = tppQ.remove();
		if (DEBUG) {
			Logger.println(indent + "JT - allocateNextTasks() | 남은 tppQ 크기 : "
					+ tppQ.size() + " | curTpp : " + curTpp);
		}

		// Task 할당 수행
		TaskPhaseProfile nextTpp = null;
		AllocationResult allocationResult;
		if (!tppQ.isEmpty()) {
			// 마지막 TaskPhase 가 아닌 경우, Task 할당
			nextTpp = tppQ.peek();
			allocationResult = h.allocateTasks(inputTraceId, jp, curTpp,
					nextTpp, trace, realModeTime, srcTid2Task, historyStat,
					zb2OrgNid);
		} else {
			// 마지막 TaskPhase 인 경우,
			// Dst 이 없는 경우이므로 Task 할당을 수행하지 않음
			allocationResult = new AllocationResult();
		}

		// 실제 Mode 라면, 실제 Task 할당를 수행 후 Node 정보 출력
		ArrayList<Integer> dstNids = Heuristic
				.getDstNids(allocationResult.conns);
		if (DEBUG) {
			Logger.println(indent + "JT - allocateNextTasks() | 실제 Task 할당완료 ("
					+ curTpp.commType + ")");

			int initTimestamp = trace.getInitCC().getTimestamp();
			int realModeTimestampOfInputFile = initTimestamp + realModeTime;
			Logger.println(indent + "JT - allocateNextTasks() | historyStat");
			for (int zbNid : zb2OrgNid.keySet()) {
				int orgNid = zb2OrgNid.get(zbNid);
				Logger.println(indent
						+ "JT - allocateNextTasks() | orgNid : "
						+ orgNid
						+ " / "
						+ "zbNid : "
						+ zbNid
						+ " / "
						+ historyStat.getFailureRate(orgNid,
								realModeTimestampOfInputFile));
			}

			ArrayList<Integer> srcNids = getSrcNids(srcTid2Task);
			Logger.println(indent + "JT - allocateNextTasks() | srcNids : "
					+ srcNids);
			Logger.println(indent + "JT - allocateNextTasks() | dstNids : "
					+ dstNids);
		}

		// TaskPhase 생성자에서
		// 최적 Dst Node ID 목록을 기반으로 Task 할당를 수행
		curTaskPhase = new TaskPhase(curTpp, nextTpp, srcTid2Task,
				allocationResult.conns, indent);

		// 할당가 끝났으므로 모든 Task 를 시작시킴
		curTaskPhase.start();

		// Task 할당 결과 반환
		return allocationResult;
	}

	private static ArrayList<Integer> getSrcNids(
			HashMap<Integer, Task> srcTid2Task) {
		HashSet<Integer> srcNidSet = new HashSet<>();
		for (Task srcTask : srcTid2Task.values()) {
			srcNidSet.add(srcTask.nid);
		}
		return new ArrayList<>(srcNidSet);
	}

}
