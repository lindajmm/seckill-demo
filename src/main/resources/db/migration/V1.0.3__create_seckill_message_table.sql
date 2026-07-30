CREATE TABLE IF NOT EXISTS `t_seckill_message` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `biz_id` varchar(64) NOT NULL COMMENT '业务唯一ID',
    `seckill_id` bigint(20) NOT NULL COMMENT '秒杀商品ID',
    `user_phone` bigint(20) NOT NULL COMMENT '用户手机号',
    `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-初始化，1-成功，2-失败',
    `retry_count` int(11) NOT NULL DEFAULT '0' COMMENT '重试次数',
    `max_retry` int(11) NOT NULL DEFAULT '10' COMMENT '最大重试次数',
    `next_retry_time` datetime NOT NULL COMMENT '下次重试时间',
    `error_msg` varchar(500) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_id` (`biz_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀消息记录表';