package com.demo.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillGoods;
import com.demo.entity.SeckillMessage;
import com.demo.entity.SeckillOrder;
import com.demo.exception.SeckillEndException;
import com.demo.exception.SeckillNotFoundException;
import com.demo.exception.SeckillNotStartException;
import com.demo.exception.SeckillStockEmptyException;
import com.demo.mapper.SeckillGoodsMapper;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.service.MQSender;
import com.demo.service.RedisStockService;
import com.demo.service.SeckillService;
import com.demo.util.RateLimiterUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillServiceImpl implements SeckillService {
    private final static Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillMessageMapper messageMapper;

    @Autowired
    private RedisStockService redisStockService;
    @Autowired
    private MQSender mqSender;

    @Autowired
    private RateLimiterUtil rateLimiterUtil;

    @Override
    public SeckillOrder doSeckill(Long seckillId, Long userPhone) {
        // ========== 限流检查（放在最前面） ==========
//        String limitKey = "rate:limit:user:" + userPhone + ":product:" + seckillId;
       /* //压力测试先把限流注释掉，测完再恢复
        String limitKey = "rate:limit:user:" + userPhone + ":product:" + seckillId;
        System.out.println("限流Key: " + limitKey);
        // 配置：每分钟最多 3 次请求
        // 在 SeckillServiceImpl 中
        boolean allowed = rateLimiterUtil.tryAcquireDoubao(limitKey, 3, 3, 60, 1);
        if (!allowed) {
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        System.out.println("限流结果: " + allowed);
        if (!allowed) {
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }*/
        long t0 = System.currentTimeMillis();
        // 2. 布隆过滤器检查（防缓存穿透）
        if (!redisStockService.bloomContains(seckillId)) {
//            throw new RuntimeException("秒杀商品不存在");
            throw new SeckillNotFoundException(seckillId);  // 抛出异常
        }

        // 1. 查询秒杀商品信息（校验活动时间）
      /* //刚开始直接从数据库里查，后面查缓存
      SeckillGoods seckillGoods = seckillGoodsMapper.selectById(seckillId);
        if (seckillGoods == null) {
            throw new SeckillNotFoundException(seckillId);  // 抛出异常
        }*/

        // ========== 1. 从缓存获取商品信息（替代直接查数据库） ==========
        SeckillGoods seckillGoods = redisStockService.getGoodsWithCache(seckillId);
        log.info("缓存命中： {}", seckillGoods);
        if (seckillGoods == null) {
            throw new SeckillNotFoundException(seckillId);
        }
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (now.isBefore(seckillGoods.getStartTime())) {
            throw new SeckillNotStartException(seckillId, seckillGoods.getStartTime().toString());
        }
        if (now.isAfter(seckillGoods.getEndTime())) {
            throw new SeckillEndException(seckillId, seckillGoods.getEndTime().toString());
        }
        long t1 = System.currentTimeMillis();

        log.info("防穿透，活动时间校验检查等: {}ms", t1 - t0);

        // 2. 获取分布式锁（锁的Key = lock:product:seckillId）
        RLock lock = redissonClient.getLock("lock:product:" + seckillId);

        try {
            // 尝试加锁，最多等待5秒，锁持有时间默认30秒（Watchdog会自动续期）
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            // 2. Redis 原子扣减库存
            Long remainStock = redisStockService.decreaseStock(seckillId);
            if (remainStock == null || remainStock < 0) {
//            throw new RuntimeException("库存不足");
                throw new SeckillStockEmptyException(seckillId, seckillGoods.getSeckillStock());
            }
            log.info("Redis 扣库存成功，剩余库存: {}", remainStock);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断");
        } finally {
            // 5. 释放锁（只释放当前线程持有的锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
// Redis 扣库存
        long t2 = System.currentTimeMillis();
        log.info("Redis 扣库存耗时: {}ms", t2 - t1);
        // ========== 2. 锁外：插入本地消息表 + 发送MQ ==========
// 注意：必须保证这两个操作的最终一致性
        // ==========  生成业务唯一ID ==========
        String bizId = UUID.randomUUID().toString().replace("-", "");

        SeckillMessage message = null;
        try {
            // 2.1 先插入本地消息表（状态=INIT）
            message = saveMessageWithTransaction(bizId, seckillId, userPhone, seckillGoods);
            log.info("本地消息表插入成功: id={}, bizId={}", message.getId(), bizId);
        } catch (Exception e) {
            // 本地消息表插入失败 → 需要补偿Redis库存（因为库存已扣，但消息没落库）
            log.error("插入本地消息表失败，开始补偿Redis库存: seckillId={}", seckillId, e);
            // 补偿：把库存加回去（注意：这里可能造成超卖，需要结合业务判断）
            redisStockService.incrementStock(seckillId); // 需要实现这个补偿方法
            throw new RuntimeException("下单失败，请重试");
        }

        long t3 = System.currentTimeMillis();
        log.info("消息表插入耗时: {}ms", t3 - t2);

// 2.2 发送MQ消息
        try {
            SeckillOrderMessage mqMessage = new SeckillOrderMessage(
                    bizId, seckillId, userPhone, seckillGoods.getSeckillPrice()
            );
            mqSender.sendSeckillOrder(mqMessage);
            log.info("MQ消息已发送: bizId={}", bizId);

            // 2.3 MQ发送成功，更新本地消息状态为 SENT（可以用同一个事务，或独立更新）
            // 推荐：直接在这个方法里更新，或者在saveMessageWithTransaction中插入时就设置状态为SENT
//            messageMapper.markSent(message.getId());

        } catch (Exception e) {
            // MQ发送失败 → 消息状态保持 INIT，由定时补偿任务重试
            log.error("MQ发送失败，等待补偿任务重试: bizId={}", bizId, e);
            throw new RuntimeException("模拟失败");//测试失败
            // 这里不要抛异常，因为消息已经在本地表里了，补偿任务会处理！！！！
            // 但可以告警或记录特殊日志
        }
        long t4 = System.currentTimeMillis();
        log.info("MQ 发送耗时: {}ms", t4 - t3);
        /*// ==========  生成业务唯一ID ==========
        String bizId = UUID.randomUUID().toString().replace("-", "");

        // ==========  插入本地消息表（状态=INIT） ==========
        // ========== 第3步：使用独立事务插入本地消息表 ==========
        // 注意：这里调用一个带 @Transactional 的方法
        SeckillMessage message =  saveMessageWithTransaction(bizId, seckillId, userPhone, seckillGoods);

        // 3. 发送 MQ 消息异步落库（不等待结果，直接返回）
        SeckillOrderMessage mqMessage = new SeckillOrderMessage(
                bizId,
                seckillId,
                userPhone,
                seckillGoods.getSeckillPrice()
        );
        mqSender.sendSeckillOrder(mqMessage);
        //MQ 发送成功，更新 本地消息状态为MQ Sent
        messageMapper.markSent(message.getId());

        log.info("MQ消息已发送: bizId={}", bizId);*/

        // 4. 为了兼容返回类型，构造一个临时订单对象返回（真实订单由 MQ 异步生成）
        //    实际业务中可返回 "排队中" 状态，让前端轮询查询订单结果
        SeckillOrder tempOrder = new SeckillOrder();
        tempOrder.setSeckillId(seckillId);
        tempOrder.setUserPhone(userPhone);
        tempOrder.setOrderAmount(seckillGoods.getSeckillPrice());
        tempOrder.setStatus(-1);//-1 表示排队中
        tempOrder.setCreateTime(LocalDateTime.now(Clock.systemUTC()));
        // 注意：此时 orderId 还未生成，因为还没落库


        return tempOrder;
    }

    public LocalDateTime getTime(Long seckillGoodId){
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillGoodId);
        log.info("数据库读出endTime：{}", goods.getEndTime());
        return goods.getEndTime();
    }

    /**
     * 独立事务：只负责插入本地消息表
     * 使用 @Transactional 确保插入失败时回滚（但 Redis 不会回滚）
     */
    @Transactional(rollbackFor = Exception.class)
    public SeckillMessage saveMessageWithTransaction(String bizId, Long seckillId,
                                           Long userPhone, SeckillGoods goods) {
        // 1. 幂等性检查（防止重复插入）
       /* 优化方案：利用数据库唯一索引 + 异常捕获
        核心思路是直接插入，通过数据库的唯一索引来保证幂等性，避免“查一次再插一次”。*/

      /*  SeckillMessage existMsg = messageMapper.selectOne(
                new LambdaQueryWrapper<SeckillMessage>()
                        .eq(SeckillMessage::getBizId, bizId)
        );
        if (existMsg != null) {
            log.warn("消息已存在，bizId={}, status={}", bizId, existMsg.getStatus());
            return existMsg;
        }*/

        SeckillMessage message = new SeckillMessage();
        message.setBizId(bizId);
        message.setSeckillId(seckillId);
        message.setUserPhone(userPhone);
        message.setOrderAmount(goods.getSeckillPrice());
        message.setStatus(0);
        message.setRetryCount(0);
        message.setMaxRetry(10);
        message.setNextRetryTime(LocalDateTime.now(Clock.systemUTC()).plusSeconds(30));
        try {
            messageMapper.insert(message);
            log.info("本地消息表记录成功: bizId={}", bizId);
            return message;
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突 → 说明消息已存在 → 直接查询返回
            log.warn("消息已存在（唯一索引冲突），bizId={}", bizId);
            SeckillMessage existMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<SeckillMessage>()
                            .eq(SeckillMessage::getBizId, bizId)
            );
            if (existMsg == null) {
                // 极端情况：索引冲突但查不到数据（理论上不会发生）
                throw new RuntimeException("消息插入失败，且查询不到已有记录: " + bizId, e);
            }
            return existMsg;
        }
       /* messageMapper.insert(message);
        log.info("本地消息表记录成功: bizId={}", bizId);
        log.info("*****刚插入的本地消息记录： {}", message);
        return message;*/
    }
}