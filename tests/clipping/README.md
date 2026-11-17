# Clipping guard regression probe

Compile `ClipperProbe.java` against the NeoForge development classpath and the AgriCraft production jar under test, then package it as a separate mod with ID `clippingprobe`. Run in a disposable Minecraft 1.21.1 / NeoForge 21.1.226 dedicated server with Resourceful Config, Mystical Agriculture 8.0.27 and Cucumber 8.0.16.

The probe uses actual registered plant definitions and crop block entities with a survival fake player through `ServerPlayerGameMode.useItemOn`. It verifies that wheat, Ferranium, Inferium, Iron and Diamond reject clipping, retain their growth and genome and produce the correct mature/immature message. It also checks that immature Allium cannot be clipped and that mature Allium still resets growth on both successful and unsuccessful drop rolls. A configured chance roll yielding nothing is intentionally different from a plant with no clipping products.

The server writes `clipping-results.txt` and exits. The probe does not exercise client hand routing or fix the separately observed offhand behavior. Test classes must remain outside production source sets.

## Gain-scaled clipping probe (issue #8)

`ClipperGainProbe.java` (mod ID `clipgainprobe`) needs only AgriCraft and Resourceful Config and can run in an integrated (client) server. On server start it clips a mature poppy 400 times at Gain 1, 4, 7 and 10 with `core.clipping_uses_gain` enabled, and at Gain 10 with it disabled, then writes `clip-gain-results.txt` and halts the server. Expected yield rates: ~25%, ~50%, ~75%, 100% and ~25%; every clip must succeed and reset the crop to stage 0.
