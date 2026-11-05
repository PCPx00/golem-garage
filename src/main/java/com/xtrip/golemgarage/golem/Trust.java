package com.xtrip.golemgarage.golem;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trusted players: each player can let friends open, edit, link, command and pick up their golems and cubs
 * ({@code /gg trust <player>}).
 */
public class Trust extends SavedData {
    private static final String ID = "golemgarage_trust";
    private final Map<UUID, Set<UUID>> trusted = new HashMap<>();

    public static Trust get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Trust::new, Trust::load, null), ID);
    }

    /** Whether {@code player} may act on things owned by {@code owner}. */
    public static boolean trusts(Player player, UUID owner) {
        if (owner == null || owner.equals(player.getUUID())) {
            return true;
        }
        MinecraftServer server = player.getServer();
        return server != null && get(server).trusted.getOrDefault(owner, Set.of()).contains(player.getUUID());
    }

    public boolean set(UUID owner, UUID friend, boolean on) {
        Set<UUID> s = trusted.computeIfAbsent(owner, k -> new HashSet<>());
        boolean changed = on ? s.add(friend) : s.remove(friend);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Set<UUID> of(UUID owner) {
        return trusted.getOrDefault(owner, Set.of());
    }

    private static Trust load(CompoundTag tag, HolderLookup.Provider registries) {
        Trust t = new Trust();
        for (String k : tag.getAllKeys()) {
            Set<UUID> s = new HashSet<>();
            for (Tag e : tag.getList(k, Tag.TAG_INT_ARRAY)) {
                s.add(NbtUtils.loadUUID(e));
            }
            t.trusted.put(UUID.fromString(k), s);
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        trusted.forEach((owner, s) -> {
            ListTag list = new ListTag();
            s.forEach(u -> list.add(NbtUtils.createUUID(u)));
            tag.put(owner.toString(), list);
        });
        return tag;
    }
}
