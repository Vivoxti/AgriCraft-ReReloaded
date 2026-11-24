# Fluidlogging probe

`FluidloggingProbe.java` is a separate NeoForge test mod (`fluidprobe`) for `core.fluidloggable_fluids`. It registers
its own fluid, `fluidprobe:quicksilver` (with a bucket and a liquid block), the way any other mod would. The harness
also packs a datapack plant, `fluidprobe:silverbloom`, that requires that fluid in `fluid_condition`.

Run `compat-review/fluidlogging-2026-11-18/run_probe.py`. It builds the probe against the NeoForge jar and starts the
test client three times:

- **main**: the config lists `fluidprobe:quicksilver`. Checks the `agricraft_fluid` state property and the state count,
  that the cached block state fluid is the quicksilver source, pouring and picking up with buckets (a second fluid, or
  the same one again, pours above the crop instead of vanishing), draining with a Create open-ended pipe (the harness
  adds Create 6.0.10; the pipe must empty the sticks once, not pull endlessly), placing sticks inside quicksilver, the growth condition (silverbloom, poppy, kelp, lava), that
  removed plants and crops without soil leave the fluid behind, and the client/server list comparison. Leaves a
  submerged silverbloom at 4 101 0.
- **reload**: same config, so the crop must still be submerged after a restart.
- **empty**: the list is emptied, so there is no extra property, the state count is back to 816 and the saved crop loads
  dry.

Results go to `client-probe/fluid-results-<stage>.txt`.
