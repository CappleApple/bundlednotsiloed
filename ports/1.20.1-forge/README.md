# Bundled Not Siloed for Minecraft 1.20.1 (Forge)

This standalone port keeps the capacity inventory, categories, search, hotbar bindings, and inventory transfer behavior of the original mod.

Install the `bundlednotsiloed-1.20.1-forge` and matching `stacksnotslots-1.20.1-forge` JARs on both the client and server. Requires Java 17 and Forge 47.4.23 or newer for Minecraft 1.20.1.

Build from the repository root with `.\ports\build-port.ps1 1.20.1-forge` on Windows or `./ports/build-port.sh 1.20.1-forge` on Linux/macOS. Keep the matching `stacks-not-slots` repository beside this repository; the helper builds its matching source port first. Output JARs are written to `ports/1.20.1-forge/build/libs/`.

The original Minecraft 1.21.1 NeoForge project remains at the repository root. See its [README](../../README.md) for inventory controls and configuration. This port stores item identity and metadata using Minecraft 1.20.1 item NBT.
