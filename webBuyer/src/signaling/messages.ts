export type PeerRole = 'Seller' | 'Buyer'

export type PeerSummary = {
  peerId: string
  role: PeerRole
  displayName?: string | null
}

export type Hello = {
  type: 'hello'
  role: PeerRole
  peerId: string
  displayName?: string | null
}

export type RoomState = {
  type: 'roomState'
  peers: PeerSummary[]
  youAre: PeerSummary
}

export type PeerJoined = { type: 'peerJoined'; peer: PeerSummary }
export type PeerLeft = { type: 'peerLeft'; peerId: string; reason?: string | null }
export type Offer = { type: 'offer'; sdp: string; from: string; to?: string | null }
export type Answer = { type: 'answer'; sdp: string; from: string; to?: string | null }
export type IceCandidateMsg = {
  type: 'ice'
  sdpMid: string
  sdpMLineIndex: number
  candidate: string
  from: string
  to?: string | null
}
export type Bye = { type: 'bye'; from: string; reason?: string | null }
export type PresencePing = { type: 'presencePing'; from: string; sentAtMs: number }
export type ProtocolError = { type: 'error'; code: string; message: string }

export type SignalingMessage =
  | Hello
  | RoomState
  | PeerJoined
  | PeerLeft
  | Offer
  | Answer
  | IceCandidateMsg
  | Bye
  | PresencePing
  | ProtocolError

export function isSignalingMessage(value: unknown): value is SignalingMessage {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return typeof v.type === 'string'
}
