# AI Assistant Lab

AI Assistant Lab 是一个面向 AI 应用工程实践落地的 Java + React 项目。当前阶段聚焦 Chat 与 Prompt/session 工程基线：普通 chat、SSE 流式输出、前端工作台、错误处理、停止生成、多轮消息请求、后端 Prompt 组装、历史裁剪、模型配置和最小验证证据。

## 技术栈

- Backend：JDK 17、Spring Boot 3.5.15、Spring AI 1.1.8、Maven
- Frontend：React 19、TypeScript、Vite、ECharts

## 后端

```bash
cd backend
mvn test
mvn spring-boot:run
```

启动后端前需要设置本地环境变量；Spring AI OpenAI autoconfiguration 会在启动期校验 API key。示例只说明变量名，不要把真实密钥提交到仓库。

```bash
set OPENAI_API_KEY=your-api-key
set OPENAI_BASE_URL=https://api.openai.com
set OPENAI_CHAT_MODEL=gpt-4.1-mini
set OPENAI_CHAT_TEMPERATURE=0.7
set AI_CHAT_SYSTEM_PROMPT=You are a concise AI assistant for Java AI application engineering practice.
set AI_CHAT_MAX_HISTORY_MESSAGES=12
set AI_CHAT_MAX_PROMPT_CHARACTERS=12000
```

API：

- `POST /api/chat`
- `POST /api/chat/stream`，响应类型为 `text/event-stream`

普通 chat 请求示例：

```bash
curl -X POST http://localhost:8080/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Explain Spring AI in one sentence.\"}"
```

多轮 messages 请求示例：

```bash
curl -X POST http://localhost:8080/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"messages\":[{\"role\":\"user\",\"content\":\"Explain SSE.\"},{\"role\":\"assistant\",\"content\":\"SSE sends server events over HTTP.\"},{\"role\":\"user\",\"content\":\"Give a Java use case.\"}]}"
```

流式 chat 请求示例：

```bash
curl -N -X POST http://localhost:8080/api/chat/stream ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Stream a short explanation of SSE.\"}"
```

Chat 请求合同：

- 兼容入口：`{"message":"..."}`。
- 多轮入口：`{"messages":[{"role":"user","content":"..."},{"role":"assistant","content":"..."}]}`。
- `messages.role` 只允许 `user` 和 `assistant`。
- 客户端不得提交 `system` role 或 system prompt；system prompt 由后端配置注入。
- 后端会按 `AI_CHAT_MAX_HISTORY_MESSAGES` 和 `AI_CHAT_MAX_PROMPT_CHARACTERS` 从旧消息开始裁剪历史。
- 当前预算是消息数和字符数代理，不是真实 token 计费。

SSE 事件语义：

- `token`：增量文本片段。
- `done`：生成完成。
- `error`：流式生成或发送失败。

## 前端

```bash
cd frontend
npm install
npm run typecheck
npm run build
npm run dev
```

Vite dev server 会把 `/api` 和 `/actuator` 代理到 `http://localhost:8080`。

前端 Chat Workbench 会把当前内存中的 user/assistant 历史和最新 user prompt 发送到 `/api/chat/stream`。空 assistant 占位消息不会发送给后端；当前阶段不做会话持久化、账号、多设备恢复、RAG、Tool 调用、Memory 或评测平台。

## 手工验证

1. 设置模型环境变量。
2. 启动后端：`cd backend && mvn spring-boot:run`。
3. 启动前端：`cd frontend && npm run dev`。
4. 打开 Vite 页面，提交一条 prompt，确认 assistant 消息会随 `token` 事件增量更新。
5. 在生成过程中点击 `Stop`，确认前端停止追加内容、解除生成中状态并允许继续输入。
6. 停止后端或使用无效配置，再次提交，确认前端显示可读错误并恢复输入。
7. 使用 `curl` 调用 `POST /api/chat`，确认 JSON 响应包含 `answer` 和 `elapsedMs`。
8. 使用多轮 `messages` 示例调用 `POST /api/chat`，确认后端仍返回结构化 JSON 响应。

如果本地没有可用模型凭据，后端应用无法完成启动；只能完成构建、单元测试和合同级验证。真实端到端模型调用需要在验证报告中标记为未执行。
