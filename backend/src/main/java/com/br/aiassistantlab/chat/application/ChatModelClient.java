package com.br.aiassistantlab.chat.application;

import reactor.core.publisher.Flux;

public interface ChatModelClient {

    /**
     * 使用后端已组装好的 Prompt 执行一次性模型生成。
     */
    String chat(AssembledPrompt prompt);

    /**
     * 使用后端已组装好的 Prompt 执行流式模型生成，并返回增量 token。
     */
    Flux<String> stream(AssembledPrompt prompt);
}
