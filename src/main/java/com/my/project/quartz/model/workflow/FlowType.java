package com.my.project.quartz.model.workflow;

public enum FlowType {
	/** 单个Job */
	SINGLE,
	/** 顺序执行的Job */
	SEQ,
	/** 所有Job都要执行完才能执行下一个Job */
	ALL,
	/** 只要有一个Job执行完就可以执行下一个Job */
	ANY
}
