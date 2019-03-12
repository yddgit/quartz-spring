package com.my.project.quartz.listener;

import static org.quartz.JobKey.jobKey;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;

import com.my.project.quartz.job.ChainedJob;

@Component
public class ChainStatusJobListener extends JobListenerSupport {

	public static final String NAME = "_chainStatusJobListener";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		Boolean isChain = context.getJobDetail().getJobClass().equals(ChainedJob.class);
		JobKey jobKey = context.getJobDetail().getKey();
		if(isChain) {
			try {
				getLog().debug("add '" + jobKey + "' to running jobChain list");
				ChainStatusTriggerListener chainStatusTriggerListener = (ChainStatusTriggerListener)context.getScheduler().getListenerManager().getTriggerListener(ChainStatusTriggerListener.NAME);
				chainStatusTriggerListener.addRunningChain(context.getJobDetail().getKey());
			} catch (SchedulerException e) {
				getLog().debug("add '" + jobKey + "' to running jobChain list error", e);
			}
		}
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		JobDataMap data = context.getJobDetail().getJobDataMap();
		JobKey jobKey = context.getJobDetail().getKey();
		// jobChain group same with current job
		JobKey jobChain = jobKey(data.getString(ChainedJob.CHAIN_NAME), jobKey.getGroup());
		if(jobKey.getName().equals(data.getString(ChainedJob.CHAIN_END_JOB))) {
			try {
				getLog().debug("remove '" + jobChain + "' from running jobChain list");
				ChainStatusTriggerListener chainStatusTriggerListener = (ChainStatusTriggerListener)context.getScheduler().getListenerManager().getTriggerListener(ChainStatusTriggerListener.NAME);
				chainStatusTriggerListener.removeRunningChain(jobChain);
			} catch (SchedulerException e) {
				getLog().debug("remove '" + jobChain + "' from running jobChain list error", e);
			}
		}
	}

}
