package com.demo.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dto.ResetStockMessage;
import com.demo.entity.SeckillGoods;
import com.demo.exception.GoodNotFoundException;
import com.demo.mapper.SeckillGoodsMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
    private RedisTemplate<String, Object> redisObjectTemplate;  // 用于存对象

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private MQSender mqSender;

    // 库存 Key 前缀
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    // 布隆过滤器Key
    private static final String BLOOM_FILTER_KEY = "bloom:seckill:goods";

    private static final String GOODS_CACHE_PREFIX = "seckill:goods:";

    @PostConstruct
    public void init(){
        log.info("========== 开始初始化 Redis 数据 ==========");
       initBloomFilter();
       initStock();
        log.info("========== 初始化 Redis 数据 完成 ==========");
    }

    /**
     * 项目启动时初始化布隆过滤器（加载所有有效商品ID）
     */

    public void initBloomFilter() {
        // 1. 从数据库查询所有有效的秒杀商品ID
        List<Long> seckillIds = seckillGoodsMapper.selectAllValidIds();
        if (seckillIds == null || seckillIds.isEmpty()) {
            System.out.println("没有需要加载的秒杀商品ID");
            return;
        }

        // 2. 将商品ID加载到布隆过滤器
        // 注意：Redisson的RBloomFilter不支持直接存储Long，需要转为String
        for (Long id : seckillIds) {
            redisTemplate.opsForSet().add(BLOOM_FILTER_KEY, id.toString());
        }
        System.out.println("布隆过滤器加载完成，共加载 " + seckillIds.size() + " 个商品ID");
    }

    /**
     * 判断商品ID是否可能存在于布隆过滤器中
     * @return true=可能存在，false=一定不存在
     */
    public boolean bloomContains(Long seckillId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(BLOOM_FILTER_KEY, seckillId.toString())
        );
    }

    /*
    try to get seckill goods from db instead of hard code
    */

    public void initStock() {
        // get seckill goods list from database where the version is 0 and end_time is greater than now

        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SeckillGoods::getEndTime, now);
//                .eq(SeckillGoods::getVersion, 0);
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

//            redisTemplate.expire(key, 10, TimeUnit.SECONDS);
            log.info("redis set key {} value NX result is {}", key, success);
            if (Boolean.TRUE.equals(success)) {
                log.info("库存预热成功：seckillId={}, stock={}", goods.getSeckillId(), stockNumber);
            }

            cacheGoodsInfo(goods);
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
//        String value = redisTemplate.opsForValue().get(key);
// 引入 private RedisTemplate<String, Object> redisTemplate; 之后用下面的代码
        String value = (String) redisTemplate.opsForValue().get(key);

        return value == null ? null : Long.valueOf(value);
    }


    public void resetSeckillStockAsync(Long seckillId, Integer stockNumber) {
        // 1. 先更新 Redis（快速生效）
        String key = STOCK_KEY_PREFIX + seckillId;
        redisTemplate.opsForValue().set(key, stockNumber.toString());

        // 2. 发送异步消息，由消费者更新数据库
        mqSender.sendResetMessage(seckillId, stockNumber);
    }
/*
    // 对外提供重置库存接口，运营/定时任务调用
    public void resetSeckillStock(Long seckillId, Integer stockNumber) {
        String key = STOCK_KEY_PREFIX + seckillId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            //给已经存在Redis 离得key 重置库存数量
            redisTemplate.opsForValue().set(key, stockNumber.toString());
        }
    }*/
    //改进的重置库存方法
    @Transactional
    public void resetSeckillStock(Long seckillId, Integer stockNumber) {
        // 1. 更新数据库
        int affected = seckillGoodsMapper.updateStock(seckillId, stockNumber);
        if (affected == 0) {
            throw new GoodNotFoundException("商品不存在，重置失败");
        }

        // 2. 更新 Redis
        try {
            String key = STOCK_KEY_PREFIX + seckillId;
            redisTemplate.opsForValue().set(key, stockNumber.toString());
            log.info("库存重置完成: seckillId={}, stock={}", seckillId, stockNumber);
        } catch (Exception e) {
            log.error("Redis 更新失败，请检查缓存一致性: seckillId={}", seckillId, e);
            // 这里不抛异常，因为 DB 已经更新成功了
            // 可以发送告警，或者将失败记录写入本地表，由定时任务补偿
        }
    }

    /**
     * 缓存商品完整信息（带随机过期时间，防雪崩）
     */
    public void cacheGoodsInfo(SeckillGoods goods) {
        String key = GOODS_CACHE_PREFIX + goods.getSeckillId();
        // 过期时间 5-10 分钟，随机值防雪崩
        int randomTtl = 300 + ThreadLocalRandom.current().nextInt(300);
        // 直接用 String 结构存储整个对象（简单高效）
        redisObjectTemplate.opsForValue().set(key, goods, randomTtl, TimeUnit.SECONDS);
/*
        redisObjectTemplate.opsForHash().put(key, "info", goods);
        redisObjectTemplate.expire(key, randomTtl, TimeUnit.SECONDS);*/
//        redisTemplate.opsForValue().set(key, goods, randomTtl, TimeUnit.SECONDS);
        log.info("商品信息缓存成功: seckillId={}, ttl={}s", goods.getSeckillId(), randomTtl);
    }

    /**
     * 从缓存获取商品完整信息
     */
    public SeckillGoods getGoodsFromCache(Long seckillId) {
        String key = GOODS_CACHE_PREFIX + seckillId;
//        Object obj = redisObjectTemplate.opsForHash().get(key, "info");
        Object obj = redisObjectTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        // 如果已经正确配置了序列化器，这里可以直接强转
        return (SeckillGoods) obj;
    }

    /**
     * 优先从缓存获取，缓存不存在则查数据库（防击穿）
     */
    public SeckillGoods getGoodsWithCache(Long seckillId) {
        // 1. 查缓存
        SeckillGoods goods = getGoodsFromCache(seckillId);
        if (goods != null) {
            return goods;
        }

        // 2. 缓存未命中 → 查数据库（带互斥锁防击穿）
        return getGoodsWithMutexLock(seckillId);
    }

    /**
     * 互斥锁重建缓存（防击穿）
     */
    private SeckillGoods getGoodsWithMutexLock(Long seckillId) {
        String lockKey = "lock:goods:" + seckillId;
        String cacheKey = GOODS_CACHE_PREFIX + seckillId;

        try {
            // 尝试获取锁，5秒超时
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 双重检查
                    SeckillGoods goods = getGoodsFromCache(seckillId);
                    if (goods != null) {
                        return goods;
                    }

                    // 查数据库
                    goods = seckillGoodsMapper.selectById(seckillId);
                    if (goods != null) {
                        cacheGoodsInfo(goods);
                    }
                    return goods;
                } finally {
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 等待其他线程重建缓存
                Thread.sleep(50);
                return getGoodsFromCache(seckillId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return seckillGoodsMapper.selectById(seckillId);
        }
    }

}
