package com.demonbreathing.combat;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.ability.AbilityExecutor;
import com.demonbreathing.model.BreathingStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
    private static final long MAX_CHARGE_MILLIS = 60_000L;
    private static final long RELEASE_WINDOW_MILLIS = 2_500L;

    private final DemonBreathingPlugin plugin;
    private final NamespacedKey katanaKey;
    private final AbilityExecutor abilityExecutor;
    private final Map<UUID, PlayerCombatState> states = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public CombatManager(DemonBreathingPlugin plugin) {
        this.plugin = plugin;
        this.katanaKey = new NamespacedKey(plugin, "demon_katana");
        this.abilityExecutor = new AbilityExecutor(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                updateChargeBars();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public PlayerCombatState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> new PlayerCombatState());
    }

    public ItemStack createKatana() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Nichirin Katana", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Universal breathing catalyst", NamedTextColor.GRAY),
                Component.text("Hold Shift to charge, release, then click.", NamedTextColor.DARK_GRAY)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(katanaKey, PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(12141);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isKatana(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        Byte mark = meta.getPersistentDataContainer().get(katanaKey, PersistentDataType.BYTE);
        return mark != null && mark == 1;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BossBar bar = player.getServer().createBossBar("Breathing Charge", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setVisible(false);
        bars.put(player.getUniqueId(), bar);
        state(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        states.remove(id);
    }

    @EventHandler
    public void onSwapHeldItem(PlayerItemHeldEvent event) {
        resetChargeIfNoKatana(event.getPlayer());
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        resetChargeIfNoKatana(event.getPlayer());
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        PlayerCombatState state = state(player);
        if (event.isSneaking()) {
            if (!isKatana(player.getInventory().getItemInMainHand())) return;
            state.setChargeStartMillis(System.currentTimeMillis());
            state.setStoredChargeMillis(0L);
            state.setReleaseWindowExpiresAt(0L);
        } else {
            if (state.chargeStartMillis() <= 0L) return;
            long elapsed = System.currentTimeMillis() - state.chargeStartMillis();
            long clamped = Math.min(MAX_CHARGE_MILLIS, Math.max(0, elapsed));
            state.setStoredChargeMillis(clamped);
            state.setReleaseWindowExpiresAt(System.currentTimeMillis() + RELEASE_WINDOW_MILLIS);
            state.setChargeStartMillis(-1L);
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.4f);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isKatana(player.getInventory().getItemInMainHand())) {
            resetCharge(state(player), player);
            return;
        }

        PlayerCombatState state = state(player);
        long now = System.currentTimeMillis();
        if (state.storedChargeMillis() <= 0 || state.releaseWindowExpiresAt() < now) {
            resetCharge(state, player);
            return;
        }

        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        int formIndex = state.selectedForm();
        if (right) {
            state.cycleFormForward();
        } else {
            state.cycleFormBackward();
        }

        BreathingStyle style = state.style();
        String cooldownKey = style.name() + "_" + formIndex;
        long coolUntil = state.cooldownUntil(cooldownKey);
        if (coolUntil > now) {
            double left = (coolUntil - now) / 1000.0;
            player.sendActionBar(Component.text("Cooldown: %.1fs".formatted(left), NamedTextColor.RED));
            return;
        }

        double ratio = state.storedChargeMillis() / (double) MAX_CHARGE_MILLIS;
        double chargeSeconds = state.storedChargeMillis() / 1000.0;

        abilityExecutor.execute(player, style, formIndex, ratio, chargeSeconds);

        long base = 6_000L + formIndex * 1_200L;
        long scaled = (long) (base + ratio * (8_000L + formIndex * 3_000L));
        state.setCooldown(cooldownKey, now + scaled);

        String abilityName = style.formName(formIndex);
        player.sendActionBar(Component.text(style.displayName() + " • " + abilityName, NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.25f);

        resetCharge(state, player);
    }

    public void setStyle(Player player, BreathingStyle style) {
        PlayerCombatState state = state(player);
        state.setStyle(style);
        state.setSelectedForm(0);
        player.sendMessage(Component.text("Breathing style set to " + style.displayName(), NamedTextColor.GREEN));
    }

    public void resetChargeIfNoKatana(Player player) {
        if (!isKatana(player.getInventory().getItemInMainHand())) {
            resetCharge(state(player), player);
        }
    }

    private void resetCharge(PlayerCombatState state, Player player) {
        state.resetCharge();
        BossBar bar = bars.get(player.getUniqueId());
        if (bar != null) {
            bar.setVisible(false);
        }
    }

    private void updateChargeBars() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerCombatState state = state(player);
            BossBar bar = bars.get(player.getUniqueId());
            if (bar == null) continue;

            if (!isKatana(player.getInventory().getItemInMainHand())) {
                resetCharge(state, player);
                continue;
            }

            if (player.isSneaking() && state.chargeStartMillis() > 0L) {
                long elapsed = now - state.chargeStartMillis();
                long clamped = Math.min(MAX_CHARGE_MILLIS, Math.max(0, elapsed));
                double progress = clamped / (double) MAX_CHARGE_MILLIS;
                bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                bar.setTitle("Charging: " + (int) (progress * 100) + "%");
                bar.setColor(progress > 0.75 ? BarColor.RED : progress > 0.35 ? BarColor.YELLOW : BarColor.BLUE);
                bar.setVisible(true);
            } else if (state.storedChargeMillis() > 0 && state.releaseWindowExpiresAt() >= now) {
                double windowProgress = (state.releaseWindowExpiresAt() - now) / (double) RELEASE_WINDOW_MILLIS;
                bar.setProgress(Math.max(0.0, Math.min(1.0, windowProgress)));
                bar.setColor(BarColor.WHITE);
                bar.setTitle("Release Window: " + String.format(Locale.US, "%.1fs", (state.releaseWindowExpiresAt() - now) / 1000.0));
                bar.setVisible(true);
            } else {
                resetCharge(state, player);
            }
        }
    }
}
