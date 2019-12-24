package com.mobilemr.task_allocation.profile;

import java.util.Arrays;

import com.mobilemr.task_allocation.platform.TaskPhaseType;
import com.mobilemr.task_allocation.util.Common;

public class TaskPhaseProfile {

	public int index;
	public TaskPhaseType tpType;
	public float S_Src;
	private float[] B_Ps;
	public CommType commType;
	public float inputSize;
	public float outputSize;

	public TaskPhaseProfile(int index, TaskPhaseType tpType, float S_Src,
			float[] B_Ps, CommType commType, float inputSize) {
		this.index = index;
		this.tpType = tpType;
		this.S_Src = S_Src;
		this.B_Ps = B_Ps;
		this.commType = commType;
		this.inputSize = inputSize;
		outputSize = inputSize * S_Src;
	}

	public Float getBandwidth() {
		if (B_Ps.length == 1) {
			// 정상 Bandwidth 인 경우
			return B_Ps[0];
		} else {
			// Heterogeneous Bandwidth 인 경우, 무작위 선택
			return B_Ps[Common.R.nextInt(B_Ps.length)];
		}
	}

	@Override
	public String toString() {
		return "tpType : " + tpType + " _ S_Src : " + S_Src + " _ B_Ps : "
				+ Arrays.toString(B_Ps) + " _ commType : " + commType;
	}

}
