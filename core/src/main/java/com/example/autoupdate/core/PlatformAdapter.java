package com.example.autoupdate.core;

/**
 * プラットフォーム固有の処理（Spigot/BungeeCord/Velocity）を抽象化するインターフェース。
 * Interface layer abstracting platform-specific operations (Spigot/BungeeCord/Velocity).
 */
public interface PlatformAdapter {
    /** 情報ログを出力 / Log informational message */
    void logInfo(String message);

    /** 警告ログを出力 / Log warning message */
    void logWarn(String message);

    /** エラーログを出力 / Log error message */
    void logError(String message);

    /** 全プレイヤーにアナウンスを放送 / Broadcast message to all players */
    void broadcastMessage(String message);

    /** コンソールコマンドを実行 / Execute console command */
    void executeConsoleCommand(String command);

    /** サーバーをシャットダウン / Shutdown server */
    void shutdownServer();

    /** 非同期タスクを実行 / Run task asynchronously */
    void scheduleAsync(Runnable runnable);

    /** 実行環境に応じた Geyser フォルダ名を取得 / Get Geyser directory name according to platform */
    String getGeyserDirectoryName();
}