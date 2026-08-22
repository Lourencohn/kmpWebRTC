package app.trovata.cast.di

import app.trovata.cast.data.signaling.KtorSignalingTransport
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.iceServersFor
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.signalingUrlFor
import app.trovata.cast.feature.call.CallSpec
import app.trovata.cast.feature.call.LiveCallScreenModel
import app.trovata.cast.feature.call.PeerSession
import app.trovata.cast.platform.ServerConfig
import app.trovata.cast.protocol.PeerRole
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.onClose

class CallSession

val callModule = module {
    scope<CallSession> {
        scoped {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        } onClose { it?.cancel() }

        scoped { (spec: CallSpec) ->
            SignalingClient(
                transport = KtorSignalingTransport(
                    httpClient = get<HttpClient>(DefaultHttpClient),
                    wsUrl = signalingUrlFor(ServerConfig.baseUrl, spec.token),
                ),
                peerId = spec.sellerPeerId,
                role = PeerRole.Seller,
                displayName = spec.sellerName,
                scope = get { parametersOf(spec) },
            )
        } onClose { it?.dispose() }

        scoped { (spec: CallSpec) ->
            val sessionsApi = get<SessionsApi>()
            PeerSession(
                signaling = get { parametersOf(spec) },
                selfPeerId = spec.sellerPeerId,
                selfRole = PeerRole.Seller,
                iceServersProvider = { sessionsApi.iceServersFor(spec.token) },
                scope = get { parametersOf(spec) },
            )
        } onClose { it?.dispose() }

        scoped { (spec: CallSpec) ->
            LiveCallScreenModel(
                spec = spec,
                signaling = get { parametersOf(spec) },
                peer = get { parametersOf(spec) },
                cartRepository = get(),
                orderRepository = get(),
                vitrineApi = get(),
                callScope = this,
            )
        }
    }
}
