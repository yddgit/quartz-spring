package com.my.project.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;

import com.my.project.quartz.job.WorkFlowJob;

@Component
public class FlowJobListener extends JobListenerSupport {

    @Override
	public String getName() {
		return "FlowJobListener";
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		Boolean isFlow = context.getTrigger().getJobDataMap().getBooleanValueFromString(WorkFlowJob.IS_FLOW);
		JobKey jobKey = context.getJobDetail().getKey();
		if(isFlow) {
			try {
				FlowTriggerListener flowTriggerListener = (FlowTriggerListener)context.getScheduler().getListenerManager().getTriggerListener("FlowTriggerListener");
				flowTriggerListener.addRunningWorkFlow(context.getJobDetail().getKey());
			} catch (SchedulerException e) {
				getLog().error("add " + jobKey + " is running workflow list error", e);
			}
		}
	}

}
