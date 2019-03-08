package com.my.project.quartz.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.my.project.quartz.job.QuartzJob;
import com.my.project.quartz.job.WorkFlowJob;
import com.my.project.quartz.listener.FlowJobListener;
import com.my.project.quartz.listener.FlowTriggerListener;
import com.my.project.quartz.listener.TriggerMisfiredListener;
import com.my.project.quartz.model.JobStatus;
import com.my.project.quartz.model.SchedulerStatus;
import com.my.project.quartz.model.workflow.FlowConfig;
import com.my.project.quartz.model.workflow.WorkFlow;
import com.my.project.quartz.util.JsonUtils;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobKey.*;
import static org.quartz.TriggerKey.*;
import static org.quartz.impl.matchers.GroupMatcher.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;

@Service
public class JobService {

	private static final String GROUP_NAME = "myGroup";

	@Autowired
	private Scheduler scheduler;
	@Autowired
	private TriggerMisfiredListener triggerMisfiredListener;
	@Autowired
	private FlowTriggerListener flowTriggerListener;
	@Autowired
	private FlowJobListener flowJobListener;

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
		listenerManager.addTriggerListener(flowTriggerListener);
		listenerManager.addJobListener(flowJobListener);
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
		TriggerKey triggerKey = null;
		Trigger trigger = null;
		for(JobKey job : jobs) {
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(job);
			if(triggers == null || triggers.size() == 0) { continue; }
			status = new JobStatus();
			status.setName(job.getName());
			status.setGroup(job.getGroup());
			triggerKey = triggerKey(job.getName(), GROUP_NAME);
			trigger = scheduler.getTrigger(triggerKey);
			status.setStartTime(trigger.getStartTime());
			status.setNextFireTime(trigger.getNextFireTime());
			status.setPreviousFireTime(trigger.getPreviousFireTime());
			status.setPriority(trigger.getPriority());
			status.setState(scheduler.getTriggerState(triggerKey));
			list.add(status);
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
				.withMisfireHandlingInstructionDoNothing())
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

	public void delete(JobKey jobKey) throws SchedulerException {
		if(scheduler.checkExists(jobKey)) {
			scheduler.deleteJob(jobKey);
		}
		TriggerKey triggerKey = triggerKey(jobKey.getName(), jobKey.getGroup());
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

	public void add(FlowConfig flowConfig) throws SchedulerException {
		JobKey jobKey = jobKey(flowConfig.getName(), GROUP_NAME);
		JobDetail job = newJob(WorkFlowJob.class)
			.withIdentity(jobKey)
			.usingJobData(WorkFlowJob.ROOT_FLOW, jobKey.toString())
			.usingJobData(WorkFlowJob.FLOW_DEFINITION, JsonUtils.toJsonString(flowConfig.getWorkflow()))
			.build();
		Trigger trigger = newTrigger()
			.withIdentity(flowConfig.getName(), GROUP_NAME)
			.usingJobData(WorkFlowJob.IS_FLOW, Boolean.toString(true))
			.startNow()
			.withSchedule(cronSchedule(flowConfig.getCronExpression())
				.withMisfireHandlingInstructionDoNothing())
			.forJob(job)
			.build();
		scheduler.scheduleJob(job, trigger);
	}

	public JobKey add(String root, String jobName, WorkFlow workflow, String listenerName) throws SchedulerException {
		JobDetail job = newJob(WorkFlowJob.class)
			.withIdentity(jobName, GROUP_NAME)
			.usingJobData(WorkFlowJob.FLOW_DEFINITION, JsonUtils.toJsonString(workflow))
			.usingJobData(WorkFlowJob.ROOT_FLOW, root)
			.usingJobData(WorkFlowJob.LISTENER_NAME, listenerName)
			.storeDurably(true)
			.build();
		scheduler.addJob(job, true);
		return job.getKey();
	}

}
