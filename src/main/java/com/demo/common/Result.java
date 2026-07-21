package com.demo.common;


import com.demo.enums.ResultCode;
import lombok.Data;

/**
 * @author: Linda
 * @date: 2026/7/17 16:48
 * @description:
 */
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    // ✅ 私有构造器，强制使用静态工厂方法
    private Result() {}

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    public static <T> Result<T> error(ResultCode resultCode, String detailMsg) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg() + ": " + detailMsg);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

}