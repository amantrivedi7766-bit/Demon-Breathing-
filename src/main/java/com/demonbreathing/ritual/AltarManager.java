package com.demonbreathing.ritual;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.model.BreathingStyle;
import com.demonbreathing.model.KatanaBlueprint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class AltarManager implements Listener {
    private final DemonBreathingPlugin plugin;
    private final CombatManager combat;
    private final NamespacedKey altarKey;
    private final Map<UUID, KatanaBlueprint> selected = new HashMap<>();
    private final Set<Location> activeRituals = new HashSet<>();

    public AltarManager(DemonBreathingPlugin plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
        this.altarKey = new NamespacedKey(plugin, "breathing_altar");
        registerRecipes();
    }

    public ItemStack createAltarItem() {
        ItemStack stack = new ItemStack(Material.LODESTONE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Breathing Altar", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(Component.text("Right-click to begin forged rituals.", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(altarKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if (inHand.hasItemMeta() && inHand.getItemMeta().getPersistentDataContainer().has(altarKey, PersistentDataType.BYTE)) {
            e.getPlayer().sendMessage(Component.text("Breathing Altar placed.", NamedTextColor.LIGHT_PURPLE));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.LODESTONE) return;
        Player p = e.getPlayer();
        Location altar = e.getClickedBlock().getLocation();
        if (activeRituals.contains(altar)) return;
        KatanaBlueprint pick = selected.get(p.getUniqueId());
        if (pick == null) {
            openMenu(p);
            return;
        }
        if (!hasIngredients(p, pick)) {
            p.sendMessage(Component.text("Required items missing for " + pick.style().displayName() + " katana.", NamedTextColor.RED));
            return;
        }
        consumeIngredients(p, pick);
        startRitual(altar, p, pick);
        selected.remove(p.getUniqueId());
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Katana Manifest Altar"));
        int slot = 10;
        for (KatanaBlueprint bp : KatanaBlueprint.values()) {
            ItemStack view = combat.createKatana(bp.style());
            ItemMeta m = view.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to select ritual preview", NamedTextColor.GRAY));
            lore.add(Component.text("Ingredients:", NamedTextColor.YELLOW));
            bp.ingredients().forEach(i -> lore.add(Component.text("- " + i.material() + " x" + i.amount(), NamedTextColor.DARK_AQUA)));
            m.lore(lore);
            view.setItemMeta(m);
            inv.setItem(slot++, view);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getView().title().equals(Component.text("Katana Manifest Altar"))) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            ItemStack current = e.getCurrentItem();
            if (current == null || !current.hasItemMeta()) return;
            String name = current.getItemMeta().displayName().toString();
            for (KatanaBlueprint bp : KatanaBlueprint.values()) {
                if (name.contains(bp.style().displayName())) {
                    selected.put(p.getUniqueId(), bp);
                    p.closeInventory();
                    p.sendMessage(Component.text(bp.style().displayName() + " ritual selected. Right-click altar again to begin.", NamedTextColor.GREEN));
                    p.getWorld().spawnParticle(bp.style().chargeParticle(), p.getLocation().add(0, 1, 0), 70, 0.9, 0.9, 0.9, 0.02);
                    break;
                }
            }
        }
    }

    private void startRitual(Location altarLoc, Player crafter, KatanaBlueprint bp) {
        activeRituals.add(altarLoc);
        World world = altarLoc.getWorld();
        altarLoc.getBlock().setType(Material.SCULK_SHRIEKER);
        ArmorStand stand = world.spawn(altarLoc.clone().add(0.5, 1.3, 0.5), ArmorStand.class, a -> {
            a.setVisible(false); a.setMarker(true); a.setGravity(false); a.getEquipment().setItemInMainHand(combat.createKatana(bp.style()));
        });

        BossBar ritualBar = Bukkit.createBossBar("Katana Ritual @ " + altarLoc.getBlockX() + "," + altarLoc.getBlockY() + "," + altarLoc.getBlockZ(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
        Bukkit.getOnlinePlayers().forEach(ritualBar::addPlayer);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                double progress = tick / 200.0;
                ritualBar.setProgress(Math.min(1.0, progress));
                ritualBar.setTitle("Ritual: " + crafter.getName() + " • " + (10 - tick / 20) + "s");
                Location p = altarLoc.clone().add(0.5, 1.0, 0.5);
                world.spawnParticle(bp.style().chargeParticle(), p, 32, 0.55, 1.0, 0.55, 0.02);
                world.spawnParticle(Particle.ENCHANT, p, 24, 0.6, 0.6, 0.6, 0.2);
                stand.teleport(stand.getLocation().add(0, Math.sin(tick / 8.0) * 0.02, 0));
                stand.setRotation(tick * 12f, 0f);

                if (tick >= 200) {
                    ItemStack katana = combat.createKatana(bp.style());
                    world.dropItemNaturally(altarLoc.clone().add(0.5, 1.3, 0.5), katana);
                    altarLoc.getBlock().setType(Material.AIR);
                    stand.remove();
                    ritualBar.removeAll();
                    Bukkit.getOnlinePlayers().forEach(pl -> pl.sendTitle("§6Katana Forged", "§f" + crafter.getName() + " obtained " + bp.style().displayName(), 5, 45, 15));
                    activeRituals.remove(altarLoc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean hasIngredients(Player player, KatanaBlueprint bp) {
        for (KatanaBlueprint.Ingredient ing : bp.ingredients()) {
            if (count(player.getInventory().getContents(), ing.material()) < ing.amount()) return false;
        }
        return true;
    }

    private void consumeIngredients(Player player, KatanaBlueprint bp) {
        for (KatanaBlueprint.Ingredient ing : bp.ingredients()) {
            int need = ing.amount();
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length && need > 0; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() != ing.material()) continue;
                int remove = Math.min(need, item.getAmount());
                item.setAmount(item.getAmount() - remove);
                need -= remove;
                if (item.getAmount() <= 0) contents[i] = null;
            }
            player.getInventory().setContents(contents);
        }
    }

    private int count(ItemStack[] contents, Material m) {
        int total = 0;
        for (ItemStack i : contents) if (i != null && i.getType() == m) total += i.getAmount();
        return total;
    }

    private void registerRecipes() {
        ShapedRecipe altar = new ShapedRecipe(new NamespacedKey(plugin, "breathing_altar"), createAltarItem());
        altar.shape("SCS", "COC", "SCS");
        altar.setIngredient('S', Material.STONE);
        altar.setIngredient('C', Material.COPPER_INGOT);
        altar.setIngredient('O', Material.OBSIDIAN);
        Bukkit.addRecipe(altar);
    }
}
