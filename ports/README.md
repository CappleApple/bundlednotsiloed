# Bundled Not Siloed: 26.2-fabric

Branch: `cappleapple/26.2-fabric`. This branch contains only the `26.2-fabric` port. The original NeoForge 1.21.1 implementation remains at the repository root.

- Minecraft 26.2; Fabric Loader 0.19.5 / Fabric API 0.161.0+26.2.
- Java 25 for the game; Java 25 to run Gradle.
- Bundled Not Siloed 1.5.1.
- Requires Stacks Not Slots 1.1 from [the matching branch](https://github.com/CappleApple/stacksnotslots/tree/cappleapple/26.2-fabric).

## Install

Install `bundlednotsiloed-26.2-fabric-1.5.1.jar` and `stacksnotslots-26.2-fabric-1.1.jar` on the client and server. Use only the pair for this Minecraft version and loader. Fabric additionally requires Fabric API.

## Build

Keep `bundled-not-siloed` and `stacks-not-slots` as sibling directories, both checked out on `cappleapple/26.2-fabric`. The dependency's default branch contains the original library, not this target.

From the Bundled Not Siloed repository root:

```powershell
.\ports\build-port.ps1 26.2-fabric
```

```bash
bash ports/build-port.sh 26.2-fabric
```

The helper builds and tests the matching dependency first. Production JARs are written to `ports/26.2-fabric/build/libs/` in each repository. Do not install `-sources.jar` or Forge `-dev.jar` artifacts.

```powershell
.\ports\26.2-fabric\gradlew.bat -p ports/26.2-fabric runGameTestServer
```

The target also provides `runClient`, `runServer`, and an opt-in hidden, muted `runClientSmoke` startup gate. Use `runClientSmoke -PclientGameplay` for the in-world checks and add `-PclientGameplayResume` for a saved-world rejoin; see [the test-world setup](VALIDATION.md#reproducing-the-client-checks). CI builds this target, runs unit tests and server GameTests, and uploads the production JAR pair.

See [validation results](VALIDATION.md) and [other target branches](BRANCHES.md). Inventory controls and shared configuration behavior are described in the [root README](../README.md); its NeoForge-specific API details describe the original project.

Fabric uses native Transfer API storage. Transactions keep the logical inventory and vanilla projection consistent during tentative changes, commits, and rollback. TOML configuration loads at startup, and client setting changes save immediately.

The 26.x port retains JEI integration and omits the optional EMI adapter because no matching upstream EMI release was available. Optional integrations were not exhaustively tested.

## License

[CC BY-NC-SA 4.0 with additional permission for Minecraft modpacks and servers](../LICENSE). The full project notice is included in the production JAR.
