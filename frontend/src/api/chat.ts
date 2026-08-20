export type ChatRequest = {
  message: string;
};

export type ChatResponse = {
  answer: string;
  elapsedMs: number;
};

export async function sendChat(message: string): Promise<ChatResponse> {
  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message } satisfies ChatRequest)
  });

  if (!response.ok) {
    throw new Error(`Chat request failed: ${response.status}`);
  }

  return response.json() as Promise<ChatResponse>;
}

export async function sendChatStream(
  message: string,
  onToken: (token: string) => void,
  onDone: () => void
): Promise<void> {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message } satisfies ChatRequest)
  });

  if (!response.ok || !response.body) {
    throw new Error(`Stream request failed: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split('\n\n');
    buffer = events.pop() ?? '';

    for (const event of events) {
      const eventName = event.match(/^event:(.*)$/m)?.[1]?.trim();
      const data = event.match(/^data:(.*)$/m)?.[1] ?? '';
      if (eventName === 'done') {
        onDone();
        return;
      }
      if (eventName === 'token') {
        onToken(data.trimStart());
      }
    }
  }

  onDone();
}
