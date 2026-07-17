package com.demo.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.entity.SeckillGoods;
import com.demo.mapper.SeckillGoodsMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author: Linda
 * @date: 2026/7/9 11:48
 * @description:
 */
@Service
public class RedisStockService {
    private final static Logger log = LoggerFactory.getLogger(RedisStockService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    // 库存 Key 前缀
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /*
    try to get seckill goods from db instead of hard code
    */
    @PostConstruct
    public void initStock() {
        // get seckill goods list from database where the version is 0 and end_time is greater than now

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SeckillGoods::getEndTime, now)
                .eq(SeckillGoods::getVersion, 0);
        List<SeckillGoods> activeList = seckillGoodsMapper.selectList(wrapper);
      /*  for(SeckillGoods seckillGoods : activeList){
            String key = STOCK_KEY_PREFIX + seckillGoods.getGoodsId();
            // 先检查 Redis 中是否已有该 key，避免重复预热导致数据混乱
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().set(key,  String.valueOf(seckillGoods.getSeckillStock()));
                log.info("库存预热成功：seckillId=1, stock="+  seckillGoods.getSeckillStock());
            }
        }

        activeList.forEach(e -> {
            String key = STOCK_KEY_PREFIX + e.getGoodsId();
            // 先检查 Redis 中是否已有该 key，避免重复预热导致数据混乱
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().set(key,  String.valueOf(e.getSeckillStock()));
                log.info("库存预热成功：seckillId=1, stock="+  e.getSeckillStock());
            }
        });
*/

        activeList.forEach(goods -> {
            String key = STOCK_KEY_PREFIX + goods.getSeckillId();
            String stockNumber = goods.getSeckillStock().toString();
            // SET key value NX，原子操作
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, stockNumber);
            if (Boolean.TRUE.equals(success)) {
                log.info("库存预热成功：seckillId={}, stock={}", goods.getSeckillId(), stockNumber);
            }
        });


       /* String key = STOCK_KEY_PREFIX + "1";
        String stockNumber = "10";
        // 先检查 Redis 中是否已有该 key，避免重复预热导致数据混乱
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, stockNumber);
            log.info("库存预热成功：seckillId=1, stock="+ stockNumber);
        }*/
    }

    /**
     * 项目启动时预热库存：将秒杀商品的库存加载到 Redis
     * 实际场景中可从数据库读取所有秒杀商品，这里先手动指定
     */
 /*   @PostConstruct
    public void initStock() {
        // 假设 seckill_id = 1 的商品，初始库存为 10
        String key = STOCK_KEY_PREFIX + "1";
        String stockNumber = "10";
        // 先检查 Redis 中是否已有该 key，避免重复预热导致数据混乱
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, stockNumber);
            log.info("库存预热成功：seckillId=1, stock="+ stockNumber);
        }else{
            String value = redisTemplate.opsForValue().get(key);
            log.info(String.format("key %s , value is %s", key, value));
        }
    }*/

    // 对外提供重置库存接口，运营/定时任务调用
    public void resetSeckillStock(Long seckillId, Integer stockNumber) {
        String key = STOCK_KEY_PREFIX + seckillId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            //给已经存在Redis 离得key 重置库存数量
            redisTemplate.opsForValue().set(key, stockNumber.toString());
        }
    }



    /**
     * 使用 Lua 脚本原子性扣减库存
     * 返回值：>0 表示扣减成功（返回剩余库存），<0 表示库存不足
     */
    public Long decreaseStock(Long seckillId) {
        String key = STOCK_KEY_PREFIX + seckillId;
        // Lua 脚本：先 GET 当前库存，如果 >0 则 DECR，否则返回 -1
        String luaScript =
                "local current = redis.call('GET', KEYS[1]) " +
                        "if current and tonumber(current) > 0 then " +
                        "    return redis.call('DECR', KEYS[1]) " +
                        "else " +
                        "    return -1 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        return redisTemplate.execute(redisScript, Collections.singletonList(key));
    }

    /**
     * 库存回滚（用于业务失败时，比如 MQ 发送失败，把库存加回去）
     */
    public void incrementStock(Long seckillId) {
        String key = STOCK_KEY_PREFIX + seckillId;
        redisTemplate.opsForValue().increment(key);
    }

    /**
     * 获取当前 Redis 库存（用于压测验证）
     */
    public Long getCurrentStock(Long seckillId) {
        String key = STOCK_KEY_PREFIX + seckillId;
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? null : Long.valueOf(value);
    }
}
