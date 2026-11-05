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


def veterans():
    return entry("veterans", "Veterans", "minecraft:iron_sword", [
        text("Golems learn on the job. They earn " + B("experience") + " from work (every few crops, stacks or "
             "chores) and from fights (more for tough mobs, 15 for a rogue, 200 for a Junk Titan). Experience stays "
             "with the golem when you pick it up or repair it.", "Experience"),
        table([
            ("Rookie", "0 xp."),
            ("Seasoned", "100 xp. +5% health and damage."),
            ("Veteran", "300 xp. +10%, and a trait."),
            ("Elite", "700 xp. +15%."),
            ("Legend", "1500 xp. +20%, and Unbreakable."),
        ], "Ranks"),
        text(B("Brawler") + ": golems that earned at least half their experience fighting hit 15% harder.\n\n"
             + B("Tireless") + ": workers use a third less power.\n\n" + B("Unbreakable") + ": Legends' parts "
             "wear half as fast.\n\nRanked golems show gold chevrons over their heads (red for Legends) when "
             "you're close, and their rank, progress and traits on the Status tab and in Jade.", "Traits"),
        table([
            ("Cheerful", "Hums little notes. Walks 5% faster."),
            ("Grumpy", "Huffs now and then. Takes 10% less damage."),
            ("Lazy", "Yawns clouds. Uses less power but walks 5% slower."),
            ("Brave", "Itching for a fight. Hits 10% harder."),
            ("Curious", "Looks around at everything. Walks a little faster."),
        ], "Personality"),
        text("Every golem is born with a " + B("personality") + ". It shows on the Status tab and in Jade, "
             "stays with the golem for life, and comes through in little idle moments.", "Born That Way"),
    ])


def horn():
    return entry("war_horn", "War Horn", MODID + ":war_horn", [
        craft(MODID + ":war_horn", "Three copper ingots, redstone and leather.", "War Horn"),
        text("Sound it and every one of your golems within 48 blocks drops what it's doing:\n\n"
             + B("Rally") + ": run to you and fight anything that comes near (30 seconds).\n"
             + B("Hold") + ": stand fast and fight anything within 8 blocks (1 minute).\n"
             + B("Charge") + ": all attack whatever you're looking at (20 seconds).\n\n"
             "Sneak + use to change the call. It needs 5 seconds to cool down.", "Calls"),
    ])


def workshop():
    return entry("workshop", "Workshop Upgrades", MODID + ":fabricator", [
        table([
            ("Scrap Shack", "Any finished Garage. Normal repair speed."),
            ("Workshop", "5 x 5 inside and 3 emitters. Repairs a quarter faster."),
            ("Industrial Bay", "7 x 7 inside, 4 emitters with at least 2 steel. Repairs half again as fast."),
        ], "Garage Tiers"),
        text("The tier shows on the neon sign, and right-clicking the controller says what the next tier needs. "
             "Steel Emitters still add their own 20% on top.", "Tiers"),
        craft(MODID + ":fabricator", "Steel, a piston, a crafting table and redstone.", "Parts Fabricator"),
        text(B("Sneak + right-click") + " to choose the part (it hovers as a hologram). Then feed it " + B("one material")
             + ": 6 for heads and cores, 7 for legs, 8 for arms, 9 for torsos. No extra ingredients needed. Its robot "
             "arm welds the part in 5 seconds.\n\nFeed it by hand, with a hopper on top or the side, or with a golem's "
             "\"deposit\" job. Take the part with a right-click or a hopper underneath.", "Using It"),
        craft(MODID + ":spray_can_flames", "Iron, gunpowder and dyes. Six finishes: flames, hazard, rust, camo, "
              "chrome and tiger stripes. Each can does 16 golems; a water bucket washes it off.", "Spray Cans"),
        craft(MODID + ":golem_tablet", "Iron, a glass pane and a redstone block.", "Golem Tablet"),
        text("A " + B("radar map") + " of all your golems around you and a list with their health, rank and what "
             "they're doing. Select one to " + B("Recall") + " it, " + B("Stop") + " or " + B("Go") + " (resume), or "
             + B("Watch") + " through its eyes if it's nearby. Sneak to stop watching.", "Using the Tablet"),
    ])


# Later updates add their entries here.
EXTRA = [finds, cards, raids, veterans, horn, workshop]
