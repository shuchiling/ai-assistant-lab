package com.br.aiassistantlab.chat.application;

import com.br.aiassistantlab.chat.dto.ChatMessage;
import com.br.aiassistantlab.chat.dto.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class PromptAssemblyServiceTest {

    private ChatPromptProperties properties;
    private PromptAssemblyService service;

    @BeforeEach
    void setUp() {
        properties = new ChatPromptProperties();
        properties.setSystemPrompt("backend system prompt");
        properties.setMaxHistoryMessages(3);
        properties.setMaxPromptCharacters(80);
        service = new PromptAssemblyService(properties);
    }

    @Test
    void injectsSystemPromptAndNormalizesSingleMessage() {
        AssembledPrompt prompt = service.assemble(new ChatRequest("Explain SSE."));

        assertThat(prompt.systemPrompt()).isEqualTo("backend system prompt");
        assertThat(prompt.messages())
                .extracting("role", "content")
                .containsExactly(tuple("user", "Explain SSE."));
    }

    @Test
    void preservesConversationMessageOrder() {
        AssembledPrompt prompt = service.assemble(new ChatRequest(null, List.of(
                new ChatMessage("user", "First"),
                new ChatMessage("assistant", "Second"),
                new ChatMessage("user", "Third")
        )));

        assertThat(prompt.messages())
                .extracting("role", "content")
                .containsExactly(
                        tuple("user", "First"),
                        tuple("assistant", "Second"),
                        tuple("user", "Third")
                );
    }

    @Test
    void trimsOldHistoryByMessageCount() {
        AssembledPrompt prompt = service.assemble(new ChatRequest(null, List.of(
                new ChatMessage("user", "old one"),
                new ChatMessage("assistant", "old two"),
                new ChatMessage("user", "kept three"),
                new ChatMessage("assistant", "kept four"),
                new ChatMessage("user", "kept five")
        )));

        assertThat(prompt.messages())
                .extracting("content")
                .containsExactly("kept three", "kept four", "kept five");
    }

    @Test
    void trimsOldHistoryByCharacterBudget() {
        properties.setMaxHistoryMessages(6);
        properties.setMaxPromptCharacters(15);

        AssembledPrompt prompt = service.assemble(new ChatRequest(null, List.of(
                new ChatMessage("user", "old message that should go away"),
                new ChatMessage("assistant", "also old"),
                new ChatMessage("user", "short latest")
        )));

        assertThat(prompt.messages())
                .extracting("content")
                .containsExactly("short latest");
    }

    @Test
    void rejectsLatestUserMessageThatExceedsCharacterBudget() {
        properties.setMaxPromptCharacters(10);

        assertThatThrownBy(() -> service.assemble(new ChatRequest(null, List.of(
                new ChatMessage("assistant", "old"),
                new ChatMessage("user", "this latest user message is too long")
        ))))
                .isInstanceOf(InvalidChatRequestException.class)
                .hasMessageContaining("prompt budget");
    }
}
