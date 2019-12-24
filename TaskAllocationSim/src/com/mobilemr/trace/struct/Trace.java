package com.mobilemr.trace.struct;

import java.util.ArrayList;

public class Trace {

	public ArrayList<CC> ccs = new ArrayList<CC>();

	public Trace(CC cc) {
		// 외부 CC 재사용을 위해 복제 후 추가
		ccs.add((CC) cc.clone());
	}

	public void add(CC cc) {
		// 중복검사
		CC prevCC = ccs.get(ccs.size() - 1);
		if (!prevCC.graphEquals(cc)) {
			// 외부 CC 재사용을 위해 복제 후 추가
			ccs.add((CC) cc.clone());
		}
	}

	private boolean complete = false;
	public int id;
	private TraceStat stat;

	public void markFinish(int id, int lastTimestamp) {
		complete = true;
		this.id = id;

		// 마지막 Timestamp 와 함께 마지막 CC 를 복제해서 최종 CC 로 사용
		CC lastCC = ccs.get(ccs.size() - 1);
		CC clonedLastCC = (CC) lastCC.clone();
		clonedLastCC.setTimestamp(lastTimestamp);
		ccs.add(clonedLastCC);

		// TraceStat 생성
		stat = new TraceStat(this);
	}

	public TraceStat getStat() {
		if (complete == false) {
			throw new IllegalStateException();
		}
		return stat;
	}

	public CC getInitCC() {
		return ccs.get(0);
	}

	public CC getLastCC() {
		return ccs.get(ccs.size() - 1);
	}

	public int getLength() {
		if (complete == false) {
			throw new IllegalStateException();
		}
		return getLastCC().getTimestamp() - getInitCC().getTimestamp();
	}

	@Override
	public String toString() {
		String ret = "";
		ret += "id: " + id + ", ";
		ret += "[";
		for (int i = 0; i < ccs.size(); i++) {
			ret += "{" + ccs.get(i) + "}";
			if (i < ccs.size() - 1) {
				ret += ", ";
			}
		}
		ret += "]";
		return ret;
	}

}
