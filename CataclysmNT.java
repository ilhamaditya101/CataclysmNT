package com.cataclysm.nt;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CataclysmNT extends JavaPlugin {
    private NametagManager nametagManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LuckPerms luckPerms;
        try { luckPerms = LuckPermsProvider.get(); }
        catch (IllegalStateException ex) {
            getLogger().severe("LuckPerms API tidak ditemukan. Plugin dimatikan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        nametagManager = new NametagManager(this, luckPerms);
        getServer().getPluginManager().registerEvents(new NametagListener(nametagManager), this);

        for (Player player : Bukkit.getOnlinePlayers()) nametagManager.update(player);

        long interval = Math.max(1L, getConfig().getLong("nametag.update-interval", 20L));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) nametagManager.update(player);
        }, interval, interval);

        // Smooth movement/animation every tick without waiting for the text refresh interval.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) nametagManager.tick(player);
        }, 1L, 1L);

        getLogger().info("CataclysmNT v2 aktif. PlaceholderAPI: " +
                Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));
    }

    @Override
    public void onDisable() {
        if (nametagManager != null) nametagManager.removeAll();
    }
}
