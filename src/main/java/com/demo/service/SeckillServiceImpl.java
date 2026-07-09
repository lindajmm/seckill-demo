package com.demo.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.entity.SeckillGoods;
import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillGoodsMapper;
import com.demo.mapper.SeckillOrderMapper;
import com.demo.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder doSeckill(Long seckillId, Long userPhone) {

        // 1. 查询秒杀商品信息
        SeckillGoods seckillGoods = seckillGoodsMapper.selectById(seckillId);
        if (seckillGoods == null) {
            throw new RuntimeException("秒杀商品不存在");
        }

        // 2. 校验活动时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillGoods.getStartTime())) {
            throw new RuntimeException("秒杀尚未开始");
        }
        if (now.isAfter(seckillGoods.getEndTime())) {
            throw new RuntimeException("秒杀已结束");
        }

        // 3. 校验并扣减库存（V1.0：直接用UPDATE扣减，无乐观锁）
        int affectedRows = seckillGoodsMapper.decreaseStock(seckillId);
        if (affectedRows == 0) {
            throw new RuntimeException("库存不足，秒杀失败");
        }

        // 4. 生成订单
        SeckillOrder order = new SeckillOrder();
        order.setUserPhone(userPhone);
        order.setSeckillId(seckillId);
        order.setOrderAmount(seckillGoods.getSeckillPrice());
        order.setStatus(0); // 0-待支付
        order.setCreateTime(LocalDateTime.now());

        seckillOrderMapper.insert(order);

        return order;
    }
}
