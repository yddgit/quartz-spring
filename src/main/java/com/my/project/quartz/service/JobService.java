package com.my.project.quartz.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.Trigger.TriggerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.my.project.quartz.job.QuartzJob;
import com.my.project.quartz.job.SimpleJob;
import com.my.project.quartz.exception.JobChainException;
import com.my.project.quartz.job.ChainedJob;
import com.my.project.quartz.listener.ChainStatusJobListener;
import com.my.project.quartz.listener.ChainStatusTriggerListener;
import com.my.project.quartz.listener.TriggerMisfiredListener;
import com.my.project.quartz.model.JobChain;
import com.my.project.quartz.model.JobStatus;
import com.my.project.quartz.model.SchedulerStatus;
import com.my.project.quartz.util.JsonUtils;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobKey.*;
import static org.quartz.TriggerKey.*;
import static org.quartz.impl.matchers.GroupMatcher.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;

@Service
public class JobService {

	private static final Logger logger = LoggerFactory.getLogger(JobService.class);

	private static final String GROUP_NAME = "myGroup";

	@Autowired
	private Scheduler scheduler;
	@Autowired
	private TriggerMisfiredListener triggerMisfiredListener;
	@Autowired
	private ChainStatusTriggerListener chainStatusTriggerListener;
	@Autowired
	private ChainStatusJobListener chainStatusJobListener;

	private ListenerManager listenerManager;

	public Scheduler getScheduler() {
		return scheduler;
	}

	public ListenerManager getListenerManager() {
		return listenerManager;
	}

	@PostConstruct
	private void start() throws SchedulerException {
		listenerManager = this.scheduler.getListenerManager();
		listenerManager.addTriggerListener(triggerMisfiredListener);
		listenerManager.addTriggerListener(chainStatusTriggerListener);
		listenerManager.addJobListener(chainStatusJobListener);
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
			runningJobList.add(key.toString());
		});
		status.setId(scheduler.getSchedulerInstanceId());
		status.setName(scheduler.getSchedulerName());
		status.setRunningJobs(runningJobList);
		status.setMetaData(scheduler.getMetaData());
		status.setJobListeners(
			listenerManager.getJobListeners().stream().map(j -> j.getName()).collect(Collectors.toList())
		);
		status.setTriggerListeners(
			listenerManager.getTriggerListeners().stream().map(t -> t.getName()).collect(Collectors.toList())
		);
		return status;
	}

	public List<JobStatus> list() throws SchedulerException {
		Set<JobKey> jobs =  scheduler.getJobKeys(jobGroupEquals(GROUP_NAME));
		List<JobStatus> list = new ArrayList<JobStatus>();
		JobStatus status = null;
		Trigger trigger = null;
		for(JobKey job : jobs) {
			status = new JobStatus();
			status.setName(job.getName());
			status.setGroup(job.getGroup());
			status.setRunning(jobIsRunning(job));
			status.setMutexJobs(chainStatusTriggerListener.getMutexJob(job).stream().map(JobKey::getName).collect(Collectors.toList()));
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(job);
			if(triggers != null && triggers.size() > 0 && triggers.get(0).getKey().getGroup().equals(GROUP_NAME)) {
				trigger = triggers.get(0);
				// check trigger
				status.setStartTime(trigger.getStartTime());
				status.setNextFireTime(trigger.getNextFireTime());
				status.setPreviousFireTime(trigger.getPreviousFireTime());
				status.setPriority(trigger.getPriority());				
			}
			list.add(status);
		}
		Collections.sort(list, (s1, s2) -> { return s1.getName().compareTo(s2.getName()); });
		return list;
	}

	private boolean jobIsRunning(JobKey job) throws SchedulerException {
		if(chainStatusTriggerListener.isRunning(job)) {
			return true;
		} else {			
			List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
			for(JobExecutionContext context : runningJobs){
				JobKey running = context.getJobDetail().getKey();
				if(job.equals(running)) {
					return true;
				}
			}
			return false;
		}
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
				.withMisfireHandlingInstructionDoNothing())
			//.withSchedule(dailyAtHourAndMinute(10, 42)
			//	.inTimeZone(TimeZone.getTimeZone("America/Los_Angeles")))
			//.withSchedule(weeklyOnDayAndHourAndMinute(WEDNESDAY, 10, 42)
			//	.inTimeZone(TimeZone.getTimeZone("America/Los_Angeles")))
			.forJob(job)
			.build();
		scheduler.scheduleJob(job, trigger);
	}

	public void delete(String name) throws SchedulerException {
		JobKey jobKey = jobKey(name, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.deleteJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(name, GROUP_NAME);
		if(scheduler.checkExists(triggerKey)) {
			scheduler.unscheduleJob(triggerKey);
		}
		chainStatusTriggerListener.removeMutexJob(jobKey);
	}

	public void pause(String name) throws SchedulerException {
		JobKey jobKey = jobKey(name, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.pauseJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(name, GROUP_NAME);
		if(scheduler.checkExists(triggerKey) && !TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey))) {
			scheduler.pauseTrigger(triggerKey);
		}
	}

	public void resume(String name) throws SchedulerException {
		JobKey jobKey = jobKey(name, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.resumeJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(name, GROUP_NAME);
		if(scheduler.checkExists(triggerKey) && TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey))) {
			scheduler.resumeTrigger(triggerKey);
		}
	}

	public void run(String name) throws SchedulerException {
		JobKey jobKey = jobKey(name, GROUP_NAME);
		if(scheduler.checkExists(jobKey)) {
			scheduler.triggerJob(jobKey);
		}
	}

	public void add(JobChain jobChain) throws SchedulerException, JobChainException {
		Assert.notNull(jobChain, "jobChain can not be null");
		Assert.notNull(jobChain.getName(), "jobChain name can not be null");
		Assert.notNull(jobChain.getChainedJob(), "chainedJob can not be null");

		String chainEndJob = checkJobChain(jobChain);
		JobKey jobKey = jobKey(jobChain.getName(), GROUP_NAME);
		JobDetail jobDetail = newJob(ChainedJob.class)
			.withIdentity(jobKey)
			.usingJobData(ChainedJob.CHAINED_JOBS, JsonUtils.toJsonString(jobChain.getChainedJob()))
			.usingJobData(ChainedJob.CHAIN_NAME, jobChain.getName())
			.usingJobData(ChainedJob.CHAIN_END_JOB, chainEndJob)
			.storeDurably(true)
			.build();

		List<String> mutexChain = jobChain.getMutexChain();
		if(mutexChain != null && mutexChain.size() > 0) {
			Set<JobKey> mutexJobs = new HashSet<JobKey>();
			for(String mutex : mutexChain) {
				mutexJobs.add(jobKey(mutex, GROUP_NAME));
			}
			chainStatusTriggerListener.addMutexJob(jobKey, mutexJobs);
		}

		if(StringUtils.isNotBlank(jobChain.getCronExpression())) {
			Trigger trigger = newTrigger()
				.withIdentity(jobChain.getName(), GROUP_NAME)
				.startNow()
				.withSchedule(cronSchedule(jobChain.getCronExpression())
					.withMisfireHandlingInstructionDoNothing())
				.forJob(jobDetail)
				.build();
			scheduler.scheduleJob(jobDetail, trigger);
		} else {
			scheduler.addJob(jobDetail, true);
		}
	}

	public JobKey add(String chainName, String chainEndJob, List<String> job, String listenerName) throws SchedulerException {
		String jobName = chainName + "." + StringUtils.join(job, ChainedJob.NAME_SEPARATOR);
		JobDetail jobDetail = newJob(SimpleJob.class)
			.withIdentity(jobName, GROUP_NAME)
			.usingJobData(ChainedJob.CHAINED_JOBS, JsonUtils.toJsonString(job))
			.usingJobData(ChainedJob.CHAIN_NAME, chainName)
			.usingJobData(ChainedJob.CHAIN_END_JOB, chainEndJob)
			.usingJobData(ChainedJob.LISTENER_NAME, listenerName)
			.storeDurably(true)
			.build();
		scheduler.addJob(jobDetail, true);
		return jobDetail.getKey();
	}

	private String checkJobChain(JobChain jobChain) throws JobChainException {
		List<List<String>> chainedJob = jobChain.getChainedJob();
		// check for null
		if(chainedJob == null) {
			throw new JobChainException("chainedJob can not be null");
		}
		// append end job
		List<String> chainEndJob = Arrays.asList(UUID.randomUUID().toString());
		chainedJob.add(chainEndJob);
		// check for duplicate names
		List<String> jobNames = new ArrayList<String>();
		String chainName = jobChain.getName();
		for(List<String> job : chainedJob) {
			if(job == null || job.size() == 0) {
				throw new JobChainException("chained job must have at least one", job);
			}
			jobNames.add(chainName + "." + StringUtils.join(job, ChainedJob.NAME_SEPARATOR));
		}
		logger.info("jobChain job list: " + JsonUtils.toJsonString(jobNames.subList(0, jobNames.size()-1), true));
		Set<String> duplicateName = checkDuplicateName(jobNames);
		if(duplicateName != null && duplicateName.size() > 0) {
			throw new JobChainException("chained job name duplicated", duplicateName);
		}
		// return end job name
		return jobNames.get(jobNames.size()-1);
	}

	private Set<String> checkDuplicateName(List<String> jobNames) {
		return jobNames.stream()
				.collect(Collectors.toMap(n -> n, n -> 1, Integer::sum))
				.entrySet().stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

}
