package com.demonbreathing.model;

import org.bukkit.Particle;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BreathingStyle {
    THUNDER("Thunder", Particle.ELECTRIC_SPARK,
            "Thunderclap and Flash Sixfold Overdrive",
            "Thunder Rift Execution",
            "Divine Thunder Judgment"),
    WATER("Water", Particle.SPLASH,
            "Tidal Severing Flow",
            "Abyssal Dragon Collapse",
            "Endless Current Dominion"),
    SUN("Sun", Particle.FLAME,
            "Radiant Solar Spiral",
            "Blazing Horizon Breaker",
            "Solar Cataclysm Core"),
    WIND("Wind", Particle.CLOUD,
            "Cyclone Fragment Slash",
            "Sky Piercing Gale Rush",
            "Tempest Domain Collapse"),
    MOON("Moon", Particle.DRAGON_BREATH,
            "Crescent Phantom Barrage",
            "Lunar Phase Distortion",
            "Eternal Night Collapse"),
    FLAME("Flame", Particle.SOUL_FIRE_FLAME,
            "Infernal Surge Slash",
            "Blazing Ascension Strike",
            "Crimson Flame Beast Assault"),
    MIST("Mist", Particle.WHITE_ASH,
            "Silent Mist Dash",
            "Illusionary Mist Maze",
            "Void Mist Execution");

    private final String displayName;
    private final Particle chargeParticle;
    private final String[] forms;

    BreathingStyle(String displayName, Particle chargeParticle, String form1, String form2, String form3) {
        this.displayName = displayName;
        this.chargeParticle = chargeParticle;
        this.forms = new String[]{form1, form2, form3};
    }

    public String displayName() { return displayName; }
    public Particle chargeParticle() { return chargeParticle; }
    public String formName(int formIndex) { return forms[Math.max(0, Math.min(forms.length - 1, formIndex))]; }
    public String[] forms() { return Arrays.copyOf(forms, forms.length); }

    public static Optional<BreathingStyle> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (BreathingStyle style : values()) {
            if (style.name().equals(normalized) || style.displayName.toUpperCase(Locale.ROOT).equals(normalized)) return Optional.of(style);
        }
        return Optional.empty();
    }
}
