package com.my.project.quartz.listener;

import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class TriggerMisfiredListener extends TriggerListenerSupport {

	public static final String NAME = "_TriggerMisfiredListener";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void triggerMisfired(Trigger trigger) {
		JobKey job = trigger.getJobKey();
		getLog().warn("Job {} in Group {} is misfired on {}", job.getName(), job.getGroup(), trigger.getNextFireTime());
	}

}