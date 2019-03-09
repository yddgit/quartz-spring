package com.my.project.quartz.listener;

import static org.quartz.JobKey.jobKey;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;

import com.my.project.quartz.job.WorkflowJob;

@Component
public class FlowJobListener extends JobListenerSupport {

	public static final String NAME = "_FlowJobListener";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		Boolean isFlow = context.getJobDetail().getJobDataMap().getBooleanValueFromString(WorkflowJob.IS_FLOW);
		JobKey jobKey = context.getJobDetail().getKey();
		if(isFlow) {
			try {
				getLog().info("add " + jobKey + " running workflow list");
				FlowTriggerListener flowTriggerListener = (FlowTriggerListener)context.getScheduler().getListenerManager().getTriggerListener(FlowTriggerListener.NAME);
				flowTriggerListener.addRunningWorkflow(context.getJobDetail().getKey());
			} catch (SchedulerException e) {
				getLog().error("add " + jobKey + " to running workflow list error", e);
			}
		}
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		JobDataMap data = context.getJobDetail().getJobDataMap();
		JobKey jobKey = context.getJobDetail().getKey();
		// root job group same with current job
		JobKey rootJobKey = jobKey(data.getString(WorkflowJob.ROOT_FLOW), jobKey.getGroup());
		if(jobKey.getName().equals(data.getString(WorkflowJob.END_FLOW))) {
			try {
				getLog().info("remove " + rootJobKey + " from running workflow list");
				FlowTriggerListener flowTriggerListener = (FlowTriggerListener)context.getScheduler().getListenerManager().getTriggerListener(FlowTriggerListener.NAME);
				flowTriggerListener.removeRunningWorkflow(rootJobKey);
			} catch (SchedulerException e) {
				getLog().error("remove " + rootJobKey + " from running workflow list error", e);
			}
		}
	}

}
