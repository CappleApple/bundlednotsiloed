# Validation: 26.2-neoforge - 2026-09-20

This target passed scripted gameplay in real Minecraft clients, followed by a complete client restart and rejoin. Screenshots were visually inspected. Clients ran in hidden, muted windows with the mouse released, using disposable local worlds.

| Target | BNS unit tests | Stacks unit tests | Required GameTests | In-world gameplay | Restart / localization |
| --- | ---: | ---: | ---: | --- | --- |
| 26.2-neoforge | 112 | 35 | 10 | Passed | Passed |

This target passed 147 unit tests and 10 required GameTests. GameTest counts include the vanilla baseline where registered. The original NeoForge 1.21.1 project was also rebuilt: 111 unit tests and 8 required GameTests passed.

## Gameplay coverage

The client sequence opens the replacement inventory, searches for an item in stowed storage, moves it through native inventory packets, scrolls the inventory, opens category and settings controls, changes and restores a setting, creates a custom tab, crafts planks, transfers items through the offhand, and shift-clicks a chest stack into the inventory. Assertions check server/client synchronization, cursor contents, expected quantities, and conservation of items. Restart checks confirm the saved hotbar, total inventory, and custom tab survive rejoining.

The server GameTests additionally exercise hidden storage, creative inventory operations, native quick-move behavior, recipe ingredient access, and transactional rollback. NeoForge 26.3 includes bulk-transfer rollback and hotbar exclusion checks for its current resource-handler API.

## Localization and appearance

Every bundled English translation key resolves through the running client's resource manager: 102 keys on the older ports and 103 on 26.x. The restart runs select German and verify that Minecraft's translated controls and the mod's English fallback resolve without displaying raw translation keys. This is fallback validation; the mod does not supply a complete German translation.

Screenshot review covered the inventory, search, settings, tab manager/editor, and container screens across the targets. It found and led to fixes for clipped tab help and transparent custom text on 26.x. Rendering and scripted interaction were checked in real clients; this was not an exhaustive manual playthrough.

## Reproducing the client checks

Build the matching dependency first. Place a disposable world at `ports/26.2-neoforge/run-client-smoke/saves/BNS-Audit/`; the `level.dat` file must be directly inside that directory. The gameplay fixture replaces the test player's inventory, so use a copied test world. From the target directory, run:

```powershell
.\gradlew.bat runClientSmoke -PclientGameplay
.\gradlew.bat runClientSmoke -PclientGameplay -PclientGameplayResume
```

The first command runs the English gameplay sequence. The second reopens that save with German selected. Successful runs create `BNS_GAMEPLAY_PASSED.txt` and `BNS_GAMEPLAY_RESUME_PASSED.txt` in `run-client-smoke/`. A failed assertion or missing completion marker fails the Gradle task. Screenshots are saved under `run-client-smoke/screenshots/`. Without these properties, `runClientSmoke` only checks title-screen startup.

## Artifacts and limits

All 16 final production JARs passed archive, metadata, Java bytecode, dependency, and loader-separation checks. The original target remains available alongside the seven ports. See the [version matrix](README.md) for exact loader/API pins and the [branch list](BRANCHES.md) for source checkouts.

Optional Sophisticated Backpacks assertions skip when the mod is absent. JEI/EMI adapters compile where available, but this audit does not establish every optional mod combination. Cross-version world conversion and downgrading are not supported. Earlier dedicated-server fresh-start/restart checks are separate from this audit's current GameTest servers and integrated client worlds.
