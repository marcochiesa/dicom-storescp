package org.getmarco.storescp;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Application implements AsyncConfigurer {

	@Autowired
	private Config config;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public AmazonS3 s3Client() {
		return AmazonS3ClientBuilder.standard().withRegion(config.getStorageBucketRegion()).build();
	}

	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(5);
		executor.setThreadNamePrefix("scp-async-");
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new CustomAsyncExceptionHandler();
	}

	public static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
		private static final Logger LOG = LoggerFactory.getLogger(CustomAsyncExceptionHandler.class);
		@Override
		public void handleUncaughtException(
		Throwable throwable, Method method, Object... obj) {
			LOG.error("async-uncaught-exception '{}' at method: {}, params: {}", throwable.getMessage(),
			  method.getName(), Stream.of(obj).map(Object::toString).collect(Collectors.toList()));
		}
	}
}
