# NexTech Solutions 企业知识库 Agent

基于 Spring AI 3.4.4 的企业内部知识库智能问答平台。支持 RAG 混合检索（向量 + BM25）、查询重写、多轮对话、工具调用、MCP 服务、ReAct Agent 自主规划，以及 Multi-Agent 事实核查闭环。

---

## 架构总览

```
前端 (Vue 3 + Vite)  :3000
     │ 代理 /api → :8123
后端 (Spring Boot 3.4.4 + Java 18)  :8123/api
     │
     ├── Controller 层
     │    ├─ AiController        → /api/chat, /api/rag, /api/agent
     │    └─ AgentDemoController → /api/agent/fact-check
     │
     ├── 核心业务 KnowledgeBaseApp
     │    ├─ doChat()            → 基础对话
     │    ├─ doChatWithRag()     → 查询重写 → 混合检索 → LLM 回答
     │    ├─ doChatWithTools()   → 工具调用
     │    └─ doChatWithMcp()     → MCP 服务调用
     │
     ├── Agent 体系 (ReAct 模式)
     │    └─ BaseAgent → ReActAgent → ToolCallAgent → EnterpriseKbSuperAgent
     │    └─ FactCheckAgent (独立组件, 显式 Think→Act→Observe 闭环)
     │
     └── AI 模型
          ├─ DashScope qwen-plus (主要, 需 API Key)
          └─ Ollama gemma3:1b (备选, 本地)
```

---

## 功能特性

### RAG 混合检索管道

```
用户问题
  │
  ├─ ① 查询重写 (QueryRewriter — LLM 改写)
  │
  ├─ ② 混合检索 (HybridDocumentRetriever)
  │    ├── 向量检索 (SimpleVectorStore)  → 语义相似度
  │    └── BM25 关键词检索 (Lucene)      → 关键词匹配
  │    └── RRF 融合排序 (Reciprocal Rank Fusion)
  │
  ├─ ③ 上下文注入 (RetrievalAugmentationAdvisor)
  │    └── 无结果时拒绝回答 (ContextualQueryAugmenter)
  │
  └─ ④ LLM 生成回答
```

文档入库时自动做两件事：
- **关键词增强**：`MyKeywordEnricher` 用 AI 为文档生成关键词标签（存于 metadata）
- **BM25 索引**：`KeywordIndexer` 构建 Lucene 内存级倒排索引

### Agent 体系

| 层级 | 类 | 职责 |
|------|-----|------|
| 基类 | `BaseAgent` | 状态机 (IDLE→RUNNING→FINISHED→ERROR)、run/runStream 循环 |
| 推理模式 | `ReActAgent` | 抽象 `think()` + `act()` → `step()` |
| 工具调用 | `ToolCallAgent` | LLM 选工具 → 执行工具 → 结果回写消息列表 |
| 超级智能体 | `EnterpriseKbSuperAgent` | 注册全部工具，20 步最大循环 |
| 事实核查 | `FactCheckAgent` | 独立组件，显式 think→act→observe 闭环 |

### Multi-Agent ReAct 闭环 (FactCheckAgent)

```
execute() 循环:
  while (!finished && step < MAX_STEPS)
    │
    ├── 1. THINK    LLM 分析上下文 → 输出结构化决策
    │               ACTION: search_kb | evaluate | finish
    │               CLAIM / SEARCH_KEYWORDS / VERDICT ...
    │
    ├── 2. ACT      search_kb → 检索向量库
    │               evaluate  → 记录裁决结果 (supported/unsupported/partial)
    │               finish    → 标记结束
    │
    └── 3. OBSERVE  结果写入 observationHistory → 供下一轮 THINK 使用
```

### 可用工具

| 工具 | 功能 |
|------|------|
| `WebSearchTool` | 搜索引擎查询 |
| `WebScrapingTool` | 网页内容抓取 |
| `DocumentSummaryTool` | 文档摘要生成 |
| `FileOperationTool` | 文件读写操作 |
| `PDFGenerationTool` | PDF 生成 |
| `ResourceDownloadTool` | 资源下载 |
| `TerminalOperationTool` | 终端命令执行 |
| `TerminateTool` | Agent 终止信号 |

---

## 快速开始

### 前置条件

- JDK 18+
- Node.js 18+

### 1. 配置 API Key

编辑 `src/main/resources/application-local.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: sk-你的阿里云百炼APIKey

search-api:
  api-key: 你的搜索APIKey
```

DashScope API Key 在 [阿里云百炼控制台](https://bailian.console.aliyun.com) 获取。

### 2. 启动后端

```bash
cd enterprise-knowledge-base-agent
./mvnw.cmd spring-boot:run
```

后端启动于 http://localhost:8123/api，Swagger 文档：http://localhost:8123/api/swagger-ui.html

### 3. 启动前端

```bash
cd enterprise-kb-agent-frontend
npm install
npm run dev
```

前端启动于 http://localhost:3000，/api 请求自动代理到后端 8123 端口。

---

## API 接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | 基础多轮对话 |
| `/api/chat/sse` | GET | SSE 流式对话 |
| `/api/rag` | POST | RAG 知识库问答（含查询重写 + 混合检索） |
| `/api/report` | POST | 结构化咨询报告 |
| `/api/agent/chat` | POST | EnterpriseKbSuperAgent 自主规划 |
| `/api/agent/chat-tools` | POST | 工具调用对话 |
| `/api/agent/chat-mcp` | POST | MCP 服务对话 |
| `/api/agent/fact-check` | GET | 事实核查 ReAct 演示 |
| `/api/health` | GET | 健康检查 |

---

## 项目结构

```
enterprise-knowledge-base-agent/
├── src/main/java/com/nextech/enterprisekbagent/
│   ├── advisor/              # 自定义 Advisor (MyLogger, ReReading)
│   ├── agent/                # Agent 体系
│   │   ├── BaseAgent.java
│   │   ├── ReActAgent.java
│   │   ├── ToolCallAgent.java
│   │   ├── EnterpriseKbSuperAgent.java
│   │   ├── FactCheckAgent.java       # ← 新增: 事实核查 ReAct 闭环
│   │   └── model/AgentState.java
│   ├── app/
│   │   └── KnowledgeBaseApp.java     # 核心业务编排
│   ├── chatmemory/
│   │   └── FileBasedChatMemory.java  # 基于文件的消息持久化
│   ├── config/
│   │   └── CorsConfig.java
│   ├── constant/
│   ├── controller/
│   │   ├── AiController.java
│   │   ├── AgentDemoController.java  # ← 新增: FactCheck 端点
│   │   └── HealthController.java
│   ├── demo/                 # 功能演示代码
│   ├── rag/                  # RAG 检索核心
│   │   ├── QueryRewriter.java
│   │   ├── HybridDocumentRetriever.java   # ← 新增: 混合检索
│   │   ├── KeywordIndexer.java            # ← 新增: BM25 索引
│   │   ├── MyKeywordEnricher.java
│   │   ├── KnowledgeBaseDocumentLoader.java
│   │   ├── KnowledgeBaseVectorStoreConfig.java
│   │   └── PgVectorVectorStoreConfig.java # 可选 PgVector
│   └── tools/                # 工具注册与实现
│       ├── ToolRegistration.java
│       ├── WebSearchTool.java
│       ├── WebScrapingTool.java
│       └── ...
│
├── enterprise-kb-agent-frontend/   # Vue 3 前端
│
└── enterprise-kb-image-search-mcp-server/  # MCP 图像搜索服务
```

---

## 配置说明

| 配置文件 | 用途 |
|----------|------|
| `application.yml` | 公共配置: 端口 8123, /api 前缀, Swagger, Knife4j |
| `application-local.yml` | 本地开发: API Key, 日志级别 (spring.ai: DEBUG) |
| `application-prod.yml` | 生产环境 (待补充) |

### 可选组件 (需额外配置)

- **PgVector 向量存储**：启用 `PgVectorVectorStoreConfig` + 配置 PostgreSQL 连接
- **阿里云知识库服务**：启用 `KnowledgeBaseRagCloudAdvisorConfig` + 配置 Knowledge Index
- **MCP 客户端**：通过 SSE 或 Stdio 连接 MCP 服务
- **Ollama 本地模型**：默认连接 `localhost:11434`，模型 `gemma3:1b`

---

## 依赖

- Spring Boot 3.4.4
- Spring AI 1.0.0 (DashScope, Ollama, PgVector, MCP)
- Apache Lucene 9.12.0 (BM25 全文检索)
- Kryo 5.6.2 (会话记忆序列化)
- Vue 3 + Vite
- Knife4j (API 文档)
- Hutool (工具库)
