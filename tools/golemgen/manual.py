"""
The Golem Engineer's Manual: writes assets/golemgarage/manual/book.json.

Chapters are written by hand; every shape and material gets its own generated entry with numbers
computed from shapes.py and materials.py, so the book always matches the game.

Text formatting: Minecraft section codes. B() makes bold, TIP() and WARN() add colored labels.
"""
import json
import re
import os

import manual_art
import materials as M
import shapes as S
from items import MODULES
from outfits import OUTFITS

MODULE_NAMES = {k: v[0] for k, v in MODULES.items()}

MODID = "golemgarage"
SLOT_TITLES = {"HEAD": "Heads", "TORSO": "Torsos", "ARMS": "Arms", "LEGS": "Legs", "CORE": "Cores"}
SLOT_NBT = {"HEAD": "head", "TORSO": "torso", "ARMS": "arms", "LEGS": "legs", "CORE": "core"}
STANDARD = {"HEAD": "sentry_head", "TORSO": "plated_chest", "ARMS": "golem_fists", "LEGS": "stub_legs",
            "CORE": "redstone_core"}
SHAPE = {s["id"]: s for s in S.SHAPES}
MAT = M.BY_ID


def B(t):
    return "§l" + t + "§r"


def TIP(t):
    return "§2§lTip:§r " + t


def WARN(t):
    return "§4§lCareful:§r " + t


def NOTE(t):
    return "§1§lNote:§r " + t


# ------------------------------------------------------------------ page builders
def text(t, title=None):
    p = {"type": "text", "text": t}
    if title:
        p["title"] = title
    return p


def spot(item, t, title=None):
    p = {"type": "spotlight", "item": item, "text": t}
    if title:
        p["title"] = title
    return p


def craft(recipe, t="", title=None):
    p = {"type": "crafting", "recipe": recipe, "text": t}
    if title:
        p["title"] = title
    return p


def golem(snbt, t="", title=None, height=84):
    p = {"type": "golem", "golem": snbt, "text": t, "height": height}
    if title:
        p["title"] = title
    return p


def image(name, size, t="", title=None):
    p = {"type": "image", "texture": "%s:textures/gui/manual/%s.png" % (MODID, name),
         "width": size[0], "height": size[1], "text": t}
    if title:
        p["title"] = title
    return p


def items(stacks, t="", title=None, links=None):
    p = {"type": "items", "items": stacks, "text": t}
    if title:
        p["title"] = title
    if links:
        p["links"] = links
    return p


def table(rows, title=None):
    p = {"type": "table", "rows": [list(r) for r in rows]}
    if title:
        p["title"] = title
    return p


def entry(eid, title, icon, pages):
    return {"id": eid, "title": title, "icon": icon, "pages": pages}


def category(cid, title, icon, desc, entries):
    return {"id": cid, "title": title, "icon": icon, "desc": desc, "entries": entries}


# ------------------------------------------------------------------ golem SNBT
def max_dur(mat, shape):
    v = (60 + 40 * mat["tier"]) * mat["dur"] * S.SLOT_DURABILITY[shape["slot"]]
    return int(v + 0.5)


def part(shape_id, mat_id, wear=0.0, ox=0):
    sh, mat = SHAPE[shape_id], MAT[mat_id]
    s = '{Shape:"%s",Material:"%s"' % (shape_id, mat_id)
    dmg = round(max_dur(mat, sh) * wear)
    if dmg:
        s += ",Damage:%d" % dmg
    if ox:
        s += ",Ox:%d" % ox
    return s + "}"


def build(parts, vines=False):
    """parts: dict slot -> (shape, material[, wear[, ox]]) or None."""
    out = []
    for slot in S.SLOT_ORDER:
        v = parts.get(slot)
        if v:
            out.append("%s:%s" % (SLOT_NBT[slot], part(*v)))
    s = "{Parts:{%s}" % ",".join(out)
    if vines:
        s += ",Vines:1b"
    return s + "}"


def matched(mat_id, **over):
    parts = {slot: (shape, mat_id) for slot, shape in STANDARD.items()}
    for k, v in over.items():
        parts[k.upper()] = v
    return build(parts)


def with_shape(shape_id, mat_id="iron"):
    parts = {slot: (shape, mat_id) for slot, shape in STANDARD.items()}
    parts[SHAPE[shape_id]["slot"]] = (shape_id, mat_id)
    return build(parts)


# ------------------------------------------------------------------ numbers
def health_mult(m):
    return 1.0 + 0.3 * m["tier"]


def shape_line(sh, m):
    """What this shape gives when made from material m (matches GolemStats)."""
    st = sh["stats"]
    hm = health_mult(m)
    dur = max_dur(m, sh)
    slot = sh["slot"]
    if slot == "HEAD":
        return "Sight %d, +%s health, %d durability" % (st["vision"] + 2 * m["tier"], fmt(st["health"] * hm), dur)
    if slot == "TORSO":
        carry = min(36, st["carry"] + (9 if m["tier"] >= 5 else 0))
        return "+%s health, carries %d stacks, %d durability" % (fmt(st["health"] * hm), carry, dur)
    if slot == "ARMS":
        work = st["work"] * (1 + 0.1 * m["tier"]) * m["work"]
        return "Damage %s, work %d%%, +%s health, %d durability" % (
            fmt(st["attack"] + 1.5 * m["tier"]), round(work * 100), fmt(st["health"] * hm), dur)
    if slot == "LEGS":
        kb = st["kb"] + (0.15 if "HEAVY" in m["traits"] else 0)
        return "Speed %d%%, knockback resist %d%%, +%s health, %d durability" % (
            round(st["speed"] * m["speed"] * 100), round(min(1, kb) * 100), fmt(st["health"] * hm), dur)
    cap = st["capacity"] * (1 + 0.25 * m["tier"]) * m["power"] * (1.15 if "CONDUCTIVE" in m["traits"] else 1)
    return "Power %d, %d durability" % (round(cap), dur)


def fmt(v):
    return ("%.1f" % v).rstrip("0").rstrip(".")


def matched_stats(m):
    """Stats of the standard five-part golem in one material (matches GolemStats.compute)."""
    shapes = [SHAPE[STANDARD[s]] for s in S.SLOT_ORDER]
    hm = health_mult(m)
    health = 20 + sum(sh["stats"].get("health", 0) * hm for sh in shapes)
    arms = SHAPE["golem_fists"]["stats"]
    legs = SHAPE["stub_legs"]["stats"]
    core = SHAPE["redstone_core"]["stats"]
    conductive = 5 if "CONDUCTIVE" in m["traits"] else 0
    cap = core["capacity"] * (1 + 0.25 * m["tier"]) * m["power"]
    cap = round(round(cap) * (1 + 0.15 * conductive))
    speed = legs["speed"] * m["speed"] * 1.1
    kb = legs["kb"] + (0.15 * 5 if "HEAVY" in m["traits"] else 0)
    return [
        ("Health", fmt(health)),
        ("Armor", fmt(0.8 * m["tier"] * 5)),
        ("Damage", fmt(arms["attack"] + 1.5 * m["tier"])),
        ("Speed", "%d%% of a normal golem" % round(speed * 100)),
        ("Knockback resist", "%d%%" % round(min(1, kb) * 100)),
        ("Sight", "%d blocks" % (SHAPE["sentry_head"]["stats"]["vision"] + 2 * m["tier"])),
        ("Power", str(cap)),
    ]


ABILITY_TEXT = {
    "SPOTTER": "Spotter: hostile mobs within its sight range start glowing, so you can see them through walls.",
    "LIGHT": "Lantern: lights up the ground around it (light level 14) as it walks, and the light follows it.",
    "SEALED": "Sealed: rain and desert sand can't wear this part down.",
    "SMELTER": "Smelter: smelts ores, raw metals and food from its own inventory, burning fuel it carries. "
               "About 10 seconds an item, faster with better arms.",
    "KNOCKBACK": "Knockback: every hit throws enemies back and up a little.",
    "COLLECT": "Collector: picks up items from twice as far away (10 blocks) and reaches further for them. "
               "Can also harvest crops, one block at a time.",
    "HARVEST": "Harvester: can run the Harvest Crops action.",
    "SWEEP": "Sweep: each hit also strikes every other hostile within 2.5 blocks for half damage.",
    "STURDY": "Sturdy: very hard to push around.",
    "TALL_STEP": "Tall step: walks straight up full blocks without jumping (step height 1.1).",
    "TREADS": "Treads: can't be knocked back at all and never takes fall damage.",
    "NO_JUMP": "Wheels: the fastest legs on flat ground, but they can't jump. Build ramps or slabs.",
    "CLIMB": "Climber: walks straight up walls like a spider.",
    "RECHARGE_REDSTONE": "Recharges by burning redstone it carries: a redstone block gives 100, dust gives 12 "
                         "(only while below 75%). Any golem can also charge next to a placed redstone block "
                         "with the Recharge action.",
    "RECHARGE_FUEL": "Recharges by burning fuel it carries, like a furnace: coal gives 80 power. It never burns "
                     "an item it could smelt instead.",
    "RECHARGE_STORM": "Charges under open sky while it rains (1 power a second) and three times faster in thunderstorms. A "
                      "lightning strike fills it completely and doesn't hurt it.",
    "RECHARGE_SUN": "Charges in daylight under open sky when it isn't raining (1 power a second).",
    "RECHARGE_SOUL": "Slowly charges anywhere, even underground (1 power every 3 seconds), and mends 1 health every 5 seconds.",
}

TRAIT_TEXT = {
    "OXIDIZES": ("Oxidizes", "Copper parts slowly turn exposed, weathered, then oxidized, and get 3% slower each "
                             "stage. Wax with honeycomb to stop it; scrape a stage off with an axe."),
    "CONDUCTIVE": ("Conductive", "Each conductive part adds 15% power capacity, and recharging is 25% faster."),
    "HARDENED": ("Hardened", "30% chance to ignore each point of wear."),
    "FIREPROOF": ("Fireproof", "With three or more fireproof parts the golem is immune to fire and lava."),
    "HEAVY": ("Heavy", "+15% knockback resistance for each heavy part."),
    "SMITE": ("Smite", "Arms of this material deal 50% more damage to undead mobs."),
    "LUMINOUS": ("Luminous", "The golem gives off light (level 10) wherever it goes."),
    "ENDER": ("Ender", "While following you, it teleports to you when left more than 24 blocks behind."),
    "THORNS": ("Thorns", "Reflects 8% of melee damage per part back at the attacker, up to 40%."),
    "FEATHERWEIGHT": ("Featherweight", "Legs of this material walk on water (and lava, if the golem is "
                                       "fireproof), and it takes no fall damage."),
}

INGREDIENT_ITEM = {
    "stone": "minecraft:cobblestone", "copper": "minecraft:copper_ingot", "iron": "minecraft:iron_ingot",
    "gold": "minecraft:gold_ingot", "steel": MODID + ":steel_ingot", "diamond": "minecraft:diamond",
    "netherite": "minecraft:netherite_ingot",
}

VANILLA = [m for m in M.CRAFTABLE if m["mod"] in ("Minecraft", "Golem Garage")]


def mat_icon(m):
    return "%s:golem@%s" % (MODID, m["id"])


# ------------------------------------------------------------------ chapters
def getting_started():
    starter = build({"HEAD": ("sentry_head", "stone"), "TORSO": ("plated_chest", "stone"),
                     "ARMS": ("golem_fists", "stone"), "LEGS": ("stub_legs", "stone"),
                     "CORE": ("redstone_core", "iron")})
    return category("start", "Getting Started", MODID + ":manual",
                    "Your first golem, start to finish, and how to read this book.", [
        entry("welcome", "Welcome, Engineer", MODID + ":manual", [
            text("This manual covers everything about Golem Garage: every part, every material, the "
                 "Garage, power, jobs and Cubs.\n\nGolems are built from five parts. Each part is a "
                 + B("shape") + " (what it does) made from a " + B("material") + " (how strong it is). The "
                 "golem looks exactly like what it's made of: iron arms are iron, gold legs are gold."),
            text(B("Reading the book") + "\n\n- Click a chapter, then an entry.\n- Arrows at the bottom "
                 "corners turn pages; so do the scroll wheel and the arrow keys.\n- The middle button goes "
                 "back; right-click or Backspace does too.\n- The top button goes home.\n- Hover any item to "
                 "see it. Items with a blue \"Click to read\" line open their own entry.\n- Search from the "
                 "home page.\n\nThe book reopens where you left it."),
        ]),
        entry("first_golem", "Your First Golem", MODID + ":golem", [
            text("A complete golem takes about ten minutes and some cobblestone, redstone and a little iron.\n\n"
                 + B("1.") + " Craft a Golem Frame.\n" + B("2.") + " Place it.\n" + B("3.") + " Craft parts "
                 "and right-click the frame with each one.\n" + B("4.") + " Give it a core so it has power.\n"
                 + B("5.") + " Open its panel and give it a job.", "Ten Minutes to a Golem"),
            craft(MODID + ":golem", "Cobblestone in a ring with redstone in the middle. The bottom middle stays "
                  "empty.", "Golem Frame"),
            golem(build({}), "Place the frame on the ground. It's a bare metal skeleton: it follows you, but it "
                  "is slow and weak until you fit parts.", "The Bare Frame"),
            craft(MODID + ":part/stub_legs/stone", "Every part is crafted the same way: the material goes in "
                  "the M spots. Stone parts use cobblestone and are the cheapest. Right-click the golem with "
                  "a part to fit it.", "Stone Stub Legs"),
            craft(MODID + ":part/redstone_core/iron", "The core holds power. Without one the golem works, but "
                  "25% slower. A Redstone Core needs a redstone block in the middle.", "Redstone Core"),
            golem(starter, "Fit a head, torso, arms and legs too. Here is a stone golem with an iron core: "
                  "cheap, slow, and a fine helper for your first night.", "A Starter Golem"),
            text("Right-click the golem with an empty hand to open its " + B("panel") + ".\n\nThe " + B("Status")
                 + " tab shows a 3D view, each part with its durability bar and a button to remove it, health, "
                 "power, armor, damage, speed, sight and carrying space, what it's doing right now, and its "
                 "recent activity.\n\nThe " + B("Rules") + " tab is where you teach it jobs.", "The Panel"),
            text(B("Press G") + " while looking at a golem (or shift + right-click it with an empty hand) to "
                 "pick it up as an item. It keeps its parts, wear, rules, links, name and everything it carries. "
                 "Right-click the ground to place it again.\n\n" + B("Upgrading") + " is just fitting a better part: the old one "
                 "comes back to you with its wear intact.", "Picking Up and Upgrading"),
        ]),
        entry("controls", "Quick Reference", "minecraft:lever", [
            table([
                ("Empty hand", "Open the golem's panel."),
                ("Shift + empty hand", "Pick the golem up as an item. The G key does the same."),
                ("A part", "Fit it. The old part comes back to you."),
                ("Repair Kit", "Repair a quarter of every part and health."),
                ("Honeycomb", "Wax copper parts so they stop aging."),
                ("Axe", "Scrape one stage of oxidation off copper."),
                ("Vines", "Drape it in vines."),
                ("Shears", "Trim the vines off."),
                ("Name tag", "Name it."),
                ("Golem Linker", "Give it the Linker's marks. Shift: clear its links."),
                ("Blueprint", "Record its build, or rebuild it from one."),
                ("Broken Golem item", "Right-click a Garage Controller to repair it."),
            ], "Right-click a Golem With..."),
        ]),
        entry("manual_recipe", "This Manual", MODID + ":manual", [
            craft(MODID + ":manual", "Every player gets one copy the first time they join. Lost it? Craft "
                  "another from a book and an iron ingot.", "Golem Engineer's Manual"),
        ]),
    ])


def anatomy():
    iron = matched("iron")
    desert = build({"HEAD": ("visor_head", "steel"), "TORSO": ("sealed_hold", "steel"),
                    "ARMS": ("grabber_claws", "steel"), "LEGS": ("tread_legs", "steel"),
                    "CORE": ("solar_core", "steel")})
    return category("anatomy", "Golem Anatomy", MODID + ":golem@iron",
                    "How a golem's five parts add up: slots, stats, scaling and set bonuses.", [
        entry("slots", "The Five Slots", MODID + ":sentry_head@iron", [
            golem(iron, "Every golem has five slots. Each can hold one part, and each part shows on the golem "
                  "in its own material.", "Five Slots"),
            table([
                ("Head", "Sight range (how far it notices hostiles and crops) and a little health. Shapes add "
                         "light, sealing or long sight."),
                ("Torso", "Most of the health and how many stacks it carries. Shapes add smelting or sealing."),
                ("Arms", "Damage and work speed. The shape decides which jobs it can do."),
                ("Legs", "Speed, step height and knockback resistance. Shapes add climbing, wheels or treads."),
                ("Core", "Power capacity and how it recharges. Its color is the glow of the eyes and core "
                         "window. The core sits on the golem's back."),
            ], "What Each Slot Does"),
            text(B("Empty slots") + " show the bare frame and work poorly:\n\n- No legs: 30% slower.\n- No "
                 "arms: weak 2-damage hits and half work speed.\n- No head: sees only 6 blocks.\n- No torso: "
                 "carries 9 stacks.\n- No core: no power system at all, and 25% slower.", "Missing Parts"),
        ]),
        entry("stats", "Stats Explained", "minecraft:spyglass", [
            table([
                ("Health", "20 for the frame, plus each part's health. At zero the golem breaks down; it "
                           "never dies."),
                ("Armor", "0.8 per part per material tier."),
                ("Damage", "The arms' damage plus 1.5 per tier."),
                ("Speed", "Shown as a percent of a normal golem. Legs and material set it."),
                ("Sight", "The head's sight plus 2 per tier. A broken head sees 4 blocks."),
                ("Carry", "Inventory stacks, from the torso. Tier 5+ torsos carry 9 more (up to 36)."),
                ("Power", "From the core. Used by moving, hitting and working."),
                ("Work", "How fast jobs go: hauling, harvesting, smelting. From the arms."),
            ], "Reading the Panel"),
            text(B("How tier scales a part") + "\n\n- Durability: (60 + 40 x tier) x material durability x "
                 "slot factor (head 0.8, torso 1.2, arms 1.0, legs 1.0, core 0.9).\n- Health: shape health x "
                 "(1 + 0.3 x tier).\n- Armor: 0.8 x tier.\n- Damage: shape damage + 1.5 x tier.\n- Power: "
                 "shape power x (1 + 0.25 x tier) x material power.\n- Sight: shape sight + 2 x tier.\n- "
                 "Work: shape work x (1 + 0.1 x tier) x material work.", "The Formulas"),
        ]),
        entry("set_bonuses", "Set Bonuses", MODID + ":golem@diamond", [
            golem(matched("diamond"), "All five parts in the same material make a " + B("matched set") + ": "
                  "+10% speed, 10% less wear, and Garage repairs 20% faster. The panel shows \"Set!\".",
                  "Matched Set"),
            text(B("Garage kit") + "\nThree or more steel-family parts (Steel, Crimson Steel, Tyrian Steel): "
                 "Garage repairs 20% faster. This stacks with a matched set.\n\n" + B("Desert kit") + "\nVisor "
                 "Head + Sealed Hold + Tread Legs: no part wears in rain or desert sand, sealed or not.",
                 "Other Sets"),
            golem(desert, "A steel desert kit: visor, sealed hold and treads, with claws and a solar core. "
                  "It also counts as a Garage kit.", "Desert Kit"),
        ]),
        entry("abilities", "Shape Abilities", MODID + ":scythe_arms@iron", [
            table([(k.replace("RECHARGE_", "Recharge: ").replace("_", " ").title(), v.split(": ", 1)[-1])
                   for k, v in ABILITY_TEXT.items() if not k.startswith("RECHARGE")],
                  "Shape Abilities"),
        ]),
    ])


def shape_entries():
    cats = []
    descs = {
        "HEAD": "Heads set how far a golem sees, and some carry lights, scopes or seals.",
        "TORSO": "Torsos give most of the health and all of the storage. One is a working furnace.",
        "ARMS": "Arms decide damage, work speed and which jobs a golem can do.",
        "LEGS": "Legs set speed and how a golem moves: climbing walls, rolling, or crawling on treads.",
        "CORE": "Cores store power and decide how it recharges. Their color lights the golem's eyes.",
    }
    for slot in S.SLOT_ORDER:
        entries = []
        for sh in S.SHAPES:
            if sh["slot"] != slot:
                continue
            sid = sh["id"]
            pages = [golem(with_shape(sid), sh["desc"], sh["name"])]
            rows = [(m["name"], shape_line(sh, m)) for m in VANILLA]
            pages.append(table(rows, "In Each Material"))
            abil = [ABILITY_TEXT[a] for a in sh["abilities"]]
            extra = ""
            if sid == "furnace_chest":
                extra = "\n\n" + TIP("give it a stack of raw iron and some coal and it hands you ingots. Pair it "
                                     "with a Furnace Core and it fuels itself from the same coal.")
            if sid == "grabber_claws":
                extra = "\n\n" + TIP("the best arms for a hauler or a collector.")
            if sid == "scythe_arms":
                extra = "\n\nScythes harvest a 3 x 3 patch of ripe crops with every swing and replant them."
            if sid == "wheel_legs":
                extra = "\n\n" + WARN("a golem on wheels can't climb a single block. Use slabs or stairs.")
            if sid == "spider_legs":
                extra = "\n\nThe legs splay out to the sides and walk with a rippling spider gait."
            if sid == "lightning_core":
                extra = "\n\n" + TIP("put a lightning rod near where it works during storms.")
            if sid == "solar_core":
                extra = "\n\nThe panel on its back tilts up toward the sky."
            if abil or extra:
                pages.append(text("\n\n".join(abil) + extra if abil else extra.strip(), "Abilities"))
            else:
                pages.append(text("No special ability: a solid, balanced choice. Its numbers are on the "
                                  "previous page.", "Abilities"))
            pattern, key = sh["recipe"]
            keys = ", ".join("%s = %s" % (k, v.lstrip("#").split(":")[1].replace("_", " ")) for k, v in key.items())
            pages.append(craft("%s:part/%s/iron" % (MODID, sid), "Shown in iron. M is the material: use any "
                               "material's ingot (or cobblestone for stone). %s." % keys))
            stacks = ["%s:%s@%s" % (MODID, sid, m["id"]) for m in M.CRAFTABLE]
            links = ["mat_" + m["id"] for m in M.CRAFTABLE]
            pages.append(items(stacks[:len(VANILLA)], "Click one to read about that material.", "Materials",
                               links[:len(VANILLA)]))
            mod_stacks = stacks[len(VANILLA):]
            pages.append(items(mod_stacks, "These appear only when a mod that adds the metal is installed.",
                               "Modded Materials", links[len(VANILLA):]))
            entries.append(entry("shape_" + sid, "%s: %s" % (slot.title(), sh["name"]), "%s:%s@iron" % (MODID, sid),
                                 pages))
        cats.extend(entries)
    return [category("parts", "Parts", MODID + ":golem_fists@iron",
                     "All 22 part shapes: heads, torsos, arms, legs, then cores. Each entry shows the shape "
                     "on a golem, its numbers in every material, and its recipe.", cats)]


def material_entries():
    entries = []
    overview_rows = [(m["name"], "Tier %d. %s" % (m["tier"], m["mod"])) for m in M.CRAFTABLE]
    entries.append(entry("materials_overview", "All Materials", MODID + ":golem@iron", [
        text("A material is matched by its ingot " + B("tag") + ", so any mod's version of an ingot works. A "
             "material only appears in recipes and the Parts tab when some installed mod provides it.\n\n"
             "Higher tiers mean more health, armor, damage, power, sight and durability. Some materials also "
             "have traits: special effects listed in their entries.", "Materials"),
        table(overview_rows, "By Tier"),
    ]))
    traits_pages = []
    for k, (name, desc) in TRAIT_TEXT.items():
        mats = [m for m in M.CRAFTABLE if k in m["traits"]]
        traits_pages.append(items(["%s:golem@%s" % (MODID, m["id"]) for m in mats], desc, name,
                                  ["mat_" + m["id"] for m in mats]))
    entries.append(entry("traits", "Material Traits", MODID + ":golem@copper", traits_pages))
    for m in M.CRAFTABLE:
        mid = m["id"]
        where = {"Minecraft": "vanilla Minecraft", "Golem Garage": "this mod (2 iron ingots + coal)"}.get(
            m["mod"], m["mod"])
        intro = "Tier %d. Uses %s, usually from %s." % (m["tier"], "#" + m["tag"], where)
        needs = []
        if m["mod"] not in ("Minecraft", "Golem Garage"):
            needs = [("Needs", "%s, or any mod that adds this ingot. Hidden until then." % m["mod"])]
        pages = [golem(matched(mid), intro, m["name"])]
        mult = []
        for key, label in (("dur", "Durability"), ("speed", "Speed"), ("power", "Power"), ("work", "Work")):
            if m[key] != 1.0:
                mult.append((label, "x%s" % fmt(m[key])))
        trait_rows = [(TRAIT_TEXT[t][0], TRAIT_TEXT[t][1]) for t in m["traits"]]
        pages.append(table(matched_stats(m), "A Matched %s Golem" % m["name"]))
        pages.append(table(needs + (mult + trait_rows if (mult or trait_rows) else
                           [("Balanced", "No special multipliers or traits.")]), "Special"))
        if mid == "copper":
            stages = build({"HEAD": ("sentry_head", "copper", 0, 0), "TORSO": ("plated_chest", "copper", 0, 1),
                            "ARMS": ("golem_fists", "copper", 0, 2), "LEGS": ("stub_legs", "copper", 0, 3),
                            "CORE": ("redstone_core", "copper")})
            pages.append(golem(stages, "Copper ages part by part: new on the head, exposed on the torso, "
                                       "weathered arms and oxidized legs.", "Aging"))
        if mid in INGREDIENT_ITEM:
            pages.append(craft("%s:part/plated_chest/%s" % (MODID, mid), "Every shape can be made from %s. "
                               "Here is a Plated Chest." % m["name"]))
        stacks = ["%s:%s@%s" % (MODID, sh["id"], mid) for sh in S.SHAPES]
        pages.append(items(stacks, "Every %s part. Click one to read about its shape." % m["name"],
                           "%s Parts" % m["name"], ["shape_" + sh["id"] for sh in S.SHAPES]))
        entries.append(entry("mat_" + mid, m["name"], "%s:golem@%s" % (MODID, mid), pages))
    return category("materials", "Materials", MODID + ":golem@gold",
                    "Every material, what it's good for, and the traits that make some special.", entries)


def wear_chapter():
    worn = build({"HEAD": ("sentry_head", "iron", 0.3), "TORSO": ("plated_chest", "iron", 0.6),
                  "ARMS": ("golem_fists", "iron", 0.85), "LEGS": ("stub_legs", "iron", 1.0),
                  "CORE": ("redstone_core", "iron")})
    vines = build({slot: (shape, "stone") for slot, shape in STANDARD.items()}, vines=True)
    return category("wear", "Wear and Repair", MODID + ":repair_kit",
                    "Parts wear out one by one. How to read the damage and fix it.", [
        entry("wear", "How Parts Wear", MODID + ":golem_fists@iron", [
            golem(worn, "Each part wears on its own. Cracks spread across a part as it wears: this golem's "
                  "head is lightly worn, its arms badly, and its legs are broken.", "Wear You Can See"),
            table([
                ("Taking hits", "A random part wears with each hit."),
                ("Landing hits", "The arms wear."),
                ("Walking", "The legs wear 1 every 48 blocks."),
                ("Working", "The arms wear as it hauls, harvests or smelts."),
                ("Using power", "The core wears 1 per 100 power spent."),
                ("Weather", "Outdoors in rain or in a desert, every unsealed part wears every 30 seconds."),
            ], "What Wears Parts"),
            text(B("Broken parts") + " stay fitted but go dark and spark:\n\n- Head: sees 4 blocks.\n- Arms: hit "
                 "and work at 30%.\n- Legs: half speed.\n- Torso: half health, carries 9 stacks.\n- Core: a "
                 "quarter of its power.\n\nThe panel shows a red bar and you get a message when a part breaks.\n\n"
                 + TIP("matched sets wear 10% less, and Hardened materials ignore 30% of wear."),
                 "Broken Parts"),
        ]),
        entry("breakdown", "Breakdown", MODID + ":golem",
              [spot(MODID + ":golem", "At zero health a golem doesn't die: it " + B("breaks down") + " into a "
                    "Broken Golem item that goes to your inventory (or drops if you aren't there).\n\nNothing is "
                    "lost: parts, rules, inventory, links and name all stay in the item, though every part takes "
                    "15% extra wear in the fall. Repair it in a Garage to bring it back.", "Breaking Down"),
               text(WARN("a broken golem can't be placed until it's repaired.") + "\n\nA golem that loses a "
                    "fight far from home: walk back with its item, or keep a Garage near where it works.")]),
        entry("repairing", "Repairing", MODID + ":repair_kit", [
            craft(MODID + ":repair_kit", "Two iron ingots and a slime ball make two kits.", "Repair Kit"),
            text(B("Repair Kit") + ": right-click a golem. Every part gets back a quarter of its durability, and "
                 "it heals a quarter of its health.\n\n" + B("Repair Self") + " rule: the golem uses kits from "
                 "its own inventory.\n\n" + B("Anvil") + ": a loose part item repairs with its own material, "
                 "like a tool.\n\n" + B("Garage") + ": a full repair of everything.", "Four Ways to Repair"),
        ]),
        entry("copper_care", "Copper Care", "minecraft:honeycomb", [
            spot("minecraft:honeycomb", "Copper parts age a stage every so often (exposed, weathered, oxidized), "
                 "turning green on the golem, and get 3% slower per stage.\n\nRight-click with " + B("honeycomb")
                 + " to wax every copper part: waxed copper never ages. The panel shows \"Wax\".", "Waxing"),
            spot("minecraft:iron_axe", "Right-click with an " + B("axe") + " to scrape copper parts: it takes the wax "
                 "off first, then one stage of oxidation per use. A Garage repair polishes copper back to new and "
                 "keeps the wax.", "Scraping"),
        ]),
        entry("vines", "Vines", "minecraft:vine", [
            golem(vines, "Right-click a golem with vines to drape it like the iron golems of old. Shears trim "
                  "them off again. Purely for looks.", "Overgrown"),
        ]),
    ])


def garage_chapter(sizes):
    return category("garage", "The Garage", MODID + ":garage_controller",
                    "A repair bay you build from your own blocks. Lasers rebuild a broken golem in about two "
                    "minutes.", [
        entry("garage_intro", "What a Garage Does", MODID + ":garage_controller", [
            image("garage_side", sizes["garage_side"], "A Garage is a sealed room with a " + B("Garage "
                  "Controller") + " in one wall and " + B("Laser Emitters") + " in the ceiling. Put a broken "
                  "golem in and the lasers repair it.", "The Garage"),
            text("A repair gives back every part's durability, all health and all power, and polishes copper "
                 "to new.\n\nIt costs one unit of each damaged part's own material and takes 2 minutes (less "
                 "with set bonuses or a Steel Emitter).\n\nWhile it works, the golem rides a lift into the "
                 "middle of the room, hung from hoist chains, and turns slowly while the emitters take turns "
                 "firing crackling red, blue, pink and amber laser bolts at it. Sparks and embers spray off where "
                 "they hit, smoke drifts off its plating, and it shudders with every shot.", "Repairs"),
        ]),
        entry("garage_build", "Building One", "minecraft:stone_bricks", [
            image("garage_top", sizes["garage_top"], "Top view of a 5 x 5 Garage. The controller (left) is in "
                  "the wall; the emitters (cyan) are in the ceiling above the bay.", "Top View"),
            table([
                ("Inside", "3 to 9 blocks wide and deep, 4 to 8 blocks tall, fully enclosed."),
                ("Walls", "Planks, stone, smooth stone, stone bricks, bricks, concrete, iron or copper blocks, "
                          "deepslate bricks, any glass, Garage Windows and Garage Doors."),
                ("Floor and ceiling", "Any solid block. Glowing blocks like sea lanterns are fine. The ceiling "
                                      "can also be glass, for a skylight."),
                ("Controller", "In a side wall, facing into the room."),
                ("Emitters", "2 to 4 Laser or Steel Emitters set into the ceiling, above the room's air."),
            ], "The Rules"),
            text(B("Hold a Garage Controller") + " and a see-through plan appears: white for walls, blue-gray for "
                 "floor and ceiling, cyan for emitters, yellow for the controller, orange for the doorway, red for anything in the way. "
                 "Each block vanishes from the plan as you place it, and a checklist counts what's left.\n\n"
                 "Sneak + scroll changes the room size.", "The Build Plan"),
            text("Place the controller first: the room goes the way you're facing. Keep holding a controller "
                 "while you build and the plan follows it.\n\nRight-click the controller with an " + B("empty hand")
                 + " to check the room.\n\nBreaking a wall pauses a repair. Breaking the controller drops the "
                 "golem out.", "Checking It"),
        ]),
        entry("garage_look", "A Real Garage", MODID + ":garage_door", [
            text("When the room is complete it " + B("transforms") + ": rusty panels sweep across every wall, "
                 "floor and ceiling, starting at the controller, and your blocks disappear behind a wasteland "
                 "workshop. Corrugated metal, grimy brick and bolted plates inside, a plank roof with a hoist "
                 "beam, an oily concrete floor, and a weathered shell outside with spikes on the roof and a "
                 "smoking exhaust stack. A workbench, tyres, oil drums and jerry cans fill the corners.\n\n"
                 "It also lights itself: hidden light blocks under the roof by the lanterns keep it bright day and "
                 "night. Break any part of the room and it goes back to how you built it, lights and all.",
                 "The Transformation"),
            text("Once complete, the emitters glow brighter, a neon "
                 + B("GOLEM GARAGE") + " sign flickers on outside above the controller, the lift pad gets a "
                 "hazard-striped ring, yellow bay lines appear on the floor and lanterns hang from the roof.\n\nInside, a panel above the "
                 "controller shows READY, or the repair's progress. During a repair amber warning lights spin "
                 "inside and on the sign, and the sign counts up the percent.", "Coming to Life"),
            craft(MODID + ":garage_door", "Iron, an iron trapdoor and redstone make 4. Place several side by side "
                  "and stacked for one big roller door. It rolls up for golems and cubs, redstone or a "
                  "right-click (right-click again to let it close).", "Garage Door"),
            craft(MODID + ":garage_window", "Four glass and an iron nugget make 4. Use them, or any glass, in the "
                  "walls and roof so you can watch the repairs.", "Garage Window"),
            text("The build plan marks a 3-wide doorway in the back wall in orange: fill it with Garage Doors so "
                 "golems can come and go, or with plain wall.\n\nA door counts as wall even while it's open, "
                 "so repairs keep going when a golem walks out.", "Doorways"),
        ]),
        entry("garage_blocks", "Garage Blocks", MODID + ":laser_emitter", [
            craft(MODID + ":garage_controller", "Iron, a lever and redstone. Goes in a wall."),
            craft(MODID + ":laser_emitter", "A glass pane, two iron ingots and a redstone torch. Goes in the "
                  "ceiling."),
            craft(MODID + ":steel_emitter", "Upgrade a Laser Emitter with a steel ingot. One anywhere in the "
                  "ceiling makes repairs 20% faster."),
            craft(MODID + ":steel_ingot", "Two iron ingots and coal. Also a golem material of its own (tier 3, "
                  "tough), and counts toward the Garage kit."),
        ]),
        entry("garage_repair", "Starting a Repair", MODID + ":golem", [
            text(B("By hand") + ": right-click the controller holding the Broken Golem item.\n\n" + B("By "
                 "itself") + ": link a golem to the controller with a Golem Linker and give it a \"go to "
                 "Garage\" rule. It walks in when the rule fires. The Medic preset does this.\n\nAny golem "
                 "can be repaired, broken down or just worn.", "Two Ways In"),
            text(B("Cost") + ": one unit of each damaged part's own material: iron parts need iron ingots, gold "
                 "parts need gold, stone parts cobblestone, and so on.\n\nThe Garage takes them from, in "
                 "order:\n- your inventory\n- the golem's own inventory\n- a chest or barrel next to the controller\n\n"
                 + TIP("keep a chest of ingots by the controller, and a golem that walks in by itself never "
                 "needs you."), "Paying"),
            table([
                ("Base time", "2 minutes."),
                ("Matched set", "20% faster."),
                ("Garage kit", "20% faster."),
                ("Steel Emitter", "20% faster."),
            ], "Repair Speed"),
        ]),
    ])


def power_chapter():
    rows = []
    for sh in S.SHAPES:
        if sh["slot"] == "CORE":
            rows.append((sh["name"], ABILITY_TEXT[sh["abilities"][0]]))
    return category("power", "Power", MODID + ":redstone_core@iron",
                    "How golems store, spend and recharge power.", [
        entry("power_basics", "Power Basics", MODID + ":redstone_core@iron", [
            text("The " + B("core") + " sets how much power a golem holds. It uses a little standing still, more "
                 "while moving (1 every 10 seconds idle, 1 every 2 seconds walking), and one point per hit or task.\n\nAt " + B("zero power") + " it moves at 40% "
                 "speed and its eyes dim and flicker.", "Power"),
            table(rows + [("No core", "Never runs out of power, but is always 25% slower.")], "Recharging"),
            text(B("Recharge action") + ": walks to a placed redstone block within 12 blocks and charges next "
                 "to it, whatever its core. Put \"power below 20%: recharge\" at the top of a worker's rules.\n\n"
                 + B("Conductive") + " materials (copper, signalum, azure electrum) add capacity and charge 25% "
                 "faster.", "Charging Up"),
        ]),
    ])


def behaviors_chapter():
    trig = [
        ("always", "Always true. Use it last, as the fallback."),
        ("health below %", "Health is under the percent."),
        ("power below %", "Power is under the percent."),
        ("a part below %", "Any part's durability is under the percent."),
        ("a part is broken", "Any part is at zero durability."),
        ("owner within / beyond", "You are closer / farther than N blocks."),
        ("owner attacked", "Something hurt you in the last 10 seconds."),
        ("hostile within", "A hostile mob is within N blocks and in sight."),
        ("away from post", "More than N blocks from its post."),
        ("crop ready within", "A ripe crop is within N blocks."),
        ("chest A has / below", "Chest A holds at least / fewer than N items."),
        ("chest B full", "Chest B has no room."),
        ("inventory full / empty", "The golem's own inventory."),
        ("day / sunset / night", "The time of day."),
        ("raining / thunderstorm", "The weather."),
        ("redstone on / off", "A redstone signal at the golem or at its post."),
        ("outside its area", "It has left the work area marked with the Linker."),
    ]
    acts = [
        ("follow owner", "Walks with you."),
        ("go to post", "Walks to its post and stays."),
        ("wait N s", "Stands still. A higher rule can interrupt."),
        ("stroll", "Wanders around its post (or where it is)."),
        ("attack hostile", "Fights the nearest hostile it can see."),
        ("defend owner", "Fights whatever hurt you."),
        ("take from chest A", "Takes a stack per work cycle."),
        ("deposit to chest B", "Stores a stack per work cycle."),
        ("take from / deposit to chest", "The same with the nearest chest or barrel within 8 blocks."),
        ("pick up items", "Collects dropped items within 5 blocks (10 with Grabber Claws)."),
        ("harvest crops", "Harvests ripe crops and replants. Needs Scythe Arms or Grabber Claws."),
        ("recharge", "Walks to a redstone block and charges."),
        ("repair self", "Uses a Repair Kit from its inventory."),
        ("go to Garage", "Walks into its linked Garage for a full repair."),
        ("emit redstone", "Powers a Golem Signal Plate it stands on."),
        ("give to a golem", "Hands its items to the nearest of your golems with room, within 12 blocks."),
        ("patrol its area", "Walks between random spots inside its work area."),
        ("rest at its dock", "Walks onto its Golem Dock and rests, slowly recharging and healing."),
    ]

    def preset(eid, name, icon, rules, desc, setup, golem_snbt):
        return entry(eid, name, icon, [
            golem(golem_snbt, desc, name),
            table(rules, "Its Rules"),
            text(setup, "Setting It Up"),
        ])

    return category("behaviors", "Jobs and Rules", MODID + ":golem_linker",
                    "Teach a golem a job with WHEN / DO / UNLESS rules, or start from a preset.", [
        entry("modes", "Simple Modes", "minecraft:iron_sword", [
            text("Don't want to write rules? Click " + B("Mode") + " under the golem's picture on the Status tab:\n\n"
                 + B("Aggressive") + ": follows you and attacks every hostile mob within 12 blocks.\n"
                 + B("Peaceful") + ": follows you and never attacks anything, even if it's hit.\n"
                 + B("Guard") + ": follows you and only fights mobs that attack you.\n"
                 + B("Rules") + ": runs its rules (the default).", "Modes"),
            text("Any mode but Rules overrides the rules completely. The rules aren't lost: switch back to Rules and "
                 "they run again.\n\nYou can also use " + B("/golemgarage mode <golem> <mode>") + ".", "Modes and Rules"),
        ]),
        entry("rules_intro", "How Rules Work", "minecraft:comparator", [
            text("Open the golem's panel and go to " + B("Rules") + ". Each rule reads:\n\n" + B("WHEN")
                 + " something is true\n" + B("DO") + " an action\n" + B("UNLESS") + " something else (optional)"
                 "\n\nUp to 8 rules, checked top to bottom twice a second. The " + B("first that applies")
                 + " runs and is highlighted. If none do, it follows you.",
                 "Rules"),
            text("Each rule reads like a sentence: \"When " + B("power is below 20%") + " \u2192 " + B("recharge")
                 + "\". Click any colored part to pick from a list of choices with explanations. Click a yellow "
                 "number to raise it, right-click to lower it, or scroll.\n\nThe arrows move a rule up or down; "
                 "the x deletes it. Quick jobs at the top load a ready-made set. Undo puts back your last change."
                 "\n\nChanges save when you close the panel.", "Editing"),
            text(B("Order matters.") + " Put urgent rules first: \"power below 20%: recharge\" above the job, and "
                 "\"always\" last as the fallback.\n\n" + B("UNLESS") + " stops a rule even when its WHEN is "
                 "true. Example: \"chest A has 1 DO take from chest A UNLESS inventory full\" stops taking when "
                 "the golem can't carry more, so the next rule (deposit) runs.", "Order and Unless"),
        ]),
        entry("triggers", "Every Trigger", "minecraft:observer", [table(trig, "WHEN...")]),
        entry("actions", "Every Action", "minecraft:piston", [table(acts, "DO...")]),
        entry("stuck", "When a Golem Is Stuck", "minecraft:barrier", [
            text("If an action can't run, the panel says why, such as \"Stuck: no path to chest B\" or "
                 "\"Stuck: chest A is empty\".\n\nIf the "
                 "same rule fails " + B("five times") + " in a row, the golem stops running rules, follows you, "
                 "and shows \"Stopped: ...\". Editing its rules starts it again.", "Stuck and Stopped"),
            text("The panel's activity list shows its last few jobs, so you can see what it has been up to.\n\n"
                 "Work has a short pause built in between items, shorter with faster arms.\n\n" + TIP("a big "
                 "golem can't squeeze into a 1-wide gap. Leave 2 blocks of room around chests and crops."),
                 "Activity"),
        ]),
        preset("preset_follower", "Follower", MODID + ":golem@iron",
               [("owner attacked", "defend owner"), ("always", "follow owner")],
               "The default: stays by your side and fights back for you.",
               "Nothing to set up. Every new golem starts as a Follower.", matched("iron")),
        preset("preset_guard", "Guard", "minecraft:iron_sword",
               [("hostile within 10", "attack hostile"), ("owner attacked", "defend owner"),
                ("away from post 2", "go to post"), ("always", "wait 5 s")],
               "Holds a spot and attacks anything hostile that comes near.",
               "1. Shift + right-click the air with a Golem Linker until it says Post.\n2. Right-click the spot to "
               "guard.\n3. Right-click the golem with the Linker.\n4. Choose Guard in its Rules tab.\n\n" +
               TIP("Hammer Arms hit every enemy nearby; a Scope Head spots them early."),
               build({"HEAD": ("scope_head", "iron"), "TORSO": ("plated_chest", "iron"),
                      "ARMS": ("hammer_arms", "iron"), "LEGS": ("stub_legs", "iron"),
                      "CORE": ("redstone_core", "iron")})),
        preset("preset_hauler", "Hauler", "minecraft:chest",
               [("power below 20", "recharge"), ("inventory full", "deposit to chest B"),
                ("chest A has 1 (unless inventory full)", "take from chest A"),
                ("inventory empty", "go to post"), ("always", "deposit to chest B")],
               "Moves everything from chest A to chest B, all day.",
               "1. With the Linker in Chest A mode, right-click the chest to empty.\n2. Switch to Chest B and "
               "click the chest to fill.\n3. Optionally mark a Post to wait at.\n4. Right-click the golem with "
               "the Linker and choose Hauler.\n\n" + TIP("Grabber Claws and a Cargo Barrel (27 stacks) make "
               "the best hauler. Place a redstone block nearby for recharging."),
               build({"HEAD": ("sentry_head", "iron"), "TORSO": ("cargo_barrel", "iron"),
                      "ARMS": ("grabber_claws", "iron"), "LEGS": ("wheel_legs", "iron"),
                      "CORE": ("redstone_core", "iron")})),
        preset("preset_harvester", "Harvester", "minecraft:wheat",
               [("power below 20", "recharge"), ("inventory full", "deposit to chest"),
                ("crop ready within 10", "harvest crops"), ("always", "pick up items")],
               "Harvests and replants ripe crops, and stores them in the nearest chest.",
               "1. Fit Scythe Arms (3 x 3 per swing) or Grabber Claws.\n2. Place a chest within 8 blocks of the "
               "field.\n3. Choose Harvester.\n\nWorks with wheat, carrots, potatoes, beetroot and nether wart "
               "(replanted), and pumpkins and melons (stem kept).\n\n" + TIP("a Lantern Head lets it work "
               "through the night."),
               build({"HEAD": ("lantern_head", "iron"), "TORSO": ("cargo_barrel", "iron"),
                      "ARMS": ("scythe_arms", "iron"), "LEGS": ("strider_legs", "iron"),
                      "CORE": ("solar_core", "iron")})),
        preset("preset_medic", "Medic", MODID + ":repair_kit",
               [("a part is broken", "go to Garage"), ("health below 50", "go to Garage"),
                ("a part below 40", "repair self"), ("always", "follow owner")],
               "Looks after itself: patches up with Repair Kits and walks to the Garage when badly hurt.",
               "1. Build a Garage.\n2. Linker in Garage mode: right-click the controller, then the golem.\n3. Give "
               "it some Repair Kits and the materials its parts need.\n4. Choose Medic.\n\nCombine with other "
               "jobs: copy the top two rules into any golem's list.",
               matched("steel")),
        preset("preset_night_guard", "Night Guard", "minecraft:lantern",
               [("power below 20", "recharge"), ("hostile within 8", "attack hostile"),
                ("night", "go to post"), ("always", "follow owner")],
               "Follows you by day and stands watch at its post by night.",
               "Mark a post with the Linker (by your door, say) and choose Night guard.\n\n" + TIP("a Lantern "
               "Head lights up the area it guards, and a Soul Core keeps it healed."),
               build({"HEAD": ("lantern_head", "netherite"), "TORSO": ("plated_chest", "netherite"),
                      "ARMS": ("golem_fists", "netherite"), "LEGS": ("tread_legs", "netherite"),
                      "CORE": ("soul_core", "netherite")})),
        entry("areas", "Work Areas", MODID + ":golem_linker", [
            text("Give a golem a " + B("work area") + " and it stays inside it: it only strolls, fights, picks "
                 "up items and harvests within the area.\n\n" + B("1.") + " Shift + right-click the air with "
                 "the Linker until it says Area corner 1, and click one corner.\n" + B("2.") + " Switch to Area "
                 "corner 2 and click the opposite corner.\n" + B("3.") + " Right-click the golem.", "Marking an Area"),
            text("While you hold a Linker you can see every golem's links and areas as colored outlines, and the "
                 "marks the Linker itself is holding.\n\nUse " + B("outside its area") + " with " + B("patrol its "
                 "area") + " to bring a golem back when it wanders off.\n\n" + TIP("an area over a farm plus "
                 "\"crop ready: harvest crops\" keeps a harvester on its own field."), "Seeing Areas"),
        ]),
        entry("teamwork", "Teamwork", MODID + ":golem@gold", [
            text("Golems can pass items to each other. A golem running " + B("give to a golem") + " walks to the "
                 "nearest of " + B("your") + " golems that has room (within 12 blocks) and hands over its items, "
                 "a stack at a time.\n\nGolems that are themselves giving items are skipped, so two givers "
                 "never pass the same items back and forth.", "Passing Items"),
            table([
                ("Harvester", "\"inventory full: give to a golem\", \"crop ready: harvest crops\""),
                ("Hauler", "\"inventory full: deposit to chest B\", \"always: wait 5 s\""),
            ], "Field to Storage"),
            text("The harvester never leaves the field; the hauler with the Cargo Barrel does the walking to "
                 "storage.\n\n" + TIP("give the hauler a Storage Module for 9 more stacks."), "Why It Works"),
        ]),
        entry("groups", "Groups", MODID + ":golem_linker", [
            text("Put golems in numbered " + B("groups") + " to set them up together.\n\n" + B("1.") + " Shift + "
                 "right-click the air with the Linker until it says Group.\n" + B("2.") + " Shift + scroll to pick a "
                 "group, 1 to 9.\n" + B("3.") + " Right-click golems to add them; shift + right-click takes one out.",
                 "Making a Group"),
            text("In a grouped golem's Rules tab, " + B("Copy to group") + " saves its rules and gives a copy to every "
                 "one of your golems in that group. Teach one harvester, and the whole crew knows the job.\n\n"
                 "The group number shows in the panel, in Jade and in /golemgarage list.", "Sharing Rules"),
        ]),
        entry("any_storage", "Any Storage", "minecraft:barrel", [
            text("Chest actions work with " + B("any block that stores items") + ": chests, barrels, shulker "
                 "boxes, hoppers, and storage from other mods such as drawers, crates, Create vaults and "
                 "storage system interfaces.\n\nThe Linker's Chest A and Chest B modes accept them all, and "
                 "\"take from / deposit to chest\" finds the nearest one within 8 blocks.", "Storage Blocks"),
        ]),
        entry("recipes_jobs", "Job Ideas", "minecraft:furnace", [
            table([
                ("Auto-smelter", "Furnace Chest + Furnace Core. Rules: \"chest A has 1: take from chest A\", "
                                 "\"always: deposit to chest B\". Put ore and coal in A; ingots come out in B."),
                ("Item sweeper", "Grabber Claws. \"inventory full: deposit to chest\", \"always: pick up "
                                 "items\". Clears mob farms and quarries."),
                ("Doorbell", "\"owner within 4: emit redstone\" on a Signal Plate wired to a bell or door."),
                ("Storm battery", "Lightning Core outdoors. \"thunderstorm: go to post\" under a lightning rod."),
                ("Rain shelter", "\"raining: go to post\" with the post under a roof stops weather wear."),
            ], "Ideas"),
        ]),
    ])


def tools_chapter():
    return category("tools", "Tools and Blocks", MODID + ":golem_linker",
                    "The Golem Linker, Signal Plate, Blueprints, and other things you can use on a golem.", [
        entry("linker", "Golem Linker", MODID + ":golem_linker", [
            craft(MODID + ":golem_linker", "An amethyst shard on two sticks.", "Golem Linker"),
            text("The Linker tells golems where things are.\n\n" + B("1.") + " Shift + right-click the air to "
                 "choose a mode: Post, Chest A, Chest B, Garage, Area corners, Home or Group.\n" + B("2.") +
                 " Right-click a block to mark it. Any storage block works for A and B; Garage needs a Garage "
                 "Controller; Post and area corners mark the spot you click.\n" + B("3.") + " Right-click a golem "
                 "to give it every mark.\n\nWhile you hold it, links and areas show as outlines.", "Using It"),
            text("The panel's Status tab lists each link and has " + B("Post here") + " (sets its post where it "
                 "stands) and " + B("Clear links") + ".\n\nOne Linker can hold every mark at once, so you "
                 "can set up a hauler in one go. Shift + right-click a golem with the Linker to clear its links.",
                 "From the Panel"),
        ]),
        entry("signal_plate", "Golem Signal Plate", MODID + ":signal_plate", [
            craft(MODID + ":signal_plate", "Iron, redstone, iron in a row.", "Golem Signal Plate"),
            text("Outputs a redstone signal while a golem running " + B("emit redstone") + " stands on it.\n\n"
                 "Use it to let golems open doors, ring bells, start machines, or tell your redstone that a "
                 "job is done.\n\n" + TIP("put the plate at the golem's post and use \"go to post\" then \"emit "
                 "redstone\" rules to signal on a schedule."), "Golems and Redstone"),
        ]),
        entry("dock", "Golem Dock", MODID + ":golem_dock", [
            craft(MODID + ":golem_dock", "Copper, a redstone block and iron.", "Golem Dock"),
            text("A charging pad that gives a golem a home. Link it with the Linker in " + B("Home") + " mode, then "
                 "give the golem a " + B("rest at its dock") + " rule, for example \"night: rest at its dock\".\n\n"
                 "While it stands on the dock it slowly recharges and heals, and the pad lights up.", "Home Sweet Home"),
        ]),
        entry("paint", "Paint", "minecraft:red_dye", [
            text("Right-click a golem with any " + B("dye") + " to paint its trim: the bands, plates and "
                 "edges on every part. Use another dye to repaint.\n\nRight-click with a " + B("water bucket") +
                 " to wash the paint off. Paint is only for looks and is kept when you pick the golem up.",
                 "Painting a Golem"),
        ]),
        entry("blueprints", "Blueprints", MODID + ":blueprint", [
            craft(MODID + ":blueprint", "A book, redstone and paper.", "Blueprint"),
            text("Use an " + B("empty blueprint") + " on a golem to record its five parts (shape and material) and "
                 "its rules.\n\nUse a " + B("written blueprint") + " on any golem to rebuild it: matching parts "
                 "are taken from your inventory and fitted, the replaced parts come back to you, and the rules "
                 "are copied.", "Copying a Build"),
        ]),
    ])


def cubs_chapter():
    colors = ["%s:cub#%d" % (MODID, i) for i in range(16)]
    return category("cubs", "Cubs", MODID + ":cub#6",
                    "Tiny chibi golems that follow you and carry your things.", [
        entry("cub_basics", "Meet the Cub", MODID + ":cub#6", [
            craft(MODID + ":cub", "A slime ball, a chest and redstone. Place it with a right-click.", "Cub"),
            text("Cubs are tiny golems with big heads, big eyes and a leaf sprout on top. They follow you "
                 "everywhere, hop along behind, and can't be hurt.\n\nIf you get too far ahead, a Cub catches "
                 "up by itself.", "Little Helpers"),
            items(colors, "Right-click with any dye to recolor it. Cubs start pink.", "Sixteen Colors"),
        ]),
        entry("cub_storage", "Storage and Magnet", "minecraft:chest", [
            text(B("Storage") + ": right-click a Cub with an empty hand to open its wardrobe and 9 pocket "
                 "slots, or right-click while holding an item to hand it over.\n\n" + B("Item magnet") + ": it picks up items dropped within 3 blocks of it. "
                 "Right-click with a " + B("feather") + " to turn the magnet off or on.\n\n" + B("Pick up") + ": "
                 "shift + right-click with an empty hand. It keeps its items, color and name.", "Carrying"),
        ]),
        entry("cub_personality", "Personality", "minecraft:poppy", [
            text("Cubs have moods:\n\n- They blink and tilt their heads at you.\n- They " + B("sit") + " when you "
                 "crouch next to them.\n- Stand still for a while and they " + B("doze off") + ", little z's "
                 "floating up.\n- Come back after being away and they " + B("jump around") + " with hearts.",
                 "Personality"),
        ]),
        entry("cub_outfits", "Outfits", MODID + ":outfit_witch_hat", [
            text("Cubs can dress up. Craft an outfit piece and right-click a Cub with it, or open the Cub with an "
                 "empty hand and put pieces in its " + B("wardrobe") + ".\n\nThere are three slots: " + B("head")
                 + ", " + B("body") + " and " + B("accessory") + ". Taking a piece out gives it back.", "Dressing Up"),
            items(["%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS if o["theme"] == "cool"],
                  "Caps, headphones, shades, jackets, hoodies and backpacks.", "Cool"),
            items(["%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS if o["theme"] == "goth"],
                  "Witch hat, lace dress, spiked collar, vampire cape and bat wings.", "Goth"),
            items(["%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS if o["theme"] == "cute"],
                  "Bunny ears, bows, frog hats, flower crowns, dresses, a bee costume, wings and a scarf.", "Cute"),
            items(["%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS if o["theme"] == "fancy"],
                  "A royal crown, a space helmet, a tuxedo, a monocle and a rocket jetpack.", "Fancy"),
            text("Wear " + B("three pieces of one style") + " for a matching set. A Cub in a full set shows it "
                 "off with its own sparkles: notes for cool, witchy swirls for goth, hearts for cute and glimmers "
                 "for fancy.\n\nOutfits go back to you if the Cub grows into a golem.", "Matching Sets"),
        ] + [craft("%s:outfit/%s" % (MODID, o["id"]), "", o["name"]) for o in OUTFITS]),
        entry("cub_growing", "Growing Up", MODID + ":golem@stone", [
            text("Feed a Cub " + B("three golem parts") + ", each for a different slot. After the third it grows "
                 "into a full golem with those parts.\n\nThe new golem is yours and keeps the Cub's stored "
                 "items.\n\nThe Cub tells you its progress: \"Cub is growing: 2 / 3\".", "Growing Up"),
        ]),
    ])


def modules_chapter():
    mods = [
        ("magnet", "Pulls dropped items within 6 blocks into the golem's inventory."),
        ("lamp", "Lights up everything around the golem."),
        ("speed", "Moves 20% faster."),
        ("storage", "Carries 9 more stacks (up to 36)."),
        ("plating", "+6 armor and +10% knockback resistance."),
        ("regen", "Heals 1 health every 4 seconds."),
        ("self_repair", "Slowly mends worn parts, a little every 30 seconds."),
    ]
    return category("modules", "Upgrade Modules", MODID + ":module_magnet",
                    "Plug-in upgrades: two sockets per golem.", [
        entry("modules_intro", "Using Modules", MODID + ":module_casing", [
            craft(MODID + ":module_casing", "Iron, copper and redstone make two casings.", "Module Casing"),
            text("Every golem has " + B("two module sockets") + ". Right-click a golem with a module to install "
                 "it. A golem can't hold two of the same module.\n\nTo take one out, open the golem's panel: "
                 "the Status tab shows both sockets with an x to remove each. The module comes back to you.\n\n"
                 "Modules stay in the golem when you pick it up.", "Sockets"),
            table([(MODULE_NAMES[m], d) for m, d in mods], "Every Module"),
        ]),
    ] + [entry("module_" + m, MODULE_NAMES[m], "%s:module_%s" % (MODID, m), [
        craft("%s:module_%s" % (MODID, m), d, MODULE_NAMES[m])]) for m, d in mods])


def settings_chapter():
    return category("settings", "Settings and Mods", "minecraft:comparator",
                    "Server settings, commands, modpack support, and JEI and Jade.", [
        entry("server_settings", "Server Settings", "minecraft:command_block", [
            text("Server owners can change these in " + B("golemgarage-server.toml") + " (in the world's "
                 "serverconfig folder, or the game's config folder for defaults).", "The Settings File"),
            table([
                ("repairSeconds", "How long a Garage repair takes (120)."),
                ("costPerPart", "Material the Garage charges per damaged part (1; 0 is free)."),
                ("wearMultiplier", "Part wear (1; 0 turns wear off)."),
                ("weatherWear", "Rain and sand wear unsealed parts (on)."),
                ("copperAging", "Copper parts oxidize (on)."),
                ("powerDrainMultiplier", "How fast golems use power (1; 0 never runs out)."),
                ("maxGolemsPerPlayer", "Golems one player can have out (0 = no limit)."),
                ("maxGolemsPerChunk", "Golems allowed in one chunk (0 = no limit)."),
                ("workRangeFromOwner", "Jobs pause while the owner is offline or further than this (0 = off)."),
                ("othersCanPickUp", "Others may pick up your golems and cubs (off)."),
                ("othersCanEdit", "Others may open, edit and link your golems (off)."),
                ("playerDamage", "Whether golems fight players: NEVER, PVP (default) or ALWAYS."),
                ("killsCountAsOwner", "Golem kills drop XP and player loot as if the owner killed them (off)."),
                ("harvestNeedsMobGriefing", "Harvesting follows the mobGriefing rule (off)."),
                ("itemDespawnMinutes", "Minutes a dropped golem or cub item lasts (0 = forever)."),
            ], "Golems and Players"),
            table([
                ("disabledActions", "Jobs golems can't use, such as harvest_crops."),
                ("disabledModules", "Modules that can't be installed (or do nothing)."),
                ("disabledShapes", "Part shapes that can't be fitted."),
                ("blockedMaterials", "Materials whose parts can't be fitted."),
                ("materialStats", "Per-material multipliers: \"diamond;durability=2;speed=0.9\". Stats: health, "
                                  "durability, speed, power, work, damage."),
            ], "Features and Balance"),
            table([
                ("magnetRange", "How far a cub's item magnet reaches (3; 0 = off)."),
                ("maxCubsPerPlayer", "Cubs one player can have out (0 = no limit)."),
                ("giveManualOnJoin", "New players get this book (on)."),
                ("playerCommands", "Players can use the /golemgarage player commands (on)."),
                ("recallCooldownSeconds", "Wait between recalls (60)."),
            ], "Cubs and Commands"),
            text("Operators can always edit any golem, and skip the recall wait.", "Operators"),
        ]),
        entry("commands", "Commands", "minecraft:command_block", [
            text(B("/golemgarage") + " (or " + B("/gg") + ") looks after your golems from anywhere. Name a golem by its "
                 "name, its number in the list, or " + B("all") + ".", "Commands"),
            table([
                ("list", "Your golems: where they are, health, power and what they're doing. Click the "
                         "coordinates to copy them."),
                ("find <golem>", "Where it is, how far and which way. Loaded golems glow for 10 seconds."),
                ("recall <golem>", "Brings it to you, even from unloaded areas or another dimension."),
                ("rename <golem> <name>", "Gives it a new name."),
                ("mode <golem> <mode>", "rules, aggressive, peaceful or guard."),
                ("stop / resume <golem>", "Pauses its jobs, and starts them again."),
            ], "For Everyone"),
            table([
                ("give <players> golem <material> [preset]", "A ready-built golem. Presets: standard, guard, "
                                                              "hauler, harvester, medic, night_guard."),
                ("give <players> cub [color]", "A cub in any dye color."),
                ("repair <golems>", "Fully repairs, heals and charges them."),
                ("owner <golems> <player>", "Hands golems to another player."),
                ("count", "How many golems the server has, by owner."),
                ("purge <player>", "Removes all of a player's golems, even unloaded ones."),
                ("debug <golem>", "Its parts, links, rules and recent activity."),
            ], "For Operators"),
        ]),
        entry("pack_makers", "For Modpacks", "minecraft:writable_book", [
            text(B("Datapack materials") + ": add a file at data/<pack>/golem_material/<id>.json with an item tag, "
                 "a tier, a built-in material to borrow the look from, a tint color, optional stat multipliers and "
                 "traits. Every part shape can then be crafted from it with the usual patterns, and the Garage "
                 "repairs it with the same item.", "New Materials"),
            text(B("Item tags") + ": golemgarage:golem_parts (and golem_parts/head, torso, arms, legs, core), "
                 "modules, outfits (and outfits/cool, goth, cute, fancy), golems, cubs, garage_blocks and tools. "
                 "Use them in recipes, quests and filters.\n\nThe " + B("/golemgarage give") + " presets make "
                 "good quest rewards.", "Tags and Rewards"),
        ]),
        entry("client_settings", "Client Settings", "minecraft:spyglass", [
            table([
                ("showGaragePlan", "The see-through build plan while holding a Garage Controller."),
                ("showStatusIcons", "The little icons over golems that are stuck, stopped, out of power or "
                                    "have a broken part."),
            ], "golemgarage-client.toml"),
            text("Golem sounds are soft, fade out within 12 blocks, and get quieter when lots of golems are close "
                 "together. " + B("golemVolume") + " (0 to 100) in golemgarage-client.toml turns golem and cub sounds "
                 "down further, on top of the Friendly Creatures slider.", "Sounds"),
        ]),
        entry("jei_jade", "JEI and Jade", "minecraft:spyglass", [
            text(B("JEI") + ": every part material, golem build and cub color shows as its own item, each item "
                 "has an info page, and the Garage Controller shows " + B("Garage Repair") + " recipes: what a "
                 "worn-out golem costs to fix.\n\n" + B("Jade") + ": look at a golem to see its power, what it's "
                 "doing and its most worn part; look at a cub to see its outfit and matching set.",
                 "Optional Helpers"),
        ]),
    ])


def mods_chapter():
    groups = [("All The Ores", "All The Ores"), ("Allthemodium", "Allthemodium"), ("Silent Gear", "Silent Gear")]
    entries = []
    for mod, title in groups:
        mats = [m for m in M.CRAFTABLE if m["mod"] == mod]
        pages = [items(["%s:golem@%s" % (MODID, m["id"]) for m in mats],
                       "With %s installed, these become golem materials. Click one to read about it." % mod,
                       title, ["mat_" + m["id"] for m in mats])]
        pages.append(table([(m["name"], "Tier %d. %s" % (m["tier"], ", ".join(TRAIT_TEXT[t][0] for t in m["traits"])
                                                       or "No traits")) for m in mats], "At a Glance"))
        entries.append(entry("mod_" + mod.lower().replace(" ", "_"), title, "%s:golem@%s" % (MODID, mats[0]["id"]),
                             pages))
    entries.insert(0, entry("mods_intro", "How It Works", "minecraft:crafting_table", [
        text("Nothing extra is needed to play. When another mod adds a metal this mod knows, that metal becomes "
             "a golem material automatically: recipes, the Parts tab and this book's crafting pages all pick it "
             "up.\n\nMaterials match by ingot tag, so any mod that adds, say, silver ingots works.\n\nA golem "
             "with parts from a mod that's later removed keeps them; they act as iron until the mod returns.",
             "Optional Mods"),
    ]))
    return category("mods", "Optional Mods", MODID + ":golem@allthemodium",
                    "Extra materials from All The Ores, Allthemodium and Silent Gear.", entries)


def help_chapter():
    return category("help", "Messages and Help", "minecraft:writable_book",
                    "What the game's messages mean, and fixes for common problems.", [
        entry("messages", "Messages", "minecraft:paper", [
            table([
                ("Your golem broke down. Repair it in a Garage.", "It reached zero health."),
                ("Your golem's ... broke.", "One part reached zero durability."),
                ("The Garage needs 2 more Iron Ingot", "Bring those materials (your inventory, a chest next "
                                                       "to the controller, or the golem's inventory)."),
                ("This golem belongs to someone else.", "Only its owner (or an operator) can change it."),
                ("You already have N golems out", "The server limits golems per player. Pick one up first."),
                ("The Garage isn't built correctly.", "Right-click the controller empty-handed to see why."),
                ("Stuck: ... / Stopped: ...", "A rule can't run; the reason follows."),
                ("Cub is growing: 2 / 3", "Feed it one more part for a different slot."),
            ], "Messages"),
        ]),
        entry("faq", "Common Problems", "minecraft:compass", [
            table([
                ("It won't move", "Out of power? Check the panel. Legs broken? Repair them."),
                ("It won't harvest", "Needs Scythe Arms or Grabber Claws, and ripe crops within the trigger "
                                     "range."),
                ("It won't open the chest", "Link it with the Linker, and leave room around the chest: golems "
                                            "are almost 3 blocks tall."),
                ("It stopped doing anything", "Read the Stopped message, fix the cause, then edit or re-save its "
                                              "rules."),
                ("It got wet and worn", "Use sealed parts (Visor Head, Sealed Hold), the desert kit, or a rain "
                                        "shelter post."),
                ("The Garage won't start", "Check the room, then the materials. A golem with no damage can't be "
                                           "repaired."),
            ], "Fixes"),
        ]),
    ])


def build_index():
    idx = {
        MODID + ":golem": "first_golem", MODID + ":manual": "manual_recipe", MODID + ":cub": "cub_basics",
        MODID + ":repair_kit": "repairing", MODID + ":garage_controller": "garage_intro",
        MODID + ":laser_emitter": "garage_blocks", MODID + ":steel_emitter": "garage_blocks",
        MODID + ":garage_door": "garage_look", MODID + ":garage_window": "garage_look",
        MODID + ":golem_linker": "linker", MODID + ":signal_plate": "signal_plate", MODID + ":blueprint": "blueprints",
        MODID + ":steel_ingot": "mat_steel", MODID + ":module_casing": "modules_intro",
        MODID + ":golem_dock": "dock", "minecraft:honeycomb": "copper_care", "minecraft:vine": "vines",
        "minecraft:redstone_block": "power_basics",
    }
    for sh in S.SHAPES:
        idx["%s:%s" % (MODID, sh["id"])] = "shape_" + sh["id"]
    for m in MODULE_NAMES:
        idx["%s:module_%s" % (MODID, m)] = "module_" + m
    for o in OUTFITS:
        idx["%s:outfit_%s" % (MODID, o["id"])] = "cub_outfits"
    for mid, item in INGREDIENT_ITEM.items():
        idx[item] = "mat_" + mid
    return idx


# ------------------------------------------------------------------ pagination
# Advance widths of Minecraft's default font (glyph + 1 px spacing). Everything else is 6.
_W = {" ": 4, "!": 2, "'": 2, ",": 2, ".": 2, ":": 2, ";": 2, "i": 2, "|": 2, "l": 3, "`": 3,
      "I": 4, "[": 4, "]": 4, "t": 4, "(": 4, ")": 4, "{": 4, "}": 4, "*": 4, '"': 4,
      "f": 5, "k": 5, "<": 5, ">": 5, "@": 7, "~": 7}
PAGE_W, PAGE_H, LINE_H, TITLE_H = 128, 160, 9, 16


def _lines(text, width=PAGE_W):
    """How many lines Minecraft will wrap this text into (close estimate)."""
    total = 0
    for para in text.split("\n"):
        words = []
        bold = False
        word, ww = "", 0
        i = 0
        tokens = []
        while i < len(para):
            ch = para[i]
            if ch == "\u00a7" and i + 1 < len(para):
                code = para[i + 1]
                bold = code == "l" or (bold and code not in "r0123456789abcdef")
                i += 2
                continue
            tokens.append((ch, _W.get(ch, 6) + (1 if bold else 0)))
            i += 1
        lines, cur = 1, 0
        word_w, line_has = 0, False
        # Greedy wrap on spaces, like StringSplitter.
        for ch, w in tokens + [(" ", 4)]:
            if ch == " ":
                if line_has and cur + word_w > width:
                    lines += 1
                    cur = word_w + 4
                else:
                    cur += word_w + 4
                line_has = True
                word_w = 0
            else:
                word_w += w
        total += lines
    return total


def _content_h(page):
    t = page["type"]
    if t == "golem":
        return page.get("height", 92)
    if t == "spotlight":
        return 40
    if t == "crafting":
        return 62
    if t == "image":
        return page["height"] * min(PAGE_W, page["width"]) // page["width"]
    if t == "items":
        cols = PAGE_W // 20
        return (len(page["items"]) + cols - 1) // cols * 20
    return 0


def paginate(page, first_title=False):
    """Splits a page's text at paragraph (or, if needed, sentence) breaks so pages end cleanly."""
    if page["type"] == "table" or not page.get("text"):
        return [page]
    top = TITLE_H if (page.get("title") or first_title) else 0
    ch = _content_h(page)
    if ch:
        top += ch + 4
    caps = [(PAGE_H - top) // LINE_H]
    full = PAGE_H // LINE_H
    # Units: (text, starts_paragraph)
    units = []
    for para in page["text"].split("\n\n"):
        sentences = re.split(r"(?<=[.!?:])\s+(?=[A-Z0-9\u00a7\"])", para)
        for i, sen in enumerate(sentences):
            units.append((sen, i == 0))
    pages = [""]

    def joined(cur, unit):
        text, new_para = unit
        if not cur:
            return text
        return cur + ("\n\n" if new_para else " ") + text

    for unit in units:
        cap = caps[0] if len(pages) == 1 else full
        trial = joined(pages[-1], unit)
        if pages[-1] and _lines(trial) > cap:
            # Prefer breaking at a paragraph; a sentence break is fine when the paragraph is long.
            pages.append(unit[0])
        else:
            pages[-1] = trial
    result = [dict(page, text=pages[0])]
    for more in pages[1:]:
        result.append({"type": "text", "text": more})
    return result


def paginate_book(book):
    for cat in book["categories"]:
        for e in cat["entries"]:
            pages = []
            for i, p in enumerate(e["pages"]):
                pages.extend(paginate(p, first_title=(i == 0)))
            e["pages"] = pages


def generate(assets, version):
    sizes = manual_art.generate(assets)
    book = {
        "title": "Golem Engineer's Manual",
        "subtitle": "Version %s, by xTrip" % version,
        "categories": [getting_started(), anatomy()] + shape_entries() + [
            material_entries(), wear_chapter(), garage_chapter(sizes), power_chapter(), behaviors_chapter(),
            modules_chapter(), tools_chapter(), cubs_chapter(), mods_chapter(), settings_chapter(), help_chapter()],
        "index": build_index(),
    }
    paginate_book(book)
    out = os.path.join(assets, "manual")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "book.json"), "w") as fh:
        json.dump(book, fh, indent=1, ensure_ascii=False)
        fh.write("\n")
    n_entries = sum(len(c["entries"]) for c in book["categories"])
    n_pages = sum(len(e["pages"]) for c in book["categories"] for e in c["entries"])
    print("manual: %d chapters, %d entries, %d source pages" % (len(book["categories"]), n_entries, n_pages))
