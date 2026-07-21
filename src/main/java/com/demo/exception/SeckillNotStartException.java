package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/17 17:14
 * @description:
 */
public class SeckillNotStartException extends BusinessException{
    public SeckillNotStartException() {
        super(ResultCode.SECKILL_NOT_START);
    }

    public SeckillNotStartException(String message) {
        super(ResultCode.SECKILL_NOT_START, message);
    }

    public SeckillNotStartException(Long seckillId, String startTime) {
        super(ResultCode.SECKILL_NOT_START, "秒杀活动未开始，ID: " + seckillId + ", 开始时间: " + startTime);
    }
}
