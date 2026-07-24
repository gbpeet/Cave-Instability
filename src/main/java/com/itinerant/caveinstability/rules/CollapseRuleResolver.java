package com.itinerant.caveinstability.rules;

import com.itinerant.caveinstability.config.CaveInstabilityConfig;
import com.itinerant.caveinstability.config.CaveInstabilityConfigManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class CollapseRuleResolver {
    private CollapseRuleResolver() {
    }

    public static boolean canCollapse(BlockState state) {
        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();

        for (String rawEntry : config.fallingEntries) {
            ParsedRule rule = parseRule(rawEntry);
            if (rule == null) {
                continue;
            }

            try {
                if (rule.isTag) {
                    TagKey<Block> tag = TagKey.of(Registries.BLOCK.getKey(), rule.id);
                    if (state.isIn(tag)) {
                        return true;
                    }
                } else {
                    Block block = Registries.BLOCK.get(rule.id);
                    if (state.isOf(block)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    public static double getSourceChance(BlockState state) {
        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();

        for (String rawEntry : config.fallingEntries) {
            ParsedRule rule = parseRule(rawEntry);
            if (rule == null) {
                continue;
            }

            try {
                if (rule.isTag) {
                    TagKey<Block> tag = TagKey.of(Registries.BLOCK.getKey(), rule.id);
                    if (state.isIn(tag)) {
                        return rule.sourceChance;
                    }
                } else {
                    Block block = Registries.BLOCK.get(rule.id);
                    if (state.isOf(block)) {
                        return rule.sourceChance;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return config.sourceCaveInChance;
    }

    private static ParsedRule parseRule(String rawEntry) {
        if (rawEntry == null) {
            return null;
        }

        String entry = rawEntry.trim();
        if (entry.isEmpty()) {
            return null;
        }

        String[] parts = entry.split("=", 2);
        String idPart = parts[0].trim();
        if (idPart.isEmpty()) {
            return null;
        }

        CaveInstabilityConfig config = CaveInstabilityConfigManager.getConfig();
        double chance = config.sourceCaveInChance;

        if (parts.length == 2) {
            try {
                chance = Double.parseDouble(parts[1].trim());
            } catch (Exception ignored) {
                chance = config.sourceCaveInChance;
            }
        }

        chance = Math.max(0.0D, Math.min(1.0D, chance));

        try {
            if (idPart.startsWith("#")) {
                return new ParsedRule(true, Identifier.of(idPart.substring(1)), chance);
            } else {
                return new ParsedRule(false, Identifier.of(idPart), chance);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class ParsedRule {
        private final boolean isTag;
        private final Identifier id;
        private final double sourceChance;

        private ParsedRule(boolean isTag, Identifier id, double sourceChance) {
            this.isTag = isTag;
            this.id = id;
            this.sourceChance = sourceChance;
        }
    }
}