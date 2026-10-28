package com.xtrip.golemgarage.golem;

import com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.item.GolemLinkerItem.Mode;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Runs a golem's rules. Rules are re-checked every half second; the first rule whose trigger
 * holds (and whose UNLESS doesn't) runs every tick until the next check.
 *
 * Guardrails from the design: repeated work actions pause between repeats (faster with a better
 * work rate), and five failed checks in a row stop the rules with a reason until they're edited.
 */
public class BehaviorGoal extends Goal {
    private static final int CHECK = 10;
    private static final int MAX_FAILURES = 5;
    private static final double REACH_SQ = 6.25;

    private final GolemEntity golem;
    private GolemRule active;
    private int activeIndex = -1;
    private int failStreak;
    private String stoppedReason;
    private int seenRulesVersion = -1;
    private int waitUntil;
    private int opCooldown;
    private int repairCooldown;
    private BlockPos cachedPos;
    private final java.util.Map<BlockPos, Integer> unreachable = new java.util.HashMap<>();

    public BehaviorGoal(GolemEntity golem) {
        this.golem = golem;
        // No flags: this goal steers by issuing navigation itself, and leaves the melee goal free to fight.
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return !golem.isBroken() && !golem.isBreakingDown();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (golem.getRulesVersion() != seenRulesVersion) {
            seenRulesVersion = golem.getRulesVersion();
            failStreak = 0;
            stoppedReason = null;
            active = null;
            waitUntil = 0;
        }
        if (opCooldown > 0) {
            opCooldown--;
        }
        if (repairCooldown > 0) {
            repairCooldown--;
        }
        if (golem.tickCount % 200 == 0) {
            unreachable.values().removeIf(t -> t <= golem.tickCount);
        }
        if (golem.isPaused()) {
            golem.setStatus(GolemText.of("paused"));
            golem.setActiveRule(-1);
            golem.getNavigation().stop();
            return;
        }
        if (golem.tickCount % CHECK == 0) {
            ownerTooFar = ownerTooFar();
        }
        if (ownerTooFar) {
            golem.setStatus(GolemText.of("owner_far"));
            golem.setActiveRule(-1);
            golem.getNavigation().stop();
            return;
        }
        if (stoppedReason != null) {
            golem.setStatus(GolemText.of("stopped", stoppedReason));
            golem.setActiveRule(-1);
            follow(golem.tickCount % CHECK == 0, true);
            return;
        }

        boolean check = golem.tickCount % CHECK == 0;
        if (golem.tickCount < waitUntil) {
            if (check) {
                int[] idx = new int[1];
                evaluate(idx);
                if (idx[0] < activeIndex) {
                    waitUntil = 0; // a more important rule now applies
                } else {
                    golem.getNavigation().stop();
                    return;
                }
            } else {
                return;
            }
        }

        if (check || active == null) {
            int[] idx = new int[1];
            active = evaluate(idx);
            activeIndex = idx[0];
            golem.setActiveRule(activeIndex);
        }

        // Let the golem finish a fight it was pulled into, unless the rule is itself a fight.
        LivingEntity target = golem.getTarget();
        boolean fightRule = active.action() == GolemRule.Action.ATTACK_HOSTILE || active.action() == GolemRule.Action.DEFEND_OWNER;
        if (target != null && !fightRule) {
            boolean recentlyHit = golem.getLastHurtByMob() == target && golem.tickCount - golem.getLastHurtByMobTimestamp() < 100;
            if (target.isAlive() && recentlyHit) {
                golem.setStatus(GolemText.of("fighting_back"));
                return;
            }
            golem.setTarget(null);
        }

        String failure = run(active, check);
        if (!check) {
            return;
        }
        if (failure == null) {
            failStreak = 0;
            return;
        }
        failStreak++;
        golem.setStatus(GolemText.of("stuck", failure));
        if (failStreak >= MAX_FAILURES) {
            stoppedReason = failure;
            golem.getNavigation().stop();
        }
    }

    private boolean ownerTooFar;

    /** workRangeFromOwner: jobs pause while the owner is offline, elsewhere, or too far away. */
    private boolean ownerTooFar() {
        int range = com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.WORK_RANGE_FROM_OWNER::get, 0);
        if (range <= 0 || golem.getOwnerUuid() == null || golem.getServer() == null) {
            return false;
        }
        net.minecraft.server.level.ServerPlayer owner = golem.getServer().getPlayerList().getPlayer(golem.getOwnerUuid());
        return owner == null || owner.level() != golem.level() || owner.distanceToSqr(golem) > (double) range * range;
    }

    // =================================================================== evaluation
    private GolemRule evaluate(int[] indexOut) {
        List<GolemRule> rules = golem.getRules();
        for (int i = 0; i < rules.size(); i++) {
            GolemRule r = rules.get(i);
            if (matches(r.trigger(), r.param()) && !(r.unless() != GolemRule.Trigger.NONE && matches(r.unless(), r.unlessParam()))) {
                indexOut[0] = i;
                return r;
            }
        }
        indexOut[0] = -1;
        return new GolemRule(GolemRule.Trigger.ALWAYS, 0, GolemRule.Action.FOLLOW_OWNER);
    }

    private boolean matches(GolemRule.Trigger t, int p) {
        Player owner = golem.getOwner();
        return switch (t) {
            case ALWAYS -> true;
            case NONE -> false;
            case HEALTH_BELOW -> golem.getHealth() / golem.getMaxHealth() * 100f < p;
            case POWER_BELOW -> golem.getMaxPower() > 0 && golem.getPowerPercent() < p;
            case PART_WORN -> golem.worstCondition() * 100f < p;
            case PART_BROKEN -> golem.hasBrokenPart();
            case OWNER_WITHIN -> owner != null && golem.distanceTo(owner) <= p;
            case OWNER_BEYOND -> owner != null && golem.distanceTo(owner) > p;
            case OWNER_ATTACKED -> ownerAttacker() != null;
            case HOSTILE_WITHIN -> nearestHostile(Math.min(p, Math.max(8f, golem.stats().vision))) != null;
            case AWAY_FROM_POST -> {
                BlockPos post = golem.getLink(Mode.POST);
                yield post != null && golem.distanceToSqr(Vec3.atBottomCenterOf(post)) > p * p;
            }
            case CROP_READY -> findCrop(p) != null;
            case CHEST_A_HAS -> countItems(container(Mode.CHEST_A)) >= p;
            case CHEST_A_BELOW -> {
                IItemHandler c = container(Mode.CHEST_A);
                yield c != null && countItems(c) < p;
            }
            case CHEST_B_FULL -> isFull(container(Mode.CHEST_B));
            case INVENTORY_FULL -> golem.isInventoryFull();
            case INVENTORY_EMPTY -> golem.isInventoryEmpty();
            case DAY -> golem.level().isDay();
            case NIGHT -> !golem.level().isDay();
            case SUNSET -> {
                long time = golem.level().getDayTime() % 24000;
                yield time >= 11500 && time <= 13500;
            }
            case RAINING -> golem.level().isRaining();
            case THUNDER -> golem.level().isThundering();
            case REDSTONE_ON -> redstone();
            case REDSTONE_OFF -> !redstone();
            case OUTSIDE_AREA -> {
                net.minecraft.world.phys.AABB area = golem.area();
                yield area != null && !area.contains(golem.position().add(0, 0.1, 0));
            }
        };
    }

    // =================================================================== actions
    /** Runs the rule's action. Returns null when fine, or the reason it can't run. */
    private String run(GolemRule rule, boolean check) {
        if (rule.action().isDisabled()) {
            golem.getNavigation().stop();
            return GolemText.of("fail.action_off");
        }
        return switch (rule.action()) {
            case FOLLOW_OWNER -> {
                follow(check, false);
                yield null;
            }
            case GO_TO_POST -> goToPost(check);
            case WAIT -> {
                golem.getNavigation().stop();
                waitUntil = golem.tickCount + Math.max(1, rule.actionParam()) * 20;
                golem.setStatus(GolemText.of("waiting", rule.actionParam()));
                yield null;
            }
            case STROLL -> {
                stroll();
                golem.setStatus(GolemText.of("strolling"));
                yield null;
            }
            case ATTACK_HOSTILE -> {
                Monster m = nearestHostile(Math.max(8f, golem.stats().vision));
                if (m != null) {
                    golem.setTarget(m);
                    golem.setStatus(GolemText.of("attacking", GolemText.name(m)));
                } else {
                    golem.setStatus(GolemText.of("guarding"));
                }
                yield null;
            }
            case DEFEND_OWNER -> {
                LivingEntity attacker = ownerAttacker();
                if (attacker != null) {
                    golem.setTarget(attacker);
                    golem.setStatus(GolemText.of("defending", GolemText.name(attacker)));
                } else {
                    follow(check, false);
                }
                yield null;
            }
            case TAKE_FROM_A -> chest(Mode.CHEST_A, true, check);
            case DEPOSIT_TO_B -> chest(Mode.CHEST_B, false, check);
            case TAKE_FROM_NEAREST -> chest(null, true, check);
            case DEPOSIT_TO_NEAREST -> chest(null, false, check);
            case PICK_UP_ITEMS -> pickUp(check);
            case HARVEST_CROPS -> harvest(check);
            case RECHARGE -> recharge(check);
            case REPAIR_SELF -> repairSelf();
            case GO_TO_GARAGE -> goToGarage(check);
            case GIVE_TO_GOLEM -> giveToGolem(check);
            case PATROL -> patrol(check);
            case GO_HOME -> goHome(check);
            case EMIT_SIGNAL -> {
                golem.emitSignal();
                golem.getNavigation().stop();
                golem.setStatus(GolemText.of("signal"));
                yield null;
            }
        };
    }

    private void follow(boolean check, boolean quiet) {
        Player owner = golem.getOwner();
        if (owner == null) {
            if (!quiet) {
                golem.setStatus(GolemText.of("waiting_owner"));
            }
            golem.getNavigation().stop();
            return;
        }
        if (!quiet) {
            golem.setStatus(GolemText.of("following"));
        }
        if (golem.distanceToSqr(owner) > 12 && check) {
            golem.getNavigation().moveTo(owner, 1.0);
        } else if (golem.distanceToSqr(owner) <= 9) {
            golem.getNavigation().stop();
            golem.getLookControl().setLookAt(owner);
        }
    }

    private String goToPost(boolean check) {
        BlockPos post = golem.getLink(Mode.POST);
        if (post == null) {
            return GolemText.of("fail.no_post");
        }
        Vec3 c = Vec3.atBottomCenterOf(post);
        int a = approach(c, 2.25, check);
        if (a == ARRIVED) {
            golem.setStatus(GolemText.of("at_post"));
            return null;
        }
        golem.setStatus(GolemText.of("to_post"));
        return a == NO_PATH ? GolemText.of("fail.no_path_post") : null;
    }

    private static final int ARRIVED = 0;
    private static final int WALKING = 1;
    private static final int NO_PATH = 2;
    private Vec3 closeEnough;

    /**
     * Walks toward a spot. A big golem can't always stand exactly where it wants (crops, chests in a corner),
     * so when no path gets any closer and it is already within 4 blocks, that counts as arrived.
     */
    private int approach(Vec3 c, double reachSq, boolean check) {
        double d = golem.distanceToSqr(c);
        boolean near = closeEnough != null && closeEnough.distanceToSqr(c) < 0.01 && d <= 16;
        if (d <= reachSq || near) {
            golem.getNavigation().stop();
            return ARRIVED;
        }
        if (!check) {
            return WALKING;
        }
        if (golem.getNavigation().moveTo(c.x, c.y, c.z, 1.0)) {
            return WALKING;
        }
        if (d <= 16) {
            closeEnough = c;
            golem.getNavigation().stop();
            return ARRIVED;
        }
        return NO_PATH;
    }

    /** Walks onto its linked Golem Dock and rests there, slowly recharging and healing. */
    private String goHome(boolean check) {
        BlockPos dock = golem.getLink(Mode.HOME);
        if (dock == null) {
            return GolemText.of("fail.no_dock");
        }
        if (!(golem.level().getBlockState(dock).getBlock() instanceof com.xtrip.golemgarage.block.GolemDockBlock)) {
            return GolemText.of("fail.dock_missing");
        }
        Vec3 c = Vec3.atBottomCenterOf(dock).add(0, 0.19, 0);
        if (golem.distanceToSqr(c) > 1.0) {
            int a = approach(c, 0.5, check);
            if (a != ARRIVED || golem.distanceToSqr(c) > 2.4) {
                golem.setStatus(GolemText.of("to_dock"));
                return a == NO_PATH ? GolemText.of("fail.no_path_dock") : null;
            }
        }
        golem.getNavigation().stop();
        golem.restOnDock(dock);
        golem.setStatus(GolemText.of("resting", golem.getPowerPercent()));
        return null;
    }

    private void stroll() {
        net.minecraft.world.phys.AABB area = golem.area();
        if (area != null && golem.getNavigation().isDone() && golem.tickCount % 60 == 0) {
            double x = area.minX + golem.getRandom().nextDouble() * (area.maxX - area.minX);
            double z = area.minZ + golem.getRandom().nextDouble() * (area.maxZ - area.minZ);
            golem.getNavigation().moveTo(x, area.minY, z, 0.6);
            return;
        }
        if (golem.getNavigation().isDone() && golem.tickCount % 60 == 0) {
            BlockPos post = golem.getLink(Mode.POST);
            Vec3 center = post != null ? Vec3.atBottomCenterOf(post) : golem.position();
            double x = center.x + (golem.getRandom().nextDouble() - 0.5) * 12;
            double z = center.z + (golem.getRandom().nextDouble() - 0.5) * 12;
            golem.getNavigation().moveTo(x, center.y, z, 0.6);
        }
    }

    /** Walks to a storage block, then moves one stack in or out per work cycle. */
    private String chest(Mode link, boolean take, boolean check) {
        BlockPos pos;
        if (link != null) {
            pos = golem.getLink(link);
            if (pos == null) {
                return GolemText.of("fail.no_chest_link", GolemText.key(link == Mode.CHEST_A ? "golemgarage.status.chest_a" : "golemgarage.status.chest_b"));
            }
        } else {
            if (check || cachedPos == null) {
                cachedPos = nearestContainer();
            }
            pos = cachedPos;
            if (pos == null) {
                return GolemText.of("fail.no_chest_near");
            }
        }
        IItemHandler storage = Storage.at(golem.level(), pos);
        if (storage == null) {
            return GolemText.of("fail.storage_missing");
        }
        String name = GolemText.key(link == null ? "golemgarage.status.chest" : (link == Mode.CHEST_A ? "golemgarage.status.chest_a" : "golemgarage.status.chest_b"));
        if (!take && golem.isInventoryEmpty()) {
            golem.setStatus(GolemText.of("nothing_to_store"));
            golem.getNavigation().stop();
            return null;
        }
        if (take && golem.isInventoryFull()) {
            return GolemText.of("fail.inventory_full");
        }
        Vec3 c = Vec3.atCenterOf(pos);
        int a = approach(c, REACH_SQ, check);
        if (a != ARRIVED) {
            golem.setStatus(GolemText.of("walking_to", name));
            return a == NO_PATH ? GolemText.of("fail.no_path_to", name) : null;
        }
        golem.getLookControl().setLookAt(c);
        if (opCooldown > 0) {
            return null;
        }
        opCooldown = workDelay();
        if (take) {
            if (!Storage.takeOne(storage, golem::store)) {
                return GolemText.of("fail.empty", name);
            }
            golem.setStatus(GolemText.of("taking", name));
        } else {
            if (!depositOne(storage)) {
                return GolemText.of("fail.full", name);
            }
            golem.setStatus(GolemText.of("storing", name));
        }
        golem.onWorkDone();
        return null;
    }

    private boolean depositOne(IItemHandler into) {
        var inv = golem.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack rest = Storage.insert(into, s.copy());
            if (rest.getCount() < s.getCount()) {
                inv.setItem(i, rest);
                return true;
            }
        }
        return false;
    }

    /** Hands its items to the nearest other golem of the same owner that has room. */
    private String giveToGolem(boolean check) {
        if (golem.isInventoryEmpty()) {
            golem.setStatus(GolemText.of("nothing_to_give"));
            golem.getNavigation().stop();
            return null;
        }
        GolemEntity target = null;
        double best = Double.MAX_VALUE;
        for (GolemEntity g : golem.level().getEntitiesOfClass(GolemEntity.class, golem.getBoundingBox().inflate(12),
                g -> g != golem && g.isAlive() && !g.isInventoryFull()
                        && java.util.Objects.equals(g.getOwnerUuid(), golem.getOwnerUuid())
                        && g.activeAction() != GolemRule.Action.GIVE_TO_GOLEM)) {
            double d = golem.distanceToSqr(g);
            if (d < best) {
                best = d;
                target = g;
            }
        }
        if (target == null) {
            return GolemText.of("fail.no_golem_near");
        }
        int a = approach(target.position(), 9.0, check);
        if (a != ARRIVED) {
            golem.setStatus(GolemText.of("walking_to_golem", GolemText.name(target)));
            return a == NO_PATH ? GolemText.of("fail.no_path_golem") : null;
        }
        golem.getLookControl().setLookAt(target);
        if (opCooldown > 0) {
            return null;
        }
        opCooldown = workDelay();
        var inv = golem.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                ItemStack rest = target.store(s.copy());
                if (rest.getCount() < s.getCount()) {
                    inv.setItem(i, rest);
                    golem.setStatus(GolemText.of("giving", GolemText.name(target)));
                    golem.onWorkDone();
                    return null;
                }
            }
        }
        return GolemText.of("fail.golem_full");
    }

    /** Walks between random spots in its area. */
    private String patrol(boolean check) {
        net.minecraft.world.phys.AABB area = golem.area();
        if (area == null) {
            return GolemText.of("fail.no_area");
        }
        golem.setStatus(GolemText.of("patrolling"));
        if (golem.getNavigation().isDone() && check) {
            double x = area.minX + golem.getRandom().nextDouble() * (area.maxX - area.minX);
            double z = area.minZ + golem.getRandom().nextDouble() * (area.maxZ - area.minZ);
            if (!golem.getNavigation().moveTo(x, area.minY, z, 0.9)) {
                golem.getNavigation().moveTo(area.getCenter().x, area.minY, area.getCenter().z, 0.9);
            }
        }
        return null;
    }

    private String pickUp(boolean check) {
        if (golem.isInventoryFull()) {
            return GolemText.of("fail.inventory_full");
        }
        double radius = golem.stats().collect ? 10 : 5;
        List<ItemEntity> items = golem.level().getEntitiesOfClass(ItemEntity.class,
                golem.getBoundingBox().inflate(radius), e -> e.isAlive() && !e.hasPickUpDelay() && golem.inArea(e.position()));
        ItemEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity e : items) {
            double d = golem.distanceToSqr(e);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        if (nearest == null) {
            golem.setStatus(GolemText.of("no_items"));
            golem.getNavigation().stop();
            return null;
        }
        golem.setStatus(GolemText.of("picking_up"));
        double reach = golem.stats().collect ? 3.0 : 2.2;
        if (best > reach * reach) {
            if (check) {
                golem.getNavigation().moveTo(nearest, 1.0);
            }
            return null;
        }
        ItemStack rest = golem.store(nearest.getItem());
        if (rest.getCount() != nearest.getItem().getCount()) {
            golem.debug("Picked up " + (nearest.getItem().getCount() - rest.getCount()) + " " + nearest.getItem().getHoverName().getString());
            golem.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.4f, 1.2f);
            golem.onWorkDone();
        }
        if (rest.isEmpty()) {
            nearest.discard();
        } else {
            nearest.setItem(rest);
        }
        return null;
    }

    private String harvest(boolean check) {
        GolemStats stats = golem.stats();
        if (!stats.harvest) {
            return GolemText.of("fail.needs_harvest_arms");
        }
        if (com.xtrip.golemgarage.Config.GOLEMS_HARVEST_NEED_GRIEFING.get()
                && !golem.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
            return GolemText.of("fail.griefing_off");
        }
        if (golem.isInventoryFull()) {
            return GolemText.of("fail.inventory_full");
        }
        if (check || cachedPos == null || !isRipe(golem.level().getBlockState(cachedPos), cachedPos)) {
            cachedPos = findCrop((int) Math.max(8, stats.vision));
        }
        if (cachedPos == null) {
            golem.setStatus(GolemText.of("no_crops"));
            golem.getNavigation().stop();
            return null;
        }
        Vec3 c = Vec3.atBottomCenterOf(cachedPos);
        int a = approach(c, 4.0, check);
        if (a != ARRIVED) {
            golem.setStatus(GolemText.of("to_crops"));
            if (a == NO_PATH) {
                // Skip this crop for a while and try another before giving up.
                unreachable.put(cachedPos, golem.tickCount + 600);
                cachedPos = findCrop((int) Math.max(8, stats.vision));
                return cachedPos == null ? GolemText.of("fail.no_path_crops") : null;
            }
            return null;
        }
        golem.getLookControl().setLookAt(c);
        if (opCooldown > 0) {
            return null;
        }
        opCooldown = workDelay();
        int radius = stats.scytheHarvest ? 1 : 0;
        for (BlockPos p : BlockPos.betweenClosed(cachedPos.offset(-radius, 0, -radius), cachedPos.offset(radius, 0, radius))) {
            harvestAt(p.immutable());
        }
        golem.debug("Harvested around " + cachedPos.toShortString() + ", carrying " + golem.readUsedSlots() + " stacks");
        golem.setStatus(GolemText.of("harvesting"));
        golem.onWorkDone();
        cachedPos = null;
        return null;
    }

    private void harvestAt(BlockPos pos) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!isRipe(state, pos)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, golem, ItemStack.EMPTY);
        level.levelEvent(2001, pos, Block.getId(state));
        BlockState replant = null;
        if (state.getBlock() instanceof CropBlock crop) {
            replant = crop.getStateForAge(0);
        } else if (state.getBlock() instanceof NetherWartBlock) {
            replant = state.setValue(NetherWartBlock.AGE, 0);
        }
        if (replant != null) {
            // Use one seed from the drops to replant.
            ItemStack seed = state.getBlock().getCloneItemStack(level, pos, state);
            for (ItemStack d : drops) {
                if (!seed.isEmpty() && d.is(seed.getItem())) {
                    d.shrink(1);
                    break;
                }
            }
            level.setBlock(pos, replant, 3);
        } else {
            level.removeBlock(pos, false);
        }
        for (ItemStack d : drops) {
            if (d.isEmpty()) {
                continue;
            }
            ItemStack rest = golem.store(d);
            if (!rest.isEmpty()) {
                Block.popResource(level, pos, rest);
            }
        }
    }

    private boolean isRipe(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (golem.level().getBlockState(pos.relative(d)).getBlock() instanceof AttachedStemBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    // Scan caches: a full area scan runs at most every RESCAN ticks; in between, a cached hit is just
    // re-checked (one block lookup) and a cached miss is trusted.
    private static final int RESCAN = Integer.getInteger("golemgarage.rescan", 40);
    private int cropScanTick = -1000;
    private int cropScanRadius = -1;
    private BlockPos cropScanOrigin;
    @org.jetbrains.annotations.Nullable
    private BlockPos cropScanResult;
    private final java.util.Map<Block, Object[]> blockScans = new java.util.HashMap<>();

    private BlockPos findCrop(int radius) {
        BlockPos here = golem.blockPosition();
        boolean fresh = golem.tickCount - cropScanTick < RESCAN && radius == cropScanRadius
                && cropScanOrigin != null && cropScanOrigin.distManhattan(here) <= 2;
        if (fresh) {
            if (cropScanResult == null) {
                return null;
            }
            Integer until = unreachable.get(cropScanResult);
            if (isRipe(golem.level().getBlockState(cropScanResult), cropScanResult) && (until == null || until <= golem.tickCount)) {
                return cropScanResult;
            }
        }
        cropScanTick = golem.tickCount;
        cropScanRadius = radius;
        cropScanOrigin = here;
        cropScanResult = scanCrop(radius);
        return cropScanResult;
    }

    private BlockPos scanCrop(int radius) {
        BlockPos origin = golem.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
            BlockState s = golem.level().getBlockState(p);
            if (isRipe(s, p) && golem.inArea(Vec3.atCenterOf(p))) {
                Integer until = unreachable.get(p);
                if (until != null && until > golem.tickCount) {
                    continue;
                }
                double d = p.distSqr(origin);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private String recharge(boolean check) {
        if (golem.getMaxPower() <= 0) {
            golem.setStatus(GolemText.of("no_core"));
            return null;
        }
        if (golem.getPowerPercent() >= 100) {
            golem.setStatus(GolemText.of("charged"));
            golem.getNavigation().stop();
            return null;
        }
        if (check || cachedPos == null || !golem.level().getBlockState(cachedPos).is(Blocks.REDSTONE_BLOCK)) {
            cachedPos = findBlock(Blocks.REDSTONE_BLOCK, 12);
        }
        if (cachedPos == null) {
            return GolemText.of("fail.no_redstone");
        }
        Vec3 c = Vec3.atCenterOf(cachedPos);
        int a = approach(c, 6.0, check);
        if (a != ARRIVED) {
            golem.setStatus(GolemText.of("to_redstone"));
            return a == NO_PATH ? GolemText.of("fail.no_path_redstone") : null;
        }
        golem.addPower(2);
        golem.setStatus(GolemText.of("charging", golem.getPowerPercent()));
        if (golem.tickCount % 4 == 0 && golem.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                    c.x, c.y + 0.6, c.z, 2, 0.2, 0.2, 0.2, 0);
        }
        return null;
    }

    private String repairSelf() {
        if (golem.worstCondition() >= 0.999f && golem.getHealth() >= golem.getMaxHealth()) {
            golem.setStatus(GolemText.of("good_shape"));
            return null;
        }
        if (repairCooldown > 0) {
            return null;
        }
        if (!golem.consumeRepairKit()) {
            return GolemText.of("fail.no_kit");
        }
        golem.useRepairKit();
        repairCooldown = 100;
        golem.setStatus(GolemText.of("self_repaired"));
        return null;
    }

    private String goToGarage(boolean check) {
        BlockPos garage = golem.getLink(Mode.GARAGE);
        if (garage == null) {
            return GolemText.of("fail.no_garage");
        }
        if (!(golem.level().getBlockEntity(garage) instanceof GarageControllerBlockEntity controller)) {
            return GolemText.of("fail.garage_missing");
        }
        Vec3 c = Vec3.atCenterOf(garage);
        int a = approach(c, 12.0, check);
        if (a != ARRIVED) {
            golem.setStatus(GolemText.of("to_garage"));
            return a == NO_PATH ? GolemText.of("fail.no_path_garage") : null;
        }
        if (!check) {
            return null;
        }
        return controller.acceptGolem(golem);
    }

    // =================================================================== lookups
    private int workDelay() {
        return Math.max(5, Math.round(20f / Math.max(0.3f, golem.stats().work)));
    }

    private LivingEntity ownerAttacker() {
        Player owner = golem.getOwner();
        if (owner == null) {
            return null;
        }
        LivingEntity a = owner.getLastHurtByMob();
        if (a != null && a.isAlive() && a != golem && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200
                && golem.distanceToSqr(a) < 32 * 32) {
            return a;
        }
        return null;
    }

    private Monster nearestHostile(float radius) {
        net.minecraft.world.phys.AABB area = golem.area();
        List<Monster> found = golem.level().getEntitiesOfClass(Monster.class,
                golem.getBoundingBox().inflate(radius), m -> m.isAlive() && (area == null || area.contains(m.position())));
        Monster nearest = null;
        double best = Double.MAX_VALUE;
        for (Monster m : found) {
            double d = golem.distanceToSqr(m);
            if (d < best) {
                best = d;
                nearest = m;
            }
        }
        return nearest;
    }

    private IItemHandler container(Mode mode) {
        BlockPos p = golem.getLink(mode);
        return p == null ? null : Storage.at(golem.level(), p);
    }

    private static int countItems(IItemHandler c) {
        return Storage.count(c);
    }

    private static boolean isFull(IItemHandler c) {
        return Storage.isFull(c);
    }

    private boolean redstone() {
        BlockPos here = golem.blockPosition();
        BlockPos post = golem.getLink(Mode.POST);
        return golem.level().hasNeighborSignal(here) || golem.level().hasNeighborSignal(here.below())
                || (post != null && golem.level().hasNeighborSignal(post));
    }

    /** The nearest storage block within 8 blocks. Walks only the block entities of nearby chunks. */
    private BlockPos nearestContainer() {
        BlockPos origin = golem.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int minX = origin.getX() - 8, maxX = origin.getX() + 8, minZ = origin.getZ() - 8, maxZ = origin.getZ() + 8;
        int minY = origin.getY() - 2, maxY = origin.getY() + 3;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (!(golem.level().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                        instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) {
                    continue;
                }
                for (var e : chunk.getBlockEntities().entrySet()) {
                    BlockPos p = e.getKey();
                    if (p.getX() < minX || p.getX() > maxX || p.getZ() < minZ || p.getZ() > maxZ
                            || p.getY() < minY || p.getY() > maxY
                            || e.getValue() instanceof com.xtrip.golemgarage.block.entity.GarageControllerBlockEntity) {
                        continue;
                    }
                    double d = p.distSqr(origin);
                    if (d < bestD && Storage.at(golem.level(), p) != null) {
                        bestD = d;
                        best = p.immutable();
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findBlock(Block block, int radius) {
        Object[] c = blockScans.get(block);
        BlockPos here = golem.blockPosition();
        if (c != null && golem.tickCount - (Integer) c[0] < RESCAN && ((BlockPos) c[1]).distManhattan(here) <= 2) {
            BlockPos hit = (BlockPos) c[2];
            if (hit == null || golem.level().getBlockState(hit).is(block)) {
                return hit;
            }
        }
        BlockPos found = scanBlock(block, radius);
        blockScans.put(block, new Object[]{golem.tickCount, here, found});
        return found;
    }

    private BlockPos scanBlock(Block block, int radius) {
        BlockPos origin = golem.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))) {
            if (golem.level().getBlockState(p).is(block)) {
                double d = p.distSqr(origin);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }
}
