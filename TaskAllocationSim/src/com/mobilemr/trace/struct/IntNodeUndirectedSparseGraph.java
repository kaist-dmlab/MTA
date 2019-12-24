package com.mobilemr.trace.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

@SuppressWarnings("unchecked")
public class IntNodeUndirectedSparseGraph<E> extends
		UndirectedSparseGraph<Integer, E> implements Cloneable {
	private static final long serialVersionUID = -6419883458897698028L;

	@Override
	public Object clone() {
		try {
			// super 인 UndirectedSparseGraph 가 Cloneable 이 아니므로,
			// super 내부 vertices 와 edges 를 직접 복제해줘야 함
			Object cloned = super.clone();
			((IntNodeUndirectedSparseGraph<E>) cloned).vertices = new HashMap<Integer, Map<Integer, E>>();
			((IntNodeUndirectedSparseGraph<E>) cloned).edges = new HashMap<E, Pair<Integer>>();
			for (E thisEdge : super.getEdges()) {
				// E 를 복제할 경우, Haggle 같이 대규모 Trace 에서는 메모리 사용량이 증가하므로
				// addEdge 호출시 E 재활용
				Pair<Integer> thisEndpoints = this.getEndpoints(thisEdge);
				((IntNodeUndirectedSparseGraph<E>) cloned).addEdge(thisEdge,
						thisEndpoints.getFirst(), thisEndpoints.getSecond());
			}
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean removeVertex(Integer vertex) {
		throw new UnsupportedOperationException();
	}

	public boolean removeNode(Integer nid) {
		if (!containsVertex(nid))
			return false;

		// iterate over copy of incident edge collection
		for (E edge : new ArrayList<E>(vertices.get(nid).values())) {
			Pair<Integer> endpoints = getEndpoints(edge);
			removeEdge(endpoints.getFirst(), endpoints.getSecond());
		}

		vertices.remove(nid);
		return true;
	}

	public void removeEdge(Integer nid1, Integer nid2) {
		E e = findEdge(nid1, nid2);
		if (e != null) {
			removeEdge(e);

			// Edge 제거 결과, Vertex 에 연결된 Edge 가 없다면 Vertex 도 마저 제거
			if (degree(nid1) == 0) {
				super.removeVertex(nid1);
			}
			if (degree(nid2) == 0) {
				super.removeVertex(nid2);
			}
		}
	}

	public double getDistance(Integer nid1, Integer nid2) {
		throw new UnsupportedOperationException();
	}

	public HashSet<Integer> getNidSet() {
		return new HashSet<Integer>(super.getVertices());
	}

	public ArrayList<Integer> getNids() {
		return new ArrayList<Integer>(super.getVertices());
	}

	public HashSet<Edge> getEdgeSet() {
		// Pair 는 순서가 있으므로,
		// 순서가 없이 사용할 수 있는 Edge 로 변환
		HashSet<Edge> edgeSet = new HashSet<Edge>();
		for (Pair<Integer> pair : edges.values()) {
			edgeSet.add(new Edge(pair.getFirst(), pair.getSecond()));
		}
		return edgeSet;
	}

	public ArrayList<Edge> getEdgeList() {
		ArrayList<Edge> edgeList = new ArrayList<Edge>();
		for (Pair<Integer> pair : edges.values()) {
			edgeList.add(new Edge(pair.getFirst(), pair.getSecond()));
		}
		return edgeList;
	}

	public boolean overlaps(IntNodeUndirectedSparseGraph<E> that) {
		// 하나라도 Node 가 겹치면 Graph 가 겹친다고 볼 수 있음
		for (Integer thisNid : getNids()) {
			if (that.containsVertex(thisNid)) {
				return true;
			}
		}
		return false;
	}

	public int getNodeCount() {
		return getVertexCount();
	}

	public boolean isEmpty() {
		return getVertexCount() == 0;
	}

	public static <E> HashSet<Integer> getNodeIntersection(
			IntNodeUndirectedSparseGraph<E> lhs,
			IntNodeUndirectedSparseGraph<E> rhs) {
		HashSet<Integer> intersectionNidSet = new HashSet<Integer>();
		HashSet<Integer> rhsNidSet = rhs.getNidSet();
		for (Integer lhsNid : lhs.getNidSet()) {
			if (rhsNidSet.contains(lhsNid)) {
				intersectionNidSet.add(lhsNid);
			}
		}
		return intersectionNidSet;
	}

	public static <E> HashSet<Edge> getEdgeIntersection(
			IntNodeUndirectedSparseGraph<E> lhs,
			IntNodeUndirectedSparseGraph<E> rhs) {
		HashSet<Edge> intersectionEdgeSet = new HashSet<Edge>();
		HashSet<Edge> rhsEdgeSet = rhs.getEdgeSet();
		for (Edge lhsEdge : lhs.getEdgeSet()) {
			if (rhsEdgeSet.contains(lhsEdge)) {
				intersectionEdgeSet.add(lhsEdge);
			}
		}
		return intersectionEdgeSet;
	}

	public static <E> Pair<HashSet<Integer>> getNodeDiff(
			IntNodeUndirectedSparseGraph<E> prevGraph,
			IntNodeUndirectedSparseGraph<E> curGraph) {
		HashSet<Integer> arrivedNidSet = new HashSet<Integer>();
		HashSet<Integer> departedNidSet = new HashSet<Integer>();

		// curGraph에 있고, prevGraph에 없으면 Arrival
		for (Integer curNid : curGraph.getNidSet()) {
			if (!prevGraph.containsVertex(curNid)) {
				arrivedNidSet.add(curNid);
			}
		}

		// prevGraph에 있고, curGraph에 없으면 Departure
		for (Integer prevNid : prevGraph.getNidSet()) {
			if (!curGraph.containsVertex(prevNid)) {
				departedNidSet.add(prevNid);
			}
		}
		return new Pair<HashSet<Integer>>(arrivedNidSet, departedNidSet);
	}

	public static <E> Pair<HashSet<Edge>> getEdgeDiff(
			IntNodeUndirectedSparseGraph<E> prevGraph,
			IntNodeUndirectedSparseGraph<E> curGraph) {
		HashSet<Edge> arrivedEdgeSet = new HashSet<Edge>();
		HashSet<Edge> departedEdgeSet = new HashSet<Edge>();

		// curGraph에 있고, prevGraph에 없으면 Arrival
		for (Edge curEdge : curGraph.getEdgeSet()) {
			if (!prevGraph.getEdgeSet().contains(curEdge)) {
				arrivedEdgeSet.add(curEdge);
			}
		}

		// prevGraph에 있고, curGraph에 없으면 Departure
		for (Edge prevEdge : prevGraph.getEdgeSet()) {
			if (!curGraph.getEdgeSet().contains(prevEdge)) {
				departedEdgeSet.add(prevEdge);
			}
		}
		return new Pair<HashSet<Edge>>(arrivedEdgeSet, departedEdgeSet);
	}

	@Override
	public boolean equals(Object obj) {
		IntNodeUndirectedSparseGraph<E> that = (IntNodeUndirectedSparseGraph<E>) obj;

		// Node 와 Edge 개수가 같지 않으면 안됨
		if (!(this.getVertexCount() == that.getVertexCount() && this
				.getEdgeCount() == that.getEdgeCount())) {
			return false;
		}

		// Node 구성이 같은 지 검사
		// 하나라도 Node 구성이 다르면 실패
		HashSet<Integer> thisNidSet = this.getNidSet();
		HashSet<Integer> thatNidSet = that.getNidSet();
		if (!thisNidSet.equals(thatNidSet)) {
			return false;
		}

		// Edge 구성이 같은 지 검사
		// 하나라도 Edge 상태가 다르면 실패
		ArrayList<Integer> thisNids = this.getNids();
		for (int i = 0; i < thisNids.size(); i++) {
			int thisNid1 = thisNids.get(i);
			for (int j = i + 1; j < thisNids.size(); j++) {
				int thisNid2 = thisNids.get(j);
				boolean thisEdgeExists = this.findEdge(thisNid1, thisNid2) != null;
				boolean thatEdgeExists = that.findEdge(thisNid1, thisNid2) != null;
				// System.out.println(thisNid1 + " " + thisNid2 + " "
				// + thisEdgeExists + " " + thatEdgeExists);
				if (thisEdgeExists != thatEdgeExists) {
					return false;
				}
			}
		}

		// 전부 통과하면 같다고 볼 수 있음
		return true;
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
			ret += e.getFirst() + "-" + e.getSecond() + ",";
		}
		return ret;
	}

	public static void main(String[] args) throws CloneNotSupportedException {
		IntNodeUndirectedSparseGraph<DummyEdge> g1 = new IntNodeUndirectedSparseGraph<>();
		g1.addEdge(new DummyEdge(), 1, 2);
		g1.addEdge(new DummyEdge(), 1, 3);
		g1.addEdge(new DummyEdge(), 2, 4);

		IntNodeUndirectedSparseGraph<DummyEdge> g2 = new IntNodeUndirectedSparseGraph<>();
		g2.addEdge(new DummyEdge(), 2, 1);
		g2.addEdge(new DummyEdge(), 3, 1);
		System.out.println(g1.equals(g2));

		// 복제 후 같은지 검사
		IntNodeUndirectedSparseGraph<DummyEdge> g3 = (IntNodeUndirectedSparseGraph<DummyEdge>) g1
				.clone();
		IntNodeUndirectedSparseGraph<DummyEdge> g4 = (IntNodeUndirectedSparseGraph<DummyEdge>) g2
				.clone();
		System.out.println(g3.equals(g4));

		// Edge 변경 후 같은지 검사
		g4.removeEdge(3, 1);
		g4.addEdge(new DummyEdge(), 3, 2);
		System.out.println(g3.equals(g4));

		g1.removeNode(1);
		System.out.println(g1);
	}

}
