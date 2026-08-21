package com.br.aiassistantlab.chat.application;

import com.br.aiassistantlab.chat.dto.ChatRequest;
import com.br.aiassistantlab.chat.dto.ChatResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(2).toMillis();

    private final ChatModelClient chatModelClient;
    private final PromptAssemblyService promptAssemblyService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ChatApplicationService(ChatModelClient chatModelClient, PromptAssemblyService promptAssemblyService) {
        this.chatModelClient = chatModelClient;
        this.promptAssemblyService = promptAssemblyService;
    }

    /**
     * 普通 chat 入口：先把兼容请求组装成受控 Prompt，再交给模型客户端一次性生成。
     */
    public ChatResponse chat(ChatRequest request) {
        long startedAt = System.currentTimeMillis();
        // 同步和流式路径都先经过同一个组装边界，避免一条路径绕过校验、裁剪或 system prompt 注入。
        AssembledPrompt prompt = promptAssemblyService.assemble(request);
        String answer = chatModelClient.chat(prompt);
        return new ChatResponse(answer, System.currentTimeMillis() - startedAt);
    }

    /**
     * 流式 chat 入口：立即返回 SseEmitter，实际模型调用放到后台线程持续推送 token。
     */
    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streamExecutor.execute(() -> streamAnswer(request, emitter));
        return emitter;
    }

    /**
     * 后台流式生成主流程：组装 Prompt、订阅模型 token、转发 SSE 事件，并在失败时发出 error 事件。
     */
    private void streamAnswer(ChatRequest request, SseEmitter emitter) {
        try {
            // 在 worker 内组装 Prompt，让校验、裁剪和模型流式调用保持在同一条执行链路上。
            AssembledPrompt prompt = promptAssemblyService.assemble(request);
            chatModelClient.stream(prompt)
                    .doOnNext(token -> send(emitter, "token", token))
                    .blockLast();
            send(emitter, "done", "[DONE]");
            emitter.complete();
        } catch (Exception ex) {
            log.warn("Streaming chat failed", ex);
            completeWithErrorEvent(emitter);
        }
    }

    /**
     * 发送单个 SSE 事件；发送失败会抛出运行时异常，由外层流式流程统一收敛为 error 事件。
     */
    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

    /**
     * 尽量把流式失败转换为前端可识别的 error 事件，避免把服务端异常细节直接暴露给浏览器。
     */
    private void completeWithErrorEvent(SseEmitter emitter) {
        try {
            send(emitter, "error", "AI_STREAM_FAILED");
            emitter.complete();
        } catch (Exception sendError) {
            emitter.completeWithError(sendError);
        }
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }
}
