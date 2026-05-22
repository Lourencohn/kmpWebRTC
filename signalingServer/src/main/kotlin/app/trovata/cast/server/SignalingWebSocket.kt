package app.trovata.cast.server

import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeSignaling
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText

fun Application.signalingRoutes(rooms: RoomManager, store: SessionStore) {
    routing {
        webSocket("/ws/session/{token}") {
            val token = call.parameters["token"].orEmpty()
            if (store.get(token) == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unknown_token"))
                return@webSocket
            }
            handleSignaling(token, rooms)
        }
    }
}

private suspend fun WebSocketServerSession.handleSignaling(token: String, rooms: RoomManager) {
    val hello = awaitHello() ?: run {
        close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "missing_hello"))
        return
    }
    val room = rooms.getOrCreate(token)
    val peer = RoomPeer(hello.peerId, hello.role, hello.displayName, this)
    when (val result = room.join(peer)) {
        Room.JoinResult.Full -> {
            peer.send(SignalingMessage.ProtocolError("room_full", "Já há dois participantes."))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "room_full"))
            rooms.closeIfEmpty(token)
            return
        }
        Room.JoinResult.RoleTaken -> {
            peer.send(SignalingMessage.ProtocolError("role_taken", "Esse papel já está ocupado."))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "role_taken"))
            rooms.closeIfEmpty(token)
            return
        }
        is Room.JoinResult.Ok -> {
            val you = peer.summary()
            val peers = result.peers.map(RoomPeer::summary)
            peer.send(SignalingMessage.RoomState(peers = peers, youAre = you))
            room.broadcastExcept(peer.peerId, SignalingMessage.PeerJoined(you))
        }
    }
    try {
        relayLoop(peer, room, this)
    } finally {
        val remaining = room.leave(peer.peerId)
        remaining.forEach { other ->
            other.send(SignalingMessage.PeerLeft(peer.peerId, "disconnected"))
        }
        rooms.closeIfEmpty(token)
    }
}

private suspend fun WebSocketServerSession.awaitHello(): SignalingMessage.Hello? {
    for (frame in incoming) {
        if (frame !is Frame.Text) continue
        val parsed = runCatching { decodeSignaling(frame.readText()) }.getOrNull() ?: continue
        return parsed as? SignalingMessage.Hello
    }
    return null
}

private suspend fun relayLoop(peer: RoomPeer, room: Room, session: WebSocketServerSession) {
    for (frame in session.incoming) {
        if (frame !is Frame.Text) continue
        val parsed = runCatching { decodeSignaling(frame.readText()) }.getOrNull() ?: continue
        when (parsed) {
            is SignalingMessage.Hello -> {
                peer.send(SignalingMessage.ProtocolError("duplicate_hello", "Hello já foi enviado."))
            }
            is SignalingMessage.Offer,
            is SignalingMessage.Answer,
            is SignalingMessage.IceCandidate,
            is SignalingMessage.PresencePing -> {
                forward(parsed, peer, room)
            }
            is SignalingMessage.Bye -> {
                room.broadcastExcept(peer.peerId, parsed)
                return
            }
            is SignalingMessage.RoomState,
            is SignalingMessage.PeerJoined,
            is SignalingMessage.PeerLeft,
            is SignalingMessage.ProtocolError -> {
                /* server-originated only — ignore from clients */
            }
        }
    }
}

private suspend fun forward(message: SignalingMessage, sender: RoomPeer, room: Room) {
    val to = message.targetPeer()
    val ok = when {
        to == null -> {
            room.broadcastExcept(sender.peerId, message)
            true
        }
        else -> room.sendTo(to, message)
    }
    if (!ok) {
        sender.send(SignalingMessage.ProtocolError("no_target", "Peer alvo não está na sala."))
    }
}

private fun SignalingMessage.targetPeer(): String? = when (this) {
    is SignalingMessage.Offer -> to
    is SignalingMessage.Answer -> to
    is SignalingMessage.IceCandidate -> to
    else -> null
}
