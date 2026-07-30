package com.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetStockMessage implements Serializable {
    private Long seckillId;
    private Integer stockNumber;
}