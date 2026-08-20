import { useMemo, useState } from 'react';
import { ChatPanel, type ChatMessage } from './components/ChatPanel';
import { MetricsPanel } from './components/MetricsPanel';

export default function App() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  const metrics = useMemo(() => {
    const assistantMessages = messages.filter((message) => message.role === 'assistant');
    return {
      totalMessages: messages.length,
      assistantMessages: assistantMessages.length,
      averageAnswerLength: assistantMessages.length === 0
        ? 0
        : Math.round(assistantMessages.reduce((sum, message) => sum + message.content.length, 0) / assistantMessages.length)
    };
  }, [messages]);

  return (
    <main className="appShell">
      <section className="workspaceHeader">
        <div>
          <p className="eyebrow">Spring Boot + Spring AI + React</p>
          <h1>AI Assistant Lab</h1>
        </div>
        <span className="statusBadge">Local practice</span>
      </section>
      <section className="workspaceGrid">
        <ChatPanel messages={messages} onMessagesChange={setMessages} />
        <MetricsPanel metrics={metrics} />
      </section>
    </main>
  );
}
