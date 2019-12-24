package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import com.mobilemr.trace.struct.Edge;
import com.mobilemr.trace.struct.GeneralPair;
import com.mobilemr.trace.struct.IntNodeUndirectedSparseGraph;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;

public class WeightedUndirectedSparseGraph extends
		IntNodeUndirectedSparseGraph<WeightedEdge> {
	private static final long serialVersionUID = -4114645534213943221L;

	public DijkstraDistance<Integer, WeightedEdge> dd;

	@Override
	public synchronized double getDistance(Integer nid1, Integer nid2) {
		if (dd == null) {
			throw new IllegalStateException();
		}
		return dd.getDistance(nid1, nid2).doubleValue();
	}

	public ArrayList<Integer> getMinDistNids(ArrayList<Integer> srcNids,
			int maxNodesToSelect) {
		ArrayList<GeneralPair<Integer, Double>> orderedNidAndDistance = new ArrayList<>();
		HashSet<Integer> nidSet = getNidSet();
		for (Integer dstNid : nidSet) {
			double sumDistance = 0;
			for (Integer srcNid : srcNids) {
				sumDistance += getDistance(srcNid, dstNid);
			}
			orderedNidAndDistance.add(new GeneralPair<>(dstNid, sumDistance));
		}

		Collections.sort(orderedNidAndDistance,
				new Comparator<GeneralPair<Integer, Double>>() {
					@Override
					public int compare(GeneralPair<Integer, Double> lhs,
							GeneralPair<Integer, Double> rhs) {
						return Double.compare(lhs.getSecond(), rhs.getSecond());
					}
				});

		ArrayList<Integer> minDistShuffleNids = new ArrayList<Integer>();
		for (int i = 0; minDistShuffleNids.size() < maxNodesToSelect; i++) {
			int curNid = orderedNidAndDistance.get(i).getFirst();
			minDistShuffleNids.add(curNid);
		}
		return minDistShuffleNids;
	}

	@Override
	public String toString() {
		String ret = "";
		ret += "nodes: ";
		for (Integer nid : getNids()) {
			ret += nid + ",";
		}
		ret += "\n";
		ret += "edges: ";
		for (Edge e : getEdgeList()) {
			int nid1 = e.getFirst();
			int nid2 = e.getSecond();
			float weight = findEdge(nid1, nid2).getWeight();
			ret += nid1 + "-" + nid2 + "(" + weight + "),";
		}
		return ret;
	}

}
