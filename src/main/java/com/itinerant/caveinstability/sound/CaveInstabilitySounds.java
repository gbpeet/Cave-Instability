package com.itinerant.caveinstability.sound;

import com.itinerant.caveinstability.CaveInstabilityMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public final class CaveInstabilitySounds {
    public static final SoundEvent HEAVY_RUMBLE = create("heavy_rumble");
    public static final SoundEvent LIGHT_CRUMBLE = create("light_crumble");
    public static final SoundEvent MEDIUM_CRUMBLE = create("medium_crumble");
    public static final SoundEvent QUICK_LIGHT_COLLAPSE = create("quick_light_collapse");

    private CaveInstabilitySounds() {
    }

    public static void register() {
        register("heavy_rumble", HEAVY_RUMBLE);
        register("light_crumble", LIGHT_CRUMBLE);
        register("medium_crumble", MEDIUM_CRUMBLE);
        register("quick_light_collapse", QUICK_LIGHT_COLLAPSE);
    }

    public static void playCaveInStartSound(ServerWorld world, BlockPos origin, int scheduledBlockCount) {
        if (scheduledBlockCount < 3) {
            return;
        }

        SoundEvent soundEvent;
        float volume;
        float pitch;

        if (scheduledBlockCount >= 9) {
            soundEvent = HEAVY_RUMBLE;
            volume = 2.0F;
            pitch = 0.90F;
        } else {
            soundEvent = MEDIUM_CRUMBLE;
            volume = 1.2F;
            pitch = 1.00F;
        }

        world.playSound(
                null,
                origin.getX() + 0.5D,
                origin.getY() + 0.5D,
                origin.getZ() + 0.5D,
                soundEvent,
                SoundCategory.BLOCKS,
                volume,
                pitch
        );
    }

    public static void playLandingSound(ServerWorld world, BlockPos pos, int fallenBlocks) {
        if (fallenBlocks <= 0) {
            return;
        }

        world.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                QUICK_LIGHT_COLLAPSE,
                SoundCategory.BLOCKS,
                0.75F,
                1.05F
        );
    }

    private static SoundEvent create(String path) {
        return SoundEvent.of(CaveInstabilityMod.id(path));
    }

    private static void register(String path, SoundEvent soundEvent) {
        Registry.register(Registries.SOUND_EVENT, CaveInstabilityMod.id(path), soundEvent);
    }
}