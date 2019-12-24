package drcl.inet.protocol.aodv.struct;

import com.mobilemr.trace.struct.CC;
import com.mobilemr.trace.struct.Trace;

public class MultihopTrace extends Trace {

	public MultihopTrace(MultihopSnapshot snapshot) {
		super(snapshot.markReady());
	}

	@Override
	public void add(CC snapshot) {
		super.add(((MultihopSnapshot) snapshot).markReady());
	}

	public MultihopSnapshot getClosestSnapshot(int targetTime) {
		MultihopSnapshot closestSnapshot = (MultihopSnapshot) ccs.get(0);
		int initTimestamp = ccs.get(0).getTimestamp();
		for (int i = 1; i < ccs.size()
				&& ccs.get(i).getTimestamp() - initTimestamp <= targetTime; i++) {
			closestSnapshot = (MultihopSnapshot) ccs.get(i);
		}
		return closestSnapshot;
	}

	public synchronized RoutableMultihopSnapshot getClosestRoutableMultihopSnapshot(
			int targetTime) {
		MultihopSnapshot closestSnapshot = getClosestSnapshot(targetTime);
		return new DsdvMultihopSnapshot(closestSnapshot);
	}

}
