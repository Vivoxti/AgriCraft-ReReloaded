# JEI recipe transfer with AgriCraft seeds

`JeiTransferProbe.java` is a separate test mod (never part of the release jar). In a singleplayer world with JEI it:

1. gives the player only a writable book and checks that JEI still reports the Journal recipe's seed as missing;
2. gives a writable book plus AgriCraft wheat and carrot seeds (with a genome) and checks that JEI allows the transfer;
3. runs the transfer into the 2x2 inventory grid and checks the grid holds the book and a seed and the result is the Journal;
4. checks that JEI still finds exactly one Produce recipe for a wheat seed and for a carrot seed.

Runner: `compat-review/jei-seed-transfer-2026-11-24/run_probe.py <agricraft-neoforge.jar> <label>`; results go to
`client-<label>/jei-transfer-results.txt`. 4.0.28 fails steps 2-3, the build with `RecipeTransferUtilMixin` passes all.
