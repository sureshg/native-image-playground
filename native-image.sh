#!/usr/bin/env bash

# ./native-image.sh --verbose --enable-https
# Make sure the latest GraalVM is installed
# ./graalvm-ce-dev.sh

set -euo pipefail
IFS=$'\n\t'

# kotlinc -version \
#  -verbose \
#  -include-runtime \
#  -java-parameters \
#  -jvm-target 18 \
#  -api-version 1.8 \
#  -language-version 1.8 \
#  -progressive \
#  src/main/kotlin/dev/suresh/Main.kt -d "${BIN_DIR}/${BIN_NAME}.jar"

echo "Building the application jar..."
./gradlew build

echo "Generating Graalvm config files..."
APP_JAR=(build/libs/native-image-playground-*-all.jar)
CONFIG_DIR="$(PWD)/build/config"
nohup java \
      --show-version \
      --enable-preview \
      -agentlib:native-image-agent=config-output-dir="${CONFIG_DIR}" \
      -jar "${APP_JAR}" &
# Wait for the server to startup
sleep 1
curl -fsSL http://localhost:9080/test
curl -fsSL http://localhost:9080/shutdown || echo "Shutdown completed!"
# Wait for agent to write the config
sleep 1

echo "Creating native image..."
native-image "$@" \
  --no-fallback \
  --native-image-info \
  --link-at-build-time \
  --install-exit-handlers \
  -H:ConfigurationFileDirectories="${CONFIG_DIR}" \
  -H:+ReportExceptionStackTraces \
  -Djava.awt.headless=false \
  -J-Xmx4G \
  -jar "${APP_JAR}" \
  "build/native-image-playground"

# https://www.graalvm.org/reference-manual/native-image/Options
# --verbose \
# --dry-run \
# --static \
# --libc=musl  \
# --libc=glibc \
# --gc=epsilon \
# // Enable quick build
# -Ob \
# // generate debug info and no optimizations
# -g -O0 \
# --enable-https \
# --enable-http \
# --enable-all-security-services \
# --enable-url-protocols=http,https,file,jar \
# --report-unsupported-elements-at-runtime \
# --initialize-at-build-time=kotlinx,kotlin,org.slf4j \
# --initialize-at-run-time=... \
# --trace-class-initialization=... \
# --trace-object-instantiation \
# --diagnostics-mode \
# -Dauthor="$USER" \
# -H:DefaultLocale="en-US" \
# -H:+AddAllCharsets \
# -H:+IncludeAllLocales  \
# -H:+IncludeAllTimeZones \
# -H:IncludeResources=<string>* \
# -H:IncludeResourceBundles=<string>* \
# -H:+AddAllFileSystemProviders \
# -H:+ReportUnsupportedElementsAtRuntime \
# -H:ReflectionConfigurationFiles=./META-INF/native-image/reflect-config.json \
# -H:CLibraryPath=".../lib" \
# -H:DashboardDump=dashboard \
# -H:+DashboardHeap \
# -H:+DashboardCode \
# // -H:+DashboardAll \
# // -H:+DashboardPointsTo \
# -H:CompilerBackend=llvm \
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles

# echo "Compressing executable ... "
# upx build/kotlin-app
# popd >/dev/null
