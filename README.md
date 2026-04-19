# Demon Breathing Plugin (Paper/Spigot 1.21.1 - 1.21.4)

## Highlights
- Universal control: **Shift hold charge -> release -> click activate**.
- Charge shown in **Action Bar cinematic status bar**.
- Each breathing has different charge particles and attack style animation.
- First-join **dragon-style cinematic awakening** sequence assigns a breathing.
- Style-locked katanas (no generic katana abuse).
- **Breathing Altar ritual crafting** with GUI preview, ingredients, 10s ritual, global bossbar coordinate alert.

## Commands
- `/breathing info`
- `/breathing abilities`
- `/breathing select <style>`
- `/breathing altar`
- `/breathing reload`

## Controls
- Right click = Form 1
- Left click = Form 2
- Sprint + click = Form 3

## Ritual Flow
1. Craft/place Breathing Altar (Lodestone).
2. Right-click altar -> open katana visual menu.
3. Select katana preview.
4. Right-click altar again with required ingredients in inventory.
5. 10s ritual starts (particles + rotating katana + global ritual bossbar).
6. Sculk shrieker altar collapses and forged katana drops.

## Build
```bash
mvn clean package
```
