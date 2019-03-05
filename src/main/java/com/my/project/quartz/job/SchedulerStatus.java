package com.my.project.quartz.job;

import java.util.List;

import org.quartz.SchedulerMetaData;

public class SchedulerStatus {

	private String id;
	private String name;
	private SchedulerMetaData metaData;
	private List<String> runningJobs;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public SchedulerMetaData getMetaData() {
		return metaData;
	}
	public void setMetaData(SchedulerMetaData metaData) {
		this.metaData = metaData;
	}
	public List<String> getRunningJobs() {
		return runningJobs;
	}
	public void setRunningJobs(List<String> runningJobs) {
		this.runningJobs = runningJobs;
	}

}
