#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

GRADLE_VERSION="8.11.1"
GRADLE_DIR=".gradle-bootstrap/gradle-${GRADLE_VERSION}"
GRADLE_BIN=""

if [ -x "./gradlew" ]; then
  echo "[bootstrap] Wrapper já existe (./gradlew). Pulando geração."
else
  if command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="gradle"
    echo "[bootstrap] Usando gradle do PATH ($(gradle --version | head -2 | tail -1))."
  else
    if [ ! -x "${GRADLE_DIR}/bin/gradle" ]; then
      echo "[bootstrap] Gradle não está no PATH — baixando ${GRADLE_VERSION} localmente em .gradle-bootstrap/ (~130MB, só uma vez)..."
      mkdir -p .gradle-bootstrap
      DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
      curl -fL --progress-bar -o ".gradle-bootstrap/gradle.zip" "${DIST_URL}"
      unzip -q ".gradle-bootstrap/gradle.zip" -d ".gradle-bootstrap/"
      rm ".gradle-bootstrap/gradle.zip"
    fi
    GRADLE_BIN="${GRADLE_DIR}/bin/gradle"
    echo "[bootstrap] Usando gradle local ($(${GRADLE_BIN} --version | head -2 | tail -1))."
  fi
  echo "[bootstrap] Gerando Gradle wrapper..."
  "${GRADLE_BIN}" wrapper --gradle-version "${GRADLE_VERSION}" --distribution-type bin
fi

if [ -d webBuyer ]; then
  if command -v npm >/dev/null 2>&1; then
    echo "[bootstrap] Instalando dependências do webBuyer..."
    ( cd webBuyer && npm install --silent )
  else
    echo "[bootstrap] (skip) npm não encontrado — instale Node 20+ pra rodar o webBuyer."
  fi
fi

if [ -d iosApp ]; then
  if command -v xcodegen >/dev/null 2>&1; then
    echo "[bootstrap] Gerando projeto Xcode..."
    ( cd iosApp && xcodegen generate )
  else
    echo "[bootstrap] (skip) xcodegen não encontrado — 'brew install xcodegen' pra gerar iosApp/iosApp.xcodeproj."
  fi
fi

echo ""
echo "[bootstrap] ✅ Pronto."
echo "[bootstrap] Próximos comandos úteis:"
echo "  ./gradlew :protocol:jvmTest                     # roda testes do protocolo"
echo "  ./gradlew :signalingServer:run                   # sobe o servidor Ktor em :8080"
echo "  ./gradlew :composeApp:assembleDebug              # build Android"
echo "  (cd webBuyer && npm run dev)                     # web buyer em :5173"
