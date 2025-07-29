# 微信云托管 - JAR包部署方案
FROM amazoncorretto:21-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 复制已经打包好的 JAR 文件
COPY target/hachimi-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8123

# 设置 JVM 参数（适合云环境）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -Dfile.encoding=UTF-8"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]