package com.demo.controller;



import com.demo.entity.SeckillOrder;
import com.demo.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/seckill")
public class SeckillController {
    private final static Logger log = LoggerFactory.getLogger(SeckillController.class);


    @Autowired
    private SeckillService seckillService;

    /**
     * 秒杀下单接口（V1.0基线版本）
     * @param seckillId 秒杀商品ID
     * 用户手机号固定为 13800138000（简化，后续可扩展）
     */
    @PostMapping("/{seckillId}")
    public Map<String, Object> seckill(@PathVariable Long seckillId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 为简化，固定用户手机号，后续压测方便
            Long userPhone = 13800138000L;
            SeckillOrder order = seckillService.doSeckill(seckillId, userPhone);
            result.put("code", 0);
            result.put("msg", "秒杀成功");
            result.put("data", order);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("msg", e.getMessage());
        }
        return result;
    }



    @GetMapping("/endtime/{seckillGoodId}")
    public Map<String, Object> getTimeOfSeckillGoods(@PathVariable Long seckillGoodId) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDateTime time = seckillService.getTime(seckillGoodId);
            result.put("code", 0);
            result.put("msg", "查询时间成功");
            result.put("data", String.format("商品id %d 的 end time 是： %s ", seckillGoodId, time));
        } catch (Exception e) {
            result.put("code", -1);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}
