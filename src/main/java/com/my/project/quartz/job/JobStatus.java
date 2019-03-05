package com.my.project.quartz.job;

import java.util.Date;

import org.quartz.Trigger.TriggerState;

public class JobStatus {

	private String name;
	private String group;
	private Date startTime;
	private Date previousFireTime;
	private Date nextFireTime;
	private int priority;
	private TriggerState state;

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
	public TriggerState getState() {
		return state;
	}
	public void setState(TriggerState state) {
		this.state = state;
	}

}
