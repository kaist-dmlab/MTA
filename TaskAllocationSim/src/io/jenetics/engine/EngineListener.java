package io.jenetics.engine;

import io.jenetics.Gene;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

public interface EngineListener<G extends Gene<?, G>, C extends Comparable<? super C>> {

	public void beforeFitnessEvaluation(ISeq<Phenotype<G, C>> population);

	public void afterFitnessEvaluation(ISeq<Phenotype<G, C>> population);

	public void afterEvolve(Phenotype<G, C> best);

}
