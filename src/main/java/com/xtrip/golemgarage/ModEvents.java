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

    @SubscribeEvent
    public static void onReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new com.xtrip.golemgarage.data.DatapackMaterials());
    }

    /** Sends the datapack materials to players as they join and after /reload. */
    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var payload = new com.xtrip.golemgarage.network.MaterialsPayload(com.xtrip.golemgarage.data.DatapackMaterials.write(
                com.xtrip.golemgarage.golem.GolemMaterials.extras()));
        event.getRelevantPlayers().forEach(p -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload));
    }

    /** Golems earn veterancy from kills: more for rogues, a lot for a Junk Titan. */
    @SubscribeEvent
    public static void onDeathVeterancy(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        var dead = event.getEntity();
        var killer = event.getSource().getEntity();
        com.xtrip.golemgarage.entity.GolemEntity golem = killer instanceof com.xtrip.golemgarage.entity.GolemEntity g ? g
                : event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile p
                && p.getOwner() instanceof com.xtrip.golemgarage.entity.GolemEntity g2 ? g2 : null;
        if (golem == null || dead.level().isClientSide) {
            return;
        }
        int xp = dead instanceof com.xtrip.golemgarage.entity.GolemEntity rg && rg.isTitan() ? 200
                : dead instanceof com.xtrip.golemgarage.entity.GolemEntity rg2 && rg2.isRogue() ? 15
                : Math.max(3, (int) (dead.getMaxHealth() / 2));
        golem.addXp(xp, true);
    }

    /** No friendly fire: your golems, rigs and turrets never hurt you, each other or your cubs. */
    @SubscribeEvent
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        java.util.UUID attacker = ownerOf(event.getSource().getEntity());
        if (attacker == null) {
            return;
        }
        var victim = event.getEntity();
        java.util.UUID victimOwner = victim instanceof Player p ? p.getUUID() : ownerOf(victim);
        if (attacker.equals(victimOwner)) {
            event.setCanceled(true);
        }
    }

    @org.jetbrains.annotations.Nullable
    private static java.util.UUID ownerOf(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity e) {
        if (e instanceof com.xtrip.golemgarage.entity.GolemEntity g) {
            return g.isRogue() ? null : g.getOwnerUuid();
        }
        if (e instanceof com.xtrip.golemgarage.entity.CubEntity c) {
            return c.getOwnerUuid();
        }
        if (e instanceof com.xtrip.golemgarage.entity.RigEntity r) {
            return r.isRogue() ? null : r.getOwnerUuid();
        }
        return null;
    }

    @SubscribeEvent
    public static void onCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        com.xtrip.golemgarage.command.GolemCommands.register(event.getDispatcher());
    }

    /** Small server-side scheduler: each task runs every tick with its age until it returns true. */
    private static final java.util.List<Object[]> LATER = new java.util.ArrayList<>();

    public static void later(net.minecraft.server.MinecraftServer server, int maxTicks,
                             java.util.function.IntPredicate task) {
        LATER.add(new Object[]{task, 0, maxTicks});
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (LATER.isEmpty()) {
            return;
        }
        for (Object[] t : new java.util.ArrayList<>(LATER)) {
            int age = (Integer) t[1] + 1;
            t[1] = age;
            boolean done = ((java.util.function.IntPredicate) t[0]).test(age) || age > (Integer) t[2] + 5;
            if (done) {
                LATER.remove(t);
            }
        }
    }

    /** killsCountAsOwner: a golem's hit marks its owner as the attacker, so XP and player-only loot drop. */
    @SubscribeEvent
    public static void onGolemHit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof com.xtrip.golemgarage.entity.GolemEntity golem
                && com.xtrip.golemgarage.Config.get(com.xtrip.golemgarage.Config.KILLS_COUNT_AS_OWNER::get, false)
                && !(event.getEntity() instanceof Player)) {
            Player owner = golem.getOwner();
            if (owner != null && owner.level() == golem.level()) {
                event.getEntity().setLastHurtByPlayer(owner);
            }
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

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().getItem() instanceof com.xtrip.golemgarage.item.PartItem) {
            Hints.show(event.getEntity(), "first_part");
        }
    }
}
