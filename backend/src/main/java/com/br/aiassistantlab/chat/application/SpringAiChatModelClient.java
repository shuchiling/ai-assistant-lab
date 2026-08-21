package com.br.aiassistantlab.chat.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
class SpringAiChatModelClient implements ChatModelClient {

    private final ChatClient chatClient;

    SpringAiChatModelClient(ChatClient.Builder builder, ChatPromptProperties properties) {
        // system prompt 来自后端配置，浏览器端不能覆盖这个高信任上下文。
        this.chatClient = builder
                .defaultSystem(properties.getSystemPrompt())
                .build();
    }

    /**
     * 同步模型调用：把组装后的多轮消息渲染为当前 Spring AI 客户端可消费的 user 内容。
     */
    @Override
    public String chat(AssembledPrompt prompt) {
        return chatClient.prompt()
                .user(prompt.renderConversation())
                .call()
                .content();
    }

    /**
     * 流式模型调用：模型返回的 token 由应用服务继续转换为 SSE token 事件。
     */
    @Override
    public Flux<String> stream(AssembledPrompt prompt) {
        return chatClient.prompt()
                .user(prompt.renderConversation())
                .stream()
                .content();
    }
}
