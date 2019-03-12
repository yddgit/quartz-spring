package com.my.project.quartz.model;

import java.util.List;

public class JobChain {

	private String name;
	private String cronExpression;
	private List<String> mutexChain;
	private List<List<String>> chainedJob;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCronExpression() {
		return cronExpression;
	}
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
	public List<String> getMutexChain() {
		return mutexChain;
	}
	public void setMutexChain(List<String> mutexChain) {
		this.mutexChain = mutexChain;
	}
	public List<List<String>> getChainedJob() {
		return chainedJob;
	}
	public void setChainedJob(List<List<String>> chainedJob) {
		this.chainedJob = chainedJob;
	}

}
