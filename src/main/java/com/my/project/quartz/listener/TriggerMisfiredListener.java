package com.my.project.quartz.listener;

import java.text.SimpleDateFormat;

import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class TriggerMisfiredListener extends TriggerListenerSupport {

	public static final String NAME = "_triggerMisfiredListener";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void triggerMisfired(Trigger trigger) {
		JobKey job = trigger.getJobKey();
		getLog().warn("job '{}' is misfired on {}", job, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(trigger.getNextFireTime()));
	}

}
