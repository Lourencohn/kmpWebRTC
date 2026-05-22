package app.trovata.cast.server

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.PeerSummary
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.encode
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private const val MAX_PEERS = 2

data class RoomPeer(
    val peerId: String,
    val role: PeerRole,
    val displayName: String?,
    val socket: WebSocketSession,
) {
    fun summary(): PeerSummary = PeerSummary(peerId, role, displayName)
}

class Room(val token: String) {
    private val mutex = Mutex()
    private val peers = mutableListOf<RoomPeer>()

    suspend fun join(peer: RoomPeer): JoinResult {
        return mutex.withLock {
            if (peers.size >= MAX_PEERS) JoinResult.Full
            else if (peers.any { it.role == peer.role }) JoinResult.RoleTaken
            else {
                peers += peer
                JoinResult.Ok(snapshot())
            }
        }
    }

    suspend fun leave(peerId: String): List<RoomPeer> {
        return mutex.withLock {
            peers.removeAll { it.peerId == peerId }
            snapshot()
        }
    }

    suspend fun snapshotPeers(): List<RoomPeer> = mutex.withLock { snapshot() }

    suspend fun broadcastExcept(senderId: String, message: SignalingMessage) {
        val targets = mutex.withLock { peers.filter { it.peerId != senderId } }
        targets.forEach { it.send(message) }
    }

    suspend fun sendTo(peerId: String, message: SignalingMessage): Boolean {
        val target = mutex.withLock { peers.firstOrNull { it.peerId == peerId } }
        target?.send(message)
        return target != null
    }

    suspend fun size(): Int = mutex.withLock { peers.size }

    private fun snapshot(): List<RoomPeer> = peers.toList()

    sealed class JoinResult {
        data class Ok(val peers: List<RoomPeer>) : JoinResult()
        data object Full : JoinResult()
        data object RoleTaken : JoinResult()
    }
}

class RoomManager {
    private val rooms = ConcurrentHashMap<String, Room>()

    fun getOrCreate(token: String): Room =
        rooms.computeIfAbsent(token) { Room(token) }

    suspend fun closeIfEmpty(token: String) {
        val room = rooms[token] ?: return
        if (room.size() == 0) rooms.remove(token)
    }

    fun size(): Int = rooms.size
}

suspend fun RoomPeer.send(message: SignalingMessage) {
    socket.send(Frame.Text(message.encode()))
}
