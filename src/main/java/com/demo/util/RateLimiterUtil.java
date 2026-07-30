package com.demo.util;


import com.demo.service.impl.SeckillServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterUtil {
    private final static Logger log = LoggerFactory.getLogger(RateLimiterUtil.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 令牌桶限流（基于 Redis + Lua）
     *
     * @param key           限流Key（如：rate:limit:user:13800138000:product:1）
     * @param maxTokens     桶的最大令牌数（如：3）
     * @param refillRate    每秒补充的令牌数（如：3/60 = 0.05）
     * @param refillPeriod  补充周期（秒）
     * @param requestTokens 每次请求消耗的令牌数（通常为1）
     * @return true=允许请求，false=被限流
     */
    public boolean tryAcquire(String key, int maxTokens, int refillRate, int refillPeriod, int requestTokens) {
        // Lua脚本：令牌桶算法
        log.info("=== 限流请求 ===");
        log.info("key: " + key);
        log.info("maxTokens: " + maxTokens + ", refillRate: " + refillRate +
                ", refillPeriod: " + refillPeriod + ", requestTokens: " + requestTokens);
        // 返回 1 表示允许，0 表示拒绝
       /* String luaScript =
                "local key = KEYS[1] " +
                        "local maxTokens = tonumber(ARGV[1]) " +
                        "local refillRate = tonumber(ARGV[2]) " +
                        "local refillPeriod = tonumber(ARGV[3]) " +
                        "local requestTokens = tonumber(ARGV[4]) " +
                        "local currentTime = tonumber(ARGV[5]) " +
                        "" +
                        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime') " +
                        "local tokens = tonumber(bucket[1]) " +
                        "local lastRefillTime = tonumber(bucket[2]) " +
                        "" +
                        "-- 初始化桶 " +
                        "if tokens == nil then " +
                        "    tokens = maxTokens " +
                        "    lastRefillTime = currentTime " +
                        "end " +
                        "" +
                        "-- 计算应该补充的令牌数 " +
                        "local elapsedTime = currentTime - lastRefillTime " +
                        "local tokensToAdd = math.floor(elapsedTime * refillRate / refillPeriod) " +
                        "" +
                        "if tokensToAdd > 0 then " +
                        "    tokens = math.min(maxTokens, tokens + tokensToAdd) " +
                        "    lastRefillTime = currentTime " +
                        "end " +
                        "" +
                        "-- 尝试消耗令牌 " +
                        "if tokens >= requestTokens then " +
                        "    tokens = tokens - requestTokens " +
                        "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    redis.call('EXPIRE', key, refillPeriod + 1) " +
                        "    return 1 " +
                        "else " +
                        "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    redis.call('EXPIRE', key, refillPeriod + 1) " +
                        "    return 0 " +
                        "end";*/

//        String luaScript = "return 1";
        // ========== 带错误处理的 Lua 脚本 ==========
      /*  String luaScript =
                "local key = KEYS[1] " +
                        "local maxTokens = tonumber(ARGV[1]) " +
                        "local refillRate = tonumber(ARGV[2]) " +
                        "local refillPeriod = tonumber(ARGV[3]) " +
                        "local requestTokens = tonumber(ARGV[4]) " +
                        "local currentTime = tonumber(ARGV[5]) " +
                        "" +
                        "-- 使用 pcall 捕获 Redis 命令异常 " +
                        "local function safeCall(func, ...) " +
                        "    local result = {pcall(func, ...)} " +
                        "    if not result[1] then " +
                        "        return nil, result[2] " +
                        "    end " +
                        "    return result[2] " +
                        "end " +
                        "" +
                        "-- 获取当前桶状态 " +
                        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime') " +
                        "local tokens = tonumber(bucket[1]) " +
                        "local lastRefillTime = tonumber(bucket[2]) " +
                        "" +
                        "-- 初始化桶 " +
                        "if tokens == nil then " +
                        "    tokens = maxTokens " +
                        "    lastRefillTime = currentTime " +
                        "end " +
                        "" +
                        "-- 计算应该补充的令牌数 " +
                        "local elapsedTime = currentTime - lastRefillTime " +
                        "local tokensToAdd = math.floor(elapsedTime * refillRate / refillPeriod) " +
                        "" +
                        "if tokensToAdd > 0 then " +
                        "    tokens = math.min(maxTokens, tokens + tokensToAdd) " +
                        "    lastRefillTime = currentTime " +
                        "end " +
                        "" +
                        "-- 尝试消耗令牌 " +
                        "if tokens >= requestTokens then " +
                        "    tokens = tokens - requestTokens " +
                        "    local ok, err = safeCall(redis.call, 'HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    if not ok then " +
                        "        return -1 " +
                        "    end " +
                        "    local ok2, err2 = safeCall(redis.call, 'EXPIRE', key, refillPeriod + 1) " +
                        "    if not ok2 then " +
                        "        return -2 " +
                        "    end " +
                        "    return 1 " +
                        "else " +
                        "    local ok, err = safeCall(redis.call, 'HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    if not ok then " +
                        "        return -3 " +
                        "    end " +
                        "    local ok2, err2 = safeCall(redis.call, 'EXPIRE', key, refillPeriod + 1) " +
                        "    if not ok2 then " +
                        "        return -4 " +
                        "    end " +
                        "    return 0 " +
                        "end";*/


        // ========== 健壮版 Lua 脚本 ==========
        String luaScript =
                "local key = KEYS[1] " +
                        "local maxTokens = tonumber(ARGV[1]) " +
                        "local refillRate = tonumber(ARGV[2]) " +
                        "local refillPeriod = tonumber(ARGV[3]) " +
                        "local requestTokens = tonumber(ARGV[4]) " +
                        "local currentTime = tonumber(ARGV[5]) " +
                        "" +
                        "-- 检查参数是否合法 " +
                        "if maxTokens == nil or refillRate == nil or refillPeriod == nil or requestTokens == nil or currentTime == nil then " +
                        "    return -999 " +
                        "end " +
                        "" +
                        "-- 获取当前桶状态（使用 HMGET，Key 不存在时返回 {nil, nil}） " +
                        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime') " +
                        "local tokens = bucket[1] " +
                        "local lastRefillTime = bucket[2] " +
                        "" +
                        "-- 转换为数字，nil 转为 0 " +
                        "if tokens == nil then " +
                        "    tokens = maxTokens " +
                        "    lastRefillTime = currentTime " +
                        "else " +
                        "    tokens = tonumber(tokens) or maxTokens " +
                        "    lastRefillTime = tonumber(lastRefillTime) or currentTime " +
                        "end " +
                        "" +
                        "-- 计算补充的令牌数 " +
                        "local elapsedTime = currentTime - lastRefillTime " +
                        "if elapsedTime < 0 then " +
                        "    elapsedTime = 0 " +
                        "end " +
                        "local tokensToAdd = math.floor(elapsedTime * refillRate / refillPeriod) " +
                        "" +
                        "if tokensToAdd > 0 then " +
                        "    tokens = tokens + tokensToAdd " +
                        "    if tokens > maxTokens then " +
                        "        tokens = maxTokens " +
                        "    end " +
                        "    lastRefillTime = currentTime " +
                        "end " +
                        "" +
                        "-- 尝试消耗令牌 " +
                        "if tokens >= requestTokens then " +
                        "    tokens = tokens - requestTokens " +
                        "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    redis.call('EXPIRE', key, refillPeriod + 1) " +
                        "    return 1 " +
                        "else " +
                        "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime) " +
                        "    redis.call('EXPIRE', key, refillPeriod + 1) " +
                        "    return 0 " +
                        "end";
// lua 脚本执行报错，返回null!!! 不能用
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        List<String> keys = Collections.singletonList(key);
        long currentTime = System.currentTimeMillis() / 1000;  // 秒级时间戳
        try {
            Long result = redisTemplate.execute(
                    redisScript,
                    keys,
                    String.valueOf(maxTokens),
                    String.valueOf(refillRate),
                    String.valueOf(refillPeriod),
                    String.valueOf(requestTokens),
                    String.valueOf(currentTime)
            );


            // 限流后打印
            log.info("result: " + result);
            log.info("=== 限流结束 ===");

            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("执行lua 抛异常：", e);
        }

        return false;
    }

    public boolean tryAcquireSimple(String key, int maxTokens, int windowSeconds) {
        String luaScript =
                "local key = KEYS[1] " +
                        "local maxTokens = tonumber(ARGV[1]) " +
                        "local windowSeconds = tonumber(ARGV[2]) " +
                        "local currentTime = tonumber(ARGV[3]) " +
                        "" +
                        "-- 移除过期的请求 " +
                        "local minTime = currentTime - windowSeconds " +
                        "redis.call('ZREMRANGEBYSCORE', key, 0, minTime) " +
                        "" +
                        "-- 统计当前窗口内的请求数 " +
                        "local count = redis.call('ZCARD', key) " +
                        "" +
                        "if count < maxTokens then " +
                        "    redis.call('ZADD', key, currentTime, currentTime .. ':' .. math.random()) " +
                        "    redis.call('EXPIRE', key, windowSeconds + 1) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        long currentTime = System.currentTimeMillis() / 1000;

        try {
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(maxTokens),
                    String.valueOf(windowSeconds),
                    String.valueOf(currentTime)
            );
            log.info("限流结果: {}", result);
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("限流执行失败", e);
            return true;
        }
    }


    /**
     * 滑动窗口限流（纯 Java + Redis 命令）
     *
     * @param key       限流Key
     * @param maxTokens 最大请求数
     * @param windowSeconds 窗口时间（秒）
     * @return true=允许，false=被限流
     */
    public boolean tryAcquire002(String key, int maxTokens, int windowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSeconds * 1000L;

        try {
            // 1. 移除窗口外的旧数据
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // 2. 统计当前窗口内的请求数
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count == null) {
                count = 0L;
            }

            // 3. 判断是否允许通过
            if (count < maxTokens) {
                // 4. 添加当前请求
                String member = String.valueOf(currentTime) + ":" + UUID.randomUUID().toString();
                redisTemplate.opsForZSet().add(key, member, currentTime);
                // 5. 设置过期时间（窗口时间 + 1 秒）
                redisTemplate.expire(key, windowSeconds + 1, TimeUnit.SECONDS);
                log.info("限流通过: key={}, 当前窗口请求数={}", key, count + 1);
                return true;
            } else {
                log.warn("被限流: key={}, 当前窗口请求数={}", key, count);
                return false;
            }

        } catch (Exception e) {
            log.error("限流执行异常，降级允许通过", e);
            return true;  // 降级策略：异常时放行
        }
    }


    public boolean tryAcquireDoubao(String key, int maxTokens, int refillRate, int refillPeriod, int requestTokens) {
        // 使用毫秒时间戳，精度更高
        long currentMs = System.currentTimeMillis();
        // 兜底过期时间：1小时，避免冷数据频繁重置令牌桶
        final long bucketExpireSecond = 3600L;

        // Java15+ 文本块编写Lua，整洁不易出错
        String luaScript = """
                local key = KEYS[1]
                local maxTokens = tonumber(ARGV[1])
                local refillRate = tonumber(ARGV[2])
                local refillPeriod = tonumber(ARGV[3])
                local requestTokens = tonumber(ARGV[4])
                local currentMs = tonumber(ARGV[5])
                local bucketExpire = tonumber(ARGV[6])
                
                -- 读取存量令牌、上次刷新时间
                local bucketData = redis.call('HMGET', key, 'tokens', 'lastRefillMs')
                local tokens = tonumber(bucketData[1])
                local lastRefillMs = tonumber(bucketData[2])
                
                -- 桶初始化
                if tokens == nil or lastRefillMs == nil then
                    tokens = maxTokens
                    lastRefillMs = currentMs
                end
                
                -- 计算流逝毫秒数，换算成秒计算令牌补充量
                local elapsedMs = currentMs - lastRefillMs
                local elapsedSecond = elapsedMs / 1000.0
                -- 每秒生成令牌数 = 周期令牌数 / 周期秒数
                local tokenPerSecond = refillRate / refillPeriod
                -- 可补充令牌（保留小数，不再向下取整）
                local addTokens = elapsedSecond * tokenPerSecond
                tokens = math.min(maxTokens, tokens + addTokens)
                
                -- 无论是否补充令牌，都更新上次刷新时间为当前时间（修复核心逻辑bug）
                lastRefillMs = currentMs
                
                local isPass = 0
                if tokens >= requestTokens then
                    tokens = tokens - requestTokens
                    isPass = 1
                end
                
                -- 统一写入数据 + 设置过期，消除重复代码
                redis.call('HMSET', key, 'tokens', tokens, 'lastRefillMs', lastRefillMs)
                redis.call('EXPIRE', key, bucketExpire)
                
                return isPass
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        List<String> keyList = Collections.singletonList(key);

        Long result = redisTemplate.execute(
                redisScript,
                keyList,
                String.valueOf(maxTokens),
                String.valueOf(refillRate),
                String.valueOf(refillPeriod),
                String.valueOf(requestTokens),
                String.valueOf(currentMs),
                String.valueOf(bucketExpireSecond)
        );
        return 1L == result;
    }
}