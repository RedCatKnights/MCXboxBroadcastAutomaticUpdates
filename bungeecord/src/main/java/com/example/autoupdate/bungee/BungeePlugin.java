package com.example.autoupdate.bungee;

import com.example.autoupdate.core.AutoUpdateCore;
import com.example.autoupdate.core.PlatformAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin implements PlatformAdapter {

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
        getProxy().broadcast(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void executeConsoleCommand(String command) {
        getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), command);
    }

    @Override
    public void shutdownServer() {
        getProxy().stop();
    }

    @Override
    public void scheduleAsync(Runnable runnable) {
        getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public String getGeyserDirectoryName() {
        return "Geyser-Bungeecord";
    }
}