import { FormEvent, useRef, useState } from 'react';
import { ChatRequestMessage, sendChatStream } from '../api/chat';

export type ChatMessage = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
};

type ChatPanelProps = {
  messages: ChatMessage[];
  onMessagesChange: (messages: ChatMessage[]) => void;
};

export function ChatPanel({ messages, onMessagesChange }: ChatPanelProps) {
  const [input, setInput] = useState('Explain what Spring AI is in practical Java terms.');
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const prompt = input.trim();
    if (!prompt || isStreaming) return;

    setError(null);
    setInput('');
    setIsStreaming(true);

    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    const assistantId = crypto.randomUUID();
    const userMessage: ChatMessage = { id: crypto.randomUUID(), role: 'user', content: prompt };
    // 先构造后端请求，再追加空 assistant 占位，避免把 UI 占位消息发送给模型。
    const requestMessages = toRequestMessages([...messages, userMessage]);
    let nextMessages: ChatMessage[] = [
      ...messages,
      userMessage,
      { id: assistantId, role: 'assistant', content: '' }
    ];
    onMessagesChange(nextMessages);

    try {
      await sendChatStream(
        requestMessages,
        {
          onToken: (token) => {
            nextMessages = nextMessages.map((message) =>
              message.id === assistantId
                ? { ...message, content: message.content + token }
                : message
            );
            onMessagesChange(nextMessages);
          },
          onDone: () => finishStreaming()
        },
        { signal: abortController.signal }
      );
    } catch (err) {
      if (!isAbortError(err)) {
        setError(err instanceof Error ? err.message : 'Chat request failed');
      }
      finishStreaming();
    }
  }

  function handleStop() {
    abortControllerRef.current?.abort();
    finishStreaming();
  }

  function finishStreaming() {
    abortControllerRef.current = null;
    setIsStreaming(false);
  }

  return (
    <section className="panel chatPanel" aria-label="AI chat">
      <div className="panelHeader">
        <div>
          <h2>Chat</h2>
          <p>Streams tokens from the Spring Boot SSE endpoint.</p>
        </div>
      </div>

      <div className="messageList">
        {messages.length === 0 ? (
          <div className="emptyState">Start a conversation to test the backend stream.</div>
        ) : (
          messages.map((message) => (
            <article key={message.id} className={`message ${message.role}`}>
              <span>{message.role}</span>
              <p>{message.content || (message.role === 'assistant' ? 'Thinking...' : '')}</p>
            </article>
          ))
        )}
      </div>

      {error ? <div className="errorState">{error}</div> : null}

      <form className="composer" onSubmit={handleSubmit}>
        <textarea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="Ask about Java AI application engineering"
          rows={3}
          disabled={isStreaming}
        />
        <div className="composerActions">
          {isStreaming ? (
            <button type="button" className="secondaryButton" onClick={handleStop}>
              Stop
            </button>
          ) : null}
          <button type="submit" disabled={isStreaming || input.trim().length === 0}>
            {isStreaming ? 'Streaming' : 'Send'}
          </button>
        </div>
      </form>
    </section>
  );
}

function toRequestMessages(messages: ChatMessage[]): ChatRequestMessage[] {
  // system prompt 由后端托管；浏览器只提交 user/assistant 对话轮次。
  return messages
    .filter((message) => message.content.trim().length > 0)
    .map((message) => ({ role: message.role, content: message.content.trim() }));
}

function isAbortError(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError';
}
