"""The Wasteland chapter of the manual: rigs, scrapyards, raiders, veterans, battle tools and the rest."""
from manual import B, TIP, category, craft, entry, table, text, MODID  # noqa: F401


def chapter():
    entries = [
        entry("rig", "Wasteland Rig", MODID + ":rig", [
            craft(MODID + ":rig", "Four tyres, scrap, two iron blocks and a blast furnace. Use it on the ground "
                  "to unpack it facing the way you look.", "Wasteland Rig"),
            text(B("Driving") + ": right-click to get in. W and S drive, A and D steer, sneak gets out. A second "
                 "player can ride shotgun, and a golem or cub rides on the back deck.\n\n" + B("Fuel") + ": "
                 "right-click it with coal, charcoal, logs or any furnace fuel. Without fuel it only crawls.", "Driving"),
            text(B("Repairs") + ": iron ingots, scrap or repair kits, or park it inside a finished Garage.\n\n"
                 + B("Crew") + ": use a Golem Linker on the rig to load your golems and cubs within 8 blocks, and "
                 "again to unload them.\n\n" + B("Packing up") + ": sneak + right-click with an empty hand. The "
                 "item keeps its fuel, damage and upgrades.", "Looking After It"),
            craft(MODID + ":rig_ram", "Pointed dripstone, iron and scrap. Rams anything in front of the rig hard "
                  "at speed. Fit it by right-clicking the rig.", "Spiked Ram"),
            craft(MODID + ":rig_turret", "A crossbow, a dispenser, iron and scrap. Fires at hostile mobs within 18 "
                  "blocks while a golem rides along to work it.", "Rig Turret"),
            craft(MODID + ":rig_plating", "Scrap and iron. Much more armour.", "Rig Plating"),
            craft(MODID + ":tyre", "Dried kelp blocks round an iron nugget make 2.", "Tyre"),
            craft(MODID + ":scrap", "Iron and gravel make 3. Also found in scrapyards and taken from raiders.",
                  "Scrap"),
        ]),
    ]
    for extra in EXTRA:
        entries.append(extra())
    return category("wasteland", "The Wasteland", MODID + ":rig",
                    "Rigs, scrap and everything out past the fence.", entries)


# Later updates add their entries here.
EXTRA = []
