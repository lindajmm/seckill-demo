package com.demo.exception;


import com.demo.enums.ResultCode;

/**
 * @author: Linda
 * @date: 2026/7/23 12:14
 * @description:
 */
public class GoodNotFoundException extends BusinessException{
    public GoodNotFoundException() {
        super(ResultCode.GOOD_NOT_FOUND);
    }

    public GoodNotFoundException(String message) {
        super(ResultCode.GOOD_NOT_FOUND, message);
    }

    public GoodNotFoundException(Long seckillId) {
        super(ResultCode.GOOD_NOT_FOUND, "商品不存在，ID: " + seckillId);
    }
}
