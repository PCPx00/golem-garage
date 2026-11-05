package com.xtrip.golemgarage.world;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.entity.ModEntities;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scavenging trips. Use a map on a rig with golems aboard and it drives off into the wasteland. A few minutes later
 * it rolls back to where it left with the crew a little worn, some experience, and a salvage barrel of loot. The
 * bigger the crew, the better the haul; a Juggernaut's armour brings back more, an Interceptor comes back sooner.
 */
@EventBusSubscriber(modid = GolemGarage.MOD_ID)
public final class Scavenging extends SavedData {
    private static final String ID = "golemgarage_trips";
    public static final int TRIP_TICKS = 20 * 60 * 4;

    private record Trip(UUID owner, BlockPos home, String dim, long back, CompoundTag rig, List<CompoundTag> crew) {
    }

    private final List<Trip> trips = new ArrayList<>();

    public static Scavenging get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scavenging::new, Scavenging::load, null), ID);
    }

    /** Sends the rig off. Returns a message key if it can't go. */
    public static String depart(RigEntity rig, Player player) {
        if (!(rig.level() instanceof ServerLevel level)) {
            return null;
        }
        List<GolemEntity> crew = new ArrayList<>();
        for (Entity e : rig.getPassengers()) {
            if (e instanceof GolemEntity g && !g.isRogue()) {
                crew.add(g);
            }
        }
        if (crew.isEmpty()) {
            return "golemgarage.trip.no_crew";
        }
        if (rig.getFuel() < RigEntity.MAX_FUEL / 8) {
            return "golemgarage.trip.no_fuel";
        }
        if (rig.getPassengers().stream().anyMatch(e -> e instanceof Player)) {
            return "golemgarage.trip.get_out";
        }
        List<CompoundTag> crewTags = new ArrayList<>();
        for (GolemEntity g : crew) {
            CompoundTag t = new CompoundTag();
            g.exportData(t);
            crewTags.add(t);
        }
        CompoundTag rigTag = (CompoundTag) rig.toItem().save(level.registryAccess());
        long time = rig.variant() == com.xtrip.golemgarage.entity.RigVariant.INTERCEPTOR ? TRIP_TICKS * 2 / 3 : TRIP_TICKS;
        get(level.getServer()).add(new Trip(player.getUUID(), rig.blockPosition(), level.dimension().location().toString(),
                level.getGameTime() + time, rigTag, crewTags));
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, rig.getX(), rig.getY() + 0.5, rig.getZ(), 30, 1.2, 0.3, 1.2, 0.04);
        level.playSound(null, rig.blockPosition(), com.xtrip.golemgarage.ModSounds.RIG_HORN.get(), SoundSource.NEUTRAL, 1.2f, 1f);
        level.playSound(null, rig.blockPosition(), com.xtrip.golemgarage.ModSounds.RIG_START.get(), SoundSource.NEUTRAL, 1.2f, 1.1f);
        for (GolemEntity g : crew) {
            g.discard();
        }
        rig.discard();
        com.xtrip.golemgarage.advancement.ModTriggers.award(player, "scavenge");
        player.displayClientMessage(Component.translatable("golemgarage.trip.left", time / 1200).withStyle(ChatFormatting.GOLD), false);
        return null;
    }

    private void add(Trip t) {
        trips.add(t);
        setDirty();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld().getGameTime() % 20 != 0) {
            return;
        }
        Scavenging data = get(server);
        for (Trip t : new ArrayList<>(data.trips)) {
            ServerLevel level = null;
            for (ServerLevel l : server.getAllLevels()) {
                if (l.dimension().location().toString().equals(t.dim())) {
                    level = l;
                }
            }
            if (level == null || level.getGameTime() < t.back() || !level.isLoaded(t.home())) {
                continue;
            }
            data.trips.remove(t);
            data.setDirty();
            data.arrive(level, t);
        }
    }

    private void arrive(ServerLevel level, Trip t) {
        var rnd = level.random;
        BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t.home());
        ItemStack rigItem = ItemStack.parseOptional(level.registryAccess(), t.rig());
        RigEntity rig = ModEntities.RIG.get().create(level);
        if (rig != null) {
            rig.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360, 0);
            rig.fromItem(rigItem);
            rig.setOwner(t.owner());
            rig.setFuel(Math.max(0, rig.getFuel() - RigEntity.MAX_FUEL / 8));
            rig.setHealth(Math.max(4, rig.getHealth() - rnd.nextInt(20)));
            level.addFreshEntity(rig);
        }
        int crew = 0;
        for (CompoundTag ct : t.crew()) {
            GolemEntity g = ModEntities.GOLEM.get().create(level);
            if (g == null) {
                continue;
            }
            g.importData(ct);
            g.moveTo(at.getX() + 0.5 + rnd.nextInt(3) - 1, at.getY(), at.getZ() + 2.5, 0, 0);
            level.addFreshEntity(g);
            g.addXp(25, false);
            g.wearAll(rnd.nextInt(30) + 10);
            crew++;
        }
        // The haul: a barrel of salvage beside the rig, more with a bigger crew or a Juggernaut.
        int rolls = Math.min(4, crew) + (rig != null && rig.variant() == com.xtrip.golemgarage.entity.RigVariant.JUGGERNAUT ? 1 : 0);
        BlockPos barrel = at.east(2);
        level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 3);
        if (level.getBlockEntity(barrel) instanceof net.minecraft.world.Container c) {
            int slot = 0;
            for (int r = 0; r < rolls; r++) {
                for (ItemStack s : com.xtrip.golemgarage.world.Salvage.crate(rnd, r == rolls - 1)) {
                    if (slot < c.getContainerSize()) {
                        c.setItem(slot++, s);
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 1.2, 0.3, 1.2, 0.04);
        level.playSound(null, at, com.xtrip.golemgarage.ModSounds.RIG_HORN.get(), SoundSource.NEUTRAL, 1.5f, 1f);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(t.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("golemgarage.trip.back", at.getX(), at.getY(), at.getZ())
                    .withStyle(ChatFormatting.GOLD));
            owner.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    // =================================================================== saving
    private static Scavenging load(CompoundTag tag, HolderLookup.Provider registries) {
        Scavenging s = new Scavenging();
        for (Tag e : tag.getList("Trips", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) e;
            List<CompoundTag> crew = new ArrayList<>();
            for (Tag c : t.getList("Crew", Tag.TAG_COMPOUND)) {
                crew.add((CompoundTag) c);
            }
            s.trips.add(new Trip(t.getUUID("Owner"), BlockPos.of(t.getLong("Home")), t.getString("Dim"), t.getLong("Back"),
                    t.getCompound("Rig"), crew));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Trip t : trips) {
            CompoundTag c = new CompoundTag();
            c.putUUID("Owner", t.owner());
            c.putLong("Home", t.home().asLong());
            c.putString("Dim", t.dim());
            c.putLong("Back", t.back());
            c.put("Rig", t.rig());
            ListTag crew = new ListTag();
            crew.addAll(t.crew());
            c.put("Crew", crew);
            list.add(c);
        }
        tag.put("Trips", list);
        return tag;
    }

    /** For the tablet and /gg trips: how many trips this player has out and when the next is back. */
    public long nextBack(UUID owner) {
        return trips.stream().filter(t -> t.owner().equals(owner)).mapToLong(Trip::back).min().orElse(-1);
    }
}
