# Hachimi-Agent: 智能AI对话助手

## 🚀 项目简介

Hachimi-Agent 是一个基于 Spring Boot 3 + Spring AI 和 Vue 3 构建的现代化AI对话系统。支持多种大语言模型集成，具备流式响应、会话管理、工具调用等功能，为用户提供智能、流畅的对话体验。

**🌟 项目亮点**: 本项目深度集成 Spring AI 框架，实现了从基础对话到高级AI Agent的完整技术栈，是学习和实践现代AI应用开发的优秀案例。

## ✨ 主要特性

- 🤖 **多Agent支持**: HachimiManus通用助手、LoveApp情感咨询助手
- 💬 **流式对话**: 基于SSE的实时打字机效果，支持可中断响应
- 🧠 **RAG检索增强**: 智能文档处理和语义搜索
- 🛠️ **丰富工具集**: 文件操作、PDF生成、网页抓取、终端命令等
- 🔌 **模型集成**: 支持Ollama、通义千问、OpenAI等多种AI模型
- 📱 **现代化UI**: 基于Vue 3的响应式前端界面
- ☁️ **云端部署**: 已部署至微信云托管，支持在线体验

## 🎯 Spring AI 核心特性应用

本项目深度实践了 Spring AI 框架的核心能力，涵盖了现代AI应用开发的各个方面：

### 🤖 AI 大模型集成
- **多模型支持**: 集成 Ollama 本地模型、阿里通义千问、OpenAI GPT
- **4种接入方式**: 
  - 云端API调用 (通义千问、OpenAI)
  - 本地模型部署 (Ollama)
  - 兼容接口适配 (统一调用标准)
  - MCP协议集成 (模型控制协议)

### 🧠 Spring AI 核心特性
- **自定义 Advisor**: 实现 `SelfLogAdvisor` 用于对话过程监控和日志记录
- **对话记忆管理**: 智能维护多轮对话上下文，支持长期记忆存储
- **结构化输出**: 利用 JSON Schema 生成器实现结构化AI响应
- **流式响应控制**: 基于 Reactor 的响应式编程，支持可中断流式对话

### 🔧 Prompt 工程实践
- **模板化 Prompt**: 实现 `ManusPrompt` 等专业化提示词模板
- **动态 Prompt 构建**: 根据上下文和用户需求动态生成优化提示词
- **多语言 Prompt**: 支持中英文场景的智能提示词优化

### 📚 RAG 检索增强生成
- **文档向量化**: 使用 `MarkdownDocumentReader` 处理多种文档格式
- **智能查询优化**: 
  - `BaiduTranslationQueryTransformer` - 多语言查询翻译
  - `MyMultiQueryExpander` - 查询扩展和优化
  - `LoveAppContextualQueryAugmenterFactory` - 上下文增强
- **PgVector 向量数据库**: 高性能语义搜索和相似度匹配
- **云数据库集成**: 支持阿里云 RDS PostgreSQL 向量存储服务

### 🛠️ Tool Calling 工具调用系统
- **丰富工具生态**: 
  - `FileOperationTool` - 智能文件操作
  - `WebScrapingTool` - 网页内容抓取
  - `PDFGenerationTool` - 动态PDF生成
  - `TerminalOperationTool` - 安全系统命令执行
  - `WebSearchTool` - 网络搜索集成
- **工具调用原理**: 基于 Spring AI 的 Function Calling 机制
- **安全执行环境**: 工具调用的权限控制和安全隔离

### 🔌 MCP 模型上下文协议
- **MCP 服务开发**: 独立的 `hachimi-image-search-mcp-server` 图像搜索服务
- **协议标准化**: 遵循 MCP 1.0 标准协议规范
- **服务扩展性**: 支持自定义 MCP 服务快速集成

### 🤖 AI 智能体 (Agent) 架构
- **多Agent系统**: 
  - `HachimiManus` - 通用智能助手，支持复杂任务分解
  - `ReactAgent` - 基于ReAct推理模式的智能体
  - `ToolCallAgent` - 工具调用专业智能体
- **Agent 自主决策**: 智能体可自主选择工具和执行策略
- **思考过程可视化**: 实时展示Agent的推理和决策过程

### ☁️ AI 服务化部署
- **容器化部署**: Docker + Docker Compose 完整部署方案
- **微信云托管**: 项目已部署至腾讯云微信云托管平台
- **Serverless 就绪**: 支持云原生和Serverless架构部署

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

本项目已部署至**微信云托管**平台，提供在线体验服务：

> 📱 **获取体验链接**: 请联系项目作者获取云端访问链接  
> 🔗 **联系方式**: [在此添加您的联系方式，如微信号、邮箱等]  
> ⚡ **云端优势**: 无需本地部署，即开即用，体验完整功能

### 云端功能特性
- ✅ 完整的AI对话体验
- ✅ 实时流式响应
- ✅ 多Agent智能切换
- ✅ 工具调用演示
- ✅ RAG知识库问答

## 🎓 技术学习价值

本项目是学习现代AI应用开发的完整案例，涵盖：

### 核心技术栈
- **Spring AI 框架**: 从入门到精通的完整实践
- **大模型集成**: 多种主流AI模型的接入和使用
- **向量数据库**: PgVector的部署、优化和使用
- **AI Agent开发**: 智能体架构设计和实现
- **工具调用系统**: Function Calling的原理和实践

### 实践技能
- **Prompt 工程**: 提示词设计和优化技巧
- **RAG 系统**: 检索增强生成的完整实现
- **流式响应**: 实时AI对话的技术实现
- **云端部署**: 现代化AI应用的部署和运维

### 架构设计
- **微服务架构**: 前后端分离的现代应用架构
- **响应式编程**: 基于Reactor的异步处理
- **容器化部署**: Docker化的云原生部署方案

## 📄 许可证

本项目采用 MIT License 开源协议。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！同时也欢迎技术交流和经验分享。

### 贡献方式
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 技术交流
- 💬 欢迎在 Issues 中讨论技术问题
- 🌟 如果项目对您有帮助，请给个 Star 支持
- 📧 技术合作请通过邮件联系

## 📞 联系作者

- 🔗 **在线体验**: 联系获取微信云托管访问链接

---

**💡 提示**: 
- 本项目持续更新中，建议关注最新版本
- 云端体验需要联系作者获取访问权限
- 部分功能需要配置相应的API密钥才能完整使用
- 欢迎Star和Fork，共同完善项目功能
