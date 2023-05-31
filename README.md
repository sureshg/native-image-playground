# GraalVM Native Image Playground

[![GitHub Workflow Status][gha_badge]][gha_url]
[![GraalVM CE][graalvm_img]][graalvm_url]
[![Kotlin release][kt_img]][kt_url]
[![Style guide][ktlint_img]][ktlint_url]

[![GraalVM Reachability][graalvm_reachability_img]][graalvm_reachability_url]
[![Style guide][nativeimage_cs_img]][nativeimage_cs_url]
[![GraalVM Dashboard][gl_dashboard_img]][gl_dashboard_url]

[GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/) of a kotlin/java app
and publish the platform binaries using GitHub action.

### Install GraalVM CE

```bash
# Install GraalVM CE Dev
$ bash <(curl -sL https://get.graalvm.org/jdk) \
     --to "$HOME/install/graalvm" \
     -c visualvm graalvm-ce-java<xx>

# Remove the MacOS quarantine attribute
$ sudo xattr -r -d com.apple.quarantine "$HOME/install/graalvm/graalvm-ce-java<xx>/Contents/Home"

# Manage using SDKMAN!
$ curl -s "https://get.sdkman.io" | bash
$ sdk i java graalvm-ce-java<xx> "$HOME/install/graalvm/graalvm-ce-java<xx>/Contents/Home"
```

### Build

```bash

# Build the native image
$ ./gradlew nativeCompile

# Run native image tests
$ ./gradlew nativeTest

# Native Image Quick Build
$ ./gradlew nativeCompile -Pquick

# Using Native Image Bundles
$ ./gradlew nativeCompile -Pbundle
$ native-image --bundle-apply="build/native/nativeCompile/native-image-playground.nib"
$ build/native-image-playground.output/default/native-image-playground

# Build by disabling the build cache
$ ./gradlew clean nativeCompile --rerun-tasks --no-build-cache

# GraalVM JIT Mode
$ ./gradlew build
$ java --enable-preview \
       --add-modules=ALL-SYSTEM \
       -jar build/libs/native-image-playground-*-all.jar

# Find out the classes/jars using top modules mentioned in the native-image build output
$ ./gradlew build
$ jdeps -q \
        -R \
        --ignore-missing-deps \
        --multi-release=20 \
        build/libs/native-image-playground-*-all.jar

# Build native image from modular jars
$ native-image \
    -p base-module.jar:main-module.jar \
    -m dev.suresh.Main
```

### Run & Debugging

- Using Distroless

   ```bash
  # Download the native images and extract it
  # https://github.com/sureshg/native-image-playground/actions/workflows/graalvm.yml
  $ chmod +x native-image-playground

  # Image built on GithubAction (Linux X64) and running on MacOS ARM64
  $ docker run \
          -it \
          --rm \
          --platform=linux/amd64 \
          --pull always \
          --publish 9080:9080 \
          --name native-image-playground \
          --mount type=bind,source=$(pwd),destination=/app,readonly \
          --entrypoint=/app/native-image-playground \
          gcr.io/distroless/base

  # Running static image compiled using musl libc
  $ docker run \
          -it \
          --rm \
          --pull always \
          --publish 9080:9080 \
          --name native-image-playground \
          --mount type=bind,source=$(pwd),destination=/app,readonly \
          --entrypoint=/app/native-image-playground \
          gcr.io/distroless/static

  # To kill the container
  $ docker kill native-image-playground
  ```

- List all runtime options

  ```bash
  $ build/native/nativeCompile/native-image-playground -XX:PrintFlags= 2>&1

  # Eg: Set HeapDump path
  $ build/native/nativeCompile/native-image-playground -XX:HeapDumpPath=$HOME/heapdump.hprof
  ```

- Object/Shared Lib Details

   ```bash
   # Show shared libs
   $ otool -L build/native/nativeCompile/native-image-playground

   # SVM details
   $ strings -a build/native/nativeCompile/native-image-playground | grep -i com.oracle.svm.core.VM

   # Show all bundled CA Certs
   $ strings -a build/native/nativeCompile/native-image-playground | grep -i "cn="
   ```

- [Mach-O Format Viewer](https://github.com/horsicq/XMachOViewer)


- Misc Gradle Tasks

   ```bash
   # Detect unused and misused dependencies
   $ ./gradlew buildHealth
   $ cat build/reports/dependency-analysis/build-health-report.txt

   $ ./gradlew reason --id org.jetbrains.kotlin:kotlin-stdlib
   ```

### Resources

* [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/)
* [Libraries and Frameworks Tested with Native Image](https://www.graalvm.org/native-image/libraries-and-frameworks/#libraries-and-frameworks-tested-with-native-image)
* [GitHub Action for GraalVM](https://github.com/marketplace/actions/github-action-for-graalvm)
* [Native Image Build Tools](https://graalvm.github.io/native-build-tools/)
* [Native Image Docs Repo](https://github.com/oracle/graal/tree/master/docs/reference-manual/native-image)

<hr>

* [GraalVM CE Version Roadmap](https://www.graalvm.org/release-notes/version-roadmap/)
* [Graalvm CE Builds](https://github.com/graalvm/graalvm-ce-builds/releases/)
* [Graalvm CE Dev Builds](https://github.com/graalvm/graalvm-ce-dev-builds/releases/)
* [Graalvm CE Docker Image](https://github.com/graalvm/container/pkgs/container/graalvm-ce)

[graalvm_url]: https://github.com/graalvm/graalvm-ce-dev-builds/releases/

[graalvm_img]: https://img.shields.io/github/v/release/graalvm/graalvm-ce-builds?color=125b6b&label=graalvm-20&logo=oracle&logoColor=d3eff5&style=for-the-badge

[graalvm_reachability_url]: https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata

[graalvm_reachability_img]: https://img.shields.io/github/v/release/oracle/graalvm-reachability-metadata?color=125b6b&label=graalvm-reachability&logo=oracle&logoColor=d3eff5&style=for-the-badge

[gl_dashboard_url]: https://www.graalvm.org/dashboard/

[gl_dashboard_img]: https://img.shields.io/badge/GraalVM-Dashboard-125b6b.svg?style=for-the-badge&logo=clyp&logoColor=d3eff5

[nativeimage_cs_url]: https://www.graalvm.org/uploads/quick-references/Native-Image_v2/CheatSheet_Native_Image_v2_(EU_A4).pdf

[nativeimage_cs_img]: https://img.shields.io/badge/NativeImage-CheatSheet-125b6b.svg?style=for-the-badge&logo=oracle&logoColor=d3eff5

[kt_url]: https://github.com/JetBrains/kotlin/releases/latest

[kt_img]: https://img.shields.io/github/v/release/Jetbrains/kotlin?include_prereleases&color=7f53ff&label=Kotlin&logo=kotlin&logoColor=7f53ff&style=for-the-badge

[gha_url]: https://github.com/sureshg/native-image-playground/actions/workflows/graalvm.yml

[gha_badge]: https://img.shields.io/github/actions/workflow/status/sureshg/native-image-playground/graalvm.yml?branch=main&color=green&label=Build&logo=Github-Actions&logoColor=green&style=for-the-badge

[sty_url]: https://kotlinlang.org/docs/coding-conventions.html

[sty_img]: https://img.shields.io/badge/style-Kotlin--Official-40c4ff.svg?style=for-the-badge&logo=kotlin&logoColor=40c4ff

[ktlint_url]: https://ktlint.github.io/

[ktlint_img]: https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg?logo=kotlin&style=for-the-badge&logoColor=FF4081

[//]: # (⬇️  🖌️  🧭🎨️ 🧭✨ 🌊 ⏳ 📫 📖 🎨 🍫 📐)
