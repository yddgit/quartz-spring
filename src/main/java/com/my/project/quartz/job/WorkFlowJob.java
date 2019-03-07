package com.my.project.quartz.job;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.my.project.quartz.model.workflow.FlowKey;
import com.my.project.quartz.model.workflow.FlowType;
import com.my.project.quartz.model.workflow.WorkFlow;
import com.my.project.quartz.service.JobService;
import com.my.project.quartz.util.JsonUtils;

import static org.quartz.impl.matchers.KeyMatcher.*;
import static org.quartz.JobKey.*;
import static com.my.project.quartz.model.workflow.FlowType.*;

@Component
@Scope("prototype")
@DisallowConcurrentExecution
public class WorkFlowJob extends QuartzJobBean {

	public static final String FLOW_DEFINITION = "_flowDefinition";
	public static final String ROOT_FLOW_ID = "_rootFlowId";
	public static final String ROOT_FLOW_NAME = "_rootFlowName";
	public static final String ROOT_FLOW_GROUP = "_rootFlowGroup";
	public static final String FLOW_ID = "_flowId";
	public static final String FLOW_NAME = "_flowName";
	public static final String FLOW_GROUP = "_flowGroup";
	public static final String LISTENER_NAME = "_listenerName";

	private static final Logger logger = LoggerFactory.getLogger(QuartzJob.class);

	@Autowired
	private JobService jobService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String rootId = data.getString(ROOT_FLOW_ID);
			String rootName = data.getString(ROOT_FLOW_NAME);
			String rootGroup = data.getString(ROOT_FLOW_GROUP);
			FlowKey root = new FlowKey(rootId, rootName, rootGroup);
			String config = context.getJobDetail().getJobDataMap().getString(FLOW_DEFINITION);
			WorkFlow workflow = JsonUtils.jsonToObject(config, WorkFlow.class);
			String flowName = data.getString(FLOW_NAME);
			if(SINGLE.equals(workflow.getType())) {
				flowName = flowName.substring(0, flowName.lastIndexOf("." + workflow.getName()));
			}
			executeWorkFlow(root, workflow, flowName);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	private void executeWorkFlow(FlowKey root, WorkFlow workflow, String parentFlowName) throws SchedulerException, WorkFlowException {
		FlowType type = workflow.getType();
		if(type == null) {
			throw new WorkFlowException("workflow type can not be null", workflow);
		}
		switch(type) {
			case SINGLE:
				executeSingleJob(root, workflow, parentFlowName);
				break;
			case SEQ:
				executeSeqJob(root, workflow, parentFlowName);
				break;
			case ALL:
				executeAllJob(root, workflow, parentFlowName);
				break;
			case ANY:
				executeAnyJob(root, workflow, parentFlowName);
				break;
			default:
				throw new WorkFlowException("unknown workflow type [" + type + "]", workflow);
		}
	}

	private void executeSingleJob(FlowKey root, WorkFlow workflow, String parentFlowName) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "]execute: " + generateFlowName(workflow, parentFlowName));
	}

	private void executeSeqJob(FlowKey root, WorkFlow workflow, String parentFlowName) throws SchedulerException, WorkFlowException {
		String flowName = generateFlowName(workflow, parentFlowName);
		logger.info("[" + SEQ.name() + "   ]execute: " + flowName);
		List<WorkFlow> jobs = workflow.getJobs();
		if(jobs == null || jobs.size() == 0) {
			jobService.delete(jobKey(root.getId(), root.getGroup()));
			throw new WorkFlowException("SEQ workflow must have at least one child job", workflow);
		}
		if(jobs.size() == 1) {
			executeWorkFlow(root, jobs.get(0), flowName);
		} else {
			String listenerName = FlowType.SEQ.name() + "-" + UUID.randomUUID().toString();
			List<JobKey> jobKeys = new ArrayList<JobKey>();
			List<Matcher<JobKey>> matchers = new ArrayList<Matcher<JobKey>>();
			for(WorkFlow job : jobs) {
				JobKey jobKey = jobService.add(generateFlowName(job, flowName), job, root, listenerName);
				jobKeys.add(jobKey);
				matchers.add(keyEquals(jobKey));
			}
			SeqJobListener seqJobListener = new SeqJobListener(listenerName, jobKeys);
			jobService.getListenerManager().addJobListener(seqJobListener, matchers);
			jobService.getScheduler().triggerJob(jobKeys.get(0));
		}
	}

	private void executeAllJob(FlowKey root, WorkFlow workflow, String parentFlowName) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "   ]execute: " + generateFlowName(workflow, parentFlowName));
	}

	private void executeAnyJob(FlowKey root, WorkFlow workflow, String parentFlowName) throws SchedulerException {
		logger.info("[" + SINGLE.name() + "   ]execute: " + generateFlowName(workflow, parentFlowName));
	}

	private String generateFlowName(WorkFlow workflow, String parentFlowName) {
		return (parentFlowName == null ? "" : parentFlowName + ".") + workflow.getName();
	}

}
