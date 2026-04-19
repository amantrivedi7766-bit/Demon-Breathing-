package com.demonbreathing.combat;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.ability.AbilityExecutor;
import com.demonbreathing.model.BreathingStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class CombatManager implements Listener {
    public static final long MAX_CHARGE_MILLIS = 60_000L;
    private static final long RELEASE_WINDOW_MILLIS = 2_500L;
    private final DemonBreathingPlugin plugin;
    private final AbilityExecutor abilityExecutor;
    private final NamespacedKey katanaKey;
    private final NamespacedKey katanaStyleKey;
    private final NamespacedKey assignedStyleKey;
    private final Map<UUID, PlayerCombatState> states = new HashMap<>();

    public CombatManager(DemonBreathingPlugin plugin) {
        this.plugin = plugin;
        this.abilityExecutor = new AbilityExecutor(plugin);
        this.katanaKey = new NamespacedKey(plugin, "demon_katana");
        this.katanaStyleKey = new NamespacedKey(plugin, "katana_style");
        this.assignedStyleKey = new NamespacedKey(plugin, "assigned_style");

        new BukkitRunnable() {
            @Override
            public void run() {
                tickActionBarsAndChargeFX();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public PlayerCombatState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new PlayerCombatState());
    }

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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        state(player);
        if (getAssignedStyle(player) == null) {
            runFirstJoinCinematic(player);
        } else {
            state(player).setStyle(getAssignedStyle(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwap(PlayerItemHeldEvent event) { resetChargeIfNoKatana(event.getPlayer()); }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) { resetChargeIfNoKatana(event.getPlayer()); }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        PlayerCombatState state = state(player);
        if (event.isSneaking()) {
            if (!isKatana(player.getInventory().getItemInMainHand(), player)) return;
            state.setChargeStartMillis(System.currentTimeMillis());
            state.setStoredChargeMillis(0L);
            state.setReleaseWindowExpiresAt(0L);
        } else {
            if (state.chargeStartMillis() <= 0) return;
            long elapsed = System.currentTimeMillis() - state.chargeStartMillis();
            state.setStoredChargeMillis(Math.min(MAX_CHARGE_MILLIS, Math.max(0L, elapsed)));
            state.setReleaseWindowExpiresAt(System.currentTimeMillis() + RELEASE_WINDOW_MILLIS);
            state.setChargeStartMillis(-1L);
            gustBurst(player, state.style());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!isKatana(player.getInventory().getItemInMainHand(), player)) {
            resetCharge(state(player));
            return;
        }

        PlayerCombatState state = state(player);
        long now = System.currentTimeMillis();
        if (state.storedChargeMillis() <= 0 || state.releaseWindowExpiresAt() < now) {
            resetCharge(state);
            return;
        }

        int formIndex = action.name().startsWith("LEFT") ? 1 : 0;
        if (player.isSprinting()) formIndex = 2;

        String cooldownKey = state.style().name() + "_" + formIndex;
        if (state.cooldownUntil(cooldownKey) > now) {
            player.sendActionBar(Component.text("Cooldown " + ((state.cooldownUntil(cooldownKey) - now) / 1000.0) + "s", NamedTextColor.RED));
            return;
        }

        double ratio = state.storedChargeMillis() / (double) MAX_CHARGE_MILLIS;
        abilityExecutor.execute(player, state.style(), formIndex, ratio, state.storedChargeMillis() / 1000.0);
        long cd = (long) (3500 + formIndex * 1600 + ratio * (7500 + formIndex * 2600));
        state.setCooldown(cooldownKey, now + cd);
        player.sendActionBar(Component.text(state.style().formName(formIndex), NamedTextColor.AQUA));
        resetCharge(state);
    }

    public void setStyle(Player player, BreathingStyle style) {
        player.getPersistentDataContainer().set(assignedStyleKey, PersistentDataType.STRING, style.name());
        state(player).setStyle(style);
    }

    public void resetChargeIfNoKatana(Player player) {
        if (!isKatana(player.getInventory().getItemInMainHand(), player)) resetCharge(state(player));
    }

    private void resetCharge(PlayerCombatState state) { state.resetCharge(); }

    private void tickActionBarsAndChargeFX() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerCombatState st = state(player);
            if (getAssignedStyle(player) != null && st.style() != getAssignedStyle(player)) st.setStyle(getAssignedStyle(player));
            if (!isKatana(player.getInventory().getItemInMainHand(), player)) continue;

            if (player.isSneaking() && st.chargeStartMillis() > 0L) {
                long elapsed = Math.min(MAX_CHARGE_MILLIS, Math.max(0L, now - st.chargeStartMillis()));
                double pct = elapsed / (double) MAX_CHARGE_MILLIS;
                int bars = (int) Math.round(pct * 20);
                String visual = "§b[" + "§a" + "|".repeat(Math.max(0, bars)) + "§7" + "|".repeat(Math.max(0, 20 - bars)) + "§b] " + (int) (pct * 100) + "%";
                player.sendActionBar(Component.text("§6Charge " + visual + " §f(" + st.style().displayName() + ")"));
                spawnChargeCinematic(player, st.style(), pct);
            } else if (st.storedChargeMillis() > 0 && st.releaseWindowExpiresAt() > now) {
                double w = (st.releaseWindowExpiresAt() - now) / (double) RELEASE_WINDOW_MILLIS;
                player.sendActionBar(Component.text("§eRelease Window: §f" + String.format(Locale.US, "%.1fs", w * 2.5)));
            }
        }
    }

    private void spawnChargeCinematic(Player player, BreathingStyle style, double pct) {
        Location center = player.getLocation().add(0, 1.0, 0);
        World world = player.getWorld();
        double radius = 0.8 + pct * 1.4;
        for (int i = 0; i < 2; i++) {
            double angle = (System.currentTimeMillis() / 120.0) + (Math.PI * i);
            Location p = center.clone().add(Math.cos(angle) * radius, 0.2 + Math.sin(angle * 2.0) * 0.4, Math.sin(angle) * radius);
            world.spawnParticle(style.chargeParticle(), p, 6, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private void gustBurst(Player player, BreathingStyle style) {
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 40, 0.5, 0.2, 0.5, 0.03);
        player.getWorld().spawnParticle(style.chargeParticle(), player.getLocation().add(0, 1, 0), 35, 0.6, 0.4, 0.6, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.1f);
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
            setStyle(player, chosen);
            player.sendTitle("§6" + chosen.displayName() + " Breathing", "§fHas awakened within you", 10, 50, 20);
            player.getWorld().spawnParticle(chosen.chargeParticle(), player.getLocation().add(0, 1, 0), 90, 0.8, 1.0, 0.8, 0.02);
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }, delay + 10L);
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
        player.sendTitle("§b" + style.displayName() + " Breathing", "§7Dragon resonance", 0, 25, 10);
    }
}
