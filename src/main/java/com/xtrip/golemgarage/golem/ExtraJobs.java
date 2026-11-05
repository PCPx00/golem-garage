package com.xtrip.golemgarage.golem;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Golem jobs added in 0.26: fishing, beekeeping, smelting at a furnace and trading with villagers. Each finds its
 * spot within the golem's sight, walks there with {@link Mover}, and works at the golem's work speed.
 */
final class ExtraJobs {
    /** Walking: returns ARRIVED, WALKING or NO_PATH, like BehaviorGoal.approach. */
    interface Mover {
        int approach(Vec3 target, double reachSq);
    }

    static final int ARRIVED = 0, WALKING = 1, NO_PATH = 2;
    private final GolemEntity golem;
    private final Mover mover;
    private BlockPos spot;
    private int cooldown;

    ExtraJobs(GolemEntity golem, Mover mover) {
        this.golem = golem;
        this.mover = mover;
    }

    private int range() {
        return (int) Math.max(8, Math.min(16, golem.stats().vision));
    }

    private int delay(int base) {
        return Math.max(10, Math.round(base / Math.max(0.3f, golem.stats().work)));
    }

    private SimpleContainer inv() {
        return golem.getInventory();
    }

    @Nullable
    private BlockPos find(java.util.function.Predicate<BlockPos> ok) {
        BlockPos c = golem.blockPosition();
        int r = range();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            if (ok.test(p) && golem.inArea(Vec3.atCenterOf(p))) {
                double d = p.distSqr(c);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    // =================================================================== fishing
    /** Stands at the edge of water and pulls up a catch every so often, like a player with a rod. */
    String fish() {
        if (golem.isInventoryFull()) {
            return GolemText.of("fail.inventory_full");
        }
        var level = golem.level();
        if (spot == null || !level.getFluidState(spot).is(FluidTags.WATER) || golem.tickCount % 200 == 0) {
            spot = find(p -> level.getFluidState(p).is(FluidTags.WATER) && level.getBlockState(p.above()).isAir()
                    && level.getFluidState(p.below()).is(FluidTags.WATER));
        }
        if (spot == null) {
            golem.setStatus(GolemText.of("no_water"));
            golem.getNavigation().stop();
            return null;
        }
        int a = mover.approach(Vec3.atCenterOf(spot), 9.0);
        if (a == NO_PATH) {
            spot = null;
            return GolemText.of("fail.no_path_water");
        }
        if (a == WALKING) {
            golem.setStatus(GolemText.of("to_water"));
            return null;
        }
        golem.getNavigation().stop();
        golem.getLookControl().setLookAt(Vec3.atCenterOf(spot));
        golem.setStatus(GolemText.of("fishing"));
        if (level instanceof ServerLevel sl && golem.tickCount % 15 == 0) {
            sl.sendParticles(ParticleTypes.FISHING, spot.getX() + 0.5, spot.getY() + 0.9, spot.getZ() + 0.5, 2, 0.1, 0, 0.1, 0);
        }
        if (--cooldown > 0) {
            return null;
        }
        cooldown = delay(400 + golem.getRandom().nextInt(300));
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(spot))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).withLuck(0)
                .create(LootContextParamSets.FISHING);
        List<ItemStack> catchList = sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
        for (ItemStack st : catchList) {
            ItemStack rest = inv().addItem(st);
            if (!rest.isEmpty()) {
                golem.spawnAtLocation(rest);
            }
        }
        sl.sendParticles(ParticleTypes.SPLASH, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.1);
        sl.playSound(null, spot, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 1f);
        golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        golem.onWorkDone();
        return null;
    }

    // =================================================================== beekeeping
    /** Collects from full hives: honey bottles if it carries empty bottles, honeycomb if it carries shears. */
    String keepBees() {
        var level = golem.level();
        int bottles = count(Items.GLASS_BOTTLE);
        boolean shears = count(Items.SHEARS) > 0;
        if (bottles == 0 && !shears) {
            golem.getNavigation().stop();
            return GolemText.of("fail.needs_bottles");
        }
        if (golem.isInventoryFull()) {
            return GolemText.of("fail.inventory_full");
        }
        if (spot == null || !full(level.getBlockState(spot)) || golem.tickCount % 100 == 0) {
            spot = find(p -> full(level.getBlockState(p)));
        }
        if (spot == null) {
            golem.setStatus(GolemText.of("no_honey"));
            golem.getNavigation().stop();
            return null;
        }
        int a = mover.approach(Vec3.atCenterOf(spot), 6.0);
        if (a == NO_PATH) {
            spot = null;
            return GolemText.of("fail.no_path");
        }
        if (a == WALKING) {
            golem.setStatus(GolemText.of("to_hive"));
            return null;
        }
        golem.getLookControl().setLookAt(Vec3.atCenterOf(spot));
        if (--cooldown > 0) {
            return null;
        }
        cooldown = delay(30);
        BlockState s = level.getBlockState(spot);
        if (bottles > 0) {
            take(Items.GLASS_BOTTLE, 1);
            inv().addItem(new ItemStack(Items.HONEY_BOTTLE));
            level.playSound(null, spot, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1f, 1f);
        } else {
            inv().addItem(new ItemStack(Items.HONEYCOMB, 3));
            level.playSound(null, spot, SoundEvents.BEEHIVE_SHEAR, SoundSource.NEUTRAL, 1f, 1f);
        }
        // Golems smell of nothing: the bees stay calm.
        level.setBlock(spot, s.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);
        golem.setStatus(GolemText.of("beekeeping"));
        golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        golem.onWorkDone();
        spot = null;
        return null;
    }

    private static boolean full(BlockState s) {
        return s.is(BlockTags.BEEHIVES) && s.hasProperty(BeehiveBlock.HONEY_LEVEL) && s.getValue(BeehiveBlock.HONEY_LEVEL) >= 5;
    }

    // =================================================================== smelting
    /** Loads a furnace (or smoker, or blast furnace) with what it carries, feeds it fuel, and takes out results. */
    String smeltAtFurnace() {
        var level = golem.level();
        if (spot == null || !(level.getBlockEntity(spot) instanceof AbstractFurnaceBlockEntity) || golem.tickCount % 200 == 0) {
            spot = find(p -> level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity);
        }
        if (spot == null) {
            golem.setStatus(GolemText.of("no_furnace"));
            golem.getNavigation().stop();
            return null;
        }
        int a = mover.approach(Vec3.atCenterOf(spot), 6.0);
        if (a == NO_PATH) {
            spot = null;
            return GolemText.of("fail.no_path");
        }
        if (a == WALKING) {
            golem.setStatus(GolemText.of("to_furnace"));
            return null;
        }
        golem.getLookControl().setLookAt(Vec3.atCenterOf(spot));
        if (--cooldown > 0) {
            return null;
        }
        cooldown = delay(20);
        AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity) level.getBlockEntity(spot);
        boolean did = false;
        // Results out.
        ItemStack result = f.getItem(2);
        if (!result.isEmpty() && inv().canAddItem(result)) {
            inv().addItem(result.copy());
            f.setItem(2, ItemStack.EMPTY);
            did = true;
        }
        // Something to cook in.
        for (int i = 0; i < inv().getContainerSize() && !did; i++) {
            ItemStack st = inv().getItem(i);
            if (st.isEmpty() || !cookable(level, st)) {
                continue;
            }
            ItemStack in = f.getItem(0);
            if (in.isEmpty() || ItemStack.isSameItemSameComponents(in, st) && in.getCount() < in.getMaxStackSize()) {
                int n = in.isEmpty() ? st.getCount() : Math.min(st.getCount(), in.getMaxStackSize() - in.getCount());
                ItemStack put = st.split(n);
                if (in.isEmpty()) {
                    f.setItem(0, put);
                } else {
                    in.grow(put.getCount());
                }
                did = true;
            }
        }
        // Fuel.
        for (int i = 0; i < inv().getContainerSize() && f.getItem(1).getCount() < 8; i++) {
            ItemStack st = inv().getItem(i);
            if (!st.isEmpty() && st.getBurnTime(RecipeType.SMELTING) > 0 && !cookable(level, st)) {
                ItemStack fuel = f.getItem(1);
                if (fuel.isEmpty()) {
                    f.setItem(1, st.split(Math.min(st.getCount(), 16)));
                    did = true;
                } else if (ItemStack.isSameItemSameComponents(fuel, st)) {
                    int n = Math.min(st.getCount(), 16 - fuel.getCount());
                    if (n > 0) {
                        fuel.grow(n);
                        st.shrink(n);
                        did = true;
                    }
                }
            }
        }
        f.setChanged();
        if (did) {
            level.playSound(null, spot, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1f);
            golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            golem.onWorkDone();
        }
        golem.setStatus(GolemText.of("smelting_at_furnace"));
        return null;
    }

    private static boolean cookable(net.minecraft.world.level.Level level, ItemStack st) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(st), level).isPresent();
    }

    // =================================================================== trading
    /** Walks to a villager and sells it what it carries, for emeralds, as long as the villager still wants it. */
    String trade() {
        var level = golem.level();
        AbstractVillager villager = level.getEntitiesOfClass(AbstractVillager.class, golem.getBoundingBox().inflate(range()),
                        v -> v.isAlive() && !v.isBaby() && sellable(v) != null)
                .stream().min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        if (villager == null) {
            golem.setStatus(GolemText.of("no_buyer"));
            golem.getNavigation().stop();
            return null;
        }
        int a = mover.approach(villager.position(), 6.0);
        if (a == NO_PATH) {
            return GolemText.of("fail.no_path");
        }
        if (a == WALKING) {
            golem.setStatus(GolemText.of("to_villager"));
            return null;
        }
        golem.getLookControl().setLookAt(villager);
        if (--cooldown > 0) {
            return null;
        }
        cooldown = delay(40);
        MerchantOffer offer = sellable(villager);
        if (offer == null) {
            return null;
        }
        take(offer.getCostA().getItem(), offer.getCostA().getCount());
        if (!offer.getCostB().isEmpty()) {
            take(offer.getCostB().getItem(), offer.getCostB().getCount());
        }
        ItemStack paid = offer.assemble();
        ItemStack rest = inv().addItem(paid);
        if (!rest.isEmpty()) {
            golem.spawnAtLocation(rest);
        }
        offer.increaseUses();
        villager.notifyTrade(offer);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY() + 0.3, villager.getZ(), 5, 0.3, 0.2, 0.3, 0);
        }
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
        golem.setStatus(GolemText.of("trading"));
        golem.onWorkDone();
        return null;
    }

    /** An offer the golem can pay for that pays out emeralds. */
    @Nullable
    private MerchantOffer sellable(AbstractVillager v) {
        for (MerchantOffer o : v.getOffers()) {
            if (o.isOutOfStock() || !o.getResult().is(Items.EMERALD)) {
                continue;
            }
            if (count(o.getCostA().getItem()) >= o.getCostA().getCount()
                    && (o.getCostB().isEmpty() || count(o.getCostB().getItem()) >= o.getCostB().getCount())) {
                return o;
            }
        }
        return null;
    }

    // =================================================================== inventory
    private int count(net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < inv().getContainerSize(); i++) {
            if (inv().getItem(i).is(item)) {
                n += inv().getItem(i).getCount();
            }
        }
        return n;
    }

    private void take(net.minecraft.world.item.Item item, int n) {
        for (int i = 0; i < inv().getContainerSize() && n > 0; i++) {
            ItemStack st = inv().getItem(i);
            if (st.is(item)) {
                int k = Math.min(n, st.getCount());
                st.shrink(k);
                n -= k;
            }
        }
        inv().setChanged();
    }
}
