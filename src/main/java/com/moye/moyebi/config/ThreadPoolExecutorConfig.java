package com.moye.moyebi.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 创建一个自定义的ThreadFactory，用于创建线程时设置线程名称
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };

        // 创建ThreadPoolExecutor线程池实例
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2,                              // corePoolSize - 核心线程数，即最小保持活动的线程数
                4,                              // maximumPoolSize - 最大线程数，即线程池中允许的最大线程数
                100,                            // keepAliveTime - 线程空闲时间，即超过核心线程数的线程在终止前等待新任务的最长时间
                TimeUnit.SECONDS,               // 时间单位，指定keepAliveTime的时间单位
                new ArrayBlockingQueue<>(100),  // 工作队列，存储等待执行的任务，队列容量为100
                threadFactory);                 // 线程工厂，用于创建新线程时自定义线程的属性

        return threadPoolExecutor;              // 返回配置好的ThreadPoolExecutor实例
    }
}
