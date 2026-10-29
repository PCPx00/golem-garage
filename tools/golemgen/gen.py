"""
Generates everything derived from materials.py and shapes.py:

  Java:      client/model/GolemGeometry.java, golem/GeneratedMaterials.java, golem/GeneratedShapes.java
  Textures:  textures/entity/golem/<material>.png, glow.png, cracks_1..3.png, vines.png
  Items:     part icons per material, item models with material overrides
  Data:      recipes, lang, tags

Run from anywhere:  python3 tools/golemgen/gen.py
"""
import json
import os
import random
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import materials as M  # noqa: E402
import paint  # noqa: E402
import shapes as S  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src/main/java/com/xtrip/golemgarage")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/golemgarage")
DATA = os.path.join(ROOT, "src/main/resources/data")
MODID = "golemgarage"
ATLAS_W = 256


# =============================================================== model tree
def mirror_part(p):
    """Mirror a right-side part to the left: flip x, flip y/z rotation."""
    boxes = []
    for b in p["boxes"]:
        nb = dict(b)
        x, y, z = b["o"]
        w = b["s"][0]
        nb["o"] = (-(x + w), y, z)
        nb["mirror"] = True
        nb["src"] = b
        boxes.append(nb)
    px, py, pz = p["pivot"]
    rx, ry, rz = p["rot"]
    return dict(name=p["name"], pivot=(-px, py, pz), rot=(rx, -ry, -rz), boxes=boxes,
                children=[mirror_part(c) for c in p["children"]])


def build_tree():
    """Returns {group_name: (pivot, [(shape_id, part), ...])} and the list of all source boxes."""
    groups = {}
    for slot, glist in S.GROUPS.items():
        for gname, pivot in glist:
            groups.setdefault(gname, (pivot, []))

    all_shapes = [(sh["id"], sh["slot"], sh["parts"]) for sh in S.SHAPES] + \
                 [(fr["id"], fr["slot"], fr["parts"]) for fr in S.FRAMES]
    for sid, slot, parts in all_shapes:
        glist = S.GROUPS[slot]
        for i, (gname, _) in enumerate(glist):
            for p in parts:
                part = p if i == 0 else mirror_part(p)
                groups[gname][1].append((sid, part))
    return groups


def iter_boxes(part, out):
    for b in part["boxes"]:
        out.append(b)
    for ch in part["children"]:
        iter_boxes(ch, out)


# =============================================================== UV packing
def pack(groups):
    boxes = []
    for gname, (_, items) in groups.items():
        for sid, part in items:
            tmp = []
            iter_boxes(part, tmp)
            for b in tmp:
                if not b.get("mirror"):
                    b["shape"] = sid
                    boxes.append(b)
    for b in boxes:
        w, h, d = b["s"]
        b["region"] = (2 * (d + w), d + h)
    order = sorted(boxes, key=lambda b: (-b["region"][1], -b["region"][0]))
    x = y = shelf_h = 0
    for b in order:
        rw, rh = b["region"]
        if x + rw > ATLAS_W:
            x = 0
            y += shelf_h
            shelf_h = 0
        b["uv"] = (x, y)
        x += rw
        shelf_h = max(shelf_h, rh)
    used_h = y + shelf_h
    atlas_h = 64
    while atlas_h < used_h:
        atlas_h *= 2
    return boxes, atlas_h


def uv_of(b):
    return b["src"]["uv"] if b.get("mirror") else b["uv"]


# =============================================================== textures
def box_seed(b):
    key = "%s|%s|%s" % (b.get("shape"), b["o"], b["s"])
    return zlib.crc32(key.encode())


def paint_material_atlas(boxes, atlas_h, mat):
    c = paint.Canvas(ATLAS_W, atlas_h)
    pal = paint.ramp(mat["base"])
    for b in boxes:
        u, v = b["uv"]
        w, h, d = b["s"]
        faces = paint.box_faces(u, v, w, h, d)
        rng = random.Random(box_seed(b))
        for fname, rect in faces.items():
            if b["glow"] and b["style"] not in ("glass", "soul"):
                x0, y0, x1, y1 = rect
                for xx in range(x0, x1):
                    for yy in range(y0, y1):
                        c.set(xx, yy, paint.GLOW_BASE)
                if b.get("deco") == "grate":
                    paint.deco(c, "grate", fname, rect, pal, rng, b)
                continue
            paint.fill_face(c, rect, pal, b["style"], mat, fname, rng)
            if b["style"] in ("metal", "dark", "trim"):
                paint.material_fx(c, rect, pal, mat, rng)
            if b.get("deco"):
                dpal = pal if b["style"] in ("metal", "dark", "trim") else {
                    "rubber": paint.RUBBER, "glass": paint.GLASS, "solar": paint.SOLAR,
                    "soul": paint.SOUL}[b["style"]]
                paint.deco(c, b["deco"], fname, rect, dpal, rng, b)
    return c.img


def paint_overlay(boxes, atlas_h, kind, level=0):
    c = paint.Canvas(ATLAS_W, atlas_h)
    for b in boxes:
        u, v = b["uv"]
        w, h, d = b["s"]
        faces = paint.box_faces(u, v, w, h, d)
        rng = random.Random(box_seed(b) + level * 7919 + (1 if kind == "vines" else 0))
        for fname, rect in faces.items():
            if kind == "glow":
                if b["glow"]:
                    paint.glow_face(c, rect, b.get("deco"))
            elif kind == "cracks":
                if not b["glow"] and b["style"] != "rubber":
                    paint.crack_face(c, rect, level, rng)
            elif kind == "paint":
                if b["style"] == "trim" and not b["glow"]:
                    paint.fill_face(c, rect, paint.ramp("#ececec"), "trim", M.BY_ID["iron"], fname, rng)
            elif kind == "vines":
                if not b["glow"] and b["style"] in ("metal", "trim") and b["shape"].split("_")[0] != "frame":
                    paint.vine_face(c, fname, rect, rng)
    return c.img


def write_textures(boxes, atlas_h):
    out = os.path.join(ASSETS, "textures/entity/golem")
    os.makedirs(out, exist_ok=True)
    for mat in M.MATERIALS:
        paint_material_atlas(boxes, atlas_h, mat).save(os.path.join(out, mat["id"] + ".png"))
    paint_overlay(boxes, atlas_h, "glow").save(os.path.join(out, "glow.png"))
    for lvl in (1, 2, 3):
        paint_overlay(boxes, atlas_h, "cracks", lvl).save(os.path.join(out, "cracks_%d.png" % lvl))
    paint_overlay(boxes, atlas_h, "vines").save(os.path.join(out, "vines.png"))
    paint_overlay(boxes, atlas_h, "paint").save(os.path.join(out, "paint.png"))


# =============================================================== Java
def f(x):
    s = ("%.4f" % x).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def java_part(lines, parent_var, part, counter, indent="        "):
    var = "p%d" % counter[0]
    counter[0] += 1
    builder = "CubeListBuilder.create()"
    mirrored = any(b.get("mirror") for b in part["boxes"])
    if mirrored:
        builder += ".mirror()"
    for b in part["boxes"]:
        u, v = uv_of(b)
        x, y, z = b["o"]
        w, h, d = b["s"]
        builder += "\n%s        .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (
            indent, u, v, f(x), f(y), f(z), f(w), f(h), f(d))
    px, py, pz = part["pivot"]
    rx, ry, rz = part["rot"]
    pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (f(px), f(py), f(pz), f(rx), f(ry), f(rz))
    lines.append('%sPartDefinition %s = %s.addOrReplaceChild("%s", %s,\n%s        %s);' % (
        indent, var, parent_var, part["name"], builder, indent, pose))
    for ch in part["children"]:
        java_part(lines, var, ch, counter, indent)


def write_geometry_java(groups, atlas_h):
    lines = []
    counter = [0]
    shape_paths = {}
    for gname, (pivot, items) in groups.items():
        gvar = "g_" + gname
        lines.append('        PartDefinition %s = root.addOrReplaceChild("%s", CubeListBuilder.create(), '
                     'PartPose.offset(%s, %s, %s));' % (gvar, gname, f(pivot[0]), f(pivot[1]), f(pivot[2])))
        for sid, part in items:
            java_part(lines, gvar, part, counter)
            shape_paths.setdefault(sid, []).append((gname, part["name"]))

    path_lines = []
    for sid, paths in shape_paths.items():
        arr = ", ".join('new String[]{"%s", "%s"}' % p for p in paths)
        path_lines.append('        m.put("%s", List.<String[]>of(%s));' % (sid, arr))

    src = """package com.xtrip.golemgarage.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Golem geometry for every part shape.
 * GENERATED by tools/golemgen/gen.py from shapes.py. Edit the generator, not this file.
 */
public final class GolemGeometry {
    public static final int TEX_W = %d;
    public static final int TEX_H = %d;

    /** Shape id to the paths of its top-level parts: {group, part name}. */
    public static final Map<String, List<String[]>> SHAPE_PARTS = buildPaths();

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    private static Map<String, List<String[]>> buildPaths() {
        Map<String, List<String[]>> m = new HashMap<>();
%s
        return m;
    }

    private GolemGeometry() {
    }
}
""" % (ATLAS_W, atlas_h, "\n".join(lines), "\n".join(path_lines))
    out = os.path.join(JAVA, "client/model")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "GolemGeometry.java"), "w") as fh:
        fh.write(src)


def jstr(s):
    return "null" if s is None else '"%s"' % s


def write_data_java():
    mat_lines = []
    for m in M.MATERIALS:
        traits = ", ".join("GolemTrait." + t for t in m["traits"])
        mat_lines.append(
            '            new GolemMaterial("%s", %d, %s, 0x%s, %sF, %sF, %sF, %sF, Set.of(%s), %s, %s)' % (
                m["id"], m["tier"], jstr(m["tag"]), m["base"].lstrip("#").upper(),
                m["dur"], m["speed"], m["power"], m["work"], traits, jstr(m["family"]),
                "true" if m["craftable"] else "false"))
    src = """package com.xtrip.golemgarage.golem;

import java.util.List;
import java.util.Set;

/**
 * All golem materials.
 * GENERATED by tools/golemgen/gen.py from materials.py. Edit the generator, not this file.
 */
final class GeneratedMaterials {
    static final List<GolemMaterial> ALL = List.of(
%s);

    private GeneratedMaterials() {
    }
}
""" % ",\n".join(mat_lines)
    with open(os.path.join(JAVA, "golem/GeneratedMaterials.java"), "w") as fh:
        fh.write(src)

    sh_lines = []
    for sh in S.SHAPES:
        st = sh["stats"]
        abil = ", ".join("ShapeAbility." + a for a in sh["abilities"])
        glow = sh.get("glow")
        sh_lines.append(
            '            new PartShape("%s", PartSlot.%s, %sF, %sF, %d, %sF, %sF, %sF, %sF, %sF, %d, %sF, Set.of(%s), 0x%s)' % (
                sh["id"], sh["slot"], st.get("health", 0), st.get("vision", 0), st.get("carry", 0),
                st.get("attack", 0), st.get("work", 0), st.get("speed", 0), st.get("step", 0.6),
                st.get("kb", 0), st.get("capacity", 0), S.SLOT_DURABILITY[sh["slot"]], abil,
                (glow or "#000000").lstrip("#").upper()))
    src = """package com.xtrip.golemgarage.golem;

import java.util.List;
import java.util.Set;

/**
 * All golem part shapes.
 * GENERATED by tools/golemgen/gen.py from shapes.py. Edit the generator, not this file.
 */
final class GeneratedShapes {
    static final List<PartShape> ALL = List.of(
%s);

    private GeneratedShapes() {
    }
}
""" % ",\n".join(sh_lines)
    with open(os.path.join(JAVA, "golem/GeneratedShapes.java"), "w") as fh:
        fh.write(src)


def main():
    groups = build_tree()
    boxes, atlas_h = pack(groups)
    print("boxes:", len(boxes), "atlas:", ATLAS_W, "x", atlas_h)
    write_textures(boxes, atlas_h)
    write_geometry_java(groups, atlas_h)
    write_data_java()
    import items  # noqa: E402  (icons, models, recipes, lang)
    items.generate(ROOT, ASSETS, DATA)
    import blocks  # noqa: E402
    blocks.generate(ASSETS)
    import skin  # noqa: E402
    skin.generate(ASSETS)
    import cub  # noqa: E402
    cub.generate(JAVA, ASSETS)
    import outfits  # noqa: E402
    outfits.generate(JAVA, ASSETS, DATA)
    lang_path = os.path.join(ASSETS, "lang/en_us.json")
    with open(lang_path) as fh:
        lang = json.load(fh)
    outfits.lang(lang)
    import sounds  # noqa: E402
    sounds.generate(ASSETS, lang)
    import advancements  # noqa: E402
    advancements.generate(DATA, lang)
    # Hand-written UI text (screens, chat, statuses, JEI/Jade), kept in one file for translators.
    with open(os.path.join(HERE, "strings_en.json")) as fh:
        lang.update(json.load(fh))
    with open(lang_path, "w") as fh:
        json.dump(lang, fh, indent=2)
        fh.write("\n")
    import guide  # noqa: E402
    version = "?"
    with open(os.path.join(ROOT, "gradle.properties")) as fh:
        for line in fh:
            if line.startswith("mod_version="):
                version = line.strip().split("=", 1)[1]
    guide.generate(ROOT, version)
    import manual  # noqa: E402
    manual.generate(ASSETS, version)
    import itemart  # noqa: E402  (last: needs every entity texture)
    itemart.generate(sys.modules[__name__], ASSETS)
    outfits.render_icons(ASSETS)


if __name__ == "__main__":
    main()
