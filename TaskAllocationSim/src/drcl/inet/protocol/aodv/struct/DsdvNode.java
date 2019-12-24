package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;

public class DsdvNode extends RoutableNode {

	public DsdvNode(int id) {
		super(id);
	}

	@Override
	public DsdvNode getNextDstNode(int dstNid) {
		@SuppressWarnings("unchecked")
		ArrayList<DsdvNode> nextDstNodes = (ArrayList<DsdvNode>) id2Node
				.get(dstNid);
		if (nextDstNodes == null) {
			return null;
		}

		float minThroughput = Float.MAX_VALUE;
		DsdvNode minNextDstNode = null;
		for (DsdvNode nextDstNode : nextDstNodes) {
			float curThroughput = nextDstNode.throughputInUnitTime;
			if (curThroughput < minThroughput) {
				minThroughput = curThroughput;
				minNextDstNode = nextDstNode;
			}
		}

		if (minNextDstNode == null) {
			throw new IllegalStateException(nextDstNodes.toString());
		}
		return minNextDstNode;
	}

}
