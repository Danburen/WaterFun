const RECONNECT_DELAY = 5000

type SseEventHandler = (event: MessageEvent) => void

export class SseClient {
  private url: string
  private headers: Record<string, string>
  private listeners: Map<string, Set<SseEventHandler>> = new Map()
  private aborted = false
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reader: ReadableStreamDefaultReader<Uint8Array> | null = null

  constructor(url: string, headers: Record<string, string> = {}) {
    this.url = url
    this.headers = headers
  }

  addEventListener(type: string, handler: SseEventHandler): void {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type)!.add(handler)
  }

  removeEventListener(type: string, handler: SseEventHandler): void {
    this.listeners.get(type)?.delete(handler)
  }

  private emit(type: string, data: string): void {
    const handlers = this.listeners.get(type)
    if (!handlers) return
    const event = new MessageEvent(type, { data })
    for (const handler of handlers) {
      handler(event)
    }
  }

  async connect(): Promise<void> {
    if (this.aborted) return
    try {
      const response = await fetch(this.url, {
        headers: {
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
          ...this.headers,
        },
      })
      if (!response.ok) {
        this.scheduleReconnect()
        return
      }
      const body = response.body
      if (!body) {
        this.scheduleReconnect()
        return
      }
      this.reader = body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await this.reader.read()
        if (done || this.aborted) break

        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''

        for (const part of parts) {
          this.parseEvent(part)
        }
      }
    } catch {
      // connection error
    }
    if (!this.aborted) {
      this.scheduleReconnect()
    }
  }

  private parseEvent(block: string): void {
    const lines = block.split('\n')
    let type = ''
    let data = ''
    for (const line of lines) {
      if (line.startsWith('event: ')) {
        type = line.slice(7)
      } else if (line.startsWith('data: ')) {
        data += (data ? '\n' : '') + line.slice(6)
      }
    }
    if (type) {
      this.emit(type, data)
    }
  }

  private scheduleReconnect(): void {
    if (this.aborted) return
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, RECONNECT_DELAY)
  }

  close(): void {
    this.aborted = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.reader) {
      this.reader.cancel()
      this.reader = null
    }
    this.listeners.clear()
  }
}
