package com.xtrip.golemgarage.block.entity;

import com.xtrip.golemgarage.golem.GolemMaterial;
import com.xtrip.golemgarage.golem.GolemMaterials;
import com.xtrip.golemgarage.golem.PartShape;
import com.xtrip.golemgarage.golem.PartShapes;
import com.xtrip.golemgarage.golem.PartSlot;
import com.xtrip.golemgarage.item.PartItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Parts Fabricator. Pick a part shape (sneak + right-click), feed it a material (by hand, a hopper or a golem's
 * "deposit" job) and its robot arm welds the part together on the bench. Finished parts pop out of the bottom
 * into a hopper, or right-click to take them. It skips the shape's extra ingredients, so it costs a little more
 * material than crafting by hand.
 */
public class FabricatorBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int WORK_TICKS = 100;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int shapeIndex;
    private int progress;
    /** Powered, it welds twice as fast (20 FE/t). */
    private final com.xtrip.golemgarage.power.PowerBuffer power = new com.xtrip.golemgarage.power.PowerBuffer(50_000, 1_000, this::setChanged);

    public com.xtrip.golemgarage.power.PowerBuffer power() {
        return power;
    }

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    public static int cost(PartSlot slot) {
        return switch (slot) {
            case HEAD, CORE -> 6;
            case TORSO -> 9;
            case ARMS -> 8;
            case LEGS -> 7;
        };
    }

    public PartShape shape() {
        List<PartShape> all = PartShapes.ALL;
        return all.get(Math.floorMod(shapeIndex, all.size()));
    }

    @Nullable
    public GolemMaterial material() {
        if (input.isEmpty()) {
            return null;
        }
        for (GolemMaterial m : GolemMaterials.CRAFTABLE) {
            if (m.matches(input)) {
                return m;
            }
        }
        return null;
    }

    public boolean working() {
        return progress > 0;
    }

    public float progress(float partial) {
        return progress <= 0 ? 0f : Math.min(1f, (progress + partial) / WORK_TICKS);
    }

    public ItemStack preview() {
        GolemMaterial m = material();
        return PartItem.create(shape(), m == null ? GolemMaterials.IRON : m);
    }

    public ItemStack input() {
        return input;
    }

    // =================================================================== using it
    public void cycle(Player player, int dir) {
        shapeIndex = Math.floorMod(shapeIndex + dir, PartShapes.ALL.size());
        progress = 0;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("golemgarage.fab.shape",
                preview().getHoverName(), cost(shape().slot())), true);
        changed();
    }

    /** Puts the held material in. Returns how many were taken. */
    public int insert(ItemStack held) {
        if (held.isEmpty() || !canPlaceItem(0, held)) {
            return 0;
        }
        int room = Math.min(64, held.getMaxStackSize()) - input.getCount();
        int n = Math.min(room, held.getCount());
        if (n <= 0) {
            return 0;
        }
        if (input.isEmpty()) {
            input = held.copyWithCount(n);
        } else {
            input.grow(n);
        }
        held.shrink(n);
        changed();
        return n;
    }

    public ItemStack takeOutput() {
        ItemStack out = output;
        output = ItemStack.EMPTY;
        if (!out.isEmpty()) {
            changed();
        }
        return out;
    }

    public ItemStack takeInput() {
        ItemStack in = input;
        input = ItemStack.EMPTY;
        progress = 0;
        changed();
        return in;
    }

    // =================================================================== ticking
    public static void serverTick(Level level, BlockPos pos, BlockState state, FabricatorBlockEntity fab) {
        GolemMaterial m = fab.material();
        int need = cost(fab.shape().slot());
        if (m == null || fab.input.getCount() < need || !fab.output.isEmpty()) {
            if (fab.progress != 0) {
                fab.progress = 0;
                fab.changed();
            }
            return;
        }
        if (fab.progress == 0) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4f, 1.4f);
            fab.changed();
        }
        fab.progress++;
        if (fab.power.use(20)) {
            fab.progress++;
        }
        if (fab.progress % 12 == 0) {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.15f, 1.6f + level.random.nextFloat() * 0.3f);
        }
        if (fab.progress >= WORK_TICKS) {
            fab.input.shrink(need);
            fab.output = PartItem.create(fab.shape(), m);
            fab.progress = 0;
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.1f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0);
            }
            fab.changed();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FabricatorBlockEntity fab) {
        if (fab.progress > 0) {
            fab.progress = Math.min(WORK_TICKS - 1, fab.progress + 1);
            if (level.random.nextInt(2) == 0) {
                double a = level.getGameTime() * 0.3;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + Math.cos(a) * 0.15, pos.getY() + 1.25,
                        pos.getZ() + 0.5 + Math.sin(a) * 0.15, (level.random.nextDouble() - 0.5) * 0.2, 0.12,
                        (level.random.nextDouble() - 0.5) * 0.2);
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =================================================================== container (hoppers in, out the bottom)
    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? input : output;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack s = getItem(slot);
        ItemStack out = s.split(amount);
        if (!out.isEmpty()) {
            changed();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            input = stack;
        } else {
            output = stack;
        }
        changed();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        input = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return false;
        }
        if (!input.isEmpty()) {
            return ItemStack.isSameItemSameComponents(input, stack);
        }
        for (GolemMaterial m : GolemMaterials.CRAFTABLE) {
            if (m.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{1} : new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && canPlaceItem(0, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && side == Direction.DOWN;
    }

    // =================================================================== save and sync
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag pw = new CompoundTag();
        power.save(pw);
        tag.put("Power", pw);
        if (!input.isEmpty()) {
            tag.put("Input", input.save(registries));
        }
        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }
        tag.putInt("Shape", shapeIndex);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power.load(tag.getCompound("Power"));
        input = tag.contains("Input") ? ItemStack.parse(registries, tag.getCompound("Input")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        output = tag.contains("Output") ? ItemStack.parse(registries, tag.getCompound("Output")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        shapeIndex = tag.getInt("Shape");
        progress = tag.getInt("Progress");
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
