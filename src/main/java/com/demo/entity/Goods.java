package com.demo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
/**
 * @author: Linda
 * @date: 2026/7/7 14:08
 * @description:
 */
@Data
@TableName("t_goods")
public class Goods {
    @TableId(type = IdType.AUTO)
    private Long goodsId;
    private String goodsName;
    private BigDecimal price;
    private Integer stock;
}
