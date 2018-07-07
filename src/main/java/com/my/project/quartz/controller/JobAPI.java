package com.my.project.quartz.controller;

import java.util.Set;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.my.project.quartz.service.JobService;

@RestController
@RequestMapping("/api/job")
public class JobAPI {

	@Autowired
	private JobService jobService;

	@RequestMapping(value = "/info", method = RequestMethod.GET)
	public String showInfo() {
		return "This is a quartz scheduler sample project.";
	}

	@RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Set<JobKey> list() throws SchedulerException {
		return jobService.list();
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public void add() throws SchedulerException {
		jobService.add();
	}

	@RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
	public void delete(@PathVariable String id) throws SchedulerException {
		jobService.delete(id);
	}

	@RequestMapping(value = "/pause/{id}", method = RequestMethod.GET)
	public void pause(@PathVariable String id) throws SchedulerException {
		jobService.pause(id);
	}

	@RequestMapping(value = "/resume/{id}", method = RequestMethod.GET)
	public void resume(@PathVariable String id) throws SchedulerException {
		jobService.resume(id);
	}

	@RequestMapping(value = "/run/{id}", method = RequestMethod.GET)
	public void run(@PathVariable String id) throws SchedulerException {
		jobService.run(id);
	}
}
