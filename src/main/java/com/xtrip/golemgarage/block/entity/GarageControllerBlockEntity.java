package com.xtrip.golemgarage.block.entity;

import com.xtrip.golemgarage.block.GarageBay;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemStats;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.GolemItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds one golem during repair. The golem is lifted on a platform in the middle of the bay,
 * the emitters lock on and sweep it with beams, then it is lowered and walks out repaired.
 *
 * Cost: one unit of each damaged part's own material (iron parts need iron ingots, gold parts
 * gold ingots, and so on), taken from the player, a chest next to the controller, or the golem.
 */
public class GarageControllerBlockEntity extends BlockEntity {
    public static final int REPAIR_TICKS = 2400;
    public static final int LIFT_TICKS = 50;

    private CompoundTag repairData;
    private int progress;
    private int required = REPAIR_TICKS;
    private BlockPos bayCenter;
    private final List<BlockPos> emitters = new ArrayList<>();

    /** Whether the room around the controller is a complete Garage (checked every 2 seconds, synced). */
    private boolean formed;
    private BlockPos bayMin;
    private BlockPos bayMax;
    private final List<BlockPos> litEmitters = new ArrayList<>();
    /** Hidden light blocks this Garage placed under its roof, by the lanterns. */
    private final List<BlockPos> lights = new ArrayList<>();

    /** Client only: the golem model shown during repair. */
    private GolemEntity displayGolem;
    /** Client only: when the Garage was last seen to become complete, for the sign's switch-on flicker. */
    private long formedAt = Long.MIN_VALUE / 2;

    public GarageControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGE_CONTROLLER.get(), pos, state);
    }

    public boolean isBusy() {
        return repairData != null;
    }

    public int getProgress() {
        return progress;
    }

    public int getRequired() {
        return required;
    }

    @Nullable
    public BlockPos getBayCenter() {
        return bayCenter;
    }

    public List<BlockPos> getEmitters() {
        return emitters;
    }

    public boolean isFormed() {
        return formed;
    }

    @Nullable
    public BlockPos getBayMin() {
        return bayMin;
    }

    @Nullable
    public BlockPos getBayMax() {
        return bayMax;
    }

    /** Client: game ticks since the Garage became complete. */
    public long formedFor(long gameTime) {
        return gameTime - formedAt;
    }

    /** The way out of the room through the controller's wall. */
    public Direction outside() {
        return getBlockState().getValue(com.xtrip.golemgarage.block.GarageControllerBlock.FACING).getOpposite();
    }

    /** Where the idle lift pad sits: the busy center, or the middle of the formed room. */
    @Nullable
    public BlockPos padCenter() {
        if (bayCenter != null && isBusy()) {
            return bayCenter;
        }
        if (bayMin == null || bayMax == null) {
            return null;
        }
        return new BlockPos((bayMin.getX() + bayMax.getX()) / 2, bayMin.getY(), (bayMin.getZ() + bayMax.getZ()) / 2);
    }

    // =================================================================== starting a repair
    /** Starts repairing a golem from its saved data. Returns an error message, or null on success. */
    @Nullable
    public String startRepair(CompoundTag golemTag, @Nullable Player payer) {
        return start(golemTag, payer, null);
    }

    /** A golem walked in on its own (Go to Garage rule). Returns an error, or null if it entered. */
    @Nullable
    public String acceptGolem(GolemEntity golem) {
        CompoundTag tag = new CompoundTag();
        golem.exportData(tag);
        String error = start(tag, null, golem);
        if (error == null) {
            golem.discard();
        }
        return error;
    }

    @Nullable
    private String start(CompoundTag golemTag, @Nullable Player payer, @Nullable GolemEntity golem) {
        if (level == null || isBusy()) {
            return com.xtrip.golemgarage.golem.GolemText.of("garage_busy");
        }
        GarageBay bay = GarageBay.find(level, worldPosition);
        if (bay == null) {
            return com.xtrip.golemgarage.golem.GolemText.of("garage_invalid");
        }
        Map<GolemMaterial, Integer> cost = cost(golemTag);
        List<IItemHandler> sources = new ArrayList<>();
        if (payer != null && payer.getAbilities().instabuild) {
            cost.clear();
        } else if (payer != null) {
            sources.add(new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(payer.getInventory()));
        }
        if (golem != null) {
            sources.add(new net.neoforged.neoforge.items.wrapper.InvWrapper(golem.getInventory()));
        }
        sources.addAll(adjacentContainers());
        String missing = missing(cost, sources);
        if (missing != null) {
            return missing;
        }
        pay(cost, sources);

        GolemStats stats = GolemStats.compute(GolemData.readParts(golemTag));
        float speed = 1f;
        if (stats.matchedSet) {
            speed *= 0.8f;
        }
        if (stats.garageKit) {
            speed *= 0.8f;
        }
        this.repairData = golemTag.copy();
        this.progress = 0;
        this.required = Math.max(20, Math.round(com.xtrip.golemgarage.Config.REPAIR_SECONDS.get() * 20 * speed));
        this.bayCenter = bay.center();
        this.emitters.clear();
        this.emitters.addAll(bay.emitters());
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1f, 0.7f);
        if (GolemEntity.DEBUG) {
            com.xtrip.golemgarage.GolemGarage.LOGGER.info("[garage {}] repair started, {} ticks, paid {}",
                    worldPosition.toShortString(), required, cost);
        }
        setChanged();
        sync();
        return null;
    }

    /** What each damaged part costs to fix: one unit of its own material. */
    public static Map<GolemMaterial, Integer> cost(CompoundTag golemTag) {
        Map<GolemMaterial, Integer> cost = new LinkedHashMap<>();
        for (PartInstance p : GolemData.readParts(golemTag).values()) {
            int per = com.xtrip.golemgarage.Config.REPAIR_COST_PER_PART.get();
            if ((p.damage() > 0 || p.oxidation() > 0) && per > 0) {
                cost.merge(p.material(), per, Integer::sum);
            }
        }
        return cost;
    }

    /** Any storage block touching the controller (chests, barrels, drawers, modded storage...). */
    private List<IItemHandler> adjacentContainers() {
        List<IItemHandler> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            IItemHandler h = com.xtrip.golemgarage.golem.Storage.at(level, worldPosition.relative(d));
            if (h != null) {
                out.add(h);
            }
        }
        return out;
    }

    @Nullable
    private static String missing(Map<GolemMaterial, Integer> cost, List<IItemHandler> sources) {
        List<String> lacking = new ArrayList<>();
        for (Map.Entry<GolemMaterial, Integer> e : cost.entrySet()) {
            int have = 0;
            for (IItemHandler c : sources) {
                for (int i = 0; i < c.getSlots(); i++) {
                    ItemStack s = c.getStackInSlot(i);
                    if (e.getKey().matches(s)) {
                        have += s.getCount();
                    }
                }
            }
            if (have < e.getValue()) {
                String what = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(e.getKey().tagKey())
                        .flatMap(t -> t.stream().findFirst())
                        .map(h -> com.xtrip.golemgarage.golem.GolemText.key(h.value().getDescriptionId()))
                        .orElse(e.getKey().id());
                lacking.add(com.xtrip.golemgarage.golem.GolemText.of("garage_needs", e.getValue() - have, what));
            }
        }
        if (lacking.isEmpty()) {
            return null;
        }
        return lacking.size() == 1 ? lacking.get(0)
                : com.xtrip.golemgarage.golem.GolemText.of("garage_needs_more", lacking.get(0), lacking.size() - 1);
    }

    private static void pay(Map<GolemMaterial, Integer> cost, List<IItemHandler> sources) {
        for (Map.Entry<GolemMaterial, Integer> e : cost.entrySet()) {
            int left = e.getValue();
            for (IItemHandler c : sources) {
                for (int i = 0; i < c.getSlots() && left > 0; i++) {
                    ItemStack s = c.getStackInSlot(i);
                    if (e.getKey().matches(s)) {
                        left -= c.extractItem(i, Math.min(left, s.getCount()), false).getCount();
                    }
                }
            }
        }
    }

    // =================================================================== ticking
    public static void serverTick(Level level, BlockPos pos, BlockState state, GarageControllerBlockEntity garage) {
        if (level instanceof ServerLevel sl) {
            if ((level.getGameTime() + pos.asLong()) % 40 == 0) {
                garage.checkFormed(sl);
            }
            garage.tickRepair(sl);
        }
    }

    /** Re-checks the room, lights or dims its emitters, and tells players when it changes. */
    private void checkFormed(ServerLevel level) {
        GarageBay bay = GarageBay.find(level, worldPosition);
        boolean nowFormed = bay != null;
        BlockPos min = bay == null ? null : bay.min();
        BlockPos max = bay == null ? null : bay.max();
        List<BlockPos> lit = bay == null ? List.of() : bay.emitters();
        if (bay != null) {
            placeLights(level, bay);
        } else {
            removeLights(level);
        }
        boolean changed = nowFormed != formed || !java.util.Objects.equals(min, bayMin) || !java.util.Objects.equals(max, bayMax)
                || !litEmitters.equals(lit);
        if (!changed) {
            return;
        }
        for (BlockPos e : litEmitters) {
            if (!lit.contains(e)) {
                setLit(level, e, false);
            }
        }
        for (BlockPos e : lit) {
            setLit(level, e, true);
        }
        if (nowFormed && !formed) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.3f);
            // The clank of the room being bolted into a garage.
            BlockPos mid = bay.center().above();
            level.playSound(null, mid, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35f, 0.6f);
            level.playSound(null, mid, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 0.8f);
            level.playSound(null, mid, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 0.7f);
        } else if (!nowFormed && formed) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 1.3f);
        }
        formed = nowFormed;
        bayMin = min;
        bayMax = max;
        litEmitters.clear();
        litEmitters.addAll(lit);
        setChanged();
        sync();
    }

    /**
     * Lights the finished room: an invisible light block in each top corner (where the lanterns hang) and one
     * over the lift, so golems, players and the workshop are all lit, day or night.
     */
    private void placeLights(Level level, GarageBay bay) {
        BlockPos min = bay.min();
        BlockPos max = bay.max();
        int y = max.getY();
        List<BlockPos> want = new ArrayList<>(List.of(new BlockPos(min.getX(), y, min.getZ()), new BlockPos(max.getX(), y, min.getZ()),
                new BlockPos(min.getX(), y, max.getZ()), new BlockPos(max.getX(), y, max.getZ())));
        if (max.getY() - min.getY() >= 3) {
            BlockPos c = bay.center();
            want.add(new BlockPos(c.getX(), y, c.getZ()));
        }
        for (BlockPos p : lights) {
            if (!want.contains(p) && level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        lights.retainAll(want);
        for (int i = 0; i < want.size(); i++) {
            BlockPos p = want.get(i);
            BlockState s = level.getBlockState(p);
            int lvl = i < 4 ? 15 : 13;
            if (s.isAir()) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, lvl), 3);
            }
            if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT) && !lights.contains(p)) {
                lights.add(p);
                setChanged();
            }
        }
    }

    /** Takes the hidden lights down again (the room was broken, or the controller removed). */
    public void removeLights(Level level) {
        if (lights.isEmpty()) {
            return;
        }
        for (BlockPos p : lights) {
            if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        lights.clear();
        setChanged();
    }

    private static void setLit(Level level, BlockPos pos, boolean lit) {
        BlockState s = level.getBlockState(pos);
        if (s.hasProperty(com.xtrip.golemgarage.block.LaserEmitterBlock.LIT)
                && s.getValue(com.xtrip.golemgarage.block.LaserEmitterBlock.LIT) != lit) {
            level.setBlock(pos, s.setValue(com.xtrip.golemgarage.block.LaserEmitterBlock.LIT, lit), 3);
        }
    }

    /** The controller is being removed: its emitters go dim. */
    public void unlightEmitters() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos e : litEmitters) {
            setLit(level, e, false);
        }
        litEmitters.clear();
        removeLights(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GarageControllerBlockEntity garage) {
        if (garage.formed) {
            garage.formedParticles(level);
        }
        if (!garage.isBusy() || garage.bayCenter == null) {
            return;
        }
        if (garage.progress < garage.required) {
            garage.progress++;
        }
        long time = level.getGameTime();
        if (!garage.isWorking()) {
            return;
        }
        var rnd = level.random;
        for (int i = 0; i < garage.emitters.size(); i++) {
            if (garage.firePower(i, time) < 0.5f) {
                continue;
            }
            Vec3 t = garage.beamTarget(i, time);
            // Sparks spray off where the bolt hits, with the odd glowing ember.
            for (int k = 0; k < 2; k++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, t.x, t.y, t.z,
                        (rnd.nextDouble() - 0.5) * 0.5, rnd.nextDouble() * 0.3, (rnd.nextDouble() - 0.5) * 0.5);
            }
            if (rnd.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CRIT, t.x, t.y, t.z,
                        (rnd.nextDouble() - 0.5) * 0.6, rnd.nextDouble() * 0.3, (rnd.nextDouble() - 0.5) * 0.6);
            }
            if (rnd.nextInt(7) == 0) {
                level.addParticle(ParticleTypes.LAVA, t.x, t.y, t.z, 0, 0, 0);
            }
        }
        // Smoke drifts up off the golem's hot plating.
        if (time % 4 == 0) {
            Vec3 c = garage.golemBase(0);
            level.addParticle(ParticleTypes.SMOKE, c.x + (rnd.nextDouble() - 0.5) * 1.2,
                    c.y + 0.5 + rnd.nextDouble() * 2, c.z + (rnd.nextDouble() - 0.5) * 1.2, 0, 0.03, 0);
        }
        if (time % 15 == 0) {
            Vec3 c = garage.golemBase(0);
            level.addParticle(ParticleTypes.LARGE_SMOKE, c.x + (rnd.nextDouble() - 0.5), c.y + 0.3,
                    c.z + (rnd.nextDouble() - 0.5), 0, 0.02, 0);
        }
    }

    /** The roof corner of the finished Garage farthest from the controller, where the exhaust stack stands. */
    @Nullable
    public BlockPos exhaustBase() {
        if (!formed || bayMin == null || bayMax == null) {
            return null;
        }
        BlockPos best = null;
        double bestD = -1;
        for (int x : new int[]{bayMin.getX() - 1, bayMax.getX() + 1}) {
            for (int z : new int[]{bayMin.getZ() - 1, bayMax.getZ() + 1}) {
                BlockPos p = new BlockPos(x, bayMax.getY() + 2, z);
                double d = p.distSqr(worldPosition);
                if (d > bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    /** Height of the exhaust stack above its base block. */
    public static final double EXHAUST_HEIGHT = 1.8;

    /** Client: sparks and smoke while the room transforms, then a smoking exhaust stack on the roof. */
    private void formedParticles(Level level) {
        var rnd = level.random;
        long since = formedFor(level.getGameTime());
        if (since < 50 && bayMin != null && bayMax != null) {
            for (int k = 0; k < 4; k++) {
                double x = bayMin.getX() + rnd.nextDouble() * (bayMax.getX() + 1 - bayMin.getX());
                double y = bayMin.getY() + rnd.nextDouble() * (bayMax.getY() + 1 - bayMin.getY());
                double z = bayMin.getZ() + rnd.nextDouble() * (bayMax.getZ() + 1 - bayMin.getZ());
                // Snap to the nearest wall so it looks like the panels are being welded on.
                switch (rnd.nextInt(3)) {
                    case 0 -> x = rnd.nextBoolean() ? bayMin.getX() + 0.05 : bayMax.getX() + 0.95;
                    case 1 -> z = rnd.nextBoolean() ? bayMin.getZ() + 0.05 : bayMax.getZ() + 0.95;
                    default -> y = rnd.nextBoolean() ? bayMin.getY() + 0.05 : bayMax.getY() + 0.95;
                }
                level.addParticle(rnd.nextInt(3) == 0 ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK, x, y, z,
                        (rnd.nextDouble() - 0.5) * 0.2, rnd.nextDouble() * 0.1, (rnd.nextDouble() - 0.5) * 0.2);
            }
        }
        BlockPos stack = exhaustBase();
        if (stack != null && level.getBlockState(stack).isAir()) {
            double top = stack.getY() + EXHAUST_HEIGHT + 0.1;
            if (level.getGameTime() % 3 == 0) {
                level.addParticle(ParticleTypes.SMOKE, stack.getX() + 0.5, top, stack.getZ() + 0.5,
                        (rnd.nextDouble() - 0.5) * 0.02, 0.08, (rnd.nextDouble() - 0.5) * 0.02);
            }
            if (level.getGameTime() % (isBusy() ? 8 : 20) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, stack.getX() + 0.5, top, stack.getZ() + 0.5,
                        0, 0.04, 0);
            }
        }
    }

    /** Bolt colours, one per emitter: red-orange, electric blue, hot pink, amber. */
    public static final float[][] BOLT_COLORS = {
            {1.0f, 0.32f, 0.12f}, {0.35f, 0.55f, 1.0f}, {1.0f, 0.25f, 0.6f}, {1.0f, 0.62f, 0.15f}};
    private static final int FIRE_CYCLE = 34;

    /**
     * How hard emitter i is firing right now, 0..1. Each emitter fires in bursts (a quick flare, about a second
     * of crackling bolt, then a short pause), staggered so the lasers take turns.
     */
    public float firePower(int i, float time) {
        if (!isWorking()) {
            return 0f;
        }
        float phase = (time + i * 11) % FIRE_CYCLE;
        if (phase < 2) {
            return phase / 2f;
        }
        if (phase < 22) {
            return 1f;
        }
        if (phase < 25) {
            return (25 - phase) / 3f;
        }
        return 0f;
    }

    private void tickRepair(ServerLevel level) {
        if (repairData == null) {
            return;
        }
        GarageBay bay = GarageBay.find(level, worldPosition);
        if (bay == null) {
            if (level.getGameTime() % 100 == 0) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5f, 0.8f);
            }
            return;
        }
        progress++;
        if (progress % 5 == 0 && bay.hasSteelEmitter(level)) {
            progress++;
        }
        if (progress == LIFT_TICKS) {
            level.playSound(null, worldPosition, com.xtrip.golemgarage.ModSounds.GARAGE_START.get(), SoundSource.BLOCKS, 1f, 1f);
        }
        if (isWorking()) {
            long now = level.getGameTime();
            for (int i = 0; i < emitters.size(); i++) {
                if ((now + i * 11) % FIRE_CYCLE == 0) {
                    level.playSound(null, emitters.get(i), com.xtrip.golemgarage.ModSounds.GARAGE_LASER.get(),
                            SoundSource.BLOCKS, 0.5f, 0.85f + 0.1f * i);
                }
            }
        }
        if (isWorking() && progress % 23 == 0) {
            level.playSound(null, bayCenter, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.15f, 1.8f);
        }
        if (progress % 100 == 0) {
            sync();
        }
        if (progress >= required) {
            finish(level);
        }
    }

    public boolean isWorking() {
        return progress > LIFT_TICKS && progress < required - LIFT_TICKS;
    }

    /** Height of the lift platform, 0..0.6 blocks: rises at the start, lowers at the end. */
    public float liftHeight(float partial) {
        float p = progress + partial;
        float up = Mth.clamp(p / LIFT_TICKS, 0f, 1f);
        float down = Mth.clamp((required - p) / LIFT_TICKS, 0f, 1f);
        float t = Math.min(up, down);
        return 0.6f * (t * t * (3 - 2 * t));
    }

    /** Where the golem's feet are during repair, in world space. */
    public Vec3 golemBase(float partial) {
        return new Vec3(bayCenter.getX() + 0.5, bayCenter.getY() + liftHeight(partial) + 0.125, bayCenter.getZ() + 0.5);
    }

    /** Where emitter i's beam hits at this time: sweeps up and down the golem and around it. */
    public Vec3 beamTarget(int i, float time) {
        Vec3 base = golemBase(0);
        double phase = time * 0.045 + i * 1.7;
        double y = base.y + 0.3 + (Math.sin(phase) * 0.5 + 0.5) * 2.2;
        double ang = time * 0.03 + i * (Math.PI * 2 / Math.max(1, emitters.size()));
        double r = 0.5;
        return new Vec3(base.x + Math.cos(ang) * r, y, base.z + Math.sin(ang) * r);
    }

    private void finish(ServerLevel level) {
        CompoundTag tag = repairData.copy();
        Map<PartSlot, PartInstance> parts = GolemData.readParts(tag);
        for (PartInstance p : parts.values()) {
            p.repairFully();
            p.polish();
        }
        tag.put("Parts", GolemData.writeParts(parts));
        tag.putBoolean("Broken", false);
        tag.remove("HealthLeft");
        tag.putInt("Power", Integer.MAX_VALUE);

        GolemEntity golem = ModEntities.GOLEM.get().create(level);
        Player owner = tag.hasUUID("Owner") ? level.getPlayerByUUID(tag.getUUID("Owner")) : null;
        com.xtrip.golemgarage.advancement.ModTriggers.award(owner, "garage_repair");
        if (golem != null) {
            golem.importData(tag);
            Vec3 c = Vec3.atBottomCenterOf(bayCenter);
            float yaw = (float) (Mth.atan2(worldPosition.getZ() + 0.5 - c.z, worldPosition.getX() + 0.5 - c.x)
                    * Mth.RAD_TO_DEG) - 90f;
            golem.moveTo(c.x, c.y, c.z, yaw, 0);
            golem.setYHeadRot(yaw);
            golem.setYBodyRot(yaw);
            level.addFreshEntity(golem);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1.4, c.z, 40, 0.6, 1.0, 0.6, 0.3);
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("golemgarage.msg.repaired")
                        .withStyle(ChatFormatting.GREEN), false);
            }
        } else {
            ItemStack fixed = GolemItem.stackFromTag(tag);
            if (owner == null || !owner.getInventory().add(fixed)) {
                Block.popResource(level, worldPosition.above(), fixed);
            }
        }
        if (GolemEntity.DEBUG) {
            com.xtrip.golemgarage.GolemGarage.LOGGER.info("[garage {}] repair finished", worldPosition.toShortString());
        }
        level.playSound(null, worldPosition, com.xtrip.golemgarage.ModSounds.GARAGE_DONE.get(), SoundSource.BLOCKS, 1f, 1f);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1f, 0.7f);
        repairData = null;
        progress = 0;
        required = REPAIR_TICKS;
        setChanged();
        sync();
    }

    /** Drops the golem being repaired, still broken, when the controller is removed. */
    public void dropGolem() {
        if (repairData == null || level == null || level.isClientSide) {
            return;
        }
        CompoundTag tag = repairData.copy();
        tag.putBoolean("Broken", true);
        Block.popResource(level, worldPosition, GolemItem.stackFromTag(tag));
        repairData = null;
    }

    public MutableComponent describe() {
        GarageBay bay = level == null ? null : GarageBay.find(level, worldPosition);
        MutableComponent msg;
        if (bay == null) {
            msg = Component.translatable("golemgarage.msg.garage_bad").withStyle(ChatFormatting.RED);
        } else {
            msg = Component.translatable(bay.hasSteelEmitter(level) ? "golemgarage.msg.garage_ok_steel" : "golemgarage.msg.garage_ok",
                    bay.emitters().size()).withStyle(ChatFormatting.GREEN);
        }
        if (isBusy()) {
            int pct = Math.round(100f * progress / Math.max(1, required));
            msg.append(" ").append(Component.translatable("golemgarage.msg.garage_progress", pct, Math.max(0, (required - progress) / 20))
                    .withStyle(ChatFormatting.AQUA));
        } else if (bay != null) {
            msg.append(" ").append(Component.translatable("golemgarage.msg.garage_hint").withStyle(ChatFormatting.GRAY));
        }
        return msg;
    }

    // =================================================================== client display
    /** Client only: the golem shown on the lift during repair, rebuilt from the synced data. */
    @Nullable
    public GolemEntity displayGolem() {
        if (repairData == null || level == null) {
            displayGolem = null;
            return null;
        }
        if (displayGolem == null) {
            displayGolem = ModEntities.GOLEM.get().create(level);
            if (displayGolem != null) {
                displayGolem.importData(repairData);
            }
        }
        return displayGolem;
    }

    // =================================================================== sync and save
    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putInt("Required", required);
        if (repairData != null) {
            tag.put("Golem", repairData);
        }
        if (bayCenter != null) {
            tag.putLong("Center", bayCenter.asLong());
        }
        ListTag list = new ListTag();
        for (BlockPos e : emitters) {
            list.add(LongTag.valueOf(e.asLong()));
        }
        tag.put("Emitters", list);
        tag.putBoolean("Formed", formed);
        if (bayMin != null && bayMax != null) {
            tag.putLong("BayMin", bayMin.asLong());
            tag.putLong("BayMax", bayMax.asLong());
        }
        ListTag lit = new ListTag();
        for (BlockPos e : litEmitters) {
            lit.add(LongTag.valueOf(e.asLong()));
        }
        tag.put("Lit", lit);
        ListTag lightList = new ListTag();
        for (BlockPos p : lights) {
            lightList.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Lights", lightList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        required = tag.contains("Required") ? tag.getInt("Required") : REPAIR_TICKS;
        CompoundTag newData = tag.contains("Golem") ? tag.getCompound("Golem") : null;
        if (newData == null || !newData.equals(repairData)) {
            displayGolem = null;
        }
        repairData = newData;
        bayCenter = tag.contains("Center") ? BlockPos.of(tag.getLong("Center")) : null;
        emitters.clear();
        for (Tag t : tag.getList("Emitters", Tag.TAG_LONG)) {
            emitters.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
        boolean nowFormed = tag.getBoolean("Formed");
        if (nowFormed && !formed && level != null && level.isClientSide) {
            formedAt = level.getGameTime();
        }
        formed = nowFormed;
        bayMin = tag.contains("BayMin") ? BlockPos.of(tag.getLong("BayMin")) : null;
        bayMax = tag.contains("BayMax") ? BlockPos.of(tag.getLong("BayMax")) : null;
        litEmitters.clear();
        for (Tag t : tag.getList("Lit", Tag.TAG_LONG)) {
            litEmitters.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
        lights.clear();
        for (Tag t : tag.getList("Lights", Tag.TAG_LONG)) {
            lights.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
    }
}
