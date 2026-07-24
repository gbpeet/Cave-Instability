package com.itinerant.caveinstability.config;

import java.util.ArrayList;
import java.util.List;

public class CaveInstabilityConfig {
    public int delayTicks = 4;
    public int horizontalRange = 8;
    public int verticalRange = 3;

    // Global fallback chance that a cave in starts when support is removed.
    public double sourceCaveInChance = 0.15D;

    // Toggle for downhill debris behavior.
    public boolean enableDebrisSliding = true;

    // How many sideways slides a falling debris block may attempt before stopping.
    public int maxDebrisSlides = 8;

    // When enabled, connected collapsible blocks left hanging in air can all collapse together.
    public boolean enableFloatingGroupCollapse = true;

    // Maximum number of connected collapsible blocks checked before assuming the group is too large.
    public int floatingGroupSearchLimit = 500;

    // When enabled, nearby support pillars can prevent a cave in from starting.
    public boolean enableSupportPillars = true;

    // Horizontal radius around the trigger source used to search for valid support pillars.
    public int supportCheckRadius = 10;

    // How many blocks below the trigger source a support pillar may still count.
    public int supportVerticalTolerance = 2;

    // How many blocks above the trigger source a support pillar may still count.
    public int supportAboveTolerance = 2;

    // Support materials that can count as part of a pillar.
    // Format:
    //   minecraft:oak_log
    //   #minecraft:logs
    public List<String> supportEntries = new ArrayList<>(List.of(
            "#minecraft:planks",
            "#minecraft:wooden_stairs",
            "#minecraft:wooden_slabs",
            "#minecraft:wooden_fences",
            "#minecraft:fence_gates"
    ));

    // Format:
    //   minecraft:stone
    //   minecraft:stone=0.35
    //   #minecraft:base_stone_overworld
    //   #minecraft:dirt=0.50
    public List<String> fallingEntries = new ArrayList<>(List.of(
            "#minecraft:base_stone_overworld",
            "#minecraft:base_stone_nether=0.25",
            "#minecraft:dirt=0.50",
            "#minecraft:logs=1.00",
            "#minecraft:leaves=1.00",
            "#minecraft:coal_ores",
            "#minecraft:iron_ores",
            "#minecraft:copper_ores",
            "#minecraft:gold_ores",
            "#minecraft:diamond_ores",
            "#minecraft:lapis_ores",
            "#minecraft:redstone_ores",
            "#minecraft:emerald_ores",
            "#minecraft:mud=0.50",
            "#minecraft:terracotta=0.50",
            "#minecraft:sand=1.00",
            "minecraft:end_stone",
            "minecraft:obsidian",
            "minecraft:crying_obsidian",
            "minecraft:sandstone",
            "minecraft:red_sandstone",
            "minecraft:clay=0.50",
            "minecraft:gravel=1.00"
    ));
}