#!/usr/bin/env bash

# ./native-image.sh -Ob (For Quick Build)
# ./native-image.sh --verbose --enable-https

# Make sure the latest GraalVM is installed
# ./graalvm-ce-dev.sh

set -euo pipefail
IFS=$'\n\t'

echo "Building the application jar..."
./gradlew clean build

# pattern="native-image-playground-*-all.jar"
# files=($(for f in $(find . -name "$pattern" -type f); do echo "$f"; done | sort -k 1 -r))
echo "Generating Graalvm config files..."
BUILD_DIR=$(pwd)/build
# shellcheck disable=SC2012
APP_JAR=$(ls -t "${BUILD_DIR}"/libs/native-image-playground-*-all.jar | head -1)
CONFIG_DIR="$(PWD)/src/main/resources/META-INF/native-image/playground"
OUT_FILE="${BUILD_DIR}/native-image-playground"

# Run the app in background (&) by ignoring SIGHUP signal (nohup).
# Enable preview and incubating features (by adding all system modules).
nohup java \
  --show-version \
  --enable-preview \
  --add-modules=ALL-SYSTEM \
  -agentlib:native-image-agent=config-merge-dir="${CONFIG_DIR}" \
  -jar "${APP_JAR}" &>"${BUILD_DIR}/nohup.out" &
# Wait for the server to startup
sleep 1
curl -fsSL http://localhost:9080/test
curl -fsSL http://localhost:9080/rsocket
curl -fsSL -o /dev/null http://localhost:9080/shutdown || echo "Native Image build config generation completed!"
# Wait for agent to write the config
sleep 1

echo "Creating native image..."
rm -f "${OUT_FILE}"

# Pass all env variables to native-image builder
# env | sed -e 's/=.*//g' | sed -e 's/^/-E/g' > env-vars.txt

args=("-jar" "${APP_JAR}"
  "-J--add-modules=ALL-SYSTEM"
  # "-march=native"
  # "@env-vars.txt"
  # "-H:+TraceSecurityServices"
  # "-H:+PrintAnalysisCallTree"
  # "-H:+DashboardAll"
  # "-H:DashboardDump=reports/dump"
  # "-H:+DashboardPretty"
  # "-H:+DashboardJson"
  # "-H:ReportAnalysisForbiddenType=java.awt.Toolkit:InHeap,Allocated"
  # "--debug-attach"
  "-o" "$OUT_FILE")

case "$OSTYPE" in
linux*)
  args+=("--static")
  args+=("-H:+StripDebugInfo")
  # args+=("-H:+StaticExecutableWithDynamicLibC")
  # args+=("--libc=musl"
  #        "-H:CCompilerOption=-Wl,-z,stack-size=2097152")
  ;;
esac

# Disable Java Module system when building native image
# export USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM=false

native-image "$@" "${args[@]}"

# echo "Compressing executable ... "
# upx "${OUT_FILE}"
# popd >/dev/null

# https://www.graalvm.org/reference-manual/native-image/overview/BuildOptions/
# --dry-run \
# --static --libc=<glib | musl | bionic>
# --target=darwin-aarch64
# --gc=epsilon \
# // Enable quick build
# -Ob \
# // generate debug info and no optimizations
# -g -O0 \
# --enable-https \
# --enable-url-protocols=http,https,file,jar,unix \
# --report-unsupported-elements-at-runtime \
# --initialize-at-build-time=kotlinx,kotlin,org.slf4j \
# --initialize-at-run-time=... \
# --trace-class-initialization=... \
# --trace-object-instantiation= \
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
# -H:±GenerateBuildArtifactsFile \
# -J-Xmx4G \
# -J--add-modules -JALL-SYSTEM \
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles
