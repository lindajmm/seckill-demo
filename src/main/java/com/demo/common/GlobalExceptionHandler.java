package com.demo.common;


import com.demo.enums.ResultCode;
import com.demo.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author: Linda
 * @date: 2026/7/17 16:48
 * @description:
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private SeckillMetrics seckillMetrics;

    /*!!!
    继承 BusinessException 就是为了统一处理，所以绝大多数情况下不需要为每个子异常单独写处理方法。
    一个 @ExceptionHandler(BusinessException.class) 就足够了。
    只有遇到特殊需求时（如特殊日志、特殊监控、特殊业务逻辑），才为特定子异常单独写方法。
    */
    @ExceptionHandler(SeckillNotFoundException.class)
    public ResponseEntity<Result<Void>> handleSeckillNotFound(SeckillNotFoundException e) {
        log.warn("秒杀活动不存在", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)  // 404
                .body(Result.error(ResultCode.SECKILL_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(SeckillNotStartException.class)
    public ResponseEntity<Result<Void>> handleSeckillNotStart(SeckillEndException e) {
        log.warn("秒杀未开始", e);
        return ResponseEntity
                .status(HttpStatus.GONE)  // 410
                .body(Result.error(ResultCode.SECKILL_NOT_START, e.getMessage()));
    }

    @ExceptionHandler(SeckillEndException.class)
    public ResponseEntity<Result<Void>> handleSeckillEnd(SeckillEndException e) {
        log.warn("秒杀已结束", e);
        return ResponseEntity
                .status(HttpStatus.GONE)  // 410
                .body(Result.error(ResultCode.SECKILL_END, e.getMessage()));
    }

    @ExceptionHandler(SeckillStockEmptyException.class)
    public ResponseEntity<Result<Void>> handleSeckillStockEmpty(SeckillStockEmptyException e) {
        log.warn("库存不足", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(Result.error(ResultCode.SECKILL_STOCK_EMPTY, e.getMessage()));
    }

    @ExceptionHandler(SeckillRepeatException.class)
    public ResponseEntity<Result<Void>> handleSeckillRepeat(SeckillRepeatException e) {
        log.warn("重复秒杀", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(Result.error(ResultCode.SECKILL_REPEAT, e.getMessage()));
    }

    /**
     * 处理业务异常 - 记录业务失败
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getResultCode().getCode(), e.getMessage());

        // ✅ 使用统一的监控组件
        seckillMetrics.incrementBusinessFailure();

      /*  // 根据异常类型返回不同状态码
        HttpStatus status = mapBusinessExceptionToHttpStatus(e);
        return ResponseEntity
                .status(status)
                .body(Result.error(e.getResultCode(), e.getMessage()));*/

        // 直接使用枚举中的状态码
        return ResponseEntity
                .status(e.getResultCode().getHttpStatus())
                .body(Result.error(e.getResultCode(), e.getMessage()));
    }

    /**
     * 处理系统异常 - 记录系统失败
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);

        // ✅ 使用统一的监控组件
        seckillMetrics.incrementSystemFailure();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.ERROR, "系统繁忙，请稍后重试"));
    }
}

