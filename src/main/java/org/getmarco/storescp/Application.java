package org.getmarco.storescp;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Application implements AsyncConfigurer {

	@Autowired
	private Config config;

	public static void main(String[] args) {
		System.out.println("starting storescp");
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public AmazonS3 s3Client() {
		return AmazonS3ClientBuilder.standard().withRegion(config.getGetStorageBucketRegion()).build();
	}

	@Override
	public Executor getAsyncExecutor() {
		return new ThreadPoolTaskExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new CustomAsyncExceptionHandler();
	}

	public static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
		@Override
		public void handleUncaughtException(
		Throwable throwable, Method method, Object... obj) {
			System.out.println("Exception message - " + throwable.getMessage());
			System.out.println("Method name - " + method.getName());
			for (Object param : obj) {
				System.out.println("Parameter value - " + param);
			}
		}
	}
}
