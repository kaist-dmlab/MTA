package com.mobilemr.task_allocation.heuristic;

import io.jenetics.EnumGene;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.engine.Codecs;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.Limits;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import com.mobilemr.task_allocation.EstTraceRunner;
import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.JobResult;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.CommType;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;

@SuppressWarnings("unchecked")
public class HeuristicMtaS extends Heuristic {

	protected JobProfile jp;
	protected TaskPhaseProfile tpp;
	protected TaskPhaseProfile nextTpp;
	protected MultihopTrace trace;
	protected int realModeTime;
	protected MultihopSnapshot curSnapshot;
	protected HashMap<Integer, Task> srcTid2Task;
	protected ArrayList<Integer> srcNids;
	protected HashMap<Integer, Integer> srcNid2Tid;
	protected ArrayList<Integer> srcTids;
	protected HistoryStat historyStat;
	protected HashMap<Integer, Integer> zb2OrgNid;
	protected Integer numSearches = 0;

	@Override
	public GeneralPair<ArrayList<OrderedNumPair<Integer>>, Double> allocateTasks(
			String inputTraceId, JobProfile jp, TaskPhaseProfile tpp,
			TaskPhaseProfile nextTpp, MultihopTrace trace, int realModeTime,
			HashMap<Integer, Task> srcTid2Task, ArrayList<Integer> srcNids,
			HashMap<Integer, Integer> srcNid2Tid, ArrayList<Integer> srcTids,
			HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid) {
		this.jp = jp;
		this.tpp = tpp;
		this.nextTpp = nextTpp;
		this.trace = trace;
		this.realModeTime = realModeTime;
		this.curSnapshot = trace.getClosestSnapshot(realModeTime);
		this.srcTid2Task = srcTid2Task;
		this.srcNids = srcNids;
		this.srcNid2Tid = srcNid2Tid;
		this.srcTids = srcTids;
		this.historyStat = historyStat;
		this.zb2OrgNid = zb2OrgNid;

		// 선택할 Node 수가 부족할 경우 제한함
		int maxNodesToSelect = (int) (trace.getClosestSnapshot(0)
				.getNodeCount() * Params.MAX_CLUSTER_UTILIZATION);
		int curNumNodes = curSnapshot.getNodeCount();
		if (curNumNodes < maxNodesToSelect) {
			maxNodesToSelect = curNumNodes;
		}

		// 현재 Snapshot 의 전체 Node ID 에 대해 입력 준비
		// Reproducibility 를 위해 ArrayList 먼저 생성 후 ISeq 로 변환
		ArrayList<Integer> curNids = curSnapshot.getNids();
		ISeq<Integer> allNodeAlleles = curNids.stream().collect(ISeq.toISeq());

		// 유전 알고리즘으로 최소 Cost 를 가지는 Dst Node Subset 결정
		Engine<EnumGene<Integer>, CostAndConn> engine = Engine
				.builder(fitness(this),
						Codecs.ofSubSet(allNodeAlleles, maxNodesToSelect)) //
				.populationSize(Params.POPULATION_SIZE) //
				.survivorsSelector(Params.SURVIVORS_SELECTOR) //
				.offspringSelector(Params.OFFSPRING_SELECTOR) //
				.alterers(Params.CROSSOVER, //
						Params.MUTATOR) //
				.optimize(Optimize.MINIMUM) //
				.build();

		// 최적화 결과 추출
		Phenotype<EnumGene<Integer>, CostAndConn> bestPhenotype = engine
				.stream()
				.limit(Limits.bySteadyFitness(Params.STEADY_GENERATIONS))
				.limit(Params.MAX_GENERATIONS)
				.collect(EvolutionResult.toBestPhenotype());

		// 최적화 결과 반환
		return new GeneralPair<>(bestPhenotype.getFitness().getConnections(),
				(double) numSearches / Params.POPULATION_SIZE);
	}

	// @Override
	// public void beforeFitnessEvaluation(
	// ISeq<Phenotype<EnumGene<Integer>, CostAndConn>> population) {
	// // 전체 Population 의 적합도에 대해 Normalize 해야 하므로,
	// // 이전에 Normalized 됐던 결과를 재계산 하기 위해
	// // 이전 Normalized 결과가 저장된 Evaluation 부분 초기화
	// for (Phenotype<EnumGene<Integer>, CostAndConn> individual : population) {
	// individual.resetEvaluation();
	// }
	// }

	protected static Function<ISeq<Integer>, CostAndConn> fitness(
			HeuristicMtaS h) {
		return nodeAlleles -> {
			ArrayList<Integer> dstNids = new ArrayList<Integer>();
			nodeAlleles.forEach(nid -> dstNids.add(nid));

			// 적합도 평가
			return h.evaluateFitness(h, dstNids);
		};
	}

	protected CostAndConn evaluateFitness(HeuristicMtaS h,
			ArrayList<Integer> dstNids) {
		// ETX Metric 을 Weight 로 하는 Graph 로 변환
		// WeightedUndirectedSparseGraph curWeightedGraph = h.curSnapshot
		// .toEtxWeightedGraph();
		ArrayList<OrderedNumPair<Integer>> conns;
		if (h.tpp.commType == CommType.REPLICATE) {
			// Src:Dst = M:1 연결
			conns = createBestClosestPaths(h.curSnapshot, h.srcNids,
					h.srcNid2Tid, dstNids);
			// 논문에는 Bubble Path 라고 적었지만, Data Locality 가 작동을 안해서
			// 우선 성능이 더 잘 나오는 위 메소드로 실험함
			// conns = createGreedyBubblePaths(h, h.srcTids, dstNids);

		} else if (h.tpp.commType == CommType.SHUFFLE) {
			// Src:Dst = M:N 연결
			conns = createShufflePaths(h.srcTids, dstNids);

		} else {
			throw new IllegalStateException(h.tpp.commType + "");
		}

		// Time to First Phase Success Cost 계산
		// SingleCost cost = new SingleCost();
		// int realModeTimestampOfInputFile = h.trace.getInitCC().getTimestamp()
		// + h.realModeTime;
		// float LD = 0;
		// float sumD = 0;
		// for (OrderedNumPair<Integer> conn : conns) {
		// int srcTid = conn.getFirst();
		// int srcNid = h.srcTid2Task.get(srcTid).nid;
		// int dstZbNid = conn.getSecond();
		// // Failure Rate 가 NaN 인 경우 이용 불가능하므로
		// // 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
		// int dstOrgNid = h.zb2OrgNid.get(dstZbNid);
		// float l_j = h.historyStat.getFailureRate(dstOrgNid,
		// realModeTimestampOfInputFile);
		// if (Float.isNaN(l_j)) {
		// throw new RuntimeException(dstZbNid + " "
		// + realModeTimestampOfInputFile);
		// }
		// double d_comm_j = curWeightedGraph.getDistance(srcNid, dstZbNid);
		// LD += l_j * d_comm_j;
		// sumD += d_comm_j;
		// }
		//
		// // Time to First Phase Success Cost 저장
		// int N = dstNids.size();
		// float d_S = sumD / N;
		// float l_P = LD / d_S;
		// float r_P = (float) Math.exp(-LD);
		// float r_P_inverse = (float) Math.exp(LD);
		// float d_F = -(d_S + 1 / l_P) * r_P + 1 / l_P;
		// float TPS = d_S + (r_P_inverse - 1) * d_F;
		// cost.setCost(TPS);
		// // Logger.println(" l_P : " + l_P + " r_P_inverse : "
		// // + r_P_inverse + " d_S : " + d_S + " d_F : " + d_F
		// // + " TPS : " + TPS + " " + cost.getCost());

		// Time to First Phase Success Cost 계산
		CostAndConn costAndConn = new CostAndConn();
		costAndConn.setCost(calcTPS(h, conns));

		// 결과 저장
		costAndConn.setConnections(conns);

		// 탐색 횟수 증가
		synchronized (h.numSearches) {
			h.numSearches++;
		}

		// 최종 Cost 반환
		return costAndConn;
	}

	private static float calcTPS(HeuristicMtaS h,
			ArrayList<OrderedNumPair<Integer>> conns) {
		HashMap<Integer, Task> srcTid2TaskCloned = new HashMap<>();
		h.srcTid2Task.forEach((k, v) -> srcTid2TaskCloned.put(k,
				(Task) v.clone()));
		JobResult result = new EstTraceRunner(h, h.trace, h.realModeTime, h.tpp,
				h.nextTpp, srcTid2TaskCloned, conns).start();
		// 현재 Topology 로만 작업 수행시간을 추정하기 때문에
		// 실패가 발생할 수 없음
		float TPS = Float.MAX_VALUE;
		if (!result.succeeded()) {
			// throw new IllegalStateException();
		} else {
			HashMap<Integer, Float> dstNid2CommDuration = result
					.getDstNid2CommDuration();

			// 미리 전체 관점의 변수 수집
			int realModeTimestampOfInputFile = h.trace.getInitCC()
					.getTimestamp() + h.realModeTime;
			// float max_m = Float.MIN_VALUE;
			// for (int dstZbNid : dstNids) {
			// Float d_comm_j = dstNid2CommDuration.get(dstZbNid);
			// if (d_comm_j != null) {
			// // Failure Rate 가 NaN 인 경우 이용 불가능하므로
			// // 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
			// int dstOrgNid = h.zb2OrgNid.get(dstZbNid);
			// float m_j = h.historyStat.getMttf(dstOrgNid,
			// realModeTimestampOfInputFile);
			// if (Float.isNaN(m_j)) {
			// throw new RuntimeException(dstZbNid + " "
			// + realModeTimestampOfInputFile);
			// }
			// if (m_j > max_m) {
			// max_m = m_j;
			// }
			// }
			// }
			// float max_d = (float) result.getSuccessTime();
			// float scaleFactor = max_d / max_m;

			// int N = dstNids.size();
			// float o = h.nextTpp.inputSize / N;
			// float d_proc = o / h.nextTpp.B_P;
			float LD = 0;
			float sumD = 0;
			int actualN = 0;
			// float sumT = 0;
			ArrayList<Integer> dstNids = getDstNids(conns);
			for (int dstZbNid : dstNids) {
				Float d_comm_j = dstNid2CommDuration.get(dstZbNid);
				if (d_comm_j != null) {
					// Failure Rate 가 NaN 인 경우 이용 불가능하므로
					// 오류를 발생시켜서 현재 Trace 를 비교대상에서 제외
					int dstOrgNid = h.zb2OrgNid.get(dstZbNid);
					float l_j = h.historyStat.getFailureRate(dstOrgNid,
							realModeTimestampOfInputFile);
					if (Float.isNaN(l_j)) {
						throw new RuntimeException(dstZbNid + " "
								+ realModeTimestampOfInputFile);
					}
					// float l_j = 1 / (m_j * scaleFactor);

					// 무한대 속도를 막기 위해 1 초씩 추가
					float d_j = d_comm_j + 1;
					// float d_j = d_comm_j + d_proc;
					// float d_hat_j = d_j * (float) Math.exp(l_j * d_j);
					// float d_S_j = d_j;
					// float r_P_j = (float) Math.exp(-l_j * d_j);
					// float r_P_j_inverse = (float) Math.exp(l_j * d_j);
					// float d_F_j = -(d_S_j + 1 / l_j) * r_P_j + 1 / l_j;
					// float TPS_j = d_S_j + (r_P_j_inverse - 1) * d_F_j;
					// sumT += TPS_j;
					// System.out.println(m_j + " " + scaleFactor + " " + l_j
					// + " " + d_j + " " + b_j + " " + b_hat_j + " "
					// + d_hat_j);
					LD += l_j * d_j;
					sumD += d_j;
					actualN++;
				}
			}
			// costAndConn.setCost(LD);

			if (actualN > 0) {
				float d_S = sumD / actualN;
				float l_P = LD / d_S;
				float r_P = (float) Math.exp(-LD);
				float r_P_inverse = (float) Math.exp(LD);
				float d_F = -(d_S + 1 / l_P) * r_P + 1 / l_P;
				TPS = d_S + (r_P_inverse - 1) * d_F;
				// Logger.println(" l_P : " + l_P + " r_P_inverse : "
				// + r_P_inverse + " d_S : " + d_S + " d_F : " + d_F
				// + " TPS : " + TPS + " " + cost.getCost());
			}
		}
		return TPS;
	}

	public static ArrayList<OrderedNumPair<Integer>> createGreedyBubblePaths(
			HeuristicMtaS h, ArrayList<Integer> srcTids,
			ArrayList<Integer> dstNidsOrg) {
		if (srcTids.size() < dstNidsOrg.size()) {
			throw new IllegalStateException(srcTids.size() + " "
					+ dstNidsOrg.size());
		}
		float stepSize = (float) dstNidsOrg.size() / srcTids.size();

		ArrayList<Integer> bestDstNids = new ArrayList<>();
		ArrayList<OrderedNumPair<Integer>> bestConns = new ArrayList<>();
		for (int dstNid : dstNidsOrg) {
			bestDstNids.add(dstNid);
		}
		for (int k = 0; k < srcTids.size(); k++) {
			int idxDstNids = (int) (k * stepSize);
			int srcTid = srcTids.get(k);
			int dstNid = bestDstNids.get(idxDstNids);
			bestConns.add(new OrderedNumPair<Integer>(srcTid, dstNid));
		}

		for (int i = 0; i < bestDstNids.size(); i++) {
			for (int j = 1; j < bestDstNids.size() - i; j++) {
				// Copy the last task order and swap j-1 with j;
				ArrayList<Integer> nextDstNids = new ArrayList<>();
				bestDstNids.forEach(dstNid -> nextDstNids.add(dstNid));
				nextDstNids.add(j - 1, nextDstNids.remove(j));

				// Task 와 Node 간 연결 갱신
				ArrayList<OrderedNumPair<Integer>> nextConns = new ArrayList<>();
				for (int k = 0; k < srcTids.size(); k++) {
					int idxDstNids = (int) (k * stepSize);
					int srcTid = srcTids.get(k);
					int dstNid = nextDstNids.get(idxDstNids);
					nextConns.add(new OrderedNumPair<Integer>(srcTid, dstNid));
				}
				if (calcTPS(h, nextConns) < calcTPS(h, bestConns)) {
					bestDstNids = nextDstNids;
					bestConns = nextConns;
				}
			}
		}
		return bestConns;
	}

	// @Override
	// public void afterFitnessEvaluation(
	// ISeq<Phenotype<EnumGene<Integer>, CostAndConn>> population) {
	// }
	//
	// @Override
	// public void afterEvolve(Phenotype<EnumGene<Integer>, CostAndConn> best) {
	// // Logger.println("GLOBAL BEST : " + best.getFitness().getCost());
	// }

}
