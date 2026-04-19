package com.demonbreathing;

import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.command.BreathingCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DemonBreathingPlugin extends JavaPlugin {
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        this.combatManager = new CombatManager(this);

        BreathingCommand breathingCommand = new BreathingCommand(combatManager);
        if (getCommand("breathing") != null) {
            getCommand("breathing").setExecutor(breathingCommand);
            getCommand("breathing").setTabCompleter(breathingCommand);
        }

        getServer().getPluginManager().registerEvents(combatManager, this);
        getLogger().info("DemonBreathing enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DemonBreathing disabled.");
    }
}
