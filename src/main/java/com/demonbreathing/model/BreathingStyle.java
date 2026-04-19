package com.demonbreathing.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BreathingStyle {
    THUNDER(
            "Thunder",
            "Thunderclap and Flash Sixfold",
            "Thunder Swarm Barrage",
            "Thunder God Descent"
    ),
    WATER(
            "Water",
            "Flowing Water Slash",
            "Water Dragon Surge",
            "Constant Flux"
    ),
    SUN(
            "Sun",
            "Solar Flame Dance",
            "Burning Horizon Slash",
            "Radiant Sun Explosion"
    ),
    WIND(
            "Wind",
            "Dust Cyclone Cutter",
            "Gale Force Dash",
            "Storm Annihilation"
    ),
    MOON(
            "Moon",
            "Crescent Moon Slash",
            "Dark Moon Barrage",
            "Lunar Cataclysm"
    ),
    FLAME(
            "Flame",
            "Unknowing Fire",
            "Rising Scorching Flame",
            "Flame Tiger Strike"
    ),
    MIST(
            "Mist",
            "Low Clouds Strike",
            "Eight Layered Mist",
            "Obscuring Mist Execution"
    );

    private final String displayName;
    private final String[] forms;

    BreathingStyle(String displayName, String form1, String form2, String form3) {
        this.displayName = displayName;
        this.forms = new String[]{form1, form2, form3};
    }

    public String displayName() {
        return displayName;
    }

    public String formName(int formIndex) {
        return forms[Math.max(0, Math.min(forms.length - 1, formIndex))];
    }

    public String[] forms() {
        return Arrays.copyOf(forms, forms.length);
    }

    public static Optional<BreathingStyle> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (BreathingStyle style : values()) {
            if (style.name().equals(normalized) || style.displayName.toUpperCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(style);
            }
        }
        return Optional.empty();
    }
}
