# Demon Breathing Plugin (Paper/Spigot 1.21.1 - 1.21.4)

## Core Features
- Universal combat control: `Shift hold -> release -> left/right click`.
- Charge/status shown in action bar style HUD.
- Breathing-specific particles, cinematic dragon join sequence, and style icon symbols.
- Style-locked katanas with unique CustomModelData.
- Breathing cores can be absorbed/withdrawn.
- Breathing bounty survival system (5 minutes) with one-time defensive quake shield.
- Altar ritual forging with dynamic GUI, recipe board above altar, 10-second ritual, global coordinate bossbar.

## Commands
- `/breathing info`
- `/breathing abilities`
- `/breathing select <style>`
- `/breathing withdraw`
- `/breathing altar`
- `/breathing katana <style>` (admin)
- `/breathing core <style>` (admin)

## CustomModelData Map
### Breathing Cores
- Thunder: `23001`
- Water: `23002`
- Sun: `23003`
- Wind: `23004`
- Moon: `23005`
- Flame: `23006`
- Mist: `23007`

### Katanas
- Thunder: `12141`
- Water: `12142`
- Sun: `12143`
- Wind: `12144`
- Moon: `12145`
- Flame: `12146`
- Mist: `12147`

### Altar
- Breathing Altar: `34001`

## Symbol Codewords (Resource Pack Icon Hook)
Plugin sends action-bar symbols via `player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f<symbol>"));`

- Thunder: `§f\uE001`
- Water: `§f\uE002`
- Sun: `§f\uE003`
- Wind: `§f\uE004`
- Moon: `§f\uE005`
- Flame: `§f\uE006`
- Mist: `§f\uE007`

## Altar Ritual Flow
1. Place Breathing Altar.
2. Right-click -> GUI title: **All Katanas**.
3. Click desired katana -> recipe board appears above altar (visible to all).
4. Right-click altar again with ingredients.
5. 10s ritual starts with dragon particles + rotating katana + global coords bossbar.
6. Ritual ends -> platform removed -> katana drops -> global obtain announcement in English.

## Build
```bash
mvn clean package
```
