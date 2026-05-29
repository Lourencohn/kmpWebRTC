package app.trovata.cast.di

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.AccountApi
import app.trovata.cast.data.remote.sfa.KeycloakAuthService
import app.trovata.cast.data.remote.sfa.SfaApi
import app.trovata.cast.data.remote.sfa.SfaConfig
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val DefaultHttpClient = named("default")
val SfaHttpClient = named("sfa")

val networkModule = module {
    single<HttpClient>(DefaultHttpClient) { HttpClientFactory.create() }
    single<HttpClient>(SfaHttpClient) { HttpClientFactory.createSfa() }

    single { SessionsApi(get(DefaultHttpClient)) }
    single { KeycloakAuthService(get(SfaHttpClient)) }
    single { AccountApi(get(SfaHttpClient)) }
    single {
        SfaApi(
            client = get(SfaHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
            empresaIdProvider = { get<AuthRepository>().empresaId() ?: SfaConfig.empresaId },
        )
    }
}
