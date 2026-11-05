package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.Config;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.entity.RigEntity;
import com.xtrip.golemgarage.golem.GolemData;
import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemRegistry;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Raider convoys. At nightfall each player with a few golems may be raided: rogue golems arrive in a rusted
 * rig 40-50 blocks out and march on their base. Bigger bases draw bigger raids, and from a big enough base a
 * Junk Titan comes too. A red bar counts the raiders down; anything left at dawn slinks off.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID)
public final class Raids {
    private static final List<Raid> ACTIVE = new ArrayList<>();

    private record Raid(UUID player, List<UUID> raiders, ServerBossEvent bar, ServerLevel level, int total, long started) {
    }

    private Raids() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        ServerLevel overworld = server.overworld();
        long day = overworld.getDayTime() % 24000;
        if (day == 13000 && Config.get(Config.RAIDS::get, true) && overworld.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL) {
            double chance = Config.get(Config.RAID_CHANCE::get, 0.15);
            for (ServerPlayer p : overworld.players()) {
                if (!p.isCreative() && !p.isSpectator() && overworld.random.nextDouble() < chance
                        && golemCount(overworld, p.getUUID()) >= Config.get(Config.RAID_MIN_GOLEMS::get, 3)) {
                    start(p, false);
                }
            }
        }
        if (overworld.getGameTime() % 10 == 0) {
            tickRaids(day);
        }
    }

    private static int golemCount(ServerLevel level, UUID owner) {
        return GolemRegistry.get(level.getServer()).forOwner(owner).size();
    }

    /** Starts a raid on a player. Returns the number of raiders, or 0 if there was nowhere to spawn them. */
    public static int start(ServerPlayer player, boolean forceTitan) {
        ServerLevel level = player.serverLevel();
        RandomSource rnd = level.random;
        com.xtrip.golemgarage.Hints.show(player, "raid");
        int golems = golemCount(level, player.getUUID());
        int count = Math.min(12, 3 + golems / 4);
        boolean titan = forceTitan || golems >= Config.get(Config.TITAN_MIN_GOLEMS::get, 12) && rnd.nextInt(3) == 0;
        BlockPos home = player.blockPosition();
        BlockPos spawn = null;
        for (int tries = 0; tries < 12 && spawn == null; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            int d = 40 + rnd.nextInt(10);
            int x = home.getX() + (int) (Math.cos(a) * d);
            int z = home.getZ() + (int) (Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p.below()).getFluidState().isEmpty() && level.getBlockState(p.below()).isSolid()) {
                spawn = p;
            }
        }
        if (spawn == null) {
            return 0;
        }
        List<UUID> raiders = new ArrayList<>();
        // The convoy rig, driven by the first raider, the rest walking alongside.
        RigEntity rig = ModEntities.RIG.get().create(level);
        if (rig != null) {
            rig.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, rnd.nextFloat() * 360, 0);
            rig.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null);
            rig.addTag(RigEntity.ROGUE_TAG);
            // Raiders turn up in War Rigs, sometimes Interceptors; a Titan's convoy brings a Juggernaut.
            rig.setVariant(titan ? com.xtrip.golemgarage.entity.RigVariant.JUGGERNAUT
                    : rnd.nextInt(3) == 0 ? com.xtrip.golemgarage.entity.RigVariant.INTERCEPTOR
                    : com.xtrip.golemgarage.entity.RigVariant.WAR_RIG);
            rig.fit(RigEntity.RAM);
            rig.setFuel(20 * 60 * 3);
            rig.setRaidTarget(home);
            level.addFreshEntity(rig);
            raiders.add(rig.getUUID());
        }
        for (int i = 0; i < count; i++) {
            BlockPos at = spawn.offset(rnd.nextInt(7) - 3, 0, rnd.nextInt(7) - 3);
            at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
            GolemEntity g = rogue(level, at, rnd, false);
            if (g == null) {
                continue;
            }
            g.setRaidTarget(home);
            if (i < 2 && rig != null) {
                g.startRiding(rig, true);
            }
            raiders.add(g.getUUID());
        }
        if (titan) {
            GolemEntity t = rogue(level, spawn.offset(4, 0, 4), rnd, true);
            if (t != null) {
                t.setRaidTarget(home);
                raiders.add(t.getUUID());
            }
        }
        ServerBossEvent bar = new ServerBossEvent(Component.translatable("golemgarage.raid.bar"),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        bar.addPlayer(player);
        ACTIVE.add(new Raid(player.getUUID(), raiders, bar, level, raiders.size(), level.getGameTime()));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("golemgarage.raid.title")
                .withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(
                titan ? "golemgarage.raid.subtitle_titan" : "golemgarage.raid.subtitle", dir(home, spawn))));
        level.playSound(null, home, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64f, 0.8f);
        return raiders.size();
    }

    private static Component dir(BlockPos from, BlockPos to) {
        double a = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
        String[] names = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        int i = (int) Math.round(((a % 360) + 360) % 360 / 45) % 8;
        return Component.translatable("golemgarage.dir." + names[i]);
    }

    /** A rogue golem in mismatched, rusted parts. Titans are huge, tough and hit like a truck. */
    public static GolemEntity rogue(ServerLevel level, BlockPos at, RandomSource rnd, boolean titan) {
        GolemEntity g = ModEntities.GOLEM.get().create(level);
        if (g == null) {
            return null;
        }
        Map<PartSlot, PartInstance> parts = new EnumMap<>(PartSlot.class);
        for (PartSlot slot : PartSlot.values()) {
            GolemMaterial m = Salvage.material(rnd, titan && rnd.nextBoolean());
            PartInstance p = new PartInstance(Salvage.shape(rnd, slot), m);
            parts.put(slot, new PartInstance(p.shape(), p.materialId(), (int) (p.maxDurability() * 0.4), 3, false));
        }
        CompoundTag tag = GolemData.preset(Salvage.material(rnd, false), "sentry_head", "plated_chest", "golem_fists",
                "stub_legs", "redstone_core");
        tag.put("Parts", GolemData.writeParts(parts));
        tag.putInt("Paint", titan ? 14 : rnd.nextBoolean() ? 15 : 12);
        g.importData(tag);
        g.addTag("golemgarage_rogue");
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360, 0);
        g.setCustomName(Component.translatable(titan ? "entity.golemgarage.junk_titan" : "entity.golemgarage.rogue")
                .withStyle(titan ? ChatFormatting.DARK_RED : ChatFormatting.RED));
        g.setCustomNameVisible(titan);
        if (titan) {
            g.addTag("golemgarage_titan");
            g.getAttribute(Attributes.SCALE).setBaseValue(2.2);
            g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(320);
            g.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18);
            g.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            g.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
            g.setPersistenceRequired();
        }
        g.setHealth(g.getMaxHealth());
        level.addFreshEntity(g);
        return g;
    }

    private static void tickRaids(long dayTime) {
        for (Raid r : new ArrayList<>(ACTIVE)) {
            List<Entity> alive = new ArrayList<>();
            for (UUID id : r.raiders()) {
                Entity e = r.level().getEntity(id);
                if (e != null && e.isAlive() && !(e instanceof RigEntity rig && rig.getPassengers().isEmpty() && rig.hasArrived())) {
                    alive.add(e);
                }
            }
            r.bar().setProgress(Math.max(0f, alive.size() / (float) Math.max(1, r.total())));
            boolean dawn = dayTime < 13000 && r.level().getGameTime() - r.started() > 200;
            if (alive.isEmpty() || dawn) {
                r.bar().removeAllPlayers();
                ACTIVE.remove(r);
                ServerPlayer p = r.level().getServer().getPlayerList().getPlayer(r.player());
                if (dawn) {
                    alive.forEach(Entity::discard);
                } else if (p != null) {
                    p.displayClientMessage(Component.translatable("golemgarage.raid.won").withStyle(ChatFormatting.GREEN), false);
                    r.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
                    com.xtrip.golemgarage.advancement.ModTriggers.award(p, "raid_won");
                }
            }
        }
    }

    public static boolean active(Level level) {
        return !ACTIVE.isEmpty();
    }
}
