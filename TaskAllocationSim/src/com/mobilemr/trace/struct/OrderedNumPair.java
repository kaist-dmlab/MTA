package com.mobilemr.trace.struct;

import java.math.BigDecimal;
import java.util.HashSet;

public class OrderedNumPair<N extends Number> extends GeneralPair<N, N>
		implements Comparable<OrderedNumPair<N>> {
	private static final long serialVersionUID = -890941026960615527L;

	public OrderedNumPair(N first, N second) {
		// first 와 second 의 입력 순서가 중요
		super(first, second);
	}

	@Override
	public int compareTo(OrderedNumPair<N> that) {
		int result = new BigDecimal(this.first.toString())
				.compareTo(new BigDecimal(that.first.toString()));
		return result == 0 ? new BigDecimal(this.second.toString())
				.compareTo(new BigDecimal(that.second.toString())) : result;
	}

	public static void main(String[] args) {
		OrderedNumPair<Float> a = new OrderedNumPair<Float>(0.5F, 1.5F);
		OrderedNumPair<Float> b = new OrderedNumPair<Float>(1.5F, 0.5F);

		HashSet<OrderedNumPair<Float>> set = new HashSet<OrderedNumPair<Float>>();
		set.add(a);

		System.out.println(a == b);
		System.out.println(a.equals(b));
		System.out.println(a.hashCode());
		System.out.println(b.hashCode());
		System.out.println(set.contains(a));
		System.out.println(set.contains(b));
		System.out.println(a.compareTo(b));
	}

}
