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


def finds():
    return entry("finds", "Scrapyards and Ruins", "minecraft:barrel", [
        text(B("Scrapyards") + " turn up in plains, savannas, deserts, badlands and taigas: churned ground, junk "
             "heaps of rusted copper and iron, a battered anvil and a salvage barrel. Sometimes there's a "
             "wrecked rig too: no fuel and badly damaged, but fix it up and it's yours.", "Scrapyards"),
        text(B("Abandoned garages") + " are ruined Garages with a stuck roller door, the controller still in its "
             "wall and one emitter hanging on. Their crates always hold a broken golem. Patch the walls and "
             "roof, add a second emitter and it works again: repair the golem right there.", "Abandoned Garages"),
        table([
            ("Scrap and tyres", "Always / often."),
            ("Loose parts", "Any material, now and then a rare one."),
            ("Broken golems", "Worn right down. Repair them in a Garage and they're yours."),
            ("Rig upgrades", "Rarely: a ram, turret or plating."),
            ("Rule cards, coal, nuggets", "Sometimes."),
        ], "What You Find"),
        text("Server owners can turn these off with " + B("generateWastelandFinds") + " in the server settings, "
             "or change where they appear with the " + B("golemgarage:has_wasteland_finds") + " biome tag.",
             "Turning Them Off"),
    ])


def cards():
    return entry("rule_cards", "Rule Cards", MODID + ":rule_card", [
        craft(MODID + ":rule_card", "Two paper and redstone make 2 blank cards.", "Rule Card"),
        text(B("Copy") + ": use a blank card on your golem and its rules are punched onto the card.\n\n"
             + B("Load") + ": use a punched card on any of your golems to give it those rules.\n\n"
             + B("Share") + ": put a punched card in a crafting grid with blank cards for copies. You keep the "
             "original, so you can hand out a farm crew's rules to friends.\n\nSneak + use in the air wipes a card.",
             "Using Cards"),
    ])


def raids():
    return entry("raids", "Raider Convoys", MODID + ":rig_turret", [
        text("Once you have " + B("3 or more golems") + ", raiders may come for you at nightfall (about one night "
             "in seven). A horn sounds and a title tells you which way they're coming from. A rusted rig full of "
             + B("Rogue Golems") + " drives at your base, drops them off, and they march in to wreck your golems, "
             "cubs, villagers and you.", "Raiders!"),
        text(B("Bigger bases draw bigger raids") + ": 3 raiders, plus one for every 4 golems you have, up to 12.\n\n"
             + B("Junk Titan") + ": with 12 or more golems, one raid in three brings a giant rogue golem with its "
             "own health bar. It hits like a truck.\n\nA yellow bar counts the raiders down. Anything left at "
             "dawn slinks off.", "How Big"),
        table([
            ("Rogue Golems", "Scrap, and sometimes one of their parts."),
            ("The raiders' rig", "Once it's empty, right-click it to claim it."),
            ("Junk Titan", "A pile of scrap, three parts, a rare broken golem, a rig upgrade and diamonds."),
        ], "Spoils"),
        text("Your golems treat rogues as hostile mobs: Aggressive and Guard modes and \"attack hostile\" rules "
             "go after them, and so does a rig turret.\n\nServer owners can change or turn off raids with "
             + B("raids") + ", " + B("raidChance") + ", " + B("raidMinGolems") + " and " + B("titanMinGolems")
             + ". Operators can start one with " + B("/golemgarage raid <player> [titan]") + ".", "Fighting Back"),
    ])


# Later updates add their entries here.
EXTRA = [finds, cards, raids]
