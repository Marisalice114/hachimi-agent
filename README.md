# Hachimi-Agent: 智能写作导师/AI Agent框架

![image-20250731153420104](D:\ideaproject\hachimi-agent\assets\image-20250731153420104.png)

## 🚀 项目简介

Hachimi-Agent 是一个专业的AI写作导师系统，同时也是一个可扩展的AI Agent应用开发框架。项目基于 Spring Boot 3 + Spring AI 和 Vue 3 构建，以写作指导为核心应用场景，展示了如何构建专业化的AI Agent系统。

该框架具有高度的可定制性，用户可以通过自定义RAG知识库和修改提示词来适配不同的应用场景。当前以写作导师为示例场景，为用户提供专业的创意写作、实用写作和写作技能提升指导。

**🌟 框架特色**: 本项目不仅是一个完整的写作导师应用，更是一个展示现代AI Agent开发最佳实践的框架，支持快速定制和场景切换。

## ✨ 核心亮点

- 📝 **专业写作导师**: 基于资深写作专家角色，提供创意写作、实用写作、技能提升三大类别的专业指导
- 🔧 **可扩展Agent框架**: 通过自定义RAG知识库和提示词，可快速适配其他专业领域
- 🧠 **智能RAG优化**: 创新的检索增强技术，显著提升回复相关度和专业性
- 🛠️ **丰富工具生态**: 内置网络搜索、资源下载、PDF生成、网页抓取等多种工具
- 🤖 **自主Agent架构**: 基于OpenManus架构的自主决策Agent，具备循环控制和自主结束机制
- 🔌 **MCP协议支持**: 支持自定义MCP服务器开发和第三方MCP服务集成
- 💾 **企业级存储**: 自定义会话记忆存储至MySQL，支持长期对话历史管理
- ⚡ **可中断响应**: 支持手动停止AI回复，提升用户体验

## 🎯 Spring AI 核心特性应用

本项目深度实践了 Spring AI 框架的核心能力，并在写作指导场景下进行了创新扩展：

### 🤖 多层次AI模型集成
- **本地模型支持**: 集成 Ollama 本地模型，用于Prompt重写等轻量级任务，降低API调用成本
- **云端模型调用**: 阿里通义千问、OpenAI GPT 用于核心对话和复杂推理
- **模型任务分工**: 不同复杂度任务分配给不同模型，优化性能和成本
- **多平台API支持**: 支持DashScope（通义千问）、OpenAI标准API、以及SiliconFlow等兼容平台
- **灵活模型配置**: 通过环境变量动态切换模型提供商和具体模型版本

### 🧠 创新RAG检索优化
- **智能文档切片**: 自动为文档片段生成描述性metadata，提升检索精度
- **查询智能转换**: 自定义Transformer结合第三方翻译API，优化查询效果
- **检索相关度提升**: 通过优化RAG检索方式，显著提升大模型回复的专业性和相关度
- **写作知识库构建**: 专门针对写作场景构建的专业知识库

### 🔧 高级Advisor系统
- **自定义日志Advisor**: 实现 `SelfLogAdvisor` 用于对话过程全链路监控
- **ReReadingAdvisor**: 支持自动实现Re2逻辑
- **会话记忆增强**: 自定义MySQL存储的会话记忆，支持长期对话历史
- **敏感词过滤**: `BanWordAdvisor` 确保输出内容的安全性

### 🛠️ 智能工具调用生态
- **网络搜索工具**: 实时获取最新写作素材和资讯
- **资源下载工具**: 自动下载和整理写作相关资源
- **PDF生成工具**: 将写作成果导出为专业文档
- **网页抓取工具**: 智能提取网络内容作为写作素材
- **文件操作工具**: 完整的文件管理和处理能力

### 🔌 MCP协议深度集成
- **自定义MCP服务器**: 开发了专门的图像搜索MCP服务器
- **第三方MCP集成**: 支持接入具有标准MCP协议的Server
- **协议标准化**: 遵循MCP 1.0标准，确保良好的扩展性

### 🤖 自主Agent架构
- **OpenManus架构**: 基于先进的Agent架构实现自主决策
- **循环控制机制**: 防止Agent进入无限循环，确保系统稳定性
- **自主结束机制**: 智能判断任务完成状态，自动结束对话
- **MCP协议Agent**: 支持MCP协议的高级Agent实现

### 💪 企业级健壮性
- **统一响应封装**: 后端响应全面封装，提升系统健壮性
- **异常处理机制**: 完善的错误处理和恢复策略
- **可中断设计**: 支持手动停止AI回复，优化用户体验
- **并发控制**: 高并发场景下的稳定性保障

## 📋 技术选型

### 后端技术栈

| 技术类别 | 技术选型 | 版本 | 描述 |
|---------|---------|------|------|
| **核心框架** | Spring Boot | 3.4.5 | 企业级Java应用框架 |
| **编程语言** | Java | 21 | 现代化Java开发 |
| **AI框架** | Spring AI | 1.0.0 | Spring生态AI集成框架 |
| **Web框架** | Spring Web | - | RESTful API支持 |
| **数据持久化** | MyBatis-Plus | 3.5.11 | 高效ORM框架 |
| **数据库** | MySQL + PostgreSQL | 8.0 + 最新 | 业务数据 + 向量数据 |
| **连接池** | Druid | 1.2.20 | 高性能数据库连接池 |
| **向量数据库** | PGVector | - | PostgreSQL向量扩展 |
| **AI模型集成** | Ollama + 通义千问 + OpenAI | - | 多模型支持 |
| **API文档** | Knife4j + SpringDoc | 4.5.0 + 2.7.0 | 接口文档生成 |
| **工具库** | Hutool | 5.8.39 | Java工具集 |
| **网页抓取** | Jsoup | 1.21.1 | HTML解析和网页抓取 |
| **PDF处理** | iText | 9.1.0 | PDF文档生成 |
| **MCP支持** | Spring AI MCP Client | 1.0.0 | 模型控制协议客户端 |

### 前端技术栈

| 技术 | 版本 | 描述 |
|------|------|------|
| Vue.js | 3.3.4 | 现代化前端框架 |
| Vite | 4.4.0 | 快速构建工具 |
| Vue Router | 4.2.4 | 单页应用路由 |
| Axios | 1.4.0 | HTTP请求库 |
| Marked | 16.1.1 | Markdown渲染 |

## 🎯 核心功能

### AI对话系统
- **多Agent对话**: 支持不同专业领域的AI助手
- **流式响应**: 实时打字效果，提升用户体验
- **会话管理**: 智能会话保存和恢复
- **可中断对话**: 随时停止AI回复生成

### 智能工具集成
- **文档处理**: 支持多种文档格式的智能解析
- **网页抓取**: 智能提取网页内容
- **PDF生成**: 动态创建PDF文档
- **文件操作**: 完整的文件管理功能
- **终端操作**: 安全的系统命令执行

### RAG检索增强
- **文档向量化**: 智能文档索引和搜索
- **语义检索**: 基于语义相似度的内容匹配
- **上下文增强**: 动态生成相关上下文信息

## 🚀 快速开始

### 环境要求

- **Java**: 21+
- **Node.js**: 16+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 建议8GB+
- **存储空间**: 建议20GB+

### 1. 项目克隆

```bash
git clone <repository-url>
cd hachimi-agent
```

### 2. 环境变量配置

创建 `.env` 文件在项目根目录：

```env
# 数据库配置
MYSQL_DATABASE=hachimi_agent
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_strong_mysql_password

# PostgreSQL配置 (用于向量存储)
POSTGRES_HOST=your_postgres_host
POSTGRES_PORT=5432
POSTGRES_DATABASE=hachimi_agent
POSTGRES_USERNAME=your_postgres_user
POSTGRES_PASSWORD=your_postgres_password

# AI服务配置
ALI_API_KEY=your_alibaba_api_key
DASH_SCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_CHAT_MODEL=qwen-plus
AI_MULTIMODAL_MODEL=qwen-vl-plus
OLLAMA_MODEL=qwen3:0.6b

# 搜索服务配置
SEARCH_API_KEY=your_search_api_key
BAIDU_APP_ID=your_baidu_app_id
BAIDU_APP_SECRET=your_baidu_app_secret

# 系统配置
LOG_LEVEL=INFO
```

### 3. 一键部署

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f hachimi-agent
```

### 4. 初始化Ollama模型

```bash
# 进入Ollama容器
docker exec -it hachimi-ollama ollama pull qwen3:0.6b

# 验证模型安装
docker exec -it hachimi-ollama ollama list
```

### 5. 访问应用

- **前端应用**: http://localhost
- **后端API**: http://localhost:8123
- **API文档**: http://localhost:8123/doc.html
- **健康检查**: http://localhost:8123/health

## 🌐 在线体验

本项目已部署至**微信云托管**平台，提供在线写作指导体验：

> 📱 **获取体验链接**: 请联系项目作者获取云端访问链接  
> 🔗 **联系方式**: [在此添加您的联系方式]  
> ⚡ **云端优势**: 无需本地部署，即开即用，体验专业写作指导

### 在线功能特性
- ✅ 专业写作导师对话体验
- ✅ 实时流式响应和可中断控制
- ✅ 多种写作场景智能切换
- ✅ 工具调用演示（搜索、下载、生成等）
- ✅ RAG知识库智能问答

## 🎓 框架学习价值

本项目是学习现代AI Agent开发的完整案例，特别适合：

### 技术学习者
- **Spring AI 框架**: 从基础到高级的完整实践
- **Agent架构设计**: 自主决策和工具调用的实现方法
- **RAG系统优化**: 检索增强生成的实战经验
- **MCP协议应用**: 模型控制协议的开发和集成

### 应用开发者
- **快速原型开发**: 基于本框架快速构建专业AI应用
- **场景定制方法**: 学习如何适配不同的业务场景
- **企业级部署**: 从开发到生产的完整部署方案
- **性能优化技巧**: 多模型协作和成本控制策略

### 架构设计者
- **微服务架构**: 前后端分离的现代应用设计
- **容器化部署**: Docker化的云原生架构
- **可扩展设计**: 组件化和插件化的系统架构

## 📄 许可证

本项目采用 MIT License 开源协议。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！特别欢迎：

- 新的应用场景适配示例
- 工具调用功能扩展
- RAG检索优化方案
- MCP服务器开发案例

### 贡献方式
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 技术交流
- 💬 欢迎在 Issues 中讨论AI Agent开发问题
- 🌟 如果项目对您有帮助，请给个 Star 支持
- 📧 框架定制和技术合作请通过邮件联系

## 📞 联系作者

- 🔗 **在线体验**: 联系获取微信云托管访问链接
- 📧 **邮箱**: [810271689@qq.com]
- 🐱 **GitHub**: [Marisalice114]

---

**💡 提示**: 
- 本项目持续更新中，建议关注最新版本
- 云端体验需要联系作者获取访问权限
- 框架定制和场景适配可提供技术支持
- 欢迎Star和Fork，共同完善AI Agent开发生态
- ⚠️ **安全提醒**: 请勿在代码中直接写入API密钥，使用环境变量管理敏感信息
