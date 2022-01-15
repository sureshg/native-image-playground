#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

# sdk i java 21.3.0.r17-grl
# gu install native-image
pushd ~/code/compose-mpp-playground >/dev/null
./gradlew packageUberJarForCurrentOS

echo "Generating Graalvm config files..."
java -agentlib:native-image-agent=config-output-dir=config -jar desktop/build/compose/jars/jvm-macos-*.jar

echo "Creating native image ... "
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
# --install-exit-handlers \
# --enable-all-security-services \
# --report-unsupported-elements-at-runtime \
# --initialize-at-build-time=kotlinx,kotlin,org.slf4j \
# -H:+ReportUnsupportedElementsAtRuntime \
# -H:CLibraryPath=".../lib"
# Resource config options: https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#:~:text=H%3AResourceConfigurationFiles

echo "Compressing executable ... "
upx compose-app
popd >/dev/null
