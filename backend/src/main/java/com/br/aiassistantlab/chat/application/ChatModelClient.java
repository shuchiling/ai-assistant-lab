package com.br.aiassistantlab.chat.application;

import reactor.core.publisher.Flux;

public interface ChatModelClient {

    String chat(String message);

    Flux<String> stream(String message);
}
