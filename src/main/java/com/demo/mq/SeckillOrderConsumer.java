package com.demo.mq;


import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillMessage;
import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillGoodsMapper;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.mapper.SeckillOrderMapper;
import com.demo.service.RedisStockService;
import com.demo.util.ErrorSummaryUtil;
import com.rabbitmq.client.Channel;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Clock;
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

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;


    @Autowired
    private SeckillMessageMapper messageMapper;

    @Autowired
    private RedisStockService redisStockService;

    @RabbitListener(queues = "seckill.order.queue")
    public void handleOrder(SeckillOrderMessage message, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                            @Header(value = "traceId", required = false) String traceId,
                            @Header(value = "spanId", required = false) String spanId) {
        // 1、优先使用消息头透传过来的链路ID；没有则新建SkyWalking链路
        // 链路id塞入MDC
        if(traceId != null){
            MDC.put("traceId",traceId);
            MDC.put("spanId",spanId);
        }else {
            MDC.put("traceId", TraceContext.traceId());
            MDC.put("spanId",String.valueOf(TraceContext.spanId()));
        }

        String bizId = message.getBizId();
        log.info("收到MQ消息: bizId={}", bizId);
         try {
             // ========== 1. 幂等性校验（检查是否已处理过） ==========
             SeckillMessage existingMsg = messageMapper.selectByBizId(bizId);
             if (existingMsg == null) {
                 log.warn("本地消息表无记录，可能重复消费，直接确认: bizId={}", bizId);
                 channel.basicAck(tag, false);
                 return;
             }

             // 如果已经成功，直接确认
             if (existingMsg.getStatus() == 2) {
                 log.info("消息已处理成功，幂等确认: bizId={}", bizId);
                 channel.basicAck(tag, false);
                 return;
             }
             // 如果已经失败，直接确认（不再重试）
             if (existingMsg.getStatus() == 3) {
                 log.warn("消息已标记为失败，直接确认: bizId={}", bizId);
                 channel.basicAck(tag, false);
                 return;
             }

             // ============ 第1步：扣减数据库库存（先扣库存，再生成订单） ============
            int affectedRows = seckillGoodsMapper.decreaseStockWithVersion(
                message.getSeckillId(),
                1  // 默认扣1件
            );

            if (affectedRows == 0) {
                // 数据库库存不足 → 回滚Redis库存 → 拒绝消息
                log.warn("数据库库存不足，回滚Redis库存，seckillId={}", message.getSeckillId());
                redisStockService.incrementStock(message.getSeckillId());

                log.warn("数据库库存不足，标记消息为失败: bizId={}", bizId);
                messageMapper.markFailed(existingMsg.getId(), "库存不足");

                channel.basicNack(tag, false, false);  // 拒绝消息，不重新入队（业务失败，不需要重试）
                return;
            }
            log.info("数据库库存扣减成功, seckillId={}", message.getSeckillId());
             log.info("数据库库存扣减成功: bizId={}", bizId);

            // ============ 第2步：生成订单（使用你现有的实体类属性） ============
            SeckillOrder order = new SeckillOrder();
            order.setBizId(bizId);
            order.setSeckillId(message.getSeckillId());
            order.setUserPhone(message.getUserPhone());
            order.setOrderAmount(message.getOrderAmount());
            order.setStatus(0);  // 0-待支付
            order.setCreateTime(LocalDateTime.now(Clock.systemUTC()));

            seckillOrderMapper.insert(order);
            log.info("订单落库成功, orderId={}", order.getOrderId());
             log.info("订单落库成功: bizId={}", bizId);

             // ========== 4. 更新本地消息表为成功 ==========
             messageMapper.markSuccess(existingMsg.getId());
             log.info("本地消息表更新为SUCCESS: bizId={}", bizId);

             // ========== 5. 手动确认消息 ==========
             channel.basicAck(tag, false);


        } catch (Exception e) {
           /* log.error("处理订单失败", e);
            try {
                // 发生异常，拒绝消息并重新入队
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("消息拒绝失败", ex);
            }*/

             log.error("处理订单失败: bizId={}", bizId, e);
             try {
                 // 记录错误信息
                 SeckillMessage msg = messageMapper.selectByBizId(bizId);
                 if (msg != null) {

                     // 只存精简信息
                     String errorSummary = ErrorSummaryUtil.extractSummary(e);
                     messageMapper.markFailed(msg.getId(), errorSummary);
                 }
               /*  // 拒绝消息，不重新入队（由定时任务重试）
                 channel.basicNack(tag, false, false);*/

                 // 异常：拒绝并重新入队（重试）
                 // 注意：需要设置requeue=true，让MQ重新投递
                 // 但需要防止死循环，可以结合重试次数判断
                 channel.basicNack(tag, false, true);
             } catch (Exception ex) {
                 log.error("消息处理异常", ex);
             }
        }finally {
             //每次消费结束强制清空MDC，解决消费者线程复用 traceId串号
             MDC.clear();
         }

          /*try {
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
            order.setCreateTime(LocalDateTime.now(Clock.systemUTC()));

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
        }*/
    }
}
