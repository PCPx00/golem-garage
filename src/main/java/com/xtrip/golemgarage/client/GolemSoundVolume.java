package com.xtrip.golemgarage.client;

import com.xtrip.golemgarage.Config;
import com.xtrip.golemgarage.GolemGarage;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jetbrains.annotations.Nullable;

/** Applies the client's golemVolume setting to every Golem Garage golem and cub sound. */
@EventBusSubscriber(modid = GolemGarage.MOD_ID, value = Dist.CLIENT)
public final class GolemSoundVolume {
    private GolemSoundVolume() {
    }

    @SubscribeEvent
    public static void onPlay(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s == null || !GolemGarage.MOD_ID.equals(s.getLocation().getNamespace())) {
            return;
        }
        String path = s.getLocation().getPath();
        if (!path.startsWith("golem.") && !path.startsWith("cub.")) {
            return;
        }
        int pct = Config.get(Config.GOLEM_VOLUME::get, 100);
        if (pct >= 100) {
            return;
        }
        if (pct <= 0) {
            event.setSound(null);
            return;
        }
        event.setSound(new Scaled(s, pct / 100f));
    }

    /** The same sound at a lower volume. */
    private record Scaled(SoundInstance inner, float scale) implements SoundInstance {
        @Override
        public ResourceLocation getLocation() {
            return inner.getLocation();
        }

        @Override
        @Nullable
        public WeighedSoundEvents resolve(SoundManager manager) {
            return inner.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return inner.getSound();
        }

        @Override
        public SoundSource getSource() {
            return inner.getSource();
        }

        @Override
        public boolean isLooping() {
            return inner.isLooping();
        }

        @Override
        public boolean isRelative() {
            return inner.isRelative();
        }

        @Override
        public int getDelay() {
            return inner.getDelay();
        }

        @Override
        public float getVolume() {
            return inner.getVolume() * scale;
        }

        @Override
        public float getPitch() {
            return inner.getPitch();
        }

        @Override
        public double getX() {
            return inner.getX();
        }

        @Override
        public double getY() {
            return inner.getY();
        }

        @Override
        public double getZ() {
            return inner.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return inner.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return inner.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return inner.canPlaySound();
        }
    }
}
