package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.Config;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Dust storms: now and then on a clear day, a storm blows through the dry biomes (deserts, badlands, savannas) for a
 * few minutes. Out in the open it wears golem parts three times as fast as sand does, shoves rigs sideways, and
 * hides the horizon behind a wall of dust.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID)
public final class DustStorms {
    private static long until;
    private static float windYaw;

    private DustStorms() {
    }

    public static boolean active(Level level) {
        return level.dimension() == Level.OVERWORLD && level.getGameTime() < until;
    }

    /** Whether a storm is blowing at this spot: a storm is on, the biome is dry, and the sky is open above it. */
    public static boolean at(Level level, BlockPos pos) {
        return active(level) && dry(level, pos) && level.canSeeSky(pos.above(2));
    }

    static boolean dry(Level level, BlockPos pos) {
        var b = level.getBiome(pos);
        return b.is(Tags.Biomes.IS_DESERT) || b.is(Tags.Biomes.IS_BADLANDS) || b.is(Tags.Biomes.IS_SAVANNA);
    }

    /** Starts a storm now (for /gg storm). */
    public static void start(ServerLevel level, int seconds) {
        until = level.getGameTime() + seconds * 20L;
        windYaw = level.random.nextFloat() * 360f;
        for (ServerPlayer p : level.players()) {
            if (dry(level, p.blockPosition())) {
                p.displayClientMessage(Component.translatable("golemgarage.storm.start").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    public static void stop(ServerLevel level) {
        until = 0;
        sync(level);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long t = level.getGameTime();
        // Once a day, around mid-morning, a one-in-eight chance of a storm.
        if (Config.get(Config.DUST_STORMS::get, true) && !active(level) && level.getDayTime() % 24000 == 3000
                && !level.isRaining() && level.random.nextInt(8) == 0) {
            start(level, 120 + level.random.nextInt(180));
        }
        if (t % 20 == 0) {
            sync(level);
        }
        if (!active(level)) {
            return;
        }
        windYaw += (level.random.nextFloat() - 0.5f) * 2f;
        Vec3 wind = Vec3.directionFromRotation(0, windYaw);
        for (ServerPlayer p : level.players()) {
            if (!at(level, p.blockPosition())) {
                continue;
            }
            // Sand blowing past.
            var dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
            for (int i = 0; i < 6; i++) {
                double ox = (level.random.nextDouble() - 0.5) * 24, oz = (level.random.nextDouble() - 0.5) * 24;
                level.sendParticles(p, dust, true, p.getX() + ox, p.getY() + level.random.nextDouble() * 6, p.getZ() + oz,
                        1, wind.x * 2, 0.1, wind.z * 2, 0.4);
            }
            if (t % 80 == 0) {
                level.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 0.25f, 0.5f);
            }
        }
        // The wind shoves rigs off course.
        if (t % 5 == 0) {
            for (RigEntity rig : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(RigEntity.class),
                    r -> at(level, r.blockPosition()))) {
                rig.push(wind.x * 0.06, 0, wind.z * 0.06);
            }
        }
    }

    private static void sync(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            boolean on = p.level() == level && at(level, p.blockPosition());
            PacketDistributor.sendToPlayer(p, new com.xtrip.golemgarage.network.StormPayload(on));
        }
    }
}
