package app.trovata.cast.di

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.PresentationsApi
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.AccountApi
import app.trovata.cast.data.remote.sfa.CartApi
import app.trovata.cast.data.remote.sfa.CatalogLinksApi
import app.trovata.cast.data.remote.sfa.KeycloakAuthService
import app.trovata.cast.data.remote.sfa.OpenCartsApi
import app.trovata.cast.data.remote.sfa.SfaApi
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val DefaultHttpClient = named("default")
val SfaHttpClient = named("sfa")

val networkModule = module {
    single<HttpClient>(DefaultHttpClient) { HttpClientFactory.create() }
    single<HttpClient>(SfaHttpClient) { HttpClientFactory.createSfa() }

    single {
        SessionsApi(
            client = get(DefaultHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
        )
    }
    single {
        PresentationsApi(
            client = get(DefaultHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
        )
    }
    single { KeycloakAuthService(get(SfaHttpClient)) }
    single { AccountApi(get(SfaHttpClient)) }
    single {
        CartApi(
            client = get(SfaHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
        )
    }
    single {
        OpenCartsApi(
            client = get(SfaHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
        )
    }
    single {
        CatalogLinksApi(
            client = get(SfaHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
        )
    }
    single {
        SfaApi(
            client = get(SfaHttpClient),
            tokenProvider = { get<AuthRepository>().accessToken() },
            empresaIdProvider = { get<AuthRepository>().empresaId() ?: AppConfig.defaultEmpresaId },
        )
    }
}
