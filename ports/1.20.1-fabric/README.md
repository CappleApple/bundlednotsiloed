# Bundled Not Siloed for Minecraft 1.20.1 (Fabric)

This standalone port keeps the capacity inventory, categories, search, hotbar bindings, and inventory transfer behavior of the original mod.

Install the `bundlednotsiloed-1.20.1-fabric` and matching `stacksnotslots-1.20.1-fabric` JARs on both the client and server. Requires Java 17, Fabric Loader 0.16.14 or newer, and Fabric API for Minecraft 1.20.1. Builds are tested with Loader 0.19.5 and Fabric API 0.92.12+1.20.1.

Use Java 21 to run Gradle; the game and compiler use a Java 17 toolchain. Build from the repository root with `.\ports\build-port.ps1 1.20.1-fabric` on Windows or `./ports/build-port.sh 1.20.1-fabric` on Linux/macOS. Keep the matching `stacks-not-slots` repository beside this repository; the helper builds its matching source port first. Output JARs are written to `ports/1.20.1-fabric/build/libs/`.

The original Minecraft 1.21.1 NeoForge project remains at the repository root. See its [README](../../README.md) for inventory controls. This port uses Minecraft 1.20.1 item NBT and Fabric Transfer API transactions. Configuration remains in `config/bundlednotsiloed-common.toml` and `config/bundlednotsiloed-client.toml`.
