package com.xtrip.golemgarage.golem;

import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers every placed golem on the server (owner, name, where it was last seen), so commands can list,
 * find and recall golems even when they're in unloaded chunks. Golems update their entry as they move.
 */
public class GolemRegistry extends SavedData {
    private static final String ID = "golemgarage_golems";

    public record Entry(UUID golem, @Nullable UUID owner, String name, ResourceKey<Level> dimension, BlockPos pos,
                        long seen, int group) {
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    /** Golems purged while unloaded: removed the next time their chunk loads. */
    private final java.util.Set<UUID> purged = new java.util.HashSet<>();

    public void markPurged(UUID golem) {
        purged.add(golem);
        entries.remove(golem);
        setDirty();
    }

    public boolean takePurged(UUID golem) {
        if (purged.remove(golem)) {
            setDirty();
            return true;
        }
        return false;
    }

    public static GolemRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GolemRegistry::new, GolemRegistry::load, null), ID);
    }

    public void update(GolemEntity g) {
        String name = g.hasCustomName() && g.getCustomName() != null ? g.getCustomName().getString() : "";
        Entry old = entries.get(g.getUUID());
        Entry e = new Entry(g.getUUID(), g.getOwnerUuid(), name, g.level().dimension(), g.blockPosition(),
                g.level().getGameTime(), g.getGroup());
        if (old == null || !old.pos().equals(e.pos()) || !old.name().equals(name) || old.group() != e.group()
                || !java.util.Objects.equals(old.owner(), e.owner()) || old.dimension() != e.dimension()) {
            entries.put(g.getUUID(), e);
            setDirty();
        }
    }

    public void remove(UUID golem) {
        if (entries.remove(golem) != null) {
            setDirty();
        }
    }

    public List<Entry> all() {
        return new ArrayList<>(entries.values());
    }

    public List<Entry> forOwner(UUID owner) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (owner.equals(e.owner())) {
                out.add(e);
            }
        }
        return out;
    }

    @Nullable
    public Entry get(UUID golem) {
        return entries.get(golem);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.golem());
            if (e.owner() != null) {
                t.putUUID("Owner", e.owner());
            }
            t.putString("Name", e.name());
            t.putString("Dim", e.dimension().location().toString());
            t.putLong("Pos", e.pos().asLong());
            t.putLong("Seen", e.seen());
            t.putInt("Group", e.group());
            list.add(t);
        }
        tag.put("Golems", list);
        ListTag gone = new ListTag();
        for (UUID u : purged) {
            gone.add(net.minecraft.nbt.NbtUtils.createUUID(u));
        }
        tag.put("Purged", gone);
        return tag;
    }

    private static GolemRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        GolemRegistry r = new GolemRegistry();
        for (Tag raw : tag.getList("Golems", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim")));
            r.entries.put(t.getUUID("Id"), new Entry(t.getUUID("Id"), t.hasUUID("Owner") ? t.getUUID("Owner") : null,
                    t.getString("Name"), dim, BlockPos.of(t.getLong("Pos")), t.getLong("Seen"), t.getInt("Group")));
        }
        for (Tag t : tag.getList("Purged", Tag.TAG_INT_ARRAY)) {
            r.purged.add(net.minecraft.nbt.NbtUtils.loadUUID(t));
        }
        return r;
    }
}
