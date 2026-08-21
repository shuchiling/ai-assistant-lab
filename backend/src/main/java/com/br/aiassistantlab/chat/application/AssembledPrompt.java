package com.br.aiassistantlab.chat.application;

import java.util.List;
import java.util.stream.Collectors;

public record AssembledPrompt(String systemPrompt, List<PromptMessage> messages) {

    public AssembledPrompt {
        messages = List.copyOf(messages);
    }

    /**
     * 将多轮 user/assistant 历史渲染为当前模型适配器可发送的文本形式。
     */
    public String renderConversation() {
        return messages.stream()
                .map(message -> message.role() + ": " + message.content())
                .collect(Collectors.joining("\n\n"));
    }
}
