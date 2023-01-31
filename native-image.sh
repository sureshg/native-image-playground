#!/usr/bin/env bash

# ./native-image.sh -Ob (For Quick Build)
# ./native-image.sh --verbose --enable-https

# Make sure the latest GraalVM is installed
# ./graalvm-ce-dev.sh

set -euo pipefail
IFS=$'\n\t'

echo "Building the application jar..."
./gradlew build

# pattern="native-image-playground-*-all.jar"
# files=($(for f in $(find . -name "$pattern" -type f); do echo "$f"; done | sort -k 1 -r))
echo "Generating Graalvm config files..."
BUILD_DIR=$(pwd)/build
APP_JAR=(${BUILD_DIR}/libs/native-image-playground-*-all.jar)
CONFIG_DIR="$(PWD)/src/main/resources/META-INF/native-image"
OUT_FILE="${BUILD_DIR}/native-image-playground"

# Run the app in background (&) by ignoring SIGHUP signal (nohup)
nohup java \
  --show-version \
  --enable-preview \
  -agentlib:native-image-agent=config-merge-dir="${CONFIG_DIR}",experimental-class-define-support \
  -jar "${APP_JAR}" &>"${BUILD_DIR}/nohup.out" &
# Wait for the server to startup
sleep 1
curl -fsSL http://localhost:9080/test
curl -fsSL http://localhost:9080/rsocket
curl -fsSL http://localhost:9080/shutdown || echo "Native Image build config generation completed!"
# Wait for agent to write the config
sleep 1

echo "Creating native image..."
rm -f "${OUT_FILE}"
# Allowing an incomplete classpath is now the default. Use "--link-at-build-time"
# to report linking errors at image build time for a class or package.
native-image "$@" \
  --no-fallback \
  --enable-preview \
  --native-image-info \
  --enable-monitoring=all \
  --install-exit-handlers \
  -H:ConfigurationFileDirectories="${CONFIG_DIR}" \
  -H:+ReportExceptionStackTraces \
  -H:+StripDebugInfo \
  -H:ErrorFile=svm_err_b_%t_pid%p.md \
  -Djava.awt.headless=false \
  -jar "${APP_JAR}" \
  -o "${OUT_FILE}"

# https://www.graalvm.org/reference-manual/native-image/overview/BuildOptions/
# --verbose \
# --dry-run \
# --static \
# --static --libc=<glib | musl | bionic>
# --target=darwin-aarch64
# -H:+StaticExecutableWithDynamicLibC \
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
# -H:+PrintAnalysisCallTree \
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
# -H:±GenerateBuildArtifactsFile \
# -J-Xmx4G \
# -J--add-modules -JALL-SYSTEM \
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles

# echo "Compressing executable ... "
# upx "${OUT_FILE}"
# popd >/dev/null
