# Clipping guard regression probe

Compile `ClipperProbe.java` against the NeoForge development classpath and the AgriCraft production jar under test, then package it as a separate mod with ID `clippingprobe`. Run in a disposable Minecraft 1.21.1 / NeoForge 21.1.226 dedicated server with Resourceful Config, Mystical Agriculture 8.0.27 and Cucumber 8.0.16.

The probe uses actual registered plant definitions and crop block entities with a survival fake player through `ServerPlayerGameMode.useItemOn`. It verifies that wheat, Ferranium, Inferium, Iron and Diamond reject clipping, retain their growth and genome and produce the correct mature/immature message. It also checks that immature Allium cannot be clipped and that mature Allium still resets growth on both successful and unsuccessful drop rolls. A configured chance roll yielding nothing is intentionally different from a plant with no clipping products.

The server writes `clipping-results.txt` and exits. The probe does not exercise client hand routing or fix the separately observed offhand behavior. Test classes must remain outside production source sets.
