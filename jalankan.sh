#!/usr/bin/env bash
# Jalankan Stok Gudang di Linux/macOS. Build otomatis bila jar belum ada (butuh JDK 17+ dan Maven).
set -e
cd "$(dirname "$0")"
if [ ! -f target/stok-gudang.jar ]; then
  echo "Membangun aplikasi, tunggu sebentar..."
  mvn -q -DskipTests package
fi
exec java -jar target/stok-gudang.jar
