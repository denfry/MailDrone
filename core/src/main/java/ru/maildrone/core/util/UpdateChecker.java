package ru.maildrone.core.util;

import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Простая проверка обновлений по последнему GitHub-релизу. Полностью на JDK
 * (без зависимостей), сетевой запрос идёт на async-планировщике (Folia-safe).
 */
public final class UpdateChecker {

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private final JavaPlugin plugin;
    private final Schedulers schedulers;
    private final String repo;

    public UpdateChecker(JavaPlugin plugin, Schedulers schedulers, String repo) {
        this.plugin = plugin;
        this.schedulers = schedulers;
        this.repo = repo;
    }

    public void check() {
        schedulers.async(() -> {
            try {
                HttpClient http = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build();
                HttpRequest req = HttpRequest.newBuilder(
                                URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "MailDrone-UpdateChecker")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    return; // релизов ещё нет или сеть недоступна — молча выходим
                }
                Matcher m = TAG.matcher(resp.body());
                if (!m.find()) {
                    return;
                }
                String latest = m.group(1).replaceFirst("^[vV]", "").trim();
                String current = plugin.getPluginMeta().getVersion();
                if (isNewer(latest, current)) {
                    plugin.getLogger().info("Доступна новая версия MailDrone: " + latest
                            + " (установлена " + current + "). Скачать: https://github.com/" + repo + "/releases");
                }
            } catch (Exception ignored) {
                // Проверка обновлений не должна влиять на работу плагина.
            }
        });
    }

    /** Сравнивает версии покомпонентно (по числовым частям). */
    static boolean isNewer(String latest, String current) {
        int[] a = parse(latest);
        int[] b = parse(current);
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String[] parts = version.split("[^0-9]+");
        int[] out = new int[parts.length];
        int n = 0;
        for (String p : parts) {
            if (!p.isEmpty()) {
                try {
                    out[n++] = Integer.parseInt(p);
                } catch (NumberFormatException ignored) {
                    // пропускаем нечисловую часть
                }
            }
        }
        int[] trimmed = new int[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }
}
