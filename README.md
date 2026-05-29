# NexTech Solutions 企业知识库 Agent

基于 Spring AI 的企业内部知识库智能问答平台，支持 RAG 知识库检索、多轮对话、工具调用、MCP 服务，以及基于 ReAct 模式的 **EnterpriseKbSuperAgent** 自主规划超级智能体。

## 项目功能

- **企业知识库问答**：基于企业内部知识库（RAG）解答文化、组织架构、技术规范、信息安全、办公行政、入职培训、应急响应等问题。

## 技术栈

- Java 21 + Spring Boot 3
- Spring AI 
- RAG 
- Tool Calling + MCP + Agent Skills
- ReAct Agent（EnterpriseKbSuperAgent）
- Vue 3 前端

## 知识库文档

RAG 知识库位于 `src/main/resources/document/企业知识库.md`。

## 快速开始

1. 在 `src/main/resources/application-local.yml` 中配置 `spring.ai.dashscope.api-key` 与 `search-api.api-key`
2. 启动后端：`mvn spring-boot:run`
3. 启动前端：进入 `enterprise-kb-agent-frontend`，执行 `npm install && npm run dev`

## 核心代码结构

```
src/main/java/com/nextech/enterprisekbagent/
├── app/KnowledgeBaseApp.java
├── agent/EnterpriseKbSuperAgent.java
├── rag/KnowledgeBaseDocumentLoader.java
└── controller/AiController.java
```
