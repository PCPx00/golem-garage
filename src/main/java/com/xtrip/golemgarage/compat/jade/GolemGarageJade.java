package com.xtrip.golemgarage.compat.jade;

import com.xtrip.golemgarage.GolemGarage;
import com.xtrip.golemgarage.entity.CubEntity;
import com.xtrip.golemgarage.entity.GolemEntity;
import com.xtrip.golemgarage.golem.GolemText;
import com.xtrip.golemgarage.golem.PartInstance;
import com.xtrip.golemgarage.item.ModItems;
import com.xtrip.golemgarage.item.OutfitItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade: look at a golem to see its power, what it's doing and its most worn part; look at a cub for its colour and outfit. */
@WailaPlugin(GolemGarage.MOD_ID)
public class GolemGarageJade implements IWailaPlugin {
    static final ResourceLocation GOLEM = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "golem");
    static final ResourceLocation CUB = ResourceLocation.fromNamespaceAndPath(GolemGarage.MOD_ID, "cub");

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerEntityComponent(GolemInfo.INSTANCE, GolemEntity.class);
        reg.registerEntityComponent(CubInfo.INSTANCE, CubEntity.class);
    }

    enum GolemInfo implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip t, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof GolemEntity golem)) {
                return;
            }
            int cap = golem.readSyncedCapacity();
            if (cap > 0) {
                int pct = Math.round(100f * golem.readSyncedPower() / cap);
                t.add(Component.translatable("jade.golemgarage.power", pct)
                        .withStyle(pct <= 10 ? ChatFormatting.RED : ChatFormatting.AQUA));
            } else {
                t.add(Component.translatable("jade.golemgarage.no_core").withStyle(ChatFormatting.GRAY));
            }
            String status = golem.readSyncedStatus();
            t.add(Component.translatable("jade.golemgarage.doing", GolemText.show(status))
                    .withStyle(GolemText.isProblem(status) ? ChatFormatting.RED : ChatFormatting.WHITE));
            PartInstance worst = null;
            for (PartInstance p : golem.getEquipped().values()) {
                if (worst == null || p.condition() < worst.condition()) {
                    worst = p;
                }
            }
            if (worst != null && worst.condition() < 1f) {
                int pct = Math.round(worst.condition() * 100);
                ChatFormatting c = worst.isBroken() ? ChatFormatting.RED : pct < 50 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
                t.add(Component.translatable("jade.golemgarage.worst_part", worst.toStack().getHoverName(), pct).withStyle(c));
            }
            int mods = golem.getModules().size();
            if (mods > 0) {
                t.add(Component.translatable("jade.golemgarage.modules", mods,
                        com.xtrip.golemgarage.golem.GolemModule.SOCKETS).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return GOLEM;
        }
    }

    enum CubInfo implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip t, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof CubEntity cub)) {
                return;
            }
            DyeColor c = cub.getColor();
            t.add(Component.translatable("jade.golemgarage.cub_color",
                    Component.translatable("color.minecraft." + c.getName())).withStyle(ChatFormatting.GRAY));
            String theme = null;
            int worn = 0;
            boolean matched = true;
            for (String id : cub.outfitIds()) {
                var item = id.isEmpty() ? null : ModItems.OUTFITS.get(id);
                if (item == null) {
                    matched = false;
                    continue;
                }
                worn++;
                OutfitItem o = item.get();
                if (theme == null) {
                    theme = o.theme();
                } else if (!theme.equals(o.theme())) {
                    matched = false;
                }
            }
            if (worn > 0) {
                t.add(Component.translatable("jade.golemgarage.cub_outfit", worn, 3).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (matched && theme != null) {
                t.add(Component.translatable("jade.golemgarage.cub_set",
                        Component.translatable("outfit.golemgarage.theme." + theme)).withStyle(ChatFormatting.GOLD));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return CUB;
        }
    }
}
