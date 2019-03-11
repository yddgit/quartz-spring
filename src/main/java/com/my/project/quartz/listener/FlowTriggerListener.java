package com.my.project.quartz.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class FlowTriggerListener extends TriggerListenerSupport {

	public static final String NAME = "_flowTriggerListener";

	private List<JobKey> runningWorkflow;
	private Map<JobKey, JobKey> mutexJobs;

    public FlowTriggerListener() {
        this.runningWorkflow = new CopyOnWriteArrayList<JobKey>();
        this.mutexJobs = new ConcurrentHashMap<JobKey, JobKey>();
    }

    public void addRunningWorkflow(JobKey jobKey) {
    	this.runningWorkflow.add(jobKey);
    }

    public void removeRunningWorkflow(JobKey jobKey) {
    	this.runningWorkflow.remove(jobKey);
    }

    public void addMutexJob(JobKey job1, JobKey job2) {
    	this.mutexJobs.put(job1, job2);
    	this.mutexJobs.put(job2, job1);
    }

    public void removeMutexJob(JobKey job1, JobKey job2) {
    	this.mutexJobs.remove(job1, job2);
    	this.mutexJobs.remove(job2, job1);
    }

    public boolean isRunning(JobKey jobKey) {
    	return runningWorkflow.contains(jobKey);
    }

    @Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		return isBlocked(trigger, context);
	}

	private synchronized boolean isBlocked(Trigger trigger, JobExecutionContext context) {
		JobKey jobKey = trigger.getJobKey();
		if(runningWorkflow.contains(jobKey)) {
			getLog().debug("job '" + jobKey + "' is running now, will not run it until the last finished.");
			return true;
		}
		JobKey mutexJob = mutexJobs.get(jobKey);
		try {
			for(JobExecutionContext c : context.getScheduler().getCurrentlyExecutingJobs()) {
				if(c.getJobDetail().getKey().equals(mutexJob)) {
					getLog().info("mutex job '" + mutexJob + "' is running now, will not run it until the mutex job finished.");
					return true;
				}
			}
		} catch (SchedulerException e) {
			getLog().error("check mutex job for job '" + jobKey + "' error, will not run it", e);
			return true;
		}
		return false;
	}

}
