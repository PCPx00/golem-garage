package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.power.PowerBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The Wind Turbine item: the whole windmill in miniature, slowly turning. */
public class PowerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final PowerItemRenderer INSTANCE = new PowerItemRenderer();

    private PowerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        pose.pushPose();
        float s;
        switch (ctx) {
            case GUI -> {
                pose.translate(0.5, 0.06, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(15));
                pose.mulPose(Axis.YP.rotationDegrees(-145));
                s = 0.205f;
            }
            case GROUND -> {
                pose.translate(0.5, 0.2, 0.5);
                s = 0.2f;
            }
            case FIXED -> {
                pose.translate(0.5, 0.05, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                s = 0.25f;
            }
            default -> {
                pose.translate(0.5, 0.3, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-140));
                s = 0.2f;
            }
        }
        pose.scale(s, s, s);
        float time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(PowerBlocks.WIND_TURBINE.get().defaultBlockState()
                .setValue(com.xtrip.golemgarage.block.WindTurbineBlock.FACING, net.minecraft.core.Direction.SOUTH),
                pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        WindTurbineRenderer.draw(pose, buffers, light, time * 2f);
        pose.popPose();
    }
}
