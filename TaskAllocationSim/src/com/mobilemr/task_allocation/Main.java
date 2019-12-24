package com.mobilemr.task_allocation;

import java.io.IOException;

import com.mobilemr.task_allocation.profile.JobProfile;

public class Main {

	public static void main(String[] args) throws IOException {
		JobProfile naiveBayes = JobProfile.of("NAIVE_BAYES", 55);
		naiveBayes.addMap(0.04F, 0.3F);
		naiveBayes.addReduce(0.9F, 0.3F);

		JobProfile sift = JobProfile.of("SIFT", 20);
		sift.addMap(1.5F, 0.1F);
		sift.addReduce(1, 4.2F);

		JobProfile join = JobProfile.of("JOIN", 100);
		join.addMap(0.6F, 2);
		join.addReduce(2, 1);

		Simulator s = new Simulator();
		s.addWorkloads(join);
		s.addFaultloads("rollernet");
		s.run();
	}

}
