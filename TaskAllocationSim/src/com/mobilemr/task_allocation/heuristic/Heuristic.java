package com.mobilemr.task_allocation.heuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.mobilemr.task_allocation.EstTraceRunner;
import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.AllocationResult;
import com.mobilemr.task_allocation.platform.JobResult;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.task_allocation.util.Common;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.IntNodeUndirectedSparseGraph;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;
import drcl.inet.protocol.aodv.struct.WeightedUndirectedSparseGraph;

public abstract class Heuristic {

	// 계속 사용되므로 미리 Singleton 으로 등록 후 재사용
	private static HashMap<String, Heuristic> CLS_2_HEURISTIC = new HashMap<>();

	static {
		CLS_2_HEURISTIC.put(HeuristicHadoop.class.getSimpleName(), new HeuristicHadoop());
		CLS_2_HEURISTIC.put(HeuristicPurlieus.class.getSimpleName(), new HeuristicPurlieus());
		CLS_2_HEURISTIC.put(HeuristicMtaS.class.getSimpleName(), new HeuristicMtaS());
		CLS_2_HEURISTIC.put(HeuristicMtaD.class.getSimpleName(), new HeuristicMtaD());
	}

	public static Heuristic of(Class<? extends Heuristic> clsHeuristic) {
		return CLS_2_HEURISTIC.get(clsHeuristic.getSimpleName());
	}

	public HashMap<Integer, Task> allocateData(JobProfile jp, MultihopSnapshot initSnapshot) {
		// 기본 전략: 목표 Node 개수만큼 Random Input 분배
		int numInitNodes = (int) (initSnapshot.getNodeCount() * Params.MAX_CLUSTER_UTILIZATION);
		double inputSize_Per_Task = jp.inputSize / numInitNodes;
		ArrayList<Integer> initNids = initSnapshot.getRandomNids(numInitNodes);
		HashMap<Integer, Task> initTid2Task = new HashMap<>();
		for (Integer initNid : initNids) {
			int tid = Task.generateId();
			initTid2Task.put(tid, new Task(tid, initNid, inputSize_Per_Task));
		}
		return initTid2Task;
	}

	public AllocationResult allocateTasks(String inputTraceId, JobProfile jp, TaskPhaseProfile tpp,
			TaskPhaseProfile nextTpp, MultihopTrace trace, int realModeTime, HashMap<Integer, Task> srcTid2Task,
			HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid) {
		// Src 관련 자료구조 초기화
		ArrayList<Integer> srcNids = new ArrayList<>();
		HashMap<Integer, Integer> srcNid2Tid = new HashMap<>();
		ArrayList<Integer> srcTids = new ArrayList<>();
		for (Task srcTask : srcTid2Task.values()) {
			int srcNid = srcTask.nid;
			srcNids.add(srcNid);
			srcNid2Tid.put(srcNid, srcTask.id);
			srcTids.add(srcTask.id);
		}

		// Task 할당 수행
		GeneralPair<ArrayList<OrderedNumPair<Integer>>, Double> conns = allocateTasks(inputTraceId, jp, tpp, nextTpp,
				trace, realModeTime, srcTid2Task, srcNids, srcNid2Tid, srcTids, historyStat, zb2OrgNid);

		// Task 할당 결과 반환
		return toAllocationResult(conns.getFirst(), conns.getSecond(), tpp, nextTpp, trace, realModeTime, srcTid2Task,
				historyStat, zb2OrgNid);
	}

	public abstract GeneralPair<ArrayList<OrderedNumPair<Integer>>, Double> allocateTasks(String inputTraceId,
			JobProfile jp, TaskPhaseProfile tpp, TaskPhaseProfile nextTpp, MultihopTrace trace, int realModeTime,
			HashMap<Integer, Task> srcTid2Task, ArrayList<Integer> srcNids, HashMap<Integer, Integer> srcNid2Tid,
			ArrayList<Integer> srcTids, HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid);

	private StandardDeviation STD = new StandardDeviation();

	private AllocationResult toAllocationResult(ArrayList<OrderedNumPair<Integer>> conns, Double numEvals,
			TaskPhaseProfile tpp, TaskPhaseProfile nextTpp, MultihopTrace trace, int realModeTime,
			HashMap<Integer, Task> srcTid2Task, HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid) {
		// 마지막 TaskPhase 이전과 할당 결과가 존재할 때 할당 결과정보 추출
		Double reliability = null;
		// Double contention = (double) 0;
		Double optimalIdx1 = null;
		Double optimalIdx2 = null;
		// ArrayList<OrderedNumPair<Float>> FAndDs = new ArrayList<>();
		ArrayList<Integer> dstNids = Heuristic.getDstNids(conns);
		if (!dstNids.isEmpty()) {
			// 현재 선택된 Dst Nodes 에 대해서 Reliability 의 Exponent Sum 을 구함
			int realModeTimestampOfInputFile = trace.getInitCC().getTimestamp() + realModeTime;
			// int N = dstNids.size();
			// float o = nextTpp.inputSize / N;
			// float d_proc = o / nextTpp.B_P;

			float LD = 0;
			for (int dstZbNid : dstNids) {
				// Failure Rate 가 NaN 인 경우 이용 불가능하므로
				// 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
				int dstOrgNid = zb2OrgNid.get(dstZbNid);
				float l_j = historyStat.getFailureRate(dstOrgNid, realModeTimestampOfInputFile);
				if (Float.isNaN(l_j)) {
					throw new RuntimeException(dstZbNid + " " + realModeTimestampOfInputFile);
				}
				LD += l_j;
			}
			reliability = Math.exp(-LD);

			// 할당 결과를 기반으로 다음 TaskPhase 수행시간 추정
			HashMap<Integer, Task> srcTid2TaskCloned = new HashMap<>();
			srcTid2Task.forEach((k, v) -> srcTid2TaskCloned.put(k, (Task) v.clone()));
			JobResult result = new EstTraceRunner(this, trace, realModeTime, tpp, nextTpp, srcTid2TaskCloned, conns)
					.start();
			// 현재 Topology 로만 작업 수행시간을 추정하기 때문에
			// 실패가 발생할 수 없음
			if (result.failed()) {
				// throw new IllegalStateException();
			} else {
				HashMap<Integer, Float> dstNid2CommDuration = result.getDstNid2CommDuration();
				// contention = result.getSuccessTime();

				// 최대 최소 Bound 구하기
				// float max_l = Float.MIN_VALUE;
				// float min_l = Float.MAX_VALUE;
				// float max_d = Float.MIN_VALUE;
				// float min_d = Float.MAX_VALUE;
				// float sumRatio = 0;
				// int actualN = 0;
				// for (int dstZbNid : dstNids) {
				// Float d_comm_j = dstNid2CommDuration.get(dstZbNid);
				// if (d_comm_j != null) {
				// // Failure Rate 가 NaN 인 경우 이용 불가능하므로
				// // 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
				// int dstOrgNid = zb2OrgNid.get(dstZbNid);
				// float l_j = historyStat.getFailureRate(dstOrgNid,
				// realModeTimestampOfInputFile);
				// if (Float.isNaN(l_j)) {
				// throw new RuntimeException(dstZbNid + " "
				// + realModeTimestampOfInputFile);
				// }
				//
				// // 무한대 속도를 막기 위해 1 초씩 추가
				// float d_j = d_comm_j + 1;
				// // float b_j = o / d_j;
				// max_l = l_j > max_l ? l_j : max_l;
				// min_l = l_j < min_l ? l_j : min_l;
				// max_d = d_j > max_d ? d_j : max_d;
				// min_d = d_j < min_d ? d_j : min_d;
				//
				// // sumRatio += d_j;
				// // actualN++;
				// }
				// }
				// float scaleFactor = max_d / max_l;
				// float avgRatio = sumRatio / actualN;

				ArrayList<Double> ratios1 = new ArrayList<>();
				ArrayList<Double> ratios2 = new ArrayList<>();
				for (int dstZbNid : dstNids) {
					Float d_comm_j = dstNid2CommDuration.get(dstZbNid);
					if (d_comm_j != null) {
						// Failure Rate 가 NaN 인 경우 이용 불가능하므로
						// 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
						int dstOrgNid = zb2OrgNid.get(dstZbNid);
						float l_j = historyStat.getFailureRate(dstOrgNid, realModeTimestampOfInputFile);
						if (Float.isNaN(l_j)) {
							throw new RuntimeException(dstZbNid + " " + realModeTimestampOfInputFile);
						}
						// float l_j = 1 / (m_j * scaleFactor);

						// 무한대 속도를 막기 위해 1 초씩 추가
						float d_j = d_comm_j + 1;
						// float b_j = o / d_j;

						// float norm_l_j = max_l == min_l ? 0.5F : (l_j -
						// min_l)
						// / (max_l - min_l);
						// float norm_d_j = max_d == min_d ? 0.5F : (d_j -
						// min_d)
						// / (max_d - min_d);
						double curRatio1 = d_j;// norm_d_j;
						double curRatio2 = l_j;// norm_l_j *
												// norm_d_j *
												// norm_d_j;
						ratios1.add(curRatio1);
						ratios2.add(curRatio2);
						// FAndDs.add(new OrderedNumPair<>(d_j, b_j));
						// System.out.println(l_j + " " + d_j + " " + l_j *
						// d_j);
					}
				}

				// Optimal Index
				double[] ratios1Arr = ratios1.stream().mapToDouble(Double::doubleValue).toArray();
				double[] ratios2Arr = ratios2.stream().mapToDouble(Double::doubleValue).toArray();
				optimalIdx1 = STD.evaluate(ratios1Arr);
				optimalIdx2 = STD.evaluate(ratios2Arr);
			}
		}

		// Contention Degree 계산
		MultihopSnapshot curSnapshot = trace.getClosestSnapshot(realModeTime);
		WeightedUndirectedSparseGraph curWeightedGraph = curSnapshot.toEtxWeightedGraph();
		Double contention = (double) 0;
		for (OrderedNumPair<Integer> conn : conns) {
			int srcTid = conn.getFirst();
			int srcNid = srcTid2Task.get(srcTid).nid;
			int dstNid = conn.getSecond();
			contention += curWeightedGraph.getDistance(srcNid, dstNid);
		}
		int curNumNodes = curSnapshot.getNodeCount();
		double clusterUtilization = (double) dstNids.size() / curNumNodes;
		return new AllocationResult(conns, numEvals, reliability, contention, optimalIdx1, optimalIdx2,
				clusterUtilization);
	}

	public static <E> ArrayList<OrderedNumPair<Integer>> createGreedyClosestPaths(
			IntNodeUndirectedSparseGraph<E> curSnapshot, ArrayList<Integer> srcNids,
			HashMap<Integer, Integer> srcNid2Tid, ArrayList<Integer> dstNids) {
		// Src 와 Dst 이 주어졌을 때, 다음 기준으로 Local & Remote 연결
		// 1. 최대한 Src 와 Dst 의 Local 로 연결하고,
		// 2. 나머지 Src 와 Dst 에 대해, 거리가 가장 가까운 Node 끼리 연결

		// 1. Local 연결
		ArrayList<OrderedNumPair<Integer>> conns = new ArrayList<>();
		ArrayList<Integer> dstNidPool = new ArrayList<>();
		ArrayList<Integer> remoteSrcNids = new ArrayList<>();
		for (Integer srcNid : srcNids) {
			// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
			if (dstNidPool.isEmpty()) {
				dstNids.forEach(dstNid -> dstNidPool.add(dstNid));
			}

			// Local 검사
			if (dstNids.contains(srcNid)) {
				// Local 일 경우 바로 연결
				Integer srcTid = srcNid2Tid.get(srcNid);
				conns.add(new OrderedNumPair<Integer>(srcTid, srcNid));

				// 연결했으므로 Dst Pool 에서 제거
				dstNidPool.remove(srcNid);

			} else {
				// Remote 일 경우 나중에 연결하기 위해 저장
				remoteSrcNids.add(srcNid);
			}
		}

		// 2. Remote 연결

		// Remote Src 노드를 순차적으로 Greedy 탐색
		for (Integer remoteSrcNid : remoteSrcNids) {
			// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
			if (dstNidPool.isEmpty()) {
				dstNids.forEach(x -> dstNidPool.add(x));
			}

			// 현재 Src Node 와 가장 가까운 Dst Node 를 탐색
			double minDist = Float.MAX_VALUE;
			Integer minDstNid = -1;
			for (Integer curDstNid : dstNidPool) {
				double curDist = curSnapshot.getDistance(remoteSrcNid, curDstNid);
				if (curDist < minDist) {
					minDist = curDist;
					minDstNid = curDstNid;
				}
			}

			// Src Tid 와 Dst Nid 연결
			Integer remoteSrcTid = srcNid2Tid.get(remoteSrcNid);
			conns.add(new OrderedNumPair<Integer>(remoteSrcTid, minDstNid));

			// 연결했으므로 Dst Pool 에서 제거
			dstNidPool.remove(minDstNid);
		}
		return conns;
	}

	public static <E> ArrayList<OrderedNumPair<Integer>> createBestClosestPaths(
			IntNodeUndirectedSparseGraph<E> curSnapshot, ArrayList<Integer> srcNids,
			HashMap<Integer, Integer> srcNid2Tid, ArrayList<Integer> dstNids) {
		// Src 와 Dst 이 주어졌을 때, 다음 기준으로 Local & Remote 연결
		// 1. 최대한 Src 와 Dst 의 Local 로 연결하고,
		// 2. 나머지 Src 와 Dst 에 대해, 거리가 가장 가까운 Node 끼리 연결

		// 1. Local 연결
		ArrayList<OrderedNumPair<Integer>> tempConns = new ArrayList<>();
		ArrayList<Integer> tempDstNidPool = new ArrayList<>();
		ArrayList<Integer> tempRemoteSrcNids = new ArrayList<>();
		for (Integer srcNid : srcNids) {
			// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
			if (tempDstNidPool.isEmpty()) {
				dstNids.forEach(dstNid -> tempDstNidPool.add(dstNid));
			}

			// Local 검사
			if (dstNids.contains(srcNid)) {
				// Local 일 경우 바로 연결
				Integer srcTid = srcNid2Tid.get(srcNid);
				tempConns.add(new OrderedNumPair<Integer>(srcTid, srcNid));

				// 연결했으므로 Dst Pool 에서 제거
				tempDstNidPool.remove(srcNid);

			} else {
				// Remote 일 경우 나중에 연결하기 위해 저장
				tempRemoteSrcNids.add(srcNid);
			}
		}

		// 2. Remote 가 있을 경우 연결
		if (tempRemoteSrcNids.isEmpty()) {
			return tempConns;
		} else {
			// remoteSrcNids 를 매번 다른 것을 처음으로 추가해서 Greedy 의 결과 변화 유도
			double minGreedyDist = Float.MAX_VALUE;
			ArrayList<OrderedNumPair<Integer>> minConns = null;
			for (int i = 0; i < tempRemoteSrcNids.size(); i++) {
				// Temp 변수를 이용한 현재 Greedy 시험 변수 초기화
				ArrayList<Integer> dstNidPool = new ArrayList<>(tempDstNidPool);
				ArrayList<Integer> remoteSrcNids = new ArrayList<>();
				for (int j = i; j < tempRemoteSrcNids.size(); j++) {
					remoteSrcNids.add(tempRemoteSrcNids.get(j));
				}
				for (int j = 0; j < i; j++) {
					remoteSrcNids.add(tempRemoteSrcNids.get(j));
				}
				ArrayList<OrderedNumPair<Integer>> conns = new ArrayList<>();
				tempConns.forEach(x -> conns.add(x));

				// Remote Src 노드를 순차적으로 Greedy 탐색
				double sumGreedyDist = 0;
				for (Integer remoteSrcNid : remoteSrcNids) {
					// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
					if (dstNidPool.isEmpty()) {
						dstNids.forEach(x -> dstNidPool.add(x));
					}

					// 현재 Src Node 와 가장 가까운 Dst Node 를 탐색
					double minDist = Float.MAX_VALUE;
					Integer minDstNid = -1;
					for (Integer curDstNid : dstNidPool) {
						double curDist = curSnapshot.getDistance(remoteSrcNid, curDstNid);
						if (curDist < minDist) {
							minDist = curDist;
							minDstNid = curDstNid;
						}
					}
					sumGreedyDist += minDist;

					// Src Tid 와 Dst Nid 연결
					Integer remoteSrcTid = srcNid2Tid.get(remoteSrcNid);
					conns.add(new OrderedNumPair<Integer>(remoteSrcTid, minDstNid));

					// 연결했으므로 Dst Pool 에서 제거
					dstNidPool.remove(minDstNid);
				}

				// 최소 Greedy 조합 검사
				if (sumGreedyDist < minGreedyDist) {
					minGreedyDist = sumGreedyDist;
					minConns = conns;
				}
			}
			return minConns;
		}
	}

	public static <E> ArrayList<OrderedNumPair<Integer>> createRandomPaths(IntNodeUndirectedSparseGraph<E> curSnapshot,
			ArrayList<Integer> srcNids, HashMap<Integer, Integer> srcNid2Tid, ArrayList<Integer> dstNids) {
		// Src 와 Dst 이 주어졌을 때, 다음 기준으로 Local & Remote 연결
		// 1. 최대한 Src 와 Dst 의 Local 로 연결하고,
		// 2. 나머지 Src 와 Dst 에 대해, Random 으로 연결

		// 1. Local 연결
		ArrayList<OrderedNumPair<Integer>> conns = new ArrayList<>();
		ArrayList<Integer> remoteSrcNids = new ArrayList<>();
		ArrayList<Integer> dstNidPool = new ArrayList<>();
		for (Integer srcNid : srcNids) {
			// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
			if (dstNidPool.isEmpty()) {
				dstNids.forEach(x -> dstNidPool.add(x));
			}

			// Local 검사
			if (dstNids.contains(srcNid)) {
				// Local 일 경우 바로 연결
				Integer srcTid = srcNid2Tid.get(srcNid);
				conns.add(new OrderedNumPair<Integer>(srcTid, srcNid));

				// 연결했으므로 Dst Pool 에서 제거
				dstNidPool.remove(srcNid);

			} else {
				// Remote 일 경우 나중에 연결하기 위해 저장
				remoteSrcNids.add(srcNid);
			}
		}

		// 2. Remote 연결
		// Cluster Utilization 을 최대한 만족시키는 Random 할당
		for (Integer remoteSrcNid : remoteSrcNids) {
			// Load Balancing 을 위해 Dst Pool 이 비어있을 경우 복제
			if (dstNidPool.isEmpty()) {
				dstNids.forEach(x -> dstNidPool.add(x));
			}

			// 연결
			Integer remoteSrcTid = srcNid2Tid.get(remoteSrcNid);
			Integer dstNid = dstNidPool.get(Common.R.nextInt(dstNidPool.size()));
			conns.add(new OrderedNumPair<Integer>(remoteSrcTid, dstNid));

			// 연결했으므로 Dst Pool 에서 제거
			dstNidPool.remove(dstNid);
		}
		return conns;
	}

	public static ArrayList<OrderedNumPair<Integer>> createShufflePaths(ArrayList<Integer> srcTids,
			ArrayList<Integer> dstNids) {
		// 각 Src Tid 를 모든 Dst Nid 와 연결
		ArrayList<OrderedNumPair<Integer>> conns = new ArrayList<>();
		for (Integer srcTid : srcTids) {
			for (Integer dstNid : dstNids) {
				conns.add(new OrderedNumPair<Integer>(srcTid, dstNid));
			}
		}
		return conns;
	}

	public static ArrayList<Integer> getDstNids(ArrayList<OrderedNumPair<Integer>> conns) {
		HashSet<Integer> dstNidSet = new HashSet<>();
		for (OrderedNumPair<Integer> conn : conns) {
			dstNidSet.add(conn.getSecond());
		}
		return new ArrayList<>(dstNidSet);
	}

}
