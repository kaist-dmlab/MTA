package com.mobilemr.task_allocation.heuristic;

import java.util.ArrayList;

import com.mobilemr.trace.struct.OrderedNumPair;

public class CostAndConn extends Number implements Comparable<CostAndConn> {
	private static final long serialVersionUID = -1311201952199964416L;

	private float cost = Float.MAX_VALUE;

	public float getCost() {
		return cost;
	}

	public void setCost(float cost) {
		if (Float.isNaN(cost)) {
			throw new IllegalArgumentException(cost + "");
		}
		if (cost > Float.MAX_VALUE) {
			this.cost = Float.MAX_VALUE;
		} else {
			this.cost = cost;
		}
	}

	private ArrayList<OrderedNumPair<Integer>> conns;

	public ArrayList<OrderedNumPair<Integer>> getConnections() {
		return conns;
	}

	public void setConnections(ArrayList<OrderedNumPair<Integer>> conns) {
		this.conns = conns;
	}

	@Override
	public double doubleValue() {
		return cost;
	}

	@Override
	public float floatValue() {
		return cost;
	}

	@Override
	public int intValue() {
		return (int) cost;
	}

	@Override
	public long longValue() {
		return (long) cost;
	}

	@Override
	public int compareTo(CostAndConn that) {
		return Float.compare(this.cost, that.cost);
	}

	@Override
	public String toString() {
		return Float.toString(cost);
	}

}
