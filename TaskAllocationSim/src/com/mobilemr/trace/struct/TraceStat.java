package com.mobilemr.trace.struct;

import java.io.Serializable;
import java.lang.reflect.Field;

public class TraceStat extends CsvRecord implements Serializable {
	private static final long serialVersionUID = -2343574097851216500L;

	public int localTraceIdx;
	public int initTimestamp;
	public int lastTimestamp;

	public int length;
	public int initGs;
	public float avgGs;
	public float avgNd;
	public float mttNodeDepartures; // 사용할 수 있을까 했지만, 결국 사용 안함
	public int numNodeArrivals;
	public int numNodeDepartures;
	public int numEdgeArrivals;
	public int numEdgeDepartures;
	public float normGed;
	public float normGcd;

	private TraceStat() {
	}

	public TraceStat(Trace trace) {
		localTraceIdx = trace.id;
		initTimestamp = trace.getInitCC().getTimestamp();
		lastTimestamp = trace.getLastCC().getTimestamp();

		// 통계치 계산
		SimpleTraceStat simpleTraceStat = new SimpleTraceStat(trace.ccs);
		length = simpleTraceStat.length;
		initGs = simpleTraceStat.initGs;
		avgGs = simpleTraceStat.avgGs;
		avgNd = simpleTraceStat.avgNd;

		// 5의 배수로 표현
		// avgGs = Math.round(simpleTraceStat.avgGs / 5) * 5F;
		mttNodeDepartures = simpleTraceStat.mttNodeDepartures;

		numNodeArrivals = simpleTraceStat.numNodeArrivals;
		numNodeDepartures = simpleTraceStat.numNodeDepartures;
		numEdgeArrivals = simpleTraceStat.numEdgeArrivals;
		numEdgeDepartures = simpleTraceStat.numEdgeDepartures;

		normGed = simpleTraceStat.sumGed / simpleTraceStat.avgGs;
		normGcd = simpleTraceStat.sumGcd / simpleTraceStat.avgGs;

		// // 0.5의 배수로 표현
		// normGed = Math
		// .round(simpleTraceStat.sumGed / simpleTraceStat.avgGs * 2) / 2F;
		// normGcd = Math
		// .round(simpleTraceStat.sumGcd / simpleTraceStat.avgGs * 2) / 2F;
	}

	@Override
	public boolean equals(Object obj) {
		TraceStat that = (TraceStat) obj;

		// 중복 Trace 제거를 위해 localTraceIdx 와 timestamp 를 제외한 값만 비교
		Field[] thisFields = this.getClass().getFields();
		for (Field thisField : thisFields) {
			String fieldName = thisField.getName();
			if (fieldName.equals("localTraceIdx")
					|| fieldName.equals("initTimestamp")
					|| fieldName.equals("lastTimestamp")) {
				continue;
			}
			try {
				Field thatField = that.getClass().getField(fieldName);
				Object thisFieldValue = thisField.get(this);
				Object thatFieldValue = thatField.get(this);

				// 하나의 Field 값이라도 다르면 false
				if (!thisFieldValue.equals(thatFieldValue)) {
					return false;
				}
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		// 모든 Field 값이 같았다면 true
		return true;
	}

	public static String getCsvTitle() {
		return "inputTraceId," + new TraceStat().getInternalCsvTitle();
	}

	public String toCsvString(String inputTraceId) {
		return inputTraceId + "," + toCsvString();
	}

}
