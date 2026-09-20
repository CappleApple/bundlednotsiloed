# Bundled Not Siloed: 1.20.1-forge

Branch: `cappleapple/1.20.1-forge`. This branch contains only the `1.20.1-forge` port. The original NeoForge 1.21.1 implementation remains at the repository root.

- Minecraft 1.20.1; Forge 47.4.10.
- Java 17 for the game; Java 17 to run Gradle.
- Bundled Not Siloed 1.5.1.
- Requires Stacks Not Slots 1.1.1 from [the matching branch](https://github.com/CappleApple/stacksnotslots/tree/cappleapple/1.20.1-forge).

Forge 47.4.10 is the [recommended build for Minecraft 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html).

## Install

Install `bundlednotsiloed-1.20.1-forge-1.5.1.jar` and `stacksnotslots-1.20.1-forge-1.1.1.jar` on the client and server. Use only the pair for this Minecraft version and loader. Fabric additionally requires Fabric API.

## Build

Keep `bundled-not-siloed` and `stacks-not-slots` as sibling directories, both checked out on `cappleapple/1.20.1-forge`. The dependency's default branch contains the original library, not this target.

From the Bundled Not Siloed repository root:

```powershell
.\ports\build-port.ps1 1.20.1-forge
```

```bash
bash ports/build-port.sh 1.20.1-forge
```

The helper builds and tests the matching dependency first. Production JARs are written to `ports/1.20.1-forge/build/libs/` in each repository. Do not install `-sources.jar` or Forge `-dev.jar` artifacts.

```powershell
.\ports\1.20.1-forge\gradlew.bat -p ports/1.20.1-forge runGameTestServer
```

The target also provides `runClient`, `runServer`, and an opt-in hidden, muted `runClientSmoke` startup gate. Use `runClientSmoke -PclientGameplay` for the in-world checks and add `-PclientGameplayResume` for a saved-world rejoin; see [the test-world setup](VALIDATION.md#reproducing-the-client-checks). CI builds this target, runs unit tests and server GameTests, and uploads the production JAR pair.

See [validation results](VALIDATION.md) and [other target branches](BRANCHES.md). Inventory controls and shared configuration behavior are described in the [root README](../README.md); its NeoForge-specific API details describe the original project.

## License

[CC BY-NC-SA 4.0 with additional permission for Minecraft modpacks and servers](../LICENSE). The full project notice is included in the production JAR.
