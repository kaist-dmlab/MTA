/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.internal.math;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static io.jenetics.internal.math.base.isMultiplicationSave;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import io.jenetics.internal.util.require;
import io.jenetics.util.RandomRegistry;

/**
 * Implementation of combinatorial helper methods.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz
 *         Wilhelmstötter</a>
 * @version 4.0
 * @since 4.0
 */
public final class comb {
	private comb() {
		require.noInstance();
	}

	/**
	 * Selects a random subset of size {@code k} from a set of size {@code n}.
	 *
	 * @see #subset(int, int[])
	 *
	 * @param n
	 *            the size of the set.
	 * @param k
	 *            the size of the subset.
	 * @throws IllegalArgumentException
	 *             if {@code n < k}, {@code k == 0} or if {@code n*k} will cause
	 *             an integer overflow.
	 * @return the subset array.
	 */
	public static int[] subset(final int n, final int k) {
		return subset(n, k, RandomRegistry.getRandom());
	}

	/**
	 * Selects a random subset of size {@code k} from a set of size {@code n}.
	 *
	 * @see #subset(int, int[], Random)
	 *
	 * @param n
	 *            the size of the set.
	 * @param k
	 *            the size of the subset.
	 * @param random
	 *            the random number generator used.
	 * @throws NullPointerException
	 *             if {@code random} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code n < k}, {@code k == 0} or if {@code n*k} will cause
	 *             an integer overflow.
	 * @return the subset array.
	 */
	private static HashSet<Integer> idxSet = new HashSet<Integer>();
	private static ArrayList<Integer> idxs = new ArrayList<Integer>();

	public static synchronized int[] subset(final int n, final int k,
			final Random random) {
		requireNonNull(random, "Random");
		if (k <= 0) {
			throw new IllegalArgumentException(format(
					"Subset size smaller or equal zero: %s", k));
		}
		if (n < k) {
			throw new IllegalArgumentException(format(
					"n smaller than k: %s < %s.", n, k));
		}

		final int[] sub = new int[k];

		// XXX subset() 버그
		// n=5, k=2 일 때 제대로 선택이 안되는 버그 발견해서 직접 구현
		// subset(n, sub, random);

		idxSet.clear();
		while (idxSet.size() < k) {
			idxSet.add(random.nextInt(n));
		}
		idxs.clear();
		idxs.addAll(idxSet);
		for (int i = 0; i < k; i++) {
			sub[i] = idxs.get(i);
		}
		return sub;
	}

	/**
	 * <p>
	 * Selects a random subset of size {@code sub.length} from a set of size
	 * {@code n}.
	 *
	 * @see #subset(int, int[], Random)
	 *
	 * @param n
	 *            the size of the set.
	 * @param sub
	 *            the sub set array.
	 * @throws NullPointerException
	 *             if {@code sub} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code n < sub.length}, {@code sub.length == 0} or
	 *             {@code n*sub.length} will cause an integer overflow.
	 */
	public static void subset(final int n, final int sub[]) {
		subset(n, sub, RandomRegistry.getRandom());
	}

	/**
	 * <p>
	 * Selects a random subset of size {@code a.length} from a set of size
	 * {@code n}. Implementation of the {@code RANKSB} algorithm described by
	 * <em>Albert Nijenhuis</em> and <em>Herbert Wilf</em> in <b>Combinatorial
	 * Algorithms for Computers and Calculators</b>
	 * </p>
	 * Reference:</a></em> Albert Nijenhuis, Herbert Wilf, Combinatorial
	 * Algorithms for Computers and Calculators, Second Edition, Academic Press,
	 * 1978, ISBN: 0-12-519260-6, LC: QA164.N54. Page: 42 </p>
	 *
	 * @param n
	 *            the size of the set.
	 * @param a
	 *            the a set array.
	 * @param random
	 *            the random number generator used.
	 * @return the a-set array for the given parameter
	 * @throws NullPointerException
	 *             if {@code a} or {@code random} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code n < a.length}, {@code a.length == 0} or
	 *             {@code n*a.length} will cause an integer overflow.
	 */
	public static int[] subset(final int n, final int a[], final Random random) {
		requireNonNull(random, "Random");
		requireNonNull(a, "Sub set array");

		final int k = a.length;
		checkSubSet(n, k);

		// Early return.
		if (a.length == n) {
			for (int i = 0; i < k; ++i)
				a[i] = i;
			return a;
		}

		// (A): Initialize a[i] to "zero" point for bin Ri.
		for (int i = 0; i < k; ++i) {
			a[i] = (i * n) / k;
		}

		// (B)
		int l = 0, x = 0;
		for (int c = 0; c < k; ++c) {
			do {
				// Choose random x;
				x = 1 + nextX(random, n - 1);

				// determine range Rl;
				l = (x * k - 1) / n;
			} while (a[l] >= x); // accept or reject.

			++a[l];
		}
		int s = k;

		// (C) Move a[i] of nonempty bins to the left.
		int m = 0, p = 0;
		for (int i = 0; i < k; ++i) {
			if (a[i] == (i * n) / k) {
				a[i] = 0;
			} else {
				++p;
				m = a[i];
				a[i] = 0;
				a[p - 1] = m;
			}
		}

		// (D) Determine l, set up space for Bl.
		int ds = 0;
		for (; p > 0; --p) {
			l = 1 + (a[p - 1] * k - 1) / n;
			ds = a[p - 1] - ((l - 1) * n) / k;
			a[p - 1] = 0;
			a[s - 1] = l;
			s -= ds;
		}

		// (E) If a[l] != 0, a new bin is to be processed.
		int r = 0, m0 = 0;
		for (int ll = 1; ll <= k; ++ll) {
			l = k + 1 - ll;

			if (a[l - 1] != 0) {
				r = l;
				m0 = 1 + ((a[l - 1] - 1) * n) / k;
				m = (a[l - 1] * n) / k - m0 + 1;
			}

			// (F) Choose a random x.
			x = m0 + nextX(random, m - 1);
			int i = l + 1;

			// (G) Check x against previously entered elements in bin;
			// increment x as it jumps over elements <= x.
			while (i <= r && x >= a[i - 1]) {
				++x;
				a[i - 2] = a[i - 1];
				++i;
			}

			a[i - 2] = x;
			--m;
		}

		return a;
	}

	public static void checkSubSet(final int n, final int k) {
		if (k <= 0) {
			throw new IllegalArgumentException(format(
					"Subset size smaller or equal zero: %s", k));
		}
		if (n < k) {
			throw new IllegalArgumentException(format(
					"n smaller than k: %s < %s.", n, k));
		}
		if (!isMultiplicationSave(n, k)) {
			throw new IllegalArgumentException(format(
					"n*sub.length > Integer.MAX_VALUE (%s*%s = %s > %s)", n, k,
					(long) n * (long) k, Integer.MAX_VALUE));
		}
	}

	private static int nextX(final Random random, final int m) {
		return m > 0 ? random.nextInt(m) : m - 1;
	}

}
