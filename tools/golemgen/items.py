"""Item icons, item models, recipes, translations and tags."""
import glob
import json
import os

import materials as M
import shapes as S

MODID = "golemgarage"

VANILLA_TAGS = {"c:cobblestones", "c:ingots/copper", "c:ingots/iron", "c:ingots/gold",
                "c:gems/diamond", "c:ingots/netherite", "c:ingots/steel"}

# Cute pastel versions of the 16 dye colors, in DyeColor id order.
CUB_COLORS = [
    ("white", "#f4f1ea"), ("orange", "#ffb170"), ("magenta", "#f08ee0"), ("light_blue", "#8fd3ff"),
    ("yellow", "#ffe37a"), ("lime", "#b6f07a"), ("pink", "#f7a6c8"), ("gray", "#9a9aa6"),
    ("light_gray", "#c9c9d1"), ("cyan", "#72e0dc"), ("purple", "#b892f0"), ("blue", "#7f9bf2"),
    ("brown", "#c69a74"), ("green", "#8cc77a"), ("red", "#f28a8a"), ("black", "#5a5866"),
]

# Old fixed parts from earlier versions; their files are removed.
OLD_PARTS = ["cracked_skull", "lantern_head", "scope_eye", "sand_seal_visor", "oracle_crown",
             "rusted_barrel", "riveted_chest", "reinforced_drum", "sealed_hold", "vault_core_casing",
             "hand_claws", "hooked_grabbers", "hydraulic_press", "harvest_scythe", "heavy_hammers",
             "stubs", "piston_stilts", "treads", "wheels", "spider_legs", "redstone_battery",
             "coal_furnace_core", "lightning_core", "steady_core", "soul_core",
             "vibranium_lens", "unobtanium_eye", "allthemodium_hull", "unobtanium_vault",
             "vibranium_servos", "unobtanium_pincers", "allthemodium_stilts", "unobtanium_hover_legs",
             "unobtanium_reactor", "silent_gear_core"]


# Upgrade modules: id -> (name, description, extra ingredients besides the casing).
MODULES = {
    "magnet": ("Magnet Module", "Pulls in dropped items within 6 blocks and stores them."),
    "lamp": ("Lamp Module", "The golem lights up everything around it (light 15)."),
    "speed": ("Speed Module", "Moves 20% faster."),
    "storage": ("Storage Module", "Carries 9 more stacks (up to 36)."),
    "plating": ("Plating Module", "+6 armor and +10% knockback resistance."),
    "regen": ("Regeneration Module", "Heals 1 health every 4 seconds."),
    "self_repair": ("Self-Repair Module", "Slowly mends worn parts, a little every 30 seconds."),
}
MODULE_RECIPES = {
    "magnet": ["minecraft:compass", "minecraft:redstone"],
    "lamp": ["minecraft:glowstone"],
    "speed": ["minecraft:sugar", "minecraft:feather"],
    "storage": ["minecraft:chest"],
    "plating": ["minecraft:iron_block"],
    "regen": ["minecraft:glistering_melon_slice"],
    "self_repair": [MODID + ":repair_kit", "minecraft:diamond"],
}


def max_durability(mat, shape):
    """Must match PartShape/GolemMaterial.maxDurability in Java."""
    v = (60 + 40 * mat["tier"]) * mat["dur"] * S.SLOT_DURABILITY[shape["slot"]]
    return int(v + 0.5)


def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def ingredient(ref):
    return {"tag": ref[1:]} if ref.startswith("#") else {"item": ref}


def clean_old(assets, data):
    recipe_dir = os.path.join(data, MODID, "recipe")
    for pid in OLD_PARTS + ["golem", "cub"]:
        # Recipes and flat textures are always old; models for reused ids are rewritten below.
        paths = [os.path.join(assets, "textures/item", pid + ".png")]
        if pid not in ("golem", "cub"):
            paths.append(os.path.join(recipe_dir, pid + ".json"))
            paths.append(os.path.join(assets, "models/item", pid + ".json"))
        for path in paths:
            if os.path.exists(path):
                os.remove(path)
    for path in glob.glob(os.path.join(recipe_dir, "part", "*", "*.json")):
        os.remove(path)
    old_skin = os.path.join(assets, "textures/entity/golem.png")
    if os.path.exists(old_skin):
        os.remove(old_skin)


def generate(root, assets, data):
    clean_old(assets, data)
    craft = M.CRAFTABLE

    # ---- part icons and models ------------------------------------------------
    for sh in S.SHAPES:
        sid = sh["id"]
        overrides = []
        for i, mat in enumerate(craft, start=1):
            tex = "item/part/%s/%s" % (sid, mat["id"])
            path = os.path.join(assets, "textures", tex + ".png")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            _write_json(os.path.join(assets, "models", tex + ".json"),
                        {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:%s" % (MODID, tex)}})
            overrides.append({"predicate": {"%s:material" % MODID: round(i * 0.01, 3)},
                              "model": "%s:%s" % (MODID, tex)})
        _write_json(os.path.join(assets, "models/item", sid + ".json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "%s:item/part/%s/iron" % (MODID, sid)},
            "overrides": overrides,
        })

    # ---- golem item: tinted by torso material, cracked when broken -------------
    g_over = []
    for i, mat in enumerate(M.MATERIALS, start=1):
        for broken in (0, 1):
            tex = "item/golem/%s%s" % (mat["id"], "_broken" if broken else "")
            path = os.path.join(assets, "textures", tex + ".png")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            _write_json(os.path.join(assets, "models", tex + ".json"),
                        {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:%s" % (MODID, tex)}})
    for i, mat in enumerate(M.MATERIALS, start=1):
        g_over.append({"predicate": {"%s:material" % MODID: round(i * 0.01, 3)},
                       "model": "%s:item/golem/%s" % (MODID, mat["id"])})
    for i, mat in enumerate(M.MATERIALS, start=1):
        g_over.append({"predicate": {"%s:material" % MODID: round(i * 0.01, 3), "%s:broken" % MODID: 1},
                       "model": "%s:item/golem/%s_broken" % (MODID, mat["id"])})
    _write_json(os.path.join(assets, "models/item/golem.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "%s:item/golem/frame" % MODID},
        "overrides": g_over,
    })

    # ---- cub item: one icon per dye color ---------------------------------------
    c_over = []
    for i, (cname, chex) in enumerate(CUB_COLORS):
        tex = "item/cub/%s" % cname
        path = os.path.join(assets, "textures", tex + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        _write_json(os.path.join(assets, "models", tex + ".json"),
                    {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:%s" % (MODID, tex)}})
        c_over.append({"predicate": {"%s:color" % MODID: round((i + 1) * 0.01, 3)},
                       "model": "%s:%s" % (MODID, tex)})
    _write_json(os.path.join(assets, "models/item/cub.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "%s:item/cub/pink" % MODID},
        "overrides": c_over,
    })

    # ---- linker ---------------------------------------------------------------------
    _write_json(os.path.join(assets, "models/item/golem_linker.json"),
                {"parent": "minecraft:item/handheld", "textures": {"layer0": "%s:item/golem_linker" % MODID}})

    # ---- part recipes ----------------------------------------------------------------
    recipe_dir = os.path.join(data, MODID, "recipe", "part")
    for sh in S.SHAPES:
        pattern, extras = sh["recipe"]
        for mat in craft:
            key = {"M": {"tag": mat["tag"]}}
            for k, ref in extras.items():
                key[k] = ingredient(ref)
            recipe = {}
            if mat["tag"] not in VANILLA_TAGS:
                recipe["neoforge:conditions"] = [
                    {"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": mat["tag"]}}]
            recipe.update({
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "pattern": pattern,
                "key": key,
                "result": {
                    "id": "%s:%s" % (MODID, sh["id"]),
                    "count": 1,
                    "components": {
                        "%s:material" % MODID: mat["id"],
                        "minecraft:max_damage": max_durability(mat, sh),
                    },
                },
            })
            _write_json(os.path.join(recipe_dir, sh["id"], mat["id"] + ".json"), recipe)

    # ---- other recipes ---------------------------------------------------------------
    rdir = os.path.join(data, MODID, "recipe")
    _write_json(os.path.join(rdir, "golem.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["CCC", "CRC", "C C"],
        "key": {"C": {"tag": "c:cobblestones"}, "R": {"item": "minecraft:redstone"}},
        "result": {"id": "%s:golem" % MODID, "count": 1},
    })
    _write_json(os.path.join(rdir, "golem_linker.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["  A", " S ", "S  "],
        "key": {"A": {"item": "minecraft:amethyst_shard"}, "S": {"item": "minecraft:stick"}},
        "result": {"id": "%s:golem_linker" % MODID, "count": 1},
    })
    _write_json(os.path.join(rdir, "signal_plate.json"), {
        "type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["IRI"],
        "key": {"I": {"tag": "c:ingots/iron"}, "R": {"item": "minecraft:redstone"}},
        "result": {"id": "%s:signal_plate" % MODID, "count": 1},
    })
    # ---- upgrade modules ---------------------------------------------------------------
    _write_json(os.path.join(rdir, "module_casing.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["ICI", "CRC", "ICI"],
        "key": {"I": {"tag": "c:ingots/iron"}, "C": {"tag": "c:ingots/copper"}, "R": {"item": "minecraft:redstone"}},
        "result": {"id": "%s:module_casing" % MODID, "count": 2},
    })
    for mid, extras in MODULE_RECIPES.items():
        _write_json(os.path.join(rdir, "module_%s.json" % mid), {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": "%s:module_casing" % MODID}] + [ingredient(e) for e in extras],
            "result": {"id": "%s:module_%s" % (MODID, mid), "count": 1},
        })
        _write_json(os.path.join(assets, "models/item/module_%s.json" % mid),
                    {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/module_%s" % (MODID, mid)}})
    _write_json(os.path.join(assets, "models/item/module_casing.json"),
                {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/module_casing" % MODID}})

    _write_json(os.path.join(rdir, "manual.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"tag": "c:ingots/iron"}],
        "result": {"id": "%s:manual" % MODID, "count": 1},
    })
    _write_json(os.path.join(assets, "models/item/manual.json"),
                {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/manual" % MODID}})
    _write_json(os.path.join(rdir, "repair_kit.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:ingots/iron"}, {"tag": "c:ingots/iron"}, {"item": "minecraft:slime_ball"}],
        "result": {"id": "%s:repair_kit" % MODID, "count": 2},
    })

    # ---- tags --------------------------------------------------------------------------
    _write_json(os.path.join(data, "c/tags/item/ingots/steel.json"),
                {"replace": False, "values": ["%s:steel_ingot" % MODID]})

    # ---- translations --------------------------------------------------------------------
    lang = {
        "itemGroup.golemgarage": "Golem Garage",
        "itemGroup.golemgarage.parts": "Golem Garage: Parts",
        "block.golemgarage.garage_controller": "Garage Controller",
        "block.golemgarage.laser_emitter": "Laser Emitter",
        "block.golemgarage.steel_emitter": "Steel Emitter",
        "block.golemgarage.signal_plate": "Golem Signal Plate",
        "item.golemgarage.golem": "Golem",
        "item.golemgarage.golem.broken": "Broken Golem",
        "item.golemgarage.cub": "Cub",
        "item.golemgarage.steel_ingot": "Steel Ingot",
        "item.golemgarage.repair_kit": "Repair Kit",
        "item.golemgarage.blueprint": "Blueprint",
        "item.golemgarage.golem_linker": "Golem Linker",
        "item.golemgarage.manual": "Golem Engineer's Manual",
        "item.golemgarage.module_casing": "Module Casing",
        "screen.golemgarage.cub.pockets": "Pockets",
        "screen.golemgarage.cub.wardrobe": "Wardrobe",
        "screen.golemgarage.cub.full_set": "%s set! Sparkly.",
        "item.golemgarage.module.hint": "Right-click a golem to install. Two sockets per golem.",
        "key.categories.golemgarage": "Golem Garage",
        "key.golemgarage.pick_up": "Pick up golem or cub",
        "item.golemgarage.manual.desc": "Everything about golems. Right-click to read.",
        "item.golemgarage.part_format": "%1$s %2$s",
        "entity.golemgarage.golem": "Golem",
        "entity.golemgarage.cub": "Cub",
    }
    for mid, (mname, mdesc) in MODULES.items():
        lang["item.golemgarage.module_%s" % mid] = mname
        lang["item.golemgarage.module_%s.desc" % mid] = mdesc.replace("%", "%%")
    for sh in S.SHAPES:
        lang["item.golemgarage.%s" % sh["id"]] = sh["name"]
        lang["item.golemgarage.%s.desc" % sh["id"]] = sh["desc"].replace("%", "%%")
    for mat in M.MATERIALS:
        lang["material.golemgarage.%s" % mat["id"]] = mat["name"]
    for slot in S.SLOT_ORDER:
        lang["slot.golemgarage.%s" % slot.lower()] = slot.title()
    _write_json(os.path.join(assets, "lang/en_us.json"), lang)

    print("items: %d shapes x %d materials, %d recipes" % (
        len(S.SHAPES), len(craft), len(S.SHAPES) * len(craft)))
