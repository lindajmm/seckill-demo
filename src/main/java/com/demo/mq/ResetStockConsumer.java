package com.demo.mq;


import com.demo.config.RabbitMQConfig;
import com.demo.dto.ResetStockMessage;
import com.demo.mapper.SeckillGoodsMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResetStockConsumer {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @RabbitListener(queues = RabbitMQConfig.RESET_QUEUE)
    public void handleResetStock(ResetStockMessage message,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            // 1. 更新数据库库存（version 重置为 0）
            int affected = seckillGoodsMapper.updateStock(message.getSeckillId(), message.getStockNumber());
            if (affected == 0) {
                log.warn("商品不存在，重置失败: seckillId={}", message.getSeckillId());
                // 业务失败，不重新入队（直接确认，避免死循环）
                channel.basicAck(tag, false);
                return;
            }

            log.info("数据库库存重置成功: seckillId={}, stock={}",
                    message.getSeckillId(), message.getStockNumber());

            // 2. 手动确认
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("重置库存失败", e);
            try {
                // 异常时重新入队，保证最终一致性
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("消息拒绝失败", ex);
            }
        }
    }
}