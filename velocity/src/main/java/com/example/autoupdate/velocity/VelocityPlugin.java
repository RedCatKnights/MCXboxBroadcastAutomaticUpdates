package com.example.autoupdate.velocity;

import com.example.autoupdate.core.AutoUpdateCore;
import com.example.autoupdate.core.PlatformAdapter;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "mcxboxbroadcastautoupdate",
        name = "MCXboxBroadcastAutoUpdate",
        version = "2.0.1",
        authors = {"RedCatKnights"}
)
public class VelocityPlugin implements PlatformAdapter {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        AutoUpdateCore core = new AutoUpdateCore(dataDirectory.toFile(), this);
        core.start();
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }

    @Override
    public void logWarn(String message) {
        logger.warn(message);
    }

    @Override
    public void logError(String message) {
        logger.error(message);
    }

    @Override
    public void broadcastMessage(String message) {
        server.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    @Override
    public void executeConsoleCommand(String command) {
        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
    }

    @Override
    public void shutdownServer() {
        server.shutdown();
    }

    @Override
    public void scheduleAsync(Runnable runnable) {
        server.getScheduler().buildTask(this, runnable).schedule();
    }

    @Override
    public String getGeyserDirectoryName() {
        return "Geyser-Velocity";
    }
}