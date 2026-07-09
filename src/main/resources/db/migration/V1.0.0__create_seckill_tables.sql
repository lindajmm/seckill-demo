
-- 1. 商品表
CREATE TABLE `t_goods` (
  `goods_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `goods_name` varchar(120) NOT NULL COMMENT '商品名称',
  `price` decimal(10,2) NOT NULL COMMENT '商品原价',
  `stock` int(11) NOT NULL COMMENT '普通库存（秒杀场景下，主要关注秒杀库存）',
  PRIMARY KEY (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通商品表';

-- 2. 秒杀商品表（关联普通商品，新增秒杀特有属性）
CREATE TABLE `t_seckill_goods` (
  `seckill_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀商品ID',
  `goods_id` bigint(20) NOT NULL COMMENT '关联的商品ID',
  `seckill_price` decimal(10,2) NOT NULL COMMENT '秒杀价格',
  `seckill_stock` int(11) NOT NULL COMMENT '秒杀库存',
  `start_time` datetime NOT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NOT NULL COMMENT '秒杀结束时间',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本号，用于后续防止超卖',
  PRIMARY KEY (`seckill_id`),
  INDEX `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 3. 秒杀订单表
CREATE TABLE `t_seckill_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `user_phone` bigint(20) NOT NULL COMMENT '用户手机号（为简化设计，直接存手机号）',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀商品ID',
  `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-支付成功，-1-无效/超时',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`order_id`),
  INDEX `idx_user_phone` (`user_phone`),
  INDEX `idx_seckill_id` (`seckill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';