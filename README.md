# Demon Breathing Plugin (Paper/Spigot 1.21.1 - 1.21.4)

## Full System Overview
This plugin provides:
- Universal breathing combat controls (`Shift hold -> release -> click`).
- 7 breathing styles with style-locked katanas.
- First-join cinematic spin/dragon sequence.
- Breathing core absorb/withdraw flow.
- Kill-triggered bounty survival event.
- 5-minute altar ritual forging with global coordinate bossbar and dragon-snake particles.

## Core Rules
1. **Katana abilities only work if player has absorbed matching breathing**.
2. **Core pickup does NOT auto-start bounty**.
3. **Bounty starts when a killer claims breathing from a killed player**.
4. If bounty carrier dies, killer receives breathing core directly (no extra bounty on killer).

## Commands
- `/breathing withdraw` (normal players)
- `/breathing info` (admin)
- `/breathing abilities` (admin)
- `/breathing select <style>` (admin)
- `/breathing altar` (admin)
- `/breathing katana <style>` (admin)
- `/breathing core <style>` (admin)
- `/breathing spin <player>` (admin)

## Permission Rules
- Normal players: only `/breathing withdraw`
- Admins (`demonbreathing.admin`): full command access

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

## Overlay Icon Codewords (resource pack hook)
These are sent in action bar using:
`player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f<symbol>"));`

- Thunder: `§f\uE001`
- Water: `§f\uE002`
- Sun: `§f\uE003`
- Wind: `§f\uE004`
- Moon: `§f\uE005`
- Flame: `§f\uE006`
- Mist: `§f\uE007`

## Altar Ritual Details
1. Place altar and open **All Katanas** GUI.
2. Click katana -> recipe board appears above altar.
3. Right-click altar with ingredients.
4. Ritual starts for **5 minutes**:
   - Bossbar shows coordinates + crafter + live timer.
   - Katana rotates in circular cinematic motion above altar.
   - Particle count is optimized (movie-style, not excessive).
   - Ritual platform is protected (players cannot break it during ritual).
5. On completion (auto):
   - Katana drops,
   - Platform is removed automatically,
   - Global English announcement is broadcast.

## Build
```bash
mvn clean package
```
