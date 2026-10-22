#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
CONTAINER="trovatacast-s3"
BUCKET="trovatacast-presentations"
PORT="${PRESENTATION_S3_PORT:-8333}"

docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
docker run -d --name "$CONTAINER" \
  -p "127.0.0.1:${PORT}:8333" \
  -v "$HERE/s3-local.json:/etc/seaweedfs/s3.json:ro" \
  chrislusf/seaweedfs:latest server -s3 -s3.config=/etc/seaweedfs/s3.json >/dev/null

until [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${PORT}/")" != "000" ]; do sleep 1; done
until docker exec "$CONTAINER" sh -c "echo 's3.bucket.create -name $BUCKET' | weed shell" 2>/dev/null | grep -q "$BUCKET"; do sleep 2; done

cat <<EOF
Armazenamento S3 local pronto em http://127.0.0.1:${PORT}, bucket ${BUCKET}.
Variáveis para o signalingServer:
  PRESENTATION_S3_ENDPOINT=http://localhost:${PORT}
  PRESENTATION_S3_BUCKET=${BUCKET}
  PRESENTATION_S3_REGION=us-east-1
  PRESENTATION_S3_ACCESS_KEY=trovatacast-local
  PRESENTATION_S3_SECRET_KEY=trovatacast-local-secret
EOF
