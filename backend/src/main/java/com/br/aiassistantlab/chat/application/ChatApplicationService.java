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
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ChatApplicationService(ChatModelClient chatModelClient) {
        this.chatModelClient = chatModelClient;
    }

    public ChatResponse chat(ChatRequest request) {
        long startedAt = System.currentTimeMillis();
        String answer = chatModelClient.chat(request.message());
        return new ChatResponse(answer, System.currentTimeMillis() - startedAt);
    }

    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streamExecutor.execute(() -> streamAnswer(request, emitter));
        return emitter;
    }

    private void streamAnswer(ChatRequest request, SseEmitter emitter) {
        try {
            chatModelClient.stream(request.message())
                    .doOnNext(token -> send(emitter, "token", token))
                    .blockLast();
            send(emitter, "done", "[DONE]");
            emitter.complete();
        } catch (Exception ex) {
            log.warn("Streaming chat failed", ex);
            completeWithErrorEvent(emitter);
        }
    }

    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

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
