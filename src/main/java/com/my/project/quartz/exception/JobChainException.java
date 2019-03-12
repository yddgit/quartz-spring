package com.my.project.quartz.exception;

import java.util.Collection;

import com.my.project.quartz.util.JsonUtils;

public class JobChainException extends Exception {

	private static final long serialVersionUID = 4995145388587356446L;

	public JobChainException() {
		super();
	}

	public JobChainException(String message, Throwable cause) {
		super(message, cause);
	}

	public JobChainException(String message) {
		super(message);
	}

	public JobChainException(Throwable cause) {
		super(cause);
	}

	public JobChainException(String message, Collection<String> job, Throwable cause) {
		super(message + ": " + JsonUtils.toJsonString(job, true), cause);
	}

	public JobChainException(String message, Collection<String> job) {
		super(message + ": " + JsonUtils.toJsonString(job, true));
	}

}
