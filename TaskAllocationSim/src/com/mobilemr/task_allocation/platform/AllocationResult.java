package com.mobilemr.task_allocation.platform;

import java.util.ArrayList;

import com.mobilemr.trace.struct.OrderedNumPair;

public class AllocationResult {

	public ArrayList<OrderedNumPair<Integer>> conns;
	public Double numEvals;
	public Double reliability;
	public Double contention;
	public Double optimalIdx1;
	public Double optimalIdx2;
	public Double clusterUtilization;

	public AllocationResult() {
		this.conns = new ArrayList<>();
	}

	public AllocationResult(ArrayList<OrderedNumPair<Integer>> conns,
			Double numEvals, Double reliability, Double contention,
			Double optimalIdx1, Double optimalIdx2, Double clusterUtilization) {
		this.conns = conns;
		this.numEvals = numEvals;
		this.reliability = reliability;
		this.contention = contention;
		this.optimalIdx1 = optimalIdx1;
		this.optimalIdx2 = optimalIdx2;
		this.clusterUtilization = clusterUtilization;
	}

}
