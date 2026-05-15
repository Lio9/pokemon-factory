package com.lio9.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程 CSV 文件下载与本地缓存管理。
 * <p>
 * 从 PokeAPI 的 GitHub CSV 仓库按需下载文件到本地临时缓存目录，
 * 避免每次启动都重复下载。支持配置远程 URL 和缓存目录。
 * </p>
 */
@Component
public class CsvDownloader {
    private static final Logger log = LoggerFactory.getLogger(CsvDownloader.class);
    private static final Duration CSV_CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CSV_DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CSV_CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final CommonDatabaseProperties properties;

    public CsvDownloader(CommonDatabaseProperties properties) {
        this.properties = properties;
    }

    /**
     * 确保 CSV 文件已缓存在本地，若不存在或已损坏则从远程下载。
     */
    public Path ensureCached(Path csvFile) throws IOException {
        if (!StringUtils.hasText(properties.getRemoteCsvBaseUrl())) {
            throw new IllegalStateException("未配置远程 CSV 源，无法获取文件: " + csvFile.getFileName());
        }
        Path cacheDir = resolveCacheDir();
        Path cachedFile = cacheDir.resolve(csvFile.getFileName());
        if (isUsableCache(cachedFile)) {
            log.info("复用已缓存的远程 CSV 文件：{}", cachedFile.toAbsolutePath().normalize());
            return cachedFile;
        }
        return download(cachedFile);
    }

    private Path download(Path cachedFile) throws IOException {
        String fileName = cachedFile.getFileName().toString();
        String baseUrl = properties.getRemoteCsvBaseUrl().replaceAll("/+$", "");
        URI uri = URI.create(baseUrl + "/" + fileName);
        Path tempFile = cachedFile.resolveSibling(fileName + ".download");
        Files.deleteIfExists(tempFile);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(CSV_DOWNLOAD_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Files.deleteIfExists(tempFile);
                throw new IllegalStateException(
                        "下载远程 CSV 失败: " + fileName + " <- " + uri + "，HTTP 状态码=" + response.statusCode());
            }
            if (!Files.exists(tempFile) || Files.size(tempFile) == 0L) {
                Files.deleteIfExists(tempFile);
                throw new IllegalStateException("下载到空的远程 CSV 文件: " + fileName + " <- " + uri);
            }
            Files.move(tempFile, cachedFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info("已从远程 CSV 源下载 {} -> {}", uri, cachedFile.toAbsolutePath().normalize());
            return cachedFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("下载远程 CSV 被中断: " + uri, e);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private boolean isUsableCache(Path cachedFile) {
        if (!Files.exists(cachedFile)) {
            return false;
        }
        try {
            if (Files.size(cachedFile) == 0L) {
                return false;
            }
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    /**
     * 获取本地 CSV 缓存目录路径，在此目录下的文件会被 ensureCached 优先复用。
     */
    public Path getCacheDirectory() {
        return resolveCacheDir();
    }

    private Path resolveCacheDir() {
        Path cacheDir = StringUtils.hasText(properties.getCsvCacheDirectory())
                ? Paths.get(properties.getCsvCacheDirectory())
                : Paths.get(System.getProperty("java.io.tmpdir"), "pokemon-factory", "csv-cache");
        if (!cacheDir.isAbsolute()) {
            cacheDir = Paths.get(System.getProperty("user.dir")).resolve(cacheDir).normalize();
        }
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("创建 CSV 缓存目录失败: " + cacheDir, e);
        }
        return cacheDir;
    }

    /**
     * 由 CsvImporter 调用的缓存校验路由，包含 CSV 内容完整性检查。
     */
    boolean validatedCache(Path cachedFile, List<List<String>> rows) {
        if (rows.size() <= 1) {
            deleteQuietly(cachedFile);
            return false;
        }
        List<String> expectedHeaders = KNOWN_CSV_HEADERS.get(cachedFile.getFileName().toString());
        if (expectedHeaders != null && !expectedHeaders.isEmpty()) {
            List<String> missing = expectedHeaders.stream()
                    .filter(h -> !rows.get(0).contains(h))
                    .toList();
            if (!missing.isEmpty()) {
                log.warn("缓存 CSV 文件头不完整，准备重新下载：{} -> {}", cachedFile.getFileName(), missing);
                deleteQuietly(cachedFile);
                return false;
            }
        }
        return true;
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    static final Map<String, List<String>> KNOWN_CSV_HEADERS = createKnownCsvHeaders();

    private static Map<String, List<String>> createKnownCsvHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("genders.csv", List.of("id", "identifier"));
        headers.put("growth_rate_prose.csv", List.of("growth_rate_id", "local_language_id", "name"));
        headers.put("growth_rates.csv", List.of("id", "identifier", "formula"));
        headers.put("egg_group_prose.csv", List.of("egg_group_id", "local_language_id", "name"));
        headers.put("egg_groups.csv", List.of("id"));
        headers.put("nature_names.csv", List.of("nature_id", "local_language_id", "name"));
        headers.put("natures.csv", List.of("id", "identifier"));
        headers.put("pokemon_move_method_prose.csv",
                List.of("pokemon_move_method_id", "local_language_id", "name", "description"));
        headers.put("pokemon_move_methods.csv", List.of("id", "identifier"));
        headers.put("evolution_triggers.csv", List.of("id", "identifier"));
        headers.put("evolution_trigger_prose.csv", List.of("evolution_trigger_id", "local_language_id", "name"));
        headers.put("move_targets.csv", List.of("id", "identifier"));
        headers.put("move_target_prose.csv", List.of("move_target_id", "local_language_id", "name", "description"));
        headers.put("move_meta_ailments.csv", List.of("id", "identifier"));
        headers.put("move_meta_categories.csv", List.of("id", "identifier"));
        headers.put("type_names.csv", List.of("type_id", "local_language_id", "name"));
        headers.put("type_efficacy.csv", List.of("damage_type_id", "target_type_id", "damage_factor"));
        headers.put("ability_names.csv", List.of("ability_id", "local_language_id", "name"));
        headers.put("ability_prose.csv", List.of("ability_id", "local_language_id", "short_effect", "effect"));
        headers.put("abilities.csv", List.of("id", "identifier", "generation_id", "is_main_series"));
        headers.put("move_names.csv", List.of("move_id", "local_language_id", "name"));
        headers.put("move_flavor_text.csv", List.of("move_id", "version_group_id", "language_id", "flavor_text"));
        headers.put("moves.csv", List.of("id", "identifier", "type_id", "damage_class_id", "target_id", "power", "pp",
                "accuracy", "priority", "effect_chance", "generation_id"));
        headers.put("move_meta.csv", List.of("move_id", "min_hits", "max_hits", "min_turns", "max_turns", "drain",
                "healing", "crit_rate", "ailment_id", "category_id", "ailment_chance", "flinch_chance", "stat_chance"));
        headers.put("move_flags.csv", List.of("id", "identifier"));
        headers.put("move_flag_map.csv", List.of("move_id", "move_flag_id"));
        headers.put("move_meta_stat_changes.csv", List.of("move_id", "stat_id", "change"));
        headers.put("version_groups.csv", List.of("id", "identifier", "generation_id", "order"));
        headers.put("item_pockets.csv", List.of("id", "identifier"));
        headers.put("item_categories.csv", List.of("id", "pocket_id", "identifier"));
        headers.put("item_fling_effects.csv", List.of("id", "identifier"));
        headers.put("item_names.csv", List.of("item_id", "local_language_id", "name"));
        headers.put("item_flavor_text.csv", List.of("item_id", "version_group_id", "language_id", "flavor_text"));
        headers.put("items.csv", List.of("id", "identifier", "category_id", "cost", "fling_power", "fling_effect_id"));
        headers.put("evolution_chains.csv", List.of("id", "baby_trigger_item_id"));
        headers.put("pokemon_species_names.csv", List.of("pokemon_species_id", "local_language_id", "name", "genus"));
        headers.put("pokemon_species_flavor_text.csv",
                List.of("species_id", "version_id", "language_id", "flavor_text"));
        headers.put("pokemon_species.csv",
                List.of("id", "identifier", "generation_id", "evolution_chain_id", "evolves_from_species_id",
                        "gender_rate", "capture_rate", "base_happiness", "hatch_counter", "is_baby", "is_legendary",
                        "is_mythical"));
        headers.put("pokemon.csv", List.of("id", "species_id", "identifier", "is_default", "height", "weight",
                "base_experience", "order"));
        headers.put("pokemon_types.csv", List.of("pokemon_id", "type_id", "slot"));
        headers.put("pokemon_abilities.csv", List.of("pokemon_id", "ability_id", "is_hidden", "slot"));
        headers.put("pokemon_stats.csv", List.of("pokemon_id", "stat_id", "base_stat", "effort"));
        headers.put("pokemon_egg_groups.csv", List.of("species_id", "egg_group_id"));
        headers.put("pokemon_moves.csv",
                List.of("pokemon_id", "move_id", "pokemon_move_method_id", "level", "version_group_id"));
        headers.put("pokemon_evolution.csv",
                List.of("evolved_species_id", "evolution_trigger_id", "minimum_level", "minimum_happiness",
                        "minimum_affection", "time_of_day", "held_item_id", "trigger_item_id", "known_move_id",
                        "known_move_type_id", "location_id", "party_species_id", "party_type_id", "trade_species_id",
                        "needs_overworld_rain", "turn_upside_down", "relative_physical_stats", "gender_id"));
        headers.put("move_meta_ailments.csv", List.of("id", "identifier"));
        headers.put("move_meta_categories.csv", List.of("id", "identifier"));
        return Map.copyOf(headers);
    }
}
