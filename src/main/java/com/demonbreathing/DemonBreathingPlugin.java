package com.demonbreathing;

import com.demonbreathing.combat.CombatManager;
import com.demonbreathing.command.BreathingCommand;
import com.demonbreathing.ritual.AltarManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DemonBreathingPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        CombatManager combat = new CombatManager(this);
        AltarManager altar = new AltarManager(this, combat);

        BreathingCommand cmd = new BreathingCommand(combat, altar);
        if (getCommand("breathing") != null) {
            getCommand("breathing").setExecutor(cmd);
            getCommand("breathing").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(combat, this);
        getServer().getPluginManager().registerEvents(altar, this);
        getLogger().info("DemonBreathing enabled with ritual/charge cinematic systems.");
    }
}
