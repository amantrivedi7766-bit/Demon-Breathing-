package com.demonbreathing.ritual;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.model.KatanaBlueprint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
    private final Map<UUID, Location> lastAltar = new HashMap<>();
    private final Map<Location, List<ArmorStand>> recipeBoards = new HashMap<>();
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
        meta.lore(List.of(Component.text("Right-click to open All Katanas menu.", NamedTextColor.GRAY)));
        meta.setCustomModelData(34001);
        meta.getPersistentDataContainer().set(altarKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.LODESTONE) return;

        Player p = e.getPlayer();
        Location altar = e.getClickedBlock().getLocation();
        lastAltar.put(p.getUniqueId(), altar);
        if (activeRituals.contains(altar)) return;

        KatanaBlueprint pick = selected.get(p.getUniqueId());
        if (pick == null) {
            openMenu(p);
            return;
        }

        if (!hasIngredients(p, pick)) {
            p.sendMessage(Component.text("Missing recipe ingredients.", NamedTextColor.RED));
            return;
        }

        consumeIngredients(p, pick);
        clearRecipeBoard(altar);
        startRitual(altar, p, pick);
        selected.remove(p.getUniqueId());
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("All Katanas"));
        for (int i = 0; i < 54; i++) inv.setItem(i, glass(i));
        int slot = 10;
        for (KatanaBlueprint bp : KatanaBlueprint.values()) {
            ItemStack katana = combat.createKatana(bp.style());
            ItemMeta m = katana.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to preview this katana recipe", NamedTextColor.GRAY));
            lore.add(Component.text("CustomModelData: " + (12141 + bp.style().ordinal()), NamedTextColor.DARK_GRAY));
            bp.ingredients().forEach(i -> lore.add(Component.text(i.material() + " x" + i.amount(), NamedTextColor.AQUA)));
            m.lore(lore);
            katana.setItemMeta(m);
            inv.setItem(slot, katana);
            slot += (slot % 9 == 7) ? 3 : 1;
        }
        player.openInventory(inv);
    }

    private ItemStack glass(int i) {
        ItemStack item = new ItemStack(i % 2 == 0 ? Material.BLACK_STAINED_GLASS_PANE : Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta m = item.getItemMeta();
        m.displayName(Component.text(" "));
        item.setItemMeta(m);
        return item;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(Component.text("All Katanas"))) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack current = e.getCurrentItem();
        if (current == null || !current.hasItemMeta()) return;

        String name = current.getItemMeta().displayName().toString();
        for (KatanaBlueprint bp : KatanaBlueprint.values()) {
            if (name.contains(bp.style().displayName())) {
                selected.put(p.getUniqueId(), bp);
                p.closeInventory();
                Location altar = lastAltar.get(p.getUniqueId());
                if (altar != null) showRecipeBoard(altar, bp);
                p.sendMessage(Component.text("Selected: " + bp.style().displayName() + " katana. Right-click altar to start ritual.", NamedTextColor.GREEN));
                break;
            }
        }
    }

    private void showRecipeBoard(Location altar, KatanaBlueprint bp) {
        clearRecipeBoard(altar);
        List<ArmorStand> lines = new ArrayList<>();
        List<String> text = new ArrayList<>();
        text.add("§6§lRecipe: " + bp.style().displayName());
        bp.ingredients().forEach(i -> text.add("§b" + i.material() + " x" + i.amount()));

        double y = 2.8;
        for (String line : text) {
            ArmorStand as = altar.getWorld().spawn(altar.clone().add(0.5, y, 0.5), ArmorStand.class, a -> {
                a.setVisible(false); a.setMarker(true); a.setGravity(false); a.customName(Component.text(line)); a.setCustomNameVisible(true);
            });
            lines.add(as);
            y -= 0.3;
        }
        recipeBoards.put(altar, lines);
    }

    private void clearRecipeBoard(Location altar) {
        List<ArmorStand> list = recipeBoards.remove(altar);
        if (list != null) list.forEach(Entity::remove);
    }

    private void startRitual(Location altarLoc, Player crafter, KatanaBlueprint bp) {
        activeRituals.add(altarLoc);
        World world = altarLoc.getWorld();

        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            Location b = altarLoc.clone().add(x, 0, z);
            b.getBlock().setType(Material.SCULK);
        }
        altarLoc.getBlock().setType(Material.SCULK_SHRIEKER);

        ArmorStand stand = world.spawn(altarLoc.clone().add(0.5, 1.8, 0.5), ArmorStand.class, a -> {
            a.setVisible(false); a.setMarker(true); a.setGravity(false); a.getEquipment().setItemInMainHand(combat.createKatana(bp.style()));
        });

        BossBar bar = Bukkit.createBossBar("Ritual at " + altarLoc.getBlockX() + ", " + altarLoc.getBlockY() + ", " + altarLoc.getBlockZ(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                tick++;
                double progress = tick / 200.0;
                bar.setProgress(Math.min(1.0, progress));
                bar.setTitle("Dragon Ritual • " + crafter.getName() + " • " + (10 - tick / 20) + "s");

                Location c = altarLoc.clone().add(0.5, 1.2, 0.5);
                world.spawnParticle(bp.style().chargeParticle(), c, 40, 1.2, 0.9, 1.2, 0.02);
                world.spawnParticle(Particle.DRAGON_BREATH, c, 35, 1.0, 0.8, 1.0, 0.02);
                stand.teleport(stand.getLocation().add(0, Math.sin(tick / 7.0) * 0.03, 0));
                stand.setRotation(tick * 12f, 0f);

                if (tick >= 200) {
                    ItemStack katana = combat.createKatana(bp.style());
                    world.dropItemNaturally(altarLoc.clone().add(0.5, 1.3, 0.5), katana);
                    stand.remove();
                    bar.removeAll();
                    for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                        Location b = altarLoc.clone().add(x, 0, z);
                        if (b.getBlock().getType() == Material.SCULK) b.getBlock().setType(Material.AIR);
                    }
                    altarLoc.getBlock().setType(Material.AIR);
                    world.spawnParticle(bp.style().chargeParticle(), crafter.getLocation().add(0, 1, 0), 90, 0.8, 1.2, 0.8, 0.02);
                    Bukkit.broadcastMessage("§6[Announcement] §f" + crafter.getName() + " has obtained the " + bp.style().displayName() + " Katana!");
                    activeRituals.remove(altarLoc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean hasIngredients(Player player, KatanaBlueprint bp) {
        for (KatanaBlueprint.Ingredient ing : bp.ingredients()) if (count(player.getInventory().getContents(), ing.material()) < ing.amount()) return false;
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

    private int count(ItemStack[] contents, Material m) { int total = 0; for (ItemStack i : contents) if (i != null && i.getType() == m) total += i.getAmount(); return total; }

    private void registerRecipes() {
        ShapedRecipe altar = new ShapedRecipe(new NamespacedKey(plugin, "breathing_altar"), createAltarItem());
        altar.shape("SCS", "COC", "SCS");
        altar.setIngredient('S', Material.STONE);
        altar.setIngredient('C', Material.COPPER_INGOT);
        altar.setIngredient('O', Material.OBSIDIAN);
        Bukkit.addRecipe(altar);
    }
}
