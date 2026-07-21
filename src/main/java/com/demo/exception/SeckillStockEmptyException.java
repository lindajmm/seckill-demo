package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/17 17:09
 * @description:
 */
public class SeckillStockEmptyException extends BusinessException{
    public SeckillStockEmptyException() {
        super(ResultCode.SECKILL_STOCK_EMPTY);
    }

    public SeckillStockEmptyException(String message) {
        super(ResultCode.SECKILL_STOCK_EMPTY, message);
    }

    public SeckillStockEmptyException(Long seckillId, Integer stock) {
        super(ResultCode.SECKILL_STOCK_EMPTY, "库存不足，秒杀ID: " + seckillId + ", 剩余库存: " + stock);
    }
}
