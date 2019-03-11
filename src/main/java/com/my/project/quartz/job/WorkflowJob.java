package com.my.project.quartz.job;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.my.project.quartz.exception.WorkflowException;
import com.my.project.quartz.listener.SeqJobListener;
import com.my.project.quartz.model.workflow.FlowType;
import com.my.project.quartz.model.workflow.Workflow;
import com.my.project.quartz.service.JobService;
import com.my.project.quartz.util.JsonUtils;

import static org.quartz.impl.matchers.KeyMatcher.*;
import static com.my.project.quartz.model.workflow.FlowType.*;

@Component
@Scope("prototype")
@DisallowConcurrentExecution
public class WorkflowJob extends QuartzJobBean {

	public static final String IS_FLOW = "_isFlow";
	public static final String FLOW_DEFINITION = "_flowDefinition";
	public static final String ROOT_FLOW = "_rootFlow";
	public static final String END_FLOW = "_endFlow";
	public static final String LISTENER_NAME = "_listenerName";

	private static final Logger logger = LoggerFactory.getLogger(QuartzJob.class);

	@Autowired
	private JobService jobService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String root = data.getString(ROOT_FLOW);
			String end = data.getString(END_FLOW);
			Boolean isFlow = data.getBooleanValueFromString(IS_FLOW);
			Workflow workflow = JsonUtils.jsonToObject(data.getString(FLOW_DEFINITION), Workflow.class);

			String jobName = context.getJobDetail().getKey().getName();
			if(isFlow) { jobName = jobName + "." + workflow.getName(); }
			executeWorkflow(root, end, jobName, workflow);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	private void executeWorkflow(String root, String end, String jobName, Workflow workflow) throws SchedulerException, WorkflowException {
		FlowType type = workflow.getType();
		if(type == null) {
			throw new WorkflowException("workflow type can not be null", workflow);
		}
		switch(type) {
			case SINGLE:
				executeSingleJob(root, end, jobName, workflow);
				break;
			case SEQ:
				executeSeqJob(root, end, jobName, workflow);
				break;
			case ALL:
				executeAllJob(root, end, jobName, workflow);
				break;
			case ANY:
				executeAnyJob(root, end, jobName, workflow);
				break;
			default:
				throw new WorkflowException("unknown workflow type [" + type + "]", workflow);
		}
	}

	private void executeSingleJob(String root, String end, String jobName, Workflow workflow) throws SchedulerException {
		if(end.equals(jobName)) { return; }

		logger.info("[" + SINGLE.name() + "]execute: " + jobName);
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			logger.error("sleep error", e);
		}
	}

	private void executeSeqJob(String root, String end, String jobName, Workflow workflow) throws SchedulerException, WorkflowException {

		logger.info("[" + SEQ.name() + "   ]execute: " + jobName);

		List<Workflow> jobs = workflow.getJobs();
		if(jobs == null || jobs.size() == 0) {
			jobService.delete(root);
			throw new WorkflowException(SEQ.name() + " workflow must have at least one child job", workflow);
		}

		String listenerName = SeqJobListener.NAME + "." + jobName + "." + UUID.randomUUID().toString();
		List<JobKey> jobKeys = new ArrayList<JobKey>();
		List<Matcher<JobKey>> matchers = new ArrayList<Matcher<JobKey>>();
		for(Workflow job : jobs) {
			JobKey jobKey = jobService.add(root, end, generateJobName(job, jobName), job, listenerName);
			jobKeys.add(jobKey);
			matchers.add(keyEquals(jobKey));
		}
		SeqJobListener seqJobListener = new SeqJobListener(listenerName, jobKeys);
		jobService.getListenerManager().addJobListener(seqJobListener, matchers);
		jobService.getScheduler().triggerJob(jobKeys.get(0));

	}

	private void executeAllJob(String root, String end, String parentJobName, Workflow workflow) throws SchedulerException {
		logger.info("[" + ALL.name() + "   ]execute: " + generateJobName(workflow, parentJobName));
	}

	private void executeAnyJob(String root, String end, String parentJobName, Workflow workflow) throws SchedulerException {
		logger.info("[" + ANY.name() + "   ]execute: " + generateJobName(workflow, parentJobName));
	}

	private String generateJobName(Workflow workflow, String parentJobName) {
		return (parentJobName == null ? "" : parentJobName + ".") + workflow.getName();
	}

}
