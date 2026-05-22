import {
  isSignalingMessage,
  type Hello,
  type PeerRole,
  type SignalingMessage,
} from './messages'

export type SignalingStatus = 'idle' | 'connecting' | 'connected' | 'failed' | 'closed'

export type SignalingClientOptions = {
  peerId: string
  role: PeerRole
  displayName?: string
}

export interface WebSocketLike {
  readonly readyState: number
  send(data: string): void
  close(code?: number, reason?: string): void
  addEventListener<K extends 'open' | 'message' | 'error' | 'close'>(
    type: K,
    listener: (event: Event | MessageEvent | CloseEvent) => void,
  ): void
}

export type WebSocketFactory = (url: string) => WebSocketLike

const READY_STATE_OPEN = 1

export class SignalingClient {
  private socket: WebSocketLike | null = null
  private listeners: ((m: SignalingMessage) => void)[] = []
  private statusListeners: ((s: SignalingStatus, err?: string) => void)[] = []
  private pending: string[] = []
  private _status: SignalingStatus = 'idle'

  constructor(
    private readonly url: string,
    private readonly options: SignalingClientOptions,
    private readonly socketFactory: WebSocketFactory = (u) => new WebSocket(u) as WebSocketLike,
  ) {}

  get status(): SignalingStatus {
    return this._status
  }

  onMessage(listener: (m: SignalingMessage) => void): () => void {
    this.listeners.push(listener)
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener)
    }
  }

  onStatus(listener: (s: SignalingStatus, err?: string) => void): () => void {
    this.statusListeners.push(listener)
    return () => {
      this.statusListeners = this.statusListeners.filter((l) => l !== listener)
    }
  }

  start(): void {
    if (this._status === 'connecting' || this._status === 'connected') return
    this.setStatus('connecting')
    const socket = this.socketFactory(this.url)
    this.socket = socket
    socket.addEventListener('open', () => {
      const hello: Hello = {
        type: 'hello',
        role: this.options.role,
        peerId: this.options.peerId,
        displayName: this.options.displayName ?? null,
      }
      socket.send(JSON.stringify(hello))
      this.pending.splice(0).forEach((raw) => socket.send(raw))
      this.setStatus('connected')
    })
    socket.addEventListener('message', (event) => {
      const data = (event as MessageEvent).data
      if (typeof data !== 'string') return
      let parsed: unknown
      try {
        parsed = JSON.parse(data)
      } catch {
        return
      }
      if (!isSignalingMessage(parsed)) return
      if (parsed.type === 'error') {
        this.setStatus('failed', parsed.message)
      }
      this.listeners.forEach((l) => l(parsed))
    })
    socket.addEventListener('close', (event) => {
      const reason = (event as CloseEvent).reason
      if (this._status !== 'failed') this.setStatus('closed', reason || undefined)
      this.socket = null
    })
    socket.addEventListener('error', () => {
      this.setStatus('failed', 'transport error')
    })
  }

  send(message: SignalingMessage): void {
    const raw = JSON.stringify(message)
    const socket = this.socket
    if (socket && socket.readyState === READY_STATE_OPEN) {
      socket.send(raw)
    } else {
      this.pending.push(raw)
    }
  }

  close(reason?: string): void {
    if (!this.socket) return
    try {
      const bye: SignalingMessage = {
        type: 'bye',
        from: this.options.peerId,
        reason: reason ?? null,
      }
      this.send(bye)
    } finally {
      this.socket?.close(1000, reason)
      this.socket = null
      this.setStatus('closed', reason)
    }
  }

  private setStatus(status: SignalingStatus, err?: string): void {
    if (this._status === status) return
    this._status = status
    this.statusListeners.forEach((l) => l(status, err))
  }
}
