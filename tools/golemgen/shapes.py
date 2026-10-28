"""
Golem part shapes: geometry, stats, abilities and recipes.

Coordinates are model pixels (1/16 block). y points down and the feet sit at y = 24,
the same convention as vanilla mob models. The front of the golem is -z, its right is -x.

Each slot has a group with a fixed pivot. Shapes are children of that group.
Arms and legs are defined for the right side; the left side is mirrored automatically.

Box fields:
  o      origin (x, y, z) relative to the part pivot
  s      size (w, h, d); sizes are whole pixels so textures stay crisp
  style  metal | dark | trim | rubber | glass | solar | soul
  deco   surface decoration painted on top (see paint.py)
  glow   True: lights up with the core color (eyes, core windows, lantern glass)

Part fields: name, pivot, rot (x, y, z radians), boxes, children.
"""

GROUPS = {
    "HEAD": [("head", (0, -7, -2))],
    "TORSO": [("body", (0, -7, 0))],
    "CORE": [("body", (0, -7, 0))],
    "ARMS": [("right_arm", (-11, -8.5, 0)), ("left_arm", (11, -8.5, 0))],
    "LEGS": [("right_leg", (-4.5, 11, 0)), ("left_leg", (4.5, 11, 0))],
}


def B(o, s, style="metal", deco=None, glow=False):
    return dict(o=o, s=s, style=style, deco=deco, glow=glow)


def P(name, boxes, pivot=(0, 0, 0), rot=(0, 0, 0), children=()):
    return dict(name=name, pivot=pivot, rot=rot, boxes=list(boxes), children=list(children))


EYES = [B((-3, -8, -5.75), (2, 1, 1), glow=True), B((1, -8, -5.75), (2, 1, 1), glow=True)]

SHAPES = [
    # =================================================================== HEAD
    dict(id="sentry_head", slot="HEAD", name="Sentry Head",
         desc="A sturdy all-round head. Good sight in every direction.",
         stats=dict(health=4, vision=16), abilities=[],
         recipe=(["MMM", "MRM", " M "], {"R": "minecraft:redstone"}),
         parts=[P("sentry_head", [
             B((-4, -12, -5.5), (8, 10, 8), deco="face"),
             B((-1, -5, -7.5), (2, 4, 2), deco="nose"),
             B((-4.5, -9.5, -6.5), (9, 1, 1), style="trim"),
             B((-4.75, -7, -2.5), (1, 2, 2), style="dark"),
             B((3.75, -7, -2.5), (1, 2, 2), style="dark"),
         ] + EYES)]),
    dict(id="scope_head", slot="HEAD", name="Scope Head",
         desc="A telescopic eye. Doubles sight range and spots hostiles early.",
         stats=dict(health=3, vision=32), abilities=["SPOTTER"],
         recipe=(["MMM", "MSM", " M "], {"S": "minecraft:spyglass"}),
         parts=[P("scope_head", [
             B((-4, -12, -5.5), (8, 10, 8), deco="face"),
             B((-1, -5, -7.5), (2, 4, 2), deco="nose"),
             B((-3.5, -10, -9.5), (3, 3, 4), style="dark"),
             B((-4, -10.5, -10.5), (4, 4, 1), style="trim", deco="ring"),
             B((-3, -9.5, -10.75), (2, 2, 1), style="glass", glow=True),
             B((1, -8, -5.75), (2, 1, 1), glow=True),
             B((2.5, -16, -0.5), (1, 4, 1), style="dark"),
             B((2, -17, -1), (2, 1, 2), glow=True),
         ])]),
    dict(id="lantern_head", slot="HEAD", name="Lantern Head",
         desc="Carries its own lantern. Lights up the ground around it as it walks.",
         stats=dict(health=4, vision=12), abilities=["LIGHT"],
         recipe=([" L ", "MMM", "MRM"], {"L": "minecraft:lantern", "R": "minecraft:redstone"}),
         parts=[P("lantern_head", [
             B((-4, -10, -5.5), (8, 8, 8), deco="face"),
             B((-1, -5, -7.5), (2, 3, 2), deco="nose"),
             B((-3, -7, -5.75), (2, 1, 1), glow=True),
             B((1, -7, -5.75), (2, 1, 1), glow=True),
             B((-3, -11, -4.5), (6, 1, 6), style="trim"),
             B((-2, -15, -3.5), (4, 4, 4), style="glass", glow=True),
             B((-3, -15, -4.5), (1, 4, 1), style="dark"),
             B((2, -15, -4.5), (1, 4, 1), style="dark"),
             B((-3, -15, 0.5), (1, 4, 1), style="dark"),
             B((2, -15, 0.5), (1, 4, 1), style="dark"),
             B((-3, -16, -4.5), (6, 1, 6), style="trim"),
             B((-1, -17, -2.5), (2, 1, 2), style="dark"),
         ])]),
    dict(id="visor_head", slot="HEAD", name="Visor Head",
         desc="A sealed helmet. Rain and desert sand can't wear it down.",
         stats=dict(health=6, vision=14), abilities=["SEALED"],
         recipe=(["MMM", "MGM", " M "], {"G": "minecraft:glass_pane"}),
         parts=[P("visor_head", [
             B((-4.5, -12.5, -6), (9, 10, 9), deco="helmet"),
             B((-1, -14, -5.5), (2, 2, 8), style="trim"),
             B((-4, -9.5, -6.5), (8, 3, 1), style="dark"),
             B((-3.5, -9, -6.75), (7, 2, 1), glow=True),
             B((-5.5, -8, -5), (1, 6, 6), style="trim", deco="rivets"),
             B((4.5, -8, -5), (1, 6, 6), style="trim", deco="rivets"),
             B((-2, -3.5, -6.5), (4, 1, 2), style="dark"),
         ])]),

    # ================================================================== TORSO
    dict(id="plated_chest", slot="TORSO", name="Plated Chest",
         desc="Heavy armor plating. The most health of any torso.",
         stats=dict(health=40, carry=9), abilities=[],
         recipe=(["M M", "MRM", "MMM"], {"R": "minecraft:redstone"}),
         parts=[P("plated_chest", [
             B((-9, -2, -6), (18, 12, 11), deco="chest_plates"),
             B((-4.5, 10, -3), (9, 5, 6), style="dark", deco="vents"),
             B((-12.5, -3.5, -4), (6, 3, 8), style="trim", deco="rivets"),
             B((6.5, -3.5, -4), (6, 3, 8), style="trim", deco="rivets"),
             B((-2.5, 1, -6.5), (5, 5, 1), style="dark"),
             B((-1.5, 2, -6.75), (3, 3, 1), glow=True),
             B((-6, 6, -6.5), (12, 3, 1), style="trim", deco="rivets"),
         ])]),
    dict(id="cargo_barrel", slot="TORSO", name="Cargo Barrel",
         desc="A banded barrel body. Carries 27 stacks.",
         stats=dict(health=26, carry=27), abilities=[],
         recipe=(["MMM", "MBM", "MMM"], {"B": "minecraft:barrel"}),
         parts=[P("cargo_barrel", [
             B((-8, -3, -7), (16, 14, 13), deco="barrel"),
             B((-8.5, -1, -7.5), (17, 2, 14), style="dark", deco="rivets"),
             B((-8.5, 7, -7.5), (17, 2, 14), style="dark", deco="rivets"),
             B((-6, -4, -5), (12, 1, 9), style="trim"),
             B((-4.5, 10, -3), (9, 5, 6), style="dark", deco="vents"),
             B((-10, -2.5, -2.5), (2, 4, 5), style="dark"),
             B((8, -2.5, -2.5), (2, 4, 5), style="dark"),
             B((-2, 2, -7.5), (4, 4, 1), style="dark"),
             B((-1, 3, -7.75), (2, 2, 1), glow=True),
             B((4, 3, -8), (2, 2, 1), style="dark"),
         ])]),
    dict(id="sealed_hold", slot="TORSO", name="Sealed Hold",
         desc="A pressurized tank with a porthole. Carries 18 stacks and keeps rain out.",
         stats=dict(health=32, carry=18), abilities=["SEALED"],
         recipe=(["MMM", "MKM", "MMM"], {"K": "minecraft:bucket"}),
         parts=[P("sealed_hold", [
             B((-8.5, -2, -6), (17, 12, 11), deco="tank"),
             B((-9, -3, -6.5), (18, 1, 12), style="trim", deco="rivets"),
             B((-9, 9, -6.5), (18, 1, 12), style="trim", deco="rivets"),
             B((-3, 0, -6.5), (6, 6, 1), style="trim", deco="ring"),
             B((-2, 1, -6.75), (4, 4, 1), style="glass", glow=True),
             B((-6, -3, 5), (3, 13, 3), style="dark", deco="pipe"),
             B((3, -3, 5), (3, 13, 3), style="dark", deco="pipe"),
             B((-7, 4, -7), (2, 2, 1), style="dark"),
             B((5, 4, -7), (2, 2, 1), style="dark"),
             B((-4.5, 10, -3), (9, 5, 6), style="dark", deco="vents"),
         ])]),
    dict(id="furnace_chest", slot="TORSO", name="Furnace Chest",
         desc="A working furnace. Smelts ores and food in its inventory using any fuel it carries.",
         stats=dict(health=30, carry=9), abilities=["SMELTER"],
         recipe=(["MMM", "MFM", "MMM"], {"F": "minecraft:furnace"}),
         parts=[P("furnace_chest", [
             B((-9, -2, -6), (18, 12, 11), deco="bricks"),
             B((-4.5, 3, -6.5), (9, 5, 1), style="dark"),
             B((-3.5, 4, -6.75), (7, 3, 1), style="dark", deco="grate", glow=True),
             B((4, -9, 1), (3, 7, 3), style="dark", deco="bricks"),
             B((3.5, -10, 0.5), (4, 1, 4), style="trim"),
             B((-4.5, 10, -3), (9, 5, 6), style="dark", deco="vents"),
             B((-11.5, -3, -3.5), (4, 2, 7), style="trim"),
             B((7.5, -3, -3.5), (4, 2, 7), style="trim"),
         ])]),

    # =================================================================== ARMS
    dict(id="golem_fists", slot="ARMS", name="Golem Fists",
         desc="Big block fists. Hit hard and knock enemies back.",
         stats=dict(health=8, attack=8, work=0.8), abilities=["KNOCKBACK"],
         recipe=(["M M", "M M", "MRM"], {"R": "minecraft:redstone"}),
         parts=[P("golem_fists", [
             B((-2.5, -2, -3.5), (5, 3, 7), style="trim", deco="rivets"),
             B((-2, -1, -3), (4, 13, 6), deco="panel"),
             B((-2.5, 12, -3.5), (5, 2, 7), style="dark"),
             B((-2, 14, -3), (4, 9, 6), deco="panel"),
             B((-3, 23, -4), (6, 6, 8), deco="knuckles"),
         ])]),
    dict(id="grabber_claws", slot="ARMS", name="Grabber Claws",
         desc="Three-fingered claws. Best at picking up items and hauling.",
         stats=dict(health=7, attack=5, work=1.2), abilities=["COLLECT", "HARVEST"],
         recipe=(["M M", "MCM", "M M"], {"C": "minecraft:chain"}),
         parts=[P("grabber_claws", [
             B((-2.5, -2, -3.5), (5, 3, 7), style="trim", deco="rivets"),
             B((-2, -1, -3), (4, 21, 6), deco="panel"),
             B((-2.5, 20, -3.5), (5, 2, 7), style="dark"),
             B((-2, 22, -3), (4, 2, 6), style="dark"),
             B((-2, 24, -3), (1, 5, 1), style="trim"),
             B((-2, 24, -0.5), (1, 5, 1), style="trim"),
             B((-2, 24, 2), (1, 5, 1), style="trim"),
             B((1, 24, -0.5), (1, 4, 1), style="trim"),
         ])]),
    dict(id="hammer_arms", slot="ARMS", name="Hammer Arms",
         desc="Heavy hammers. The strongest hits, and they strike every enemy nearby.",
         stats=dict(health=9, attack=12, work=0.7), abilities=["SWEEP", "KNOCKBACK"],
         recipe=(["MMM", "MMM", "R R"], {"R": "minecraft:redstone"}),
         parts=[P("hammer_arms", [
             B((-2.5, -2, -3.5), (5, 3, 7), style="trim", deco="rivets"),
             B((-2, -1, -3), (4, 19, 6), deco="panel"),
             B((-1, 18, -1), (2, 3, 2), style="dark"),
             B((-3.5, 21, -5), (7, 6, 10), deco="hammer"),
         ])]),
    dict(id="scythe_arms", slot="ARMS", name="Scythe Arms",
         desc="Curved blades. Harvest crops three times faster and slice through groups.",
         stats=dict(health=7, attack=9, work=1.5), abilities=["HARVEST", "SWEEP"],
         recipe=(["MMM", " RM", "  M"], {"R": "minecraft:redstone"}),
         parts=[P("scythe_arms", [
             B((-2.5, -2, -3.5), (5, 3, 7), style="trim", deco="rivets"),
             B((-2, -1, -3), (4, 21, 6), deco="panel"),
             B((-2.5, 20, -3.5), (5, 2, 7), style="dark"),
             B((-0.5, 21, -13), (1, 3, 10), style="trim", deco="blade"),
             B((-0.5, 24, -14), (1, 3, 2), style="trim", deco="blade"),
         ])]),

    # =================================================================== LEGS
    dict(id="stub_legs", slot="LEGS", name="Stub Legs",
         desc="Short, wide legs. Slow, but very hard to knock back.",
         stats=dict(health=12, speed=0.8, step=0.6, kb=0.6), abilities=["STURDY"],
         recipe=(["MRM", "M M"], {"R": "minecraft:redstone"}),
         parts=[P("stub_legs", [
             B((-3.5, -3, -3), (7, 9, 6), deco="panel"),
             B((-3, 6, -3.5), (6, 2, 7), style="dark"),
             B((-4, 8, -5), (8, 5, 9), deco="foot"),
         ])]),
    dict(id="strider_legs", slot="LEGS", name="Strider Legs",
         desc="Long piston legs. Fast, and step straight up full blocks.",
         stats=dict(health=8, speed=1.1, step=1.1, kb=0.1), abilities=["TALL_STEP"],
         recipe=(["MPM", "M M", "M M"], {"P": "minecraft:piston"}),
         parts=[P("strider_legs", [
             B((-2.5, -3, -2.5), (5, 3, 5), style="dark"),
             B((-2, 0, -2), (4, 6, 4), deco="panel"),
             B((-2.5, 6, -3), (5, 2, 5), style="trim"),
             B((-1.5, 8, -1.5), (3, 4, 3), deco="panel"),
             B((1.5, 1, 0), (1, 10, 1), style="dark"),
             B((-3, 12, -4), (6, 1, 7), style="trim"),
         ])]),
    dict(id="tread_legs", slot="LEGS", name="Tread Legs",
         desc="Tank treads. Can't be knocked back, take no fall damage, and cross sand and mud.",
         stats=dict(health=14, speed=0.92, step=0.6, kb=1.0), abilities=["TREADS"],
         recipe=(["MMM", "D D", "MMM"], {"D": "minecraft:dried_kelp_block"}),
         parts=[P("tread_legs", [
             B((-1.5, -3, -1.5), (3, 5, 3), style="dark"),
             B((-3, 2, -7), (6, 11, 14), style="rubber", deco="tread"),
             B((-3.5, 5, -2), (1, 5, 4), style="trim", deco="hub"),
             B((2.5, 5, -2), (1, 5, 4), style="trim", deco="hub"),
             B((-3.5, 1, -6), (7, 1, 12), style="trim", deco="rivets"),
         ])]),
    dict(id="wheel_legs", slot="LEGS", name="Wheel Legs",
         desc="Big rubber wheels. The fastest legs on flat ground, but they can't jump.",
         stats=dict(health=8, speed=1.35, step=0.6, kb=0.2), abilities=["NO_JUMP"],
         recipe=([" M ", "MDM", " M "], {"D": "minecraft:dried_kelp_block"}),
         parts=[P("wheel_legs", [
             B((-1, -3, -1), (2, 6, 2), deco="panel"),
             B((-2, 2, -2), (4, 2, 4), style="dark"),
         ], children=[
             P("wheel", [
                 B((-2, -5, -3), (4, 10, 6), style="rubber", deco="tire"),
                 B((-2, -3, -5), (4, 6, 10), style="rubber", deco="tire"),
                 B((-2.5, -2, -2), (5, 4, 4), style="trim", deco="hub"),
             ], pivot=(0, 8, 0)),
         ])]),
    dict(id="spider_legs", slot="LEGS", name="Spider Legs",
         desc="Four jointed legs. Climbs straight up walls.",
         stats=dict(health=10, speed=1.0, step=0.6, kb=0.3), abilities=["CLIMB"],
         recipe=(["M M", "MSM", "M M"], {"S": "minecraft:string"}),
         parts=[P("spider_legs", [
             B((-2.5, -3, -2.5), (5, 4, 5), style="dark", deco="rivets"),
         ], children=[
             P("front_leg", [B((-9, -1, -1), (9, 2, 2), deco="panel")], pivot=(-2, -1, -1.5),
               rot=(0, -0.55, 0.3), children=[
                 P("lower", [B((-1, 0, -1), (2, 17, 2), style="trim"),
                             B((-1.5, 16, -1.5), (3, 1, 3), style="dark")],
                   pivot=(-9, 0, 0), rot=(0, 0, 0)),
             ]),
             P("back_leg", [B((-9, -1, -1), (9, 2, 2), deco="panel")], pivot=(-2, -1, 1.5),
               rot=(0, 0.55, 0.3), children=[
                 P("lower", [B((-1, 0, -1), (2, 17, 2), style="trim"),
                             B((-1.5, 16, -1.5), (3, 1, 3), style="dark")],
                   pivot=(-9, 0, 0), rot=(0, 0, 0)),
             ]),
         ])]),

    # =================================================================== CORE
    dict(id="redstone_core", slot="CORE", name="Redstone Core",
         desc="A redstone battery. Recharges at redstone blocks and burns redstone dust it carries.",
         stats=dict(capacity=150), abilities=["RECHARGE_REDSTONE"], glow="#ff3b2a",
         recipe=([" M ", "MRM", " M "], {"R": "minecraft:redstone_block"}),
         parts=[P("redstone_core", [
             B((-4, 0, 5), (8, 8, 3), deco="battery"),
             B((-3, -1, 5.5), (2, 1, 2), style="dark"),
             B((1, -1, 5.5), (2, 1, 2), style="dark"),
             B((-3, 2, 7.25), (6, 1, 1), glow=True),
             B((-3, 5, 7.25), (6, 1, 1), glow=True),
         ])]),
    dict(id="furnace_core", slot="CORE", name="Furnace Core",
         desc="Burns coal, charcoal or any fuel it carries to recharge.",
         stats=dict(capacity=200), abilities=["RECHARGE_FUEL"], glow="#ff8a2a",
         recipe=([" M ", "MCM", " M "], {"C": "#minecraft:coals"}),
         parts=[P("furnace_core", [
             B((-4, 0, 5), (8, 7, 4), deco="bricks"),
             B((-2, 2, 8.25), (4, 3, 1), style="dark", deco="grate", glow=True),
             B((1.5, -5, 6), (2, 5, 2), style="dark"),
         ])]),
    dict(id="lightning_core", slot="CORE", name="Lightning Core",
         desc="Charges in rain and thunderstorms. A lightning strike fills it instantly.",
         stats=dict(capacity=250), abilities=["RECHARGE_STORM"], glow="#9fd8ff",
         recipe=([" M ", "MLM", " M "], {"L": "minecraft:lightning_rod"}),
         parts=[P("lightning_core", [
             B((-3, 1, 5), (6, 6, 3), deco="coils"),
             B((-3.5, 2, 4.75), (7, 1, 4), style="dark"),
             B((-3.5, 5, 4.75), (7, 1, 4), style="dark"),
             B((-0.5, -11, 6), (1, 12, 1), style="trim"),
             B((-1, -12, 5.5), (2, 1, 2), glow=True),
             B((-5, 2, 5.5), (2, 4, 2), style="glass", glow=True),
             B((3, 2, 5.5), (2, 4, 2), style="glass", glow=True),
         ])]),
    dict(id="solar_core", slot="CORE", name="Solar Core",
         desc="A solar panel. Recharges in daylight under open sky.",
         stats=dict(capacity=120), abilities=["RECHARGE_SUN"], glow="#ffe45a",
         recipe=([" M ", "MDM", " M "], {"D": "minecraft:daylight_detector"}),
         parts=[P("solar_core", [
             B((-1.5, -1, 5), (3, 4, 2), style="dark"),
             B((-3, 3, 5), (6, 4, 2), style="trim", deco="rivets"),
         ], children=[
             P("panel", [B((-8, -0.5, 0), (16, 1, 10), style="solar", deco="solar")],
               pivot=(0, -1, 6.5), rot=(0.6, 0, 0)),
         ])]),
    dict(id="soul_core", slot="CORE", name="Soul Core",
         desc="A bound soul flame. Slowly recharges anywhere and mends the golem over time.",
         stats=dict(capacity=220), abilities=["RECHARGE_SOUL"], glow="#45e0e8",
         recipe=([" M ", "MSM", " M "], {"S": "minecraft:soul_lantern"}),
         parts=[P("soul_core", [
             B((-3, 0, 5), (1, 7, 1), style="dark"),
             B((2, 0, 5), (1, 7, 1), style="dark"),
             B((-3, 0, 8), (1, 7, 1), style="dark"),
             B((2, 0, 8), (1, 7, 1), style="dark"),
             B((-3.5, -1, 4.5), (7, 1, 5), style="trim"),
             B((-3.5, 7, 4.5), (7, 1, 5), style="trim"),
             B((-2, 1, 5.5), (4, 5, 3), style="soul", glow=True),
         ])]),
]

# Bare frame parts, shown when a slot is empty. Rendered in the "frame" material.
FRAMES = [
    dict(id="frame_head", slot="HEAD", parts=[P("frame_head", [
        B((-2, -4, -3), (4, 3, 4), style="dark"),
        B((-2.5, -7, -4), (5, 3, 5), deco="panel"),
        B((-1, -6, -4.25), (2, 1, 1), glow=True),
    ])]),
    dict(id="frame_torso", slot="TORSO", parts=[P("frame_torso", [
        B((-2, -2, -2), (4, 17, 4), deco="panel"),
        B((-10, -2, -1.5), (20, 3, 3), style="dark", deco="rivets"),
        B((-6, 9, -1.5), (12, 2, 3), style="dark"),
        B((-3, 3, -2.5), (6, 4, 1), style="trim"),
    ])]),
    dict(id="frame_arms", slot="ARMS", parts=[P("frame_arms", [
        B((-1.5, -1, -1.5), (3, 20, 3), deco="panel"),
        B((-2, 19, -2), (4, 3, 4), style="dark"),
    ])]),
    dict(id="frame_legs", slot="LEGS", parts=[P("frame_legs", [
        B((-1.5, -3, -1.5), (3, 15, 3), deco="panel"),
        B((-2.5, 12, -3), (5, 1, 5), style="dark"),
    ])]),
]

SLOT_ORDER = ["HEAD", "TORSO", "ARMS", "LEGS", "CORE"]
SLOT_DURABILITY = {"HEAD": 0.8, "TORSO": 1.2, "ARMS": 1.0, "LEGS": 1.0, "CORE": 0.9}
BY_ID = {s["id"]: s for s in SHAPES}
