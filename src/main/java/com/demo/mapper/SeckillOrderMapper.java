package com.demo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
    @Select("SELECT * FROM t_seckill_order WHERE biz_id = #{bizId}")
    SeckillOrder selectByBizId(@Param("bizId") String bizId);
}
