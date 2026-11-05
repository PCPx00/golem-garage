package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.entity.RigVariant;
import com.xtrip.golemgarage.item.RigItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rig items are little 3D models of the rig itself, in inventories, in hand and on the ground. */
public class RigItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final RigItemRenderer INSTANCE = new RigItemRenderer();

    private RigItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        RigVariant v = stack.getItem() instanceof RigItem r ? r.variant() : RigVariant.WAR_RIG;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        switch (ctx) {
            case GUI -> {
                pose.translate(0, 0.28, 0);
                pose.mulPose(Axis.XP.rotationDegrees(28));
                pose.mulPose(Axis.YP.rotationDegrees(-140));
                pose.scale(0.34f, 0.34f, 0.34f);
            }
            case FIXED -> {
                pose.translate(0, 0.3, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(0.36f, 0.36f, 0.36f);
            }
            case GROUND -> {
                pose.translate(0, 0.3, 0);
                pose.scale(0.25f, 0.25f, 0.25f);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                pose.translate(0, 0.45, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-60));
                pose.scale(0.3f, 0.3f, 0.3f);
            }
            default -> {
                pose.translate(0, 0.35, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(0.25f, 0.25f, 0.25f);
            }
        }
        float time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        CarModel.render(Minecraft.getInstance().getBlockRenderer(), pose, buffers, light, time,
                new CarModel.Options(v, false, false, false, 0f, 0f, false, false, false, 0f));
        pose.popPose();
    }
}
