package com.xtrip.golemgarage.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Additive glow (lasers, lantern halos, headlights) that doesn't write depth, so windows and glass behind a
 * glow still draw instead of showing the sky through it.
 */
final class GlowRenderType extends RenderType {
    static final RenderType GLOW = create("golemgarage_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private GlowRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                           boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
