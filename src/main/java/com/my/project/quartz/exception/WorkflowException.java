package com.my.project.quartz.exception;

import com.my.project.quartz.model.workflow.Workflow;
import com.my.project.quartz.util.JsonUtils;

public class WorkflowException extends Exception {

	private static final long serialVersionUID = 4995145388587356446L;

	public WorkflowException() {
		super();
	}

	public WorkflowException(String message, Throwable cause) {
		super(message, cause);
	}

	public WorkflowException(String message) {
		super(message);
	}

	public WorkflowException(Throwable cause) {
		super(cause);
	}

	public WorkflowException(Workflow workflow) {
		super(JsonUtils.toJsonString(workflow));
	}

	public WorkflowException(String message, Workflow workflow) {
		super(message + ": " + JsonUtils.toJsonString(workflow));
	}
	
	public WorkflowException(Workflow workflow, Throwable cause) {
		super(JsonUtils.toJsonString(workflow), cause);
	}

	public WorkflowException(String message, Workflow workflow, Throwable cause) {
		super(message + ": " + JsonUtils.toJsonString(workflow), cause);
	}

}
