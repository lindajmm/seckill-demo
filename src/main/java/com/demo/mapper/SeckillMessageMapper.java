package com.demo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.entity.SeckillMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SeckillMessageMapper extends BaseMapper<SeckillMessage> {

    /**
     * 根据业务ID查询消息
     */
    @Select("SELECT * FROM t_seckill_message WHERE biz_id = #{bizId}")
    SeckillMessage selectByBizId(@Param("bizId") String bizId);

    /**
     * 查询需要重试的消息
     * 条件：状态为初始化、下次重试时间已到、重试次数未达上限
     */
  /*  @Select("SELECT * FROM t_seckill_message " +
            "WHERE status = 0 " +
            "AND next_retry_time <= NOW() " +
            "AND retry_count < max_retry " +
            "ORDER BY create_time ASC " +
            "LIMIT 100")
    List<SeckillMessage> selectPendingRetryMessages();*/


    /**
     * 查询需要重试的消息（状态为INIT且不超过下次重试时间）
     */
    @Select("SELECT * FROM t_seckill_message WHERE status IN (0) " +
            "AND next_retry_time <= NOW() A" +
            "ND retry_count < max_retry " +
            "ORDER BY next_retry_time ASC LIMIT #{limit}")
    List<SeckillMessage> selectPendingRetryMessages(@Param("limit") int limit);

    /**
     * 增加重试次数，并更新下次重试时间（30秒后）
     */
    @Update("UPDATE t_seckill_message " +
            "SET retry_count = retry_count + 1, " +
            "    next_retry_time = DATE_ADD(NOW(), INTERVAL 30 SECOND),error_msg = #{errorMsg} " +
            "WHERE id = #{id} AND status = 0")
    int incrementRetry(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /**
     * 标记消息为成功
     */
    @Update("UPDATE t_seckill_message SET status = 2 WHERE id = #{id}")
    int markSuccess(@Param("id") Long id);

    /**
     * 标记消息为MQ sent
     */
    @Update("UPDATE t_seckill_message SET status = 1 WHERE id = #{id}")
    int markSent(@Param("id") Long id);

    /**
     * 标记消息为失败（重试超限）
     */
    @Update("UPDATE t_seckill_message SET status = 3, error_msg = #{errorMsg} " +
            "WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);
}