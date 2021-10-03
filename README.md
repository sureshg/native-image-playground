# GraalVM Native Image Playground

[![GitHub Workflow Status][gha_badge]][gha_url]
[![GraalVM CE][graalvm_img]][graalvm_url]
[![Kotlin release][kt_img]][kt_url]
[![Style guide][ktlint_img]][ktlint_url]

 [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/) of a kotlin/java app and publish the platform binaries using Github action.

#### Install GraalVM

```bash
$ curl -s "https://get.sdkman.io" | bash
$ sdk i java 21.2.0.r16-grl
```

#### Build

```bash
$ ./gradlew clean build
```

#### Resources

 * [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/)
 * [Native Image Build Tools](https://graalvm.github.io/native-build-tools/)
 * [Native Image Docs Repo](https://github.com/oracle/graal/tree/master/docs/reference-manual/native-image)

<hr>

 * [Graalvm CE Builds](https://github.com/graalvm/graalvm-ce-builds/releases/)
 * [Graalvm CE Dev Builds](https://github.com/graalvm/graalvm-ce-dev-builds/releases/)
 * [Graalvm CE Docker Image](https://github.com/graalvm/container/pkgs/container/graalvm-ce)


[graalvm_url]: https://github.com/graalvm/graalvm-ce-builds/releases
[graalvm_img]: https://img.shields.io/github/v/release/graalvm/graalvm-ce-builds?color=125b6b&label=graalvm-16&logo=java&logoColor=d3eff5&style=for-the-badge

[kt_url]: https://github.com/JetBrains/kotlin/releases/latest
[kt_img]: https://img.shields.io/github/v/release/Jetbrains/kotlin?include_prereleases&color=7f53ff&label=Kotlin&logo=kotlin&logoColor=7f53ff&style=for-the-badge

[gha_url]: https://github.com/sureshg/openjdk-playground/actions/workflows/build.yml
[gha_img]: https://github.com/sureshg/openjdk-playground/actions/workflows/build.yml/badge.svg
[gha_badge]: https://img.shields.io/github/workflow/status/sureshg/openjdk-playground/Build?color=green&label=Build&logo=Github-Actions&logoColor=green&style=for-the-badge

[sty_url]: https://kotlinlang.org/docs/coding-conventions.html
[sty_img]: https://img.shields.io/badge/style-Kotlin--Official-40c4ff.svg?style=for-the-badge&logo=kotlin&logoColor=40c4ff

[ktlint_url]: https://ktlint.github.io/
[ktlint_img]: https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg?logo=kotlin&style=for-the-badge&logoColor=FF4081
