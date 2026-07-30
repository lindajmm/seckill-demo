package com.demo.enums;

import lombok.Getter;

@Getter
public enum MessageStatus {
    INIT(0, "初始化"),
    SENT(1, "已发送MQ"),
    SUCCESS(2, "消费成功"),
    FAILED(3, "最终失败");

    private int code;
    private String desc;

    MessageStatus(int code, String desc){
        this.code = code;
        this.desc = desc;
    }

}