package com.xtrip.golemgarage.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.golem.PartSlot;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The modular golem model. It holds the geometry of every part shape (see GolemGeometry);
 * before each render pass the renderer shows only the shapes for one slot, so every slot can
 * be drawn with its own material texture.
 */
public class GolemModel extends EntityModel<GolemEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("golemgarage", "golem"), "main");

    private static final Map<PartSlot, String> FRAMES = Map.of(
            PartSlot.HEAD, "frame_head", PartSlot.TORSO, "frame_torso",
            PartSlot.ARMS, "frame_arms", PartSlot.LEGS, "frame_legs");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final Map<String, List<ModelPart>> shapes = new HashMap<>();
    private final List<ModelPart> wheels = new ArrayList<>();
    private final List<ModelPart[]> spiderLegs = new ArrayList<>();

    /** The shape shown in each slot for the entity being rendered (null = nothing, e.g. no core). */
    private final Map<PartSlot, String> shown = new EnumMap<>(PartSlot.class);
    private float attackTicks;
    private float workTicks;
    private float slump;
    private int seed;

    public GolemModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        for (Map.Entry<String, List<String[]>> e : GolemGeometry.SHAPE_PARTS.entrySet()) {
            List<ModelPart> list = new ArrayList<>();
            for (String[] path : e.getValue()) {
                list.add(root.getChild(path[0]).getChild(path[1]));
            }
            shapes.put(e.getKey(), list);
        }
        for (ModelPart leg : shapes.getOrDefault("wheel_legs", List.of())) {
            wheels.add(leg.getChild("wheel"));
        }
        for (ModelPart leg : shapes.getOrDefault("spider_legs", List.of())) {
            ModelPart front = leg.getChild("front_leg");
            ModelPart back = leg.getChild("back_leg");
            spiderLegs.add(new ModelPart[]{front, back});
        }
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition createBodyLayer() {
        return GolemGeometry.create();
    }

    /** Decides which shape each slot shows for this golem. */
    public void choose(GolemEntity golem) {
        shown.clear();
        for (PartSlot slot : PartSlot.values()) {
            PartInstance p = golem.getPart(slot);
            if (p != null) {
                shown.put(slot, p.shape().id());
            } else if (FRAMES.containsKey(slot)) {
                shown.put(slot, FRAMES.get(slot));
            }
        }
    }

    /** Shows only the given slot's shape (null shows every chosen shape). */
    public void showOnly(PartSlot only) {
        for (List<ModelPart> parts : shapes.values()) {
            for (ModelPart p : parts) {
                p.visible = false;
            }
        }
        for (Map.Entry<PartSlot, String> e : shown.entrySet()) {
            if (only == null || e.getKey() == only) {
                for (ModelPart p : shapes.getOrDefault(e.getValue(), List.of())) {
                    p.visible = true;
                }
            }
        }
    }

    public boolean shows(PartSlot slot) {
        return shown.containsKey(slot);
    }

    @Override
    public void prepareMobModel(GolemEntity golem, float limbSwing, float limbSwingAmount, float partialTick) {
        int t = golem.getAttackAnimationTick();
        attackTicks = t > 0 ? t - partialTick : 0;
        int w = golem.getWorkAnimationTick();
        workTicks = w > 0 ? w - partialTick : 0;
        slump = golem.breakdownProgress(partialTick);
        seed = golem.getId();
    }

    @Override
    public void setupAnim(GolemEntity golem, float limbSwing, float amount, float age, float yaw, float pitch) {
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        head.zRot = 0;
        body.xRot = 0;
        String legs = shown.get(PartSlot.LEGS);
        String arms = shown.get(PartSlot.ARMS);

        // Arms: iron-golem style swing, or a two-handed smash when attacking.
        if (attackTicks > 0) {
            float swing = -2.0f + 1.5f * Mth.triangleWave(attackTicks, 10f);
            rightArm.xRot = swing;
            leftArm.xRot = swing;
        } else if (workTicks > 0) {
            // Working: both arms reach forward and pull back in.
            float p = Mth.sin((1f - workTicks / 12f) * Mth.PI);
            rightArm.xRot = -1.1f * p;
            leftArm.xRot = -0.8f * p;
        } else {
            float wave = Mth.triangleWave(limbSwing, 13f);
            rightArm.xRot = (-0.2f + 1.5f * wave) * amount;
            leftArm.xRot = (-0.2f - 1.5f * wave) * amount;
        }
        // Idle fidgets: now and then it glances around and tilts its head.
        if (amount < 0.05f && attackTicks <= 0 && workTicks <= 0) {
            float phase = age * 0.012f + (seed % 97) * 0.37f;
            float window = Mth.clamp((Mth.sin(phase) - 0.55f) * 3f, 0f, 1f);
            head.yRot += Mth.sin(age * 0.05f + seed) * 0.6f * window;
            head.zRot = Mth.sin(age * 0.03f + seed) * 0.08f * window;
        }
        float sway = Mth.sin(age * 0.06f) * 0.03f;
        rightArm.zRot = sway;
        leftArm.zRot = -sway;
        rightArm.yRot = 0;
        leftArm.yRot = 0;
        if ("scythe_arms".equals(arms) && attackTicks <= 0) {
            // Blades held slightly out to the sides.
            rightArm.zRot += 0.12f;
            leftArm.zRot -= 0.12f;
        }

        // Legs.
        body.y = -7f;
        head.y = -7f;
        boolean swingLegs = legs == null || legs.equals("stub_legs") || legs.equals("strider_legs")
                || legs.startsWith("frame");
        if (swingLegs) {
            rightLeg.xRot = -1.5f * Mth.triangleWave(limbSwing, 13f) * amount;
            leftLeg.xRot = 1.5f * Mth.triangleWave(limbSwing, 13f) * amount;
        } else {
            rightLeg.xRot = 0;
            leftLeg.xRot = 0;
        }
        if ("tread_legs".equals(legs)) {
            // A gentle rumble while moving.
            float rumble = Mth.sin(age * 1.7f) * 0.15f * amount;
            body.y += rumble;
            head.y += rumble;
        }
        for (ModelPart wheel : wheels) {
            wheel.xRot = limbSwing * 0.9f;
        }
        for (int side = 0; side < spiderLegs.size(); side++) {
            ModelPart[] pair = spiderLegs.get(side);
            float dir = side == 0 ? 1 : -1;
            float s = Mth.sin(limbSwing * 0.9f + side * Mth.PI) * 0.45f * amount;
            float lift = Math.abs(Mth.cos(limbSwing * 0.9f + side * Mth.PI)) * 0.3f * amount;
            pair[0].yRot = dir * (-0.55f + s);
            pair[1].yRot = dir * (0.55f - s);
            pair[0].zRot = dir * (0.3f + lift);
            pair[1].zRot = dir * (0.3f + (0.3f * amount - lift));
        }
        rightArm.y = -8.5f;
        leftArm.y = -8.5f;
        applySlump(age);
    }

    /** Breaking down: the body slumps forward, head and arms droop. Called last in setupAnim. */
    private void applySlump(float age) {
        if (slump <= 0) {
            return;
        }
        float shake = Mth.sin(age * 2.7f) * 0.04f * (1f - slump);
        body.xRot = 0.45f * slump + shake;
        head.xRot = 0.8f * slump;
        head.zRot = 0.25f * slump;
        body.y += 2.5f * slump;
        head.y += 4f * slump;
        rightArm.y = -8.5f + 2.5f * slump;
        leftArm.y = -8.5f + 2.5f * slump;
        rightArm.xRot = 0.35f * slump;
        leftArm.xRot = 0.25f * slump;
        rightArm.zRot = 0.3f * slump;
        leftArm.zRot = -0.3f * slump;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(poseStack, buffer, light, overlay, color);
    }
}
