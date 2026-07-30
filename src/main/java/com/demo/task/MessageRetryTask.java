package com.demo.task;


import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillMessage;
import com.demo.enums.MessageStatus;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.service.MQSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class MessageRetryTask {

    @Autowired
    private SeckillMessageMapper messageMapper;

    @Autowired
    private MQSender mqSender;

    /**
     * 每30秒执行一次，扫描需要重新发送MQ的消息
     */
    @Scheduled(cron = "0/30 * * * * ?")
    @Async("asyncExecutor") // 异步执行，避免阻塞定时任务线程
    public void retryPendingMessages() {
        log.debug("开始扫描待重试消息...");

        try{
            // 每次取100条待重试的消息
            List<SeckillMessage> messages = messageMapper.selectPendingRetryMessages(100);
            if (messages.isEmpty()) {
                return;
            }
            log.info("扫描到 {} 条需要重试的消息", messages.size());
            messages.parallelStream().forEach(message -> {
                retrySingleMessage(message);
            });
        }catch (Exception e) {
            log.error("扫描重试消息失败", e);
        }

      /*  for (SeckillMessage msg : messages) {
            try {
                // 1. 更新重试次数
                int updated = messageMapper.incrementRetry(msg.getId());
                if (updated == 0) {
                    log.warn("消息状态已变更，跳过重试: bizId={}", msg.getBizId());
                    continue;
                }

                // 2. 重新发送 MQ 消息
                SeckillOrderMessage mqMsg = new SeckillOrderMessage(
                        msg.getBizId(),
                        msg.getSeckillId(),
                        msg.getUserPhone(),
                        msg.getOrderAmount()
                );
                mqSender.sendSeckillOrder(mqMsg);

                log.info("重试消息已发送: bizId={}, 第{}次重试",
                        msg.getBizId(), msg.getRetryCount() + 1);

            } catch (Exception e) {
                log.error("重试消息失败: bizId={}", msg.getBizId(), e);

                // 检查是否超过最大重试次数
                if (msg.getRetryCount() + 1 >= msg.getMaxRetry()) {
                    messageMapper.markFailed(msg.getId(), "重试超限: " + e.getMessage());
                    log.error("消息重试超限，标记为失败: bizId={}", msg.getBizId());
                    // TODO: 发送告警通知（钉钉/邮件/短信）
                }
            }
        }*/
    }

    /**
     * 重试单条消息
     */
    private void retrySingleMessage(SeckillMessage message) {
        log.info("开始重试消息，id={}, bizId={}, retryCount={}/{}",
                message.getId(), message.getBizId(),
                message.getRetryCount(), message.getMaxRetry());

        try {
            // 构造MQ消息
            SeckillOrderMessage mqMessage = new SeckillOrderMessage(
                    message.getBizId(),
                    message.getSeckillId(),
                    message.getUserPhone(),
                    message.getOrderAmount()
            );

            // 发送MQ
            mqSender.sendSeckillOrder(mqMessage);
            log.info("补偿发送MQ成功，bizId={}", message.getBizId());

            // 更新状态为SENT
            message.setStatus(MessageStatus.SENT.getCode());
            messageMapper.updateById(message);

        } catch (Exception e) {
            log.error("补偿发送MQ失败，bizId={}, retryCount={}",
                    message.getBizId(), message.getRetryCount(), e);

            // 更新重试信息
            int newRetryCount = message.getRetryCount() + 1;

            messageMapper.incrementRetry(
                    message.getId(),
                    e.getMessage()
            );

            // 检查是否超过最大重试次数
            if (newRetryCount >= message.getMaxRetry()) {
                // 标记为最终失败
                messageMapper.markFailed(message.getId(), "超过最大重试次数：" + e.getMessage());

                // 发送告警
//                sendAlert(message, e);

                // 严重情况：需要补偿Redis库存（消息彻底失败，库存需要回滚）
                // 注意：这里需要谨慎处理，确认MQ确实没发出去且无法重试
                // 一般需要人工介入，不建议自动回滚库存，防止超卖
                log.error("❌ 消息最终失败，需要人工介入！bizId={}, seckillId={}",
                        message.getBizId(), message.getSeckillId());
                // 发送紧急告警
                sendUrgentAlert(message);
            }
        }
    }



    /**
     * 发送紧急告警
     */
    private void sendUrgentAlert(SeckillMessage message) {
        String alertMsg = String.format(
                "🚨【严重】秒杀消息最终失败，需人工介入！\nbizId=%s\nseckillId=%d\nuserPhone=%s\nprice=%s",
                message.getBizId(), message.getSeckillId(),
                message.getUserPhone(), message.getOrderAmount()
        );
        log.error("🚨 紧急告警：{}", alertMsg);
        // 钉钉/企业微信/邮件/电话告警
        // dingTalkService.sendUrgentAlert(alertMsg);
    }
}