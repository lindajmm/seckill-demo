package com.demo.controller;



import com.demo.common.Result;
import com.demo.common.SeckillMetrics;
import com.demo.entity.SeckillOrder;
import com.demo.enums.ResultCode;
import com.demo.service.SeckillService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private SeckillMetrics seckillMetrics;


   /* public SeckillController(MeterRegistry registry) {
        log.info("========== SeckillController 构造函数被执行了！ ==========");
        log.info("当前时间：{}", LocalDateTime.now());
        // 创建两个计数器：秒杀成功计数、秒杀失败计数
        this.successCounter = Counter.builder("seckill.request.success")
                .description("秒杀成功次数")
                .register(registry);
        this.failureCounter = Counter.builder("seckill.request.failure")
                .description("秒杀失败次数")
                .register(registry);
    }*/

    /*
    ResponseEntity.status(HttpStatus.OK) 的 HTTP 状态码是"快递员是否把包裹送到了"，
    Result.code 是"包裹里的商品是否正确"。
    HTTP状态码让前端知道请求通了，业务code让前端知道业务成了。两者分工明确，缺一不可！
    */


    /**
     * 秒杀下单接口（V1.0基线版本）
     * @param seckillId 秒杀商品ID
     * 用户手机号固定为 13800138000（简化，后续可扩展）
     */
    @PostMapping("/{seckillId}")
    public ResponseEntity<Result<SeckillOrder>> seckill(@PathVariable Long seckillId) {
        log.info("秒杀活动开始, seckillId: {}", seckillId);
        Long userPhone = 13800138000L;
        SeckillOrder order = seckillService.doSeckill(seckillId, userPhone);
        seckillMetrics.incrementSuccess();
//        return ResponseEntity.ok(Result.success(order));
        return ResponseEntity.status(ResultCode.SUCCESS.getHttpStatus())
                .body(Result.success(order));
    }
 /*   public ResponseEntity<Result<SeckillOrder>> seckill(@PathVariable Long seckillId) {
        Map<String, Object> result = new HashMap<>();
//        log.info("秒杀活动开始");
        log.info("秒杀活动开始, seckillId: {}", seckillId);
        try {
            // 为简化，固定用户手机号，后续压测方便
            Long userPhone = 13800138000L;
            SeckillOrder order = seckillService.doSeckill(seckillId, userPhone);
            result.put("code", 0);
            result.put("msg", "秒杀成功");
            result.put("data", order);

            // 成功计数 +1
            successCounter.increment();
            return ResponseEntity.ok(Result.success(order));
        } catch (Exception e) {
            result.put("code", -1);
            result.put("msg", e.getMessage());

            // 失败计数 +1
            failureCounter.increment();
        }
        return result;
    }*/



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
