package com.mobilemr.task_allocation.platform;

import java.util.HashMap;

public class JobResult {

	private JobFinishReason finishReason;
	private Double mapReliability;
	private Double redReliability;
	private Double mapContention;
	private Double redContention;
	private Double traffic;
	private Integer completionTime;
	private Double optimalIdx1;
	private Double optimalIdx2;
	private Double avgClusterUtilization;
	private Double sumEvals;
	private HashMap<Integer, Float> dstNid2CommDuration;
	private Integer replicateDuration;
	private Integer shuffleDuration;

	private JobResult(JobFinishReason jobFinishReason, Double mapReliability,
			Double redReliability, Double mapContention, Double redContention,
			Double traffic, Integer completionTime, Double optimalIdx1,
			Double optimalIdx2, Double avgClusterUtilization, Double sumEvals,
			HashMap<Integer, Float> dstNid2CommDuration,
			Integer replicateDuration, Integer shuffleDuration) {
		this.finishReason = jobFinishReason;
		this.mapReliability = mapReliability;
		this.redReliability = redReliability;
		this.mapContention = mapContention;
		this.redContention = redContention;
		this.traffic = traffic;
		this.completionTime = completionTime;
		this.optimalIdx1 = optimalIdx1;
		this.optimalIdx2 = optimalIdx2;
		this.avgClusterUtilization = avgClusterUtilization;
		this.sumEvals = sumEvals;
		this.dstNid2CommDuration = dstNid2CommDuration;
		this.replicateDuration = replicateDuration;
		this.shuffleDuration = shuffleDuration;
	}

	public static JobResult unavailable() {
		return new JobResult(JobFinishReason.UNAVAILABLE, null, null, null,
				null, null, null, null, null, null, null, null, null, null);
	}

	public static JobResult failed(TaskPhaseType tpType, Double mapReliability,
			Double redReliability, Double mapContention, Double redContention,
			Integer failureTime) {
		if (tpType == TaskPhaseType.DIST) {
			return new JobResult(JobFinishReason.DIST_FAILURE, mapReliability,
					redReliability, mapContention, redContention, null,
					failureTime, null, null, null, null, null, null, null);
		} else if (tpType == TaskPhaseType.MAP) {
			return new JobResult(JobFinishReason.MAP_FAILURE, mapReliability,
					redReliability, mapContention, redContention, null,
					failureTime, null, null, null, null, null, null, null);
		} else if (tpType == TaskPhaseType.REDUCE) {
			return new JobResult(JobFinishReason.REDUCE_FAILURE,
					mapReliability, redReliability, mapContention,
					redContention, null, failureTime, null, null, null, null,
					null, null, null);
		} else {
			throw new IllegalStateException(tpType + "");
		}
	}

	public static JobResult succeeded(Double mapReliability,
			Double redReliability, Double mapContention, Double redContention,
			Double traffic, Integer successTime, Double optimalIdx1,
			Double optimalIdx2, Double avgClusterUtilization, Double sumEvals,
			HashMap<Integer, Float> dstNid2CommDuration,
			Integer replicateDuration, Integer shuffleDuration) {
		return new JobResult(JobFinishReason.SUCCESS, mapReliability,
				redReliability, mapContention, redContention, traffic,
				successTime, optimalIdx1, optimalIdx2, avgClusterUtilization,
				sumEvals, dstNid2CommDuration, replicateDuration,
				shuffleDuration);
	}

	private Double stdSuccessTime = null;

	public String getStdSuccessTimeString() {
		return succeeded() ? toNonNullString(stdSuccessTime) : "";
	}

	public void setStdSuccessTime(double varSuccessTime) {
		this.stdSuccessTime = varSuccessTime;
	}

	public JobFinishReason getJobFinishReason() {
		return finishReason;
	}

	public String getMapReliabilityString() {
		return succeeded() ? toNonNullString(mapReliability) : "";
	}

	public String getRedReliabilityString() {
		return succeeded() ? toNonNullString(redReliability) : "";
	}

	public double getMapContention() {
		if (!succeeded()) {
			throw new UnsupportedOperationException();
		}
		return mapContention;
	}

	public double getRedContention() {
		if (!succeeded()) {
			throw new UnsupportedOperationException();
		}
		return redContention;
	}

	public double getTraffic() {
		if (!succeeded()) {
			throw new UnsupportedOperationException();
		}
		return traffic;
	}

	public String getTrafficString() {
		return succeeded() ? toNonNullString(traffic) : "";
	}

	public Double getFt() {
		// if (!failed()) {
		// throw new UnsupportedOperationException();
		// }
		return failed() ? completionTime.doubleValue() : null;
	}

	public String getFtString() {
		return failed() ? toNonNullString(completionTime) : "";
	}

	public Double getSt() {
		// if (!succeeded()) {
		// throw new UnsupportedOperationException();
		// }
		return succeeded() ? completionTime.doubleValue() : null;
	}

	public String getStString() {
		return succeeded() ? toNonNullString(completionTime) : "";
	}

	public String getOptimalIdx1String() {
		return toNonNullString(optimalIdx1);
	}

	public String getOptimalIdx2String() {
		return toNonNullString(optimalIdx2);
	}

	public String getAvgClusterUtilizationString() {
		return succeeded() ? toNonNullString(avgClusterUtilization) : "";
	}

	public String getSumEvalsString() {
		return succeeded() ? toNonNullString(sumEvals) : "";
	}

	public HashMap<Integer, Float> getDstNid2CommDuration() {
		if (!succeeded()) {
			throw new UnsupportedOperationException();
		}
		return dstNid2CommDuration;
	}

	public String getReplicateDurationString() {
		return succeeded() ? toNonNullString(replicateDuration) : "";
	}

	public String getShuffleDurationString() {
		return succeeded() ? toNonNullString(shuffleDuration) : "";
	}

	public boolean succeeded() {
		return finishReason == JobFinishReason.SUCCESS;
	}

	public boolean failed() {
		return finishReason == JobFinishReason.DIST_FAILURE
				|| finishReason == JobFinishReason.MAP_FAILURE
				|| finishReason == JobFinishReason.REDUCE_FAILURE;
	}

	// private static String toSuccessString(boolean condition, Object obj) {
	// return condition ? obj.toString() : "";
	// }

	private static String toNonNullString(Object obj) {
		return obj == null ? "" : obj + "";
	}

}
