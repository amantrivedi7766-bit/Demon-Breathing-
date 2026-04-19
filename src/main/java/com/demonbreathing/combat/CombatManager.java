package com.demonbreathing.combat;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.ability.AbilityExecutor;
import com.demonbreathing.model.BreathingStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class CombatManager implements Listener {
    public static final long MAX_CHARGE_MILLIS = 60_000L;
    private static final long RELEASE_WINDOW_MILLIS = 2_500L;

    private final DemonBreathingPlugin plugin;
    private final AbilityExecutor abilityExecutor;
    private final NamespacedKey katanaKey;
    private final NamespacedKey katanaStyleKey;
    private final NamespacedKey assignedStyleKey;
    private final NamespacedKey breathingItemKey;
    private final Map<UUID, PlayerCombatState> states = new HashMap<>();
    private final Map<UUID, BountyState> bountyStates = new HashMap<>();

    public CombatManager(DemonBreathingPlugin plugin) {
        this.plugin = plugin;
        this.abilityExecutor = new AbilityExecutor(plugin);
        this.katanaKey = new NamespacedKey(plugin, "demon_katana");
        this.katanaStyleKey = new NamespacedKey(plugin, "katana_style");
        this.assignedStyleKey = new NamespacedKey(plugin, "assigned_style");
        this.breathingItemKey = new NamespacedKey(plugin, "breathing_core");

        new BukkitRunnable() {
            @Override public void run() { tickHUDAndBounty(); }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public PlayerCombatState state(Player player) { return states.computeIfAbsent(player.getUniqueId(), k -> new PlayerCombatState()); }

    public ItemStack createKatana(BreathingStyle style) {
        ItemStack stack = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(style.displayName() + " Nichirin Katana", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━"),
                Component.text("§7Forged through disciplined breath and steel."),
                Component.text("§7Bound to §f" + style.displayName() + " Breathing §7resonance."),
                Component.text("§7Control: §fHold Shift §8→ §fRelease §8→ §fClick"),
                Component.text("§7Only a trained wielder can awaken its forms."),
                Component.text("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        ));
        meta.setCustomModelData(12141 + style.ordinal());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(katanaKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(katanaStyleKey, PersistentDataType.STRING, style.name());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createBreathingCore(BreathingStyle style) {
        ItemStack stack = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(style.displayName() + " Breathing Core", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("§7Right-click to absorb this breathing."),
                Component.text("§cCannot absorb more than one breathing."),
                Component.text("§8Serialized symbol: " + styleSymbol(style))
        ));
        meta.setCustomModelData(23001 + style.ordinal());
        meta.getPersistentDataContainer().set(breathingItemKey, PersistentDataType.STRING, style.name());
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isBreathingCore(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(breathingItemKey, PersistentDataType.STRING);
    }

    public BreathingStyle coreStyle(ItemStack stack) {
        if (!isBreathingCore(stack)) return null;
        String raw = stack.getItemMeta().getPersistentDataContainer().get(breathingItemKey, PersistentDataType.STRING);
        return BreathingStyle.parse(raw).orElse(null);
    }

    public boolean isKatana(ItemStack stack, Player player) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        Byte mark = meta.getPersistentDataContainer().get(katanaKey, PersistentDataType.BYTE);
        String styleMark = meta.getPersistentDataContainer().get(katanaStyleKey, PersistentDataType.STRING);
        if (mark == null || mark != 1 || styleMark == null) return false;
        BreathingStyle assigned = getAssignedStyle(player);
        return assigned != null && assigned.name().equals(styleMark);
    }

    public BreathingStyle getAssignedStyle(Player player) {
        String raw = player.getPersistentDataContainer().get(assignedStyleKey, PersistentDataType.STRING);
        if (raw == null) return null;
        return BreathingStyle.parse(raw).orElse(null);
    }

    public void setStyle(Player player, BreathingStyle style) {
        player.getPersistentDataContainer().set(assignedStyleKey, PersistentDataType.STRING, style.name());
        state(player).setStyle(style);
    }

    public void withdrawBreathing(Player player) {
        BreathingStyle style = getAssignedStyle(player);
        if (style == null) {
            player.sendMessage(Component.text("No breathing to withdraw.", NamedTextColor.RED));
            return;
        }
        player.getPersistentDataContainer().remove(assignedStyleKey);
        state(player).setStyle(BreathingStyle.THUNDER);
        player.getWorld().dropItemNaturally(player.getLocation(), createBreathingCore(style));
        player.sendMessage(Component.text("Breathing withdrawn as item.", NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        state(p);
        BreathingStyle assigned = getAssignedStyle(p);
        if (assigned == null) runFirstJoinCinematic(p); else state(p).setStyle(assigned);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { states.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onSwap(PlayerItemHeldEvent e) { resetChargeIfNoKatana(e.getPlayer()); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { resetChargeIfNoKatana(e.getPlayer()); }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        PlayerCombatState st = state(player);
        if (event.isSneaking()) {
            if (!isKatana(player.getInventory().getItemInMainHand(), player)) return;
            st.setChargeStartMillis(System.currentTimeMillis());
            st.setStoredChargeMillis(0L);
            st.setReleaseWindowExpiresAt(0L);
        } else {
            if (st.chargeStartMillis() <= 0) return;
            long elapsed = System.currentTimeMillis() - st.chargeStartMillis();
            st.setStoredChargeMillis(Math.min(MAX_CHARGE_MILLIS, Math.max(0L, elapsed)));
            st.setReleaseWindowExpiresAt(System.currentTimeMillis() + RELEASE_WINDOW_MILLIS);
            st.setChargeStartMillis(-1L);
            gustBurst(player, st.style());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (action.name().startsWith("RIGHT") && isBreathingCore(hand)) {
            BreathingStyle style = coreStyle(hand);
            if (style != null) absorbBreathing(player, style, hand);
            return;
        }

        if (!isKatana(hand, player)) {
            state(player).resetCharge();
            return;
        }

        PlayerCombatState st = state(player);
        long now = System.currentTimeMillis();
        if (st.storedChargeMillis() <= 0 || st.releaseWindowExpiresAt() < now) {
            st.resetCharge();
            return;
        }

        int formIndex = action.name().startsWith("LEFT") ? 1 : 0;
        if (player.isSprinting()) formIndex = 2;
        String cooldownKey = st.style().name() + "_" + formIndex;
        if (st.cooldownUntil(cooldownKey) > now) return;

        double ratio = st.storedChargeMillis() / (double) MAX_CHARGE_MILLIS;
        abilityExecutor.execute(player, st.style(), formIndex, ratio, st.storedChargeMillis() / 1000.0);
        long cd = (long) (3500 + formIndex * 1600 + ratio * (7500 + formIndex * 2600));
        st.setCooldown(cooldownKey, now + cd);
        st.resetCharge();
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item itemEntity = event.getItem();
        if (!isBreathingCore(itemEntity.getItemStack())) return;
        event.setCancelled(true);

        BreathingStyle core = coreStyle(itemEntity.getItemStack());
        itemEntity.remove();
        if (core == null) return;
        if (getAssignedStyle(player) != null) {
            player.sendMessage(Component.text("You already have a breathing.", NamedTextColor.RED));
            player.getWorld().dropItemNaturally(player.getLocation(), createBreathingCore(core));
            return;
        }
        startBounty(player, core);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        BountyState bounty = bountyStates.get(target.getUniqueId());
        if (bounty == null) return;
        long now = System.currentTimeMillis();
        if (bounty.shieldUsed || bounty.shieldCooldownUntil > now) return;

        bounty.shieldUsed = true;
        bounty.shieldCooldownUntil = now + 120_000;
        event.setCancelled(true);
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 120, 1.5, 0.2, 1.5, Material.STONE.createBlockData());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
        if (event.getDamager() instanceof Player attacker) {
            Vector kb = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.8);
            kb.setY(0.7);
            attacker.setVelocity(kb);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        BountyState bounty = bountyStates.remove(dead.getUniqueId());
        if (bounty == null) return;
        bounty.orbit.remove();

        Player killer = dead.getKiller();
        if (killer != null) {
            setStyle(killer, bounty.style);
            Bukkit.broadcastMessage("§6[Breathing Bounty] §f" + killer.getName() + " has claimed " + bounty.style.displayName() + " Breathing!");
        } else {
            dead.getWorld().dropItemNaturally(dead.getLocation(), createBreathingCore(bounty.style));
        }
    }

    public void resetChargeIfNoKatana(Player player) {
        if (!isKatana(player.getInventory().getItemInMainHand(), player)) state(player).resetCharge();
    }

    private void absorbBreathing(Player player, BreathingStyle style, ItemStack stack) {
        if (getAssignedStyle(player) != null) {
            player.sendMessage(Component.text("You cannot absorb more than one breathing.", NamedTextColor.RED));
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        Location origin = player.getLocation().add(0, 1.1, 0);
        Location head = player.getLocation().add(0, 1.8, 0);
        player.getWorld().playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.3f);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                t++;
                double p = t / 20.0;
                Location point = origin.clone().add(head.toVector().subtract(origin.toVector()).multiply(p));
                player.getWorld().spawnParticle(style.chargeParticle(), point, 20, 0.15, 0.15, 0.15, 0.01);
                if (t >= 20) {
                    setStyle(player, style);
                    player.getWorld().spawnParticle(style.chargeParticle(), head, 70, 0.6, 0.6, 0.6, 0.02);
                    player.sendTitle("§b" + style.displayName() + " Breathing", "§fAbsorbed", 6, 30, 14);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startBounty(Player player, BreathingStyle style) {
        ArmorStand orbit = player.getWorld().spawn(player.getLocation().add(1, 1.3, 0), ArmorStand.class, a -> {
            a.setVisible(false); a.setGravity(false); a.setMarker(true); a.getEquipment().setHelmet(createBreathingCore(style));
        });
        BountyState state = new BountyState(style, System.currentTimeMillis() + 300_000, orbit);
        bountyStates.put(player.getUniqueId(), state);
        Bukkit.broadcastMessage("§c[Bounty] §f" + player.getName() + " is carrying " + style.displayName() + " core. Survive 5 minutes!");
    }

    private void tickHUDAndBounty() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerCombatState st = state(player);
            BreathingStyle assigned = getAssignedStyle(player);
            if (assigned != null) st.setStyle(assigned);

            BountyState bounty = bountyStates.get(player.getUniqueId());
            if (bounty != null) {
                double left = (bounty.expiresAt - now) / 1000.0;
                if (left <= 0) {
                    bounty.orbit.remove();
                    bountyStates.remove(player.getUniqueId());
                    setStyle(player, bounty.style);
                    Bukkit.broadcastMessage("§a[Bounty] §f" + player.getName() + " survived and absorbed " + bounty.style.displayName() + " Breathing!");
                } else {
                    double angle = (System.currentTimeMillis() % 4000) / 4000.0 * (Math.PI * 2);
                    Location base = player.getLocation().add(0, 1.2, 0);
                    bounty.orbit.teleport(base.clone().add(Math.cos(angle), 0.1, Math.sin(angle)));
                    player.sendActionBar(Component.text("§cBounty Survival: " + String.format(Locale.US, "%.1fs", left)));
                }
            }

            if (assigned != null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f" + styleSymbol(assigned)));
            }

            if (isKatana(player.getInventory().getItemInMainHand(), player) && player.isSneaking() && st.chargeStartMillis() > 0) {
                long elapsed = Math.min(MAX_CHARGE_MILLIS, Math.max(0, now - st.chargeStartMillis()));
                double pct = elapsed / (double) MAX_CHARGE_MILLIS;
                int bars = (int) (pct * 18);
                String bar = "§8[" + "§a" + "|".repeat(Math.max(0, bars)) + "§7" + "|".repeat(Math.max(0, 18 - bars)) + "§8]";
                player.sendActionBar(Component.text("§6" + bar + " " + (int) (pct * 100) + "% "+st.style().displayName()));
                spawnChargeCinematic(player, st.style(), pct);
            }
        }
    }

    private void spawnChargeCinematic(Player player, BreathingStyle style, double pct) {
        Location center = player.getLocation().add(0, 1.0, 0);
        double radius = 0.8 + pct * 1.4;
        for (int i = 0; i < 3; i++) {
            double angle = (System.currentTimeMillis() / 90.0) + (2.09 * i);
            Location p = center.clone().add(Math.cos(angle) * radius, 0.25 + Math.sin(angle * 2.0) * 0.35, Math.sin(angle) * radius);
            player.getWorld().spawnParticle(style.chargeParticle(), p, 6, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void gustBurst(Player player, BreathingStyle style) {
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 40, 0.5, 0.2, 0.5, 0.03);
        player.getWorld().spawnParticle(style.chargeParticle(), player.getLocation().add(0, 1, 0), 35, 0.6, 0.4, 0.6, 0.02);
    }

    private void runFirstJoinCinematic(Player player) {
        List<BreathingStyle> styles = new ArrayList<>(List.of(BreathingStyle.values()));
        Collections.shuffle(styles);
        long delay = 120L;
        for (BreathingStyle style : styles) {
            long start = delay;
            Bukkit.getScheduler().runTaskLater(plugin, () -> showDragonPass(player, style, true), start);
            Bukkit.getScheduler().runTaskLater(plugin, () -> showDragonPass(player, style, false), start + 80L);
            delay += 140L;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            BreathingStyle chosen = styles.get(0);
            showStylePopupItem(player, chosen);
            setStyle(player, chosen);
            player.sendTitle("§6" + chosen.displayName() + " Breathing", "§fHas awakened within you", 10, 50, 20);
        }, delay + 10L);
    }

    private void showStylePopupItem(Player player, BreathingStyle style) {
        ArmorStand popup = player.getWorld().spawn(player.getLocation().add(0, 2.2, 0), ArmorStand.class, a -> {
            a.setVisible(false); a.setMarker(true); a.setGravity(false); a.getEquipment().setHelmet(createBreathingCore(style));
            a.customName(Component.text(style.displayName() + " Breathing Core", NamedTextColor.AQUA));
            a.setCustomNameVisible(true);
        });
        Bukkit.getScheduler().runTaskLater(plugin, popup::remove, 60L);
    }

    private void showDragonPass(Player player, BreathingStyle style, boolean downward) {
        if (!player.isOnline()) return;
        World world = player.getWorld();
        Location base = player.getLocation();
        for (int t = 0; t < 50; t++) {
            double progress = t / 49.0;
            double y = downward ? (8 - progress * 7) : (1 + progress * 7);
            double spiral = progress * Math.PI * 6;
            Location point = base.clone().add(Math.cos(spiral) * (1.6 - progress * 0.8), y, Math.sin(spiral) * (1.6 - progress * 0.8));
            world.spawnParticle(style.chargeParticle(), point, 4, 0.07, 0.07, 0.07, 0.01);
        }
    }

    public String styleSymbol(BreathingStyle style) {
        return switch (style) {
            case THUNDER -> "\uE001";
            case WATER -> "\uE002";
            case SUN -> "\uE003";
            case WIND -> "\uE004";
            case MOON -> "\uE005";
            case FLAME -> "\uE006";
            case MIST -> "\uE007";
        };
    }

    private static final class BountyState {
        private final BreathingStyle style;
        private final long expiresAt;
        private final ArmorStand orbit;
        private boolean shieldUsed;
        private long shieldCooldownUntil;

        private BountyState(BreathingStyle style, long expiresAt, ArmorStand orbit) {
            this.style = style;
            this.expiresAt = expiresAt;
            this.orbit = orbit;
        }
    }
}
