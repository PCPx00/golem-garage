"""The Wasteland chapter of the manual: rigs, scrapyards, raiders, veterans, battle tools and the rest."""
from manual import B, TIP, category, craft, entry, table, text, MODID  # noqa: F401


def chapter():
    entries = [
        entry("rig", "Rigs", MODID + ":rig", [
            table([
                ("War Rig", "All-rounder."),
                ("Interceptor", "Fastest, with nitro."),
                ("Juggernaut", "Armoured tank, cannon."),
                ("Swamp Skimmer", "Land and water."),
            ], "Four Rigs"),
            craft(MODID + ":rig", "Four tyres, scrap, two iron blocks and a blast furnace. Use it on the ground "
                  "to unpack it facing the way you look.", "War Rig"),
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
            ("Scrap and tyres", "Nearly always."),
            ("Loose parts", "Any material, sometimes rare."),
            ("Broken golems", "Repair one to keep it."),
            ("Rig upgrades", "Rarely."),
            ("Cards, coal, nuggets", "Sometimes."),
        ], "What You Find"),
        text("Server owners can turn these off with the server setting\ngenerateWastelandFinds\n\nTo choose the "
             "biomes they appear in, edit the biome tag\nhas_wasteland_finds\n(in golemgarage).",
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
            ("Cheerful", "Hums. 5% faster."),
            ("Grumpy", "Huffs. 10% less damage."),
            ("Lazy", "Yawns. Less power, 5% slower."),
            ("Brave", "Hits 10% harder."),
            ("Curious", "Looks around. A bit faster."),
        ], "Personality"),
        text("Every golem is born with a " + B("personality") + ". It shows on the Status tab and in Jade, "
             "stays with the golem for life, and comes through in little idle moments.", "Born That Way"),
    ])


def horn():
    return entry("war_horn", "War Horn", MODID + ":war_horn", [
        craft(MODID + ":war_horn", "Three copper ingots, redstone and leather.", "War Horn"),
        text("Sound it and your golems within 48 blocks drop what they're doing:\n\n"
             + B("Rally") + ": come to you and fight (30 s).\n"
             + B("Hold") + ": stand fast, fight within 8 blocks (1 min).\n"
             + B("Charge") + ": attack what you look at (20 s).\n\n"
             "Sneak + use changes the call. 5 s cooldown.", "Calls"),
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
def rig_variants():
    return entry("rig_variants", "More Rigs", MODID + ":rig_interceptor", [
        craft(MODID + ":rig_interceptor", "Tyres, scrap, gold and a blast furnace.", "Interceptor"),
        text("Low, light and red, with a blower through the bonnet. The fastest thing on wheels, but it can't "
             "take much of a beating.\n\n" + B("Nitro") + ": hold " + B("jump") + " while driving to charge it "
             "(the bar fills like a horse's jump), let go to fire it. It kicks you forward and keeps you faster "
             "for a second or two, uses a bit of fuel, then needs a few seconds to recharge.", "Nitro"),
        craft(MODID + ":rig_juggernaut", "Obsidian, an iron block, five tyres and a blast furnace.", "Juggernaut"),
        text("A tracked tank in olive drab. Slow, but with more than twice a War Rig's health and armour, a ram "
             "that hits much harder, and tracks that climb a block and a half.", "Tank"),
        text("While anyone rides it, the turret turns to the nearest hostile mob within 24 blocks and fires a "
             "shell every few seconds. Shells blast everything hostile where they land and never break "
             "blocks.\n\nIt has its own gun, so it doesn't take the Rig Turret.", "Cannon"),
        craft(MODID + ":rig_skimmer", "Scrap, a tyre, any five boats and a blast furnace.", "Swamp Skimmer"),
        text("An airboat on a rubber skirt, pushed along by a big caged fan. It floats and keeps going over "
             "rivers, lakes and the sea at full speed, then runs straight up onto the bank. Golems and Cubs "
             "ride in front of the engine.", "Land and Water"),
        text(B("Golem drivers") + ": load a golem with the Linker and get out. With no player aboard, the golem "
             "drives and follows you, keeping a few car lengths back, so several rigs make a convoy. Sneak + "
             "right-click the rig with an empty hand to park it, and again to send it after you. A golem gunner "
             "makes the Juggernaut's cannon reload much faster.", "Golem Drivers"),
        text("Every rig takes fuel, repairs, the Linker, the Spiked Ram and Rig Plating the same way. Raiders "
             "sometimes come in Interceptors, and a Titan's convoy brings a Juggernaut. Wrecks of any kind turn "
             "up in scrapyards.", "All Rigs"),
    ])


def road_trips():
    return entry("road_trips", "Scavenging and Paint", "minecraft:filled_map", [
        text(B("Scavenging trips") + ": load golems into a rig, give it at least an eighth of a tank, get out and "
             "right-click it with a " + B("map") + ". It drives off into the wasteland and comes back about 4 "
             "minutes later (3 for an Interceptor) to the same spot, with a " + B("barrel of salvage") + " beside "
             "it.", "Road Trips"),
        text("Every golem on the trip earns experience and comes back a little worn, and the rig a little "
             "dented. More crew means a bigger haul, and a Juggernaut brings back an extra crate. You get a message "
             "when they're home.", "The Haul"),
        text(B("Paint jobs") + ": spray cans work on rigs too. Flames, hazard stripes, rust, camo, chrome or "
             "tiger stripes cover the bodywork. A water bucket washes it back to factory paint, and the paint "
             "stays when you pack the rig up.", "Paint Jobs"),
        text(B("Dust storms") + ": now and then on a clear day, a storm blows through deserts, badlands and "
             "savannas for a few minutes. Out in the open the sky turns to a tan haze, sand streams past, golem "
             "parts wear three times as fast as in sand, and the wind shoves rigs sideways. Get your golems "
             "indoors, or seal their parts with honeycomb.", "Dust Storms"),
    ])


def arena():
    return entry("arena", "Golem Arena", MODID + ":arena_bell", [
        craft(MODID + ":arena_bell", "A copper block on a log, on three scrap.", "Arena Bell"),
        text("Stand two golems inside the bell's ring (6 blocks, marked by golden dust) and " + B("ring it")
             + ". They spar while a bar shows each one's health. The first down to a quarter of its health "
             "yields: nobody breaks down.", "Sparring"),
        text("The winner earns 40 experience and an " + B("arena win") + ", the loser still earns 15. A minute "
             "without a winner is a draw. Arena wins show when you look at a golem on a showcase shelf. Your own "
             "golems can spar each other.", "Glory"),
        craft(MODID + ":garage_blueprint", "Two paper, blue dye and a Garage Controller.", "Garage Blueprint"),
        text(B("Sneak + use") + " it on a Garage Controller to copy its size and style. Then use it on the "
             + B("ground") + ": it puts down a controller from your inventory with that plan and starts the auto "
             "build if you carry the blocks. Use it on another controller to give it the same plan.", "Copying Garages"),
    ])


def gadgets():
    return entry("rig_gadgets", "Rig Gadgets", MODID + ":rig_harpoon", [
        text("Four bolt-on gadgets fit any rig: use one on the rig to fit it. They come off again if the rig is "
             "wrecked, and stay on when you pack it up. Work them with keys while you drive (change them in "
             "Controls).", "Gadgets"),
        craft(MODID + ":rig_harpoon", "Iron, flint, a crossbow and scrap.", "Harpoon"),
        text(B("Z") + " fires the harpoon where you look, up to 24 blocks. The first mob it hits is hurt and "
             "dragged back to the rig. It reloads in a couple of seconds.", "Harpoon"),
        craft(MODID + ":rig_smoke", "Two campfires, gunpowder and scrap.", "Smoke Dispenser"),
        text(B("V") + " drops a thick cloud behind the rig. Mobs caught in it are blinded, slowed and lose track "
             "of you. 15 seconds to refill.", "Smoke"),
        craft(MODID + ":rig_spikes", "Pointed dripstone, iron and scrap.", "Spike Dropper"),
        text(B("B") + " drops a row of spike strips across the road behind you. Mobs and raiders' rigs that "
             "cross them are slowed and hurt; you, your golems, Cubs and your own rigs roll over safely. The "
             "spikes rust away after 30 seconds.", "Spikes"),
        craft(MODID + ":rig_rack", "Iron bars, a chest, iron and scrap.", "Roof Rack"),
        text(B("R") + " opens nine slots of storage on the roof (anyone aboard can open it). The load stays when "
             "you pack the rig up and spills out if it's wrecked. " + B("H") + " switches the headlights and "
             "their beams on or off.", "Rack and Lights"),
        craft(MODID + ":jerry_can", "A bucket, iron and scrap.", "Jerry Can"),
        text("Use a jerry can on a " + B("Scrap Burner") + " to fill it from the lava tank (a bucket fills it) or "
             "from the fuel waiting in its slot. Use it on a rig to top up the tank: a full can fills a rig from "
             "empty.", "Fuel Run"),
    ])


def decor():
    return entry("garage_decor", "Decorations and Trophies", MODID + ":neon_garage", [
        text("Dress up the yard. " + B("Tyre stacks") + " grow one tyre at a time: use a tyre on one, or an empty "
             "hand to take one off. " + B("Oil drums") + " come in red, blue and rust; light one with flint and "
             "steel for a burn barrel, and put it out with a shovel or water.", "Junkyard"),
        craft(MODID + ":workbench", "A crafting table under iron, on scrap.", "Garage Workbench"),
        text("The " + B("Garage Workbench") + " works as a crafting table. The " + B("Chain Hoist") + " hangs "
             "from a ceiling over your repair bay, and " + B("neon signs") + " (Open, Garage, Scrap) hang flat on "
             "a wall. Right-click a sign to switch it on or off.", "Workshop"),
        text(B("Trophies") + ": a Junk Titan drops its " + B("head") + " for your wall. A golem that reaches "
             "Legend earns a " + B("Legend Medal") + ", and every fifth arena win earns an " + B("Arena Medal")
             + ", engraved with the golem's name. Medals go to the owner, or drop beside the golem.", "Trophies"),
    ])


EXTRA = [rig_variants, gadgets, road_trips, arena, decor, finds, cards, raids, veterans, horn, workshop]
