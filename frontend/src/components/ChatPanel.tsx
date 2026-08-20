import { FormEvent, useState } from 'react';
import { sendChatStream } from '../api/chat';

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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const prompt = input.trim();
    if (!prompt || isStreaming) return;

    setError(null);
    setInput('');
    setIsStreaming(true);

    const assistantId = crypto.randomUUID();
    const nextMessages: ChatMessage[] = [
      ...messages,
      { id: crypto.randomUUID(), role: 'user', content: prompt },
      { id: assistantId, role: 'assistant', content: '' }
    ];
    onMessagesChange(nextMessages);

    try {
      await sendChatStream(
        prompt,
        (token) => {
          onMessagesChange(
            nextMessages.map((message) =>
              message.id === assistantId
                ? { ...message, content: message.content + token }
                : message
            )
          );
          nextMessages[nextMessages.length - 1] = {
            ...nextMessages[nextMessages.length - 1],
            content: nextMessages[nextMessages.length - 1].content + token
          };
        },
        () => setIsStreaming(false)
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chat request failed');
      setIsStreaming(false);
    }
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
        />
        <button type="submit" disabled={isStreaming || input.trim().length === 0}>
          {isStreaming ? 'Streaming' : 'Send'}
        </button>
      </form>
    </section>
  );
}
