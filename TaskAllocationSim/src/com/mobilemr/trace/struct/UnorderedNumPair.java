package com.mobilemr.trace.struct;

import java.math.BigDecimal;
import java.util.HashSet;

public class UnorderedNumPair<N extends Number> extends OrderedNumPair<N> {
	private static final long serialVersionUID = -890941026960615527L;

	public UnorderedNumPair(N first, N second) {
		super(first, second);
		reset(first, second);
	}

	public void reset(N first, N second) {
		// 순서를 없애기 위해 오름차순 배치
		// Number 는 Comparable 이 아니므로 BigDecimal 이용
		// https://stackoverflow.com/questions/2683202/comparing-the-values-of-two-generic-numbers
		boolean firstSmaller = new BigDecimal(first.toString())
				.compareTo(new BigDecimal(second.toString())) < 0;
		this.first = firstSmaller ? first : second;
		this.second = firstSmaller ? second : first;
	}

	public static void main(String[] args) {
		UnorderedNumPair<Float> a = new UnorderedNumPair<Float>(0.5F, 1.5F);
		UnorderedNumPair<Float> b = new UnorderedNumPair<Float>(1.5F, 0.5F);

		HashSet<UnorderedNumPair<Float>> set = new HashSet<UnorderedNumPair<Float>>();
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
