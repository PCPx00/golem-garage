package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.RigEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the Wasteland Rig with the shared car model: wheels roll, the body shakes while the engine runs. */
public class RigRenderer extends EntityRenderer<RigEntity> {
    public RigRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.2f;
    }

    @Override
    public void render(RigEntity rig, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partial, rig.yRotO, rig.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(-(bodyYaw + 90)));
        // Tip over a little when hurt.
        if (rig.hurtTime > 0) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(rig.hurtTime * 1.3f) * rig.hurtTime * 0.4f));
        }
        pose.scale(RigEntity.SCALE, RigEntity.SCALE, RigEntity.SCALE);
        float time = rig.tickCount + partial;
        float wheels = Mth.lerp(partial, rig.wheelAngleO, rig.wheelAngle);
        CarModel.render(Minecraft.getInstance().getBlockRenderer(), pose, buffers, light, time,
                new CarModel.Options(rig.variant(), rig.has(RigEntity.RAM), rig.has(RigEntity.TURRET),
                        rig.has(RigEntity.PLATING), wheels, rig.turretYaw(), rig.engineOn(), false, rig.boosting(),
                        rig.flash(partial)));
        pose.popPose();
        super.render(rig, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RigEntity rig) {
        return ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "textures/entity/garage_skin.png");
    }
}
