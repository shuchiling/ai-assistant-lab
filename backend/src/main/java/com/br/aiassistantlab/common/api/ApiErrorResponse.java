package com.br.aiassistantlab.common.api;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        OffsetDateTime timestamp
) {
}
