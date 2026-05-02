package com.sqnu.ai.Aionfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
        @Bean(name = "sseExecutor")
        public Executor sseExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(10); // 核心线程数
            executor.setMaxPoolSize(50);  // 最大线程数
            executor.setQueueCapacity(100); // 队列大小
            executor.setThreadNamePrefix("SSE-Thread-"); // 线程名前缀
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略
            executor.initialize();
            return executor;
        }
    }

