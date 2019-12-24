package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.mobilemr.task_allocation.util.Common;
import com.mobilemr.trace.history.HistoryStat;
import com.mobilemr.trace.struct.CC;
import com.mobilemr.trace.struct.DummyEdge;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.UnorderedNumPair;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.algorithms.shortestpath.ShortestPathUtils;
import edu.uci.ics.jung.graph.util.Pair;

public class MultihopSnapshot extends CC {
	private static final long serialVersionUID = -8376926125754479767L;

	public MultihopSnapshot(int timestamp) {
		super(timestamp);
	}

	private BetweennessCentrality<Integer, DummyEdge> bc;
	private BronKerbosch<Integer, DummyEdge> bb;
	private DijkstraDistance<Integer, DummyEdge> dd;
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> srcNid2dstNid2NextNids;
	private HashMap<UnorderedNumPair<Integer>, Float> link2DeliveryRatio;

	public MultihopSnapshot markReady() {
		checkIntegrity();

		bc = new BetweennessCentrality<>(this);
		bb = new BronKerbosch<>(this);
		dd = new DijkstraDistance<>(this);
		srcNid2dstNid2NextNids = new HashMap<>();
		link2DeliveryRatio = new HashMap<>();

		DijkstraShortestPath<Integer, DummyEdge> sp = new DijkstraShortestPath<>(
				this);
		ArrayList<Integer> curNids = getNids();
		for (Integer srcNid : curNids) {
			for (Integer dstNid : curNids) {
				if (srcNid == dstNid) {
					continue;
				}
				ArrayList<Integer> shortestNextNids = new ArrayList<>();
				int minLength = Integer.MAX_VALUE;
				ArrayList<Integer> neighborNids = new ArrayList<>(
						getNeighbors(srcNid));
				for (Integer neighborNid : neighborNids) {
					List<DummyEdge> path = ShortestPathUtils.getPath(this, sp,
							neighborNid, dstNid);
					int curLength = path.size();
					if (curLength < minLength) {
						minLength = curLength;
						shortestNextNids.clear();
						shortestNextNids.add(neighborNid);
					} else if (curLength == minLength) {
						shortestNextNids.add(neighborNid);
					}
				}

				if (!srcNid2dstNid2NextNids.containsKey(srcNid)) {
					srcNid2dstNid2NextNids.put(srcNid, new HashMap<>());
				}
				HashMap<Integer, ArrayList<Integer>> dstNid2NextNids = srcNid2dstNid2NextNids
						.get(srcNid);
				dstNid2NextNids.put(dstNid, shortestNextNids);
			}
		}

		float sumEb = 0;
		for (DummyEdge e : getEdges()) {
			sumEb += getEdgeBetweennessCentrality(e);
		}
		for (DummyEdge e : getEdges()) {
			float deliveryRatio = 1 - getEdgeBetweennessCentrality(e) / sumEb;
			if (deliveryRatio < 0 || deliveryRatio > 1) {
				throw new IllegalStateException(deliveryRatio + "");
			}
			Pair<Integer> endpoints = getEndpoints(e);
			int nid1 = endpoints.getFirst();
			int nid2 = endpoints.getSecond();
			link2DeliveryRatio.put(new UnorderedNumPair<>(nid1, nid2),
					deliveryRatio);
		}
		return this;
	}

	public void checkIntegrity() {
		WeakComponentClusterer<Integer, DummyEdge> clusterer = new WeakComponentClusterer<Integer, DummyEdge>();
		Set<Set<Integer>> ccNodeSetSet = clusterer.apply(this);
		if (ccNodeSetSet.size() != 1) {
			throw new IllegalStateException(ccNodeSetSet.size() + "");
		}
	}

	public float getVertexBetweennessCentrality(Integer nid) {
		if (bc == null) {
			throw new IllegalStateException();
		}
		return bc.getVertexScore(nid).floatValue();
	}

	public float getEdgeBetweennessCentrality(DummyEdge e) {
		if (bc == null) {
			throw new IllegalStateException();
		}
		return bc.getEdgeScore(e).floatValue();
	}

	private UnorderedNumPair<Integer> key = new UnorderedNumPair<>(0, 0);

	public float getLinkDeliveryRatio(Integer nid1, Integer nid2) {
		synchronized (key) {
			key.reset(nid1, nid2);
			return link2DeliveryRatio.get(key);
		}
	}

	private int getNumNodesToSelect(ArrayList<Integer> nidsToSearch,
			int maxNodesToSelect) {
		if (getNodeCount() < maxNodesToSelect) {
			return -1;
		}

		int numSearchTargetNodes = nidsToSearch.size();
		return numSearchTargetNodes < maxNodesToSelect ? numSearchTargetNodes
				: maxNodesToSelect;
	}

	public ArrayList<Integer> getRandomNids(int maxNodesToSelect) {
		return getRandomNids(getNids(), maxNodesToSelect);
	}

	public ArrayList<Integer> getRandomNids(ArrayList<Integer> nidsToSearch,
			int maxNodesToSelect) {
		int numNodesToSelect = getNumNodesToSelect(nidsToSearch,
				maxNodesToSelect);
		if (numNodesToSelect == -1) {
			return getNids();
		}

		HashSet<Integer> randNidSet = new HashSet<Integer>();
		while (randNidSet.size() < numNodesToSelect) {
			int randNid = nidsToSearch
					.get(Common.R.nextInt(nidsToSearch.size()));
			randNidSet.add(randNid);
		}
		return new ArrayList<Integer>(randNidSet);
	}

	public ArrayList<Integer> getDataLocalNids(ArrayList<Integer> srcNids,
			int numNodesToSelect) {
		if (srcNids.size() == numNodesToSelect) {
			return srcNids;

		} else if (srcNids.size() > numNodesToSelect) {
			HashSet<Integer> randNidSet = new HashSet<Integer>();
			while (randNidSet.size() < numNodesToSelect) {
				int randNid = srcNids.get(Common.R.nextInt(srcNids.size()));
				randNidSet.add(randNid);
			}
			return new ArrayList<Integer>(randNidSet);

		} else {
			ArrayList<Integer> allNids = getNids();
			ArrayList<Integer> dstNids = new ArrayList<Integer>(srcNids);
			while (dstNids.size() < numNodesToSelect) {
				int randNid = allNids.get(Common.R.nextInt(allNids.size()));

				if (!dstNids.contains(randNid)) {
					Collection<Integer> neighborNids = getNeighbors(randNid);
					for (Integer neighborNid : neighborNids) {
						if (dstNids.contains(neighborNid)) {
							dstNids.add(randNid);
							break;
						}
					}
				}
			}
			return dstNids;
		}
	}

	public ArrayList<Integer> getMostCentralNids(int maxNodesToSelect) {
		return getMostCentralNids(getNids(), maxNodesToSelect);
	}

	public ArrayList<Integer> getMostCentralNids(
			ArrayList<Integer> nidsToSearch, int maxNodesToSelect) {
		int numNodesToSelect = getNumNodesToSelect(nidsToSearch,
				maxNodesToSelect);
		if (numNodesToSelect == -1) {
			return getNids();
		}

		ArrayList<GeneralPair<Integer, Float>> orderedNidAndCentrality = new ArrayList<GeneralPair<Integer, Float>>();
		for (Integer curNid : nidsToSearch) {
			float curCentrality = getVertexBetweennessCentrality(curNid);
			orderedNidAndCentrality.add(new GeneralPair<Integer, Float>(curNid,
					curCentrality));
		}
		Collections.sort(orderedNidAndCentrality,
				new Comparator<GeneralPair<Integer, Float>>() {
					@Override
					public int compare(GeneralPair<Integer, Float> lhs,
							GeneralPair<Integer, Float> rhs) {
						return -Float.compare(lhs.getSecond(), rhs.getSecond());
					}
				});

		ArrayList<Integer> mostCentralNids = new ArrayList<Integer>();
		for (int i = 0; mostCentralNids.size() < numNodesToSelect; i++) {
			int curNid = orderedNidAndCentrality.get(i).getFirst();
			mostCentralNids.add(curNid);
		}
		return mostCentralNids;
	}

	public ArrayList<Integer> getMostOuterNids(int maxNodesToSelect) {
		return getMostOuterNids(getNids(), maxNodesToSelect);
	}

	public synchronized ArrayList<Integer> getMostOuterNids(
			ArrayList<Integer> nidsToSearch, int maxNodesToSelect) {
		int numNodesToSelect = getNumNodesToSelect(nidsToSearch,
				maxNodesToSelect);
		if (numNodesToSelect == -1) {
			return getNids();
		}

		ArrayList<GeneralPair<Integer, Float>> orderedNidAndCentrality = new ArrayList<GeneralPair<Integer, Float>>();
		for (Integer curNid : nidsToSearch) {
			float curCentrality = getVertexBetweennessCentrality(curNid);
			orderedNidAndCentrality.add(new GeneralPair<Integer, Float>(curNid,
					curCentrality));
		}
		Collections.sort(orderedNidAndCentrality,
				new Comparator<GeneralPair<Integer, Float>>() {
					@Override
					public int compare(GeneralPair<Integer, Float> lhs,
							GeneralPair<Integer, Float> rhs) {
						return Float.compare(lhs.getSecond(), rhs.getSecond());
					}
				});

		ArrayList<Integer> mostOuterNids = new ArrayList<Integer>();
		for (int i = 0; mostOuterNids.size() < numNodesToSelect; i++) {
			int curNid = orderedNidAndCentrality.get(i).getFirst();
			mostOuterNids.add(curNid);
		}
		return mostOuterNids;
	}

	public ArrayList<Integer> getKClubNids(int maxNodesToSelect) {
		return getKClubNids(getNids(), maxNodesToSelect);
	}

	public ArrayList<Integer> getKClubNids(ArrayList<Integer> nidsToSearch,
			int maxNodesToSelect) {
		int numNodesToSelect = getNumNodesToSelect(nidsToSearch,
				maxNodesToSelect);
		if (numNodesToSelect == -1) {
			return getNids();
		}

		HashSet<Integer> maximalCliqueNidSet = bb.getMaximalClique();

		if (maximalCliqueNidSet.size() == numNodesToSelect) {
			return new ArrayList<>(maximalCliqueNidSet);

		} else if (maximalCliqueNidSet.size() > numNodesToSelect) {
			ArrayList<Integer> maximumCliqueNids = new ArrayList<>(
					maximalCliqueNidSet);
			HashSet<Integer> randNidSet = new HashSet<>();
			while (randNidSet.size() < numNodesToSelect) {
				int randNid = maximumCliqueNids.get(Common.R
						.nextInt(maximumCliqueNids.size()));
				randNidSet.add(randNid);
			}
			return new ArrayList<>(randNidSet);

		} else {
			ArrayList<Integer> maxKClubNids = new ArrayList<>(
					maximalCliqueNidSet);
			ArrayList<Integer> allNids = getNids();
			while (maxKClubNids.size() < numNodesToSelect) {
				int randNid = allNids.get(Common.R.nextInt(allNids.size()));
				if (!maxKClubNids.contains(randNid)) {
					Collection<Integer> neighborNids = getNeighbors(randNid);
					for (Integer neighborNid : neighborNids) {
						if (maxKClubNids.contains(neighborNid)) {
							maxKClubNids.add(randNid);
							break;
						}
					}
				}
			}
			return maxKClubNids;
		}
	}

	public ArrayList<Integer> getMaxRelNids(MultihopTrace trace,
			int realModeTime, HashMap<Integer, Integer> zb2OrgNid,
			HistoryStat historyStat, int maxNodesToSelect) {
		return getMaxRelNids(trace, realModeTime, zb2OrgNid, historyStat,
				getNids(), maxNodesToSelect);
	}

	public ArrayList<Integer> getMaxRelNids(MultihopTrace trace,
			int realModeTime, HashMap<Integer, Integer> zb2OrgNid,
			HistoryStat historyStat, ArrayList<Integer> nidsToSearch,
			int maxNodesToSelect) {
		int numNodesToSelect = getNumNodesToSelect(nidsToSearch,
				maxNodesToSelect);
		if (numNodesToSelect == -1) {
			return getNids();
		}

		ArrayList<GeneralPair<Integer, Float>> orderedNidAndFailureRate = new ArrayList<>();
		int initTimestamp = trace.getInitCC().getTimestamp();
		int realModeTimestampOfInputFile = initTimestamp + realModeTime;
		for (Integer dstZbNid : nidsToSearch) {
			int dstOrgNid = zb2OrgNid.get(dstZbNid);
			float curFailureRate = historyStat.getFailureRate(dstOrgNid,
					realModeTimestampOfInputFile);
			if (Float.isNaN(curFailureRate)) {
				throw new RuntimeException(dstZbNid + " " + dstOrgNid + " "
						+ realModeTimestampOfInputFile);
			}
			orderedNidAndFailureRate.add(new GeneralPair<>(dstZbNid,
					curFailureRate));
		}

		Collections.sort(orderedNidAndFailureRate,
				new Comparator<GeneralPair<Integer, Float>>() {
					@Override
					public int compare(GeneralPair<Integer, Float> lhs,
							GeneralPair<Integer, Float> rhs) {
						return Float.compare(lhs.getSecond(), rhs.getSecond());
					}
				});

		HashSet<Integer> maxRelNidSet = new HashSet<>();
		for (int i = 0; maxRelNidSet.size() < numNodesToSelect - 1; i++) {
			int curNid = orderedNidAndFailureRate.get(i).getFirst();
			maxRelNidSet.add(curNid);
		}
		while (maxRelNidSet.size() < numNodesToSelect) {
			int randNid = nidsToSearch
					.get(Common.R.nextInt(nidsToSearch.size()));
			if (!maxRelNidSet.contains(randNid)) {
				maxRelNidSet.add(randNid);
			}
		}
		return new ArrayList<>(maxRelNidSet);
	}

	@Override
	public synchronized double getDistance(Integer nid1, Integer nid2) {
		if (dd == null) {
			throw new IllegalStateException();
		}
		return dd.getDistance(nid1, nid2).doubleValue();
	}

	public ArrayList<Integer> getPathNids(Integer srcNid, Integer dstNid) {
		ArrayList<Integer> pathNids = new ArrayList<>();
		pathNids.add(srcNid);
		int firstNextNid = srcNid;

		while (firstNextNid != dstNid) {
			ArrayList<Integer> nextNids = getNextNids(firstNextNid, dstNid);
			firstNextNid = nextNids.get(0);
			pathNids.add(firstNextNid);
		}
		return pathNids;
	}

	public ArrayList<Integer> getNextNids(Integer srcNid, Integer dstNid) {
		if (srcNid == dstNid) {
			throw new IllegalArgumentException(srcNid + " " + dstNid);
		}
		HashMap<Integer, ArrayList<Integer>> dstNid2NextNids = srcNid2dstNid2NextNids
				.get(srcNid);
		if (dstNid2NextNids == null) {
			throw new IllegalStateException(srcNid2dstNid2NextNids.toString());
		}
		ArrayList<Integer> nextNids = dstNid2NextNids.get(dstNid);
		if (nextNids == null) {
			throw new IllegalStateException(srcNid2dstNid2NextNids.toString());
		}
		return nextNids;
	}

	@Deprecated
	public WeightedUndirectedSparseGraph toEbWeightedGraph() {
		WeightedUndirectedSparseGraph g = new WeightedUndirectedSparseGraph();
		for (DummyEdge e : getEdges()) {
			float eb = getEdgeBetweennessCentrality(e);
			Pair<Integer> endpoints = getEndpoints(e);
			int nid1 = endpoints.getFirst();
			int nid2 = endpoints.getSecond();
			g.addEdge(new WeightedEdge(eb), nid1, nid2);
		}

		Function<WeightedEdge, Float> edgeWeights = new Function<WeightedEdge, Float>() {
			@Override
			public Float apply(WeightedEdge e) {
				return e.getWeight();
			}
		};
		g.dd = new DijkstraDistance<>(g, edgeWeights);
		return g;
	}

	public WeightedUndirectedSparseGraph toEtxWeightedGraph() {
		WeightedUndirectedSparseGraph g = new WeightedUndirectedSparseGraph();
		for (DummyEdge e : getEdges()) {
			Pair<Integer> endpoints = getEndpoints(e);
			int nid1 = endpoints.getFirst();
			int nid2 = endpoints.getSecond();
			float etx = 1 / getLinkDeliveryRatio(nid1, nid2);
			g.addEdge(new WeightedEdge(etx), nid1, nid2);
		}

		Function<WeightedEdge, Float> edgeWeights = new Function<WeightedEdge, Float>() {
			@Override
			public Float apply(WeightedEdge e) {
				return e.getWeight();
			}
		};
		g.dd = new DijkstraDistance<>(g, edgeWeights);
		return g;
	}

	public ArrayList<Integer> getOuterNeighbors(ArrayList<Integer> targetNids) {
		HashSet<Integer> outerNbrSet = new HashSet<>();

		for (Integer targetNid : targetNids) {
			outerNbrSet.addAll(getNeighbors(targetNid));
		}

		outerNbrSet.removeAll(targetNids);
		return new ArrayList<>(outerNbrSet);
	}

}
