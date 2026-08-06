package com.demo.config;


import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ScheduledConfig {

    /*异步任务线程池 （用于 @Async 标注的方法）*/
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);     // 核心线程数
        executor.setMaxPoolSize(20);     // 最大线程数
        executor.setQueueCapacity(100);  // 队列容量
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略

        // MDC上下文拷贝装饰器
        executor.setTaskDecorator(runnable -> {
            // 捕获父线程全部MDC
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    runnable.run();
                } finally {
                    // 执行完毕强制清空，线程池复用防污染
                    MDC.clear();
                }
            };
        });

        executor.initialize();
        return executor;
    }


    /**
     * 定时任务线程池（用于 @Scheduled 标注的方法）
     * 如果不配置，Spring 默认使用单线程执行所有定时任务
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);              // 线程池大小
        scheduler.setThreadNamePrefix("scheduled-task-");

        scheduler.setTaskDecorator(runnable -> {
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        });

        scheduler.initialize();
        return scheduler;
    }
}
