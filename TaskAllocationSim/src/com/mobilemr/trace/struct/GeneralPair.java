package com.mobilemr.trace.struct;

import java.io.Serializable;

public class GeneralPair<F, S> implements Serializable {
	private static final long serialVersionUID = 7059411222565970624L;

	protected F first;
	protected S second;

	public GeneralPair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}

	@Override
	public boolean equals(Object obj) {
		@SuppressWarnings("unchecked")
		GeneralPair<F, S> that = (GeneralPair<F, S>) obj;
		return this.first.equals(that.first) && this.second.equals(that.second);
	}

	@Override
	public int hashCode() {
		final int prime = 31;

		int result = 1;
		result = prime * result + first.hashCode();
		result = prime * result + second.hashCode();

		return result;
	}

	@Override
	public String toString() {
		return first + " " + second;
	}

}
