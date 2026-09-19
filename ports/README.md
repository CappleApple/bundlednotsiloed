# Bundled Not Siloed: 26.3-neoforge

Branch: `cappleapple/26.3-neoforge`. This branch contains only the `26.3-neoforge` port. The original NeoForge 1.21.1 implementation remains at the repository root.

- Minecraft 26.3; NeoForge 26.3.0.1-beta.
- Java 25 for the game; Java 25 to run Gradle.
- Bundled Not Siloed 1.5.
- Requires Stacks Not Slots 1.1 from [the matching branch](https://github.com/CappleApple/stacksnotslots/tree/cappleapple/26.3-neoforge).

## Install

Install `bundlednotsiloed-26.3-neoforge-1.5.jar` and `stacksnotslots-26.3-neoforge-1.1.jar` on the client and server. Use only the pair for this Minecraft version and loader. Fabric additionally requires Fabric API.

## Build

Keep `bundled-not-siloed` and `stacks-not-slots` as sibling directories, both checked out on `cappleapple/26.3-neoforge`. The dependency's default branch contains the original library, not this target.

From the Bundled Not Siloed repository root:

```powershell
.\ports\build-port.ps1 26.3-neoforge
```

```bash
bash ports/build-port.sh 26.3-neoforge
```

The helper builds and tests the matching dependency first. Production JARs are written to `ports/26.3-neoforge/build/libs/` in each repository. Do not install `-sources.jar` or Forge `-dev.jar` artifacts.

```powershell
.\ports\26.3-neoforge\gradlew.bat -p ports/26.3-neoforge runGameTestServer
```

The target also provides `runClient`, `runServer`, and an opt-in hidden, muted `runClientSmoke` startup gate. CI builds this target, runs unit tests and server GameTests, and uploads the production JAR pair.

See [validation results](VALIDATION.md) and [other target branches](BRANCHES.md). Inventory controls and shared configuration behavior are described in the [root README](../README.md); its NeoForge-specific API details describe the original project.

The 26.x port retains JEI integration and omits the optional EMI adapter because no matching upstream EMI release was available. Optional integrations were not exhaustively tested.

## License

[CC BY-NC-SA 4.0 with additional permission for Minecraft modpacks and servers](../LICENSE). The full project notice is included in the production JAR.
