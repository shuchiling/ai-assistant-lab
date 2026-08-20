export type ChatRequest = {
  message: string;
};

export type ChatResponse = {
  answer: string;
  elapsedMs: number;
};

export type ChatStreamHandlers = {
  onToken: (token: string) => void;
  onDone: () => void;
};

export type ChatStreamOptions = {
  signal?: AbortSignal;
};

export class ChatApiError extends Error {
  readonly status?: number;
  readonly code?: string;

  constructor(message: string, status?: number, code?: string) {
    super(message);
    this.name = 'ChatApiError';
    this.status = status;
    this.code = code;
  }
}

export async function sendChat(message: string): Promise<ChatResponse> {
  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message } satisfies ChatRequest)
  });

  if (!response.ok) {
    throw await toChatError(response, 'Chat request failed');
  }

  return response.json() as Promise<ChatResponse>;
}

export async function sendChatStream(
  message: string,
  handlers: ChatStreamHandlers,
  options: ChatStreamOptions = {}
): Promise<void> {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message } satisfies ChatRequest),
    signal: options.signal
  });

  if (!response.ok || !response.body) {
    throw await toChatError(response, 'Stream request failed');
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
      const { eventName, data } = parseSseEvent(event);
      if (eventName === 'done') {
        handlers.onDone();
        return;
      }
      if (eventName === 'token') {
        handlers.onToken(data);
      }
      if (eventName === 'error') {
        throw new ChatApiError(data || 'AI stream failed', undefined, data || 'AI_STREAM_FAILED');
      }
    }
  }

  handlers.onDone();
}

function parseSseEvent(event: string): { eventName: string; data: string } {
  const eventName = event.match(/^event:(.*)$/m)?.[1]?.trim() ?? 'message';
  const data = event
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trimStart())
    .join('\n');

  return { eventName, data };
}

async function toChatError(response: Response, fallback: string): Promise<ChatApiError> {
  try {
    const body = (await response.json()) as { code?: string; message?: string };
    return new ChatApiError(body.message ?? `${fallback}: ${response.status}`, response.status, body.code);
  } catch {
    return new ChatApiError(`${fallback}: ${response.status}`, response.status);
  }
}
