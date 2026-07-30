package com.demo.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.entity.SeckillMessage;
import com.demo.enums.MessageStatus;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.service.RedisStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class RedisStockCleanupScheduler {

    @Autowired
    private SeckillMessageMapper messageMapper;

    @Autowired
    private RedisStockService redisStockService;

    /**
     * 每天凌晨清理：检查最终失败的消息，补偿库存
     * 注意：这只是兜底方案，正常情况下不应该触发
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupFailedMessages() {
        log.info("开始清理最终失败消息...");

        // 查询状态为FAILED且创建时间超过24小时的消息
        List<SeckillMessage> failedMessages = messageMapper.selectList(
                new LambdaQueryWrapper<SeckillMessage>()
                        .eq(SeckillMessage::getStatus, MessageStatus.FAILED.getCode())
                        .lt(SeckillMessage::getCreateTime, LocalDateTime.now(Clock.systemUTC()).minusHours(24))
        );

        for (SeckillMessage message : failedMessages) {
            try {
                // 补偿Redis库存（加回去）
                redisStockService.incrementStock(message.getSeckillId());
                log.info("补偿库存成功，seckillId={}, bizId={}",
                        message.getSeckillId(), message.getBizId());

                // 记录补偿日志（用于对账）
                // compensationLogService.record(message, "库存补偿");

            } catch (Exception e) {
                log.error("补偿库存失败，seckillId={}, bizId={}",
                        message.getSeckillId(), message.getBizId(), e);
                // 发送告警，需要人工介入
            }
        }
    }
}
