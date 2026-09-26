package com.example.autoupdate.spigot;

import com.example.autoupdate.core.AutoUpdateCore;
import com.example.autoupdate.core.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlugin extends JavaPlugin implements PlatformAdapter {

    @Override
    public void onEnable() {
        AutoUpdateCore core = new AutoUpdateCore(getDataFolder(), this);
        core.start();
    }

    @Override
    public void logInfo(String message) {
        getLogger().info(message);
    }

    @Override
    public void logWarn(String message) {
        getLogger().warning(message);
    }

    @Override
    public void logError(String message) {
        getLogger().severe(message);
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    @Override
    public void executeConsoleCommand(String command) {
        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        );
    }

    @Override
    public void shutdownServer() {
        Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
    }

    @Override
    public void scheduleAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public String getGeyserDirectoryName() {
        return "Geyser-Spigot";
    }
}