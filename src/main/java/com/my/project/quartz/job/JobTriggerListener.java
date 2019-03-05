package com.my.project.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobTriggerListener implements TriggerListener {

	private static final Logger logger = LoggerFactory.getLogger(JobTriggerListener.class);

	@Override
	public String getName() {
		return "jobTriggerListener";
	}

	@Override
	public void triggerFired(Trigger trigger, JobExecutionContext context) { }

	@Override
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		return false;
	}

	@Override
	public void triggerMisfired(Trigger trigger) {
		JobKey job = trigger.getJobKey();
		logger.warn("Job {} in Group {} is misfired on {}", job.getName(), job.getGroup(), trigger.getNextFireTime());
	}

	@Override
	public void triggerComplete(Trigger trigger, JobExecutionContext context,
			CompletedExecutionInstruction triggerInstructionCode) { }

}
