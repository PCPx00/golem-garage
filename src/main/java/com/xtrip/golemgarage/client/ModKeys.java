package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.network.PickUpPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Key bindings. "Pick up golem" (G by default) puts the golem or cub you're looking at in your inventory. */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.golemgarage";
    public static final KeyMapping PICK_UP = new KeyMapping("key.golemgarage.pick_up", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    @EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(PICK_UP);
        }
    }

    @EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
    public static final class Handler {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (PICK_UP.consumeClick()) {
                if (mc.player == null || mc.screen != null) {
                    continue;
                }
                Entity target = lookedAt(mc, 6.0);
                if (target instanceof GolemEntity || target instanceof CubEntity) {
                    PacketDistributor.sendToServer(new PickUpPayload(target.getId()));
                } else {
                    mc.player.displayClientMessage(Component.translatable("golemgarage.msg.look_at_golem"), true);
                }
            }
        }
    }

    /** The golem or cub under the crosshair, up to {@code reach} blocks away (a little further than normal reach). */
    static Entity lookedAt(Minecraft mc, double reach) {
        if (mc.hitResult instanceof EntityHitResult hit
                && (hit.getEntity() instanceof GolemEntity || hit.getEntity() instanceof CubEntity)) {
            return hit.getEntity();
        }
        Entity cam = mc.getCameraEntity();
        if (cam == null) {
            return null;
        }
        Vec3 eye = cam.getEyePosition(1f);
        Vec3 look = cam.getViewVector(1f);
        Vec3 end = eye.add(look.scale(reach));
        double blockDist = reach * reach;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            blockDist = mc.hitResult.getLocation().distanceToSqr(eye);
        }
        AABB box = cam.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, eye, end, box,
                e -> e instanceof GolemEntity || e instanceof CubEntity, blockDist);
        return hit == null ? null : hit.getEntity();
    }

    private ModKeys() {
    }
}
