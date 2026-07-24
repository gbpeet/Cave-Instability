package com.itinerant.caveinstability.mixin;

import com.itinerant.caveinstability.config.CaveInstabilityConfigManager;
import com.itinerant.caveinstability.rules.CollapseRuleResolver;
import com.itinerant.caveinstability.system.CaveInSystem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {
    @Unique
    private static final Map<String, Long> caveinstability$RECENT_TRIGGERS = new HashMap<>();

    @Inject(method = "onStateReplaced", at = @At("TAIL"))
    private void caveinstability$onStateReplaced(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (state.isOf(newState.getBlock())) {
            return;
        }

        if (!CaveInSystem.shouldTriggerFromReplacement(state, newState)) {
            return;
        }

        if (CaveInSystem.consumeInternalFall(pos)) {
            return;
        }

        if (CaveInSystem.isInsideActiveZone(serverWorld, pos)) {
            return;
        }

        if (caveinstability$isDuplicateRecentTrigger(serverWorld, pos)) {
            return;
        }

        caveinstability$recordRecentTrigger(serverWorld, pos);
        CaveInSystem.handleSupportRemoval(serverWorld, pos, state);
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("TAIL"))
    private void caveinstability$getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!CollapseRuleResolver.canCollapse(state)) {
            return;
        }

        boolean shouldSchedule = false;

        if (direction == Direction.DOWN
                && CaveInSystem.isInsideActiveZone(serverWorld, pos)
                && CaveInSystem.isFallThrough(neighborState)) {
            shouldSchedule = true;
        }

        if (!shouldSchedule && CaveInSystem.isFullyUnsupported(serverWorld, pos)) {
            shouldSchedule = true;
        }

        if (!shouldSchedule) {
            return;
        }

        serverWorld.scheduleBlockTick(
                pos,
                state.getBlock(),
                CaveInstabilityConfigManager.getConfig().delayTicks
        );
    }

    @Inject(method = "scheduledTick", at = @At("TAIL"))
    private void caveinstability$scheduledTick(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            Random random,
            CallbackInfo ci
    ) {
        if (!CollapseRuleResolver.canCollapse(state)) {
            return;
        }

        CaveInSystem.tryStartFalling(state, world, pos);
    }

    @Unique
    private static boolean caveinstability$isDuplicateRecentTrigger(ServerWorld world, BlockPos pos) {
        caveinstability$pruneRecentTriggers(world);

        long now = world.getTime();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String key = caveinstability$makeTriggerKey(
                            world,
                            pos.add(dx, dy, dz)
                    );
                    Long expiresAt = caveinstability$RECENT_TRIGGERS.get(key);
                    if (expiresAt != null && expiresAt >= now) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Unique
    private static void caveinstability$recordRecentTrigger(ServerWorld world, BlockPos pos) {
        caveinstability$pruneRecentTriggers(world);
        caveinstability$RECENT_TRIGGERS.put(
                caveinstability$makeTriggerKey(world, pos),
                world.getTime() + 2L
        );
    }

    @Unique
    private static void caveinstability$pruneRecentTriggers(ServerWorld world) {
        long now = world.getTime();
        String worldPrefix = world.getRegistryKey().getValue().toString() + "|";

        Iterator<Map.Entry<String, Long>> it = caveinstability$RECENT_TRIGGERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getKey().startsWith(worldPrefix) && entry.getValue() < now) {
                it.remove();
            }
        }
    }

    @Unique
    private static String caveinstability$makeTriggerKey(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue().toString()
                + "|"
                + pos.getX()
                + ","
                + pos.getY()
                + ","
                + pos.getZ();
    }
}