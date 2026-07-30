package com.demo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_seckill_message")
public class SeckillMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private Long seckillId;
    private Long userPhone;
    private BigDecimal orderAmount;

    /**
     * 状态：0-INIT，1-SENT，2-SUCCESS, 3-FAILED
     */
    private Integer status;

    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryTime;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}