package com.itinerant.caveinstability.mixin;

import com.itinerant.caveinstability.rules.CollapseRuleResolver;
import com.itinerant.caveinstability.system.CaveInSystem;
import com.itinerant.caveinstability.system.SlideTrackedFallingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin implements SlideTrackedFallingBlock {
    @Unique
    private boolean caveinstability$trackedStart = false;

    @Unique
    private int caveinstability$startY = 0;

    @Unique
    private int caveinstability$slideCount = 0;

    @Unique
    private boolean caveinstability$landingHandled = false;

    @Override
    public int caveinstability$getSlideCount() {
        return this.caveinstability$slideCount;
    }

    @Override
    public void caveinstability$setSlideCount(int slideCount) {
        this.caveinstability$slideCount = slideCount;
    }

    @Override
    public int caveinstability$getStartY() {
        return this.caveinstability$startY;
    }

    @Override
    public void caveinstability$setStartY(int startY) {
        this.caveinstability$startY = startY;
    }

    @Override
    public boolean caveinstability$hasTrackedStart() {
        return this.caveinstability$trackedStart;
    }

    @Override
    public void caveinstability$setTrackedStart(boolean trackedStart) {
        this.caveinstability$trackedStart = trackedStart;
    }

    @Override
    public boolean caveinstability$hasHandledLanding() {
        return this.caveinstability$landingHandled;
    }

    @Override
    public void caveinstability$setHandledLanding(boolean handledLanding) {
        this.caveinstability$landingHandled = handledLanding;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void caveinstability$trackStart(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;

        if (!this.caveinstability$trackedStart) {
            this.caveinstability$startY = self.getBlockPos().getY();
            this.caveinstability$trackedStart = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void caveinstability$handleLanding(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;

        if (this.caveinstability$landingHandled) {
            return;
        }

        if (!(self.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!self.isOnGround()) {
            return;
        }

        BlockState fallingState = self.getBlockState();
        if (!CollapseRuleResolver.canCollapse(fallingState)) {
            return;
        }

        BlockPos landedPos = self.getBlockPos();
        int fallenBlocks = Math.max(0, this.caveinstability$startY - landedPos.getY());

        if (fallenBlocks <= 0) {
            this.caveinstability$landingHandled = true;
            return;
        }

        // First impact: always play sound + dust as soon as the block lands.
        CaveInSystem.playLandingSoundForImpact(
                serverWorld,
                landedPos,
                fallenBlocks,
                fallingState
        );

        // Then allow avalanche/debris sliding to continue afterward.
        CaveInSystem.trySlideDebris(
                serverWorld,
                landedPos,
                fallingState,
                this.caveinstability$slideCount
        );

        this.caveinstability$landingHandled = true;
    }
}