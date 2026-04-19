package com.demonbreathing.model;

import org.bukkit.Material;

import java.util.List;

public enum KatanaBlueprint {
    THUNDER(BreathingStyle.THUNDER, List.of(new Ingredient(Material.GOLD_INGOT, 10), new Ingredient(Material.COPPER_INGOT, 16), new Ingredient(Material.REDSTONE, 20))),
    WATER(BreathingStyle.WATER, List.of(new Ingredient(Material.PRISMARINE_SHARD, 12), new Ingredient(Material.LAPIS_LAZULI, 18), new Ingredient(Material.IRON_INGOT, 8))),
    SUN(BreathingStyle.SUN, List.of(new Ingredient(Material.BLAZE_ROD, 8), new Ingredient(Material.GOLD_INGOT, 14), new Ingredient(Material.MAGMA_CREAM, 10))),
    WIND(BreathingStyle.WIND, List.of(new Ingredient(Material.FEATHER, 24), new Ingredient(Material.IRON_INGOT, 10), new Ingredient(Material.PHANTOM_MEMBRANE, 4))),
    MOON(BreathingStyle.MOON, List.of(new Ingredient(Material.AMETHYST_SHARD, 20), new Ingredient(Material.OBSIDIAN, 12), new Ingredient(Material.ENDER_PEARL, 6))),
    FLAME(BreathingStyle.FLAME, List.of(new Ingredient(Material.BLAZE_POWDER, 20), new Ingredient(Material.NETHER_BRICK, 16), new Ingredient(Material.IRON_INGOT, 10))),
    MIST(BreathingStyle.MIST, List.of(new Ingredient(Material.QUARTZ, 18), new Ingredient(Material.GHAST_TEAR, 4), new Ingredient(Material.LIGHT_GRAY_DYE, 12)));

    private final BreathingStyle style;
    private final List<Ingredient> ingredients;

    KatanaBlueprint(BreathingStyle style, List<Ingredient> ingredients) {
        this.style = style;
        this.ingredients = ingredients;
    }

    public BreathingStyle style() { return style; }
    public List<Ingredient> ingredients() { return ingredients; }

    public record Ingredient(Material material, int amount) {}
}
