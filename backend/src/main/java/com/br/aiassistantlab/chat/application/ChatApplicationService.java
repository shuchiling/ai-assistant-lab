package com.br.aiassistantlab.chat.application;

import com.br.aiassistantlab.chat.dto.ChatRequest;
import com.br.aiassistantlab.chat.dto.ChatResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChatApplicationService {

    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(2).toMillis();

    private final ChatClient chatClient;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ChatApplicationService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a concise AI assistant for Java AI application engineering practice.")
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        long startedAt = System.currentTimeMillis();
        String answer = chatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(answer, System.currentTimeMillis() - startedAt);
    }

    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streamExecutor.execute(() -> streamAnswer(request, emitter));
        return emitter;
    }

    private void streamAnswer(ChatRequest request, SseEmitter emitter) {
        try {
            chatClient.prompt()
                    .user(request.message())
                    .stream()
                    .content()
                    .doOnNext(token -> send(emitter, "token", token))
                    .blockLast();
            send(emitter, "done", "[DONE]");
            emitter.complete();
        } catch (Exception ex) {
            try {
                send(emitter, "error", "AI_STREAM_FAILED");
            } finally {
                emitter.completeWithError(ex);
            }
        }
    }

    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }
}
