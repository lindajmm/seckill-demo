package com.demo.service;


import com.demo.config.RabbitMQConfig;
import com.demo.dto.SeckillOrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author: Linda
 * @date: 2026/7/9 11:55
 * @description:
 */
@Service
public class MQSender {
    private final static Logger log = LoggerFactory.getLogger(MQSender.class);


    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendSeckillOrder(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message
        );
        log.info("MQ 消息已发送: {} ", message);
    }
}
