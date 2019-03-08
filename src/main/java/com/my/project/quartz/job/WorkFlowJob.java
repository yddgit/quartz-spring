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

import com.my.project.quartz.exception.WorkFlowException;
import com.my.project.quartz.listener.SeqJobListener;
import com.my.project.quartz.model.workflow.FlowType;
import com.my.project.quartz.model.workflow.WorkFlow;
import com.my.project.quartz.service.JobService;
import com.my.project.quartz.util.JsonUtils;

import static org.quartz.impl.matchers.KeyMatcher.*;
import static com.my.project.quartz.model.workflow.FlowType.*;

@Component
@Scope("prototype")
@DisallowConcurrentExecution
public class WorkFlowJob extends QuartzJobBean {

	public static final String IS_FLOW = "_isFlow";
	public static final String FLOW_DEFINITION = "_flowDefinition";
	public static final String ROOT_FLOW = "_rootFlow";
	public static final String LISTENER_NAME = "_listenerName";

	private static final Logger logger = LoggerFactory.getLogger(QuartzJob.class);

	@Autowired
	private JobService jobService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String root = data.getString(ROOT_FLOW);
			WorkFlow workflow = JsonUtils.jsonToObject(data.getString(FLOW_DEFINITION), WorkFlow.class);

			String jobName = context.getJobDetail().getKey().getName();
			String name = workflow.getName();
			if(SINGLE.equals(workflow.getType()) && jobName.endsWith("." + name)) {
				jobName = jobName.substring(0, jobName.lastIndexOf("." + name));
			}
			executeWorkFlow(root, jobName, workflow);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	private void executeWorkFlow(String root, String parentJobName, WorkFlow workflow) throws SchedulerException, WorkFlowException {
		FlowType type = workflow.getType();
		if(type == null) {
			throw new WorkFlowException("workflow type can not be null", workflow);
		}
		switch(type) {
			case SINGLE:
				executeSingleJob(root, parentJobName, workflow);
				break;
			case SEQ:
				executeSeqJob(root, parentJobName, workflow);
				break;
			case ALL:
				executeAllJob(root, parentJobName, workflow);
				break;
			case ANY:
				executeAnyJob(root, parentJobName, workflow);
				break;
			default:
				throw new WorkFlowException("unknown workflow type [" + type + "]", workflow);
		}
	}

	private void executeSingleJob(String root, String parentJobName, WorkFlow workflow) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "]execute: " + generateJobName(workflow, parentJobName));
		try {
			logger.info("sleep...60s");
			TimeUnit.SECONDS.sleep(60);
			logger.info("sleep...end");
		} catch (InterruptedException e) {
			logger.error("sleep error", e);
		}
	}

	private void executeSeqJob(String root, String parentJobName, WorkFlow workflow) throws SchedulerException, WorkFlowException {

		String jobName = generateJobName(workflow, parentJobName);
		logger.info("[" + SEQ.name() + "   ]execute: " + jobName);

		List<WorkFlow> jobs = workflow.getJobs();
		if(jobs == null || jobs.size() == 0) {
			jobService.delete(root);
			throw new WorkFlowException(SEQ.name() + " workflow must have at least one child job", workflow);
		}

		if(jobs.size() == 1) {
			executeWorkFlow(root, jobName, jobs.get(0));
		} else {
			String listenerName = SEQ.name() + "-" + UUID.randomUUID().toString();
			List<JobKey> jobKeys = new ArrayList<JobKey>();
			List<Matcher<JobKey>> matchers = new ArrayList<Matcher<JobKey>>();
			for(WorkFlow job : jobs) {
				JobKey jobKey = jobService.add(root, generateJobName(job, jobName), job, listenerName);
				jobKeys.add(jobKey);
				matchers.add(keyEquals(jobKey));
			}
			SeqJobListener seqJobListener = new SeqJobListener(listenerName, jobKeys);
			jobService.getListenerManager().addJobListener(seqJobListener, matchers);
			jobService.getScheduler().triggerJob(jobKeys.get(0));
		}
	}

	private void executeAllJob(String root, String parentJobName, WorkFlow workflow) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "   ]execute: " + generateJobName(workflow, parentJobName));
	}

	private void executeAnyJob(String root, String parentJobName, WorkFlow workflow) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "   ]execute: " + generateJobName(workflow, parentJobName));
	}

	private String generateJobName(WorkFlow workflow, String parentJobName) {
		return (parentJobName == null ? "" : parentJobName + ".") + workflow.getName();
	}

}
