package com.itinerant.caveinstability.client;

import com.itinerant.caveinstability.config.CaveInstabilityConfig;
import com.itinerant.caveinstability.config.CaveInstabilityConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class CaveInstabilityModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createScreen;
    }

    private Screen createScreen(Screen parent) {
        CaveInstabilityConfig current = CaveInstabilityConfigManager.getConfig();
        CaveInstabilityConfig working = copyOf(current);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.caveinstability.config"))
                .setSavingRunnable(() -> {
                    CaveInstabilityConfig live = CaveInstabilityConfigManager.getConfig();
                    live.delayTicks = working.delayTicks;
                    live.horizontalRange = working.horizontalRange;
                    live.verticalRange = working.verticalRange;
                    live.sourceCaveInChance = working.sourceCaveInChance;
                    live.enableDebrisSliding = working.enableDebrisSliding;
                    live.maxDebrisSlides = working.maxDebrisSlides;
                    live.enableFloatingGroupCollapse = working.enableFloatingGroupCollapse;
                    live.floatingGroupSearchLimit = working.floatingGroupSearchLimit;
                    live.enableSupportPillars = working.enableSupportPillars;
                    live.supportCheckRadius = working.supportCheckRadius;
                    live.supportVerticalTolerance = working.supportVerticalTolerance;
                    live.supportAboveTolerance = working.supportAboveTolerance;
                    live.supportEntries = new ArrayList<>(working.supportEntries);
                    live.fallingEntries = new ArrayList<>(working.fallingEntries);
                    CaveInstabilityConfigManager.save();
                });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.caveinstability.general"));
        ConfigCategory supports = builder.getOrCreateCategory(Text.translatable("category.caveinstability.supports"));
        ConfigCategory materials = builder.getOrCreateCategory(Text.translatable("category.caveinstability.materials"));

        general.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.delay_ticks"),
                        working.delayTicks
                )
                .setDefaultValue(4)
                .setMin(1)
                .setMax(40)
                .setTooltip(Text.translatable("tooltip.caveinstability.delay_ticks"))
                .setSaveConsumer(value -> working.delayTicks = value)
                .build());

        general.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.horizontal_range"),
                        working.horizontalRange
                )
                .setDefaultValue(8)
                .setMin(0)
                .setMax(32)
                .setTooltip(Text.translatable("tooltip.caveinstability.horizontal_range"))
                .setSaveConsumer(value -> working.horizontalRange = value)
                .build());

        general.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.vertical_range"),
                        working.verticalRange
                )
                .setDefaultValue(3)
                .setMin(0)
                .setMax(32)
                .setTooltip(Text.translatable("tooltip.caveinstability.vertical_range"))
                .setSaveConsumer(value -> working.verticalRange = value)
                .build());

        general.addEntry(entries.startDoubleField(
                        Text.translatable("option.caveinstability.source_cave_in_chance"),
                        working.sourceCaveInChance
                )
                .setDefaultValue(0.15D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Text.translatable("tooltip.caveinstability.source_cave_in_chance"))
                .setSaveConsumer(value -> working.sourceCaveInChance = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("option.caveinstability.enable_debris_sliding"),
                        working.enableDebrisSliding
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.caveinstability.enable_debris_sliding"))
                .setSaveConsumer(value -> working.enableDebrisSliding = value)
                .build());

        general.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.max_debris_slides"),
                        working.maxDebrisSlides
                )
                .setDefaultValue(8)
                .setMin(0)
                .setMax(64)
                .setTooltip(Text.translatable("tooltip.caveinstability.max_debris_slides"))
                .setSaveConsumer(value -> working.maxDebrisSlides = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("option.caveinstability.enable_floating_group_collapse"),
                        working.enableFloatingGroupCollapse
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.caveinstability.enable_floating_group_collapse"))
                .setSaveConsumer(value -> working.enableFloatingGroupCollapse = value)
                .build());

        general.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.floating_group_search_limit"),
                        working.floatingGroupSearchLimit
                )
                .setDefaultValue(500)
                .setMin(16)
                .setMax(4096)
                .setTooltip(Text.translatable("tooltip.caveinstability.floating_group_search_limit"))
                .setSaveConsumer(value -> working.floatingGroupSearchLimit = value)
                .build());

        supports.addEntry(entries.startBooleanToggle(
                        Text.translatable("option.caveinstability.enable_support_pillars"),
                        working.enableSupportPillars
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.caveinstability.enable_support_pillars"))
                .setSaveConsumer(value -> working.enableSupportPillars = value)
                .build());

        supports.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.support_check_radius"),
                        working.supportCheckRadius
                )
                .setDefaultValue(10)
                .setMin(1)
                .setMax(64)
                .setTooltip(Text.translatable("tooltip.caveinstability.support_check_radius"))
                .setSaveConsumer(value -> working.supportCheckRadius = value)
                .build());

        supports.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.support_vertical_tolerance"),
                        working.supportVerticalTolerance
                )
                .setDefaultValue(2)
                .setMin(0)
                .setMax(32)
                .setTooltip(Text.translatable("tooltip.caveinstability.support_vertical_tolerance"))
                .setSaveConsumer(value -> working.supportVerticalTolerance = value)
                .build());

        supports.addEntry(entries.startIntField(
                        Text.translatable("option.caveinstability.support_above_tolerance"),
                        working.supportAboveTolerance
                )
                .setDefaultValue(6)
                .setMin(0)
                .setMax(32)
                .setTooltip(Text.translatable("tooltip.caveinstability.support_above_tolerance"))
                .setSaveConsumer(value -> working.supportAboveTolerance = value)
                .build());

        supports.addEntry(entries.startStrList(
                        Text.translatable("option.caveinstability.support_entries"),
                        working.supportEntries
                )
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Text.translatable("tooltip.caveinstability.support_entries"))
                .setAddButtonTooltip(Text.translatable("tooltip.caveinstability.support_entries_add"))
                .setCreateNewInstance(list -> new StringListListEntry.StringListCell("<new support block or tag>", list))
                .setSaveConsumer(value -> working.supportEntries = new ArrayList<>(value))
                .build());

        materials.addEntry(entries.startStrList(
                        Text.translatable("option.caveinstability.falling_entries"),
                        working.fallingEntries
                )
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Text.translatable("tooltip.caveinstability.falling_entries"))
                .setAddButtonTooltip(Text.translatable("tooltip.caveinstability.falling_entries_add"))
                .setCreateNewInstance(list -> new StringListListEntry.StringListCell("<new block or tag>=0.50", list))
                .setSaveConsumer(value -> working.fallingEntries = new ArrayList<>(value))
                .build());

        return builder.build();
    }

    private static CaveInstabilityConfig copyOf(CaveInstabilityConfig source) {
        CaveInstabilityConfig copy = new CaveInstabilityConfig();
        copy.delayTicks = source.delayTicks;
        copy.horizontalRange = source.horizontalRange;
        copy.verticalRange = source.verticalRange;
        copy.sourceCaveInChance = source.sourceCaveInChance;
        copy.enableDebrisSliding = source.enableDebrisSliding;
        copy.maxDebrisSlides = source.maxDebrisSlides;
        copy.enableFloatingGroupCollapse = source.enableFloatingGroupCollapse;
        copy.floatingGroupSearchLimit = source.floatingGroupSearchLimit;
        copy.enableSupportPillars = source.enableSupportPillars;
        copy.supportCheckRadius = source.supportCheckRadius;
        copy.supportVerticalTolerance = source.supportVerticalTolerance;
        copy.supportAboveTolerance = source.supportAboveTolerance;
        copy.supportEntries = new ArrayList<>(source.supportEntries);
        copy.fallingEntries = new ArrayList<>(source.fallingEntries);
        return copy;
    }
}