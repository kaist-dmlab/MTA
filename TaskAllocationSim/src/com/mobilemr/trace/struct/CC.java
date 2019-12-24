package com.mobilemr.trace.struct;

import java.util.ArrayList;
import java.util.HashSet;

import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Connected Component
 * 
 * @author Administrator
 */
public class CC extends IntNodeUndirectedSparseGraph<DummyEdge> {
	private static final long serialVersionUID = -9118271921242628648L;

	protected int timestamp;

	public CC(int timestamp) {
		this.timestamp = timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public boolean graphEquals(CC that) {
		// timestamp는 상관없이, Graph 가 같은지 검사
		return this.equals(that);
	}

	@Deprecated
	public CC cleanFromPrevScope(CC prevCC) {
		// 가입 이벤트는 상관없이 계속 이탈하는 이벤트만 사용하기 위해서
		// 이전 CC 내에 있는 Node 가 아닌 경우 지우기
		ArrayList<Integer> nids = getNids();
		for (int nid : nids) {
			if (!prevCC.containsVertex(nid)) {
				removeNode(nid);
			}
		}
		return this;
	}

	public void addEdge(int nid1, int nid2) {
		addEdge(new DummyEdge(), nid1, nid2, EdgeType.UNDIRECTED);
	}

	// public void zerobaseNids(HashMap<Integer, Integer> refOld2NewId) {
	// HashSet<Integer> newNids = new HashSet<Integer>();
	// this.nidSet.forEach(x -> newNids.add(refOld2NewId.get(x)));
	// this.nidSet = newNids;
	// HashSet<Edge> newEdgeSet = new HashSet<Edge>();
	// this.edgeSet.forEach(x -> newEdgeSet.add(new Edge(refOld2NewId
	// .get(x.nid1), refOld2NewId.get(x.nid2))));
	// this.edgeSet = newEdgeSet;
	// }

	public static HashSet<Integer> getNodeIntersection(CC lhs, CC rhs) {
		return IntNodeUndirectedSparseGraph.getNodeIntersection(lhs, rhs);
	}

	public static HashSet<Edge> getEdgeIntersection(CC lhs, CC rhs) {
		return IntNodeUndirectedSparseGraph.getEdgeIntersection(lhs, rhs);
	}

	public static Pair<HashSet<Integer>> getNodeDiff(CC prevCC, CC curCC) {
		return IntNodeUndirectedSparseGraph.getNodeDiff(prevCC, curCC);
	}

	public static Pair<HashSet<Edge>> getEdgeDiff(CC prevCC, CC curCC) {
		return IntNodeUndirectedSparseGraph.getEdgeDiff(prevCC, curCC);
	}

	public static int getGraphEditDistance(Pair<HashSet<Integer>> nodeDiff,
			Pair<HashSet<Edge>> edgeDiff) {
		// GED (Graph Edge Distance)
		// http://gedevo.mpi-inf.mpg.de/
		// = N_NODE_INSERT + N_NODE_DELETE + N_EDGE_INSERT + N_EDGE_DELETE

		// 가입 이벤트는 제외하고 이탈만 고려
		// = N_NODE_DELETE + N_EDGE_DELETE
		return nodeDiff.getSecond().size() + edgeDiff.getSecond().size();
	}

	public static float getGraphCentralityDistance(CC prevCC, CC curCC) {
		ClosenessCentrality<Integer, DummyEdge> prevCloseness = new ClosenessCentrality<Integer, DummyEdge>(
				prevCC);
		ClosenessCentrality<Integer, DummyEdge> curCloseness = new ClosenessCentrality<Integer, DummyEdge>(
				curCC);

		// // CC 간 노드 가입/이탈에 상관없이 Score 를 구하기 위해 Union 으로 합침
		// HashSet<Integer> unionNidSet = new HashSet<Integer>();
		// unionNidSet.addAll(prevCC.getNidSet());
		// unionNidSet.addAll(curCC.getNidSet());

		// 가입 이벤트는 제외하고 이탈만 고려하기 위해 prevCC 의 노드 사용
		float sumGraphCentralityDistance = 0;
		for (int prevNid : prevCC.getNidSet()) {
			float prevScore = (float) (prevCC.containsVertex(prevNid) ? prevCloseness
					.getVertexScore(prevNid) : 0);
			float curScore = (float) (curCC.containsVertex(prevNid) ? curCloseness
					.getVertexScore(prevNid) : 0);
			sumGraphCentralityDistance += Math.abs(prevScore - curScore);
		}
		return sumGraphCentralityDistance;
	}

	@Override
	public boolean equals(Object obj) {
		CC that = (CC) obj;
		return this.timestamp == that.timestamp && super.equals(that);
	}

	@Override
	public String toString() {
		String ret = "";
		ret += "t: " + timestamp;
		ret += "\n";
		ret += "graph: " + super.toString();
		return ret;
	}

	public static void main(String[] args) {
		CC cc1 = new CC(0);
		cc1.addEdge(0, 1);
		cc1.addEdge(1, 2);
		cc1.addEdge(2, 3);

		CC cc2 = new CC(0);
		cc2.addEdge(1, 2);
		cc2.addEdge(2, 3);
		cc2.addEdge(3, 4);

		// diff 검사
		System.out.println(CC.getNodeDiff(cc1, cc2));
		System.out.println(CC.getEdgeDiff(cc1, cc2));
		System.out.println(CC.getEdgeIntersection(cc1, cc2));

		// Clone equals 검사
		CC cc1Cloned = (CC) cc1.clone();
		CC cc2Cloned = (CC) cc2.clone();
		System.out.println(cc1.equals(cc1Cloned));
		System.out.println(cc2.equals(cc2Cloned));

		// Clone 객체 공유 검사
		cc1Cloned.removeEdge(0, 1);
		System.out.println(cc1.equals(cc1Cloned));
	}

}
