-- 插入普通商品
INSERT INTO `t_goods` (`goods_name`, `price`, `stock`)
VALUES ('iPhone 15 Pro Max', 9999.00, 100);

-- 插入秒杀商品（关联上面刚插入的商品，假设goods_id为1）
INSERT INTO `t_seckill_goods` (`goods_id`, `seckill_price`, `seckill_stock`, `start_time`, `end_time`, `version`)
VALUES (1, 7999.00, 10, '2026-07-06 00:00:00', '2026-07-07 23:59:59', 0);