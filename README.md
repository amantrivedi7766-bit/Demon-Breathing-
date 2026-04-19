# Demon Breathing Plugin

A full combat plugin inspired by Demon Slayer breathing styles, built for **Spigot/Paper 1.21.1-1.21.4** with a **single universal control system**.

## Core Universal Controls

1. Hold the custom **Nichirin Katana**.
2. Hold **Shift (crouch)** to charge (up to **60s**).
3. Release shift to store charge for **2.5s**.
4. During release window, use **Left Click** or **Right Click** to fire the selected form.
5. Right click/left click also rotate form direction after activation.

If katana is not in hand (or swapped mid-charge), charge instantly resets.

## Commands

- `/breathing select <type>` - select breathing style
- `/breathing info` - show current style and control help
- `/breathing abilities` - list current style forms
- `/breathing reload` - runtime reload message (configless runtime)
- `/breathing katana` - receive the custom katana

## Breathing Styles and Forms

### Thunder
1. Thunderclap and Flash Sixfold
2. Thunder Swarm Barrage
3. Thunder God Descent

### Water
1. Flowing Water Slash
2. Water Dragon Surge
3. Constant Flux

### Sun
1. Solar Flame Dance
2. Burning Horizon Slash
3. Radiant Sun Explosion

### Wind
1. Dust Cyclone Cutter
2. Gale Force Dash
3. Storm Annihilation

### Moon
1. Crescent Moon Slash
2. Dark Moon Barrage
3. Lunar Cataclysm

### Flame
1. Unknowing Fire
2. Rising Scorching Flame
3. Flame Tiger Strike

### Mist
1. Low Clouds Strike
2. Eight Layered Mist
3. Obscuring Mist Execution

## Balance and Performance

- Damage/range/speed scale with charge ratio.
- Even low charge is meaningful; full charge enables heavy burst (totem-pop-capable forms).
- Cooldowns scale by charge and form tier to prevent spam while keeping combat fluid.
- Boss bar updates every 2 ticks with charge/release-window visual feedback.
- Event-driven design with bounded scheduled tasks for smooth PvP server usage.

## Build

```bash
mvn clean package
```

JAR output: `target/demon-breathing-1.0.0.jar`

## CI

GitHub Actions workflow included at `.github/workflows/build.yml` to compile on push/PR.
