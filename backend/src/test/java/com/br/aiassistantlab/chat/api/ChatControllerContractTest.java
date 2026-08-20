package com.br.aiassistantlab.chat.api;

import com.br.aiassistantlab.chat.application.ChatApplicationService;
import com.br.aiassistantlab.chat.application.ChatModelClient;
import com.br.aiassistantlab.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import({ChatApplicationService.class, GlobalExceptionHandler.class, ChatControllerContractTest.TestConfig.class})
class ChatControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingChatModelClient chatModelClient;

    @BeforeEach
    void resetRecordingClient() {
        chatModelClient.reset();
    }

    @Test
    void chatReturnsAnswerAndElapsedTimeForValidRequest() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Explain SSE.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hello from fake model"))
                .andExpect(jsonPath("$.elapsedMs").isNumber());

        assertThat(chatModelClient.chatCalls()).isEqualTo(1);
    }

    @Test
    void invalidChatRequestReturnsStructuredErrorWithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/chat"));

        assertThat(chatModelClient.totalCalls()).isZero();
    }

    @Test
    void streamReturnsTokenAndDoneEvents() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Stream please.\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:token")))
                .andExpect(content().string(containsString("data:Hello")))
                .andExpect(content().string(containsString("event:done")))
                .andExpect(content().string(containsString("data:[DONE]")));

        assertThat(chatModelClient.streamCalls()).isEqualTo(1);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        RecordingChatModelClient recordingChatModelClient() {
            return new RecordingChatModelClient();
        }
    }

    static class RecordingChatModelClient implements ChatModelClient {

        private final AtomicInteger chatCalls = new AtomicInteger();
        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public String chat(String message) {
            chatCalls.incrementAndGet();
            return "Hello from fake model";
        }

        @Override
        public Flux<String> stream(String message) {
            streamCalls.incrementAndGet();
            return Flux.just("Hello", " from stream");
        }

        int chatCalls() {
            return chatCalls.get();
        }

        int streamCalls() {
            return streamCalls.get();
        }

        int totalCalls() {
            return chatCalls.get() + streamCalls.get();
        }

        void reset() {
            chatCalls.set(0);
            streamCalls.set(0);
        }
    }
}
