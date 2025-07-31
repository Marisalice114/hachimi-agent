# 多阶段构建 - 构建阶段
FROM node:18-alpine as build-stage

# 设置工作目录
WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装所有依赖（包括开发依赖，构建需要）
RUN npm ci

# 复制源代码
COPY . .

# 构建项目
RUN npm run build

# 生产阶段 - nginx
FROM nginx:alpine as production-stage

# 复制自定义nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 从构建阶段复制构建结果到nginx目录
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]