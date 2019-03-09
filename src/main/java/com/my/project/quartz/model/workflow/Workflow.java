package com.my.project.quartz.model.workflow;

import java.util.ArrayList;
import java.util.List;

public class Workflow {

	private FlowType type;
	private String name;
	private List<Workflow> jobs = new ArrayList<Workflow>();

	public Workflow() { }

	public Workflow(FlowType type, String name) {
		this.type = type;
		this.name = name;
		this.jobs = new ArrayList<Workflow>();
	}

	public Workflow(FlowType type, String name, List<Workflow> jobs) {
		this.type = type;
		this.name = name;
		if(jobs == null) {
			this.jobs = new ArrayList<Workflow>();
		} else {
			this.jobs = jobs;
		}
	}

	public Workflow(FlowType type, String name, Workflow... workflows) {
		this.type = type;
		this.name = name;
		if(workflows != null && workflows.length > 0) {
			List<Workflow> jobs = new ArrayList<Workflow>();
			for(Workflow w : workflows) {
				jobs.add(w);
			}
			this.jobs = jobs;
		} else {
			this.jobs = new ArrayList<Workflow>();
		}
	}

	public void addWorkflow(Workflow workflow) {
		if(this.jobs == null) {
			this.jobs = new ArrayList<Workflow>();
		}
		this.jobs.add(workflow);
	}

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
	public List<Workflow> getJobs() {
		return jobs;
	}
	public void setJobs(List<Workflow> jobs) {
		this.jobs = jobs;
	}

}
