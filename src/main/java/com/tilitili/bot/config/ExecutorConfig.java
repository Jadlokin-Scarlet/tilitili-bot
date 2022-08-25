package com.tilitili.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {

	@Autowired
	public ExecutorConfig(Executor taskExecutor) {
		if (taskExecutor instanceof ThreadPoolTaskExecutor) {
			ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
			executor.setWaitForTasksToCompleteOnShutdown(true);
			executor.setAwaitTerminationSeconds(10);
		}
	}
}
