# Inventory return validation — 1.4.5 — 2026-09-09

`BundledInventoryScreen` could open a new browser session while retaining its previous applied-page cache. A same-object return after EMI, JEI or settings therefore skipped the first page request and kept the pending-session input gate locked. Invalidate `windowDirty` and `appliedWindowRevision` when opening the new session so the existing page request/acknowledgement lifecycle runs normally.

No protocol, item movement, server handler or pending-input safety check changed. The source fix is independent of Inventory Re-Init.

Validation:

- `gradlew test build runGameTestServer -PskipOptionalCompatRuntime`: 110 unit tests and 8 required GameTests passed.
- Sibling Inventory Re-Init's settled graphical fixture: repeated EMI and JEI returns, BNS settings, and a generic secondary screen preserve the same screen/menu, retain a stable child count, release the normal input gate and allow E to close.
- With Inventory Re-Init disabled, the EMI/settings/generic sequence independently passed 11 grouped assertions ([recorded report](validation/inventory-return-1.4.5.txt)). The original 1.4.4 session had sequence zero and no acknowledgement after return; 1.4.5 submits and acknowledges its first page.
- Inventory Re-Init's deferred GUI repair emitted zero packets in the corrected compatibility fixtures.

Reproduce in the sibling `Inventory Re-Init` project with BNS 1.4.5 and the optional dependencies in its isolated profile:

```powershell
.\gradlew.bat runClientTest -PcompatProfile=emi -PsettledReturnRegression=true -PsettledReturnCheck=input
```

Detailed reports and state evidence are in that project's `docs/VALIDATION.md`, `docs/RETURN_FAILURES.md`, and `docs/validation/bns-independent-1.4.5*.txt`. The graphics fixture reads the actual key interceptor but uses empty slots; populated crafting/cursor interactions, physical mouse input and multiplayer desync checks remain manual.

Artifact: `build/libs/bundlednotsiloed-1.4.5.jar`

SHA-256: `FFEAD6015483E89EE597629C9F13FDA0951ADC0D9EFE57E76165E735E6A43C68`

The isolated validation runs did not modify the live modpack.
