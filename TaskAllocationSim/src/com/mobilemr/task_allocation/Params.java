package com.mobilemr.task_allocation;

import io.jenetics.Alterer;
import io.jenetics.Mutator;
import io.jenetics.RouletteWheelSelector;
import io.jenetics.Selector;
import io.jenetics.SinglePointCrossover;
import io.jenetics.TournamentSelector;

@SuppressWarnings({ "rawtypes" })
public class Params {

	// Genetic Algorithm Parameters
	public static Selector SURVIVORS_SELECTOR = new TournamentSelector<>(5);
	public static Selector OFFSPRING_SELECTOR = new RouletteWheelSelector();
	public static Alterer CROSSOVER = new SinglePointCrossover<>(0.16);
	public static Alterer MUTATOR = new Mutator<>(0.115);
	public static int MAX_GENERATIONS = 2000;
	public static int STEADY_GENERATIONS = 5;
	public static int POPULATION_SIZE = 200;

	// Environment Parameters
	public static float MAX_CLUSTER_UTILIZATION = 0.5F;
	public static int MAX_LINK_BANDWIDTH = 5;
	public static int NUM_TRIALS = 30;

}
