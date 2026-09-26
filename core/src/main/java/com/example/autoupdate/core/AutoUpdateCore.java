package com.example.autoupdate.core;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自動アップデート処理の中核を担うコアクラス。
 * Core class responsible for handling auto-update logic, task scheduling, and restart sequences.
 */
public class AutoUpdateCore {

    private final PlatformAdapter adapter;
    private final ConfigManager configManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 重複実行を防ぐためのフラグ。
     * Flag to prevent duplicate execution of update tasks.
     */
    private boolean isUpdating = false;

    /**
     * コンストラクタ / Constructor
     *
     * @param dataFolder プラグインデータフォルダー / Plugin data directory
     * @param adapter    各プラットフォーム用のアダプター / Platform adapter instance
     */
    public AutoUpdateCore(File dataFolder, PlatformAdapter adapter) {
        this.adapter = adapter;
        this.configManager = new ConfigManager(dataFolder, adapter);
    }

    /**
     * アップデート監視・タスク処理を開始する。
     * Starts the auto-update checker and scheduled tasks.
     */
    public void start() {
        configManager.load();

        // 起動時チェックが有効な場合 / If startup check is enabled
        if (configManager.isCheckOnStartup()) {
            adapter.scheduleAsync(this::checkAndApplyUpdates);
        }

        // 定期チェックタスクの登録 / Schedule periodic check task
        int intervalHours = configManager.getCheckIntervalHours();
        if (intervalHours > 0) {
            scheduler.scheduleAtFixedRate(
                    this::checkAndApplyUpdates,
                    intervalHours,
                    intervalHours,
                    TimeUnit.HOURS
            );
        }
    }

    /**
     * アップデートの有無を確認し、更新ファイルを適用する。
     * Checks for updates and downloads/applies them if a newer version exists.
     */
    private synchronized void checkAndApplyUpdates() {
        if (isUpdating) return;

        logConsole("checking");

        File targetJar = new File(configManager.getResolvedTargetJarPath());

        // 対象 JAR が存在しない場合は初回ダウンロードを実施
        // If target JAR doesn't exist, execute initial download
        if (!targetJar.exists()) {
            String missingMsg = configManager.getMessage("target-jar-missing")
                    .replace("%path%", targetJar.getPath());
            adapter.logWarn(stripColor(missingMsg));

            downloadInitialJar(targetJar);
            return;
        }

        // バージョン比較 / Version comparison
        String localVer = VersionChecker.extractLocalVersion(targetJar);
        VersionChecker.ModrinthVersion remote = VersionChecker.fetchLatestVersion(configManager.getModrinthApiUrl());

        if (remote == null) {
            logWarnConsole("fetch-latest-failed");
            return;
        }

        // リモートの方が新しい場合、アップデート処理を実施
        // If remote version is newer, execute update procedure
        if (!VersionChecker.isLatest(localVer, remote.versionNumber())) {
            isUpdating = true;

            String updateMsg = configManager.getMessage("update-found")
                    .replace("%remote_version%", remote.versionNumber());
            adapter.logInfo(stripColor(updateMsg));

            File updateFolder = new File(targetJar.getParentFile(), "update");
            if (!updateFolder.exists()) updateFolder.mkdirs();

            File dest = new File(updateFolder, targetJar.getName());
            if (UpdateDownloader.downloadFile(remote.downloadUrl(), dest)) {

                // auto-restart の設定値に応じてログと動作を切り替え
                // Switch log message and action based on auto-restart setting
                if (configManager.isAutoRestart()) {
                    logConsole("download-success");
                    startCountdown();
                } else {
                    logConsole("download-success-manual");
                    isUpdating = false;
                }
            } else {
                logConsole("download-failed");
                isUpdating = false;
            }
        } else {
            logConsole("latest-version");
        }
    }

    /**
     * 初回起動時など、対象 JAR が存在しない場合のダウンロード処理。
     * Downloads the target JAR file when it does not exist locally.
     *
     * @param targetJar ダウンロード先の Target JAR ファイル / Target destination JAR file
     */
    private void downloadInitialJar(File targetJar) {
        isUpdating = true;
        VersionChecker.ModrinthVersion remote = VersionChecker.fetchLatestVersion(configManager.getModrinthApiUrl());

        if (remote == null) {
            logErrorConsole("initial-download-failed-remote");
            isUpdating = false;
            return;
        }

        if (targetJar.getParentFile() != null && !targetJar.getParentFile().exists()) {
            targetJar.getParentFile().mkdirs();
        }

        if (UpdateDownloader.downloadFile(remote.downloadUrl(), targetJar)) {
            String successMsg = configManager.getMessage("initial-download-success")
                    .replace("%file%", targetJar.getName());
            adapter.logInfo(stripColor(successMsg));

            if (configManager.isAutoRestart()) {
                startCountdown();
            } else {
                isUpdating = false;
            }
        } else {
            logErrorConsole("initial-download-failed");
            isUpdating = false;
        }
    }

    /**
     * 再起動アナウンスとカウントダウンを開始する。
     * Starts the restart announcements and countdown timer.
     */
    private void startCountdown() {
        List<Integer> announcements = configManager.getAnnouncements();
        if (announcements == null || announcements.isEmpty() || announcements.contains(0)) {
            executeFinalShutdown();
            return;
        }

        int maxSeconds = Collections.max(announcements);

        final int[] remaining = {maxSeconds};
        scheduler.scheduleAtFixedRate(() -> {
            if (remaining[0] <= 0) {
                executeFinalShutdown();
                return;
            }

            if (announcements.contains(remaining[0])) {
                String msg = configManager.getMessage("announcement")
                        .replace("%seconds%", String.valueOf(remaining[0]));

                adapter.logInfo(stripColor(msg));
                adapter.broadcastMessage(msg);
            }

            remaining[0]--;
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * サーバーの停止・再起動コマンドを実行する。
     * Executes the final shutdown or restart command.
     */
    private void executeFinalShutdown() {
        String stopMsg = configManager.getMessage("stopping");
        adapter.logInfo(stripColor(stopMsg));
        adapter.broadcastMessage(stopMsg);

        if (configManager.isUseCustomShutdownCommand()) {
            adapter.executeConsoleCommand(configManager.getCustomShutdownCommand());
        } else {
            adapter.shutdownServer();
        }
    }

    // --- ログ出力ヘルパーメソッド / Log helper methods ---

    private void logConsole(String messageKey) {
        String msg = configManager.getMessage(messageKey);
        if (!msg.isBlank()) {
            adapter.logInfo(stripColor(msg));
        }
    }

    private void logWarnConsole(String messageKey) {
        String msg = configManager.getMessage(messageKey);
        if (!msg.isBlank()) {
            adapter.logWarn(stripColor(msg));
        }
    }

    private void logErrorConsole(String messageKey) {
        String msg = configManager.getMessage(messageKey);
        if (!msg.isBlank()) {
            adapter.logError(stripColor(msg));
        }
    }

    /**
     * カラーコード（§a 等）を取り除く。
     * Strips color codes (e.g. §a) from text.
     */
    private String stripColor(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
}