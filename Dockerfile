# 基础镜像：使用 OpenJDK 21
FROM openjdk:21-jdk-slim

# 作者信息
LABEL maintainer="lindajmm@email.com"

# 设置工作目录
WORKDIR /app

# 复制 Jar 包到容器内
COPY target/seckill-demo-1.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]