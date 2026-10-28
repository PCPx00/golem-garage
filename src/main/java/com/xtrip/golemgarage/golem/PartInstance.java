package com.xtrip.golemgarage.golem;

import com.xtrip.golemgarage.item.ModDataComponents;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.PartItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * One equipped part: a shape, a material, and its own wear.
 * The material id is kept even if the material's mod is removed, so the part survives;
 * it then works as iron until the mod comes back.
 */
public final class PartInstance {
    private final PartShape shape;
    private final String materialId;
    private final GolemMaterial material;
    private int damage;
    private int oxidation;
    private boolean waxed;

    public PartInstance(PartShape shape, String materialId, int damage, int oxidation, boolean waxed) {
        this.shape = shape;
        this.materialId = materialId;
        this.material = GolemMaterials.getOrIron(materialId);
        this.damage = Math.max(0, damage);
        this.oxidation = Math.max(0, Math.min(3, oxidation));
        this.waxed = waxed;
    }

    public PartInstance(PartShape shape, GolemMaterial material) {
        this(shape, material.id(), 0, 0, false);
    }

    public PartShape shape() {
        return shape;
    }

    public GolemMaterial material() {
        return material;
    }

    public String materialId() {
        return materialId;
    }

    /** True when the part's material comes from a mod that is no longer installed. */
    public boolean isMissingMaterial() {
        return GolemMaterials.get(materialId) == null;
    }

    public int damage() {
        return damage;
    }

    public int oxidation() {
        return oxidation;
    }

    public boolean waxed() {
        return waxed;
    }

    public int maxDurability() {
        return shape.maxDurability(material);
    }

    public int durability() {
        return Math.max(0, maxDurability() - damage);
    }

    public boolean isBroken() {
        return damage >= maxDurability();
    }

    /** Durability left, 0..1. */
    public float condition() {
        return 1.0f - Math.min(1.0f, damage / (float) maxDurability());
    }

    /** Adds wear. Returns true if this broke the part. */
    public boolean wear(int amount) {
        boolean wasBroken = isBroken();
        damage = Math.min(maxDurability(), damage + amount);
        return !wasBroken && isBroken();
    }

    public void repair(int amount) {
        damage = Math.max(0, damage - amount);
    }

    public void repairFully() {
        damage = 0;
    }

    public boolean canOxidize() {
        return material.has(GolemTrait.OXIDIZES) && !waxed && oxidation < 3;
    }

    public void oxidize() {
        if (canOxidize()) {
            oxidation++;
        }
    }

    /** Removes one stage of oxidation, or the wax first. Returns true if anything changed. */
    public boolean scrape() {
        if (waxed) {
            waxed = false;
            return true;
        }
        if (oxidation > 0) {
            oxidation--;
            return true;
        }
        return false;
    }

    /** Garage polish: removes all oxidation but keeps the wax. */
    public void polish() {
        oxidation = 0;
    }

    public boolean wax() {
        if (!material.has(GolemTrait.OXIDIZES) || waxed) {
            return false;
        }
        waxed = true;
        return true;
    }

    /** The texture material: copper shows its oxidation stage. */
    public String visualMaterialId() {
        if (material.has(GolemTrait.OXIDIZES) && oxidation > 0) {
            return GolemMaterials.COPPER_STAGES[oxidation];
        }
        return material.id();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Shape", shape.id());
        tag.putString("Material", materialId);
        tag.putInt("Damage", damage);
        if (oxidation > 0) {
            tag.putInt("Ox", oxidation);
        }
        if (waxed) {
            tag.putBoolean("Wax", true);
        }
        return tag;
    }

    /** Reads a saved part. Returns null for a shape that no longer exists. */
    public static PartInstance load(CompoundTag tag) {
        PartShape shape = PartShapes.get(tag.getString("Shape"));
        if (shape == null) {
            return null;
        }
        String mat = tag.contains("Material") ? tag.getString("Material") : "iron";
        return new PartInstance(shape, mat, tag.getInt("Damage"), tag.getInt("Ox"), tag.getBoolean("Wax"));
    }

    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModItems.partItem(shape));
        stack.set(ModDataComponents.MATERIAL.get(), materialId);
        stack.set(DataComponents.MAX_DAMAGE, maxDurability());
        if (damage > 0) {
            stack.set(DataComponents.DAMAGE, Math.min(damage, maxDurability() - 1));
        }
        if (oxidation > 0) {
            stack.set(ModDataComponents.OXIDATION.get(), oxidation);
        }
        if (waxed) {
            stack.set(ModDataComponents.WAXED.get(), true);
        }
        return stack;
    }

    /** The part an item stack represents, or null if it is not a part item. */
    public static PartInstance fromStack(ItemStack stack) {
        if (!(stack.getItem() instanceof PartItem item)) {
            return null;
        }
        String mat = stack.getOrDefault(ModDataComponents.MATERIAL.get(), "iron");
        PartInstance part = new PartInstance(item.shape(), mat, 0,
                stack.getOrDefault(ModDataComponents.OXIDATION.get(), 0),
                stack.getOrDefault(ModDataComponents.WAXED.get(), false));
        if (stack.has(DataComponents.MAX_DAMAGE) && stack.getMaxDamage() > 0) {
            // Keep the same fraction of wear even if the durability formula changed.
            float worn = stack.getDamageValue() / (float) stack.getMaxDamage();
            part.damage = Math.round(worn * part.maxDurability());
        }
        return part;
    }

    public PartInstance copy() {
        return new PartInstance(shape, materialId, damage, oxidation, waxed);
    }
}
