"""Verify release packs against a real Croptopia 4.x jar (Python standard library only).

Usage: python tests/verify_croptopia_pack.py Croptopia.jar AgriCraft-neoforge.jar AgriCraft-fabric.jar
"""

import json
import sys
from pathlib import Path
from zipfile import ZipFile


def read_json(archive, name):
    return json.loads(archive.read(name))


def verify(native_path, release_paths):
    with ZipFile(native_path) as native:
        native_files = set(native.namelist())
        item_prefix = "assets/croptopia/models/item/"
        seeds = {
            Path(name).stem
            for name in native_files
            if name.startswith(item_prefix) and name.endswith(("_seed.json", "vanilla_seeds.json"))
        }
        crop_names = {seed.removesuffix("_seeds").removesuffix("_seed") for seed in seeds}
        assert len(crop_names) == 58, f"Unexpected native farmland crop count: {len(crop_names)}"
        for release_path in release_paths:
            with ZipFile(release_path) as release:
                files = set(release.namelist())
                data_prefix = "datapacks/croptopia/data/croptopia/agricraft/plants/"
                resource_prefix = "resourcepacks/croptopia/assets/croptopia/"
                plants = {Path(name).stem: read_json(release, name)
                          for name in files if name.startswith(data_prefix) and name.endswith(".json")}
                assert set(plants) == crop_names, "Missing or unexpected crops"
                assert not any(name.startswith("datapacks/croptopia/") and "/mutations/" in name
                               for name in files), "Unrequested species mutation recipes"
                assert read_json(release, "datapacks/croptopia/pack.mcmeta")["pack"]["pack_format"] == 48
                assert read_json(release, "resourcepacks/croptopia/pack.mcmeta")["pack"]["pack_format"] == 34
                for name, plant in plants.items():
                    seed = name + ("_seeds" if name == "vanilla" else "_seed")
                    assert plant["seeds"][0]["item"] == "croptopia:" + seed, name
                    assert plant["seeds"][0]["grass_drop_chance"] == 0, name
                    assert plant["mods"] == ["croptopia"], name
                    assert len(plant["stages"]) == 8 and plant["harvest_stage"] == 0, name
                    for product in plant["products"]:
                        product_name = product["item"].split(":")[1]
                        assert item_prefix + product_name + ".json" in native_files, product_name
                    native_states = read_json(native, "assets/croptopia/blockstates/" + name + "_crop.json")
                    for stage in range(8):
                        model = read_json(release, resource_prefix + f"models/crop/{name}_stage{stage}.json")
                        assert model["parent"] == native_states["variants"][f"age={stage}"]["model"], name
                        assert "assets/croptopia/models/" + model["parent"].split(":")[1] + ".json" in native_files
                    seed_model = read_json(release, resource_prefix + "models/seed/" + name + ".json")
                    assert seed_model["parent"] == "croptopia:item/" + seed, name
                for locale in ("en_us", "ru_ru", "uk_ua", "zh_cn"):
                    translations = read_json(release, resource_prefix + f"lang/{locale}.json")
                    for name in crop_names:
                        assert translations["plant.agricraft.croptopia." + name]
                        assert translations["seed.agricraft.croptopia." + name]
                assert not any(name.startswith("mcjty/theoneprobe/") for name in files), "TOP bundled into release"
                top_class = "com/agricraft/agricraft/compat/theoneprobe/AgriCraftProbePlugin.class"
                assert (top_class in files) == ("neoforge" in Path(release_path).name), "TOP platform isolation"
                print(f"PASS {Path(release_path).name}: 58 crops, 464 crop models, 58 seed models, four locales, optional TOP.")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    verify(sys.argv[1], sys.argv[2:])
