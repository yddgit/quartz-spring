package com.my.project.quartz.model;

import java.util.List;

import org.quartz.SchedulerMetaData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SchedulerStatus {

	private String id;
	private String name;
	@JsonIgnoreProperties("summary")
	private SchedulerMetaData metaData;
	private List<String> runningJobs;
	private List<String> jobListeners;
	private List<String> triggerListeners;

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
	public List<String> getJobListeners() {
		return jobListeners;
	}
	public void setJobListeners(List<String> jobListeners) {
		this.jobListeners = jobListeners;
	}
	public List<String> getTriggerListeners() {
		return triggerListeners;
	}
	public void setTriggerListeners(List<String> triggerListeners) {
		this.triggerListeners = triggerListeners;
	}

}
