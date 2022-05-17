#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

# sdk i java 22.0.0.2.r17-grl
# gu install native-image

pushd ~/code/compose-mpp-playground >/dev/null
./gradlew packageUberJarForCurrentOS

echo "Generating Graalvm config files..."
java -agentlib:native-image-agent=config-output-dir=config -jar desktop/build/compose/jars/jvm-macos-*.jar

echo "Creating native image..."
native-image \
      --verbose \
      --no-fallback \
      --allow-incomplete-classpath \
      -H:ConfigurationFileDirectories=config \
      -H:+ReportExceptionStackTraces \
      -H:DashboardDump=dashboard \
      -H:+DashboardHeap \
      -H:+DashboardCode \
      -Djava.awt.headless=false \
      -J-Xmx7G \
      -jar desktop/build/compose/jars/jvm-macos-*.jar \
      compose-app

# https://www.graalvm.org/reference-manual/native-image/Options
# --dry-run \
# --native-image-info \
# --static \
# --libc=musl  \
# --libc=glibc \
# --gc=epsilon \
# // Enable quick build
# -Ob \
# // generate debug info and no optimizations
# -g -O0 \
# --install-exit-handlers \
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
# -H:+DashboardAll \
# -H:+DashboardPointsTo
# -H:CompilerBackend=llvm \
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles

echo "Compressing executable ... "
upx compose-app
popd >/dev/null
