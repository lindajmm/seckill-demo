package com.demo.exception;

import com.demo.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常基类
 * 所有自定义业务异常都继承此类
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;
    private final Object data;  // 可选：携带额外数据

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMsg(), cause);
        this.resultCode = resultCode;
        this.data = null;
    }

    // 如果需要携带额外数据
    public BusinessException(ResultCode resultCode, String message, Object data) {
        super(message);
        this.resultCode = resultCode;
        this.data = data;
    }
}