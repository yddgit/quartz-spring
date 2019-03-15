package com.my.project.quartz.listener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

import com.my.project.quartz.util.JsonUtils;

@Component
public class ChainStatusTriggerListener extends TriggerListenerSupport {

	public static final String NAME = "_chainStatusTriggerListener";

	private List<JobKey> runningChain;
	private Map<JobKey, Set<JobKey>> mutexChain;

    public ChainStatusTriggerListener() {
        this.runningChain = new CopyOnWriteArrayList<JobKey>();
        this.mutexChain = new ConcurrentHashMap<JobKey, Set<JobKey>>();
    }

    public synchronized void addRunningChain(JobKey jobKey) {
    	getLog().info("jobChain '" + jobKey + "' start");
    	this.runningChain.add(jobKey);
    }

    public synchronized void removeRunningChain(JobKey jobKey) {
    	getLog().info("jobChain '" + jobKey + "' done");
    	this.runningChain.remove(jobKey);
    }

    public synchronized void addMutexJob(JobKey job, Set<JobKey> mutexJobs) {
        if(job == null || mutexJobs == null) {
            throw new IllegalArgumentException("jobKey added to mutex list cannot be null!");
        }
        // job --> mutexJobs
        Set<JobKey> oldMutexJobs = this.mutexChain.get(job);
        if(oldMutexJobs == null) {
        	oldMutexJobs = new CopyOnWriteArraySet<JobKey>();
        }
        oldMutexJobs.addAll(mutexJobs);
    	this.mutexChain.put(job, mutexJobs);
    	// mutexJobs --> job
    	for(JobKey mutexJob : mutexJobs) {
            Set<JobKey> old = this.mutexChain.get(mutexJob);
            if(old == null) {
            	old = new CopyOnWriteArraySet<JobKey>();
            }
            old.add(job);
        	this.mutexChain.put(mutexJob, old);
    	}
    }

    public synchronized void removeMutexJob(JobKey job) {
    	this.mutexChain.remove(job);
    	Iterator<Map.Entry<JobKey, Set<JobKey>>> it = this.mutexChain.entrySet().iterator();
    	Map.Entry<JobKey, Set<JobKey>> entry = null;
    	while(it.hasNext()) {
    		entry = it.next();
    		if(entry.getValue().contains(job)) {
    			entry.getValue().remove(job);
    		}
    	}
    }

    public Set<JobKey> getMutexJob(JobKey job) {
    	Set<JobKey> mutex = this.mutexChain.get(job);
    	if(mutex == null) {
    		mutex = new CopyOnWriteArraySet<JobKey>();
    	}
    	return mutex;
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
		Set<JobKey> mutexJobs = mutexChain.get(jobKey);
		if(mutexJobs != null && mutexJobs.size() > 0) {
			Set<JobKey> runningMutexJobs = new HashSet<JobKey>();
			for(JobKey mutexJob : mutexJobs) {
				if(runningChain.contains(mutexJob)) {
					runningMutexJobs.add(mutexJob);
				}
			}
			if(runningMutexJobs.size() > 0) {
				getLog().info("mutex jobChain " + JsonUtils.toJsonString(runningMutexJobs, true) + " is running now, will not run '" + jobKey + "'.");
				return true;
			}
		}
		return false;
	}

}
