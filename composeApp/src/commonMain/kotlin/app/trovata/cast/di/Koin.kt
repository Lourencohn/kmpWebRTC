package app.trovata.cast.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun appModules(): List<Module> = listOf(
    platformModule(),
    databaseModule,
    networkModule,
    repositoryModule,
    authModule,
    syncModule,
    screenModelModule,
    callModule,
)

fun initKoin(config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModules())
}
