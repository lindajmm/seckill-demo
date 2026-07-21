package com.demo.enums;


import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author: Linda
 * @date: 2026/7/17 16:48
 * @description:
 */
@Getter
public enum ResultCode {
    // ========== 通用状态码 (0-999) ==========
    SUCCESS(0, "操作成功", HttpStatus.OK),
    ERROR(-1, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAM_ERROR(1001, "参数错误", HttpStatus.BAD_REQUEST),

    // ========== 秒杀业务状态码 (1000-1999) ==========
    SECKILL_FAIL(1001, "秒杀失败", HttpStatus.BAD_REQUEST),
    SECKILL_END(1002, "秒杀已结束", HttpStatus.GONE),
    SECKILL_NOT_FOUND(1003, "秒杀活动不存在", HttpStatus.NOT_FOUND),
    SECKILL_STOCK_EMPTY(1004, "库存不足", HttpStatus.CONFLICT),
    SECKILL_REPEAT(1005, "请勿重复秒杀", HttpStatus.BAD_REQUEST),
    SECKILL_NOT_START(1006, "秒杀尚未开始", HttpStatus.PRECONDITION_FAILED), // 412

    // ========== 用户业务状态码 (2000-2999) ==========
    USER_NOT_LOGIN(2001, "用户未登录", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND(2002, "用户不存在", HttpStatus.NOT_FOUND),
    USER_FROZEN(2003, "用户已被冻结", HttpStatus.FORBIDDEN),
    USER_INVALID(2004, "用户信息无效", HttpStatus.BAD_REQUEST),

    // ========== 订单业务状态码 (3000-3999) ==========
    ORDER_NOT_FOUND(3001, "订单不存在", HttpStatus.NOT_FOUND),
    ORDER_STATUS_ERROR(3002, "订单状态异常", HttpStatus.CONFLICT);

    private final Integer code;
    private final String msg;
    private final HttpStatus httpStatus;

    ResultCode(Integer code, String msg, HttpStatus httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }

    // 根据code查找枚举
    public static ResultCode fromCode(Integer code) {
        for (ResultCode resultCode : ResultCode.values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return null;
    }
    /*SUCCESS(0, "操作成功"),
    ERROR(-1, "系统异常"),
    SECKILL_FAIL(-1001, "秒杀失败"),
    SECKILL_END(-1002, "秒杀已结束"),
    SECKILL_NOT_FOUND(-1003, "秒杀活动不存在"),
    SECKILL_NOT_START(-1004, "秒杀已结束"),
    USER_NOT_LOGIN(1001, "用户未登录"),
    PARAM_ERROR(1002, "参数错误"),
    SECKILL_STOCK_EMPTY(1003, "seckill stock is empty"),
    SECKILL_REPEAT(1004, "no repeat seckill allowed");

    private final Integer code;
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // getter 省略*/
}
