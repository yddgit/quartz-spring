package com.my.project.quartz.listener;

import static org.quartz.TriggerKey.triggerKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.listeners.JobListenerSupport;

import com.my.project.quartz.job.WorkflowJob;

public class SeqJobListener extends JobListenerSupport {

	public static final String NAME = "_SeqJobListener";

	private String name;
	private List<JobKey> jobKeys;
	private Map<JobKey, JobKey> chainLinks;

    public SeqJobListener(String name, List<JobKey> jobKeys) {
        if(name == null) {
            throw new IllegalArgumentException("Listener name cannot be null!");
        }
        this.name = name;
        this.jobKeys = jobKeys;
        chainLinks = new HashMap<JobKey, JobKey>();
		for(int i = 0; i < jobKeys.size()-1; i++) {
			JobKey firstJob = jobKeys.get(i);
			JobKey secondJob = jobKeys.get(i+1);
			if(firstJob == null || secondJob == null) {
				throw new IllegalArgumentException("Key cannot be null!");
			}
			if(firstJob.getName() == null || secondJob.getName() == null) {
				throw new IllegalArgumentException("Key cannot have a null name!");
			}
			chainLinks.put(firstJob, secondJob);
		}
    }

	@Override
	public String getName() {
		return name;
	}

    @Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobKey secondJob = chainLinks.get(context.getJobDetail().getKey());
        if(secondJob == null) {
        	removeListener(context);
        	removeJob(context);
            return;
        }

        getLog().debug("Job '" + context.getJobDetail().getKey() + "' will now chain to Job '" + secondJob + "'");

        try {
             context.getScheduler().triggerJob(secondJob);
        } catch(SchedulerException se) {
            getLog().error("Error encountered during chaining to Job '" + secondJob + "'", se);
        }
	}

	private void removeListener(JobExecutionContext context) {
    	String listenerName = context.getJobDetail().getJobDataMap().getString(WorkflowJob.LISTENER_NAME);
    	try {
			context.getScheduler().getListenerManager().removeJobListener(listenerName);
		} catch (SchedulerException e) {
			getLog().error("Error encountered during remove job listener [" + listenerName + "]", e);
		}
    }

	private void removeJob(JobExecutionContext context) {
		Scheduler scheduler = context.getScheduler();
		for(JobKey jobKey : jobKeys) {
			try {
				if(scheduler.checkExists(jobKey)) {
					scheduler.deleteJob(jobKey);
				}
				TriggerKey triggerKey = triggerKey(jobKey.getName(), jobKey.getGroup());
				if(scheduler.checkExists(triggerKey)) {
					scheduler.unscheduleJob(triggerKey);
				}
			} catch (SchedulerException e) {
				getLog().error("Error encountered during remove job [" + jobKey + "]", e);
			}
		}
	}

}
