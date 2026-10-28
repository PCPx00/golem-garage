package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.client.ClientHooks;
import com.xtrip.golemgarage.golem.BehaviorGoal;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemRule;
import com.xtrip.golemgarage.golem.GolemStats;
import com.xtrip.golemgarage.golem.GolemTrait;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.golem.ShapeAbility;
import com.xtrip.golemgarage.item.BlueprintItem;
import com.xtrip.golemgarage.item.GolemItem;
import com.xtrip.golemgarage.item.GolemLinkerItem;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A modular golem. Five part slots (head, torso, arms, legs, core) each hold a shape made
 * from a material; together they decide its stats, abilities and look.
 *
 * It never dies. At zero health it breaks down into a Broken Golem item for its owner.
 * Each part wears on its own and can break; broken parts weaken the golem until repaired.
 */
public class GolemEntity extends PathfinderMob {
    public static final int INVENTORY_SIZE = 36;
    public static final int LOG_SIZE = 10;
    public static final int FLAG_VINES = 1;
    public static final int FLAG_SMELTING = 2;
    public static final int FLAG_EMITTING = 4;
    public static final int FLAG_CLIMBING = 8;
    public static final int FLAG_BREAKING = 16;
    private static final byte EVENT_WORK = 101;
    private static final int BREAKDOWN_TICKS = 30;
    private static final EntityDataAccessor<Integer> DATA_PAINT =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GROUP =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private boolean paused;
    @Nullable
    private BlockPos dockedAt;
    /** The owner, synced so clients know whose golem it is before opening its panel. */
    private static final EntityDataAccessor<java.util.Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int workAnimationTick;
    private int breakingTicks;
    private int clientBreakingTicks;

    private static final EntityDataAccessor<CompoundTag> DATA_PARTS =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_RULES =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_LINKS =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_LOG =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> DATA_USED =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POWER =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTIVE_RULE =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_STATUS =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BYTE);

    private final Map<PartSlot, PartInstance> parts = new EnumMap<>(PartSlot.class);
    private final Map<GolemLinkerItem.Mode, BlockPos> links = new EnumMap<>(GolemLinkerItem.Mode.class);
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private final List<String> log = new ArrayList<>();
    private final List<com.xtrip.golemgarage.golem.GolemModule> modules = new ArrayList<>();
    private GolemStats stats = GolemStats.compute(parts);
    private UUID ownerUuid;
    private boolean broken;
    private List<GolemRule> rules = new ArrayList<>(GolemRule.followerPreset());
    private int rulesVersion;
    private int power;
    private int powerSpent;
    private int attackAnimationTick;
    private double walked;
    private int workOps;
    private int smeltProgress;
    private int burnTicks;
    private int emitTicks;
    private BlockPos lightPos;
    private Vec3 lastPos;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    // =================================================================== setup
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PARTS, new CompoundTag());
        builder.define(DATA_RULES, new CompoundTag());
        builder.define(DATA_LINKS, new CompoundTag());
        builder.define(DATA_LOG, new CompoundTag());
        builder.define(DATA_USED, 0);
        builder.define(DATA_POWER, 0);
        builder.define(DATA_ACTIVE_RULE, -1);
        builder.define(DATA_PAINT, -1);
        builder.define(DATA_OWNER, java.util.Optional.empty());
        builder.define(DATA_GROUP, 0);
        builder.define(DATA_MODE, 0);
        builder.define(DATA_STATUS, "");
        builder.define(DATA_FLAGS, (byte) 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, GolemStats.BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, GolemStats.BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BehaviorGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // =================================================================== parts
    public Map<PartSlot, PartInstance> getEquipped() {
        return parts;
    }

    @Nullable
    public PartInstance getPart(PartSlot slot) {
        return parts.get(slot);
    }

    public GolemStats stats() {
        return stats;
    }

    /** Fits a part. Returns the part it replaced, or null. */
    @Nullable
    public PartInstance equipPart(PartInstance part) {
        PartInstance old = parts.put(part.shape().slot(), part);
        refresh();
        return old;
    }

    @Nullable
    public PartInstance removePart(PartSlot slot) {
        PartInstance old = parts.remove(slot);
        refresh();
        return old;
    }

    /** Recomputes stats from the parts and pushes them to attributes and clients. */
    public void refresh() {
        stats = GolemStats.compute(parts).withModules(modules);
        if (level().isClientSide) {
            return;
        }
        power = Math.min(power, stats.capacity);
        applyAttributes();
        PathNavigation wanted = stats.climb ? new WallClimberNavigation(this, level()) : new GroundPathNavigation(this, level());
        if (navigation.getClass() != wanted.getClass()) {
            navigation.stop();
            navigation = wanted;
        }
        CompoundTag sync = new CompoundTag();
        sync.put("Parts", GolemData.writeParts(parts));
        sync.putInt("Cap", stats.capacity);
        sync.put("Mods", modulesTag());
        this.entityData.set(DATA_PARTS, sync);
        this.entityData.set(DATA_POWER, power);
    }

    private void applyAttributes() {
        set(Attributes.MAX_HEALTH, stats.maxHealth);
        float powerFactor = stats.capacity > 0 && power <= 0 ? 0.4f : 1f;
        set(Attributes.MOVEMENT_SPEED, stats.speed * powerFactor);
        set(Attributes.ATTACK_DAMAGE, stats.attack);
        set(Attributes.ATTACK_KNOCKBACK, stats.attackKnockback);
        set(Attributes.ARMOR, Math.min(30f, stats.armor));
        set(Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResist);
        set(Attributes.STEP_HEIGHT, stats.step);
        set(Attributes.FOLLOW_RANGE, Math.max(16f, stats.vision));
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private void set(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = getAttribute(attr);
        if (inst != null && inst.getBaseValue() != value) {
            inst.setBaseValue(value);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PARTS.equals(key) && level().isClientSide) {
            parts.clear();
            parts.putAll(GolemData.readParts(this.entityData.get(DATA_PARTS)));
            readModules(this.entityData.get(DATA_PARTS));
            stats = GolemStats.compute(parts).withModules(modules);
        }
    }

    // =================================================================== tick
    @Override
    public void tick() {
        super.tick();
        if (attackAnimationTick > 0) {
            attackAnimationTick--;
        }
        if (workAnimationTick > 0) {
            workAnimationTick--;
        }
        if (level().isClientSide) {
            clientBreakingTicks = hasFlag(FLAG_BREAKING) ? clientBreakingTicks + 1 : 0;
            clientEffects();
            return;
        }
        if (hasFlag(FLAG_BREAKING)) {
            tickBreakdown();
            return;
        }
        if (tickCount % 100 == 7 && getServer() != null) {
            com.xtrip.golemgarage.golem.GolemRegistry.get(getServer()).update(this);
        }
        if (dockedAt != null && tickCount % 20 == 0 && (distanceToSqr(Vec3.atBottomCenterOf(dockedAt)) > 2.5
                || activeAction() != GolemRule.Action.GO_HOME)) {
            undock();
        }
        if (stats.climb) {
            setFlag(FLAG_CLIMBING, horizontalCollision);
        }
        trackWalking();
        if (tickCount % 5 == 0) {
            updateLight();
        }
        if (tickCount % 20 == 0) {
            secondTick();
        }
        if (tickCount % 600 == 0) {
            weatherWear();
        }
        moduleTick();
        if (tickCount % 1200 == 0) {
            oxidize();
        }
        if (emitTicks > 0 && --emitTicks == 0) {
            setFlag(FLAG_EMITTING, false);
        }
    }

    private void secondTick() {
        int used = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                used++;
            }
        }
        this.entityData.set(DATA_USED, used);

        // Power drain: a little while idle, more while moving.
        if (stats.capacity > 0) {
            boolean moving = !getNavigation().isDone();
            if (moving && tickCount % 40 == 0) {
                drainPower(1);
            } else if (!moving && tickCount % 200 == 0) {
                drainPower(1);
            }
            recharge();
        }
        if (stats.smelter) {
            smelt();
        } else {
            setFlag(FLAG_SMELTING, false);
        }
        if (stats.ender && tickCount % 40 == 0) {
            enderCatchUp();
        }
        if (stats.spotter && tickCount % 40 == 0) {
            spotHostiles();
        }
        applyAttributes();
    }

    // =================================================================== power
    public int getPower() {
        return power;
    }

    public int getMaxPower() {
        return stats.capacity;
    }

    /** Percent of power left. Golems without a core show 100. */
    public int getPowerPercent() {
        return stats.capacity <= 0 ? 100 : power * 100 / stats.capacity;
    }

    public void addPower(int amount) {
        int before = power;
        power = Math.max(0, Math.min(stats.capacity, power + amount));
        if (power != before) {
            this.entityData.set(DATA_POWER, power);
            if (power == 0 && before > 0 && !level().isClientSide) {
                playSound(com.xtrip.golemgarage.ModSounds.GOLEM_POWER_LOW.get(), 0.6f, 1f);
            }
        }
    }

    /** Spends power. Every 100 points spent wears the core a little. */
    public void drainPower(int amount) {
        if (stats.capacity <= 0) {
            return;
        }
        amount = scaled(amount, com.xtrip.golemgarage.Config.POWER_DRAIN_MULTIPLIER.get());
        if (amount <= 0) {
            return;
        }
        addPower(-amount);
        powerSpent += amount;
        if (powerSpent >= 100) {
            powerSpent -= 100;
            wearPart(PartSlot.CORE, 1);
        }
    }

    private void recharge() {
        PartInstance core = parts.get(PartSlot.CORE);
        if (core == null || core.isBroken() || stats.rechargeType == null) {
            return;
        }
        boolean conductive = parts.values().stream().anyMatch(p -> p.material().has(GolemTrait.CONDUCTIVE));
        float boost = conductive ? 1.25f : 1f;
        boolean sky = level().canSeeSky(blockPosition().above());
        switch (stats.rechargeType) {
            case RECHARGE_REDSTONE -> {
                if (getPowerPercent() < 75) {
                    if (consume(s -> s.is(Items.REDSTONE_BLOCK))) {
                        addPower(Math.round(100 * boost));
                    } else if (consume(s -> s.is(Items.REDSTONE))) {
                        addPower(Math.round(12 * boost));
                    }
                }
            }
            case RECHARGE_FUEL -> {
                if (getPowerPercent() < 90) {
                    for (int i = 0; i < INVENTORY_SIZE; i++) {
                        ItemStack s = inventory.getItem(i);
                        int burn = s.isEmpty() ? 0 : s.getBurnTime(RecipeType.SMELTING);
                        if (burn > 0 && findSmeltable() != i) {
                            useFuel(i);
                            addPower(Math.round(burn / 20f * boost));
                            break;
                        }
                    }
                }
            }
            case RECHARGE_STORM -> {
                if (sky && level().isThundering()) {
                    addPower(Math.round(3 * boost));
                } else if (sky && level().isRaining()) {
                    addPower(Math.round(1 * boost));
                }
            }
            case RECHARGE_SUN -> {
                if (sky && level().isDay() && !level().isRaining() && tickCount % 40 == 0) {
                    addPower(Math.round(2 * boost));
                }
            }
            case RECHARGE_SOUL -> {
                if (tickCount % 60 == 0) {
                    addPower(Math.max(1, Math.round(boost)));
                }
                if (tickCount % 100 == 0 && getHealth() < getMaxHealth()) {
                    heal(1f);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        if (stats.rechargeType == ShapeAbility.RECHARGE_STORM) {
            addPower(stats.capacity);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 40, 0.6, 1.0, 0.6, 0.2);
            setStatus(com.xtrip.golemgarage.golem.GolemText.of("lightning"));
            return;
        }
        super.thunderHit(level, bolt);
    }

    // =================================================================== wear
    /** Wears one part. Hardened materials and matched sets sometimes shrug it off. */
    public void wearPart(PartSlot slot, int amount) {
        PartInstance part = parts.get(slot);
        if (part == null || level().isClientSide || amount <= 0) {
            return;
        }
        amount = scaled(amount, com.xtrip.golemgarage.Config.WEAR_MULTIPLIER.get());
        if (amount <= 0) {
            return;
        }
        if (part.material().has(GolemTrait.HARDENED) && random.nextFloat() < 0.3f) {
            return;
        }
        if (stats.matchedSet && random.nextFloat() < 0.1f) {
            return;
        }
        boolean justBroke = part.wear(amount);
        if (justBroke) {
            Player owner = getOwner();
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("golemgarage.msg.part_broke", part.toStack().getHoverName())
                        .withStyle(ChatFormatting.GOLD), false);
            }
            playSound(SoundEvents.ITEM_BREAK, 1f, 0.8f);
            log(com.xtrip.golemgarage.golem.GolemText.of("log.part_broke", com.xtrip.golemgarage.golem.GolemText.key(part.shape().displayKey())));
            refresh();
        } else if (tickCount % 5 == 0) {
            refresh();
        }
    }

    private void wearRandomPart(int amount) {
        PartSlot[] slots = {PartSlot.TORSO, PartSlot.TORSO, PartSlot.HEAD, PartSlot.ARMS, PartSlot.LEGS};
        wearPart(slots[random.nextInt(slots.length)], amount);
    }

    /** Scales a whole amount by a multiplier, rounding the fraction randomly so averages come out right. */
    private int scaled(int amount, double mult) {
        double v = amount * mult;
        int whole = (int) Math.floor(v);
        return whole + (random.nextDouble() < v - whole ? 1 : 0);
    }

    private void weatherWear() {
        if (!com.xtrip.golemgarage.Config.WEATHER_WEAR.get() || !level().canSeeSky(blockPosition().above(2))) {
            return;
        }
        boolean rain = level().isRainingAt(blockPosition().above(2));
        boolean sand = level().getBiome(blockPosition()).is(Tags.Biomes.IS_DESERT);
        if (!rain && !sand) {
            return;
        }
        for (Map.Entry<PartSlot, PartInstance> e : new ArrayList<>(parts.entrySet())) {
            if (!stats.isSealed(e.getKey(), e.getValue())) {
                wearPart(e.getKey(), 1);
            }
        }
    }

    private void oxidize() {
        if (!com.xtrip.golemgarage.Config.COPPER_AGING.get()) {
            return;
        }
        boolean changed = false;
        for (PartInstance p : parts.values()) {
            if (p.canOxidize() && random.nextInt(16) == 0) {
                p.oxidize();
                changed = true;
            }
        }
        if (changed) {
            refresh();
        }
    }

    private void trackWalking() {
        Vec3 pos = position();
        if (lastPos != null && onGround()) {
            walked += Math.sqrt(pos.distanceToSqr(lastPos));
            if (walked >= 48) {
                walked -= 48;
                wearPart(PartSlot.LEGS, 1);
            }
        }
        lastPos = pos;
    }

    /** Called by behaviors after each unit of work (moving a stack, harvesting a crop). */
    public void onWorkDone() {
        workOps++;
        level().broadcastEntityEvent(this, EVENT_WORK);
        if (workOps % 3 == 0) {
            crowdSound(com.xtrip.golemgarage.ModSounds.GOLEM_WORK.get(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.WORK, 0.35f, 0.9f + random.nextFloat() * 0.2f);
        }
        drainPower(1);
        if (workOps % 12 == 0) {
            wearPart(PartSlot.ARMS, 1);
        }
    }

    /** A Repair Kit restores a quarter of every part and a quarter of health. */
    public void useRepairKit() {
        for (PartInstance p : parts.values()) {
            p.repair(Math.max(1, p.maxDurability() / 4));
        }
        heal(getMaxHealth() / 4f);
        refresh();
        playSound(SoundEvents.IRON_GOLEM_REPAIR, 1f, 1f);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.4, getZ(), 12, 0.6, 0.8, 0.6, 0);
        }
    }

    public boolean hasBrokenPart() {
        return parts.values().stream().anyMatch(PartInstance::isBroken);
    }

    /** Lowest part condition, 0..1 (1 when there are no parts). */
    public float worstCondition() {
        return (float) parts.values().stream().mapToDouble(PartInstance::condition).min().orElse(1.0);
    }

    // =================================================================== smelting
    private int findSmeltable() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && smeltingRecipe(s).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    private Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe(ItemStack stack) {
        return level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level());
    }

    private void smelt() {
        int input = findSmeltable();
        if (input < 0) {
            smeltProgress = 0;
            setFlag(FLAG_SMELTING, false);
            return;
        }
        if (burnTicks <= 0) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack s = inventory.getItem(i);
                if (i != input && !s.isEmpty() && s.getBurnTime(RecipeType.SMELTING) > 0) {
                    burnTicks = s.getBurnTime(RecipeType.SMELTING);
                    useFuel(i);
                    break;
                }
            }
        }
        if (burnTicks <= 0) {
            setFlag(FLAG_SMELTING, false);
            setStatus(com.xtrip.golemgarage.golem.GolemText.of("needs_fuel"));
            return;
        }
        setFlag(FLAG_SMELTING, true);
        burnTicks -= 20;
        smeltProgress += Math.round(20 * Math.max(0.5f, stats.work));
        if (smeltProgress >= 200) {
            smeltProgress = 0;
            ItemStack in = inventory.getItem(input);
            smeltingRecipe(in).ifPresent(r -> {
                ItemStack out = r.value().assemble(new SingleRecipeInput(in), level().registryAccess()).copy();
                if (inventory.canAddItem(out)) {
                    debug("Smelted " + in.getHoverName().getString() + " into " + out.getHoverName().getString());
                    in.shrink(1);
                    inventory.addItem(out);
                    onWorkDone();
                }
            });
        }
    }

    private void useFuel(int slot) {
        ItemStack fuel = inventory.getItem(slot);
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty()) {
            inventory.addItem(remainder);
        }
    }

    // =================================================================== light, ender, spotter
    private void updateLight() {
        int level = stats.lightLevel;
        BlockPos target = level > 0 ? BlockPos.containing(getX(), getY() + 1.6, getZ()) : null;
        if (lightPos != null && !lightPos.equals(target)) {
            clearLight();
        }
        if (target != null && !target.equals(lightPos)) {
            BlockState here = level().getBlockState(target);
            if (here.isAir()) {
                level().setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level), 3);
                lightPos = target;
            }
        }
    }

    private void clearLight() {
        if (lightPos != null && level().getBlockState(lightPos).is(Blocks.LIGHT)) {
            level().setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3);
        }
        lightPos = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            clearLight();
            undock();
            if (getServer() != null) {
                var registry = com.xtrip.golemgarage.golem.GolemRegistry.get(getServer());
                if (reason.shouldDestroy()) {
                    registry.remove(getUUID()); // picked up, broken down, repaired into an item, or killed
                } else {
                    registry.update(this); // unloaded or changing dimension: remember where it was
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide && getServer() != null) {
            var registry = com.xtrip.golemgarage.golem.GolemRegistry.get(getServer());
            if (registry.takePurged(getUUID())) {
                discard(); // removed by /golemgarage purge while it was unloaded
                return;
            }
            registry.update(this);
        }
    }

    /** Admin repair: every part like new, full health and power. */
    public void repairFully() {
        for (PartInstance p : parts.values()) {
            p.repairFully();
        }
        refresh();
        setHealth(getMaxHealth());
        addPower(stats.capacity);
    }

    // =================================================================== groups, pausing, docking
    /** Aggressive, Peaceful or Guard override the rules; Rules (the default) runs them. */
    public com.xtrip.golemgarage.golem.GolemMode getMode() {
        return com.xtrip.golemgarage.golem.GolemMode.byId(entityData.get(DATA_MODE));
    }

    public void setMode(com.xtrip.golemgarage.golem.GolemMode mode) {
        entityData.set(DATA_MODE, mode.ordinal());
        setTarget(null);
        getNavigation().stop();
    }

    public int getGroup() {
        return entityData.get(DATA_GROUP);
    }

    public void setGroup(int group) {
        entityData.set(DATA_GROUP, Math.max(0, Math.min(9, group)));
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            getNavigation().stop();
            setTarget(null);
        }
    }

    /** Called each work cycle while it stands on its dock: a slow top-up of power and health. */
    public void restOnDock(BlockPos dock) {
        if (dockedAt == null || !dockedAt.equals(dock)) {
            undock();
            dockedAt = dock.immutable();
            BlockState s = level().getBlockState(dock);
            if (s.hasProperty(com.xtrip.golemgarage.block.GolemDockBlock.OCCUPIED)) {
                level().setBlock(dock, s.setValue(com.xtrip.golemgarage.block.GolemDockBlock.OCCUPIED, true), 3);
            }
        }
        if (tickCount % 40 == 0) {
            if (stats.capacity > 0) {
                addPower(Math.max(1, stats.capacity / 50));
            }
            if (getHealth() < getMaxHealth()) {
                heal(1f);
            }
        }
    }

    private void undock() {
        if (dockedAt == null) {
            return;
        }
        BlockState s = level().getBlockState(dockedAt);
        if (s.hasProperty(com.xtrip.golemgarage.block.GolemDockBlock.OCCUPIED)) {
            level().setBlock(dockedAt, s.setValue(com.xtrip.golemgarage.block.GolemDockBlock.OCCUPIED, false), 3);
        }
        dockedAt = null;
    }

    /** Players are fair game only as the server's playerDamage setting allows, and never the owner. */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (getMode() == com.xtrip.golemgarage.golem.GolemMode.PEACEFUL) {
            return false; // never fights, even when hit
        }
        if (target instanceof Player p) {
            if (p.getUUID().equals(getOwnerUuid()) || p.isCreative() || p.isSpectator()) {
                return false;
            }
            com.xtrip.golemgarage.Config.PlayerDamage mode = com.xtrip.golemgarage.Config.get(
                    com.xtrip.golemgarage.Config.PLAYER_DAMAGE::get, com.xtrip.golemgarage.Config.PlayerDamage.PVP);
            return switch (mode) {
                case NEVER -> false;
                case PVP -> getServer() != null && getServer().isPvpAllowed() && super.canAttack(target);
                case ALWAYS -> super.canAttack(target);
            };
        }
        if (target instanceof GolemEntity other && java.util.Objects.equals(other.getOwnerUuid(), getOwnerUuid())) {
            return false;
        }
        return super.canAttack(target);
    }

    private void enderCatchUp() {
        Player owner = getOwner();
        if (owner == null || distanceToSqr(owner) < 24 * 24) {
            return;
        }
        GolemRule active = activeRule();
        if (active != null && active.action() != GolemRule.Action.FOLLOW_OWNER) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            double x = owner.getX() + random.nextInt(5) - 2;
            double z = owner.getZ() + random.nextInt(5) - 2;
            BlockPos p = BlockPos.containing(x, owner.getY(), z);
            if (level().getBlockState(p.below()).isSolid() && level().noCollision(this, getBoundingBox().move(
                    x - getX(), owner.getY() - getY(), z - getZ()))) {
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.5, 1, 0.5, 0.2);
                }
                teleportTo(x, owner.getY(), z);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
                getNavigation().stop();
                return;
            }
        }
    }

    private void spotHostiles() {
        for (Monster m : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(stats.vision))) {
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
        }
    }

    // =================================================================== client effects
    private void clientEffects() {
        if (random.nextInt(4) != 0) {
            return;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        if (hasFlag(FLAG_SMELTING) && random.nextInt(2) == 0) {
            // Chimney sits on the back right of the torso.
            double cx = getX() - fx * 0.15 + fz * 0.35;
            double cz = getZ() - fz * 0.15 - fx * 0.35;
            level().addParticle(ParticleTypes.SMOKE, cx, getY() + 2.45, cz, 0, 0.05, 0);
        }
        if (stats.rechargeType == ShapeAbility.RECHARGE_SOUL && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() - fx * 0.45, getY() + 1.6, getZ() - fz * 0.45, 0, 0.02, 0);
        }
        if (stats.rechargeType == ShapeAbility.RECHARGE_STORM && random.nextInt(8) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() - fx * 0.4, getY() + 2.9, getZ() - fz * 0.4, 0, 0, 0);
        }
        for (Map.Entry<PartSlot, PartInstance> e : parts.entrySet()) {
            if (e.getValue().isBroken() && random.nextInt(5) == 0) {
                double h = switch (e.getKey()) {
                    case HEAD -> 2.4;
                    case TORSO, CORE -> 1.7;
                    case ARMS -> 1.2;
                    case LEGS -> 0.5;
                };
                ParticleOptions p = random.nextBoolean() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE;
                level().addParticle(p, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + h,
                        getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.03, 0);
            }
        }
    }

    // =================================================================== status and log
    public void setStatus(String status) {
        if (status.equals(this.entityData.get(DATA_STATUS))) {
            return;
        }
        this.entityData.set(DATA_STATUS, status);
        // Only log a new kind of activity, so "Charging 41%" -> "Charging 42%" doesn't flood the log.
        String key = status.replaceAll("[0-9]+", "#");
        if (!key.equals(lastLogKey)) {
            lastLogKey = key;
            log(status);
        }
    }

    private String lastLogKey = "";

    /** Prints a line only in debug runs; never shown in game. */
    public void debug(String entry) {
        if (DEBUG && !level().isClientSide) {
            com.xtrip.golemgarage.GolemGarage.LOGGER.info("[golem {} at {}] {}",
                    hasCustomName() && getCustomName() != null ? getCustomName().getString() : getId(),
                    blockPosition().toShortString(), entry);
        }
    }

    public String readSyncedStatus() {
        return this.entityData.get(DATA_STATUS);
    }

    /** Set -Dgolemgarage.debug=true to print every golem's activity to the log (used by the behavior tests). */
    public static final boolean DEBUG = Boolean.getBoolean("golemgarage.debug");

    private void log(String entry) {
        if (entry.isEmpty()) {
            return;
        }
        if (DEBUG && !level().isClientSide) {
            com.xtrip.golemgarage.GolemGarage.LOGGER.info("[golem {} at {}] {}",
                    hasCustomName() && getCustomName() != null ? getCustomName().getString() : getId(),
                    blockPosition().toShortString(), entry);
        }
        log.add(entry);
        while (log.size() > LOG_SIZE) {
            log.remove(0);
        }
        ListTag list = new ListTag();
        for (String s : log) {
            list.add(StringTag.valueOf(s));
        }
        CompoundTag t = new CompoundTag();
        t.put("L", list);
        this.entityData.set(DATA_LOG, t);
    }

    public List<String> readSyncedLog() {
        ListTag list = this.entityData.get(DATA_LOG).getList("L", Tag.TAG_STRING);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    public void setActiveRule(int index) {
        if (this.entityData.get(DATA_ACTIVE_RULE) != index) {
            this.entityData.set(DATA_ACTIVE_RULE, index);
        }
    }

    public int readActiveRule() {
        return this.entityData.get(DATA_ACTIVE_RULE);
    }

    @Nullable
    private GolemRule activeRule() {
        int i = readActiveRule();
        return i >= 0 && i < rules.size() ? rules.get(i) : null;
    }

    public int readSyncedPower() {
        return this.entityData.get(DATA_POWER);
    }

    public int readSyncedCapacity() {
        return this.entityData.get(DATA_PARTS).getInt("Cap");
    }

    public int readUsedSlots() {
        return this.entityData.get(DATA_USED);
    }

    // =================================================================== flags
    public boolean hasFlag(int flag) {
        return (this.entityData.get(DATA_FLAGS) & flag) != 0;
    }

    private void setFlag(int flag, boolean on) {
        byte b = this.entityData.get(DATA_FLAGS);
        byte nb = (byte) (on ? b | flag : b & ~flag);
        if (nb != b) {
            this.entityData.set(DATA_FLAGS, nb);
        }
    }

    public boolean hasVines() {
        return hasFlag(FLAG_VINES);
    }

    /** Keeps the redstone signal on for a moment; Signal Plates under the golem output it. */
    public void emitSignal() {
        emitTicks = 20;
        setFlag(FLAG_EMITTING, true);
    }

    public boolean isEmittingSignal() {
        return hasFlag(FLAG_EMITTING);
    }

    // =================================================================== inventory
    public SimpleContainer getInventory() {
        return inventory;
    }

    public int getCarrySlots() {
        return stats.carry;
    }

    public int readCarrySlots() {
        return stats.carry;
    }

    public boolean isInventoryFull() {
        int used = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                used++;
            }
        }
        return used >= stats.carry;
    }

    public boolean isInventoryEmpty() {
        return inventory.isEmpty();
    }

    /** Adds a stack within the golem's carry limit. Returns what did not fit. */
    public ItemStack store(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int i = 0; i < INVENTORY_SIZE && !left.isEmpty(); i++) {
            ItemStack cur = inventory.getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, left)) {
                int move = Math.min(cur.getMaxStackSize() - cur.getCount(), left.getCount());
                cur.grow(move);
                left.shrink(move);
            }
        }
        for (int i = 0; i < INVENTORY_SIZE && !left.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                if (isInventoryFull()) {
                    break;
                }
                inventory.setItem(i, left.copy());
                left.setCount(0);
            }
        }
        inventory.setChanged();
        return left;
    }

    private boolean consume(java.util.function.Predicate<ItemStack> test) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && test.test(s)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Uses one Repair Kit from the golem's own inventory. */
    public boolean consumeRepairKit() {
        return consume(s -> s.is(ModItems.REPAIR_KIT.get()));
    }

    // =================================================================== links
    @Nullable
    public BlockPos getLink(GolemLinkerItem.Mode mode) {
        return links.get(mode);
    }

    public void setLink(GolemLinkerItem.Mode mode, @Nullable BlockPos pos) {
        if (pos == null) {
            links.remove(mode);
        } else {
            links.put(mode, pos.immutable());
        }
        syncLinks();
    }

    private void syncLinks() {
        CompoundTag t = new CompoundTag();
        links.forEach((m, p) -> t.putLong(m.name(), p.asLong()));
        this.entityData.set(DATA_LINKS, t);
    }

    @Nullable
    public BlockPos readSyncedLink(GolemLinkerItem.Mode mode) {
        CompoundTag t = this.entityData.get(DATA_LINKS);
        return t.contains(mode.name()) ? BlockPos.of(t.getLong(mode.name())) : null;
    }

    // =================================================================== rules
    public List<GolemRule> getRules() {
        return rules;
    }

    public int getRulesVersion() {
        return rulesVersion;
    }

    public void setRules(List<GolemRule> newRules) {
        this.rules = new ArrayList<>(newRules.subList(0, Math.min(GolemRule.MAX_RULES, newRules.size())));
        this.entityData.set(DATA_RULES, GolemRule.toTag(this.rules));
        this.rulesVersion++;
    }

    public List<GolemRule> readSyncedRules() {
        return GolemRule.fromTag(this.entityData.get(DATA_RULES));
    }

    // =================================================================== owner
    @Nullable
    public Player getOwner() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public UUID getOwnerUuid() {
        return level().isClientSide ? this.entityData.get(DATA_OWNER).orElse(null) : ownerUuid;
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
        this.entityData.set(DATA_OWNER, java.util.Optional.ofNullable(uuid));
    }

    /**
     * Whether this player may change the golem: its owner, anyone if it has no owner or the server
     * lets others edit, and operators.
     */
    public boolean canEdit(Player player) {
        UUID owner = getOwnerUuid();
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2)
                || com.xtrip.golemgarage.Config.OTHERS_CAN_EDIT.get();
    }

    public boolean isBroken() {
        return broken;
    }

    // =================================================================== combat
    @Override
    public boolean fireImmune() {
        return stats.fireproof || super.fireImmune();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || broken) {
            return super.hurt(source, amount);
        }
        if (isInvulnerableTo(source) || (fireImmune() && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE))) {
            return false;
        }
        if (hasFlag(FLAG_BREAKING)) {
            return false;
        }
        if (getHealth() <= amount && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            startBreakdown();
            return false;
        }
        boolean hit = super.hurt(source, amount);
        if (hit && amount > 0) {
            wearRandomPart(1);
            Entity attacker = source.getEntity();
            if (stats.thorns > 0 && attacker instanceof LivingEntity living && source.getDirectEntity() == attacker
                    && attacker != this) {
                living.hurt(damageSources().thorns(this), amount * stats.thorns);
            }
        }
        return hit;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        attackAnimationTick = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (stats.smite && target.getType().is(EntityTypeTags.UNDEAD)) {
            dmg *= 1.5f;
        }
        dmg *= 0.8f + random.nextFloat() * 0.4f;
        DamageSource src = damageSources().mobAttack(this);
        boolean hit = target.hurt(src, dmg);
        if (hit) {
            double kb = getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            if (target instanceof LivingEntity living) {
                living.knockback(0.4 + kb * 0.5, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                if (kb > 0) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.25 + 0.1 * kb, 0));
                }
            }
            if (stats.sweep) {
                for (Monster m : level().getEntitiesOfClass(Monster.class, target.getBoundingBox().inflate(2.5))) {
                    if (m != target && m.isAlive()) {
                        m.hurt(src, dmg * 0.5f);
                        m.knockback(0.4, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                    }
                }
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
                }
            }
            wearPart(PartSlot.ARMS, 1);
            drainPower(1);
        }
        crowdSound(com.xtrip.golemgarage.ModSounds.GOLEM_ATTACK.get(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.FIGHT, 0.7f, 0.9f + random.nextFloat() * 0.2f);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackAnimationTick = 10;
        } else if (id == EVENT_WORK) {
            workAnimationTick = 12;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public int getAttackAnimationTick() {
        return attackAnimationTick;
    }

    public int getWorkAnimationTick() {
        return workAnimationTick;
    }

    /** 0..1 while the golem slumps before breaking down (client side). */
    public float breakdownProgress(float partialTick) {
        return hasFlag(FLAG_BREAKING) ? Math.min(1f, (clientBreakingTicks + partialTick) / 12f) : 0f;
    }

    public boolean isBreakingDown() {
        return hasFlag(FLAG_BREAKING);
    }

    /** Dye color id of the trim paint, or -1 for none. */
    public int getPaint() {
        return entityData.get(DATA_PAINT);
    }

    public void setPaint(int dye) {
        entityData.set(DATA_PAINT, dye);
    }

    /** Starts the breakdown: sparks and a slump, then {@link #breakDown()} turns it into an item. */
    private void startBreakdown() {
        if (hasFlag(FLAG_BREAKING) || broken) {
            return;
        }
        setFlag(FLAG_BREAKING, true);
        breakingTicks = BREAKDOWN_TICKS;
        setHealth(1f);
        getNavigation().stop();
        setTarget(null);
        playSound(com.xtrip.golemgarage.ModSounds.GOLEM_BREAKDOWN.get(), 1f, 1f);
    }

    private void tickBreakdown() {
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        if (level() instanceof ServerLevel sl && breakingTicks % 4 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.6, getZ(), 6, 0.5, 0.6, 0.5, 0.15);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.4, getZ(), 3, 0.4, 0.4, 0.4, 0.01);
        }
        if (--breakingTicks <= 0) {
            setFlag(FLAG_BREAKING, false);
            breakDown();
        }
    }

    // =================================================================== movement
    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return !stats.noFallDamage && super.causeFallDamage(distance, mult, source);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return stats.waterWalk && (fluid.is(FluidTags.WATER) || (fireImmune() && fluid.is(FluidTags.LAVA)));
    }

    @Override
    public boolean onClimbable() {
        return stats.climb && hasFlag(FLAG_CLIMBING);
    }

    @Override
    public void jumpFromGround() {
        if (!stats.noJump) {
            super.jumpFromGround();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.xtrip.golemgarage.ModSounds.GOLEM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.xtrip.golemgarage.ModSounds.GOLEM_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 900;
    }

    @Override
    protected float getSoundVolume() {
        return 0.45f;
    }

    /** Ambient beeps share a budget with nearby golems, so a crowd doesn't chatter. */
    @Override
    public void playAmbientSound() {
        crowdSound(getAmbientSound(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.AMBIENT, getSoundVolume(), getVoicePitch());
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        crowdSound(getHurtSound(source), com.xtrip.golemgarage.golem.SoundCrowd.Kind.FIGHT, getSoundVolume(), getVoicePitch());
    }

    /** Plays a sound quieter (or not at all) when many golems nearby just made the same kind of sound. */
    private void crowdSound(@Nullable SoundEvent sound, com.xtrip.golemgarage.golem.SoundCrowd.Kind kind, float volume, float pitch) {
        if (sound == null || isSilent()) {
            return;
        }
        float f = com.xtrip.golemgarage.golem.SoundCrowd.factor(this, kind);
        if (f > 0f) {
            playSound(sound, volume * f, pitch);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        PartInstance legs = parts.get(PartSlot.LEGS);
        if (legs != null && legs.shape().has(ShapeAbility.NO_JUMP)) {
            crowdSound(com.xtrip.golemgarage.ModSounds.GOLEM_STEP_WHEELS.get(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.STEP, 0.35f, 0.9f + random.nextFloat() * 0.2f);
            return;
        }
        if (legs != null && legs.shape().has(ShapeAbility.TREADS)) {
            crowdSound(com.xtrip.golemgarage.ModSounds.GOLEM_STEP_TREADS.get(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.STEP, 0.4f, 1f);
        } else {
            crowdSound(com.xtrip.golemgarage.ModSounds.GOLEM_STEP.get(), com.xtrip.golemgarage.golem.SoundCrowd.Kind.STEP, 0.45f, legs != null && legs.shape().has(ShapeAbility.CLIMB) ? 1.3f : 0.95f + random.nextFloat() * 0.1f);
        }
    }

    // =================================================================== breakdown
    /** Turns the golem into a Broken Golem item and sends it to the owner. */
    public void breakDown() {
        if (broken || level().isClientSide) {
            return;
        }
        broken = true;
        log(com.xtrip.golemgarage.golem.GolemText.of("log.broke_down"));
        for (PartInstance p : parts.values()) {
            p.wear(Math.max(1, p.maxDurability() * 15 / 100));
        }
        ItemStack stack = GolemItem.createStack(this, true);
        Player owner = getOwner();
        com.xtrip.golemgarage.advancement.ModTriggers.award(owner, "breakdown");
        if (owner != null && owner.getInventory().add(stack)) {
            owner.displayClientMessage(Component.translatable("golemgarage.msg.broke_down")
                    .withStyle(ChatFormatting.RED), false);
        } else {
            spawnAtLocation(stack);
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.2, getZ(), 2, 0.4, 0.4, 0.4, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2, getZ(), 20, 0.5, 0.8, 0.5, 0.02);
        }
        playSound(SoundEvents.IRON_GOLEM_DEATH, 1f, 0.8f);
        discard();
    }

    // =================================================================== interaction
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            if (!player.isShiftKeyDown() && held.isEmpty() && canEdit(player)) {
                ClientHooks.openGolemScreen(this);
            }
            return InteractionResult.SUCCESS;
        }

        if (!canEdit(player) && !(player.isShiftKeyDown() && held.isEmpty())) {
            say(player, Component.translatable("golemgarage.msg.not_yours"), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof GolemLinkerItem) {
            GolemLinkerItem.applyTo(this, held, player);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            tryPickUp(player);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof com.xtrip.golemgarage.item.ModuleItem mi) {
            installModule(player, held, mi.module());
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof BlueprintItem) {
            useBlueprint(player, held);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.REPAIR_KIT.get())) {
            useRepairKit();
            consumeHeld(player, held);
            say(player, Component.translatable("golemgarage.msg.kit"), ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof net.minecraft.world.item.DyeItem dye) {
            if (getPaint() != dye.getDyeColor().getId()) {
                setPaint(dye.getDyeColor().getId());
                consumeHeld(player, held);
                com.xtrip.golemgarage.advancement.ModTriggers.award(player, "paint");
                playSound(SoundEvents.DYE_USE, 1f, 1f);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.WATER_BUCKET) && getPaint() >= 0) {
            setPaint(-1);
            playSound(SoundEvents.BUCKET_EMPTY, 0.8f, 1.3f);
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.HONEYCOMB)) {
            boolean any = false;
            for (PartInstance p : parts.values()) {
                any |= p.wax();
            }
            if (any) {
                consumeHeld(player, held);
                refresh();
                playSound(SoundEvents.HONEYCOMB_WAX_ON, 1f, 1f);
                burst(ParticleTypes.WAX_ON);
                return InteractionResult.SUCCESS;
            }
            say(player, Component.translatable("golemgarage.msg.no_wax"), ChatFormatting.GRAY);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ItemTags.AXES)) {
            PartInstance most = parts.values().stream()
                    .filter(p -> p.waxed() || p.oxidation() > 0)
                    .max((a, b) -> Integer.compare(a.oxidation() + (a.waxed() ? 10 : 0), b.oxidation() + (b.waxed() ? 10 : 0)))
                    .orElse(null);
            if (most != null) {
                boolean wasWaxed = most.waxed();
                most.scrape();
                refresh();
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                playSound(wasWaxed ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE, 1f, 1f);
                burst(wasWaxed ? ParticleTypes.WAX_OFF : ParticleTypes.SCRAPE);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (held.is(Items.VINE) && !hasVines()) {
            setFlag(FLAG_VINES, true);
            consumeHeld(player, held);
            playSound(SoundEvents.VINE_PLACE, 1f, 1f);
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.SHEARS) && hasVines()) {
            setFlag(FLAG_VINES, false);
            spawnAtLocation(new ItemStack(Items.VINE));
            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            playSound(SoundEvents.SHEEP_SHEAR, 1f, 1f);
            return InteractionResult.SUCCESS;
        }
        PartInstance part = PartInstance.fromStack(held);
        if (part != null && part.blockedReason() != null) {
            say(player, part.blockedReason(), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (part != null) {
            PartInstance old = equipPart(part);
            consumeHeld(player, held);
            giveBack(player, old);
            playSound(com.xtrip.golemgarage.ModSounds.GOLEM_PART_FIT.get(), 0.9f, 1f);
            burst(ParticleTypes.CRIT);
            com.xtrip.golemgarage.advancement.ModTriggers.award(player, "fit_part");
            if (stats.matchedSet) {
                com.xtrip.golemgarage.advancement.ModTriggers.award(player, "matched_set");
            }
            if (part.material().tier() >= 5) {
                com.xtrip.golemgarage.advancement.ModTriggers.award(player, "endgame_part");
            }
            player.displayClientMessage(Component.translatable("golemgarage.msg.fitted", part.toStack().getHoverName()), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void burst(ParticleOptions particle) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(particle, getX(), getY() + 1.4, getZ(), 14, 0.6, 0.9, 0.6, 0.05);
        }
    }

    private static void consumeHeld(Player player, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    private static void say(Player player, Component text, ChatFormatting color) {
        player.displayClientMessage(text.copy().withStyle(color), true);
    }

    private void giveBack(Player player, @Nullable PartInstance old) {
        if (old == null) {
            return;
        }
        ItemStack stack = old.toStack();
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
    }

    /** Removes a part and hands it to the player. Used by the status screen. */
    public void ejectPart(Player player, PartSlot slot) {
        PartInstance old = removePart(slot);
        giveBack(player, old);
        if (old != null) {
            playSound(com.xtrip.golemgarage.ModSounds.GOLEM_PART_FIT.get(), 0.8f, 0.8f);
        }
    }

    private void useBlueprint(Player player, ItemStack blueprint) {
        if (BlueprintItem.isEmpty(blueprint)) {
            if (parts.isEmpty()) {
                say(player, Component.translatable("golemgarage.msg.bp_empty"), ChatFormatting.RED);
                return;
            }
            BlueprintItem.write(blueprint, parts, rules);
            say(player, Component.translatable("golemgarage.msg.bp_saved", parts.size(), rules.size()), ChatFormatting.AQUA);
            return;
        }
        int fitted = 0;
        int missing = 0;
        for (PartSlot slot : PartSlot.values()) {
            BlueprintItem.Entry e = BlueprintItem.entry(blueprint, slot);
            if (e == null) {
                continue;
            }
            PartInstance cur = parts.get(slot);
            if (cur != null && cur.shape() == e.shape() && cur.materialId().equals(e.material())) {
                continue;
            }
            PartInstance found = takePart(player, e);
            if (found == null) {
                missing++;
                continue;
            }
            giveBack(player, equipPart(found));
            fitted++;
        }
        List<GolemRule> planRules = BlueprintItem.rules(blueprint);
        if (!planRules.isEmpty()) {
            setRules(planRules);
        }
        say(player, Component.translatable("golemgarage.msg.bp_used", fitted, missing),
                missing > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
    }

    @Nullable
    private static PartInstance takePart(Player player, BlueprintItem.Entry e) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            PartInstance p = PartInstance.fromStack(s);
            if (p != null && p.shape() == e.shape() && p.materialId().equals(e.material()) && p.blockedReason() == null) {
                s.shrink(1);
                return p;
            }
        }
        return null;
    }

    /** The work area from its two Linker corners (full height range of 6 blocks), or null if it has none. */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.phys.AABB area() {
        BlockPos a = getLink(GolemLinkerItem.Mode.AREA_1);
        BlockPos b = getLink(GolemLinkerItem.Mode.AREA_2);
        if (a == null || b == null) {
            return null;
        }
        return new net.minecraft.world.phys.AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()) - 1,
                Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 4,
                Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** True when there's no area, or the point is inside it. */
    public boolean inArea(Vec3 p) {
        net.minecraft.world.phys.AABB area = area();
        return area == null || area.contains(p);
    }

    /** The action of the rule it's running now, or null. */
    @org.jetbrains.annotations.Nullable
    public GolemRule.Action activeAction() {
        int i = entityData.get(DATA_ACTIVE_RULE);
        return i >= 0 && i < rules.size() ? rules.get(i).action() : null;
    }

    // =================================================================== modules
    public List<com.xtrip.golemgarage.golem.GolemModule> getModules() {
        return modules;
    }

    private net.minecraft.nbt.ListTag modulesTag() {
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (var m : modules) {
            l.add(StringTag.valueOf(m.id));
        }
        return l;
    }

    private void readModules(CompoundTag tag) {
        modules.clear();
        String key = tag.contains("Modules") ? "Modules" : "Mods";
        for (net.minecraft.nbt.Tag t : tag.getList(key, net.minecraft.nbt.Tag.TAG_STRING)) {
            var m = com.xtrip.golemgarage.golem.GolemModule.byId(t.getAsString());
            if (m != null && modules.size() < com.xtrip.golemgarage.golem.GolemModule.SOCKETS) {
                modules.add(m);
            }
        }
    }

    /** Installs a module from the held item. */
    private boolean installModule(Player player, ItemStack held, com.xtrip.golemgarage.golem.GolemModule m) {
        if (m.isDisabled()) {
            say(player, Component.translatable("golemgarage.msg.module_off"), ChatFormatting.RED);
            return false;
        }
        if (modules.contains(m)) {
            say(player, Component.translatable("golemgarage.msg.module_dupe"), ChatFormatting.YELLOW);
            return false;
        }
        if (modules.size() >= com.xtrip.golemgarage.golem.GolemModule.SOCKETS) {
            say(player, Component.translatable("golemgarage.msg.module_full"), ChatFormatting.YELLOW);
            return false;
        }
        modules.add(m);
        consumeHeld(player, held);
        refresh();
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "module");
        if (modules.size() >= com.xtrip.golemgarage.golem.GolemModule.SOCKETS) {
            com.xtrip.golemgarage.advancement.ModTriggers.award(player, "two_modules");
        }
        playSound(com.xtrip.golemgarage.ModSounds.GOLEM_MODULE.get(), 1f, 1f);
        log(com.xtrip.golemgarage.golem.GolemText.of("log.module", com.xtrip.golemgarage.golem.GolemText.key("item.golemgarage." + m.itemId())));
        return true;
    }

    /** Takes a module out and gives its item back. */
    public void removeModule(Player player, int index) {
        if (index < 0 || index >= modules.size()) {
            return;
        }
        var m = modules.remove(index);
        refresh();
        ItemStack stack = new ItemStack(com.xtrip.golemgarage.item.ModItems.MODULES.get(m).get());
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8f, 0.8f);
    }

    /** Magnet, regeneration and self-repair modules. */
    private void moduleTick() {
        if (stats.magnet && tickCount % 5 == 0 && !isInventoryFull()) {
            for (net.minecraft.world.entity.item.ItemEntity it : level().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(6),
                    e -> e.isAlive() && !e.hasPickUpDelay())) {
                Vec3 to = position().add(0, 1, 0).subtract(it.position());
                if (to.lengthSqr() < 2.5) {
                    debug("Magnet pulled in " + it.getItem().getCount() + " " + it.getItem().getHoverName().getString());
                    ItemStack rest = store(it.getItem());
                    if (rest.isEmpty()) {
                        it.discard();
                    } else {
                        it.setItem(rest);
                    }
                } else {
                    it.setDeltaMovement(it.getDeltaMovement().scale(0.5).add(to.normalize().scale(0.25)));
                }
            }
        }
        if (stats.regen && tickCount % 80 == 0 && getHealth() < getMaxHealth()) {
            heal(1f);
            if (getHealth() >= getMaxHealth()) {
                debug("Regen module: back to full health");
            }
        }
        if (stats.selfRepair && tickCount % 600 == 0) {
            boolean any = false;
            for (PartInstance p : parts.values()) {
                if (p.damage() > 0) {
                    p.repair(2);
                    any = true;
                }
            }
            if (any) {
                refresh();
            }
        }
    }

    /** False (and tells the player) when they're at the server's golem limit. */
    public static boolean canPlaceAnother(net.minecraft.server.level.ServerLevel level, UUID owner, Player player) {
        return canPlaceAnother(level, owner, player, null);
    }

    /** Checks the per-player and per-chunk golem limits; {@code at} is where the new golem would go. */
    public static boolean canPlaceAnother(net.minecraft.server.level.ServerLevel level, UUID owner, Player player,
                                          @Nullable BlockPos at) {
        int perChunk = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.MAX_GOLEMS_PER_CHUNK::get, 0);
        if (perChunk > 0 && at != null) {
            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(at);
            int here = level.getEntities(ModEntities.GOLEM.get(), new net.minecraft.world.phys.AABB(
                    cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                    cp.getMaxBlockX() + 1, level.getMaxBuildHeight(), cp.getMaxBlockZ() + 1), g -> true).size();
            if (here >= perChunk) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("golemgarage.msg.chunk_limit", here, perChunk)
                            .withStyle(ChatFormatting.RED), true);
                }
                return false;
            }
        }
        int max = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.MAX_GOLEMS_PER_PLAYER::get, 0);
        if (max <= 0 || owner == null) {
            return true;
        }
        // Count every golem the server knows of, loaded or not.
        int count = com.xtrip.golemgarage.golem.GolemRegistry.get(level.getServer()).forOwner(owner).size();
        if (count >= max) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("golemgarage.msg.limit", count, max).withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        return true;
    }

    /** Picks the golem up into the player's inventory, if it's theirs (or nobody's). */
    public void tryPickUp(Player player) {
        if (getOwnerUuid() != null && !getOwnerUuid().equals(player.getUUID())
                && !com.xtrip.golemgarage.Config.OTHERS_CAN_PICK_UP.get()) {
            say(player, Component.translatable("golemgarage.msg.not_yours"), ChatFormatting.RED);
            return;
        }
        pickUp(player);
    }

    private void pickUp(Player player) {
        ItemStack stack = GolemItem.createStack(this, false);
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
        playSound(SoundEvents.ITEM_PICKUP, 1f, 0.8f);
        discard();
    }

    // =================================================================== save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        exportData(tag);
        if (lightPos != null) {
            tag.putLong("LightPos", lightPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        importData(tag);
        if (tag.contains("LightPos")) {
            lightPos = BlockPos.of(tag.getLong("LightPos"));
        }
    }

    /** Everything needed to rebuild this golem: used for saves, golem items and the Garage. */
    public void exportData(CompoundTag tag) {
        tag.put("Parts", GolemData.writeParts(parts));
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putBoolean("Broken", broken);
        tag.put("Rules", GolemRule.toTag(rules).get("Rules"));
        tag.putInt("Power", power);
        tag.putFloat("HealthLeft", getHealth());
        tag.putBoolean("Vines", hasVines());
        tag.put("Modules", modulesTag());
        tag.putInt("Paint", getPaint());
        tag.putInt("Group", getGroup());
        tag.putString("Mode", getMode().id());
        tag.putBoolean("Paused", paused);
        CompoundTag l = new CompoundTag();
        links.forEach((m, p) -> l.putLong(m.name(), p.asLong()));
        tag.put("Links", l);
        if (hasCustomName() && getCustomName() != null) {
            tag.putString("Name", getCustomName().getString());
        }
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, level().registryAccess());
    }

    public void importData(CompoundTag tag) {
        parts.clear();
        parts.putAll(GolemData.readParts(tag));
        if (tag.hasUUID("Owner")) {
            setOwnerUuid(tag.getUUID("Owner"));
        }
        broken = tag.getBoolean("Broken");
        List<GolemRule> loaded = GolemRule.fromTag(tag);
        setRules(loaded.isEmpty() ? GolemRule.followerPreset() : loaded);
        links.clear();
        CompoundTag l = tag.getCompound("Links");
        for (GolemLinkerItem.Mode m : GolemLinkerItem.Mode.MARKS) {
            if (l.contains(m.name())) {
                links.put(m, BlockPos.of(l.getLong(m.name())));
            }
        }
        syncLinks();
        setFlag(FLAG_VINES, tag.getBoolean("Vines"));
        setPaint(tag.contains("Paint") ? tag.getInt("Paint") : -1);
        setGroup(tag.getInt("Group"));
        entityData.set(DATA_MODE, 0);
        for (com.xtrip.golemgarage.golem.GolemMode m : com.xtrip.golemgarage.golem.GolemMode.values()) {
            if (m.id().equals(tag.getString("Mode"))) {
                entityData.set(DATA_MODE, m.ordinal());
            }
        }
        paused = tag.getBoolean("Paused");
        if (tag.contains("Modules")) {
            readModules(tag);
        } else {
            modules.clear();
        }
        if (tag.contains("Name") && !hasCustomName()) {
            setCustomName(Component.literal(tag.getString("Name")));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, level().registryAccess());
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, items.get(i));
        }
        stats = GolemStats.compute(parts).withModules(modules);
        power = tag.contains("Power") ? Math.min(tag.getInt("Power"), stats.capacity) : stats.capacity;
        refresh();
        float hp = tag.contains("HealthLeft") ? tag.getFloat("HealthLeft") : getMaxHealth();
        setHealth(hp <= 0 ? getMaxHealth() : Math.min(getMaxHealth(), hp));
    }
}
