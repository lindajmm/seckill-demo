package com.demo.service;


import com.demo.config.RabbitMQConfig;
import com.demo.dto.ResetStockMessage;
import com.demo.dto.SeckillOrderMessage;
import org.flywaydb.core.internal.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;

import org.springframework.amqp.core.MessageBuilder;
//import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;


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
                message,
                message1 -> {
                    message1.getMessageProperties().setHeader("traceId",MDC.get("traceId"));
                    message1.getMessageProperties().setHeader("spanId",MDC.get("spanId"));
                    return message1;
                }
        );


      /*  rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message
        );*/
        log.info("MQ 消息已发送: {} ", message);
    }

    public void sendResetMessage(Long seckillId, Integer stockNumber){
        // 2. 发送异步消息，由消费者更新数据库
        ResetStockMessage message = new ResetStockMessage(seckillId, stockNumber);
        rabbitTemplate.convertAndSend("reset.exchange", "reset.stock", message);
    }
}
