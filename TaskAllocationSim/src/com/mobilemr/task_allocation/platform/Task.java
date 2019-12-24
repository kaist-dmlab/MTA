package com.mobilemr.task_allocation.platform;

import java.util.ArrayList;

import drcl.inet.protocol.aodv.struct.RoutableMultihopSnapshot;

public class Task implements Comparable<Task>, Cloneable {

	private static int cntTid = 0;

	public static void resetId() {
		cntTid = 0;
	}

	public static int generateId() {
		return cntTid++;
	}

	public int id;
	public int nid;
	public Float B_P;

	public double pendingInputSize;
	public double targetInputSize;

	public Task(int id, int nid, double inputSize) {
		// 처음 Block 전송 Task 생성 시
		this.id = id;
		this.nid = nid;
		this.B_P = null;

		pendingInputSize = inputSize;
		targetInputSize = inputSize;
	}

	public Task(int id, int nid, Float B_P, double targetInputSize,
			boolean dummy) {
		// 이후 Mapper 또는 Reducer Task 생성 시
		this.id = id;
		this.nid = nid;
		this.B_P = B_P;

		// Pending Input 은 onReceive() 에서 나중에 전송받으므로 우선은 없음
		pendingInputSize = 0;
		this.targetInputSize = targetInputSize;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// public Task cloneUnbound() {
	// return new Task(this.id, this.nid, this.pendingInputSize);
	// }

	public TaskPhase tp;
	public ArrayList<Task> dstTasks;
	public double outputBuffer = 0;

	public void lateBind(TaskPhase tp) {
		this.tp = tp;
		dstTasks = new ArrayList<Task>();
	}

	public void addDstTask(Task dstTask) {
		dstTasks.add(dstTask);
	}

	public enum Status {
		WAITING, STARTED, PROC_FINISHED
	}

	public Status status = Status.WAITING;

	public boolean isProcessingFinished() {
		return status.compareTo(Status.PROC_FINISHED) >= 0;
	}

	public void processByUnitTime(RoutableMultihopSnapshot curRoutableSnapshot) {
		// 처리할 입력이 있는 지 검사
		if (pendingInputSize == 0) {
			// 입력이 없는 경우 처리하지 않음

		} else if (pendingInputSize > 0) {
			// 입력이 남아있는 경우
			// 처리할 필요가 있는 TaskPhase 인지 검사
			if (tp.isNonProcessingTaskPhase()) {
				// 처리할 필요가 없는 TaskPhase 인 경우
				// 처리를 하지 않고, Input 을 Snapshot Output Buffer 로 옮기는 작업 수행
				double pendingPartitionOutputSize = pendingInputSize
						/ dstTasks.size();
				for (Task dstTask : dstTasks) {
					curRoutableSnapshot.queuePackets(nid, dstTask.nid, id,
							dstTask.id, pendingPartitionOutputSize);
				}

				// Pending Input 처리완료 표시
				pendingInputSize = 0;

			} else {
				// 처리할 필요가 있는 TaskPhase 인 경우

				// Pending Input 과 Processing 속도 중 더 작은 값을 처리 크기로 결정
				double processedSize = pendingInputSize < B_P ? pendingInputSize
						: B_P;

				// Pending Input 데이터 크기 줄이기
				pendingInputSize -= processedSize;

				// 마지막 TaskPhase 인지 검사
				if (tp.isLastTaskPhase()) {
					// 마지막 TaskPhase 인 경우
					// Dummy Buffer 하나에만 저장
					double pendingPartitionOutputSize = processedSize
							* tp.tpp.S_Src;
					outputBuffer += pendingPartitionOutputSize;

				} else {
					// 마지막 TaskPhase 가 아닌 경우

					// 처리 후, Input 을 Snapshot Output Buffer 로 옮기는 작업 수행
					double pendingPartitionOutputSize = processedSize
							* tp.tpp.S_Src / dstTasks.size();
					for (Task dstTask : dstTasks) {
						curRoutableSnapshot.queuePackets(nid, dstTask.nid, id,
								dstTask.id, pendingPartitionOutputSize);
					}
				}
			}

		} else {
			// 입력이 음수인 경우는 있을 수 없음
			throw new RuntimeException(pendingInputSize + "");
		}
	}

	public void onReceive(RoutableMultihopSnapshot curRoutableSnapshot,
			TaskPhaseType tpType, int curTime) {
		// Loopback 데이터 추출
		double loopbackDataSize = curRoutableSnapshot.getLoopbackData(nid, id);
		if (loopbackDataSize > 0) {
			// 수신 데이터 크기 누적
			pendingInputSize += loopbackDataSize;
		}

		// 통신 데이터 추출
		double completedDataSize = curRoutableSnapshot
				.getCompletedData(nid, id);
		if (completedDataSize > 0) {
			// 통신 시작시간 기록
			curRoutableSnapshot.nid2Node.get(nid).markCommStarted(tpType,
					curTime);

			// 수신 데이터 크기 누적
			pendingInputSize += completedDataSize;

			// 통신 종료시간 기록
			curRoutableSnapshot.nid2Node.get(nid).markCommFinished(tpType,
					curTime + 1);
		}
	}

	@Override
	public int compareTo(Task that) {
		if (this.id < that.id) {
			return -1;
		} else if (this.id > that.id) {
			return 1;
		}
		if (this.nid < that.nid) {
			return -1;
		} else if (this.nid > that.nid) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		String ret = "";
		ret += "id: " + id + " _ ";
		ret += "nid: " + nid + " _ ";
		ret += "status: " + status + " _ ";
		ret += "pendingInputSize: " + pendingInputSize + " _ ";
		ret += "outputDataBuffer: " + outputBuffer;
		return ret;
	}

}
