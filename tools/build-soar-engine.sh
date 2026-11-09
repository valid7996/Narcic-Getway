#!/usr/bin/env bash
# Builds the Soar client (Go, from tools/soar-engine) as an Android executable for every ABI the app ships.
# Needs Go 1.25+ (not 1.26.0-1.26.2), the Android NDK and python3.
#   NDK_ROOT=/path/to/ndk bash tools/build-soar-engine.sh            (output: app/src/main/jniLibs/<abi>/libsoar_client.so)
#   OUT=/some/dir bash tools/build-soar-engine.sh                      (output: $OUT/<abi>/libsoar_client.so)
set -Eeuo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
go_command=${GO:-go}
if [[ -z ${NDK_ROOT:-} ]]; then
    : "${ANDROID_HOME:?Set ANDROID_HOME or NDK_ROOT}"
    NDK_ROOT="$ANDROID_HOME/ndk/$(ls -1 "$ANDROID_HOME/ndk" | sort -V | tail -1)"
fi
host=${NDK_HOST:-linux-x86_64}
api=${ANDROID_API:-24}
out=${OUT:-$root/app/src/main/jniLibs}
toolchain="$NDK_ROOT/toolchains/llvm/prebuilt/$host/bin"

cd "$root/tools/soar-engine"
python3 "$root/tools/android_go_toolchain.py" "$("$go_command" env GOVERSION)"
"$go_command" mod download
"$go_command" mod verify

for spec in 'arm64-v8a arm64 aarch64-linux-android' 'armeabi-v7a arm armv7a-linux-androideabi' 'x86_64 amd64 x86_64-linux-android'; do
    read -r abi arch target <<< "$spec"
    cc="$toolchain/$target$api-clang"
    [[ -x $cc ]] || { printf 'Missing Android compiler: %s\n' "$cc" >&2; exit 1; }
    mkdir -p "$out/$abi"
    printf 'Building Soar for %s\n' "$abi"
    CGO_ENABLED=1 GOOS=android GOARCH="$arch" GOARM=7 CC="$cc" \
        "$go_command" build -mod=readonly -trimpath -buildvcs=false -buildmode=pie \
        -ldflags '-s -w -linkmode external -extldflags "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"' \
        -o "$out/$abi/libsoar_client.so" .
    chmod +x "$out/$abi/libsoar_client.so"
done
