# Mystical Agriculture cloning regression probe

`MysticalCloningProbe.java` is a separate NeoForge test mod, outside production source sets. Compile and package it as mod ID `mysticalcloningprobe`, then run in a disposable Minecraft 1.21.1 / NeoForge 21.1.226 server with AgriCraft and Resourceful Config.

Run once with the new option absent from `config/agricraft.jsonc` (expected default `false`), then with `compat.mysticalagriculture_cloning=true` and JVM property `-Dcloning.expected=true`. The probe checks configuration loading and exercises the production parent selector and breeding engine with synthetic crop objects and a registered wheat genome. Mystical Agriculture itself is not required for this engine-level check; namespace routing is tested explicitly.

Checks cover one-parent cloning disabled/enabled, two-parent breeding with the default, datapack-enabled cloning, maturity, Fertility selection, spread chance, unrelated namespaces including similar prefixes, the compatibility activation switch and disabling the option again. The server writes `mystical-cloning-results.txt` and exits. This is not a native MA growth or client-interaction test.
