package com.xtrip.golemgarage.block.entity;

import com.xtrip.golemgarage.block.GarageRadioBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Plays the jukebox songs on shuffle and moves on to the next one when a song ends. */
public class GarageRadioBlockEntity extends BlockEntity {
    private int song = -1;
    private long endsAt;

    public GarageRadioBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGE_RADIO.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GarageRadioBlockEntity radio) {
        if (state.getValue(GarageRadioBlock.PLAYING) && level.getGameTime() >= radio.endsAt && level.getGameTime() % 20 == 0) {
            radio.next(null);
        }
    }

    /** Starts a random song other than the one playing now. */
    public void next(@Nullable Player who) {
        if (level == null) {
            return;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG);
        int n = registry.size();
        if (n == 0) {
            return;
        }
        int pick = level.random.nextInt(n);
        if (pick == song && n > 1) {
            pick = (pick + 1) % n;
        }
        JukeboxSong s = registry.byId(pick);
        if (s == null) {
            return;
        }
        song = pick;
        endsAt = level.getGameTime() + s.lengthInTicks() + 40;
        level.levelEvent(1011, worldPosition, 0);
        level.levelEvent(null, 1010, worldPosition, pick);
        level.setBlock(worldPosition, getBlockState().setValue(GarageRadioBlock.PLAYING, true), 3);
        if (who != null) {
            who.displayClientMessage(Component.translatable("golemgarage.radio.now_playing", s.description()), true);
        }
        setChanged();
    }

    public void stop() {
        if (level == null) {
            return;
        }
        level.levelEvent(1011, worldPosition, 0);
        level.setBlock(worldPosition, getBlockState().setValue(GarageRadioBlock.PLAYING, false), 3);
        song = -1;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Song", song);
        tag.putLong("EndsAt", endsAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        song = tag.getInt("Song");
        endsAt = tag.getLong("EndsAt");
    }
}
