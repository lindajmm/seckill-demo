package com.demo.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author: Linda
 * @date: 2026/7/9 11:54
 * @description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {
    private Long seckillId;
    private Long userPhone;
    private BigDecimal orderAmount;
}
