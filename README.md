# GraalVM Native Image Playground

[![GitHub Workflow Status][gha_badge]][gha_url]
[![GraalVM CE][graalvm_img]][graalvm_url]
[![Kotlin release][kt_img]][kt_url]
[![GraalVM Dashboard][gl_dashboard_img]][gl_dashboard_url]
[![Style guide][ktlint_img]][ktlint_url]

 [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/) of a kotlin/java app and publish the platform binaries using Github action.

#### Install GraalVM

```bash
$ curl -s "https://get.sdkman.io" | bash
$ sdk i java 22.1.0.r17-grl
```

#### Build

```bash
$ ./gradlew build
```

#### Resources

 * [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/)
 * [GitHub Action for GraalVM](https://github.com/marketplace/actions/github-action-for-graalvm)
 * [Native Image Build Tools](https://graalvm.github.io/native-build-tools/)
 * [Native Image Docs Repo](https://github.com/oracle/graal/tree/master/docs/reference-manual/native-image)

<hr>

 * [Graalvm CE Builds](https://github.com/graalvm/graalvm-ce-builds/releases/)
 * [Graalvm CE Dev Builds](https://github.com/graalvm/graalvm-ce-dev-builds/releases/)
 * [Graalvm CE Docker Image](https://github.com/graalvm/container/pkgs/container/graalvm-ce)

<hr>

 * [Graalvm Setup](https://graalvm.github.io/native-build-tools/latest/graalvm-setup.html)
 * [Setup Microsoft Visual C++](https://github.com/marketplace/actions/enable-developer-command-prompt)


[graalvm_url]: https://github.com/graalvm/graalvm-ce-builds/releases
[graalvm_img]: https://img.shields.io/github/v/release/graalvm/graalvm-ce-builds?color=125b6b&label=graalvm-17&logo=java&logoColor=d3eff5&style=for-the-badge

[gl_dashboard_url]: https://www.graalvm.org/dashboard/
[gl_dashboard_img]: https://img.shields.io/badge/GraalVM-Dashboard-f39727.svg?style=for-the-badge&logo=clyp&logoColor=40c4ff

[kt_url]: https://github.com/JetBrains/kotlin/releases/latest
[kt_img]: https://img.shields.io/github/v/release/Jetbrains/kotlin?include_prereleases&color=7f53ff&label=Kotlin&logo=kotlin&logoColor=7f53ff&style=for-the-badge

[gha_url]: https://github.com/sureshg/native-image-playground/actions/workflows/graalvm.yml
[gha_badge]: https://img.shields.io/github/workflow/status/sureshg/native-image-playground/Build?color=green&label=Build&logo=Github-Actions&logoColor=green&style=for-the-badge

[sty_url]: https://kotlinlang.org/docs/coding-conventions.html
[sty_img]: https://img.shields.io/badge/style-Kotlin--Official-40c4ff.svg?style=for-the-badge&logo=kotlin&logoColor=40c4ff

[ktlint_url]: https://ktlint.github.io/
[ktlint_img]: https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg?logo=kotlin&style=for-the-badge&logoColor=FF4081

[//]: # (⬇️  🖌️  🧭🎨️ 🧭✨ 🌊 ⏳ 📫 📖 🎨 🍫 📐)
