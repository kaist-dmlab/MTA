package com.mobilemr.trace.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import edu.uci.ics.jung.graph.util.Pair;

public class SimpleTraceStat {

	public static SimpleTraceStat getTempStatInAdvance(ArrayList<CC> prevCCs,
			int lastTimestamp) {
		// 이전 CC 목록과 목표 길이에 대해 임시 통계치를 생성하기 위해 새로운 CC 목록을 만듬
		ArrayList<CC> expectedTempCCs = new ArrayList<CC>();
		expectedTempCCs.addAll(prevCCs);

		// 마지막 Timestamp 와 함께 마지막 CC 를 복제해서 최종 CC 로 사용
		CC lastCC = prevCCs.get(prevCCs.size() - 1);
		CC clonedLastCC = (CC) lastCC.clone();
		clonedLastCC.setTimestamp(lastTimestamp);
		expectedTempCCs.add(clonedLastCC);

		// 임시 통계치 생성
		return new SimpleTraceStat(expectedTempCCs);
	}

	public int length;
	public int initGs;
	public float avgGs; // 평균 그룹크기
	public float avgNd; // 평균 Node Degree
	public float mttNodeDepartures; // 평균 Node 이탈간격
	public int numNodeArrivals = 0;
	public int numNodeDepartures = 0;
	public int numEdgeArrivals = 0;
	public int numEdgeDepartures = 0;
	public int sumGed = 0;
	public float sumGcd = 0;

	public SimpleTraceStat(ArrayList<CC> ccs) {
		Mean avg = new Mean();

		// Trace 시간 길이 구하기
		CC initCc = ccs.get(0);
		length = ccs.get(ccs.size() - 1).getTimestamp() - initCc.getTimestamp();
		initGs = initCc.getNodeCount();

		// Node Degree 합 구하기
		float sumNd = 0;
		float sumCcNd = 0;
		for (int nid : initCc.getNids()) {
			sumCcNd += initCc.getNeighborCount(nid);
		}
		sumNd += sumCcNd / initCc.getNodeCount();

		// 가중 평균, 분산을 구하기 위한 자료구조
		ArrayList<Double> valueGss = new ArrayList<Double>();
		ArrayList<Integer> intervalWeights = new ArrayList<Integer>();

		// 통계치 수집
		// ////////////////////////////////////////////////////////

		// 시간 초기화
		int initTimestamp = initCc.getTimestamp();
		int prevTimestamp = initTimestamp;

		// Node 이탈간격의 평균을 구하기 위해 가입시간을 저장하는 Map
		HashMap<Integer, Integer> nid2ArrivalTimestamp = new HashMap<Integer, Integer>();
		initCc.getNids().forEach(
				initNid -> nid2ArrivalTimestamp.put(initNid, initTimestamp));
		int sumTimeToNodeDepartures = 0;
		int cntTimeToNodeDepartures = 0;

		// 2번째 CC 부터 시작
		CC prevCC = initCc;
		for (int i = 1; i < ccs.size(); i++) {
			CC curCC = ccs.get(i);
			int curTimestamp = curCC.getTimestamp();

			// 이전 CC 와 현재 CC 를 비교해서 가입 / 이탈 관련 통계 계산
			Pair<HashSet<Integer>> nodeDiff = CC.getNodeDiff(prevCC, curCC);
			Pair<HashSet<Edge>> edgeDiff = CC.getEdgeDiff(prevCC, curCC);
			// int numArrivals = nodeDiffPair.first.size();

			numNodeArrivals += nodeDiff.getFirst().size();
			numNodeDepartures += nodeDiff.getSecond().size();
			numEdgeArrivals += edgeDiff.getFirst().size();
			numEdgeDepartures += edgeDiff.getSecond().size();

			sumGed += CC.getGraphEditDistance(nodeDiff, edgeDiff);
			sumGcd += CC.getGraphCentralityDistance(prevCC, curCC);

			sumCcNd = 0;
			for (int nid : initCc.getNids()) {
				sumCcNd += initCc.getNeighborCount(nid);
			}
			sumNd += sumCcNd / initCc.getNodeCount();

			// 가입했을 경우 가입정보 기록
			if (numNodeArrivals > 0) {
				for (int arrivedNid : nodeDiff.getFirst()) {
					nid2ArrivalTimestamp.put(arrivedNid, curTimestamp);
				}
			}
			// 이탈했을 경우 이탈정보 누적
			if (numNodeDepartures > 0) {
				for (int departedNid : nodeDiff.getSecond()) {
					// 이탈 반영
					int prevArrivedTimestamp = nid2ArrivalTimestamp
							.remove(departedNid);

					// 이탈정보 누적
					int timeToNodeDepartures = curTimestamp
							- prevArrivedTimestamp;
					sumTimeToNodeDepartures += timeToNodeDepartures;
					cntTimeToNodeDepartures++;
				}
			}

			// 현재 구간 관련 통계치 계산
			// ////////////////////////////////////////////////////////

			// 앞구간 가중치 저장
			int interval = curTimestamp - prevTimestamp;
			intervalWeights.add(interval);

			// 구간 그룹크기 평균
			int numPrevGs = prevCC.getNodeCount();
			int numCurGs = curCC.getNodeCount();
			double intervalMeanGs = (numPrevGs + numCurGs) / 2.0;
			valueGss.add(intervalMeanGs);

			prevCC = curCC;
			prevTimestamp = curTimestamp;
		}

		// ArrayList 를 배열로 변환
		double[] valueGssArr = valueGss.stream()
				.mapToDouble(Double::doubleValue).toArray();
		double[] intervalWeightsArr = intervalWeights.stream()
				.mapToDouble(Integer::doubleValue).toArray();

		// 통계치 계산
		// ////////////////////////////////////////////////////////
		avgGs = (float) avg.evaluate(valueGssArr, intervalWeightsArr);
		avgNd = sumNd / ccs.size();
		mttNodeDepartures = (float) sumTimeToNodeDepartures
				/ cntTimeToNodeDepartures;
	}

}
