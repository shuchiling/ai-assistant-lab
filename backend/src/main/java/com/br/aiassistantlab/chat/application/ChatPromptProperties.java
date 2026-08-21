package com.br.aiassistantlab.chat.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.chat")
public class ChatPromptProperties {

    private String systemPrompt = "You are a concise AI assistant for Java AI application engineering practice.";
    private int maxHistoryMessages = 12;
    private int maxPromptCharacters = 12000;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = hasText(systemPrompt)
                ? systemPrompt
                : "You are a concise AI assistant for Java AI application engineering practice.";
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = Math.max(1, maxHistoryMessages);
    }

    public int getMaxPromptCharacters() {
        return maxPromptCharacters;
    }

    public void setMaxPromptCharacters(int maxPromptCharacters) {
        this.maxPromptCharacters = Math.max(1, maxPromptCharacters);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
