# 微信云托管 - JAR包部署方案
FROM amazoncorretto:21-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 安装 Node.js 和 npm
RUN apk add --no-cache nodejs npm

# 安装百度地图 MCP 服务器包
RUN npm install -g @baidumap/mcp-server-baidu-map

WORKDIR /app

# 复制主应用JAR文件
COPY hachimi-agent-0.0.1-SNAPSHOT.jar app.jar

# 复制lib目录（包含MCP服务器JAR）
COPY lib/ lib/

# 添加调试信息（修复版本）
RUN echo "=== Container file structure ===" && \
    ls -la && \
    echo "=== lib directory contents ===" && \
    ls -la lib/ && \
    echo "=== Testing MCP server JAR ===" && \
    java -jar lib/hachimi-image-search-mcp-server-0.0.1-SNAPSHOT.jar --help 2>&1 | head -10 || echo "MCP server JAR help test completed"

# 暴露端口
EXPOSE 8123

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms${JVM_XMS:-512m} -Xmx${JVM_XMX:-1024m} -XX:+UseG1GC -XX:+UseContainerSupport -Dfile.encoding=UTF-8"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]