package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/17 17:10
 * @description:
 */
public class SeckillRepeatException extends BusinessException{
    public SeckillRepeatException() {
        super(ResultCode.SECKILL_REPEAT);
    }

    public SeckillRepeatException(String message) {
        super(ResultCode.SECKILL_REPEAT, message);
    }

    public SeckillRepeatException(Long userPhone, Long seckillId) {
        super(ResultCode.SECKILL_REPEAT, "用户已参与此秒杀活动, 用户: " + userPhone + ", 活动ID: " + seckillId);
    }
}
