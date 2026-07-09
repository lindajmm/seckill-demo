package com.demo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 扣减秒杀库存（直接UPDATE，V1.0版本不做乐观锁优化）
     * 返回值为受影响的行数
     */
    @Update("UPDATE t_seckill_goods SET seckill_stock = seckill_stock - 1 " +
            "WHERE seckill_id = #{seckillId} AND seckill_stock > 0")
    int decreaseStock(@Param("seckillId") Long seckillId);
}
