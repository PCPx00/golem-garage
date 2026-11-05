package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.ModSounds;
import com.xtrip.golemgarage.item.ModDataComponents;
import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Wasteland Rig: a drivable car built from scrap. W/S drive, A/D steer. It burns furnace fuel (it crawls
 * without), carries a driver, a passenger and a golem or cub on the back deck, and takes three upgrades: a
 * spiked ram that hurts mobs it hits at speed, armour plating, and a roof turret that fires at hostile mobs while
 * a golem rides on the deck to work it. Iron, scrap and repair kits fix it, and so does parking it in a finished
 * Garage. Sneak + right-click with an empty hand packs it back into an item.
 */
public class RigEntity extends Mob implements net.minecraft.world.entity.PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.BYTE);
    /** Ticks of nitro left (Interceptor) and of cannon flash (Juggernaut), for speed and the renderer. */
    private static final EntityDataAccessor<Integer> BOOST = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLASH = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.INT);
    private int boostCooldown;
    private boolean wasOn;
    private int engineSoundAt;
    private static final EntityDataAccessor<Byte> UPGRADES = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(RigEntity.class, EntityDataSerializers.FLOAT);

    public static final int RAM = 1, TURRET = 2, PLATING = 4;
    /** Rigs that came with a raid: they drive themselves at the base and drop their crew. */
    public static final String ROGUE_TAG = "golemgarage_rogue_rig";
    public static final int MAX_FUEL = 20 * 60 * 20;
    /** The car model is drawn at this scale. */
    public static final float SCALE = 1.3f;

    @Nullable
    private UUID owner;
    private int turretCooldown;
    /** Client: how far the wheels have turned, for the renderer. */
    public float wheelAngle;
    public float wheelAngleO;
    private Vec3 lastPos = Vec3.ZERO;

    public RigEntity(EntityType<? extends RigEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.05)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUEL, 0);
        builder.define(UPGRADES, (byte) 0);
        builder.define(TURRET_YAW, 0f);
        builder.define(VARIANT, (byte) 0);
        builder.define(BOOST, 0);
        builder.define(FLASH, 0);
    }

    // =================================================================== state
    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        entityData.set(FUEL, Mth.clamp(fuel, 0, MAX_FUEL));
    }

    public boolean has(int upgrade) {
        return (entityData.get(UPGRADES) & upgrade) != 0;
    }

    public void fit(int upgrade) {
        entityData.set(UPGRADES, (byte) (entityData.get(UPGRADES) | upgrade));
        if (upgrade == PLATING) {
            getAttribute(Attributes.ARMOR).setBaseValue(variant().armor + 10.0);
        }
    }

    public RigVariant variant() {
        return RigVariant.of(entityData.get(VARIANT));
    }

    /** Makes this rig a War Rig, Interceptor, Juggernaut or Skimmer, with that rig's toughness and speed. */
    public void setVariant(RigVariant v) {
        entityData.set(VARIANT, (byte) v.ordinal());
        boolean full = getHealth() >= getMaxHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(v.health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(v.speed);
        getAttribute(Attributes.ARMOR).setBaseValue(v.armor + (has(PLATING) ? 10.0 : 0.0));
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(v.step);
        if (full || getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    public boolean boosting() {
        return entityData.get(BOOST) > 0;
    }

    /** Cannon flash, 1 just after a shot fading to 0. */
    public float flash(float partial) {
        int f = entityData.get(FLASH);
        return f <= 0 ? 0 : Math.max(0, (f - partial) / 6f);
    }

    public float turretYaw() {
        return entityData.get(TURRET_YAW);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return owner;
    }

    public boolean engineOn() {
        return getFuel() > 0 && (getControllingPassenger() != null || isRogue() && !getPassengers().isEmpty() || golemDriving());
    }

    /** A golem drives when no player is aboard: it follows its owner, so a line of rigs makes a convoy. */
    public boolean golemDriving() {
        return !isRogue() && getControllingPassenger() == null && driver() != null;
    }

    @Nullable
    private GolemEntity driver() {
        for (Entity p : getPassengers()) {
            if (p instanceof GolemEntity g && !g.isRogue() && !g.isBroken()) {
                return g;
            }
        }
        return null;
    }

    /** Parked: golem drivers stay put (sneak + use with an empty hand on a crewed rig toggles it). */
    public boolean parked() {
        return getPersistentData().getBoolean("GGParked");
    }

    private void tickConvoy() {
        GolemEntity g = driver();
        if (g == null || getFuel() <= 0 || parked() || g.getOwnerUuid() == null) {
            if (getNavigation().isInProgress() && !isRogue()) {
                getNavigation().stop();
            }
            return;
        }
        Player owner = level().getPlayerByUUID(g.getOwnerUuid());
        if (owner == null || owner.level() != level() || distanceToSqr(owner) > 96 * 96) {
            getNavigation().stop();
            return;
        }
        // Follow the owner, or the rig the owner is driving, keeping a car length or two back.
        Entity lead = owner.getVehicle() instanceof RigEntity r && r != this ? r : owner;
        double d = distanceToSqr(lead);
        if (d < 7 * 7) {
            getNavigation().stop();
            return;
        }
        if (tickCount % 10 == 0 || getNavigation().isDone()) {
            Vec3 step = d > 20 * 20 ? com.xtrip.golemgarage.golem.RogueMarchGoal.hop(this, lead.blockPosition(), 14)
                    : lead.position();
            getNavigation().moveTo(step.x, step.y, step.z, d > 14 * 14 ? 1.35 : 1.1);
        }
        // Face where it's heading so the body turns like a car.
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistanceSqr() > 0.002) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * 57.2958) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 8f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    public boolean isRogue() {
        return getTags().contains(ROGUE_TAG);
    }

    public void setRaidTarget(BlockPos pos) {
        getPersistentData().putLong("GGRaidTarget", pos.asLong());
    }

    /** A raid rig that has dropped its crew. */
    public boolean hasArrived() {
        return getPersistentData().getBoolean("GGArrived");
    }

    private void tickRogue() {
        if (!getPersistentData().contains("GGRaidTarget") || hasArrived()) {
            return;
        }
        BlockPos t = BlockPos.of(getPersistentData().getLong("GGRaidTarget"));
        if (blockPosition().distSqr(t) < 14 * 14 || tickCount > 20 * 60 || getPassengers().isEmpty()) {
            getPersistentData().putBoolean("GGArrived", true);
            getNavigation().stop();
            for (Entity p : getPassengers()) {
                p.stopRiding();
            }
            playSound(SoundEvents.IRON_DOOR_OPEN, 1f, 0.6f);
            return;
        }
        if (getNavigation().isDone() || tickCount % 30 == 0) {
            Vec3 step = com.xtrip.golemgarage.golem.RogueMarchGoal.hop(this, t, 12);
            getNavigation().moveTo(step.x, step.y, step.z, 1.3);
        }
    }

    // =================================================================== driving
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity p : getPassengers()) {
            if (p instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private List<Entity> riders(boolean players) {
        return getPassengers().stream().filter(p -> (p instanceof Player) == players).toList();
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        // A and D steer; turning is sharper at low speed and while reversing it turns the other way.
        float steer = player.xxa;
        float forward = player.zza;
        if (steer != 0 && (forward != 0 || getDeltaMovement().horizontalDistanceSqr() > 0.001)) {
            float rate = 5.5f * (forward < 0 ? -1 : 1);
            setYRot(getYRot() - steer * rate);
        }
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        setXRot(0);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza;
        if (forward < 0) {
            forward *= 0.4f;
        }
        return new Vec3(0, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float speed = (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (boosting()) {
            speed *= 1.6f;
        }
        return getFuel() > 0 ? speed : speed * 0.25f;
    }

    /**
     * Seats: the first player drives, a second player rides shotgun. Golems and cubs take the back deck first,
     * then the shotgun seat if it's free. Rig space here is z forward, x to the left.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        float s = SCALE;
        boolean golem = passenger instanceof GolemEntity;
        // Seat height and depth, and where the deck is, for each rig.
        double seatY, seatZ, deckY, deckZ;
        switch (variant()) {
            case INTERCEPTOR -> {
                seatY = 0.3; seatZ = -0.15; deckY = golem ? 0.5 : 0.66; deckZ = -0.9;
            }
            case JUGGERNAUT -> {
                seatY = 0.66; seatZ = 0.55; deckY = golem ? 0.72 : 0.82; deckZ = -0.95;
            }
            case SKIMMER -> {
                seatY = 0.42; seatZ = 0.05; deckY = golem ? 0.45 : 0.6; deckZ = -0.45;
            }
            default -> {
                seatY = 0.38; seatZ = -0.25; deckY = golem ? 0.38 : 0.72; deckZ = -0.95;
            }
        }
        Vec3 driver = new Vec3(0.22 * s, seatY * s, seatZ * s);
        Vec3 shotgun = new Vec3(-0.22 * s, seatY * s, seatZ * s);
        Vec3 deck = new Vec3(0, deckY * s, deckZ * s);
        if (passenger instanceof Player) {
            return riders(true).indexOf(passenger) == 0 ? driver : shotgun;
        }
        return riders(false).indexOf(passenger) == 0 ? deck : riders(true).size() < 2 ? shotgun : driver;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 3 && riders(passenger instanceof Player).size() < 2;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        if (passenger instanceof Mob mob) {
            mob.yBodyRot = getYRot();
            mob.setYRot(getYRot());
            mob.yHeadRot = getYRot();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return isRogue() && distance > 96 * 96;
    }

    @Override
    protected void registerGoals() {
    }

    // =================================================================== ticking
    @Override
    public void tick() {
        super.tick();
        Vec3 moved = position().subtract(lastPos);
        lastPos = position();
        double speed = moved.horizontalDistance();
        if (level().isClientSide) {
            wheelAngleO = wheelAngle;
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
            double sign = Math.signum(moved.x * fwd.x + moved.z * fwd.z);
            wheelAngle += (float) (sign * speed / 0.35 * 57.3);
            clientEffects(speed);
            return;
        }
        if (engineOn() && speed > 0.02) {
            setFuel(getFuel() - 1);
        }
        // Engine note: a start-up cough, then a burble (or the Skimmer's fan) pitched up with speed.
        boolean on = engineOn();
        if (on && !wasOn) {
            playSound(variant() == RigVariant.SKIMMER ? com.xtrip.golemgarage.ModSounds.RIG_FAN.get()
                    : com.xtrip.golemgarage.ModSounds.RIG_START.get(), 1f, 1f);
            engineSoundAt = tickCount + 26;
        } else if (on && tickCount >= engineSoundAt) {
            float pitch = (variant() == RigVariant.JUGGERNAUT ? 0.6f : variant() == RigVariant.INTERCEPTOR ? 1.0f : 0.8f)
                    + (float) Math.min(0.6, speed * 1.2);
            playSound(variant() == RigVariant.SKIMMER ? com.xtrip.golemgarage.ModSounds.RIG_FAN.get()
                    : com.xtrip.golemgarage.ModSounds.RIG_ENGINE.get(), 0.7f, pitch);
            engineSoundAt = tickCount + Math.max(10, Math.round(17 / pitch));
        }
        wasOn = on;
        if (entityData.get(BOOST) > 0) {
            entityData.set(BOOST, entityData.get(BOOST) - 1);
        }
        if (entityData.get(FLASH) > 0) {
            entityData.set(FLASH, entityData.get(FLASH) - 1);
        }
        if (boostCooldown > 0) {
            boostCooldown--;
        }
        if (isRogue()) {
            tickRogue();
        } else {
            tickConvoy();
        }
        if (speed > 0.2) {
            ram(speed);
        }
        if (variant() == RigVariant.JUGGERNAUT) {
            tickCannon();
        } else {
            tickTurret();
        }
    }

    private void clientEffects(double speed) {
        if (!engineOn()) {
            return;
        }
        Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-0.9 * SCALE);
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(0.36 * SCALE);
        RigVariant v = variant();
        if (v == RigVariant.SKIMMER) {
            // Spray and bubbles on water, dust on land, blown back by the fan.
            boolean water = isInWater();
            for (int k = 0; k < (speed > 0.1 ? 3 : 1); k++) {
                double ox = (random.nextDouble() - 0.5) * 1.6, oz = (random.nextDouble() - 0.5) * 1.6;
                level().addParticle(water ? ParticleTypes.SPLASH : ParticleTypes.POOF, getX() + ox, getY() + 0.1,
                        getZ() + oz, back.x * 0.05, 0.02, back.z * 0.05);
            }
            if (water && speed > 0.1) {
                level().addParticle(ParticleTypes.BUBBLE_POP, getX() + back.x * 1.3, getY() + 0.15, getZ() + back.z * 1.3, 0, 0.02, 0);
            }
            return;
        }
        if (boosting()) {
            for (int k = -1; k <= 1; k += 2) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + back.x * 1.4 + side.x * k * 0.5, getY() + 0.4,
                        getZ() + back.z * 1.4 + side.z * k * 0.5, back.x * 0.1, 0.01, back.z * 0.1);
            }
        }
        if (v == RigVariant.JUGGERNAUT) {
            back = back.scale(1.1);
            side = side.scale(0.75);
            if (tickCount % (speed > 0.05 ? 4 : 8) == 0) {
                int k = (tickCount / 4) % 2 == 0 ? -1 : 1;
                level().addParticle(ParticleTypes.SMOKE, getX() + back.x + side.x * k, getY() + 1.75 * SCALE,
                        getZ() + back.z + side.z * k, 0, 0.05, 0);
            }
            if (speed > 0.05 && tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + side.x * 2.5, getY() + 0.1, getZ() + side.z * 2.5, 0, 0.005, 0);
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() - side.x * 2.5, getY() + 0.1, getZ() - side.z * 2.5, 0, 0.005, 0);
            }
            return;
        }
        if (v == RigVariant.INTERCEPTOR) {
            // Smoke puffs from the zoomie pipes along each side.
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot()).scale(0.45 * SCALE);
            Vec3 out = side.scale(1.75);
            if (tickCount % (speed > 0.1 ? 3 : 6) == 0) {
                for (int k = -1; k <= 1; k += 2) {
                    level().addParticle(ParticleTypes.SMOKE, getX() + fwd.x + out.x * k, getY() + 0.55 * SCALE,
                            getZ() + fwd.z + out.z * k, out.x * k * 0.05, 0.03, out.z * k * 0.05);
                }
            }
            return;
        }
        if (tickCount % (speed > 0.1 ? 2 : 5) == 0) {
            for (int k = -1; k <= 1; k += 2) {
                level().addParticle(ParticleTypes.SMOKE, getX() + back.x * 0.7 + side.x * k, getY() + 1.5 * SCALE,
                        getZ() + back.z * 0.7 + side.z * k, 0, 0.06, 0);
            }
        }
        if (speed > 0.15 && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + back.x, getY() + 0.2, getZ() + back.z, 0, 0.01, 0);
        }
    }

    /** At speed the rig hits whatever is in front of it: hard with the ram fitted, a bump without. */
    private void ram(double speed) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        AABB front = getBoundingBox().move(fwd.scale(0.8)).inflate(0.2, 0, 0.2);
        List<Entity> hit = level().getEntities(this, front, e -> e instanceof LivingEntity le && le.isAlive()
                && !hasPassenger(e) && !friendly(le));
        if (hit.isEmpty()) {
            return;
        }
        float damage = (float) ((has(RAM) ? 26 : 8) * speed * variant().ram);
        for (Entity e : hit) {
            if (e.hurt(damageSources().mobAttack(this), damage)) {
                e.push(fwd.x * speed * 2, 0.35, fwd.z * speed * 2);
            }
        }
        playSound(SoundEvents.ANVIL_LAND, 0.4f, 0.6f);
        if (random.nextInt(3) == 0) {
            playSound(com.xtrip.golemgarage.ModSounds.RIG_HORN.get(), 1f, 0.9f);
        }
        setDeltaMovement(getDeltaMovement().scale(0.5));
    }

    /** Never hurt players (unless they're hostile mobs), your golems, cubs or other rigs. */
    private boolean friendly(LivingEntity e) {
        if (isRogue()) {
            return e instanceof GolemEntity g && g.isRogue() || e instanceof RigEntity
                    || e instanceof Player p && (p.isCreative() || p.isSpectator());
        }
        return e instanceof Player || e instanceof GolemEntity g && !g.isRogue() || e instanceof CubEntity || e instanceof RigEntity;
    }

    /** With a golem on board to work it, the roof turret picks off hostile mobs. */
    private void tickTurret() {
        if (!has(TURRET) || riders(false).stream().noneMatch(p -> p instanceof GolemEntity)) {
            return;
        }
        if (--turretCooldown > 0) {
            return;
        }
        Vec3 gun = position().add(0, 1.5 * SCALE, 0);
        LivingEntity target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(18),
                        e -> enemy(e) && hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) {
            turretCooldown = 10;
            return;
        }
        Vec3 aim = target.getEyePosition().subtract(gun);
        // Turret angle in car space (about +y, from forward): forward is yaw + 90 in atan2 terms.
        entityData.set(TURRET_YAW, (float) (getYRot() + 90 - Math.toDegrees(Math.atan2(aim.z, aim.x))));
        Arrow arrow = new Arrow(EntityType.ARROW, level());
        arrow.setPos(gun.add(aim.normalize().scale(1.2)));
        arrow.setOwner(this);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(5.0);
        arrow.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.08, aim.z, 3.0f, 1.5f);
        level().addFreshEntity(arrow);
        playSound(SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.8f + random.nextFloat() * 0.2f);
        turretCooldown = 14;
    }

    /** The Juggernaut's cannon: a heavy shell at the nearest hostile every few seconds while it's crewed. */
    private void tickCannon() {
        if (getPassengers().isEmpty() || --turretCooldown > 0) {
            return;
        }
        Vec3 gun = position().add(0, 1.2 * SCALE, 0);
        LivingEntity target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24),
                        e -> enemy(e) && hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) {
            turretCooldown = 10;
            return;
        }
        Vec3 aim = target.position().add(0, target.getBbHeight() / 2, 0).subtract(gun);
        entityData.set(TURRET_YAW, (float) (getYRot() + 90 - Math.toDegrees(Math.atan2(aim.z, aim.x))));
        entityData.set(FLASH, 6);
        playSound(com.xtrip.golemgarage.ModSounds.RIG_CANNON.get(), 2f, 0.95f + random.nextFloat() * 0.1f);
        if (level() instanceof ServerLevel sl) {
            Vec3 muzzle = gun.add(aim.normalize().scale(2.2));
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.1, 0.1, 0.1, 0.02);
            Vec3 hit = target.position();
            sl.sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y + 0.5, hit.z, 2, 0.4, 0.3, 0.4, 0);
            sl.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1f, 1.1f);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
                    this::enemy)) {
                e.hurt(damageSources().mobAttack(this), e == target ? 18f : 9f);
                Vec3 push = e.position().subtract(hit).normalize().scale(0.6);
                e.push(push.x, 0.3, push.z);
            }
        }
        // A golem gunner aboard reloads much faster.
        turretCooldown = getPassengers().stream().anyMatch(p -> p instanceof GolemEntity) ? 30 : 50;
    }

    /** Who the guns shoot at: hostile mobs, or for raiders' rigs, players and their golems. */
    private boolean enemy(LivingEntity e) {
        if (isRogue()) {
            return e.isAlive() && (e instanceof Player p && !p.isCreative() && !p.isSpectator()
                    || e instanceof GolemEntity g && !g.isRogue());
        }
        return GolemEntity.isHostile(e);
    }

    // =================================================================== nitro (Interceptor)
    @Override
    public boolean canJump() {
        return variant() == RigVariant.INTERCEPTOR && getFuel() > 300 && boostCooldown <= 0 && !boosting();
    }

    @Override
    public void onPlayerJump(int power) {
        // Client: the driver's own client moves the rig, so the kick happens here.
        if (canJump() && power > 0) {
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
            setDeltaMovement(getDeltaMovement().add(fwd.scale(0.4 + power / 100.0 * 0.8)));
        }
    }

    @Override
    public void handleStartJump(int power) {
        if (level().isClientSide || !canJump()) {
            return;
        }
        entityData.set(BOOST, 20 + power / 3);
        boostCooldown = 80;
        setFuel(getFuel() - 300);
        playSound(com.xtrip.golemgarage.ModSounds.RIG_NITRO.get(), 1.4f, 1f);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return boostCooldown;
    }

    // =================================================================== water (Skimmer)
    @Override
    public void travel(Vec3 input) {
        if (variant() == RigVariant.SKIMMER && isInWater()) {
            double depth = getFluidHeight(net.minecraft.tags.FluidTags.WATER);
            moveRelative(getSpeed() * 0.3f, input);
            Vec3 v = getDeltaMovement();
            double vy = depth > 0.12 ? Math.min(v.y + 0.05, 0.15) : v.y - 0.025;
            setDeltaMovement(v.x, vy, v.z);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.86, 0.6, 0.86));
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean isPushedByFluid() {
        return variant() != RigVariant.SKIMMER && super.isPushedByFluid();
    }

    // =================================================================== using it
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = level().isClientSide;
        // A raiders' rig left empty can be claimed.
        if (isRogue()) {
            if (!getPassengers().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!client) {
                removeTag(ROGUE_TAG);
                setPersistenceRequired();
                owner = player.getUUID();
                player.displayClientMessage(Component.translatable("golemgarage.rig.claimed"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // Fuel.
        int burn = held.getBurnTime(null);
        if (burn > 0 && getFuel() < MAX_FUEL - 100) {
            if (!client) {
                setFuel(getFuel() + burn * 3);
                held.consume(1, player);
                playSound(SoundEvents.FURNACE_FIRE_CRACKLE, 1f, 0.8f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // Repairs.
        float heal = held.is(ModItems.REPAIR_KIT.get()) ? 40 : held.is(Items.IRON_INGOT) ? 10 : held.is(ModItems.SCRAP.get()) ? 6 : 0;
        if (heal > 0 && getHealth() < getMaxHealth()) {
            if (!client) {
                heal(heal);
                held.consume(1, player);
                playSound(SoundEvents.ANVIL_USE, 0.6f, 1.2f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // Upgrades.
        int upgrade = held.is(ModItems.RIG_RAM.get()) ? RAM : held.is(ModItems.RIG_TURRET.get()) ? TURRET
                : held.is(ModItems.RIG_PLATING.get()) ? PLATING : 0;
        if (upgrade == TURRET && variant() == RigVariant.JUGGERNAUT) {
            if (!client) {
                player.displayClientMessage(Component.translatable("golemgarage.rig.has_cannon"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (upgrade != 0 && !has(upgrade)) {
            if (!client) {
                fit(upgrade);
                held.consume(1, player);
                playSound(SoundEvents.SMITHING_TABLE_USE, 1f, 0.9f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // The Golem Linker loads your golems and cubs on board, or unloads them.
        if (held.is(ModItems.GOLEM_LINKER.get())) {
            if (!client) {
                loadCrew(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // Sneak with an empty hand on a rig with a golem driver parks it, or sends it off again.
        if (held.isEmpty() && player.isSecondaryUseActive() && driver() != null) {
            if (!client) {
                boolean p = !parked();
                getPersistentData().putBoolean("GGParked", p);
                player.displayClientMessage(Component.translatable(p ? "golemgarage.rig.parked" : "golemgarage.rig.following"), true);
                playSound(p ? SoundEvents.IRON_TRAPDOOR_CLOSE : com.xtrip.golemgarage.ModSounds.RIG_HORN.get(), 1f, 1f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        // Sneak with an empty hand packs it up.
        if (held.isEmpty() && player.isSecondaryUseActive()) {
            if (!client && getPassengers().isEmpty() && (owner == null || owner.equals(player.getUUID()) || player.isCreative())) {
                ItemStack item = toItem();
                if (!player.addItem(item)) {
                    spawnAtLocation(item);
                }
                discard();
            } else if (!client) {
                player.displayClientMessage(Component.translatable("golemgarage.rig.cant_pack"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (!client && getPassengers().size() < 3) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(client);
    }

    private void loadCrew(Player player) {
        List<Entity> crew = riders(false);
        if (!crew.isEmpty()) {
            crew.forEach(Entity::stopRiding);
            player.displayClientMessage(Component.translatable("golemgarage.rig.unloaded"), true);
            return;
        }
        int loaded = 0;
        for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(8),
                m -> (m instanceof GolemEntity g && player.getUUID().equals(g.getOwnerUuid()) && !g.isPassenger())
                        || (m instanceof CubEntity c && player.getUUID().equals(c.getOwnerUuid()) && !c.isPassenger()))) {
            if (!canAddPassenger(m)) {
                break;
            }
            if (m.startRiding(this, true)) {
                loaded++;
            }
        }
        player.displayClientMessage(Component.translatable("golemgarage.rig.loaded", loaded), true);
    }

    // =================================================================== damage and saving
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.SCRAP.get(), 6 + random.nextInt(4)));
        if (has(RAM)) {
            spawnAtLocation(new ItemStack(ModItems.RIG_RAM.get()));
        }
        if (has(TURRET)) {
            spawnAtLocation(new ItemStack(ModItems.RIG_TURRET.get()));
        }
        if (has(PLATING)) {
            spawnAtLocation(new ItemStack(ModItems.RIG_PLATING.get()));
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.8, getZ(), 3, 0.8, 0.4, 0.8, 0);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuel", getFuel());
        tag.putByte("Upgrades", entityData.get(UPGRADES));
        tag.putString("Variant", variant().id());
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float hp = getHealth();
        for (RigVariant v : RigVariant.values()) {
            if (v.id().equals(tag.getString("Variant"))) {
                setVariant(v);
            }
        }
        if (tag.contains("Health")) {
            setHealth(Math.min(hp, getMaxHealth()));
        }
        setFuel(tag.getInt("Fuel"));
        byte up = tag.getByte("Upgrades");
        for (int u : new int[]{RAM, TURRET, PLATING}) {
            if ((up & u) != 0) {
                fit(u);
            }
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    /** The rig as an item, keeping its fuel, health and upgrades. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(variant().item());
        CompoundTag t = new CompoundTag();
        t.putInt("Fuel", getFuel());
        t.putByte("Upgrades", entityData.get(UPGRADES));
        t.putFloat("Health", getHealth());
        stack.set(ModDataComponents.RIG_DATA.get(), net.minecraft.world.item.component.CustomData.of(t));
        return stack;
    }

    /** Sets a freshly placed rig up from its item. */
    public void fromItem(ItemStack stack) {
        if (stack.getItem() instanceof com.xtrip.golemgarage.item.RigItem ri) {
            setVariant(ri.variant());
        }
        var data = stack.get(ModDataComponents.RIG_DATA.get());
        if (data == null) {
            setFuel(20 * 60 * 2);
            return;
        }
        CompoundTag t = data.copyTag();
        setFuel(t.getInt("Fuel"));
        byte up = t.getByte("Upgrades");
        for (int u : new int[]{RAM, TURRET, PLATING}) {
            if ((up & u) != 0) {
                fit(u);
            }
        }
        if (t.contains("Health")) {
            setHealth(Math.max(1, t.getFloat("Health")));
        }
    }

    /** Garages heal rigs parked inside them. */
    public void garageRepair(float amount) {
        if (getHealth() < getMaxHealth()) {
            heal(amount);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 6, 0.8, 0.4, 0.8, 0.05);
            }
        }
    }
}
