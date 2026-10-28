package com.xtrip.golemgarage;

import com.xtrip.golemgarage.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Game events: every player gets one Golem Engineer's Manual the first time they join. */
@EventBusSubscriber(modid = GolemGarage.MOD_ID)
public final class ModEvents {
    private static final String GOT_MANUAL = "golemgarage:got_manual";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !Config.GIVE_MANUAL.get()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GOT_MANUAL)) {
            return;
        }
        persisted.putBoolean(GOT_MANUAL, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        ItemStack book = new ItemStack(ModItems.MANUAL.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    /** Golems that kill a monster earn their owner the "Bodyguard" advancements. */
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof com.xtrip.golemgarage.entity.GolemEntity golem
                && event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            Player owner = golem.getOwner();
            com.xtrip.golemgarage.advancement.ModTriggers.award(owner, "golem_kill");
            if (event.getEntity().getType() == net.minecraft.world.entity.EntityType.WARDEN) {
                com.xtrip.golemgarage.advancement.ModTriggers.award(owner, "golem_kill_warden");
            }
        }
    }

    private ModEvents() {
    }
}
