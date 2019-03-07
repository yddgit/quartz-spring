package com.my.project.quartz.model.workflow;

public class FlowConfig {

	private String name;
	private String cronExpression;
	private WorkFlow workflow;

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
	public WorkFlow getWorkflow() {
		return workflow;
	}
	public void setWorkflow(WorkFlow workflow) {
		this.workflow = workflow;
	}

}
