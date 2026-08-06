package com.demo.service.impl;


import com.demo.entity.SeckillGoods;
import com.demo.entity.SeckillMessage;
import com.demo.entity.SeckillOrder;
import com.demo.mapper.SeckillGoodsMapper;
import com.demo.mapper.SeckillMessageMapper;
import com.demo.mapper.SeckillOrderMapper;
import com.demo.service.MQSender;
import com.demo.service.RedisStockService;
import com.demo.util.RateLimiterUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private SeckillMessageMapper seckillMessageMapper;

    @Mock
    private SeckillOrderMapper seckillOrderMapper;

    @Mock
    private RedisStockService redisStockService;

    @Mock
    private MQSender mqSender;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private RateLimiterUtil rateLimiterUtil;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    private SeckillGoods mockGoods;
    private final Long seckillId = 1L;
    private final Long userPhone = 13800138000L;

    @BeforeEach
    void setUp() {
        mockGoods = new SeckillGoods();
        mockGoods.setSeckillId(seckillId);
        mockGoods.setGoodsId(1L);
        mockGoods.setSeckillPrice(new BigDecimal("7999.00"));
        mockGoods.setSeckillStock(100);
        mockGoods.setVersion(0);
        mockGoods.setStartTime(LocalDateTime.now().minusHours(1));
        mockGoods.setEndTime(LocalDateTime.now().plusHours(1));
    }

    @Test
    void doSeckill_ShouldSuccess_WhenAllConditionsMet() throws Exception {
        // ========== Arrange（准备） ==========
        when(redisStockService.bloomContains(seckillId)).thenReturn(true);

        when(redisStockService.getGoodsWithCache(seckillId)).thenReturn(mockGoods);

        // 模拟分布式锁获取成功
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 模拟 Redis 扣库存成功
        when(redisStockService.decreaseStock(seckillId)).thenReturn(99L);

        // 模拟消息表插入成功
        when(seckillMessageMapper.insert(any(SeckillMessage.class))).thenReturn(1);

        // ========== Act（执行） ==========
        SeckillOrder result = seckillService.doSeckill(seckillId, userPhone);

        // ========== Assert（验证） ==========
        assertNotNull(result);
        assertEquals(seckillId, result.getSeckillId());
        assertEquals(userPhone, result.getUserPhone());
        assertEquals(-1, result.getStatus()); // -1 表示排队中

        // 验证 Redis 扣库存被调用
        verify(redisStockService, times(1)).decreaseStock(seckillId);

        // 验证 MQ 发送被调用
        verify(mqSender, times(1)).sendSeckillOrder(any());

        // 验证锁被释放
        verify(rLock, times(1)).unlock();
    }

    @Test
    void doSeckill_ShouldThrowException_WhenGoodsNotFound() {
        // ========== Arrange ==========
//        when(redisStockService.getGoodsWithCache(seckillId)).thenReturn(null);
//        doReturn(null).when(redisStockService).getGoodsWithCache(seckillId);
        // 改成 lenient 模式，抑制 UnnecessaryStubbingException
        lenient().when(redisStockService.getGoodsWithCache(seckillId)).thenReturn(null);

        // ========== Act & Assert ==========
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            seckillService.doSeckill(seckillId, userPhone);
        });

        assertEquals("秒杀活动不存在，ID: 1", exception.getMessage());

        // 验证后续操作未被调用
        verify(mqSender, never()).sendSeckillOrder(any());
    }
}