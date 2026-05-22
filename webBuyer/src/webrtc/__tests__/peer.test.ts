import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SignalingClient, type WebSocketLike } from '../../signaling/client'
import type { SignalingMessage } from '../../signaling/messages'
import { PeerSession } from '../peer'

type Handler = (event: any) => void

class FakeDataChannel {
  readyState: 'connecting' | 'open' | 'closing' | 'closed' = 'connecting'
  sent: string[] = []
  private handlers: Record<string, Handler[]> = {}
  send(data: string): void {
    this.sent.push(data)
  }
  close(): void {
    this.readyState = 'closed'
    this.fire('close', {})
  }
  addEventListener(type: string, listener: Handler): void {
    ;(this.handlers[type] ||= []).push(listener)
  }
  fire(type: string, event: any): void {
    this.handlers[type]?.forEach((h) => h(event))
  }
  open(): void {
    this.readyState = 'open'
    this.fire('open', {})
  }
}

class FakePeerConnection {
  iceConnectionState: RTCIceConnectionState = 'new'
  localDescription: RTCSessionDescription | null = null
  remoteDescription: RTCSessionDescription | null = null
  closed = false
  addedTracks: Array<{ track: MediaStreamTrack; stream: MediaStream }> = []
  addedIce: RTCIceCandidateInit[] = []
  dataChannels: FakeDataChannel[] = []
  private handlers: Record<string, Handler[]> = {}

  addTrack(track: MediaStreamTrack, stream: MediaStream): RTCRtpSender {
    this.addedTracks.push({ track, stream })
    return {} as RTCRtpSender
  }
  createDataChannel(_label: string): RTCDataChannel {
    const dc = new FakeDataChannel()
    this.dataChannels.push(dc)
    return dc as unknown as RTCDataChannel
  }
  async createOffer(_opts?: RTCOfferOptions): Promise<RTCSessionDescriptionInit> {
    return { type: 'offer', sdp: 'v=0-offer' }
  }
  async createAnswer(): Promise<RTCSessionDescriptionInit> {
    return { type: 'answer', sdp: 'v=0-answer' }
  }
  async setLocalDescription(desc: RTCSessionDescriptionInit): Promise<void> {
    this.localDescription = desc as RTCSessionDescription
  }
  async setRemoteDescription(desc: RTCSessionDescriptionInit): Promise<void> {
    this.remoteDescription = desc as RTCSessionDescription
  }
  async addIceCandidate(candidate: RTCIceCandidateInit): Promise<void> {
    this.addedIce.push(candidate)
  }
  close(): void {
    this.closed = true
  }
  addEventListener(type: string, listener: Handler): void {
    ;(this.handlers[type] ||= []).push(listener)
  }
  fire(type: string, event: any): void {
    this.handlers[type]?.forEach((h) => h(event))
  }
  emitIce(candidate: { candidate: string; sdpMid: string; sdpMLineIndex: number } | null): void {
    this.fire('icecandidate', { candidate })
  }
  emitIceState(state: RTCIceConnectionState): void {
    this.iceConnectionState = state
    this.fire('iceconnectionstatechange', {})
  }
  emitDataChannel(dc: FakeDataChannel): void {
    this.fire('datachannel', { channel: dc })
  }
}

class FakeSocket implements WebSocketLike {
  readyState = 1
  sent: string[] = []
  private handlers: Record<string, Handler[]> = {}
  send(data: string): void {
    this.sent.push(data)
  }
  close(_code?: number, reason?: string): void {
    this.fire('close', { reason: reason ?? '' })
  }
  addEventListener(type: string, listener: Handler): void {
    ;(this.handlers[type] ||= []).push(listener)
  }
  fire(type: string, event: any): void {
    this.handlers[type]?.forEach((h) => h(event))
  }
  open(): void {
    this.fire('open', {})
  }
  message(payload: unknown): void {
    this.fire('message', { data: JSON.stringify(payload) })
  }
  sentSignaling(): SignalingMessage[] {
    return this.sent.map((raw) => JSON.parse(raw) as SignalingMessage)
  }
}

const originalRTC = globalThis.RTCPeerConnection
let fakePc: FakePeerConnection

function installFakeRtc(): void {
  fakePc = new FakePeerConnection()
  ;(globalThis as any).RTCPeerConnection = vi.fn(() => fakePc)
}

beforeEach(() => {
  installFakeRtc()
})

afterEach(() => {
  ;(globalThis as any).RTCPeerConnection = originalRTC
  vi.useRealTimers()
})

function makeSignaling(): { client: SignalingClient; socket: FakeSocket } {
  const socket = new FakeSocket()
  const client = new SignalingClient(
    'ws://srv',
    { peerId: 'p-buyer', role: 'Buyer', displayName: 'Diego' },
    () => socket,
  )
  client.start()
  socket.open()
  return { client, socket }
}

describe('PeerSession (buyer)', () => {
  it('cria offer assim que conhece o peer remoto', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    socket.message({
      type: 'roomState',
      peers: [
        { peerId: 'p-buyer', role: 'Buyer' },
        { peerId: 'p-seller', role: 'Seller' },
      ],
      youAre: { peerId: 'p-buyer', role: 'Buyer' },
    })
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    const offerSent = socket.sentSignaling().find((m) => m.type === 'offer')
    expect(offerSent).toBeDefined()
    expect((offerSent as any).from).toBe('p-buyer')
    expect((offerSent as any).to).toBe('p-seller')
    expect(fakePc.localDescription?.type).toBe('offer')
  })

  it('emite candidates ICE para o peer descoberto', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    socket.message({ type: 'peerJoined', peer: { peerId: 'p-seller', role: 'Seller' } })
    fakePc.emitIce({
      candidate: 'candidate:abc',
      sdpMid: '0',
      sdpMLineIndex: 0,
    })
    const ice = socket.sentSignaling().find((m) => m.type === 'ice')
    expect(ice).toMatchObject({ candidate: 'candidate:abc', sdpMid: '0', sdpMLineIndex: 0 })
  })

  it('responde answer quando recebe offer (papel inverso)', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-seller', selfRole: 'Seller' })
    session.start()
    socket.message({ type: 'offer', sdp: 'v=remote', from: 'p-buyer', to: 'p-seller' })
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    const answer = socket.sentSignaling().find((m) => m.type === 'answer')
    expect(answer).toMatchObject({ from: 'p-seller', to: 'p-buyer' })
    expect(fakePc.remoteDescription?.sdp).toBe('v=remote')
  })

  it('aplica answer recebido', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    socket.message({ type: 'peerJoined', peer: { peerId: 'p-seller', role: 'Seller' } })
    socket.message({ type: 'answer', sdp: 'v=ans', from: 'p-seller', to: 'p-buyer' })
    await Promise.resolve()
    await Promise.resolve()
    expect(fakePc.remoteDescription?.sdp).toBe('v=ans')
  })

  it('adiciona candidates ICE recebidos', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    socket.message({
      type: 'ice',
      sdpMid: '0',
      sdpMLineIndex: 0,
      candidate: 'candidate:remote',
      from: 'p-seller',
      to: 'p-buyer',
    })
    await Promise.resolve()
    expect(fakePc.addedIce).toHaveLength(1)
    expect(fakePc.addedIce[0]?.candidate).toBe('candidate:remote')
  })

  it('emite status connected quando ICE conecta', () => {
    const { client } = makeSignaling()
    const statuses: string[] = []
    const session = new PeerSession(
      client,
      { peerId: 'p-buyer', selfRole: 'Buyer' },
      { onStatus: (s) => statuses.push(s) },
    )
    session.start()
    fakePc.emitIceState('connected')
    expect(statuses).toContain('connected')
  })

  it('falha se signaling cair', () => {
    const { client, socket } = makeSignaling()
    const statuses: string[] = []
    const session = new PeerSession(
      client,
      { peerId: 'p-buyer', selfRole: 'Buyer' },
      { onStatus: (s) => statuses.push(s) },
    )
    session.start()
    socket.fire('error', new Event('error'))
    expect(statuses).toContain('failed')
  })

  it('manda presence pings via DC ao abrir', async () => {
    vi.useFakeTimers()
    const { client } = makeSignaling()
    const session = new PeerSession(client, {
      peerId: 'p-buyer',
      selfRole: 'Buyer',
      presenceIntervalMs: 1_000,
    })
    session.start()
    const dc = fakePc.dataChannels[0]!
    dc.open()
    vi.advanceTimersByTime(2_500)
    expect(dc.sent.length).toBeGreaterThanOrEqual(2)
    const parsed = JSON.parse(dc.sent[0])
    expect(parsed.kind).toBe('presence')
  })

  it('close limpa pc/dc/timers', () => {
    const { client } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    session.close('bye')
    expect(fakePc.closed).toBe(true)
    expect(session.status).toBe('closed')
  })

  it('restartIce cria offer com flag e envia novo offer', async () => {
    const { client, socket } = makeSignaling()
    const session = new PeerSession(client, { peerId: 'p-buyer', selfRole: 'Buyer' })
    session.start()
    socket.message({ type: 'peerJoined', peer: { peerId: 'p-seller', role: 'Seller' } })
    socket.sent.length = 0
    await session.restartIce()
    const offerSent = socket.sentSignaling().find((m) => m.type === 'offer')
    expect(offerSent).toBeDefined()
  })
})
