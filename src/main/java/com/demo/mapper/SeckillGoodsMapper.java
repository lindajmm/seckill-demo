package com.demo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 扣减秒杀库存（直接UPDATE，V1.0版本不做乐观锁优化）
     * 返回值为受影响的行数
     */
    @Update("UPDATE t_seckill_goods SET seckill_stock = seckill_stock - 1 " +
            "WHERE seckill_id = #{seckillId} AND seckill_stock > 0")
    int decreaseStock(@Param("seckillId") Long seckillId);

    /**
     * 使用乐观锁扣减数据库库存
     *
     * @param seckillId 秒杀商品ID
     * @param quantity  扣减数量
     * @return 受影响的行数（0表示库存不足或版本冲突）
     */
    @Update("UPDATE t_seckill_goods SET " +
            "seckill_stock = seckill_stock - #{quantity}, " +
            "version = version + 1 " +
            "WHERE seckill_id = #{seckillId} " +
            "AND seckill_stock >= #{quantity}")
    int decreaseStockWithVersion(@Param("seckillId") Long seckillId,
                                 @Param("quantity") Integer quantity);


    /**
     * 直接更新数据库库存（用于重置场景）
     */
    @Update("UPDATE t_seckill_goods SET seckill_stock = #{stockNumber}, version = 0 WHERE seckill_id = #{seckillId}")
    int updateStock(@Param("seckillId") Long seckillId, @Param("stockNumber") Integer stockNumber);


    /**
     * 查询所有有效的秒杀商品 ID（用于布隆过滤器预热）
     * 条件：当前时间在 start_time 和 end_time 之间
     */
    @Select("SELECT seckill_id FROM t_seckill_goods " +
            "WHERE start_time <= NOW() AND end_time >= NOW()")
    List<Long> selectAllValidIds();

}
