package com.mobilemr.task_allocation.util;

import org.apache.commons.math3.stat.IStatRecord;

import com.mobilemr.task_allocation.platform.JobResult;

public class EvalRecord implements IStatRecord {

	public Double st0;
	public Double st1;
	public Double st2;
	public Double ft0;
	public Double ft1;
	public Double ft2;

	public EvalRecord(JobResult jr_H0, JobResult jr_H1, JobResult jr_H2) {
		this.ft0 = jr_H0.getFt();
		this.ft1 = jr_H1.getFt();
		this.ft2 = jr_H2.getFt();
		this.st0 = jr_H0.getSt();
		this.st1 = jr_H1.getSt();
		this.st2 = jr_H2.getSt();
	}

}
