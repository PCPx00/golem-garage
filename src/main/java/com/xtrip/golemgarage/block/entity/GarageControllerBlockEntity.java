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

    /** Client only: the golem model shown during repair. */
    private GolemEntity displayGolem;

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
            garage.tickRepair(sl);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GarageControllerBlockEntity garage) {
        if (!garage.isBusy() || garage.bayCenter == null) {
            return;
        }
        if (garage.progress < garage.required) {
            garage.progress++;
        }
        long time = level.getGameTime();
        if (garage.isWorking() && time % 3 == 0) {
            for (int i = 0; i < garage.emitters.size(); i++) {
                Vec3 t = garage.beamTarget(i, time);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, t.x, t.y, t.z,
                        (level.random.nextDouble() - 0.5) * 0.2, 0.05, (level.random.nextDouble() - 0.5) * 0.2);
            }
        }
        if (garage.isWorking() && time % 20 == 0) {
            Vec3 c = garage.golemBase(0);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, c.x + (level.random.nextDouble() - 0.5),
                    c.y + 1 + level.random.nextDouble() * 1.5, c.z + (level.random.nextDouble() - 0.5), 0, 0, 0);
        }
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
        if (isWorking() && progress % 40 == 0) {
            level.playSound(null, bayCenter, com.xtrip.golemgarage.ModSounds.GARAGE_LASER.get(), SoundSource.BLOCKS, 0.7f, 1f);
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
    }
}
