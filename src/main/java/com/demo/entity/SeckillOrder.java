package com.demo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_seckill_order")
public class SeckillOrder {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    private Long userPhone;
    private Long seckillId;
    private BigDecimal orderAmount;
    private Integer status;
    private LocalDateTime createTime;
}
