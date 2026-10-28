"""
Golem part materials. This file is the single source of truth: gen.py turns it into
Java constants, entity textures, item icons, recipes and translations.

Each material matches ingots by an item tag, so any mod that adds that ingot works.
Recipes for tag-based materials only load when the tag has at least one item.

Fields:
  id        registry-safe id
  name      display name
  tier      0 (starter) and up; drives health, durability, power and damage
  tag       ingredient tag (without '#')
  base      main color as hex
  style     metal | stone | gem
  dur       durability multiplier
  speed     speed multiplier
  power     power capacity multiplier
  work      work rate multiplier
  traits    special abilities (see Java GolemTrait)
  family    material family for set bonuses ("steel" counts toward the Garage kit)
  fx        extra surface effect: veins | sparkle | glow_veins | none
  mod       which mod usually provides it (for docs only)
  craftable False for visual-only materials (copper oxidation stages, bare frame)
"""

MATERIALS = [
    # ---- Vanilla -------------------------------------------------------------
    dict(id="stone", name="Stone", tier=0, tag="c:cobblestones", base="#8b8b8b", style="stone",
         dur=0.8, speed=0.9, power=0.8, work=0.9, traits=[], mod="Minecraft"),
    dict(id="copper", name="Copper", tier=1, tag="c:ingots/copper", base="#d0743f", style="metal",
         dur=0.9, traits=["OXIDIZES", "CONDUCTIVE"], mod="Minecraft"),
    dict(id="iron", name="Iron", tier=2, tag="c:ingots/iron", base="#cdc6bc", style="metal",
         traits=[], mod="Minecraft"),
    dict(id="gold", name="Gold", tier=2, tag="c:ingots/gold", base="#f4c73b", style="metal",
         dur=0.6, speed=1.08, power=1.5, work=1.3, traits=[], mod="Minecraft"),
    dict(id="steel", name="Steel", tier=3, tag="c:ingots/steel", base="#8e959c", style="metal",
         dur=1.3, family="steel", traits=[], mod="Golem Garage"),
    dict(id="diamond", name="Diamond", tier=4, tag="c:gems/diamond", base="#5fe2d4", style="gem",
         dur=1.5, traits=["HARDENED"], mod="Minecraft"),
    dict(id="netherite", name="Netherite", tier=5, tag="c:ingots/netherite", base="#4d4448", style="metal",
         dur=1.4, speed=0.95, traits=["FIREPROOF", "HARDENED"], fx="veins", mod="Minecraft"),

    # ---- All The Ores (and any other mod with these ingots) ------------------
    dict(id="aluminum", name="Aluminum", tier=1, tag="c:ingots/aluminum", base="#cfd6dc", style="metal",
         dur=0.8, speed=1.1, mod="All The Ores"),
    dict(id="tin", name="Tin", tier=1, tag="c:ingots/tin", base="#b9c4c9", style="metal",
         dur=0.85, mod="All The Ores"),
    dict(id="zinc", name="Zinc", tier=1, tag="c:ingots/zinc", base="#b7c2ab", style="metal",
         dur=0.85, mod="All The Ores"),
    dict(id="lead", name="Lead", tier=2, tag="c:ingots/lead", base="#636886", style="metal",
         speed=0.9, traits=["HEAVY"], mod="All The Ores"),
    dict(id="nickel", name="Nickel", tier=2, tag="c:ingots/nickel", base="#d6d1a2", style="metal",
         mod="All The Ores"),
    dict(id="silver", name="Silver", tier=2, tag="c:ingots/silver", base="#dfe9f0", style="metal",
         traits=["SMITE"], mod="All The Ores"),
    dict(id="bronze", name="Bronze", tier=2, tag="c:ingots/bronze", base="#c98a43", style="metal",
         dur=1.1, mod="All The Ores / Silent Gear"),
    dict(id="brass", name="Brass", tier=2, tag="c:ingots/brass", base="#dbb558", style="metal",
         work=1.1, mod="All The Ores"),
    dict(id="constantan", name="Constantan", tier=2, tag="c:ingots/constantan", base="#d69a69", style="metal",
         power=1.2, mod="All The Ores"),
    dict(id="electrum", name="Electrum", tier=2, tag="c:ingots/electrum", base="#e7d86a", style="metal",
         power=1.4, traits=["CONDUCTIVE"], mod="All The Ores"),
    dict(id="invar", name="Invar", tier=3, tag="c:ingots/invar", base="#a9b1a1", style="metal",
         dur=1.25, mod="All The Ores"),
    dict(id="platinum", name="Platinum", tier=3, tag="c:ingots/platinum", base="#bce4f1", style="metal",
         power=1.2, mod="All The Ores"),
    dict(id="osmium", name="Osmium", tier=3, tag="c:ingots/osmium", base="#93a8c6", style="metal",
         dur=1.2, traits=["HEAVY"], mod="All The Ores"),
    dict(id="uranium", name="Uranium", tier=3, tag="c:ingots/uranium", base="#72c14b", style="metal",
         traits=["LUMINOUS"], fx="glow_veins", mod="All The Ores"),
    dict(id="signalum", name="Signalum", tier=4, tag="c:ingots/signalum", base="#ff7d35", style="metal",
         power=1.5, traits=["CONDUCTIVE"], mod="All The Ores"),
    dict(id="lumium", name="Lumium", tier=4, tag="c:ingots/lumium", base="#f6efa3", style="metal",
         traits=["LUMINOUS"], fx="sparkle", mod="All The Ores"),
    dict(id="iridium", name="Iridium", tier=5, tag="c:ingots/iridium", base="#d7d8e8", style="metal",
         dur=1.3, traits=["HARDENED"], mod="All The Ores"),
    dict(id="enderium", name="Enderium", tier=5, tag="c:ingots/enderium", base="#2b7b78", style="metal",
         traits=["ENDER"], fx="sparkle", mod="All The Ores"),

    # ---- Allthemodium ---------------------------------------------------------
    dict(id="allthemodium", name="Allthemodium", tier=6, tag="c:ingots/allthemodium", base="#f3aa3c", style="metal",
         dur=1.3, traits=["FIREPROOF"], fx="veins", mod="Allthemodium"),
    dict(id="vibranium", name="Vibranium", tier=7, tag="c:ingots/vibranium", base="#4fe3a2", style="metal",
         dur=1.4, speed=1.1, traits=["THORNS"], fx="glow_veins", mod="Allthemodium"),
    dict(id="unobtainium", name="Unobtainium", tier=8, tag="c:ingots/unobtainium", base="#b65df3", style="metal",
         dur=1.6, speed=1.15, power=1.3, traits=["FEATHERWEIGHT"], fx="sparkle", mod="Allthemodium"),
    dict(id="vibranium_allthemodium_alloy", name="Vibranium-Allthemodium Alloy", tier=8,
         tag="c:ingots/vibranium_allthemodium_alloy", base="#a2e36b", style="metal",
         dur=1.6, speed=1.1, traits=["FIREPROOF", "THORNS"], fx="veins", mod="Allthemodium"),
    dict(id="unobtainium_allthemodium_alloy", name="Unobtainium-Allthemodium Alloy", tier=9,
         tag="c:ingots/unobtainium_allthemodium_alloy", base="#e27bd8", style="metal",
         dur=1.8, speed=1.15, power=1.4, traits=["FIREPROOF", "FEATHERWEIGHT"], fx="sparkle", mod="Allthemodium"),
    dict(id="unobtainium_vibranium_alloy", name="Unobtainium-Vibranium Alloy", tier=9,
         tag="c:ingots/unobtainium_vibranium_alloy", base="#6fa1f3", style="metal",
         dur=1.8, speed=1.2, power=1.4, traits=["THORNS", "FEATHERWEIGHT"], fx="glow_veins", mod="Allthemodium"),

    # ---- Silent Gear ------------------------------------------------------------
    dict(id="crimson_iron", name="Crimson Iron", tier=3, tag="c:ingots/crimson_iron", base="#c4383b", style="metal",
         dur=1.1, mod="Silent Gear"),
    dict(id="crimson_steel", name="Crimson Steel", tier=4, tag="c:ingots/crimson_steel", base="#9c2030", style="metal",
         dur=1.35, family="steel", traits=["FIREPROOF"], fx="veins", mod="Silent Gear"),
    dict(id="azure_silver", name="Azure Silver", tier=3, tag="c:ingots/azure_silver", base="#90b8f1", style="metal",
         traits=["SMITE"], mod="Silent Gear"),
    dict(id="azure_electrum", name="Azure Electrum", tier=4, tag="c:ingots/azure_electrum", base="#4f90f1", style="metal",
         power=1.4, traits=["CONDUCTIVE"], fx="sparkle", mod="Silent Gear"),
    dict(id="tyrian_steel", name="Tyrian Steel", tier=5, tag="c:ingots/tyrian_steel", base="#7b308f", style="metal",
         dur=1.5, family="steel", traits=["HARDENED"], fx="veins", mod="Silent Gear"),
    dict(id="blaze_gold", name="Blaze Gold", tier=3, tag="c:ingots/blaze_gold", base="#ffb43b", style="metal",
         power=1.3, work=1.2, traits=["FIREPROOF"], fx="glow_veins", mod="Silent Gear"),

    # ---- Visual only --------------------------------------------------------------
    dict(id="frame", name="Bare Frame", tier=0, tag=None, base="#55585e", style="metal", craftable=False),
    dict(id="exposed_copper", name="Exposed Copper", tier=1, tag=None, base="#a8805f", style="metal",
         craftable=False, fx="patina_light"),
    dict(id="weathered_copper", name="Weathered Copper", tier=1, tag=None, base="#6a9c7c", style="metal",
         craftable=False, fx="patina_mid"),
    dict(id="oxidized_copper", name="Oxidized Copper", tier=1, tag=None, base="#55a58c", style="metal",
         craftable=False, fx="patina_full"),
]

DEFAULTS = dict(dur=1.0, speed=1.0, power=1.0, work=1.0, traits=[], family=None, fx="none",
                mod="", craftable=True)

for _m in MATERIALS:
    for _k, _v in DEFAULTS.items():
        _m.setdefault(_k, _v)

CRAFTABLE = [m for m in MATERIALS if m["craftable"]]
BY_ID = {m["id"]: m for m in MATERIALS}
