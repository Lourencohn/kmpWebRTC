import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.cocoapods)
}

val appEnvironment = providers.gradleProperty("trovata.environment").getOrElse("staging")
val appVersionName = providers.gradleProperty("trovata.versionName").get()
val appVersionCode = providers.gradleProperty("trovata.versionCode").get().toInt()

val generateAppConfig by tasks.registering {
    val environmentFile = file("environments/$appEnvironment.properties")
    val localOverrides = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }.filterKeys { it.toString().startsWith("trovata.") }
        .mapKeys { it.key.toString().removePrefix("trovata.") }
        .mapValues { it.value.toString() }
    val outputDir = layout.buildDirectory.dir("generated/appConfig/kotlin")

    inputs.file(environmentFile)
    inputs.property("environment", appEnvironment)
    inputs.property("versionName", appVersionName)
    inputs.property("overrides", localOverrides)
    outputs.dir(outputDir)

    doLast {
        val values = Properties().apply { environmentFile.inputStream().use { load(it) } }
            .entries.associate { it.key.toString() to it.value.toString() } + localOverrides
        fun required(key: String) = values[key] ?: error("$key ausente em ${environmentFile.name}")
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        val catalogWebUrlOverride = values["catalogWebUrlOverride"]?.takeIf { it.isNotBlank() }?.let(::literal) ?: "null"
        val target = outputDir.get().file("app/trovata/cast/config/AppConfig.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(
            """
            |package app.trovata.cast.config
            |
            |object AppConfig {
            |    const val environment: String = ${literal(appEnvironment)}
            |    const val versionName: String = ${literal(appVersionName)}
            |    const val signalingUrl: String = ${literal(required("signalingUrl"))}
            |    const val sfaApiUrl: String = ${literal(required("sfaApiUrl"))}
            |    const val laravelApiUrl: String = ${literal(required("laravelApiUrl"))}
            |    const val keycloakUrl: String = ${literal(required("keycloakUrl"))}
            |    const val keycloakRealm: String = ${literal(required("keycloakRealm"))}
            |    const val keycloakClientId: String = ${literal(required("keycloakClientId"))}
            |    const val defaultEmpresaId: Long = ${required("defaultEmpresaId").toLong()}L
            |    const val catalogWebUrl: String = ${literal(required("catalogWebUrl"))}
            |    val catalogWebUrlOverride: String? = $catalogWebUrlOverride
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }

    cocoapods {
        version = "0.1.0"
        summary = "TrovataCast shared composeApp module"
        homepage = "https://trovata.cast"
        ios.deploymentTarget = "15.0"

        framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        pod("WebRTC-SDK") {
            version = "125.6422.07"
            linkOnly = true
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppConfig)
        }
        commonMain.dependencies {
            implementation(projects.protocol)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kermit)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.qrose)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.webrtc.kmp)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.ktor.client.mock)
        }
        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "app.trovata.cast"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.trovata.cast"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("TrovataDatabase") {
            packageName.set("app.trovata.cast.db")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.trovata.cast.resources"
}
