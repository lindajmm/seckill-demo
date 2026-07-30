package com.demo.controller;


import com.demo.common.Result;
import com.demo.entity.SeckillOrder;
import com.demo.enums.ResultCode;
import com.demo.service.RedisStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Linda
 * @date: 2026/7/16 16:04
 * @description:
 */
@RestController
@RequestMapping("/stock")
public class StockController {
    private final static Logger log = LoggerFactory.getLogger(StockController.class);


    @Autowired
    private RedisStockService redisStockService;

    /**
     *
     */
 /*   @PostMapping("/{seckillId}/{stockNumber}")
    public Map<String, Object> resetStock(@PathVariable Long seckillId, @PathVariable Integer stockNumber) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 为某一个秒杀商品重置库存
//            redisStockService.resetSeckillStock(seckillId, stockNumber);
            redisStockService.resetSeckillStockAsync(seckillId, stockNumber);
            result.put("code", 0);
            result.put("msg", "重置库存成功");
            result.put("data", String.format("商品id %d 的当前库存是： %d ", seckillId, stockNumber));
        } catch (Exception e) {
            result.put("code", -1);
            result.put("msg", e.getMessage());
        }
        return result;
    }*/

    @PostMapping("/{seckillId}/{stockNumber}")
    public ResponseEntity<Result<Long>> resetStock(@PathVariable Long seckillId, @PathVariable Integer stockNumber) {
        redisStockService.resetSeckillStock(seckillId, stockNumber);
        return ResponseEntity.status(ResultCode.SUCCESS.getHttpStatus())
                .body(Result.success(seckillId));
    }
}
