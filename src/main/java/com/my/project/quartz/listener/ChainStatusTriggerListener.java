package com.my.project.quartz.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class ChainStatusTriggerListener extends TriggerListenerSupport {

	public static final String NAME = "_chainStatusTriggerListener";

	private List<JobKey> runningChain;
	private Map<JobKey, JobKey> mutexChain;

    public ChainStatusTriggerListener() {
        this.runningChain = new CopyOnWriteArrayList<JobKey>();
        this.mutexChain = new ConcurrentHashMap<JobKey, JobKey>();
    }

    public synchronized void addRunningChain(JobKey jobKey) {
    	this.runningChain.add(jobKey);
    }

    public synchronized void removeRunningChain(JobKey jobKey) {
    	this.runningChain.remove(jobKey);
    }

    public synchronized void addMutexJob(JobKey job1, JobKey job2) {
    	this.mutexChain.put(job1, job2);
    	this.mutexChain.put(job2, job1);
    }

    public synchronized void removeMutexJob(JobKey job) {
    	for(Map.Entry<JobKey, JobKey> entry : this.mutexChain.entrySet()) {
    		if(entry.getKey().equals(job) || entry.getValue().equals(job)) {
    			this.mutexChain.remove(entry.getKey(), entry.getValue());
    		}
    	}
    }

    public boolean isRunning(JobKey jobKey) {
    	return runningChain.contains(jobKey);
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
		if(runningChain.contains(jobKey)) {
			getLog().info("jobChain '" + jobKey + "' is running now, will not run again.");
			return true;
		}
		JobKey mutexJob = mutexChain.get(jobKey);
		if(runningChain.contains(mutexJob)) {
			getLog().info("mutex jobChain '" + mutexJob + "' is running now, will not run '" + jobKey + "'.");
			return true;
		}
		return false;
	}

}
