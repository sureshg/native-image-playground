#!/usr/bin/env bash

# set -u won't work for sdkman
set -e

jdk_version=${1:-24}
extn="tar.gz"

# Find OS type
case "$OSTYPE" in
darwin*)
  os=darwin
  ;;
msys*)
  os=windows
  extn="zip"
  ;;
linux*)
  os=linux
  ;;
*)
  echo "Unsupported OS: $OSTYPE"
  exit 1
  ;;
esac

# Find CPU architecture
case "$(uname -m)" in
amd64 | x86_64)
  arch=amd64
  ;;
aarch64 | arm64)
  arch=aarch64
  ;;
*)
  echo "Unsupported arch: $(uname -m)"
  exit 1
  ;;
esac

echo "Using OS: $os-$arch"

# download_path=$(curl -sSL --no-buffer "https://github.com/graalvm/graalvm-ce-dev-builds/releases/latest" | grep -m1 -Eioh "/graalvm/graalvm-ce-dev-builds/releases/download/.*/graalvm-community-java${jdk_version}-($os-$arch)-dev.(tar.gz|zip)")
graalvm_base_url="https://github.com/graalvm/graalvm-ce-dev-builds/releases"
graalvm_release=$(curl -Ls -o /dev/null -w %{url_effective} "${graalvm_base_url}/latest")
graalvm_tag="${graalvm_release##*/}"

openjdk_file="graalvm-community-java${jdk_version}-$os-$arch-dev.${extn}"
download_url="${graalvm_base_url}/download/${graalvm_tag}/${openjdk_file}"

# Download the GraalVM
pushd "$HOME/install/graalvm" >/dev/null
echo "Openjdk-$jdk_version file: $openjdk_file"
echo "Downloading $download_url ..."
curl --progress-bar --request GET -L --url "$download_url" --output "$openjdk_file"

# Extract the GraalVM and cleanup old/downloaded files
jdk_dir=$(tar -tzf "$openjdk_file" | head -1 | cut -f1 -d"/")
rm -rf "$jdk_dir" && tar -xvzf "$openjdk_file" && rm -f "$openjdk_file"
if [ "$os" == "darwin" ]; then
  echo "Removing the quarantine attribute..."
  sudo xattr -r -d com.apple.quarantine "$jdk_dir"
fi

# Install GraalVM using sdkman
sdkman_id="graalvm-ce-dev"
echo "Installing $jdk_dir ..."
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk rm java "$sdkman_id" || true
sdk i java "$sdkman_id" "$jdk_dir/Contents/Home"
popd >/dev/null

# Set GraalVM as default JDK in the current shell
sdk u java "$sdkman_id"
native-image --version
