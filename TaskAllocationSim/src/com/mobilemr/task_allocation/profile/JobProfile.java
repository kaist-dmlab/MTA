package com.mobilemr.task_allocation.profile;

import java.util.LinkedList;

import com.mobilemr.task_allocation.platform.TaskPhaseType;

public class JobProfile {

	public String name;
	public float inputSize;
	public JobWeightType weightType;

	private JobProfile(String name, float inputSize) {
		this.name = name;
		this.inputSize = inputSize;
		if (name.equals("NAIVE_BAYES")) {
			weightType = JobWeightType.MAP_INPUT_HEAVY;
		} else if (name.equals("SIFT")) {
			weightType = JobWeightType.RED_INPUT_HEAVY;
		} else if (name.equals("JOIN")) {
			weightType = JobWeightType.MR_INPUT_HEAVY;
		} else {
			throw new IllegalArgumentException(
					"Possible job profile name: NAIVE_BAYES, SIFT, or JOIN");
		}
		addDist();
	}

	public static JobProfile of(String name, float inputSize) {
		return new JobProfile(name, inputSize);
	}

	public LinkedList<TaskPhaseProfile> tpps = new LinkedList<TaskPhaseProfile>();

	public void addDist() {
		// 처음 Block 분배 TaskPhase 와 같이, 처리 과정이 없고 전송 과정만 있는 경우
		float srcInputSize;
		if (tpps.isEmpty()) {
			srcInputSize = inputSize;
		} else {
			TaskPhaseProfile lastTpp = tpps.get(tpps.size() - 1);
			srcInputSize = lastTpp.outputSize;
		}
		tpps.add(new TaskPhaseProfile(tpps.size(), TaskPhaseType.DIST, 1, null,
				CommType.REPLICATE, srcInputSize));
	}

	public void addMap(float S_Src, float... B_Ps) {
		// Mapper TaskPhase 와 같이, 처리 과정과 전송 과정 모두 있는 경우
		float srcInputSize;
		if (tpps.isEmpty()) {
			srcInputSize = inputSize;
		} else {
			TaskPhaseProfile lastTpp = tpps.get(tpps.size() - 1);
			srcInputSize = lastTpp.outputSize;
		}
		tpps.add(new TaskPhaseProfile(tpps.size(), TaskPhaseType.MAP, S_Src,
				B_Ps, CommType.SHUFFLE, srcInputSize));
	}

	public void addReduce(float S_Src, float... B_Ps) {
		// Reducer TaskPhase 와 같이, 마지막이라서, 처리 과정만 있고 전송 과정이 없는 경우
		float srcInputSize;
		if (tpps.isEmpty()) {
			srcInputSize = inputSize;
		} else {
			TaskPhaseProfile lastTpp = tpps.get(tpps.size() - 1);
			srcInputSize = lastTpp.outputSize;
		}
		tpps.add(new TaskPhaseProfile(tpps.size(), TaskPhaseType.REDUCE, S_Src,
				B_Ps, CommType.NONE, srcInputSize));
	}

}
