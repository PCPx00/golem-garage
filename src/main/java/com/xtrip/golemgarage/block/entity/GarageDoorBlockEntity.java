package com.xtrip.golemgarage.block.entity;

import com.xtrip.golemgarage.block.GarageDoorBlock;
import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One block of a roller door. The bottom corner block of each door runs it: every few ticks it checks for
 * golems or cubs nearby, redstone power and the held-open flag, and opens or closes the whole door.
 * On the client it eases the curtain up and down for the renderer.
 */
public class GarageDoorBlockEntity extends BlockEntity {
    private static final int MAX_DOOR = 64;
    private static final int STAY_OPEN_TICKS = 40;
    private static final float ROLL_SPEED = 1f / 22f;

    /** Right-clicked open: stays open until right-clicked again. */
    private boolean held;
    /** Leader only: the last game time something wanted the door open. */
    private long lastWanted = Long.MIN_VALUE / 2;

    /** Client only: how far the curtain is rolled up, 0..1. */
    private float rolled;
    private float rolledO;

    public GarageDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGE_DOOR.get(), pos, state);
        rolled = rolledO = state.getValue(GarageDoorBlock.OPEN) ? 1f : 0f;
    }

    public float rolled(float partial) {
        return rolledO + (rolled - rolledO) * partial;
    }

    // =================================================================== the door as a whole
    /** The direction along the door's width. */
    public static Direction across(BlockState state) {
        return state.getValue(GarageDoorBlock.FACING).getClockWise();
    }

    private static boolean sameDoor(BlockState a, BlockState b) {
        return b.getBlock() instanceof GarageDoorBlock && b.getValue(GarageDoorBlock.FACING) == a.getValue(GarageDoorBlock.FACING);
    }

    /** Every block of the door this block belongs to. */
    public static List<BlockPos> door(Level level, BlockPos start) {
        BlockState first = level.getBlockState(start);
        List<BlockPos> out = new ArrayList<>();
        if (!(first.getBlock() instanceof GarageDoorBlock)) {
            return out;
        }
        Direction side = across(first);
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && out.size() < MAX_DOOR) {
            BlockPos p = queue.poll();
            out.add(p);
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, side, side.getOpposite()}) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && sameDoor(first, level.getBlockState(n))) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    /** True for the block that runs the door: nothing of the door below it or to its left. */
    private boolean isLeader(Level level, BlockState state) {
        return !sameDoor(state, level.getBlockState(worldPosition.below()))
                && !sameDoor(state, level.getBlockState(worldPosition.relative(across(state).getOpposite())));
    }

    private static void setOpen(Level level, List<BlockPos> door, boolean open) {
        for (BlockPos p : door) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof GarageDoorBlock && s.getValue(GarageDoorBlock.OPEN) != open) {
                level.setBlock(p, s.setValue(GarageDoorBlock.OPEN, open), 3);
            }
        }
        if (!door.isEmpty()) {
            BlockPos at = door.get(0);
            level.playSound(null, at, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 0.6f, open ? 0.55f : 0.5f);
            level.playSound(null, at, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.5f, 0.7f);
            level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, at);
        }
    }

    /** Right-click: keep the whole door open, or let it close again. */
    public static void toggleHeld(Level level, BlockPos pos) {
        List<BlockPos> door = door(level, pos);
        if (door.isEmpty()) {
            return;
        }
        boolean open = !level.getBlockState(pos).getValue(GarageDoorBlock.OPEN);
        for (BlockPos p : door) {
            if (level.getBlockEntity(p) instanceof GarageDoorBlockEntity be) {
                be.held = open;
                be.setChanged();
            }
        }
        setOpen(level, door, open);
    }

    // =================================================================== ticking
    public static void serverTick(Level level, BlockPos pos, BlockState state, GarageDoorBlockEntity door) {
        if ((level.getGameTime() + pos.asLong()) % 4 != 0 || !door.isLeader(level, state)) {
            return;
        }
        List<BlockPos> blocks = door(level, pos);
        boolean want = false;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
            if (!want && (level.hasNeighborSignal(p)
                    || level.getBlockEntity(p) instanceof GarageDoorBlockEntity be && be.held)) {
                want = true;
            }
        }
        if (!want) {
            AABB near = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(2.2, 0.5, 2.2);
            want = !level.getEntitiesOfClass(Entity.class, near,
                    e -> e.isAlive() && (e instanceof GolemEntity || e instanceof CubEntity)).isEmpty();
        }
        long now = level.getGameTime();
        if (want) {
            door.lastWanted = now;
        }
        boolean open = state.getValue(GarageDoorBlock.OPEN);
        if (want && !open) {
            setOpen(level, blocks, true);
        } else if (!want && open && now - door.lastWanted >= STAY_OPEN_TICKS) {
            setOpen(level, blocks, false);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GarageDoorBlockEntity door) {
        door.rolledO = door.rolled;
        float target = state.getValue(GarageDoorBlock.OPEN) ? 1f : 0f;
        if (door.rolled < target) {
            door.rolled = Math.min(target, door.rolled + ROLL_SPEED);
        } else if (door.rolled > target) {
            door.rolled = Math.max(target, door.rolled - ROLL_SPEED * 1.3f);
        }
    }

    // =================================================================== save
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Held", held);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        held = tag.getBoolean("Held");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
