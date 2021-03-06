package com.my.project.quartz.model;

import java.util.Date;
import java.util.List;

public class JobStatus {

	private String name;
	private String group;
	private Date startTime;
	private Date previousFireTime;
	private Date nextFireTime;
	private int priority;
	private boolean running;
	private List<String> mutexJobs;
	private List<?> chainedJobs;
	private String cronExpression;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getPreviousFireTime() {
		return previousFireTime;
	}
	public void setPreviousFireTime(Date previousFireTime) {
		this.previousFireTime = previousFireTime;
	}
	public Date getNextFireTime() {
		return nextFireTime;
	}
	public void setNextFireTime(Date nextFireTime) {
		this.nextFireTime = nextFireTime;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	public List<String> getMutexJobs() {
		return mutexJobs;
	}
	public void setMutexJobs(List<String> mutexJobs) {
		this.mutexJobs = mutexJobs;
	}
	public List<?> getChainedJobs() {
		return chainedJobs;
	}
	public void setChainedJobs(List<?> chainedJobs) {
		this.chainedJobs = chainedJobs;
	}
	public String getCronExpression() {
		return cronExpression;
	}
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

}
