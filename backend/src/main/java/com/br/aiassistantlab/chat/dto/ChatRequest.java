package com.br.aiassistantlab.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        @Valid
        @Size(max = 50, message = "messages must contain at most 50 items")
        List<ChatMessage> messages
) {

    public ChatRequest(String message) {
        this(message, null);
    }

    @AssertTrue(message = "message or messages must contain content")
    public boolean hasPromptInput() {
        return hasMessages() || hasText(message);
    }

    @AssertTrue(message = "messages must contain at least one user message")
    public boolean hasUserMessageWhenMessagesProvided() {
        if (!hasMessages()) {
            return true;
        }
        return messages.stream().anyMatch(message -> "user".equals(message.role()));
    }

    public boolean hasMessages() {
        return messages != null && !messages.isEmpty();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
