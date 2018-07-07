package com.my.project.quartz.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.my.project.quartz.job.QuartzJob;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobKey.*;
import static org.quartz.TriggerKey.*;
import static org.quartz.impl.matchers.GroupMatcher.*;

import java.util.Set;
import java.util.UUID;

import org.quartz.JobDetail;
import org.quartz.JobKey;

@Service
public class JobService {

	private static final String GROUP_NAME = "testGroup";
	@Autowired
	private Scheduler scheduler;

	public Set<JobKey> list() throws SchedulerException {
		return scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
	}

	public void add() throws SchedulerException {
		String id = UUID.randomUUID().toString();
		JobDetail job = newJob(QuartzJob.class)
				.withIdentity(id, GROUP_NAME)
				.build();
		Trigger trigger = newTrigger()
				.withIdentity(id, GROUP_NAME)
				.startNow()
				.withSchedule(cronSchedule("*/5 * * * * ?"))
				//.withSchedule(dailyAtHourAndMinute(10, 42)
				//	.inTimeZone(TimeZone.getTimeZone("America/Los_Angeles")))
				//.withSchedule(weeklyOnDayAndHourAndMinute(WEDNESDAY, 10, 42)
				//	.inTimeZone(TimeZone.getTimeZone("America/Los_Angeles")))
				.forJob(job)
				.build();
		scheduler.scheduleJob(job, trigger);
	}

	public void delete(String id) throws SchedulerException {
		JobKey jobKey = jobKey(id, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.deleteJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(id, GROUP_NAME);
		if(scheduler.checkExists(triggerKey)) {
			scheduler.unscheduleJob(triggerKey);
		}
	}

	public void pause(String id) throws SchedulerException {
		JobKey jobKey = jobKey(id, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.pauseJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(id, GROUP_NAME);
		if(scheduler.checkExists(triggerKey) && !TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey))) {
			scheduler.pauseTrigger(triggerKey);
		}
	}

	public void resume(String id) throws SchedulerException {
		JobKey jobKey = jobKey(id, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.resumeJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(id, GROUP_NAME);
		if(scheduler.checkExists(triggerKey) && TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey))) {
			scheduler.resumeTrigger(triggerKey);
		}
	}

	public void run(String id) throws SchedulerException {
		JobKey jobKey = jobKey(id, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.triggerJob(jobKey);
		}
	}

}
