# iosApp — bootstrap iOS do TrovataCast

App Swift que hospeda a UI Compose Multiplatform (`composeApp/`) via framework Kotlin/Native.

## Pré-requisitos (uma vez por máquina)

```bash
# Xcode 15.4+ (App Store)
xcode-select --install

# Homebrew + ferramentas
brew install xcodegen
# CocoaPods só será necessário a partir do Milestone 5 (WebRTC.framework)
```

## Gerar o projeto Xcode

```bash
cd iosApp
xcodegen generate
```

Isso cria `iosApp.xcodeproj` a partir de `project.yml`. **Não comitar o `.xcodeproj`** — ele é gerado.

## Rodar

```bash
open iosApp.xcodeproj
# Selecione um simulador (iPhone 15) e cmd+R
```

O build executa automaticamente o script `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, que:
1. Compila a framework Kotlin `ComposeApp.framework` para o SDK alvo.
2. Embute e assina no app bundle.

## Estrutura

```
iosApp/
├── project.yml              ← spec do projeto Xcode (XcodeGen)
├── iosApp/
│   ├── iOSApp.swift         ← entry point SwiftUI
│   ├── ContentView.swift    ← bridge SwiftUI → MainViewController() do Compose
│   ├── Info.plist
│   └── Assets.xcassets/
└── README.md
```

## Troubleshooting

| Sintoma | Causa provável | Como resolver |
|---|---|---|
| `xcodegen: command not found` | Falta instalar | `brew install xcodegen` |
| `No such module 'ComposeApp'` | Framework não foi gerada | Rode `./gradlew :composeApp:linkPodReleaseFrameworkIosX64` ou faça Clean Build em Xcode |
| Build trava em "Run Build Kotlin Framework" | Gradle daemon travado | `./gradlew --stop` e build de novo |
| Falha de assinatura | Sem time configurado | Em Xcode, Target > Signing & Capabilities, escolha um team pessoal |
