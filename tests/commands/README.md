# Command message regression probe

`CommandMessageProbe.java` (package `cmdprobe`, property `cmd.probe`) runs `/agricraft_seed` in its default, `all` and `distinct` forms as a fake player with a capturing command source. Each form must give exactly one seed and send exactly one reply, and each reply must encode through `ClientboundSystemChatPacket.STREAM_CODEC`. A control confirms that the pre-4.0.25 reply shape with a raw `ResourceLocation` argument still fails, so the probe can detect the original disconnect.

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
