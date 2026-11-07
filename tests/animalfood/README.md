# Animal food regression probe

`SeedFoodProbe.java` (package `seedfoodprobe`, property `seedfood.probe`) checks that AgriCraft seeds count as their vanilla seed item for animal food and tempt tags, and only for those tags.

Tag checks cover chicken, parrot, sniffer, pig, rabbit, fox, panda, turtle, camel, strider, hoglin and bee food. Negative checks confirm that a wheat seed is not pig or cow food, a carrot seed is not chicken food, villager planting tags do not match and AgriCraft flower seeds (which have no vanilla seed item) are not rabbit or bee food. A survival fake player then feeds real entities through `Player.interactOn`: chickens with five seed types, a sniffer, pigs and a rabbit must enter love mode and consume exactly one seed; a parrot must be tamed; a cow must ignore wheat seeds.

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
