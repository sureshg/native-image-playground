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
      -Djava.awt.headless=false \
      -J-Xmx7G \
      -jar desktop/build/compose/jars/jvm-macos-*.jar \
      compose-app

# --static \
# --gc=epsilon \
# // generate debug info and no optimizations
# -g -O0 \
# --libc musl \
# --install-exit-handlers \
# --enable-https \
# --enable-all-security-services \
# --enable-url-protocols=http,https,file,jar \
# --report-unsupported-elements-at-runtime \
# --initialize-at-build-time=kotlinx,kotlin,org.slf4j \
# --initialize-at-run-time=... \
# --trace-class-initialization=... \
# -Dauthor="$USER" \
# -H:DefaultLocale="en-US" \
# -H:+AddAllCharsets \
# -H:+IncludeAllLocales  \
# -H:+IncludeAllTimeZones \
# -H:IncludeResources=<string>* \
# -H:IncludeResourceBundles=<string>* \
# -H:+AddAllFileSystemProviders \
# -H:+ReportUnsupportedElementsAtRuntime \
# -H:CLibraryPath=".../lib"
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles

echo "Compressing executable ... "
upx compose-app
popd >/dev/null
