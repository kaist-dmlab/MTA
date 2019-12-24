package com.mobilemr.task_allocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.StatRecords;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.mobilemr.task_allocation.heuristic.Heuristic;
import com.mobilemr.task_allocation.heuristic.HeuristicHadoop;
import com.mobilemr.task_allocation.heuristic.HeuristicMtaS;
import com.mobilemr.task_allocation.heuristic.HeuristicPurlieus;
import com.mobilemr.task_allocation.platform.JobResult;
import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.util.ByteUtil;
import com.mobilemr.task_allocation.util.EvalRecord;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.TraceStat;

import drcl.inet.protocol.aodv.struct.MultihopSnapshot;
import drcl.inet.protocol.aodv.struct.MultihopTrace;

public class Evaluator {

	private static final boolean EVALUATE_ONLY_PROPOSED = false;

	private static Class<? extends Heuristic> HEURISTIC_0 = HeuristicHadoop.class;
	private static Class<? extends Heuristic> HEURISTIC_1 = HeuristicPurlieus.class;
	private static Class<? extends Heuristic> HEURISTIC_2 = HeuristicMtaS.class;

	private int evalId = 1;

	private HashMap<String, JobResult> evaluatedJobResult_H0 = new HashMap<String, JobResult>();
	private HashMap<String, JobResult> evaluatedJobResult_H1 = new HashMap<String, JobResult>();
	private HashMap<String, JobResult> evaluatedJobResult_H2 = new HashMap<String, JobResult>();

	public void evaluate(String inputTraceId, HashMap<String, TraceStat> globalTraceId2Stat,
			HashMap<String, HistoryStat> inputTraceId2HistoryStat, JobProfile jp)
			throws NumberFormatException, IOException {
		long evaluateStart = System.currentTimeMillis();

		if (EVALUATE_ONLY_PROPOSED) {
			if (evalId == 2) {
				HEURISTIC_0 = null;
				HEURISTIC_1 = null;
				HEURISTIC_2 = null;
			}

		}

		// int cntSucceeded_H0 = 0;
		//
		// double sumSpeedup_H1 = 0;
		// int cntRelative_H1 = 0;
		// int cntSucceeded_H1 = 0;
		//
		// double sumSpeedup_H2 = 0;
		// int cntRelative_H2 = 0;
		// int cntSucceeded_H2 = 0;

		// String evalFileName = Date.CURRENT_DATE_TIME + "_evalId="
		// + String.format("%04d", evalId) + ".csv";
		// System.out.println(evalFileName);
		// Logger.println(evalFileName);
		// PrintWriter pwEval;
		// try {
		// pwEval = new PrintWriter("output" + File.separator + evalFileName);
		// pwEval.println(EvalRecord.getCsvTitle());
		//
		// pwEval.flush();
		// } catch (FileNotFoundException e) {
		// throw new RuntimeException(e);
		// }

		// float sumNormGcd = 0;
		// int numTraces = 0;
		File tracesDir = new File(Path.FAULTLOAD_DIR_PATH + File.separator + inputTraceId);
		ArrayList<File> traceFiles = new ArrayList<>();
		for (File traceFile : tracesDir.listFiles()) {
			if (traceFile.getName().contains("#")) {
				traceFiles.add(traceFile);
				continue;
			}
		}
		StatRecords statRecords = new StatRecords(EvalRecord.class);
		for (int i = 0; i < traceFiles.size(); i += 2) {
			// numTraces++;

			File traceFile = traceFiles.get(i);
			File traceZb2OrgNidFile = traceFiles.get(i + 1);
			String globalTraceId = traceFile.getName();
			// int idx1 = globalTraceId.indexOf("-");
			// globalTraceId = globalTraceId.substring(idx1 + 1);
			// int idx2 = globalTraceId.indexOf("-");
			// globalTraceId = globalTraceId.substring(idx2 + 1);
			System.out.println(globalTraceId);
			Logger.println(globalTraceId);

			TraceStat traceStat = globalTraceId2Stat.get(globalTraceId);
			int idxDelim = globalTraceId.indexOf("#");
			// String inputTraceId = globalTraceId.substring(0, idxDelim);
			String localTraceIdx = globalTraceId.substring(idxDelim + 1);
			HistoryStat historyStat = inputTraceId2HistoryStat.get(inputTraceId);

			BufferedReader br = new BufferedReader(new FileReader(traceFile));
			int initTimestamp = Integer.parseInt(br.readLine());

			MultihopSnapshot curSnapshot = new MultihopSnapshot(initTimestamp);
			String strOfInitValues = br.readLine();
			String[] initValueStrs = strOfInitValues.split(",");
			for (String initValueStr : initValueStrs) {
				String[] edgePairStrs = initValueStr.split("-");
				int nid1 = Integer.parseInt(edgePairStrs[0]);
				int nid2 = Integer.parseInt(edgePairStrs[1]);
				curSnapshot.addEdge(nid1, nid2);
			}
			MultihopTrace trace = new MultihopTrace(curSnapshot);

			String line;
			while ((line = br.readLine()) != null) {
				String[] tmp = line.split(" ");
				int curTimestamp = initTimestamp + Integer.parseInt(tmp[0]);
				curSnapshot = (MultihopSnapshot) curSnapshot.clone();
				curSnapshot.setTimestamp(curTimestamp);
				String type = tmp[1];

				if (type.equals("A") || type.equals("D")) {
					String strOfCurValues = tmp[2];
					String[] curValueStrs = strOfCurValues.split(",");
					for (String curValueStr : curValueStrs) {
						int idxOfDelim = curValueStr.indexOf("-");
						int nid1 = Integer.parseInt(curValueStr.substring(0, idxOfDelim));
						int nid2 = Integer.parseInt(curValueStr.substring(idxOfDelim + 1));
						if (type.equals("A")) {
							curSnapshot.addEdge(nid1, nid2);
						} else if (type.equals("D")) {
							curSnapshot.removeEdge(nid1, nid2);
						}
					}
					trace.add(curSnapshot);
					// Logger.println(curCC);
				}
			}
			br.close();

			byte[] zb2OrgNidBytes = FileUtils.readFileToByteArray(traceZb2OrgNidFile);
			@SuppressWarnings("unchecked")
			HashMap<Integer, Integer> zb2OrgNid = (HashMap<Integer, Integer>) ByteUtil.deserialize(zb2OrgNidBytes);

			JobResult jr_H0 = null;
			if (HEURISTIC_0 != null) {
				jr_H0 = evaluateTrace(trace, jp, HEURISTIC_0, inputTraceId, historyStat, zb2OrgNid, Params.NUM_TRIALS);

				if (EVALUATE_ONLY_PROPOSED) {
					evaluatedJobResult_H0.put(globalTraceId, jr_H0);
				}

			} else {
				jr_H0 = evaluatedJobResult_H0.get(globalTraceId);
				if (jr_H0 == null) {
					jr_H0 = JobResult.unavailable();
				}
			}

			// if (jr_H0.succeeded()) {
			// cntSucceeded_H0++;
			// }

			JobResult jr_H1 = null;
			if (HEURISTIC_1 != null) {
				jr_H1 = evaluateTrace(trace, jp, HEURISTIC_1, inputTraceId, historyStat, zb2OrgNid, Params.NUM_TRIALS);

				if (EVALUATE_ONLY_PROPOSED) {
					evaluatedJobResult_H1.put(globalTraceId, jr_H1);
				}

			} else {
				jr_H1 = evaluatedJobResult_H1.get(globalTraceId);
				if (jr_H1 == null) {
					jr_H1 = JobResult.unavailable();
				}
			}

			// if (jr_H1.succeeded()) {
			// cntSucceeded_H1++;
			// }

			JobResult jr_H2;
			if (HEURISTIC_2 != null) {
				jr_H2 = evaluateTrace(trace, jp, HEURISTIC_2, inputTraceId, historyStat, zb2OrgNid, Params.NUM_TRIALS);

				if (EVALUATE_ONLY_PROPOSED) {
					evaluatedJobResult_H2.put(globalTraceId, jr_H2);
				}

			} else {
				jr_H2 = evaluatedJobResult_H2.get(globalTraceId);
				if (jr_H2 == null) {
					jr_H2 = JobResult.unavailable();
				}
			}

			// if (jr_H2.succeeded()) {
			// cntSucceeded_H2++;
			// }

			// Relative Traffic
			// https://en.wikipedia.org/wiki/Speedup
			String relativeMapContention_H1_Str = "";
			String relativeRedContention_H1_Str = "";
			Double speedup_H1 = null;
			String speedup_H1_Str = "";
			if (jr_H0.succeeded() && jr_H1.succeeded()) {
				relativeMapContention_H1_Str = jr_H1.getMapContention() / jr_H0.getMapContention() + "";
				relativeRedContention_H1_Str = jr_H1.getRedContention() / jr_H0.getRedContention() + "";
				speedup_H1 = jr_H0.getSt() / jr_H1.getSt();
				speedup_H1_Str = speedup_H1 + "";

				// sumSpeedup_H1 += speedup_H1;
				// cntRelative_H1++;
			}
			String relativeMapContention_H2_Str = "";
			String relativeRedContention_H2_Str = "";
			Double speedup_H2 = null;
			String speedup_H2_Str = "";
			if (jr_H0.succeeded() && jr_H2.succeeded()) {
				relativeMapContention_H2_Str = jr_H2.getMapContention() / jr_H0.getMapContention() + "";
				relativeRedContention_H2_Str = jr_H2.getRedContention() / jr_H0.getRedContention() + "";
				speedup_H2 = jr_H0.getSt() / jr_H2.getSt();
				speedup_H2_Str = speedup_H2 + "";

				// sumSpeedup_H2 += speedup_H2;
				// cntRelative_H2++;
			}

			// Evaluation Record 출력
			// sumNormGcd += traceStat.normGcd;
			// pwEval.println(new EvalRecord( //
			// inputTraceId, //
			// localTraceIdx, //
			// traceStat, //
			// relativeMapContention_H1_Str, //
			// relativeMapContention_H2_Str, //
			// relativeRedContention_H1_Str, //
			// relativeRedContention_H2_Str, //
			// speedup_H1_Str, //
			// speedup_H2_Str, //
			// jr_H0, //
			// jr_H1, //
			// jr_H2 //
			// ).toCsvString());

			// CDF 결과 누적
			// cdfRecord_H0.cumulate(jr_H0.getCDFRecordMap());
			// cdfRecord_H1.cumulate(jr_H1.getCDFRecordMap());
			// cdfRecord_H2.cumulate(jr_H2.getCDFRecordMap());

			// 프로그램 중간 종료 상황에도 어느정도 기록이 되도록 flush
			// pwEval.flush();

			EvalRecord eval = new EvalRecord(//
					jr_H0, //
					jr_H1, //
					jr_H2 //
			);
			statRecords.add(eval);
		}
		// pwEval.close();

		Logger.println("evaluate() duration : " + (System.currentTimeMillis() - evaluateStart));
		Logger.println();
		System.out.println(jp.name + " on " + inputTraceId);
		statRecords.digest();

		// float avgNormGcd = sumNormGcd / numTraces;
		// String avgSpeedup_H1_Str = (cntRelative_H1 > 0) ? sumSpeedup_H1
		// / cntRelative_H1 + "" : "";
		// String avgSpeedup_H2_Str = (cntRelative_H2 > 0) ? sumSpeedup_H2
		// / cntRelative_H2 + "" : "";
		// float jcr_H0 = (float) cntSucceeded_H0 / numTraces;
		// float jcr_H1 = (float) cntSucceeded_H1 / numTraces;
		// float jcr_H2 = (float) cntSucceeded_H2 / numTraces;
		// pwSummary.println(new SummaryRecord( //
		// evalId, //
		// jp, //
		// avgNormGcd, //
		// avgSpeedup_H1_Str, //
		// avgSpeedup_H2_Str, //
		// jcr_H0, //
		// jcr_H1, //
		// jcr_H2 //
		// ).toCsvString());
		//
		// pwSummary.flush();

		evalId++;
	}

	private static JobResult evaluateTrace(MultihopTrace trace, JobProfile jp, Class<? extends Heuristic> clsHeuristic,
			String inputTraceId, HistoryStat historyStat, HashMap<Integer, Integer> zb2OrgNid, int numTrials) {
		StandardDeviation stdev = new StandardDeviation();

		ArrayList<JobResult> succeededJrs = new ArrayList<JobResult>();
		float sumSuccessTime = 0;
		JobResult failedJr = null;
		for (int j = 0; j < numTrials; j++) {
			JobResult curJr = new TraceRunner(trace, jp, clsHeuristic, inputTraceId, historyStat, zb2OrgNid).start();
			if (curJr.succeeded()) {
				succeededJrs.add(curJr);
				sumSuccessTime += curJr.getSt();

			} else {
				failedJr = curJr;
			}
		}

		JobResult jr = null;
		if (succeededJrs.size() >= numTrials / 2.0F) {
			ArrayList<Double> successTimes = new ArrayList<Double>();
			double avgSuccessTime = sumSuccessTime / succeededJrs.size();
			double dist = Double.MAX_VALUE;
			for (JobResult succeededJr : succeededJrs) {
				successTimes.add(succeededJr.getSt());
				if (Math.abs(avgSuccessTime - succeededJr.getSt()) < dist) {
					dist = Math.abs(avgSuccessTime - succeededJr.getSt());
					jr = succeededJr;
				}
			}

			double[] successTimesArr = successTimes.stream().mapToDouble(Double::doubleValue).toArray();

			double stdSuccessTime = stdev.evaluate(successTimesArr);
			jr.setStdSuccessTime(stdSuccessTime);

		} else {
			jr = failedJr;
		}
		return jr;
	}

}
