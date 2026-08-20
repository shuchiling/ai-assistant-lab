# AI Assistant Lab

AI Assistant Lab is a Java + React practice project for AI application engineering.

## Stack

- Backend: JDK 17, Spring Boot 3.5.15, Spring AI 1.1.8, Maven
- Frontend: React 19, TypeScript, Vite, ECharts

## Backend

```bash
cd backend
mvn test
mvn spring-boot:run
```

Set model credentials before calling chat APIs:

```bash
set OPENAI_API_KEY=your-api-key
set OPENAI_BASE_URL=https://api.openai.com
set OPENAI_CHAT_MODEL=gpt-4.1-mini
```

APIs:

- `POST /api/chat`
- `POST /api/chat/stream` with `text/event-stream`

## Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` and `/actuator` to `http://localhost:8080`.
