#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

BIN_DIR=build
BIN_NAME=app

# Make sure GraalVM is installed
# ./graalvm-ce-dev.sh 17

# pushd ~/code/compose-mpp-playground >/dev/null
# ./gradlew packageUberJarForCurrentOS

kotlinc -version \
  -verbose \
  -include-runtime \
  -java-parameters \
  -jvm-target 17 \
  -api-version 1.7 \
  -language-version 1.7 \
  -progressive \
  src/main/kotlin/dev/suresh/Main.kt -d "${BIN_DIR}/${BIN_NAME}.jar"

echo "Generating Graalvm config files..."
# java -agentlib:native-image-agent=config-output-dir=config -jar desktop/build/compose/jars/jvm-macos-*.jar
# java -agentlib:native-image-agent=config-output-dir=build/config -jar build/tmp/app.jar

echo "Creating native image ${BIN_DIR}/${BIN_NAME}..."
native-image "$@" \
  --no-fallback \
  --native-image-info \
  --link-at-build-time \
  --install-exit-handlers \
  -H:ConfigurationFileDirectories="${BIN_DIR}/config" \
  -H:+ReportExceptionStackTraces \
  -Djava.awt.headless=false \
  -J-Xmx4G \
  -jar "${BIN_DIR}/${BIN_NAME}.jar" \
  "${BIN_DIR}/${BIN_NAME}"

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
