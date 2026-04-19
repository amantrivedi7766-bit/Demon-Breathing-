package com.demonbreathing.command;

import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.combat.PlayerCombatState;
import com.demonbreathing.model.BreathingStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class BreathingCommand implements CommandExecutor, TabCompleter {
    private final CombatManager combatManager;

    public BreathingCommand(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "select" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /breathing select <type>", NamedTextColor.RED));
                    return true;
                }
                BreathingStyle.parse(args[1]).ifPresentOrElse(
                        style -> combatManager.setStyle(player, style),
                        () -> player.sendMessage(Component.text("Unknown breathing type.", NamedTextColor.RED))
                );
            }
            case "info" -> {
                PlayerCombatState state = combatManager.state(player);
                player.sendMessage(Component.text("Current style: " + state.style().displayName(), NamedTextColor.GOLD));
                player.sendMessage(Component.text("Core controls: Hold SHIFT to charge (up to 60s), release, click within 2.5s.", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Use right/left click to activate and rotate forms.", NamedTextColor.YELLOW));
            }
            case "abilities" -> {
                PlayerCombatState state = combatManager.state(player);
                BreathingStyle style = state.style();
                player.sendMessage(Component.text(style.displayName() + " Breathing Forms:", NamedTextColor.AQUA));
                String[] forms = style.forms();
                for (int i = 0; i < forms.length; i++) {
                    NamedTextColor color = i == state.selectedForm() ? NamedTextColor.GREEN : NamedTextColor.GRAY;
                    player.sendMessage(Component.text((i + 1) + ". " + forms[i], color));
                }
            }
            case "reload" -> player.sendMessage(Component.text("No external config found. Runtime system is active.", NamedTextColor.GREEN));
            case "katana" -> {
                player.getInventory().addItem(combatManager.createKatana());
                player.sendMessage(Component.text("You received a Nichirin Katana.", NamedTextColor.GOLD));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("/breathing select <type>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/breathing info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/breathing abilities", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/breathing reload", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/breathing katana", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("select", "info", "abilities", "reload", "katana"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            List<String> styles = Arrays.stream(BreathingStyle.values()).map(s -> s.name().toLowerCase(Locale.ROOT)).toList();
            return filter(styles, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> all, String current) {
        String low = current.toLowerCase(Locale.ROOT);
        return all.stream().filter(v -> v.startsWith(low)).toList();
    }
}
