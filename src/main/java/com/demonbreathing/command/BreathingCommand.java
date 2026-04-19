package com.demonbreathing.command;

import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.model.BreathingStyle;
import com.demonbreathing.ritual.AltarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class BreathingCommand implements CommandExecutor, TabCompleter {
    private final CombatManager combat;
    private final AltarManager altar;

    public BreathingCommand(CombatManager combat, AltarManager altar) {
        this.combat = combat;
        this.altar = altar;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length == 0) return help(p);

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "select" -> {
                if (args.length < 2) { p.sendMessage(Component.text("/breathing select <type>", NamedTextColor.RED)); return true; }
                BreathingStyle.parse(args[1]).ifPresentOrElse(style -> {
                    combat.setStyle(p, style);
                    p.sendMessage(Component.text("Breathing changed to " + style.displayName(), NamedTextColor.GREEN));
                }, () -> p.sendMessage(Component.text("Invalid style", NamedTextColor.RED)));
            }
            case "withdraw" -> combat.withdrawBreathing(p);
            case "info" -> {
                BreathingStyle style = combat.getAssignedStyle(p);
                p.sendMessage(Component.text("Assigned breathing: " + (style == null ? "None" : style.displayName()), NamedTextColor.GOLD));
                p.sendMessage(Component.text("Right click = Form 1, Left click = Form 2, Sprint+Click = Form 3.", NamedTextColor.YELLOW));
            }
            case "abilities" -> {
                BreathingStyle style = combat.getAssignedStyle(p);
                if (style == null) { p.sendMessage(Component.text("No breathing assigned yet.", NamedTextColor.RED)); return true; }
                p.sendMessage(Component.text(style.displayName() + " Forms:", NamedTextColor.AQUA));
                String[] forms = style.forms();
                for (int i = 0; i < forms.length; i++) p.sendMessage(Component.text((i + 1) + ". " + forms[i], NamedTextColor.GRAY));
            }
            case "reload" -> p.sendMessage(Component.text("Config scaffold active.", NamedTextColor.GREEN));
            case "altar" -> { p.getInventory().addItem(altar.createAltarItem()); p.sendMessage(Component.text("Breathing Altar granted.", NamedTextColor.LIGHT_PURPLE)); }
            case "katana" -> {
                if (!p.hasPermission("demonbreathing.admin")) { p.sendMessage(Component.text("No permission", NamedTextColor.RED)); return true; }
                if (args.length < 2) { p.sendMessage(Component.text("/breathing katana <style>", NamedTextColor.RED)); return true; }
                BreathingStyle.parse(args[1]).ifPresent(style -> p.getInventory().addItem(combat.createKatana(style)));
            }
            case "core" -> {
                if (!p.hasPermission("demonbreathing.admin")) { p.sendMessage(Component.text("No permission", NamedTextColor.RED)); return true; }
                if (args.length < 2) { p.sendMessage(Component.text("/breathing core <style>", NamedTextColor.RED)); return true; }
                BreathingStyle.parse(args[1]).ifPresent(style -> p.getInventory().addItem(combat.createBreathingCore(style)));
            }
            default -> help(p);
        }
        return true;
    }

    private boolean help(Player p) {
        p.sendMessage(Component.text("/breathing select <type>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing withdraw", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing info", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing abilities", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing altar", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing katana <style> (admin)", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/breathing core <style> (admin)", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("select", "withdraw", "info", "abilities", "reload", "altar", "katana", "core"), args[0]);
        if (args.length == 2 && List.of("select", "katana", "core").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Arrays.stream(BreathingStyle.values()).map(v -> v.name().toLowerCase(Locale.ROOT)).toList(), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> all, String s) {
        String low = s.toLowerCase(Locale.ROOT);
        return all.stream().filter(v -> v.startsWith(low)).toList();
    }
}
