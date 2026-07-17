package com.demo.service;


import com.demo.entity.SeckillOrder;

import java.time.LocalDateTime;

public interface SeckillService {

    /**
     * 执行秒杀下单
     * @param seckillId 秒杀商品ID
     * @param userPhone 用户手机号（为简化，直接传手机号）
     * @return 生成的订单对象
     */
    SeckillOrder doSeckill(Long seckillId, Long userPhone);

    public LocalDateTime getTime(Long seckillGoodId);
}
