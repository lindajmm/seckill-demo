package com.demo.mq;


import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * @author: Linda
 * @date: 2026/7/9 11:56
 * @description:
 */

@Component
public class SeckillOrderConsumer {
    private final static Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @RabbitListener(queues = "seckill.order.queue")
    public void handleOrder(SeckillOrderMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            // 1. 打印原始消息体（二进制转字符串，会看到乱码）
//            String body = new String(message.toString(), StandardCharsets.UTF_8);
            log.info("原始消息体: {}", message.toString());

            // 2. 打印消息头
//            log.info("Content-Type: " + message.getMessageProperties().getContentType());


            // 1. 创建订单实体
            SeckillOrder order = new SeckillOrder();
            order.setSeckillId(message.getSeckillId());
            order.setUserPhone(message.getUserPhone());
            order.setOrderAmount(message.getOrderAmount());
            order.setStatus(0); // 0-待支付
            order.setCreateTime(LocalDateTime.now());

            // 2. 插入数据库
            seckillOrderMapper.insert(order);
            log.info("订单落库成功: {}", order);

            // 3. 手动确认消息（告诉 MQ 处理成功，可以删除了）
            channel.basicAck(tag, false);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                // 处理失败，拒绝消息并重新入队（可根据业务配置重试次数）
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
