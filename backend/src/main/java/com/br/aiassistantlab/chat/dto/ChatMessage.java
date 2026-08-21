package com.br.aiassistantlab.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotBlank(message = "role must not be blank")
        @Pattern(regexp = "user|assistant", message = "role must be user or assistant")
        String role,

        @NotBlank(message = "content must not be blank")
        @Size(max = 4000, message = "content must be at most 4000 characters")
        String content
) {
}
