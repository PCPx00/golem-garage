package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.GolemItem;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.PartItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** What turns up in the wasteland: worn-out golems, loose parts, scrap and the odd rig upgrade. */
public final class Salvage {
    private static final String[] COMMON = {"stone", "copper", "iron", "copper", "iron", "gold", "steel"};
    private static final String[] RARE = {"diamond", "steel", "gold", "netherite"};

    private Salvage() {
    }

    public static GolemMaterial material(RandomSource rnd, boolean rare) {
        String[] pool = rare ? RARE : COMMON;
        GolemMaterial m = GolemMaterials.get(pool[rnd.nextInt(pool.length)]);
        return m != null ? m : GolemMaterials.get("iron");
    }

    public static PartShape shape(RandomSource rnd, PartSlot slot) {
        List<PartShape> fits = PartShapes.ALL.stream().filter(s -> s.slot() == slot).toList();
        return fits.get(rnd.nextInt(fits.size()));
    }

    /** A broken golem with random parts, every part badly worn. Repair it in a Garage. */
    public static ItemStack brokenGolem(RandomSource rnd, boolean rare) {
        GolemMaterial mat = material(rnd, rare);
        Map<PartSlot, PartInstance> parts = new EnumMap<>(PartSlot.class);
        for (PartSlot slot : PartSlot.values()) {
            GolemMaterial m = rnd.nextInt(3) == 0 ? material(rnd, false) : mat;
            PartInstance p = new PartInstance(shape(rnd, slot), m);
            parts.put(slot, new PartInstance(p.shape(), p.materialId(),
                    (int) (p.maxDurability() * (0.75 + rnd.nextDouble() * 0.25)), m.id().equals("copper") ? 3 : 0, false));
        }
        CompoundTag tag = GolemData.preset(mat, "sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core");
        tag.put("Parts", GolemData.writeParts(parts));
        tag.putBoolean("Broken", true);
        return GolemItem.stackFromTag(tag);
    }

    public static ItemStack part(RandomSource rnd) {
        PartSlot slot = PartSlot.values()[rnd.nextInt(PartSlot.values().length)];
        return PartItem.create(shape(rnd, slot), material(rnd, rnd.nextInt(8) == 0));
    }

    /** A crate's worth of salvage. */
    public static List<ItemStack> crate(RandomSource rnd, boolean garage) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ModItems.SCRAP.get(), 3 + rnd.nextInt(8)));
        if (rnd.nextInt(2) == 0) {
            out.add(new ItemStack(ModItems.TYRE.get(), 1 + rnd.nextInt(3)));
        }
        for (int i = rnd.nextInt(3); i > 0; i--) {
            out.add(part(rnd));
        }
        if (garage || rnd.nextInt(3) == 0) {
            out.add(brokenGolem(rnd, rnd.nextInt(6) == 0));
        }
        if (rnd.nextInt(6) == 0) {
            out.add(new ItemStack(switch (rnd.nextInt(3)) {
                case 0 -> ModItems.RIG_RAM.get();
                case 1 -> ModItems.RIG_TURRET.get();
                default -> ModItems.RIG_PLATING.get();
            }));
        }
        if (rnd.nextInt(3) == 0) {
            out.add(new ItemStack(ModItems.RULE_CARD.get(), 1 + rnd.nextInt(2)));
        }
        if (rnd.nextInt(2) == 0) {
            out.add(new ItemStack(Items.IRON_NUGGET, 4 + rnd.nextInt(12)));
        }
        if (rnd.nextInt(3) == 0) {
            out.add(new ItemStack(Items.COAL, 2 + rnd.nextInt(8)));
        }
        if (garage && rnd.nextInt(2) == 0) {
            out.add(new ItemStack(ModItems.REPAIR_KIT.get(), 1 + rnd.nextInt(2)));
        }
        return out;
    }
}
