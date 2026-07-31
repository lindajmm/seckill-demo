package com.demo.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillMessage;
import com.demo.enums.MessageStatus;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.util.ErrorSummaryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Slf4j
public class SeckillMessageService {

    @Autowired
    private SeckillMessageMapper messageMapper;

    @Autowired
    private MQSender mqSender;

    @Autowired
    private RedisStockService redisStockService;

    /**
     * 插入本地消息表并发送MQ（带事务和补偿机制）
     */
    @Transactional(rollbackFor = Exception.class)
    public SeckillMessage saveAndSendMessage(String bizId, Long seckillId,
                                             Long userPhone, BigDecimal orderAmount) {
        // 1. 幂等性检查（防止重复插入）
        SeckillMessage existMsg = messageMapper.selectOne(
                new LambdaQueryWrapper<SeckillMessage>()
                        .eq(SeckillMessage::getBizId, bizId)
        );
        if (existMsg != null) {
            log.warn("消息已存在，bizId={}, status={}", bizId, existMsg.getStatus());
            return existMsg;
        }

        // 2. 计算下次重试时间（初始立即重试）
        LocalDateTime nextRetryTime = LocalDateTime.now(Clock.systemUTC()).plusSeconds(10);

        // 3. 插入本地消息表（状态=INIT）
        SeckillMessage message = new SeckillMessage();
        message.setBizId(bizId);
        message.setSeckillId(seckillId);
        message.setUserPhone(userPhone);
        message.setOrderAmount(orderAmount);
        message.setStatus(MessageStatus.INIT.getCode());
        message.setRetryCount(0);
        message.setMaxRetry(3);
        message.setNextRetryTime(nextRetryTime);

        messageMapper.insert(message);
        log.info("本地消息表插入成功，id={}, bizId={}", message.getId(), bizId);

        // 4. 立即尝试发送MQ（事务提交后执行，避免长事务）
        // 使用事务同步器，在事务提交后执行
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        trySendMq(message);
                    }
                }
        );

        return message;
    }

    /**
     * 尝试发送MQ（带重试逻辑）
     */
    private void trySendMq(SeckillMessage message) {
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
            log.info("MQ发送成功，bizId={}", message.getBizId());

            // 更新状态为SENT
            message.setStatus(MessageStatus.SENT.getCode());
            messageMapper.updateById(message);

        } catch (Exception e) {
            log.error("MQ发送失败，bizId={}, error={}", message.getBizId(), e.getMessage(), e);

            // 更新重试信息（下次重试时间按指数退避计算）
            messageMapper.incrementRetry(
                    message.getId(),
                    ErrorSummaryUtil.extractSummary(e)
            );

            // 如果达到最大重试次数，标记为最终失败并告警
            if (message.getRetryCount() + 1 >= message.getMaxRetry()) {
                messageMapper.markFailed(message.getId(), "MQ发送失败：" + ErrorSummaryUtil.extractSummary(e));
                // 发送告警（钉钉/邮件/电话）
                sendAlert(message, e);
            }
        }
    }


    /**
     * 发送告警
     */
    private void sendAlert(SeckillMessage message, Exception e) {
        // 实际项目中调用钉钉/企业微信/邮件告警
        String alertMsg = String.format(
                "【秒杀消息重试失败】bizId=%s, seckillId=%d, error=%s",
                message.getBizId(), message.getSeckillId(), e.getMessage()
        );
        log.error("⚠️ 告警：{}", alertMsg);
        // dingTalkService.sendAlert(alertMsg);
    }
}
