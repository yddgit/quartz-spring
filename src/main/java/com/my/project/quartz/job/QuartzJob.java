package com.my.project.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.my.project.quartz.service.JobService;

@Component
@Scope("prototype")
public class QuartzJob extends QuartzJobBean {

	@Autowired
	private JobService jobService;
	@Value("${name}")
	private String name;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		System.out.println(this.name + " says: " + jobService);
	}

}