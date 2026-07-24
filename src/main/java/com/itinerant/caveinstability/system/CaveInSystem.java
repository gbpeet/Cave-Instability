package com.itinerant.caveinstability.system;

import com.itinerant.caveinstability.config.CaveInstabilityConfig;
import com.itinerant.caveinstability.config.CaveInstabilityConfigManager;
import com.itinerant.caveinstability.rules.CollapseRuleResolver;
import com.itinerant.caveinstability.sound.CaveInstabilitySounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class CaveInSystem {
    private static final Set<BlockPos> INTERNAL_FALLS = new HashSet<>();
    private static final List<ActiveCaveInZone> ACTIVE_ZONES = new ArrayList<>();

    private CaveInSystem() {
    }

    public static boolean consumeInternalFall(BlockPos pos) {
        return INTERNAL_FALLS.remove(pos.toImmutable());
    }

    public static void markInternalFall(BlockPos pos) {
        INTERNAL_FALLS.add(pos.toImmutable());
    }

    public static void handleSupportRemoval(ServerWorld world, BlockPos origin, BlockState removedState) {
        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();

        pruneZones(world);

        if (config.enableFloatingGroupCollapse) {
            boolean triggeredFloatingGroup = tryTriggerFloatingGroupCollapse(
                    world,
                    origin,
                    config.delayTicks,
                    Math.max(16, config.floatingGroupSearchLimit)
            );

            if (triggeredFloatingGroup) {
                return;
            }
        }

        if (!CollapseRuleResolver.canCollapse(removedState)) {
            return;
        }

        if (config.enableSupportPillars && hasNearbySupportPillar(
                world,
                origin,
                config.supportCheckRadius,
                config.supportVerticalTolerance,
                config.supportAboveTolerance
        )) {
            return;
        }

        double sourceChance = CollapseRuleResolver.getSourceChance(removedState);
        if (world.random.nextDouble() > sourceChance) {
            return;
        }

        ActiveCaveInZone zone = new ActiveCaveInZone(
                world.getRegistryKey(),
                origin.toImmutable(),
                Math.max(0, config.horizontalRange),
                Math.max(0, config.verticalRange),
                world.getTime() + Math.max(20L, config.delayTicks + 10L)
        );
        ACTIVE_ZONES.add(zone);

        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> scheduled = new HashSet<>();

        for (BlockPos start : getInitialNeighbors(origin)) {
            BlockPos immutable = start.toImmutable();
            if (!zone.contains(immutable)) {
                continue;
            }
            if (visited.add(immutable)) {
                frontier.add(immutable);
            }
        }

        while (!frontier.isEmpty()) {
            BlockPos current = frontier.poll();

            if (!zone.contains(current)) {
                continue;
            }

            BlockState state = world.getBlockState(current);
            if (!CollapseRuleResolver.canCollapse(state)) {
                continue;
            }

            if (!isFallThrough(world.getBlockState(current.down()))) {
                continue;
            }

            double effectiveChance = getDistanceDecayChance(zone, current);
            if (world.random.nextDouble() > effectiveChance) {
                continue;
            }

            if (scheduled.add(current.toImmutable())) {
                world.scheduleBlockTick(current.toImmutable(), state.getBlock(), config.delayTicks);
            }

            for (BlockPos next : getPropagationNeighbors(current)) {
                BlockPos immutable = next.toImmutable();
                if (!zone.contains(immutable)) {
                    continue;
                }
                if (visited.add(immutable)) {
                    frontier.add(immutable);
                }
            }
        }

        int audibleScheduledCount = 0;
        for (BlockPos pos : scheduled) {
            if (!isSilentMaterial(world.getBlockState(pos))) {
                audibleScheduledCount++;
            }
        }

        CaveInstabilitySounds.playCaveInStartSound(world, origin, audibleScheduledCount);
    }

    public static boolean isInsideActiveZone(ServerWorld world, BlockPos pos) {
        pruneZones(world);

        for (ActiveCaveInZone zone : ACTIVE_ZONES) {
            if (zone.worldKey.equals(world.getRegistryKey()) && zone.contains(pos)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFullyUnsupported(ServerWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (!neighborState.isAir()) {
                return false;
            }
        }

        return true;
    }

    public static boolean shouldTriggerFromReplacement(BlockState oldState, BlockState newState) {
        if (oldState == null || newState == null) {
            return false;
        }

        if (isFallThrough(oldState)) {
            return false;
        }

        if (newState.isLiquid()) {
            return false;
        }

        return isTriggerOpenSpace(newState);
    }

    public static void tryStartFalling(BlockState state, ServerWorld world, BlockPos pos) {
        if (!CollapseRuleResolver.canCollapse(state)) {
            return;
        }

        boolean insideZone = isInsideActiveZone(world, pos);
        boolean fullyUnsupported = isFullyUnsupported(world, pos);

        if (!insideZone && !fullyUnsupported) {
            return;
        }

        if (pos.getY() <= world.getBottomY()) {
            return;
        }

        if (!isFallThrough(world.getBlockState(pos.down()))) {
            return;
        }

        if (state.contains(Properties.WATERLOGGED) && Boolean.TRUE.equals(state.get(Properties.WATERLOGGED))) {
            return;
        }

        markInternalFall(pos);
        FallingBlockEntity entity = FallingBlockEntity.spawnFromBlock(world, pos, state);
        ((SlideTrackedFallingBlock) entity).caveinstability$setSlideCount(0);
        ((SlideTrackedFallingBlock) entity).caveinstability$setStartY(pos.getY());
    }

    public static void playLandingSoundForImpact(ServerWorld world, BlockPos pos, int fallenBlocks, BlockState landedState) {
        if (fallenBlocks <= 0) {
            return;
        }

        if (isSilentMaterial(landedState)) {
            return;
        }

        CaveInstabilitySounds.playLandingSound(world, pos, fallenBlocks);
        spawnLandingDust(world, pos, landedState, fallenBlocks);
    }

    public static boolean trySlideDebris(ServerWorld world, BlockPos landedPos, BlockState landedState, int currentSlideCount) {
        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();

        if (!config.enableDebrisSliding) {
            return false;
        }

        if (currentSlideCount >= config.maxDebrisSlides) {
            return false;
        }

        if (!CollapseRuleResolver.canCollapse(landedState)) {
            return false;
        }

        if (isSilentMaterial(landedState)) {
            return false;
        }

        BlockState supportState = world.getBlockState(landedPos.down());
        if (isFallThrough(supportState)) {
            return false;
        }

        List<Direction> validDirections = new ArrayList<>(4);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = landedPos.offset(direction);
            BlockPos sideBelowPos = sidePos.down();

            BlockState sideState = world.getBlockState(sidePos);
            BlockState sideBelowState = world.getBlockState(sideBelowPos);

            if (!isSlideSideOpen(sideState)) {
                continue;
            }

            if (!isFallThrough(sideBelowState)) {
                continue;
            }

            validDirections.add(direction);
        }

        if (validDirections.isEmpty()) {
            return false;
        }

        Direction chosenDirection = validDirections.get(world.random.nextInt(validDirections.size()));
        BlockPos targetPos = landedPos.offset(chosenDirection);

        markInternalFall(landedPos);
        world.setBlockState(landedPos, Blocks.AIR.getDefaultState(), 3);

        FallingBlockEntity entity = FallingBlockEntity.spawnFromBlock(world, targetPos, landedState);
        ((SlideTrackedFallingBlock) entity).caveinstability$setSlideCount(currentSlideCount + 1);
        ((SlideTrackedFallingBlock) entity).caveinstability$setStartY(targetPos.getY());

        return true;
    }

    public static boolean isFallThrough(BlockState state) {
        if (state == null) {
            return false;
        }

        return state.isAir()
                || state.isIn(BlockTags.FIRE)
                || state.isReplaceable()
                || state.isLiquid();
    }

    public static boolean isTriggerOpenSpace(BlockState state) {
        if (state == null) {
            return false;
        }

        return state.isAir()
                || state.isIn(BlockTags.FIRE)
                || state.isReplaceable();
    }

    private static boolean isSlideSideOpen(BlockState state) {
        if (state == null) {
            return false;
        }

        return state.isAir() || state.isLiquid();
    }

    private static boolean isSilentMaterial(BlockState state) {
        return state.isIn(BlockTags.LEAVES);
    }

    private static boolean tryTriggerFloatingGroupCollapse(ServerWorld world, BlockPos origin, int delayTicks, int searchLimit) {
        Set<BlockPos> scanned = new HashSet<>();
        Set<BlockPos> scheduled = new HashSet<>();

        for (BlockPos start : getInitialNeighbors(origin)) {
            BlockPos immutableStart = start.toImmutable();

            if (scanned.contains(immutableStart)) {
                continue;
            }

            BlockState startState = world.getBlockState(immutableStart);
            if (!CollapseRuleResolver.canCollapse(startState)) {
                continue;
            }

            Set<BlockPos> cluster = findFloatingCluster(world, immutableStart, searchLimit, scanned);
            if (cluster == null || cluster.size() < 2) {
                continue;
            }

            ActiveCaveInZone floatingZone = createZoneForCluster(
                    world,
                    cluster,
                    world.getTime() + Math.max(20L, delayTicks + 10L)
            );
            ACTIVE_ZONES.add(floatingZone);

            for (BlockPos pos : cluster) {
                BlockState state = world.getBlockState(pos);
                if (!CollapseRuleResolver.canCollapse(state)) {
                    continue;
                }

                BlockPos immutable = pos.toImmutable();
                if (scheduled.add(immutable)) {
                    world.scheduleBlockTick(immutable, state.getBlock(), delayTicks);
                }
            }
        }

        if (scheduled.isEmpty()) {
            return false;
        }

        int audibleScheduledCount = 0;
        for (BlockPos pos : scheduled) {
            if (!isSilentMaterial(world.getBlockState(pos))) {
                audibleScheduledCount++;
            }
        }

        CaveInstabilitySounds.playCaveInStartSound(world, origin, audibleScheduledCount);
        return true;
    }

    private static Set<BlockPos> findFloatingCluster(
            ServerWorld world,
            BlockPos start,
            int searchLimit,
            Set<BlockPos> scanned
    ) {
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> cluster = new HashSet<>();

        BlockPos immutableStart = start.toImmutable();
        frontier.add(immutableStart);
        cluster.add(immutableStart);
        scanned.add(immutableStart);

        while (!frontier.isEmpty()) {
            BlockPos current = frontier.poll();

            if (cluster.size() > searchLimit) {
                return null;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.offset(direction).toImmutable();

                if (cluster.contains(next)) {
                    continue;
                }

                BlockState nextState = world.getBlockState(next);
                if (!CollapseRuleResolver.canCollapse(nextState)) {
                    continue;
                }

                cluster.add(next);
                frontier.add(next);
                scanned.add(next);
            }
        }

        if (cluster.size() > searchLimit) {
            return null;
        }

        for (BlockPos pos : cluster) {
            BlockPos below = pos.down();

            if (cluster.contains(below)) {
                continue;
            }

            if (!isFallThrough(world.getBlockState(below))) {
                return null;
            }
        }

        return cluster;
    }

    private static ActiveCaveInZone createZoneForCluster(ServerWorld world, Set<BlockPos> cluster, long expiresAt) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : cluster) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        BlockPos center = new BlockPos(
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
        );

        int horizontalRange = Math.max(
                Math.max(Math.abs(maxX - center.getX()), Math.abs(minX - center.getX())),
                Math.max(Math.abs(maxZ - center.getZ()), Math.abs(minZ - center.getZ()))
        ) + 1;

        int verticalRange = Math.max(
                Math.abs(maxY - center.getY()),
                Math.abs(minY - center.getY())
        ) + 1;

        return new ActiveCaveInZone(
                world.getRegistryKey(),
                center,
                horizontalRange,
                verticalRange,
                expiresAt
        );
    }

    private static boolean hasNearbySupportPillar(
            ServerWorld world,
            BlockPos origin,
            int horizontalRadius,
            int belowTolerance,
            int aboveTolerance
    ) {
        int minY = origin.getY() - Math.max(0, belowTolerance);
        int maxY = origin.getY() + Math.max(0, aboveTolerance);

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                for (int y = maxY; y >= minY; y--) {
                    BlockPos checkPos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);

                    if (!isSupportPillarBlock(world.getBlockState(checkPos))) {
                        continue;
                    }

                    if (isValidSupportPillar(world, checkPos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isValidSupportPillar(ServerWorld world, BlockPos pillarPos) {
        BlockPos top = pillarPos.toImmutable();
        while (isSupportPillarBlock(world.getBlockState(top.up()))) {
            top = top.up().toImmutable();
        }

        BlockPos bottom = pillarPos.toImmutable();
        while (isSupportPillarBlock(world.getBlockState(bottom.down()))) {
            bottom = bottom.down().toImmutable();
        }

        int validAbove = 0;
        BlockPos cursorAbove = top.up();
        for (int i = 0; i < 2; i++) {
            BlockState stateAbove = world.getBlockState(cursorAbove);
            if (!CollapseRuleResolver.canCollapse(stateAbove)) {
                break;
            }
            validAbove++;
            cursorAbove = cursorAbove.up();
        }

        if (validAbove < 2) {
            return false;
        }

        int validBelow = 0;
        BlockPos cursorBelow = bottom.down();
        for (int i = 0; i < 2; i++) {
            BlockState stateBelow = world.getBlockState(cursorBelow);
            if (isFallThrough(stateBelow)) {
                break;
            }
            validBelow++;
            cursorBelow = cursorBelow.down();
        }

        return validBelow >= 2;
    }

    private static boolean isSupportPillarBlock(BlockState state) {
        if (state == null) {
            return false;
        }

        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();

        for (String rawEntry : config.supportEntries) {
            String entry = rawEntry == null ? "" : rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }

            if (entry.startsWith("#")) {
                Identifier id = Identifier.tryParse(entry.substring(1));
                if (id == null) {
                    continue;
                }

                TagKey<net.minecraft.block.Block> tagKey = TagKey.of(RegistryKeys.BLOCK, id);
                if (state.isIn(tagKey)) {
                    return true;
                }
            } else {
                Identifier id = Identifier.tryParse(entry);
                if (id == null) {
                    continue;
                }

                Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                if (blockId.equals(id)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void spawnLandingDust(ServerWorld world, BlockPos pos, BlockState landedState, int fallenBlocks) {
        BlockStateParticleEffect dust = new BlockStateParticleEffect(ParticleTypes.BLOCK, landedState);

        int clampedFall = Math.max(1, Math.min(20, fallenBlocks));

        int sideCount = lerpInt(6, 25, clampedFall, 1, 20);
        int topCount = lerpInt(10, 40, clampedFall, 1, 20);

        double sideSpeed = lerpDouble(0.010D, 0.025D, clampedFall, 1, 20);
        double topSpeed = lerpDouble(0.008D, 0.020D, clampedFall, 1, 20);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = pos.offset(direction);
            BlockState sideState = world.getBlockState(sidePos);

            if (!sideState.isAir()) {
                continue;
            }

            double x = pos.getX() + 0.5D + direction.getOffsetX() * 0.55D;
            double y = pos.getY() + 0.35D;
            double z = pos.getZ() + 0.5D + direction.getOffsetZ() * 0.55D;

            double spreadX = direction.getAxis() == Direction.Axis.X ? 0.03D : 0.20D;
            double spreadY = 0.14D;
            double spreadZ = direction.getAxis() == Direction.Axis.Z ? 0.03D : 0.20D;

            world.spawnParticles(dust, x, y, z, sideCount, spreadX, spreadY, spreadZ, sideSpeed);
        }

        BlockPos abovePos = pos.up();
        if (world.getBlockState(abovePos).isAir()) {
            world.spawnParticles(
                    dust,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.02D,
                    pos.getZ() + 0.5D,
                    topCount,
                    0.24D,
                    0.08D,
                    0.24D,
                    topSpeed
            );
        }
    }

    private static int lerpInt(int start, int end, int value, int minValue, int maxValue) {
        if (maxValue <= minValue) {
            return start;
        }

        double t = (value - minValue) / (double) (maxValue - minValue);
        t = clamp01(t);
        return (int) Math.round(start + (end - start) * t);
    }

    private static double lerpDouble(double start, double end, int value, int minValue, int maxValue) {
        if (maxValue <= minValue) {
            return start;
        }

        double t = (value - minValue) / (double) (maxValue - minValue);
        t = clamp01(t);
        return start + (end - start) * t;
    }

    private static double getDistanceDecayChance(ActiveCaveInZone zone, BlockPos pos) {
        double horizontalProgress;
        if (zone.horizontalRange <= 0) {
            horizontalProgress = 0.0D;
        } else {
            int dx = Math.abs(pos.getX() - zone.origin.getX());
            int dz = Math.abs(pos.getZ() - zone.origin.getZ());
            horizontalProgress = Math.max(dx, dz) / (double) zone.horizontalRange;
        }

        double verticalProgress;
        if (zone.verticalRange <= 0) {
            verticalProgress = 0.0D;
        } else {
            int dy = Math.abs(pos.getY() - zone.origin.getY());
            verticalProgress = dy / (double) zone.verticalRange;
        }

        horizontalProgress = clamp01(horizontalProgress);
        verticalProgress = clamp01(verticalProgress);

        double decay = (horizontalProgress * 0.65D) + (verticalProgress * 0.35D);
        return clamp01(1.0D - decay);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static List<BlockPos> getInitialNeighbors(BlockPos origin) {
        List<BlockPos> neighbors = new ArrayList<>(22);

        neighbors.add(origin.up());
        neighbors.add(origin.down());
        neighbors.add(origin.north());
        neighbors.add(origin.south());
        neighbors.add(origin.east());
        neighbors.add(origin.west());

        addRing(neighbors, origin, 1);
        addRing(neighbors, origin, -1);

        return neighbors;
    }

    private static void addRing(List<BlockPos> neighbors, BlockPos origin, int yOffset) {
        int targetY = origin.getY() + yOffset;

        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                if (xOffset == 0 && zOffset == 0) {
                    continue;
                }

                neighbors.add(new BlockPos(
                        origin.getX() + xOffset,
                        targetY,
                        origin.getZ() + zOffset
                ));
            }
        }
    }

    private static List<BlockPos> getPropagationNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(5);
        neighbors.add(pos.up());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());
        return neighbors;
    }

    private static void pruneZones(ServerWorld world) {
        long currentTime = world.getTime();
        RegistryKey<World> worldKey = world.getRegistryKey();

        Iterator<ActiveCaveInZone> it = ACTIVE_ZONES.iterator();
        while (it.hasNext()) {
            ActiveCaveInZone zone = it.next();
            if (!zone.worldKey.equals(worldKey) || zone.expiresAt < currentTime) {
                it.remove();
            }
        }
    }

    private static final class ActiveCaveInZone {
        private final RegistryKey<World> worldKey;
        private final BlockPos origin;
        private final int horizontalRange;
        private final int verticalRange;
        private final long expiresAt;

        private ActiveCaveInZone(
                RegistryKey<World> worldKey,
                BlockPos origin,
                int horizontalRange,
                int verticalRange,
                long expiresAt
        ) {
            this.worldKey = worldKey;
            this.origin = origin;
            this.horizontalRange = horizontalRange;
            this.verticalRange = verticalRange;
            this.expiresAt = expiresAt;
        }

        private boolean contains(BlockPos pos) {
            return Math.abs(pos.getX() - origin.getX()) <= horizontalRange
                    && Math.abs(pos.getY() - origin.getY()) <= verticalRange
                    && Math.abs(pos.getZ() - origin.getZ()) <= horizontalRange;
        }
    }
}