package com.example.autoupdate.core;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * HTTP 経由でアップデートファイルをダウンロードするユーティリティクラス。
 * Utility class for downloading update files via HTTP.
 */
public class UpdateDownloader {

    /**
     * リモート URL からファイルをダウンロードしてターゲットパスへ保存する。
     * Downloads a file from the remote URL and saves it to the target file destination.
     *
     * @param downloadUrl ダウンロード対象 URL / Target download URL
     * @param targetFile  保存先ファイル / Destination file path
     * @return 成功した場合は true / Returns true if successful
     */
    public static boolean downloadFile(String downloadUrl, File targetFile) {
        try {
            // 親ディレクトリが存在しない場合は生成 / Ensure parent directory exists
            if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "MCXboxBroadcastAutoUpdate/1.0.0 (Minecraft Server Plugin)")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // 上書きでファイルを書き込み / Copy downloaded stream replacing existing file
                Files.copy(response.body(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }
}