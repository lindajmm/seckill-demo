package com.demo.common;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 秒杀监控指标组件
 * 统一管理所有秒杀相关的计数器
 */
@Component
@Getter
public class SeckillMetrics {

    private final MeterRegistry registry;
    private Counter successCounter;        // 秒杀成功次数
    private Counter systemFailureCounter;  // 系统失败次数
    private Counter businessFailureCounter; // 业务失败次数

    public SeckillMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        // 成功计数器
        this.successCounter = Counter.builder("seckill.request.success")
                .description("秒杀成功次数")
                .register(registry);

        // 系统失败计数器
        this.systemFailureCounter = Counter.builder("seckill.request.system.failure")
                .description("秒杀系统失败次数（系统异常）")
                .register(registry);

        // 业务失败计数器（可选）
        this.businessFailureCounter = Counter.builder("seckill.request.business.failure")
                .description("秒杀业务失败次数（业务异常）")
                .register(registry);
    }

    // 便捷方法
    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementSystemFailure() {
        systemFailureCounter.increment();
    }

    public void incrementBusinessFailure() {
        businessFailureCounter.increment();
    }
}
