package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/17 17:09
 * @description:
 */
public class SeckillEndException extends BusinessException{
    public SeckillEndException() {
        super(ResultCode.SECKILL_END);
    }

    public SeckillEndException(String message) {
        super(ResultCode.SECKILL_END, message);
    }

    public SeckillEndException(Long seckillId, String endTime) {
        super(ResultCode.SECKILL_END, "秒杀活动已结束，ID: " + seckillId + ", 结束时间: " + endTime);
    }
}
