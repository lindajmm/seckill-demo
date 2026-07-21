package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/17 17:07
 * @description:
 */
public class SeckillNotFoundException extends BusinessException{
    public SeckillNotFoundException() {
        super(ResultCode.SECKILL_NOT_FOUND);
    }

    public SeckillNotFoundException(String message) {
        super(ResultCode.SECKILL_NOT_FOUND, message);
    }

    public SeckillNotFoundException(Long seckillId) {
        super(ResultCode.SECKILL_NOT_FOUND, "秒杀活动不存在，ID: " + seckillId);
    }
}
