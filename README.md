# Aether (Fabric 1.21.11 client)

Push this repo to GitHub. The workflow in `.github/workflows/build.yml` builds on every push;
download the jar from the run's **Artifacts** section (use the one WITHOUT `-sources`).

Right Shift opens the ClickGUI. Right-click a module to expand its settings.
Config saves to `config/aether.json`.

If the build fails on dependency versions, update `gradle.properties` from https://fabricmc.net/develop.
Files marked `VERSION-SENSITIVE` (EspRenderer, AetherClient, ClickGuiScreen input) may need small mapping tweaks.

## Modules
Combat: Aimbot, CrystalMacro, AnchorMacro, StunSlam. Movement: Freecam.
Render: StorageFinder, SpawnerFinder, Visuals (fullbright / FOV / entity ESP). World: TuffChunkFinder, BaseFinder.

**Keybinds:** middle-click a module in the GUI, press a key (Backspace clears, Esc cancels).
