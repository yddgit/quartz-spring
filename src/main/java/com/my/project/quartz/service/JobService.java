package com.my.project.quartz.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.my.project.quartz.job.JobStatus;
import com.my.project.quartz.job.QuartzJob;
import com.my.project.quartz.job.SchedulerStatus;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobKey.*;
import static org.quartz.TriggerKey.*;
import static org.quartz.impl.matchers.GroupMatcher.*;
import static org.quartz.impl.matchers.EverythingMatcher.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

@Service
public class JobService {

	private static final String GROUP_NAME = "testGroup";
	@Autowired
	private Scheduler scheduler;
	@Autowired
	private TriggerListener triggerListener;

	@PostConstruct
	private void start() throws SchedulerException {
		this.scheduler.getListenerManager().addTriggerListener(triggerListener, allTriggers());
	}

	@PreDestroy
	private void stop() throws SchedulerException {
		this.scheduler.shutdown();
	}

	public SchedulerStatus info() throws SchedulerException {
		SchedulerStatus status = new SchedulerStatus();
		List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
		List<String> runningJobList = new ArrayList<String>();
		runningJobs.forEach(job -> {
			JobKey key = job.getJobDetail().getKey();
			runningJobList.add(key.getName() + "-" + key.getGroup());
		});
		status.setId(scheduler.getSchedulerInstanceId());
		status.setName(scheduler.getSchedulerName());
		status.setRunningJobs(runningJobList);
		status.setMetaData(scheduler.getMetaData());
		return status;
	}

	public List<JobStatus> list() throws SchedulerException {
		Set<JobKey> jobs =  scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
		List<JobStatus> list = new ArrayList<JobStatus>();
		JobStatus job = null;
		TriggerKey triggerKey = null;
		Trigger trigger = null;
		for(JobKey j : jobs) {
			job = new JobStatus();
			job.setName(j.getName());
			job.setGroup(j.getGroup());
			triggerKey = triggerKey(job.getName(), GROUP_NAME);
			trigger = scheduler.getTrigger(triggerKey);
			job.setStartTime(trigger.getStartTime());
			job.setNextFireTime(trigger.getNextFireTime());
			job.setPreviousFireTime(trigger.getPreviousFireTime());
			job.setPriority(trigger.getPriority());
			job.setState(scheduler.getTriggerState(triggerKey));
			list.add(job);
		}
		return list;
	}

	public void add() throws SchedulerException {
		String id = "testJob";//UUID.randomUUID().toString();
		JobDetail job = newJob(QuartzJob.class)
			.withIdentity(id, GROUP_NAME)
			.usingJobData("jobName", id)
			.build();
		Trigger trigger = newTrigger()
			.withIdentity(id, GROUP_NAME)
			.startNow()
			.withSchedule(cronSchedule("*/5 * * * * ?")
				// If the Trigger misfires, the CronTrigger wants to have it's next-fire-time
				// updated to the next time in the schedule after the current time 
				.withMisfireHandlingInstructionDoNothing()
			)
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
