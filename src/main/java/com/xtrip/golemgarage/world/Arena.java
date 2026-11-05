package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Golem Arena duels. Ring an Arena Bell with two golems inside the ring (6 blocks) and they spar: a boss bar shows
 * both, and the first to drop to a quarter of its health yields. Nobody breaks down. The winner earns 40
 * experience and an arena win; the loser still earns 15. A minute without a winner is a draw.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID)
public final class Arena {
    public static final int RING = 6, MAX_TICKS = 1200;

    private static final class Duel {
        final GolemEntity a, b;
        final BlockPos bell;
        final ServerBossEvent barA, barB;
        int ticks;

        Duel(GolemEntity a, GolemEntity b, BlockPos bell) {
            this.a = a;
            this.b = b;
            this.bell = bell;
            this.barA = new ServerBossEvent(a.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
            this.barB = new ServerBossEvent(b.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        }
    }

    private static final List<Duel> DUELS = new ArrayList<>();

    private Arena() {
    }

    public static boolean opponents(GolemEntity x, GolemEntity y) {
        for (Duel d : DUELS) {
            if (d.a == x && d.b == y || d.a == y && d.b == x) {
                return true;
            }
        }
        return false;
    }

    public static boolean inDuel(GolemEntity g) {
        return dueling(g);
    }

    private static boolean dueling(GolemEntity g) {
        return DUELS.stream().anyMatch(d -> d.a == g || d.b == g);
    }

    /** Rings the bell: picks the two nearest golems in the ring and starts a bout. Returns a message key if not. */
    public static String ring(ServerLevel level, BlockPos bell, Player by) {
        if (DUELS.stream().anyMatch(d -> d.bell.equals(bell))) {
            return "golemgarage.arena.busy";
        }
        List<GolemEntity> in = new ArrayList<>(level.getEntitiesOfClass(GolemEntity.class, new AABB(bell).inflate(RING),
                g -> g.isAlive() && !g.isBroken() && !g.isRogue() && !dueling(g) && !g.isPassenger()));
        in.sort((p, q) -> Double.compare(p.distanceToSqr(bell.getCenter()), q.distanceToSqr(bell.getCenter())));
        if (in.size() < 2) {
            return "golemgarage.arena.need_two";
        }
        Duel d = new Duel(in.get(0), in.get(1), bell);
        DUELS.add(d);
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 0.8f);
        level.playSound(null, bell, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 0.6f, 1.4f);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(bell.getCenter()) < 48 * 48) {
                d.barA.addPlayer(p);
                d.barB.addPlayer(p);
                p.displayClientMessage(Component.translatable("golemgarage.arena.start", d.a.getDisplayName(), d.b.getDisplayName())
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        for (Duel d : new ArrayList<>(DUELS)) {
            d.ticks++;
            if (!d.a.isAlive() || !d.b.isAlive() || d.a.isBroken() || d.b.isBroken() || d.a.level() != d.b.level()
                    || d.a.distanceToSqr(d.bell.getCenter()) > (RING + 14) * (RING + 14)
                    || d.b.distanceToSqr(d.bell.getCenter()) > (RING + 14) * (RING + 14)) {
                end(d, null);
                continue;
            }
            if (d.ticks > MAX_TICKS) {
                end(d, null);
                continue;
            }
            d.a.setTarget(d.b);
            d.b.setTarget(d.a);
            d.barA.setProgress(Math.max(0, d.a.getHealth() / d.a.getMaxHealth()));
            d.barB.setProgress(Math.max(0, d.b.getHealth() / d.b.getMaxHealth()));
        }
    }

    /** Duel hits go through between the two (even for one owner), and stop a golem at a quarter health. */
    public static boolean onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof GolemEntity victim) || !(event.getSource().getEntity() instanceof GolemEntity hitter)) {
            return false;
        }
        for (Duel d : new ArrayList<>(DUELS)) {
            if (d.a == victim && d.b == hitter || d.b == victim && d.a == hitter) {
                if (victim.getHealth() - event.getAmount() <= victim.getMaxHealth() * 0.25f) {
                    event.setCanceled(true);
                    end(d, hitter);
                }
                return true;
            }
        }
        return false;
    }

    private static void end(Duel d, GolemEntity winner) {
        DUELS.remove(d);
        d.barA.removeAllPlayers();
        d.barB.removeAllPlayers();
        d.a.setTarget(null);
        d.b.setTarget(null);
        if (!(d.a.level() instanceof ServerLevel level)) {
            return;
        }
        Component msg;
        if (winner == null) {
            msg = Component.translatable("golemgarage.arena.draw").withStyle(ChatFormatting.GRAY);
        } else {
            GolemEntity loser = winner == d.a ? d.b : d.a;
            winner.addXp(40, true);
            loser.addXp(15, true);
            winner.getPersistentData().putInt("GGArenaWins", winner.getPersistentData().getInt("GGArenaWins") + 1);
            msg = Component.translatable("golemgarage.arena.win", winner.getDisplayName(),
                    winner.getPersistentData().getInt("GGArenaWins")).withStyle(ChatFormatting.GOLD);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, winner.getX(), winner.getY() + 2, winner.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
            level.playSound(null, d.bell, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1f);
            com.xtrip.golemgarage.advancement.ModTriggers.award(winner.getOwner(), "arena_win");
        }
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(d.bell.getCenter()) < 48 * 48) {
                p.displayClientMessage(msg, false);
            }
        }
    }
}
