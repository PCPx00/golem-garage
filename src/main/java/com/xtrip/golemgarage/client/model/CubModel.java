package com.xtrip.golemgarage.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.entity.CubEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A chibi golem: big head, little sprout, tiny arms and legs. Blinks, tilts, hops and sits. */
public class CubModel extends EntityModel<CubEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("golemgarage", "cub"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart sprout;
    private final ModelPart eyelids;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CubModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.sprout = head.getChild("sprout");
        this.eyelids = head.getChild("eyelids");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /** Copies this Cub's current pose onto an outfit model's matching parts. */
    public void copyPoseTo(ModelPart outfitRoot) {
        outfitRoot.getChild("head").copyFrom(head);
        outfitRoot.getChild("body").copyFrom(body);
        outfitRoot.getChild("right_arm").copyFrom(rightArm);
        outfitRoot.getChild("left_arm").copyFrom(leftArm);
        outfitRoot.getChild("right_leg").copyFrom(rightLeg);
        outfitRoot.getChild("left_leg").copyFrom(leftLeg);
    }

    public static LayerDefinition createBodyLayer() {
        return CubGeometry.create();
    }

    @Override
    public void setupAnim(CubEntity cub, float limbSwing, float amount, float age, float yaw, float pitch) {
        float hop = 0;
        if (amount > 0.05f) {
            hop = -Math.abs(Mth.sin(limbSwing * 0.7f)) * 1.6f * Math.min(1f, amount * 1.5f);
        }
        if (cub.isExcited()) {
            hop = -Math.abs(Mth.sin(age * 0.6f)) * 3.0f;
        }
        boolean sitting = cub.isSitting();
        float sitDrop = sitting ? 2.5f : 0;

        head.y = 16 + hop + sitDrop;
        body.y = 16 + hop + sitDrop;
        rightArm.y = 16.5f + hop + sitDrop;
        leftArm.y = 16.5f + hop + sitDrop;
        rightLeg.y = 21 + hop + (sitting ? 1.5f : 0);
        leftLeg.y = 21 + hop + (sitting ? 1.5f : 0);

        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD * 0.6f;
        head.zRot = Mth.sin(age * 0.045f + cub.getId()) * 0.09f;
        if (cub.isSleepy()) {
            head.xRot = 0.35f + Mth.sin(age * 0.08f) * 0.05f;
            head.zRot = 0.15f;
        }
        sprout.zRot = Mth.sin(age * 0.11f) * 0.25f;
        sprout.xRot = Mth.sin(age * 0.07f) * 0.1f;

        int blinkCycle = (int) (age + cub.getId() * 13) % 90;
        eyelids.visible = cub.isSleepy() || blinkCycle < 3;

        if (sitting) {
            rightLeg.xRot = -1.45f;
            leftLeg.xRot = -1.45f;
            rightArm.xRot = -0.4f;
            leftArm.xRot = -0.4f;
        } else {
            float walk = Mth.cos(limbSwing * 0.9f) * 1.2f * amount;
            rightLeg.xRot = walk;
            leftLeg.xRot = -walk;
            rightArm.xRot = -walk * 0.8f;
            leftArm.xRot = walk * 0.8f;
        }
        float flap = cub.isExcited() ? Mth.sin(age * 1.2f) * 0.8f : Mth.sin(age * 0.08f) * 0.05f;
        rightArm.zRot = 0.15f + Math.abs(flap);
        leftArm.zRot = -0.15f - Math.abs(flap);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(poseStack, buffer, light, overlay, color);
    }
}
