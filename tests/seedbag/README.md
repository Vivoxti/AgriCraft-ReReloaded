# Seed bag regression probes

`SeedBagProbe.java` (package `seedbagprobe`, property `seedbag.probe`) uses a fake player's inventory menu and `AbstractContainerMenu.clicked` to exercise both insert paths (bag onto seeds, seeds onto bag), rejection of another species, merging of identical genomes, save/load round trips, the capacity limit with leftovers, planting from the bag through `ServerPlayerGameMode.useItemOn`, full extraction back to a clean empty bag and reuse with a new species.

`SorterProbe.java` (package `seedbagprobe`, property `sorter.probe`) checks the `agricraft:seed_bag_sorter` payload codec, the scroll direction mapping, that all seven sorters are registered before any genome is used, forward and backward wrap-around through the server handler, unchanged contents, empty bags and that non-bag items are ignored. Client input routing (NeoForge `InputEvent.MouseScrollingEvent`, Fabric `MouseHandlerMixin`) is not covered and needs a graphical client.

Each probe is a plain class annotated with `@EventBusSubscriber(modid = "agricraft")` that runs on `ServerStartedEvent` only when its system property is set, prints `PASS`/`FAIL` lines and halts the server. To run it, temporarily copy it into `neoforge/src/main/java/<package>/` and add a temporary Loom run to `neoforge/build.gradle`:

```groovy
probe {
	server()
	runDir "build/probe-run"
	vmArgs "-D<property>=true"
	programArgs "--nogui"
}
```

Create `neoforge/build/probe-run/eula.txt` and a `server.properties` with a flat level, run `./gradlew :neoforge:runProbe`, then remove the class and the run. Test classes must never stay in production source sets; `tests/starfield/verify_release.py` rejects their packages in release JARs.
