package com.demo.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author: Linda
 * @date: 2026/7/9 11:51
 * @description:
 */

@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    // 队列名称
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    // 路由键
    public static final String SECKILL_ROUTING_KEY = "seckill.order";


    // ========== 重置库存相关（新增） ==========
    public static final String RESET_EXCHANGE = "reset.exchange";
    public static final String RESET_QUEUE = "reset.stock.queue";
    public static final String RESET_ROUTING_KEY = "reset.stock";

    /**
     * 创建交换机（Topic 类型，支持通配符）
     */
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE);
    }

    /**
     * 创建队列（持久化，避免重启丢失）
     */
    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    /**
     * 绑定：队列 → 交换机，使用路由键
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder
                .bind(seckillQueue())
                .to(seckillExchange())
                .with(SECKILL_ROUTING_KEY);
    }


    // ========== 重置库存相关 Bean（新增） ==========
    @Bean
    public TopicExchange resetExchange() {
        return new TopicExchange(RESET_EXCHANGE);
    }

    @Bean
    public Queue resetQueue() {
        return new Queue(RESET_QUEUE, true);
    }

    @Bean
    public Binding resetBinding() {
        return BindingBuilder
                .bind(resetQueue())
                .to(resetExchange())
                .with(RESET_ROUTING_KEY);
    }


    /*
几乎都会把消息格式改为 JSON，因为：
体积小、可读性强
跨语言支持好
版本兼容性灵活

配置 JSON 消息转换器
     */
    @Bean
    public MessageConverter jackson2MessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
