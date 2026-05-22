import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SignalingClient, type WebSocketLike } from '../client'
import type { SignalingMessage } from '../messages'

type EventName = 'open' | 'message' | 'error' | 'close'

class FakeSocket implements WebSocketLike {
  readyState = 0
  sent: string[] = []
  closed: { code?: number; reason?: string } | null = null
  private handlers: Record<EventName, Array<(event: any) => void>> = {
    open: [],
    message: [],
    error: [],
    close: [],
  }

  send(data: string): void {
    this.sent.push(data)
  }

  close(code?: number, reason?: string): void {
    this.closed = { code, reason }
    this.readyState = 3
    this.fire('close', { reason: reason ?? '' })
  }

  addEventListener<K extends EventName>(type: K, listener: (event: any) => void): void {
    this.handlers[type].push(listener)
  }

  fire(type: EventName, event: any): void {
    this.handlers[type].forEach((h) => h(event))
  }

  open(): void {
    this.readyState = 1
    this.fire('open', {})
  }

  message(data: unknown): void {
    this.fire('message', { data: JSON.stringify(data) })
  }
}

let socket: FakeSocket

function makeClient(role: 'Seller' | 'Buyer' = 'Buyer') {
  return new SignalingClient(
    'ws://srv/ws/session/abc',
    { peerId: 'p-buyer', role, displayName: 'Diego' },
    () => socket,
  )
}

beforeEach(() => {
  socket = new FakeSocket()
})

describe('SignalingClient', () => {
  it('envia hello ao abrir e vai para connected', () => {
    const client = makeClient()
    const statuses: string[] = []
    client.onStatus((s) => statuses.push(s))
    client.start()
    expect(client.status).toBe('connecting')
    socket.open()
    expect(client.status).toBe('connected')
    expect(socket.sent).toHaveLength(1)
    const hello = JSON.parse(socket.sent[0])
    expect(hello).toMatchObject({
      type: 'hello',
      role: 'Buyer',
      peerId: 'p-buyer',
      displayName: 'Diego',
    })
    expect(statuses).toEqual(['connecting', 'connected'])
  })

  it('entrega mensagens parseadas aos listeners', () => {
    const client = makeClient()
    const onMessage = vi.fn()
    client.onMessage(onMessage)
    client.start()
    socket.open()
    const offer: SignalingMessage = { type: 'offer', sdp: 'v=0', from: 'p-seller' }
    socket.message(offer)
    expect(onMessage).toHaveBeenCalledWith(offer)
  })

  it('ignora payload não-JSON ou sem type', () => {
    const client = makeClient()
    const onMessage = vi.fn()
    client.onMessage(onMessage)
    client.start()
    socket.open()
    socket.fire('message', { data: 'not-json' })
    socket.fire('message', { data: JSON.stringify({ foo: 'bar' }) })
    expect(onMessage).not.toHaveBeenCalled()
  })

  it('protocol error coloca em failed', () => {
    const client = makeClient()
    const statuses: Array<{ s: string; err?: string }> = []
    client.onStatus((s, err) => statuses.push({ s, err }))
    client.start()
    socket.open()
    socket.message({ type: 'error', code: 'room_full', message: 'cheia' })
    expect(client.status).toBe('failed')
    expect(statuses.at(-1)).toEqual({ s: 'failed', err: 'cheia' })
  })

  it('send enfileira mensagens antes do open', () => {
    const client = makeClient()
    client.start()
    client.send({ type: 'presencePing', from: 'p-buyer', sentAtMs: 1 })
    expect(socket.sent).toHaveLength(0)
    socket.open()
    expect(socket.sent).toHaveLength(2)
    const flushed = JSON.parse(socket.sent[1])
    expect(flushed.type).toBe('presencePing')
  })

  it('send após open envia direto', () => {
    const client = makeClient()
    client.start()
    socket.open()
    client.send({ type: 'answer', sdp: 'v=0', from: 'p-buyer', to: 'p-seller' })
    const lastRaw = socket.sent.at(-1)!
    expect(JSON.parse(lastRaw).type).toBe('answer')
  })

  it('close envia bye e fecha o socket', () => {
    const client = makeClient()
    client.start()
    socket.open()
    socket.sent.length = 0
    client.close('tchau')
    expect(JSON.parse(socket.sent[0]).type).toBe('bye')
    expect(socket.closed).not.toBeNull()
    expect(client.status).toBe('closed')
  })

  it('error event move para failed', () => {
    const client = makeClient()
    client.start()
    socket.fire('error', new Event('error'))
    expect(client.status).toBe('failed')
  })
})
