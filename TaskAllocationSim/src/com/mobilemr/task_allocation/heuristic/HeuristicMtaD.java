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

import com.mobilemr.task_allocation.Params;
import com.mobilemr.task_allocation.platform.Task;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.profile.TaskPhaseProfile;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.OrderedNumPair;

import drcl.inet.protocol.aodv.struct.MultihopTrace;

@SuppressWarnings("unchecked")
public class HeuristicMtaD extends HeuristicMtaS {

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

		// MTA-S 자체의 Local Optima 찾는 능력의 한계로 인해
		// haggle 데이터셋에서 수렴을 안해서 아래처럼 탐색범위를 강제함
		// int K_Low = 1;
		// int K_High = curSnapshot.getNodeCount();
		int K_Low = (int) (curSnapshot.getNodeCount() * 0.6);
		int K_High = (int) (curSnapshot.getNodeCount() * 0.8);
		CostAndConn bestCostAndConn = new CostAndConn();
		while (K_Low <= K_High) {
			int K_L = (3 * K_Low + K_High) / 4;
			int K_M = (K_Low + K_High) / 2;
			int K_R = (K_Low + 3 * K_High) / 4;
			// System.out.println(K_Low + " " + K_High);

			// 현재 Snapshot 의 전체 Node ID 에 대해 입력 준비
			// Reproducibility 를 위해 ArrayList 먼저 생성 후 ISeq 로 변환
			ArrayList<Integer> curNids = curSnapshot.getNids();
			ISeq<Integer> allNodeAlleles = curNids.stream().collect(
					ISeq.toISeq());

			// 왼쪽 오른쪽 Engine 생성
			Engine<EnumGene<Integer>, CostAndConn> engine_L = Engine
					.builder(fitness(this),
							Codecs.ofSubSet(allNodeAlleles, K_L)) //
					.populationSize(Params.POPULATION_SIZE) //
					.survivorsSelector(Params.SURVIVORS_SELECTOR) //
					.offspringSelector(Params.OFFSPRING_SELECTOR) //
					.alterers(Params.CROSSOVER, //
							Params.MUTATOR) //
					.optimize(Optimize.MINIMUM) //
					.build();
			Engine<EnumGene<Integer>, CostAndConn> engine_R = Engine
					.builder(fitness(this),
							Codecs.ofSubSet(allNodeAlleles, K_R)) //
					.populationSize(Params.POPULATION_SIZE) //
					.survivorsSelector(Params.SURVIVORS_SELECTOR) //
					.offspringSelector(Params.OFFSPRING_SELECTOR) //
					.alterers(Params.CROSSOVER, //
							Params.MUTATOR) //
					.optimize(Optimize.MINIMUM) //
					.build();

			// 최적화 결과 추출
			Phenotype<EnumGene<Integer>, CostAndConn> bestPhenotype_L = engine_L
					.stream()
					.limit(Limits.bySteadyFitness(Params.STEADY_GENERATIONS))
					.limit(Params.MAX_GENERATIONS)
					.collect(EvolutionResult.toBestPhenotype());
			Phenotype<EnumGene<Integer>, CostAndConn> bestPhenotype_R = engine_R
					.stream()
					.limit(Limits.bySteadyFitness(Params.STEADY_GENERATIONS))
					.limit(Params.MAX_GENERATIONS)
					.collect(EvolutionResult.toBestPhenotype());

			// 더 Cost 가 작은 쪽으로 탐색
			float cost_L = bestPhenotype_L.getFitness().getCost();
			float cost_R = bestPhenotype_R.getFitness().getCost();
			if (cost_L < cost_R) {
				K_High = K_M - 1;
				bestCostAndConn = bestPhenotype_L.getFitness();
			} else {
				K_Low = K_M + 1;
				bestCostAndConn = bestPhenotype_R.getFitness();
			}

			// 어느쪽도 성공 못하면 빠른 포기
			if (bestCostAndConn.getCost() == Float.MAX_VALUE) {
				break;
			}
		}
		return new GeneralPair<>(bestCostAndConn.getConnections(),
				(double) numSearches / Params.POPULATION_SIZE);
	}

}
