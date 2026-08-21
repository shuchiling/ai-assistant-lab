package com.br.aiassistantlab.chat.application;

import com.br.aiassistantlab.chat.dto.ChatMessage;
import com.br.aiassistantlab.chat.dto.ChatRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptAssemblyService {

    private final ChatPromptProperties properties;

    public PromptAssemblyService(ChatPromptProperties properties) {
        this.properties = properties;
    }

    /**
     * Prompt 组装总入口：把外部请求规整为模型输入，并在这里统一注入后端托管的 system prompt。
     */
    public AssembledPrompt assemble(ChatRequest request) {
        List<PromptMessage> messages = normalize(request);
        if (messages.stream().noneMatch(message -> "user".equals(message.role()))) {
            throw new InvalidChatRequestException("messages must contain at least one user message");
        }
        // 先保留最新对话轮次，再套用更粗粒度的字符预算，保证模型输入有明确上界。
        List<PromptMessage> trimmed = trimByMessageCount(messages);
        trimmed = trimByCharacterBudget(trimmed);
        return new AssembledPrompt(properties.getSystemPrompt(), trimmed);
    }

    /**
     * 兼容两种请求形态：多轮 messages 优先，旧的单 message 会被转换为一条 user 消息。
     */
    private List<PromptMessage> normalize(ChatRequest request) {
        if (request.hasMessages()) {
            return request.messages().stream()
                    .map(this::toPromptMessage)
                    .toList();
        }
        return List.of(new PromptMessage("user", request.message().trim()));
    }

    /**
     * 把 API DTO 转成应用层内部消息，后续模型客户端只消费这个受控结构。
     */
    private PromptMessage toPromptMessage(ChatMessage message) {
        return new PromptMessage(message.role(), message.content().trim());
    }

    /**
     * 按最大历史消息数裁剪，优先丢弃最旧消息，保留最近的对话上下文。
     */
    private List<PromptMessage> trimByMessageCount(List<PromptMessage> messages) {
        int maxMessages = properties.getMaxHistoryMessages();
        if (messages.size() <= maxMessages) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - maxMessages, messages.size()));
    }

    /**
     * 按字符预算继续裁剪旧消息；如果只剩最后消息仍超预算，就拒绝请求而不是静默截断。
     */
    private List<PromptMessage> trimByCharacterBudget(List<PromptMessage> messages) {
        int budget = properties.getMaxPromptCharacters();
        List<PromptMessage> trimmed = new ArrayList<>(messages);
        while (totalContentLength(trimmed) > budget && trimmed.size() > 1) {
            trimmed.remove(0);
        }
        if (totalContentLength(trimmed) > budget) {
            throw new InvalidChatRequestException("latest user message exceeds prompt budget");
        }
        if (trimmed.stream().noneMatch(message -> "user".equals(message.role()))) {
            throw new InvalidChatRequestException("messages must retain at least one user message");
        }
        return trimmed;
    }

    /**
     * 当前阶段用字符数作为 token 预算代理，后续可替换为模型 tokenizer 或 usage 统计。
     */
    private int totalContentLength(List<PromptMessage> messages) {
        return messages.stream()
                .mapToInt(message -> message.content().length())
                .sum();
    }
}
