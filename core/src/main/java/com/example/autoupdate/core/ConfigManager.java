package com.example.autoupdate.core;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 設定ファイル（config.yml, language.yml）の読み込み・保持を行うマネージャークラス。
 * Manager class responsible for loading and retrieving config.yml and language.yml settings.
 */
public class ConfigManager {

    private final File dataFolder;
    private final PlatformAdapter adapter;
    private Map<String, Object> configData;
    private Map<String, Object> languageData;

    public ConfigManager(File dataFolder, PlatformAdapter adapter) {
        this.dataFolder = dataFolder;
        this.adapter = adapter;
    }

    /**
     * 設定ファイルおよび言語ファイルを読み込む。
     * Loads configuration and language files into memory.
     */
    @SuppressWarnings("unchecked")
    public void load() {
        if (!dataFolder.exists()) dataFolder.mkdirs();

        File configFile = new File(dataFolder, "config.yml");
        File langFile = new File(dataFolder, "language.yml");

        // リソースからデフォルトファイルを抽出 / Save default files from resources
        saveDefaultFile("config.yml", configFile);
        saveDefaultFile("language.yml", langFile);

        Yaml yaml = new Yaml();

        // language.yml のロード
        try (InputStream in = new FileInputStream(langFile)) {
            languageData = yaml.load(in);
        } catch (Exception e) {
            adapter.logError("[MCXboxBroadcastAutoUpdate] language.yml の読み込みに失敗しました: " + e.getMessage());
        }

        // config.yml のロード
        try (InputStream in = new FileInputStream(configFile)) {
            configData = yaml.load(in);
        } catch (Exception e) {
            logFormattedError("config-load-failed", "%error%", e.getMessage());
        }
    }

    /**
     * デフォルトファイルをリソースから複製する。
     * Saves default resource files to disk if they do not exist.
     */
    private void saveDefaultFile(String resourcePath, File targetFile) {
        if (!targetFile.exists()) {
            try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resourcePath);
                 FileOutputStream out = new FileOutputStream(targetFile)) {
                if (in != null) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            } catch (Exception e) {
                logFormattedError("default-copy-failed", "%resource%", resourcePath);
            }
        }
    }

    private void logFormattedError(String key, String placeholder, String value) {
        String msg = getMessage(key).replace(placeholder, value);
        adapter.logError(msg.replaceAll("§[0-9a-fk-or]", ""));
    }

    // --- 各設定項目のゲッター / Configuration Getters ---

    @SuppressWarnings("unchecked")
    public boolean isCheckOnStartup() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("auto-update");
        return sec != null && (boolean) sec.getOrDefault("check-on-startup", true);
    }

    @SuppressWarnings("unchecked")
    public int getCheckIntervalHours() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("auto-update");
        return sec != null ? ((Number) sec.getOrDefault("check-interval-hours", 1)).intValue() : 1;
    }

    @SuppressWarnings("unchecked")
    public String getModrinthApiUrl() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("auto-update");
        return sec != null ? (String) sec.getOrDefault("modrinth-api-url", "") : "";
    }

    @SuppressWarnings("unchecked")
    public String getPlatformSetting() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("auto-update");
        return sec != null ? (String) sec.getOrDefault("platform", "Automatic") : "Automatic";
    }

    /**
     * %automatic% プレースホルダーを置換済みのターゲットパスを取得する。
     * Resolves and gets the target JAR path by replacing the %automatic% placeholder.
     */
    @SuppressWarnings("unchecked")
    public String getResolvedTargetJarPath() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("auto-update");
        String rawPath = sec != null ? (String) sec.getOrDefault("target-jar-path", "") : "";

        String platform = getPlatformSetting();
        String geyserDirName;

        if ("Automatic".equalsIgnoreCase(platform) || platform.isBlank()) {
            geyserDirName = adapter.getGeyserDirectoryName();
        } else {
            geyserDirName = platform;
        }

        return rawPath.replace("%automatic%", geyserDirName);
    }

    @SuppressWarnings("unchecked")
    public boolean isAutoRestart() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("restart-settings");
        return sec != null && (boolean) sec.getOrDefault("auto-restart", true);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getAnnouncements() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("restart-settings");
        return sec != null ? (List<Integer>) sec.get("announcements") : List.of();
    }

    @SuppressWarnings("unchecked")
    public boolean isUseCustomShutdownCommand() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("restart-settings");
        return sec != null && (boolean) sec.getOrDefault("use-custom-shutdown-command", false);
    }

    @SuppressWarnings("unchecked")
    public String getCustomShutdownCommand() {
        Map<String, Object> sec = (Map<String, Object>) configData.get("restart-settings");
        return sec != null ? (String) sec.getOrDefault("custom-shutdown-command", "stop") : "stop";
    }

    /**
     * language.yml からキーに対応するメッセージを取得する（プレフィックス自動付与）。
     * Retrieves a formatted message from language.yml with prefix attached.
     */
    @SuppressWarnings("unchecked")
    public String getMessage(String key) {
        if (languageData == null) return "";
        String prefix = (String) languageData.getOrDefault("prefix", "");
        Map<String, String> msgs = (Map<String, String>) languageData.get("messages");
        String msg = msgs != null ? msgs.getOrDefault(key, "") : "";
        return msg.replace("%prefix%", prefix);
    }
}