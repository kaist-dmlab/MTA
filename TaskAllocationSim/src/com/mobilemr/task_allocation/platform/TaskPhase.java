package com.mobilemr.task_allocation.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.mobilemr.task_allocation.platform.Task.Status;
import com.mobilemr.task_allocation.profile.CommType;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;

public class TaskPhase {

	// XXX Load Balancing & Heterogeneity
	// Load Balancing 을 위해서는 dstTaskInputSize 분포를 다르게 줘야하고 (상수),
	// Heterogeneity 를 위해서는 TaskPhaseProfile.B_P 가 아니라, Node 마다 다른 속도를 줘야 함 (변수)

	public static final boolean DEBUG = false;

	public TaskPhaseProfile tpp;
	public HashMap<Integer, Task> srcTid2Task = new HashMap<>();
	public HashMap<Integer, Task> dstTid2Task = new HashMap<>();
	public HashMap<Integer, Task> failedTid2Task = new HashMap<>();

	public TaskPhase(TaskPhaseProfile tpp, TaskPhaseProfile nextTpp,
			HashMap<Integer, Task> srcTid2Task,
			ArrayList<OrderedNumPair<Integer>> conns, String indent) {
		this.tpp = tpp;
		this.srcTid2Task = srcTid2Task;

		// Src Task 가 없거나
		// 마지막 TaskPhase 가 아닌데도 Dst Task 가 없는 경우
		// TaskPhase 생성을 중지함
		if (srcTid2Task.isEmpty() || (!isLastTaskPhase() && conns.isEmpty())) {
			return;
		}

		// Src Task 늦은 초기화
		ArrayList<Task> srcTasks = new ArrayList<>(srcTid2Task.values());
		srcTasks.forEach(srcTask -> srcTask.lateBind(this));

		// Dst Nid 목록 추출
		// 중복제거를 위해 HashSet 생성
		// -> Reproducibility 를 위해 ArrayList 변환
		HashSet<Integer> dstNidSet = new HashSet<>();
		conns.forEach(conn -> dstNidSet.add(conn.getSecond()));
		ArrayList<Integer> dstNids = new ArrayList<>(dstNidSet);

		// Dst Task 생성 및 보관
		// Dst Nid -> Dst Tid -> Dst Task 접근이 가능한 자료구조 생성
		HashMap<Integer, Integer> dstNid2Tid = new HashMap<>();
		double dstTaskInputSize = tpp.outputSize / dstNids.size();
		for (int dstNid : dstNids) {
			int dstTid = Task.generateId();

			// Dst Node ID -> Task ID 저장
			dstNid2Tid.put(dstNid, dstTid);

			// Dst Task ID -> Task 저장
			dstTid2Task.put(dstTid, new Task(dstTid, dstNid, nextTpp.getBandwidth(),
					dstTaskInputSize, false));
		}

		for (OrderedNumPair<Integer> conn : conns) {
			// Src Task 추출
			int srcTid = conn.getFirst();
			Task srcTask = srcTid2Task.get(srcTid);

			// Dst Task 추출
			int dstNid = conn.getSecond();
			int dstTid = dstNid2Tid.get(dstNid);
			Task dstTask = dstTid2Task.get(dstTid);

			// Src Task 에 Dst Task 추가
			srcTask.addDstTask(dstTask);
		}

		if (DEBUG) {
			ArrayList<Integer> srcNids = new ArrayList<>();
			srcTid2Task.values().forEach(srcTask -> srcNids.add(srcTask.nid));
			Logger.println(indent + "TaskPhase - srcNids : " + srcNids);
			Logger.println(indent + "TaskPhase - dstNids : " + dstNids);
		}
	}

	public boolean isProcessingFinished() {
		// 하나라도 Task 처리가 완료되지 않았다면 Phase 미완료
		for (Task srcTask : srcTid2Task.values()) {
			if (!srcTask.isProcessingFinished()) {
				return false;
			}
		}

		// 모든 조건이 완료됐다면 TaskPhase 완료
		return true;
	}

	public boolean isFinished(RoutableMultihopSnapshot curRoutableSnapshot) {
		// Task 처리가 완료되지 않았다면 미완료
		if (!isProcessingFinished()) {
			return false;
		}

		// 현재 TaskPhase 에서 전송할 Snapshot Size 가 남아있다면 미완료
		if (curRoutableSnapshot.anyPendingPackets()) {
			return false;
		}

		// 모든 조건이 완료됐다면 TaskPhase 완료
		return true;
	}

	public void start() {
		for (Task srcTask : srcTid2Task.values()) {
			srcTask.status = Status.STARTED;
		}
	}

	public boolean isLastTaskPhase() {
		// 마지막 TaskPhase 는 통신이 필요없음
		return tpp.commType == CommType.NONE;
	}

	public boolean isNonProcessingTaskPhase() {
		// 입력분배는 처리가 필요없는 TaskPhase
		return tpp.tpType == TaskPhaseType.DIST;
	}

	public double getPendingInputSize() {
		double sumDataSize = 0;
		for (Task t : srcTid2Task.values()) {
			sumDataSize += t.pendingInputSize;
		}
		for (Task t : dstTid2Task.values()) {
			sumDataSize += t.pendingInputSize;
		}
		return sumDataSize;
	}

	public double getOutputBufferSize() {
		// 모든 Task 처리량을 반환
		double sumDataSize = 0;
		for (Task t : srcTid2Task.values()) {
			sumDataSize += t.outputBuffer;
		}
		for (Task t : dstTid2Task.values()) {
			sumDataSize += t.outputBuffer;
		}
		return sumDataSize;
	}

	@Override
	public String toString() {
		return getPendingInputSize() + getOutputBufferSize() + "";
	}

}
