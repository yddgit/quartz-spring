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

	private List<JobKey> runningWorkFlow;
	private Map<JobKey, JobKey> mutexJobs;

    public FlowTriggerListener() {
        this.runningWorkFlow = new CopyOnWriteArrayList<JobKey>();
        this.mutexJobs = new ConcurrentHashMap<JobKey, JobKey>();
    }

    public void addRunningWorkFlow(JobKey jobKey) {
    	this.runningWorkFlow.add(jobKey);
    }

    public void removeRunningWorkFlow(JobKey jobKey) {
    	this.runningWorkFlow.remove(jobKey);
    }

    public void addMutexJob(JobKey job1, JobKey job2) {
    	this.mutexJobs.put(job1, job2);
    	this.mutexJobs.put(job2, job1);
    }

    public void removeMutexJob(JobKey job1, JobKey job2) {
    	this.mutexJobs.remove(job1, job2);
    	this.mutexJobs.remove(job2, job1);
    }

    @Override
	public String getName() {
		return "FlowTriggerListener";
	}

	@Override
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		boolean isBlocked = isBlocked(trigger, context);
		if(isBlocked) {
			getLog().info(trigger.getJobKey() + " is running now, it will not run until the last finished.");
			return true;
		}
		return false;
	}

	private synchronized boolean isBlocked(Trigger trigger, JobExecutionContext context) {
		JobKey jobKey = trigger.getJobKey();
		if(runningWorkFlow.contains(jobKey)) {
			return true;
		}
		JobKey mutexJob = mutexJobs.get(jobKey);
		try {
			for(JobExecutionContext c : context.getScheduler().getCurrentlyExecutingJobs()) {
				if(c.getJobDetail().getKey().equals(mutexJob)) {
					return true;
				}
			}
		} catch (SchedulerException e) {
			getLog().error(e.getMessage(), e);
			return true;
		}
		return false;
	}

}
