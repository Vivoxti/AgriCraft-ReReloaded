# NeoForge seed composting regression probe

`SeedCompostProbe.java` is a separate test mod for a disposable Minecraft 1.21.1 / NeoForge 21.1.226 dedicated server. It is outside all production source sets and must not be bundled with AgriCraft.

Compile against the NeoForge development compile classpath and the AgriCraft jar under test. Package the resulting classes with `META-INF/neoforge.mods.toml` declaring JavaFML loader `[4,)`, mod ID `seedcompostprobe`, version `1`, and license `MIT`. Install it alongside AgriCraft and Resourceful Config in a fresh test server.

Run once for each `core.seed_compost_value` of `0.3`, `0`, and `1` in `config/agricraft.jsonc`, also passing the matching JVM property `-Dcompost.value=VALUE`. For reproduction against unpatched 4.0.21, use `0.3` and add `-Dcompost.baseline=true`.

On server startup, the probe checks the configured NeoForge compost value, survival-player right-clicks with seeds both with and without genome data, top-face item-handler insertion and simulation, partially filled and processing composters, and the unchanged vanilla wheat-seed chance. It repeats the checks after a real resource reload, writes `compost-results.txt`, and stops the server. The baseline run expects the reported broken behavior: the legacy map has `0.3`, but NeoForge rejects the seeds.

A successful run ends with `RESULT N checks passed`; any `FAIL` is a regression or a broken test setup. The probe deliberately uses fake players through `ServerPlayerGameMode.useItemOn` so the normal NeoForge interaction events and composter implementation are exercised.
