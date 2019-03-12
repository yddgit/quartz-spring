package com.my.project.quartz.job;

import static org.quartz.impl.matchers.KeyMatcher.keyEquals;

import java.util.ArrayList;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.my.project.quartz.exception.JobChainException;
import com.my.project.quartz.listener.JobChainJobListener;
import com.my.project.quartz.service.JobService;
import com.my.project.quartz.util.JsonUtils;

@Component
@Scope("prototype")
@DisallowConcurrentExecution
public class ChainedJob extends QuartzJobBean {

	public static final String NAME_SEPARATOR = "^_^";
	public static final String CHAIN_NAME = "_chainName";
	public static final String CHAIN_END_JOB = "_chainEndJob";
	public static final String CHAINED_JOBS = "_chainedJobs";
	public static final String LISTENER_NAME = "_listenerName";

	private static final Logger logger = LoggerFactory.getLogger(ChainedJob.class);

	@Autowired
	private JobService jobService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			String chainName = context.getJobDetail().getKey().getName();
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String chainEndJob = data.getString(CHAIN_END_JOB);
			List<List<String>> chainedJobs = JsonUtils.jsonToObject(
				data.getString(CHAINED_JOBS),
				new TypeReference<List<List<String>>>(){});
			executeJobChain(chainName, chainEndJob, chainedJobs);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	private void executeJobChain(String chainName, String chainEndJob, List<List<String>> chainedJobs) throws SchedulerException, JobChainException {

		logger.info("execute: " + chainName);

		if(chainedJobs == null || chainedJobs.size() == 0) {
			jobService.delete(chainName);
			throw new JobChainException("jobChain must have at least one child job" + JsonUtils.toJsonString(chainedJobs, true));
		}

		String listenerName = JobChainJobListener.NAME + "." + chainName;
		List<JobKey> jobKeys = new ArrayList<JobKey>();
		List<Matcher<JobKey>> matchers = new ArrayList<Matcher<JobKey>>();
		for(List<String> job : chainedJobs) {
			JobKey jobKey = jobService.add(chainName, chainEndJob, job, listenerName);
			jobKeys.add(jobKey);
			matchers.add(keyEquals(jobKey));
		}
		JobChainJobListener seqJobListener = new JobChainJobListener(listenerName, jobKeys);
		jobService.getListenerManager().addJobListener(seqJobListener, matchers);
		jobService.getScheduler().triggerJob(jobKeys.get(0));

	}

}
