package com.mobilemr.task_allocation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.mobilemr.task_allocation.profile.JobProfile;
import com.mobilemr.task_allocation.util.ByteUtil;
import com.mobilemr.task_allocation.util.Date;
import com.mobilemr.task_allocation.util.Logger;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.TraceStat;

public class Simulator {

	private Evaluator evaluator = new Evaluator();

	public Simulator() {
		String curDateTime = Date.CURRENT_DATE_TIME;
		Logger.create(Path.LOGS_DIR_PATH, curDateTime);
	}

	private ArrayList<JobProfile> workloads = new ArrayList<>();

	public void addWorkloads(JobProfile... workloads) {
		for (JobProfile workload : workloads) {
			this.workloads.add(workload);
		}
	}

	private ArrayList<String> faultloads = new ArrayList<>();

	public void addFaultloads(String... faultloads) {
		for (String faultload : faultloads) {
			this.faultloads.add(faultload);
		}
	}

	public void run() throws IOException {
		for (JobProfile workload : workloads) {
			for (String faultload : faultloads) {
				run(workload, faultload);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void run(JobProfile jp, String inputTraceId) throws IOException {
		HashMap<String, HistoryStat> inputTraceId2HistoryStat = new HashMap<String, HistoryStat>();
		String traceId2StatSerFilePath = Path.FAULTLOAD_DIR_PATH
				+ File.separator + inputTraceId + File.separator
				+ Path.TRACE_ID_2_STAT_SER_FILE_NAME;
		File traceId2StatSerFile = new File(traceId2StatSerFilePath);
		String historyStatSerFilePath = Path.FAULTLOAD_DIR_PATH
				+ File.separator + inputTraceId + File.separator
				+ Path.HISTORY_STAT_SER_FILE_NAME;
		File historyStatSerFile = new File(historyStatSerFilePath);
		byte[] traceId2StatBytes = FileUtils
				.readFileToByteArray(traceId2StatSerFile);
		HashMap<String, TraceStat> traceId2Stat = (HashMap<String, TraceStat>) ByteUtil
				.deserialize(traceId2StatBytes);
		byte[] historyStatBytes = FileUtils
				.readFileToByteArray(historyStatSerFile);
		HistoryStat historyStat = (HistoryStat) ByteUtil
				.deserialize(historyStatBytes);
		inputTraceId2HistoryStat.put(inputTraceId, historyStat);

		try {
			evaluator.evaluate(inputTraceId, traceId2Stat,
					inputTraceId2HistoryStat, jp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
