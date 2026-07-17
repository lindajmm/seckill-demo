package com.demo.service.impl;


import com.demo.dto.SeckillOrderMessage;
import com.demo.entity.SeckillGoods;
import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillGoodsMapper;
import com.demo.service.MQSender;
import com.demo.service.RedisStockService;
import com.demo.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SeckillServiceImpl implements SeckillService {
    private final static Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);


    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisStockService redisStockService;
    @Autowired
    private MQSender mqSender;

    @Override
    public SeckillOrder doSeckill(Long seckillId, Long userPhone) {

        // 1. 查询秒杀商品信息（校验活动时间）
        SeckillGoods seckillGoods = seckillGoodsMapper.selectById(seckillId);
        if (seckillGoods == null) {
            throw new RuntimeException("秒杀商品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillGoods.getStartTime())) {
            throw new RuntimeException("秒杀尚未开始");
        }
        if (now.isAfter(seckillGoods.getEndTime())) {
            throw new RuntimeException("秒杀已结束");
        }

        // 2. Redis 原子扣减库存
        Long remainStock = redisStockService.decreaseStock(seckillId);
        if (remainStock == null || remainStock < 0) {
            // 库存不足，直接返回失败
            throw new RuntimeException("库存不足");
        }
        log.info("Redis 扣库存成功，剩余库存: {}", remainStock);

        // 3. 发送 MQ 消息异步落库（不等待结果，直接返回）
        SeckillOrderMessage message = new SeckillOrderMessage(
                seckillId,
                userPhone,
                seckillGoods.getSeckillPrice()
        );
        mqSender.sendSeckillOrder(message);

        // 4. 为了兼容返回类型，构造一个临时订单对象返回（真实订单由 MQ 异步生成）
        //    实际业务中可返回 "排队中" 状态，让前端轮询查询订单结果
        SeckillOrder tempOrder = new SeckillOrder();
        tempOrder.setSeckillId(seckillId);
        tempOrder.setUserPhone(userPhone);
        tempOrder.setOrderAmount(seckillGoods.getSeckillPrice());
        tempOrder.setStatus(-1);//-1 表示排队中
        tempOrder.setCreateTime(LocalDateTime.now());
        // 注意：此时 orderId 还未生成，因为还没落库
        return tempOrder;
    }

    public LocalDateTime getTime(Long seckillGoodId){
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillGoodId);
        log.info("数据库读出endTime：{}", goods.getEndTime());
        return goods.getEndTime();
    }
}