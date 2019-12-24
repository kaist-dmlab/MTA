package com.mobilemr.trace.history;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class HistoryStat implements Serializable {
	private static final long serialVersionUID = 3393966301707668851L;

	private int traceInitTimestamp;
	private int traceLastTimestamp;
	private int timeInterval;

	public HistoryStat(int traceInitTimestamp, int traceLastTimestamp,
			int timeInterval) {
		this.traceInitTimestamp = traceInitTimestamp;
		this.traceLastTimestamp = traceLastTimestamp;
		this.timeInterval = timeInterval;
	}

	private HashMap<Integer, float[]> nid2FailureRates = new HashMap<Integer, float[]>();

	public void addNid2FailureRates(int nid, float[] failureRates) {
		nid2FailureRates.put(nid, failureRates);
	}

	public float getMttf(int nid, int targetTimestamp) {
		return 1 / getFailureRate(nid, targetTimestamp);
	}

	public float getFailureRate(int nid, int targetTimestamp) {
		// 시간 Bound 검사
		if (targetTimestamp < traceInitTimestamp
				|| traceLastTimestamp < targetTimestamp) {
			// 아예 Trace 의 시간범위 밖일 경우 NaN 반환
			return Float.NaN;
		} else if (traceInitTimestamp <= targetTimestamp
				&& targetTimestamp < traceInitTimestamp + timeInterval) {
			// 최소 Failure Rate 계산을 timeInterval 크기부터 시작하므로
			// 그 사이에서는 첫번째 Failure Rate 반환
			return nid2FailureRates.get(nid)[0];
		} else {
			// 이전 timeInterval 시간 범위의 failureRate 에 관심이 있으므로 -1
			int idxTimestamp = (targetTimestamp - traceInitTimestamp)
					/ timeInterval - 1;
			return nid2FailureRates.get(nid)[idxTimestamp];
		}
	}

	public float getAvgFailureRate(int nid) {
		float[] failureRates = nid2FailureRates.get(nid);
		float sum = 0;
		for (float failureRate : failureRates) {
			sum += failureRate;
		}
		return sum / failureRates.length;
	}

	public void interpolate() {
		for (float[] failureRates : nid2FailureRates.values()) {
			int idxNanStart = -1;
			boolean interpolating = false;
			for (int i = 0; i < failureRates.length; i++) {
				if (Float.isNaN(failureRates[i])) {
					// NaN 일 경우
					// NaN 의 처음 시작 인덱스 기록해두기
					if (!interpolating) {
						idxNanStart = i;
						interpolating = true; // interpolating 시작 표시
					}
				} else {
					// NaN 이 아닐 경우
					// interpolating 도중이라면 마무리
					if (interpolating) {
						// 앞서 모든 j 번째 요소를 i 번째 요소로 치환
						float curFailureRate = failureRates[i];
						for (int j = idxNanStart; j < i; j++) {
							failureRates[j] = curFailureRate;
						}

						// interpolating 종료 표시
						interpolating = false;
						idxNanStart = -1;
					}
				}
			}

			// 마지막에 NaN 채워지지 않은 곳이 있는지 검사
			if (idxNanStart != -1) {
				float lastFailureRate = failureRates[idxNanStart - 1];
				// 모든 나머지 요소를 이전 마지막 요소로 치환
				for (int j = idxNanStart; j < failureRates.length; j++) {
					failureRates[j] = lastFailureRate;
				}
			}
		}
	}

	public void scaleBy(Float duration) {
		// Upper Outlier Bound 구하기 (시간이므로 Lower 필요 없음)
		DescriptiveStatistics dsMttf = new DescriptiveStatistics();
		for (float[] failureRates : nid2FailureRates.values()) {
			for (float failureRate : failureRates) {
				float mttf = 1 / failureRate;
				dsMttf.addValue(mttf);
			}
		}
		double q1 = dsMttf.getPercentile(25);
		double q3 = dsMttf.getPercentile(75);
		double IQR = q3 - q1;
		double upperWhisker = q1 + 1.5 * IQR;

		// Upper Outlier Bound 내부분포 확인
		dsMttf.clear();
		for (float[] failureRates : nid2FailureRates.values()) {
			for (float failureRate : failureRates) {
				float mttf = 1 / failureRate;
				if (mttf <= upperWhisker) {
					dsMttf.addValue(mttf);
				}
			}
		}

		// 현재 MTTF 값이 Duration 정도가 되도록 Scale Factor 설정
		double scaleFactor = duration / dsMttf.getMax();

		// Normalize by MTTF & Scale by Duration
		for (float[] failureRates : nid2FailureRates.values()) {
			for (int i = 0; i < failureRates.length; i++) {
				float mttf = 1 / failureRates[i];
				failureRates[i] = (float) (1 / (mttf * scaleFactor));
			}
		}
	}

	public void printInfo() {
		// Upper Outlier Bound 구하기 (시간이므로 Lower 필요 없음)
		DescriptiveStatistics dsMttf = new DescriptiveStatistics();
		for (float[] failureRates : nid2FailureRates.values()) {
			for (float failureRate : failureRates) {
				float mttf = 1 / failureRate;
				dsMttf.addValue(mttf);
			}
		}
		double q1 = dsMttf.getPercentile(25);
		double q3 = dsMttf.getPercentile(75);
		double IQR = q3 - q1;
		double upperWhisker = q1 + 1.5 * IQR;

		// Upper Outlier Bound 내부분포 확인
		dsMttf.clear();
		for (float[] failureRates : nid2FailureRates.values()) {
			for (float failureRate : failureRates) {
				float mttf = 1 / failureRate;
				if (mttf <= upperWhisker) {
					dsMttf.addValue(mttf);
				}
			}
		}
		System.out.println("    Mttf 개요");
		System.out.println("      Min   : " + dsMttf.getMin());
		System.out.println("      Mean  : " + dsMttf.getMean());
		System.out.println("      Max   : " + dsMttf.getMax());
		System.out.println("      Upper : " + upperWhisker);
	}

	public void print(OutputStream os) {
		PrintWriter pw = new PrintWriter(os);
		for (Entry<Integer, float[]> entry : nid2FailureRates.entrySet()) {
			pw.println(entry.getKey());
			for (float failureRate : entry.getValue()) {
				pw.print((failureRate == Float.NaN ? "" : failureRate) + ", ");
			}
			pw.println();
		}
		pw.close();
	}

}
