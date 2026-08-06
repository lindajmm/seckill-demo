
FROM docker.m.daocloud.io/eclipse-temurin:21-jre-alpine

# 作者信息
LABEL maintainer="lindajmm@email.com"

# 设置工作目录
WORKDIR /app

# 复制 Jar 包到容器内
COPY seckill-demo-1.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]