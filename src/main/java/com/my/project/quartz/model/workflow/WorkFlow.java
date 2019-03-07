package com.my.project.quartz.model.workflow;

import java.util.ArrayList;
import java.util.List;

public class WorkFlow {

	private FlowType type;
	private String name;
	private List<WorkFlow> jobs = new ArrayList<WorkFlow>();

	public FlowType getType() {
		return type;
	}
	public void setType(FlowType type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<WorkFlow> getJobs() {
		return jobs;
	}
	public void setJobs(List<WorkFlow> jobs) {
		this.jobs = jobs;
	}

}
