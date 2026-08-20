package com.br.aiassistantlab.chat.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankMessage() {
        assertThat(validator.validate(new ChatRequest(" "))).isNotEmpty();
    }

    @Test
    void acceptsNormalMessage() {
        assertThat(validator.validate(new ChatRequest("Explain SSE in one sentence."))).isEmpty();
    }

    @Test
    void rejectsMessageLongerThanLimit() {
        assertThat(validator.validate(new ChatRequest("a".repeat(4001)))).isNotEmpty();
    }
}
