# Validation: 26.2-fabric

Validated locally on 2026-09-19:

- 110 Bundled Not Siloed unit tests passed.
- 35 Stacks Not Slots unit tests passed.
- 13 required server GameTests passed, including the vanilla baseline where registered by the loader.
- Hidden, muted client startup and client mixin loading passed with clean exit.
- Dedicated-server fresh start and saved-world restart reached `Done` and stopped normally.

The tests cover capacity accounting, persistence, inventory projection, payload handling, and native storage transfers. Fabric and NeoForge 26.x include rollback/reconciliation regression coverage. Optional Sophisticated Backpacks assertions skip when the mod is absent. Gameplay interaction and visual layout were not manually observed; optional integration combinations were not exhaustively tested.

Publication incorporates upstream license/documentation updates. Production JARs were rebuilt and inspected for correct target metadata, Java bytecode, dependency declarations, and the included license notice. These publication edits do not change gameplay code.
