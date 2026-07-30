# seckill-demo 分布式秒杀系统
## 项目简介
基于 SpringCloudAlibaba + Redis + RabbitMQ + Redisson 实现的高并发秒杀系统。
解决商品超卖、流量打垮数据库、请求恶意刷接口问题；使用布隆过滤器拦截无效请求、令牌桶限流、分布式锁保证库存原子扣减，异步削峰落地订单。

## 技术栈
### 后端核心
- 基础框架：SpringBoot 4.0.x、SpringCloud Alibaba
- ORM：MyBatis-Plus、Flyway（数据库版本管理）
- 缓存&分布式锁：Redis、Redisson、Lua脚本
- 消息队列：RabbitMQ（订单异步处理、流量削峰）
- 限流：Sentinel 流量防护、令牌桶算法
- 测试：JMeter 压测、JUnit
### 环境要求
- JDK：1.8 / 17
- Maven：3.6+
- MySQL：8.0
- Redis：6.0+
- RabbitMQ：3.9+
- 构建：Docker（可选容器化部署）

## 系统架构简图
客户端请求 → Nginx → 网关Gateway → 限流(Sentinel) → 布隆过滤器校验 → Redis预减库存 → Redisson分布式锁 → MQ异步创建订单 → MySQL落库

## 前置环境准备
1. 启动本地 MySQL
2. 本地启动 Redis，默认端口6379，密码 devlinda123456
3. 启动 RabbitMQ，创建对应交换机、队列（项目内置自动声明）
4. 修改 application.yml 数据库、Redis、MQ连接地址与账号密码

## 本地启动步骤
### 方式1：IDE 直接运行
1. 根目录 maven 刷新依赖：`mvn clean install -DskipTests`
2. 依次启动：Nacos注册中心 → Gateway网关 → seckill-goods服务 → seckill-order服务
3. 访问地址：
   - Swagger文档：http://127.0.0.1:8080/doc.html
   - Nacos控制台：http://localhost:8848/nacos

### 方式2：命令行打包启动
```shell
# 打包
mvn clean package -Dmaven.test.skip=true
# 运行jar
java -jar seckill-order-0.0.1.jar --spring.profiles.active=dev

## 压力测试
### 刚完成项目代码进行压测，项目情况
1. 秒杀请求过来，先根据id查秒杀商品表并进行校验
2. 执行Redis扣库存
3. 最后执行发送MQ,发送MQ之后更新t_seckill_message 的status字段为SENT
4. 所有的这三个步骤都放到了分布式锁了
这种情况导致接口相应较慢， 故对项目进行优化
### 优化后项目情况
1. 秒杀请求过来，先在缓存中查秒杀商品表并进行校验
2. 执行Redis扣库存， 只有这一个动作是放在分布式锁种完成，移除了步骤1 和步骤3
3. 插入数据到本地临时表t_seckill_message
4. 执行发送MQ后不更新数据库(没必要更新状态)
5. 开启定时任务查询t_seckill_message status 是0的数据并重新发送MQ消息

优化后接口响应时间从300ms降低到150ms,提升了一半的时间，由于本地电脑内存网络限制，这个响应时间比预期的高一些，
正常情况应该在100ms以内

|环节   |理论耗时    |说明   |
| ---- | -------- | ---- |
|查商品信息缓存|	1-3ms|	Redis GET，纯内存操作 + 本地回环网络|
|Redis 扣库存 (Lua)|	2-5ms|	Lua 脚本在 Redis 内执行，原子操作，极快|
|分布式锁开销 (Redisson)|	3-8ms|	获取锁 → 执行 → 释放，3 次网络往返|
|插入本地消息表 (MySQL)|	8-20ms|	INSERT 写入 InnoDB，受磁盘 fsync 影响|
|发送 MQ 消息|	3-10ms|	异步发送到本地 Broker，几乎无等待|
|序列化/反序列化 (JSON)|	2-5ms|	Jackson 转换，CPU 操作|
|其他（日志、GC、框架）|	5-10ms|	日志写入、线程切换等|
|总计|	25-60ms|	理论最佳值|

优化之前的错误率大概是2%，我把分布式锁的等待时间从3s改成了5s,错误率降为零。主要是等锁超时的错误。
// 尝试加锁，最多等待5秒，锁持有时间默认30秒（Watchdog会自动续期）
boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);




