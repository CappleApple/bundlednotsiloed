# Inventory return regression — 1.4.5

Version 1.4.5 fixed a browser-session bug that could leave the inventory input gate locked after returning from another screen such as EMI, JEI, BNS settings, or another secondary GUI.

## What went wrong

`BundledInventoryScreen` could begin a new browser session while still carrying the previous session's applied-page cache. On a same-screen-object return, the client could decide that the first page was already current and skip the request that normally starts the request/acknowledgement cycle.

The result was a newly opened session that never received its expected first acknowledgement, leaving normal inventory input blocked.

## Fix

Opening a new browser session now invalidates `windowDirty` and `appliedWindowRevision` so the first page is requested and acknowledged normally.

The change is local to the browser-session lifecycle. It does not change the network protocol, item movement rules, server inventory handler, or the safety checks around pending input.

## Regression checks

The normal project tests should be run with:

```powershell
.\gradlew.bat test build runGameTestServer -PskipOptionalCompatRuntime
```

The regression was also reproduced and checked through the sibling Inventory Re-Init test profile with repeated returns from EMI, JEI, BNS settings, and a generic secondary screen.

A small retained report from the standalone BNS sequence is available at:

```text
docs/validation/inventory-return-1.4.5.txt
```

The important assertions are:

- a returning screen starts a fresh browser session;
- the first page request is actually sent;
- its acknowledgement releases the normal input gate;
- repeated returns do not grow the screen's child/widget list; and
- pressing `E` can close the inventory again after the return.

This fixture focuses on the session/input regression. Populated crafting/cursor interaction and high-latency multiplayer remain separate gameplay checks.
