import { SignalingClient } from '../signaling/client'
import type { SignalingMessage } from '../signaling/messages'

export type PeerStatus = 'idle' | 'negotiating' | 'connected' | 'failed' | 'closed'

export type PeerSessionOptions = {
  peerId: string
  selfRole: 'Seller' | 'Buyer'
  iceServers?: RTCIceServer[]
  localAudio?: MediaStream | null
  presenceIntervalMs?: number
}

export type PeerEvents = {
  onStatus?: (status: PeerStatus, detail?: string) => void
  onDataChannelOpen?: () => void
  onDataChannelMessage?: (raw: string) => void
  onRemoteAudio?: (stream: MediaStream) => void
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [
  { urls: ['stun:stun.l.google.com:19302', 'stun:stun1.l.google.com:19302'] },
]

export class PeerSession {
  private pc: RTCPeerConnection | null = null
  private dc: RTCDataChannel | null = null
  private peerId: string | null = null
  private offerSent = false
  private presenceTimer: ReturnType<typeof setInterval> | null = null
  private offMessage: (() => void) | null = null
  private offStatus: (() => void) | null = null
  private _status: PeerStatus = 'idle'

  constructor(
    private readonly signaling: SignalingClient,
    private readonly options: PeerSessionOptions,
    private readonly events: PeerEvents = {},
  ) {}

  get status(): PeerStatus {
    return this._status
  }

  start(): void {
    if (this._status !== 'idle' && this._status !== 'closed' && this._status !== 'failed') return
    this.setStatus('negotiating')
    this.pc = new RTCPeerConnection({
      iceServers: this.options.iceServers ?? DEFAULT_ICE_SERVERS,
    })
    this.installPeerHandlers(this.pc)
    if (this.options.localAudio) {
      this.options.localAudio.getTracks().forEach((track) => {
        this.pc!.addTrack(track, this.options.localAudio!)
      })
    }
    this.offMessage = this.signaling.onMessage((m) => void this.handleSignal(m))
    this.offStatus = this.signaling.onStatus((s) => {
      if (s === 'failed' || s === 'closed') this.setStatus('failed', `signaling ${s}`)
    })
  }

  private maybeCreateOfferAsBuyer(): void {
    if (this.options.selfRole !== 'Buyer') return
    if (!this.peerId) return
    if (this.offerSent) return
    this.offerSent = true
    void this.createOffer()
  }

  async restartIce(): Promise<void> {
    const pc = this.pc
    if (!pc) return
    this.setStatus('negotiating')
    const offer = await pc.createOffer({ iceRestart: true } as RTCOfferOptions)
    await pc.setLocalDescription(offer)
    this.sendOffer(offer.sdp ?? '')
  }

  send(data: string): boolean {
    const dc = this.dc
    if (!dc || dc.readyState !== 'open') return false
    dc.send(data)
    return true
  }

  close(reason?: string): void {
    if (this.presenceTimer != null) {
      clearInterval(this.presenceTimer)
      this.presenceTimer = null
    }
    this.offMessage?.()
    this.offStatus?.()
    this.offMessage = null
    this.offStatus = null
    this.dc?.close()
    this.dc = null
    this.pc?.close()
    this.pc = null
    this.offerSent = false
    this.setStatus('closed', reason)
  }

  private installPeerHandlers(pc: RTCPeerConnection): void {
    pc.addEventListener('icecandidate', (event) => {
      const candidate = (event as RTCPeerConnectionIceEvent).candidate
      if (!candidate || !this.peerId) return
      this.signaling.send({
        type: 'ice',
        sdpMid: candidate.sdpMid ?? '0',
        sdpMLineIndex: candidate.sdpMLineIndex ?? 0,
        candidate: candidate.candidate,
        from: this.options.peerId,
        to: this.peerId,
      })
    })
    pc.addEventListener('iceconnectionstatechange', () => {
      const state = pc.iceConnectionState
      if (state === 'connected' || state === 'completed') {
        if (this._status !== 'connected') this.setStatus('connected', state)
      } else if (state === 'failed') {
        this.setStatus('failed', 'ice_failed')
      } else if (state === 'disconnected') {
        this.setStatus('negotiating', 'ice_disconnected')
      }
    })
    pc.addEventListener('track', (event) => {
      const stream = (event as RTCTrackEvent).streams[0]
      if (stream) this.events.onRemoteAudio?.(stream)
    })
    pc.addEventListener('datachannel', (event) => {
      this.bindDataChannel((event as RTCDataChannelEvent).channel)
    })
  }

  private async createOffer(): Promise<void> {
    const pc = this.pc
    if (!pc) return
    this.dc = pc.createDataChannel('presence', { ordered: true })
    this.bindDataChannel(this.dc)
    const offer = await pc.createOffer()
    if (this.pc !== pc) return
    await pc.setLocalDescription(offer)
    if (this.pc !== pc) return
    this.sendOffer(offer.sdp ?? '')
  }

  private async handleSignal(m: SignalingMessage): Promise<void> {
    if (m.type === 'roomState') {
      const other = m.peers.find((p) => p.peerId !== this.options.peerId)
      this.peerId = other?.peerId ?? null
      this.maybeCreateOfferAsBuyer()
      return
    }
    if (m.type === 'peerJoined') {
      this.peerId = m.peer.peerId
      this.maybeCreateOfferAsBuyer()
      return
    }
    if (m.type === 'peerLeft') {
      if (m.peerId === this.peerId) this.setStatus('failed', 'peer_left')
      return
    }
    if (!this.pc) return
    if (m.type === 'offer') {
      this.peerId = m.from
      await this.pc.setRemoteDescription({ type: 'offer', sdp: m.sdp })
      const answer = await this.pc.createAnswer()
      await this.pc.setLocalDescription(answer)
      this.signaling.send({
        type: 'answer',
        sdp: answer.sdp ?? '',
        from: this.options.peerId,
        to: m.from,
      })
    } else if (m.type === 'answer') {
      await this.pc.setRemoteDescription({ type: 'answer', sdp: m.sdp })
    } else if (m.type === 'ice') {
      try {
        await this.pc.addIceCandidate({
          candidate: m.candidate,
          sdpMid: m.sdpMid,
          sdpMLineIndex: m.sdpMLineIndex,
        })
      } catch {
        /* candidate aged out; ignore */
      }
    }
  }

  private sendOffer(sdp: string): void {
    this.signaling.send({
      type: 'offer',
      sdp,
      from: this.options.peerId,
      to: this.peerId ?? undefined,
    })
  }

  private bindDataChannel(channel: RTCDataChannel): void {
    this.dc = channel
    channel.binaryType = 'arraybuffer'
    channel.addEventListener('open', () => {
      this.events.onDataChannelOpen?.()
      this.startPresenceLoop()
    })
    channel.addEventListener('message', (event) => {
      const data = (event as MessageEvent).data
      if (typeof data === 'string') {
        this.events.onDataChannelMessage?.(data)
      } else if (data instanceof ArrayBuffer) {
        this.events.onDataChannelMessage?.(new TextDecoder().decode(data))
      }
    })
    channel.addEventListener('close', () => {
      if (this.presenceTimer != null) {
        clearInterval(this.presenceTimer)
        this.presenceTimer = null
      }
    })
  }

  private startPresenceLoop(): void {
    const interval = this.options.presenceIntervalMs ?? 5_000
    if (this.presenceTimer != null) clearInterval(this.presenceTimer)
    this.presenceTimer = setInterval(() => {
      this.send(
        JSON.stringify({ kind: 'presence', at: Date.now(), from: this.options.peerId }),
      )
    }, interval)
  }

  private setStatus(status: PeerStatus, detail?: string): void {
    if (this._status === status) return
    this._status = status
    this.events.onStatus?.(status, detail)
  }
}
