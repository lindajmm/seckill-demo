package com.demo.controller;


import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private SeckillOrderMapper orderMapper;

    /**
     * 根据 bizId 查询订单状态（前端轮询）,补充订单状态查询接口（供前端轮询）
     */
    @GetMapping("/status/{bizId}")
    public Map<String, Object> getOrderStatus(@PathVariable String bizId) {
        Map<String, Object> result = new HashMap<>();

        SeckillOrder order = orderMapper.selectByBizId(bizId);
        if (order == null) {
            result.put("status", "QUEUEING");
            result.put("msg", "订单处理中");
            return result;
        }

        result.put("status", "SUCCESS");
        result.put("orderId", order.getOrderId());
        result.put("orderStatus", order.getStatus());
        return result;
    }
}
