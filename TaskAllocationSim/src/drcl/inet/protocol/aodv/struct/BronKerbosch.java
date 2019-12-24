package drcl.inet.protocol.aodv.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class BronKerbosch<V extends Number, E> {
	// ~ Instance fields
	// --------------------------------------------------------

	private final UndirectedGraph<V, E> graph;

	private Collection<HashSet<V>> cliques;

	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Creates a new clique finder. Make sure this is a simple graph.
	 *
	 * @param graph
	 *            the graph in which cliques are to be found; graph must be
	 *            simple
	 */
	public BronKerbosch(UndirectedGraph<V, E> graph) {
		this.graph = graph;
	}

	// ~ Methods
	// ----------------------------------------------------------------

	public HashSet<V> getMaximalClique() {
		ArrayList<HashSet<V>> allMaximalCliques = getAllMaximalCliques();
		Collections.sort(allMaximalCliques, new Comparator<HashSet<V>>() {
			@Override
			public int compare(HashSet<V> o1, HashSet<V> o2) {
				double sumIds1 = 0;
				for (V e : o1) {
					sumIds1 += e.doubleValue();
				}
				double sumIds2 = 0;
				for (V e : o2) {
					sumIds2 += e.doubleValue();
				}
				return Double.compare(sumIds1, sumIds2);
			}
		});
		return allMaximalCliques.get(0);
	}

	/**
	 * Finds all maximal cliques of the graph. A clique is maximal if it is
	 * impossible to enlarge it by adding another vertex from the graph. Note
	 * that a maximal clique is not necessarily the biggest clique in the graph.
	 *
	 * @return Collection of cliques (each of which is represented as a Set of
	 *         vertices)
	 */
	public ArrayList<HashSet<V>> getAllMaximalCliques() {
		cliques = new ArrayList<HashSet<V>>();
		List<V> potential_clique = new ArrayList<V>();
		List<V> candidates = new ArrayList<V>();
		List<V> already_found = new ArrayList<V>();
		candidates.addAll(graph.getVertices());
		findCliques(potential_clique, candidates, already_found);
		return (ArrayList<HashSet<V>>) cliques;
	}

	/**
	 * Finds the biggest maximal cliques of the graph.
	 *
	 * @return Collection of cliques (each of which is represented as a Set of
	 *         vertices)
	 */
	public ArrayList<HashSet<V>> getAllMaximumCliques() {
		// first, find all cliques
		getAllMaximalCliques();

		int maximum = 0;
		ArrayList<HashSet<V>> biggest_cliques = new ArrayList<HashSet<V>>();
		for (HashSet<V> clique : cliques) {
			if (maximum < clique.size()) {
				maximum = clique.size();
			}
		}
		for (HashSet<V> clique : cliques) {
			if (maximum == clique.size()) {
				biggest_cliques.add(clique);
			}
		}
		return biggest_cliques;
	}

	public HashSet<V> getMaximumCliqueNidSet() {
		return getAllMaximumCliques().get(0);
	}

	private void findCliques(List<V> potential_clique, List<V> candidates,
			List<V> already_found) {
		List<V> candidates_array = new ArrayList<V>(candidates);
		if (!end(candidates, already_found)) {
			// for each candidate_node in candidates do
			for (V candidate : candidates_array) {
				List<V> new_candidates = new ArrayList<V>();
				List<V> new_already_found = new ArrayList<V>();

				// move candidate node to potential_clique
				potential_clique.add(candidate);
				candidates.remove(candidate);

				// create new_candidates by removing nodes in candidates not
				// connected to candidate node
				for (V new_candidate : candidates) {
					if (graph.isNeighbor(candidate, new_candidate)) {
						new_candidates.add(new_candidate);
					} // of if
				} // of for

				// create new_already_found by removing nodes in already_found
				// not connected to candidate node
				for (V new_found : already_found) {
					if (graph.isNeighbor(candidate, new_found)) {
						new_already_found.add(new_found);
					} // of if
				} // of for

				// if new_candidates and new_already_found are empty
				if (new_candidates.isEmpty() && new_already_found.isEmpty()) {
					// potential_clique is maximal_clique
					cliques.add(new HashSet<V>(potential_clique));
				} // of if
				else {
					// recursive call
					findCliques(potential_clique, new_candidates,
							new_already_found);
				} // of else

				// move candidate_node from potential_clique to already_found;
				already_found.add(candidate);
				potential_clique.remove(candidate);
			} // of for
		} // of if
	}

	private boolean end(List<V> candidates, List<V> already_found) {
		// if a node in already_found is connected to all nodes in candidates
		boolean end = false;
		int edgecounter;
		for (V found : already_found) {
			edgecounter = 0;
			for (V candidate : candidates) {
				if (graph.isNeighbor(found, candidate)) {
					edgecounter++;
				} // of if
			} // of for
			if (edgecounter == candidates.size()) {
				end = true;
			}
		} // of for
		return end;
	}

}
