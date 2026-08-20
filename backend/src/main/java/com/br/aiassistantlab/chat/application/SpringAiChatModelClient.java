package com.br.aiassistantlab.chat.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
class SpringAiChatModelClient implements ChatModelClient {

    private final ChatClient chatClient;

    SpringAiChatModelClient(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a concise AI assistant for Java AI application engineering practice.")
                .build();
    }

    @Override
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @Override
    public Flux<String> stream(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
