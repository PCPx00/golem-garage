"""Writes docs/GUIDE.md. Tables of shapes and materials come straight from the spec."""
import os

import materials as M
import shapes as S

TRAITS = {
    "OXIDIZES": "Slowly oxidizes (3% slower per stage). Wax with honeycomb; scrape with an axe.",
    "CONDUCTIVE": "Each part adds 15% power capacity; recharging is 25% faster.",
    "HARDENED": "30% chance to ignore wear.",
    "FIREPROOF": "Three or more of these parts make the golem immune to fire and lava.",
    "HEAVY": "+15% knockback resistance per part.",
    "SMITE": "Arms of this material deal 50% more damage to undead.",
    "LUMINOUS": "The golem gives off light.",
    "ENDER": "Teleports to its owner when left more than 24 blocks behind while following.",
    "THORNS": "Reflects 8% of melee damage per part (up to 40%).",
    "FEATHERWEIGHT": "Legs walk on water (and lava if fireproof); no fall damage.",
}


def ing(ref):
    return ref.lstrip("#").split(":")[1].replace("_", " ")


def recipe_text(sh):
    pattern, key = sh["recipe"]
    rows = " / ".join("`%s`" % r.replace(" ", "·") for r in pattern)
    keys = ", ".join("%s = %s" % (k, ing(v)) for k, v in key.items())
    return "%s (M = material, %s)" % (rows, keys)


def stat_text(sh):
    st = sh["stats"]
    slot = sh["slot"]
    if slot == "HEAD":
        return "sight %d, health +%d" % (st["vision"], st["health"])
    if slot == "TORSO":
        return "health +%d, carries %d" % (st["health"], st["carry"])
    if slot == "ARMS":
        return "damage %d, work %d%%" % (st["attack"], round(st["work"] * 100))
    if slot == "LEGS":
        return "speed %d%%, knockback resist %d%%" % (round(st["speed"] * 100), round(st["kb"] * 100))
    return "power %d" % st["capacity"]


def generate(root, version):
    lines = []
    w = lines.append
    with open(os.path.join(os.path.dirname(__file__), "guide_intro.md")) as fh:
        w(fh.read().replace("{VERSION}", version))
    w("\n## Part shapes\n")
    w("Every shape can be made from any material. Numbers are for the shape itself; the material scales them.\n")
    for slot in S.SLOT_ORDER:
        w("\n### %s\n" % slot.title())
        w("| Shape | Stats | What it does | Recipe |")
        w("|---|---|---|---|")
        for sh in S.SHAPES:
            if sh["slot"] == slot:
                w("| %s | %s | %s | %s |" % (sh["name"], stat_text(sh), sh["desc"], recipe_text(sh)))
    w("\n## Materials\n")
    w("Materials are matched by ingot tag, so any mod that adds the ingot works. A material only appears "
      "(in recipes and the Parts tab) when some installed mod provides it.\n")
    w("| Material | Tier | Ingredient | Usually from | Special |")
    w("|---|---|---|---|---|")
    for m in M.CRAFTABLE:
        special = []
        if m["dur"] != 1.0:
            special.append("durability x%s" % m["dur"])
        if m["speed"] != 1.0:
            special.append("speed x%s" % m["speed"])
        if m["power"] != 1.0:
            special.append("power x%s" % m["power"])
        if m["work"] != 1.0:
            special.append("work x%s" % m["work"])
        special += [t.title() for t in m["traits"]]
        w("| %s | %d | `#%s` | %s | %s |" % (m["name"], m["tier"], m["tag"], m["mod"], ", ".join(special) or "-"))
    w("\n### Material traits\n")
    w("| Trait | Effect |")
    w("|---|---|")
    for k, v in TRAITS.items():
        w("| %s | %s |" % (k.title(), v))
    w("\n### How tier scales a part\n")
    w("- Durability: (60 + 40 x tier) x material durability x slot (head 0.8, torso 1.2, arms 1.0, legs 1.0, core 0.9).")
    w("- Health from a part: shape health x (1 + 0.3 x tier).")
    w("- Armor: 0.8 x tier per part.")
    w("- Arm damage: shape damage + 1.5 x tier.")
    w("- Core power: shape power x (1 + 0.25 x tier) x material power.")
    w("- Head sight: shape sight + 2 x tier.")
    with open(os.path.join(os.path.dirname(__file__), "guide_outro.md")) as fh:
        w(fh.read())
    with open(os.path.join(root, "docs", "GUIDE.md"), "w") as fh:
        fh.write("\n".join(lines) + "\n")
