package com.my.project.quartz.listener;

import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.JobKey.jobKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.listeners.JobListenerSupport;

import com.my.project.quartz.job.ChainedJob;

public class JobChainJobListener extends JobListenerSupport {

	public static final String NAME = "_jobChainJobListener";

	private String name;
	private List<JobKey> jobKeys;
	private Map<JobKey, JobKey> chainLinks;

    public JobChainJobListener(String name, List<JobKey> jobKeys) {
        if(name == null) {
            throw new IllegalArgumentException("listener name cannot be null!");
        }
        this.name = name;
        this.jobKeys = jobKeys;
        chainLinks = new HashMap<JobKey, JobKey>();
		for(int i = 0; i < jobKeys.size()-1; i++) {
			JobKey firstJob = jobKeys.get(i);
			JobKey secondJob = jobKeys.get(i+1);
			if(firstJob == null || secondJob == null) {
				throw new IllegalArgumentException("key cannot be null!");
			}
			if(firstJob.getName() == null || secondJob.getName() == null) {
				throw new IllegalArgumentException("key cannot have a null name!");
			}
			chainLinks.put(firstJob, secondJob);
		}
		chainLinks.put(jobKeys.get(jobKeys.size()-1), null);
    }

	@Override
	public String getName() {
		return name;
	}

    @Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    	JobKey jobKey = context.getJobDetail().getKey();
        JobKey secondJob = chainLinks.get(jobKey);
        if(secondJob == null) {
        	removeListener(context);
        	removeJob(context);
            return;
        }

        if(jobException != null) {
        	JobDataMap data = context.getJobDetail().getJobDataMap();
        	String chainName = data.getString(ChainedJob.CHAIN_NAME);
        	getLog().error("error encountered during jobChain '" + chainName + "' executing", jobException);
        	JobKey chainEndJob = jobKey(data.getString(ChainedJob.CHAIN_END_JOB), jobKey.getGroup());
        	try {
				context.getScheduler().triggerJob(chainEndJob);
			} catch (SchedulerException e) {
				getLog().error("error encountered during end the jobChain '" + chainName + "'", e);
			}
        	return;
        }

        getLog().debug("job '" + jobKey + "' will now chain to job '" + secondJob + "'");

        try {
             context.getScheduler().triggerJob(secondJob);
        } catch(SchedulerException se) {
            getLog().error("error encountered during chaining to Job '" + secondJob + "'", se);
        }
	}

	private void removeListener(JobExecutionContext context) {
    	String listenerName = context.getJobDetail().getJobDataMap().getString(ChainedJob.LISTENER_NAME);
    	try {
			context.getScheduler().getListenerManager().removeJobListener(listenerName);
		} catch (SchedulerException e) {
			getLog().error("error encountered during remove job listener '" + listenerName + "'", e);
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
				getLog().error("error encountered during remove job '" + jobKey + "'", e);
			}
		}
	}

}
