package com.my.project.quartz.exception;

import com.my.project.quartz.model.workflow.WorkFlow;
import com.my.project.quartz.util.JsonUtils;

public class WorkFlowException extends Exception {

	private static final long serialVersionUID = 4995145388587356446L;

	public WorkFlowException(WorkFlow workflow) {
		super(JsonUtils.toJsonString(workflow));
	}

	public WorkFlowException(String message, WorkFlow workflow) {
		super(message + ": " + JsonUtils.toJsonString(workflow));
	}
	
	public WorkFlowException(WorkFlow workflow, Throwable cause) {
		super(JsonUtils.toJsonString(workflow), cause);
	}

	public WorkFlowException(String message, WorkFlow workflow, Throwable cause) {
		super(message + ": " + JsonUtils.toJsonString(workflow), cause);
	}

}
