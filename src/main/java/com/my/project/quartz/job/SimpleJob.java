package com.my.project.quartz.job;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.my.project.quartz.exception.JobChainException;
import com.my.project.quartz.util.JsonUtils;

@Component
@Scope("prototype")
@DisallowConcurrentExecution
public class SimpleJob extends QuartzJobBean {

	private static final int TIMEOUT_SECONDS = 1 * 60 * 60; // 1 hour
	private static final Logger logger = LoggerFactory.getLogger(SimpleJob.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String chainEndJob = data.getString(ChainedJob.CHAIN_END_JOB);
			String jobName = context.getJobDetail().getKey().getName();
			if(jobName.equals(chainEndJob)) { return; }
			List<String> chainedJobs = JsonUtils.jsonToList(data.getString(ChainedJob.CHAINED_JOBS), String.class);
			executeSimpleJob(jobName, chainedJobs);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

	private void executeSimpleJob(String jobName, List<String> chainedJobs) throws SchedulerException, JobChainException, InterruptedException {
		logger.info("execute chained job: " + jobName);
		if(chainedJobs == null || chainedJobs.size() == 0) {
			throw new JobChainException("chained job must have at least one", chainedJobs);
		}

		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(cores);
		for(String job : chainedJobs) {
			service.execute(() -> {
				logger.info("execute: " + job);
				try {
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		service.shutdown();
		if(!service.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			throw new JobChainException("chained job execution time out", chainedJobs);
		}
	}

}
