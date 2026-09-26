package com.example.autoupdate.core;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * リモート（Modrinth API）およびローカル JAR からバージョン情報を確認・比較するユーティリティクラス。
 * Utility class for checking and comparing version details from remote (Modrinth API) and local JAR files.
 */
public class VersionChecker {

    /**
     * Modrinth から取得したバージョン情報用レコード。
     * Record holding version details fetched from Modrinth.
     */
    public record ModrinthVersion(String versionNumber, String downloadUrl) {}

    /**
     * Modrinth API から最新リリース情報を取得する。
     * Fetches the latest release version information from the Modrinth API.
     *
     * @param apiUrl 対象の Modrinth API URL / Modrinth API endpoint URL
     * @return ModrinthVersion オブジェクト（失敗時は null） / ModrinthVersion object or null on failure
     */
    public static ModrinthVersion fetchLatestVersion(String apiUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "MCXboxBroadcastAutoUpdate/1.0.0 (Minecraft Server Plugin)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            String body = response.body();

            // レスポンス配列の先頭にある最新版のバージョン番号を取得
            // Extract the version number of the latest release from the JSON array
            Matcher verMatcher = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"").matcher(body);

            // .jar ファイルのダウンロード URL を取得
            // Extract the .jar download URL from the JSON response
            Matcher urlMatcher = Pattern.compile("\"url\"\\s*:\\s*\"(https://cdn\\.modrinth\\.com/[^\"]+\\.jar)\"").matcher(body);

            if (verMatcher.find() && urlMatcher.find()) {
                return new ModrinthVersion(verMatcher.group(1), urlMatcher.group(1));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * ローカルの Target JAR (Geyser拡張/プラグイン等) からバージョン文字列を抽出する。
     * Extracts the version string from a local Target JAR (Geyser Extension, Plugin, etc.).
     *
     * @param jarFile 解析対象の JAR ファイル / Target JAR file to parse
     * @return 抽出されたバージョン文字列 / Extracted version string
     */
    public static String extractLocalVersion(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return "";

        try (ZipFile zip = new ZipFile(jarFile)) {
            // 1. Geyser Extension (extension.yml) の確認
            // Check Geyser Extension metadata
            ZipEntry extensionYmlEntry = zip.getEntry("extension.yml");
            if (extensionYmlEntry != null) {
                String ver = extractFromYmlEntry(zip, extensionYmlEntry);
                if (!ver.isBlank()) return ver;
            }

            // 2. Velocity Plugin (velocity-plugin.json) の確認
            // Check Velocity plugin metadata
            ZipEntry velocityEntry = zip.getEntry("velocity-plugin.json");
            if (velocityEntry != null) {
                String ver = extractFromJsonEntry(zip, velocityEntry);
                if (!ver.isBlank()) return ver;
            }

            // 3. Spigot / Paper / BungeeCord (plugin.yml または bungee.yml) の確認
            // Check Spigot/Paper or BungeeCord plugin metadata
            ZipEntry ymlEntry = zip.getEntry("plugin.yml");
            if (ymlEntry == null) {
                ymlEntry = zip.getEntry("bungee.yml");
            }
            if (ymlEntry != null) {
                String ver = extractFromYmlEntry(zip, ymlEntry);
                if (!ver.isBlank()) return ver;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String extractFromYmlEntry(ZipFile zip, ZipEntry entry) {
        try (InputStream is = zip.getInputStream(entry)) {
            String content = new String(is.readAllBytes());
            Matcher matcher = Pattern.compile("(?m)^version:\\s*['\"]?([^'\"\\r\\n]+)['\"]?").matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String extractFromJsonEntry(ZipFile zip, ZipEntry entry) {
        try (InputStream is = zip.getInputStream(entry)) {
            String content = new String(is.readAllBytes());
            Matcher matcher = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * ローカルバージョンがリモートバージョン以上（最新）であるか判定する。
     * Determines whether the local version is equal to or newer than the remote version.
     *
     * @param localVersion  ローカルのバージョン / Local version string
     * @param remoteVersion リモートのバージョン / Remote version string
     * @return ローカルが最新であれば true / True if local is up-to-date
     */
    public static boolean isLatest(String localVersion, String remoteVersion) {
        if (localVersion == null || localVersion.isBlank()) return false;
        if (remoteVersion == null || remoteVersion.isBlank()) return true;

        // 完全一致チェック / Exact match check
        if (localVersion.trim().equalsIgnoreCase(remoteVersion.trim())) {
            return true;
        }

        // 文字列からビルド番号（数字）を取り出して比較
        // Extract numeric build numbers from strings and compare
        String localNumStr = extractFirstNumber(localVersion);
        String remoteNumStr = extractFirstNumber(remoteVersion);

        if (!localNumStr.isEmpty() && !remoteNumStr.isEmpty()) {
            try {
                int localNum = Integer.parseInt(localNumStr);
                int remoteNum = Integer.parseInt(remoteNumStr);
                // ローカルのビルド番号がリモート以上であれば最新と判定
                // If local build number is greater or equal, consider it latest
                return localNum >= remoteNum;
            } catch (NumberFormatException ignored) {
                return localNumStr.equals(remoteNumStr);
            }
        }

        return false;
    }

    /**
     * 文字列内から最初に見つかった連続する数字（ビルド番号など）を取り出す。
     * Extracts the first consecutive digit sequence (such as a build number) from text.
     */
    private static String extractFirstNumber(String input) {
        Matcher matcher = Pattern.compile("\\d+").matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
}