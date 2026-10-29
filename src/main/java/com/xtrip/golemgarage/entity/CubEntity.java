package com.xtrip.golemgarage.entity;

import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.item.CubItem;
import com.xtrip.golemgarage.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A Cub: a tiny chibi golem companion. It follows its owner, carries 9 stacks, and can't be hurt.
 *
 * Right-click: open storage (or put the held item in). Shift + right-click: pick it up.
 * Dye: recolor. Feather: toggle its item magnet. Feed it three golem parts to grow it into a golem.
 * It sits when you crouch nearby, dozes off when you stand still, and is thrilled when you come back.
 */
public class CubEntity extends PathfinderMob {
    public static final int STORAGE_SLOTS = 9;
    public static final int FEEDS_TO_GROW = 3;
    private static final int FLAG_SITTING = 1;
    private static final int FLAG_SLEEPY = 2;
    private static final int FLAG_EXCITED = 4;

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(CubEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(CubEntity.class, EntityDataSerializers.BYTE);
    /** Worn outfit ids for head, body and accessory, joined with '|' ("" for none). */
    private static final EntityDataAccessor<String> DATA_OUTFIT =
            SynchedEntityData.defineId(CubEntity.class, EntityDataSerializers.STRING);
    private final SimpleContainer wardrobe = new SimpleContainer(com.xtrip.golemgarage.menu.CubMenu.WARDROBE);

    private final SimpleContainer storage = new SimpleContainer(STORAGE_SLOTS);
    private final List<PartInstance> fed = new ArrayList<>();
    private UUID ownerUuid;
    private boolean magnet = true;
    private int awayTicks;
    private int excitedTicks;
    private int stillTicks;
    private Vec3 lastOwnerPos;

    public CubEntity(EntityType<? extends CubEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        wardrobe.addListener(c -> syncOutfit());
    }

    // =================================================================== outfits
    private void syncOutfit() {
        if (level() == null || level().isClientSide) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wardrobe.getContainerSize(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            if (wardrobe.getItem(i).getItem() instanceof com.xtrip.golemgarage.item.OutfitItem o) {
                sb.append(o.outfitId());
            }
        }
        this.entityData.set(DATA_OUTFIT, sb.toString());
    }

    /** The outfit ids worn in each wardrobe slot (empty strings for none). Works on both sides. */
    public String[] outfitIds() {
        String[] parts = this.entityData.get(DATA_OUTFIT).split("\\|", -1);
        String[] out = new String[com.xtrip.golemgarage.menu.CubMenu.WARDROBE];
        for (int i = 0; i < out.length; i++) {
            out[i] = i < parts.length ? parts[i] : "";
        }
        return out;
    }

    public SimpleContainer getWardrobe() {
        return wardrobe;
    }

    /** The style shared by all three worn pieces, or null. */
    @org.jetbrains.annotations.Nullable
    public String fullSetTheme() {
        String theme = null;
        for (int i = 0; i < wardrobe.getContainerSize(); i++) {
            if (!(wardrobe.getItem(i).getItem() instanceof com.xtrip.golemgarage.item.OutfitItem o)) {
                return null;
            }
            if (theme == null) {
                theme = o.theme();
            } else if (!theme.equals(o.theme())) {
                return null;
            }
        }
        return theme;
    }

    private void wear(Player player, ItemStack held, com.xtrip.golemgarage.item.OutfitItem o) {
        int i = o.slot().ordinal();
        ItemStack old = wardrobe.getItem(i);
        ItemStack piece = held.copyWithCount(1);
        wardrobe.setItem(i, piece);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        if (!old.isEmpty() && !player.getInventory().add(old)) {
            spawnAtLocation(old);
        }
        hearts();
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "cub_dress");
        if (fullSetTheme() != null) {
            com.xtrip.golemgarage.advancement.ModTriggers.award(player, "cub_full_set");
        }
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.5f);
        playSound(com.xtrip.golemgarage.ModSounds.CUB_HAPPY.get(), 0.6f, 1.1f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, DyeColor.PINK.getId());
        builder.define(DATA_FLAGS, (byte) 0);
        builder.define(DATA_OUTFIT, "||");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.15, 3.0));
        this.goalSelector.addGoal(2, new WanderNearOwnerGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // =================================================================== state
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    public Player getOwner() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public DyeColor getColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLOR));
    }

    public void setColor(DyeColor color) {
        this.entityData.set(DATA_COLOR, color.getId());
    }

    public SimpleContainer getStorage() {
        return storage;
    }

    private boolean flag(int f) {
        return (this.entityData.get(DATA_FLAGS) & f) != 0;
    }

    private void setFlag(int f, boolean on) {
        byte b = this.entityData.get(DATA_FLAGS);
        byte nb = (byte) (on ? b | f : b & ~f);
        if (nb != b) {
            this.entityData.set(DATA_FLAGS, nb);
        }
    }

    public boolean isSitting() {
        return flag(FLAG_SITTING);
    }

    public boolean isSleepy() {
        return flag(FLAG_SLEEPY);
    }

    public boolean isExcited() {
        return flag(FLAG_EXCITED);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return isSleepy() ? null : com.xtrip.golemgarage.ModSounds.CUB_CHIRP.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    /** Cubs can't be hurt. If one falls out of the world it pops back to its owner. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Cubs nudge each other (and golems nudge cubs) so they never stand inside one another. */
    @Override
    public boolean isPushable() {
        return true;
    }

    /** Cubs push other cubs and mobs aside, but never shove players around. */
    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
        if (!(other instanceof Player)) {
            super.doPush(other);
        }
    }

    // =================================================================== ticking
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        Player owner = getOwner();

        if (getY() < level().getMinBuildHeight() - 8 && owner != null) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }

        // Sit when the owner crouches nearby.
        setFlag(FLAG_SITTING, owner != null && owner.isShiftKeyDown() && distanceToSqr(owner) < 36);

        // Doze off when the owner has stood still nearby for a while.
        if (owner != null) {
            Vec3 op = owner.position();
            if (lastOwnerPos != null && op.distanceToSqr(lastOwnerPos) < 0.01 && distanceToSqr(owner) < 64) {
                stillTicks++;
            } else {
                stillTicks = 0;
            }
            lastOwnerPos = op;
        }
        boolean sleepy = stillTicks > 600;
        setFlag(FLAG_SLEEPY, sleepy);
        if (sleepy && tickCount % 120 == 0) {
            playSound(com.xtrip.golemgarage.ModSounds.CUB_SLEEP.get(), 0.5f, 1f);
        }
        if (sleepy && tickCount % 40 == 0) {
            sl.sendParticles(ModParticles.SLEEP_Z.get(), getX() + 0.15, getY() + 1.2, getZ(), 1, 0, 0, 0, 0);
        }

        // Excited greeting after the owner was away for a minute.
        if (owner == null || distanceToSqr(owner) > 24 * 24) {
            awayTicks++;
        } else {
            if (awayTicks > 1200 && distanceToSqr(owner) < 64) {
                excitedTicks = 60;
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.1, getZ(), 8, 0.4, 0.3, 0.4, 0);
                playSound(com.xtrip.golemgarage.ModSounds.CUB_HAPPY.get(), 1f, 1f);
            }
            awayTicks = 0;
        }
        if (excitedTicks > 0) {
            excitedTicks--;
            if (onGround() && tickCount % 8 == 0) {
                setDeltaMovement(getDeltaMovement().add(0, 0.35, 0));
            }
        }
        setFlag(FLAG_EXCITED, excitedTicks > 0);

        // Item magnet.
        int magnetRange = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.CUB_MAGNET_RANGE::get, 3);
        if (magnet && magnetRange > 0 && tickCount % 10 == 0) {
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(magnetRange),
                    e -> e.isAlive() && !e.hasPickUpDelay())) {
                ItemStack rest = storage.addItem(item.getItem().copy());
                if (rest.getCount() != item.getItem().getCount()) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, item.getX(), item.getY() + 0.3, item.getZ(), 4, 0.2, 0.2, 0.2, 0);
                    playSound(SoundEvents.ITEM_PICKUP, 0.4f, 1.8f);
                    if (rest.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(rest);
                    }
                }
            }
        }

        // A full outfit set in one style sparkles.
        if (tickCount % 30 == 0) {
            String theme = fullSetTheme();
            if (theme != null) {
                net.minecraft.core.particles.ParticleOptions p = switch (theme) {
                    case "cool" -> ParticleTypes.NOTE;
                    case "goth" -> ParticleTypes.WITCH;
                    case "cute" -> ParticleTypes.HEART;
                    default -> ParticleTypes.END_ROD;
                };
                sl.sendParticles(p, getX(), getY() + 1.3, getZ(), 1, 0.3, 0.2, 0.3, 0.0);
            }
        }
        if (random.nextInt(160) == 0 && !sleepy) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        }
    }

    // =================================================================== interaction
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean mine = ownerUuid == null || ownerUuid.equals(player.getUUID()) || player.hasPermissions(2)
                || com.xtrip.golemgarage.Config.OTHERS_CAN_EDIT.get();
        if (!mine && !(player.isShiftKeyDown() && held.isEmpty())) {
            player.displayClientMessage(Component.translatable("golemgarage.msg.cub_not_yours")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && held.isEmpty()) {
            tryPickUp(player);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof DyeItem dye) {
            if (dye.getDyeColor() != getColor()) {
                setColor(dye.getDyeColor());
                consume(player, held);
                hearts();
            }
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof com.xtrip.golemgarage.item.OutfitItem outfit) {
            wear(player, held, outfit);
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.FEATHER)) {
            magnet = !magnet;
            player.displayClientMessage(Component.translatable(magnet ? "golemgarage.msg.cub_magnet_on" : "golemgarage.msg.cub_magnet_off")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.SUCCESS;
        }
        PartInstance part = PartInstance.fromStack(held);
        if (part != null && part.blockedReason() != null) {
            player.displayClientMessage(part.blockedReason().copy().withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        if (part != null) {
            feed(player, part);
            consume(player, held);
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty()) {
            ItemStack rest = storage.addItem(held.copy());
            if (rest.getCount() != held.getCount()) {
                held.setCount(rest.getCount());
                hearts();
                playSound(com.xtrip.golemgarage.ModSounds.CUB_CHIRP.get(), 0.7f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new com.xtrip.golemgarage.menu.CubMenu(id, inv, storage, wardrobe, this, getId()),
                hasCustomName() ? getCustomName() : Component.translatable("entity.golemgarage.cub")),
                buf -> buf.writeVarInt(getId()));
        playSound(com.xtrip.golemgarage.ModSounds.CUB_CHIRP.get(), 0.7f, 1f);
        return InteractionResult.SUCCESS;
    }

    private static void consume(Player player, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    private void hearts() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.1, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private void feed(Player player, PartInstance part) {
        fed.removeIf(p -> p.shape().slot() == part.shape().slot());
        fed.add(part);
        hearts();
        playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.6f);
        int feeds = fed.size();
        if (feeds >= FEEDS_TO_GROW) {
            grow(player);
        } else {
            player.displayClientMessage(Component.translatable("golemgarage.msg.cub_growing", feeds, FEEDS_TO_GROW).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** Turns this cub into a golem with its fed parts, owner and storage items. */
    private void grow(Player player) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (!GolemEntity.canPlaceAnother(sl, ownerUuid != null ? ownerUuid : player.getUUID(), player, blockPosition())) {
            return;
        }
        Entity created = ModEntities.GOLEM.get().create(sl);
        if (!(created instanceof GolemEntity golem)) {
            return;
        }
        golem.moveTo(getX(), getY(), getZ(), getYRot(), 0);
        golem.setOwnerUuid(ownerUuid);
        for (PartInstance p : fed) {
            golem.equipPart(p);
        }
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            golem.getInventory().setItem(i, storage.getItem(i).copy());
        }
        if (hasCustomName()) {
            golem.setCustomName(getCustomName());
        }
        golem.setHealth(golem.getMaxHealth());
        for (int i = 0; i < wardrobe.getContainerSize(); i++) {
            ItemStack worn = wardrobe.removeItemNoUpdate(i);
            if (!worn.isEmpty() && !player.getInventory().add(worn)) {
                spawnAtLocation(worn);
            }
        }
        sl.addFreshEntity(golem);
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "cub_grow");
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.0, getZ(), 50, 0.6, 0.8, 0.6, 0.3);
        playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, 1.3f);
        player.displayClientMessage(Component.translatable("golemgarage.msg.cub_grew").withStyle(ChatFormatting.GOLD), false);
        discard();
    }

    /** Picks the cub up into the player's inventory, if it's theirs (or nobody's). */
    public void tryPickUp(Player player) {
        if (getOwnerUuid() != null && !getOwnerUuid().equals(player.getUUID())
                && !com.xtrip.golemgarage.Config.OTHERS_CAN_PICK_UP.get()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("golemgarage.msg.cub_not_yours")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        pickUp(player);
    }

    private void pickUp(Player player) {
        ItemStack stack = CubItem.createStack(this);
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
        playSound(SoundEvents.ITEM_PICKUP, 1f, 1.5f);
        discard();
    }

    // =================================================================== save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        exportData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        importData(tag);
    }

    public void exportData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putInt("Color", getColor().getId());
        tag.putBoolean("Magnet", magnet);
        ListTag fedList = new ListTag();
        for (PartInstance p : fed) {
            fedList.add(p.save());
        }
        tag.put("FedParts", fedList);
        if (hasCustomName() && getCustomName() != null) {
            tag.putString("Name", getCustomName().getString());
        }
        NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            items.set(i, storage.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, level().registryAccess());
        NonNullList<ItemStack> worn = NonNullList.withSize(wardrobe.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < worn.size(); i++) {
            worn.set(i, wardrobe.getItem(i));
        }
        CompoundTag w = new CompoundTag();
        ContainerHelper.saveAllItems(w, worn, level().registryAccess());
        tag.put("Wardrobe", w);
    }

    public void importData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("Color")) {
            setColor(DyeColor.byId(tag.getInt("Color")));
        }
        magnet = !tag.contains("Magnet") || tag.getBoolean("Magnet");
        fed.clear();
        for (Tag t : tag.getList("FedParts", Tag.TAG_COMPOUND)) {
            PartInstance p = PartInstance.load((CompoundTag) t);
            if (p != null) {
                fed.add(p);
            }
        }
        if (tag.contains("Name") && !hasCustomName()) {
            setCustomName(Component.literal(tag.getString("Name")));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, level().registryAccess());
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            storage.setItem(i, items.get(i));
        }
        NonNullList<ItemStack> worn = NonNullList.withSize(wardrobe.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("Wardrobe"), worn, level().registryAccess());
        for (int i = 0; i < worn.size(); i++) {
            wardrobe.setItem(i, worn.get(i));
        }
        syncOutfit();
    }

    /**
     * Each cub has its own spot around its owner (an angle and distance taken from its id), so a group of
     * cubs fans out around you instead of piling onto the same block.
     */
    Vec3 spotAround(Player owner) {
        long bits = getUUID().getLeastSignificantBits();
        double angle = ((bits & 0xFFFF) / 65536.0) * Math.PI * 2;
        double radius = 1.8 + ((bits >>> 16) & 0xFF) / 255.0 * 1.8;
        return owner.position().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    /** A free spot near the owner to catch up to (never the owner's own block). */
    private Vec3 catchUpSpot(Player owner) {
        Vec3 want = spotAround(owner);
        for (int i = 0; i < 8; i++) {
            double jx = i == 0 ? 0 : (random.nextDouble() - 0.5) * 3;
            double jz = i == 0 ? 0 : (random.nextDouble() - 0.5) * 3;
            BlockPos p = BlockPos.containing(want.x + jx, owner.getY(), want.z + jz);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = p.above(dy);
                if (level().getBlockState(q.below()).isFaceSturdy(level(), q.below(), net.minecraft.core.Direction.UP)
                        && level().noCollision(this, getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(q)))) {
                    return Vec3.atBottomCenterOf(q);
                }
            }
        }
        return owner.position();
    }

    /** Follows the owner to its own spot nearby; catches up by teleport if left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final CubEntity cub;
        private final double speed;
        private final double stopDistance;
        private Player owner;

        FollowOwnerGoal(CubEntity cub, double speed, double stopDistance) {
            this.cub = cub;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean farFromSpot() {
            return cub.distanceToSqr(owner) > stopDistance * stopDistance * 2.2
                    || cub.distanceToSqr(cub.spotAround(owner)) > 2.25 && cub.distanceToSqr(owner) > stopDistance * stopDistance;
        }

        @Override
        public boolean canUse() {
            owner = cub.getOwner();
            return owner != null && !cub.isSitting() && farFromSpot();
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && !cub.isSitting() && cub.distanceToSqr(cub.spotAround(owner)) > 1.0;
        }

        @Override
        public void start() {
            Vec3 s = cub.spotAround(owner);
            cub.getNavigation().moveTo(s.x, s.y, s.z, speed);
        }

        @Override
        public void tick() {
            if (cub.distanceToSqr(owner) > 144.0) {
                Vec3 s = cub.catchUpSpot(owner);
                cub.teleportTo(s.x, s.y, s.z);
                cub.getNavigation().stop();
            } else if (cub.tickCount % 10 == 0) {
                Vec3 s = cub.spotAround(owner);
                // Hurry when far behind, amble when nearly there.
                double sp = cub.distanceToSqr(owner) > 64 ? speed * 1.2 : speed;
                cub.getNavigation().moveTo(s.x, s.y, s.z, sp);
            }
        }

        @Override
        public void stop() {
            owner = null;
            cub.getNavigation().stop();
        }
    }

    /** When it's near its owner with nothing to do, the cub pootles around a little on its own. */
    static class WanderNearOwnerGoal extends Goal {
        private final CubEntity cub;
        private int cooldown;

        WanderNearOwnerGoal(CubEntity cub) {
            this.cub = cub;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cub.isSitting() || cub.isSleepy() || --cooldown > 0) {
                return false;
            }
            cooldown = 60 + cub.getRandom().nextInt(120);
            Player owner = cub.getOwner();
            return owner == null || cub.distanceToSqr(owner) < 36;
        }

        @Override
        public boolean canContinueToUse() {
            return !cub.getNavigation().isDone() && !cub.isSitting();
        }

        @Override
        public void start() {
            Player owner = cub.getOwner();
            Vec3 c = owner != null ? cub.spotAround(owner) : cub.position();
            double a = cub.getRandom().nextDouble() * Math.PI * 2;
            double r = 0.8 + cub.getRandom().nextDouble() * 2.2;
            cub.getNavigation().moveTo(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0.7);
        }
    }

    /** The parts this cub has been fed so far. */
    public List<PartInstance> fedParts() {
        return fed;
    }
}
