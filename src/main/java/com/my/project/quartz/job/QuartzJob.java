package com.my.project.quartz.job;

import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
// Marks a Job class as one that must not have multiple instances executed concurrently
// (Based-upon a JobDetail definition - or in other words based upon a JobKey)
@DisallowConcurrentExecution
public class QuartzJob extends QuartzJobBean {

	private static final Logger logger = LoggerFactory.getLogger(QuartzJob.class);

	@Value("${name}")
	private String name;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		logger.info("Job Start..." + context.getFireTime().getTime());
		String jobName = context.getJobDetail().getJobDataMap().getString("jobName");
		logger.info(this.name + " says: " + jobName);
		try {
			TimeUnit.SECONDS.sleep(60 * 1);
		} catch (InterruptedException e) {
			logger.error("Job Error", e);
		}
		logger.info("Job End..." + context.getFireTime().getTime());
	}

}