-- 修改单个字段注释
ALTER TABLE `t_seckill_message`
MODIFY COLUMN `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-INIT，1-SENT, 2-SUCCESS，3-FAILED';



-- 1、订单表新增两个字段
ALTER TABLE t_seckill_order
ADD COLUMN biz_id varchar(64) NOT NULL COMMENT '业务唯一ID（订单号）' AFTER order_id,
ADD UNIQUE KEY uk_biz_id (biz_id);
