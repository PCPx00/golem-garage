package com.xtrip.golemgarage.golem;

import java.util.Map;

/**
 * Everything a golem's parts add up to. Recomputed whenever a part changes.
 * Missing slots behave like a weak bare frame.
 */
public final class GolemStats {
    public static final float BASE_HEALTH = 20f;
    public static final float BASE_SPEED = 0.25f;

    public float maxHealth = BASE_HEALTH;
    public float speed = BASE_SPEED;
    public float step = 0.6f;
    public float knockbackResist;
    public float armor;
    public float attack = 2f;
    public float attackKnockback;
    public float vision = 6f;
    public float work = 0.5f;
    public int carry = 9;
    public int capacity;
    public float thorns;
    public int lightLevel;
    public int glowColor = 0x707070;

    public boolean fireproof;
    public boolean climb;
    public boolean noJump;
    public boolean noFallDamage;
    public boolean waterWalk;
    public boolean smelter;
    public boolean sweep;
    public boolean collect;
    public boolean harvest;
    public boolean scytheHarvest;
    public boolean spotter;
    public boolean ender;
    public boolean smite;
    public boolean matchedSet;
    public boolean garageKit;
    public boolean desertKit;
    public boolean unpowered = true;
    public boolean magnet;
    public boolean regen;
    public boolean selfRepair;

    /** Adds the effects of installed upgrade modules. */
    public GolemStats withModules(java.util.List<GolemModule> modules) {
        for (GolemModule m : modules) {
            m.apply(this);
        }
        return this;
    }
    public ShapeAbility rechargeType;

    public static GolemStats compute(Map<PartSlot, PartInstance> parts) {
        GolemStats s = new GolemStats();
        float speedMult = 1f;
        float health = BASE_HEALTH;
        int fireproofParts = 0;
        int conductive = 0;
        int steelParts = 0;
        String firstMaterial = null;
        boolean allSame = parts.size() == PartSlot.values().length;

        for (Map.Entry<PartSlot, PartInstance> e : parts.entrySet()) {
            PartInstance part = e.getValue();
            PartShape shape = part.shape();
            GolemMaterial mat = part.material();
            boolean broken = part.isBroken();
            float hm = mat.healthMult();

            s.armor += mat.armorPerPart() * (broken ? 0.3f : 1f);
            if (mat.has(GolemTrait.FIREPROOF)) {
                fireproofParts++;
            }
            if (mat.has(GolemTrait.CONDUCTIVE)) {
                conductive++;
            }
            if (mat.has(GolemTrait.HEAVY)) {
                s.knockbackResist += 0.15f;
            }
            if (mat.has(GolemTrait.THORNS)) {
                s.thorns += 0.08f;
            }
            if (mat.has(GolemTrait.LUMINOUS)) {
                s.lightLevel = Math.max(s.lightLevel, 10);
            }
            if (mat.has(GolemTrait.ENDER)) {
                s.ender = true;
            }
            if (mat.has(GolemTrait.OXIDIZES)) {
                speedMult *= 1f - 0.03f * part.oxidation();
            }
            if ("steel".equals(mat.family())) {
                steelParts++;
            }
            if (firstMaterial == null) {
                firstMaterial = part.materialId();
            } else if (!firstMaterial.equals(part.materialId())) {
                allSame = false;
            }

            switch (e.getKey()) {
                case HEAD -> {
                    health += shape.health() * hm;
                    s.vision = broken ? 4f : shape.vision() + 2f * mat.tier();
                    s.spotter = !broken && shape.has(ShapeAbility.SPOTTER);
                    if (!broken && shape.has(ShapeAbility.LIGHT)) {
                        s.lightLevel = 14;
                    }
                }
                case TORSO -> {
                    health += shape.health() * hm * (broken ? 0.5f : 1f);
                    s.carry = broken ? 9 : Math.min(36, shape.carry() + (mat.tier() >= 5 ? 9 : 0));
                    s.smelter = !broken && shape.has(ShapeAbility.SMELTER);
                }
                case ARMS -> {
                    health += shape.health() * hm;
                    float brokenMult = broken ? 0.3f : 1f;
                    s.attack = (shape.attack() + mat.attackBonus()) * brokenMult;
                    s.work = shape.work() * (1f + 0.1f * mat.tier()) * mat.workMult() * brokenMult;
                    if (shape.has(ShapeAbility.KNOCKBACK)) {
                        s.attackKnockback += 1.0f;
                    }
                    s.sweep = !broken && shape.has(ShapeAbility.SWEEP);
                    s.collect = !broken && shape.has(ShapeAbility.COLLECT);
                    s.harvest = !broken && shape.has(ShapeAbility.HARVEST);
                    s.scytheHarvest = s.harvest && shape.has(ShapeAbility.SWEEP);
                    s.smite = mat.has(GolemTrait.SMITE);
                }
                case LEGS -> {
                    health += shape.health() * hm;
                    speedMult *= shape.speed() * mat.speedMult() * (broken ? 0.5f : 1f);
                    s.step = broken ? 0.6f : shape.step();
                    s.knockbackResist += shape.knockbackResist();
                    s.climb = !broken && shape.has(ShapeAbility.CLIMB);
                    s.noJump = shape.has(ShapeAbility.NO_JUMP);
                    s.noFallDamage = shape.has(ShapeAbility.TREADS) || mat.has(GolemTrait.FEATHERWEIGHT);
                    s.waterWalk = !broken && mat.has(GolemTrait.FEATHERWEIGHT);
                }
                case CORE -> {
                    s.unpowered = false;
                    s.capacity = Math.round(shape.capacity() * mat.capacityMult() * (broken ? 0.25f : 1f));
                    s.glowColor = broken ? 0x5a3a30 : shape.glowColor();
                    for (ShapeAbility a : shape.abilities()) {
                        if (a.name().startsWith("RECHARGE_")) {
                            s.rechargeType = a;
                        }
                    }
                }
            }
        }

        if (!parts.containsKey(PartSlot.LEGS)) {
            speedMult *= 0.7f;
        }
        if (s.unpowered) {
            speedMult *= 0.75f;
        }
        s.capacity = Math.round(s.capacity * (1f + 0.15f * conductive));
        s.fireproof = fireproofParts >= 3;
        s.matchedSet = allSame;
        if (allSame) {
            speedMult *= 1.1f;
        }
        s.garageKit = steelParts >= 3;
        s.desertKit = has(parts, PartSlot.HEAD, "visor_head") && has(parts, PartSlot.TORSO, "sealed_hold")
                && has(parts, PartSlot.LEGS, "tread_legs");
        s.knockbackResist = Math.min(1f, s.knockbackResist);
        s.thorns = Math.min(0.4f, s.thorns);
        s.maxHealth = health;
        s.speed = BASE_SPEED * speedMult;
        return s;
    }

    private static boolean has(Map<PartSlot, PartInstance> parts, PartSlot slot, String shape) {
        PartInstance p = parts.get(slot);
        return p != null && p.shape().id().equals(shape);
    }

    /** True if this slot's part keeps out rain and sand. */
    public boolean isSealed(PartSlot slot, PartInstance part) {
        return desertKit || part.shape().has(ShapeAbility.SEALED) || slot == PartSlot.CORE;
    }
}
