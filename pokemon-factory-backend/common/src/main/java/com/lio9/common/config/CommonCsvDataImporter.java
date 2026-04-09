package com.lio9.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class CommonCsvDataImporter {
    private static final Logger log = LoggerFactory.getLogger(CommonCsvDataImporter.class);
    private static final int BATCH_SIZE = 2000;

    private static final Map<Integer, String> LANGUAGE_MAP = Map.ofEntries(
            Map.entry(1, "ja"),
            Map.entry(4, "zh-hant"),
            Map.entry(5, "fr"),
            Map.entry(6, "de"),
            Map.entry(7, "es"),
            Map.entry(8, "it"),
            Map.entry(9, "en"),
            Map.entry(11, "ja"),
            Map.entry(12, "zh-hans")
    );

    private final CommonDatabaseProperties properties;

    public CommonCsvDataImporter(CommonDatabaseProperties properties) {
        this.properties = properties;
    }

    public void importIfNeeded(Connection connection) throws Exception {
        if (!properties.isImportCsvOnStartup()) {
            log.debug("当前进程未启用 CSV 导入，跳过数据灌库。");
            return;
        }
        if (!tableExists(connection, "pokemon_species") || count(connection, "pokemon_species") > 0) {
            log.info("检测到 pokemon_species 已有数据，跳过 CSV 导入。");
            return;
        }

        Path csvDirectory = resolveCsvDirectory();
        log.info("开始执行 CSV 数据导入，目录：{}", csvDirectory.toAbsolutePath().normalize());

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            importGenders(connection, csvDirectory);
            importGrowthRates(connection, csvDirectory);
            importEggGroups(connection, csvDirectory);
            importNatures(connection, csvDirectory);
            importMoveLearnMethods(connection, csvDirectory);

            importTypes(connection, csvDirectory);
            importTypeEfficacy(connection, csvDirectory);
            importAbilities(connection, csvDirectory);
            importMoves(connection, csvDirectory);
            importMoveMeta(connection, csvDirectory);
            importMoveFlags(connection, csvDirectory);
            importMoveStatChanges(connection, csvDirectory);
            importItems(connection, csvDirectory);
            syncEffectSeeds(connection);

            importEvolutionChains(connection, csvDirectory);
            Map<Integer, Integer> evolvesFromMap = importPokemonSpecies(connection, csvDirectory);
            importPokemonForms(connection, csvDirectory);
            importPokemonTypes(connection, csvDirectory);
            importPokemonAbilities(connection, csvDirectory);
            importPokemonStats(connection, csvDirectory);
            importPokemonEggGroups(connection, csvDirectory);
            importPokemonMoves(connection, csvDirectory);
            importPokemonEvolution(connection, csvDirectory, evolvesFromMap);

            connection.commit();
            log.info("CSV 数据导入完成。pokemon_species={}, move={}, item={}",
                    count(connection, "pokemon_species"), count(connection, "move"), count(connection, "item"));
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public void syncEffectSeeds(Connection connection) throws Exception {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (originalAutoCommit) {
            connection.setAutoCommit(false);
        }
        try {
            importEffectSeeds(connection);
            if (originalAutoCommit) {
                connection.commit();
            }
        } catch (Exception exception) {
            if (originalAutoCommit) {
                connection.rollback();
            }
            throw exception;
        } finally {
            if (originalAutoCommit) {
                connection.setAutoCommit(true);
            }
        }
    }

    private void importGenders(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO gender (id, name) VALUES (?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("genders.csv"))) {
                statement.setInt(1, requiredInt(record, "id"));
                statement.setString(2, record.get("identifier"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 gender：{} 条", count);
        }
    }

    private void importGrowthRates(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, CSVRecord> proseById = firstByIntKey(records(csvDirectory.resolve("growth_rate_prose.csv")), "growth_rate_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE growth_rate SET formula = ?, description = ? WHERE id = ?")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("growth_rates.csv"))) {
                int id = requiredInt(record, "id");
                CSVRecord prose = proseById.get(id);
                statement.setString(1, nullable(record, "formula"));
                statement.setString(2, prose == null ? null : nullable(prose, "description"));
                statement.setInt(3, id);
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 更新 growth_rate：{} 条", count);
        }
    }

    private void importEggGroups(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, CSVRecord> proseById = firstByIntKey(records(csvDirectory.resolve("egg_group_prose.csv")), "egg_group_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE egg_group SET name = ? WHERE id = ?")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("egg_groups.csv"))) {
                int id = requiredInt(record, "id");
                CSVRecord prose = proseById.get(id);
                statement.setString(1, prose == null ? null : nullable(prose, "name"));
                statement.setInt(2, id);
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 更新 egg_group：{} 条", count);
        }
    }

    private void importNatures(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("nature_names.csv")), "nature_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO nature (id, name, name_en, name_jp, increased_stat, decreased_stat, likes_flavor, hates_flavor) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("natures.csv"))) {
                int id = requiredInt(record, "id");
                List<CSVRecord> names = namesById.getOrDefault(id, List.of());
                statement.setInt(1, id);
                statement.setString(2, localizedName(names, List.of("zh-hans", "zh-hant", "en", "ja")));
                statement.setString(3, nullable(record, "identifier"));
                statement.setString(4, localizedName(names, List.of("ja", "en")));
                bindNullableInt(statement, 5, nullableInt(record, "increased_stat_id"));
                bindNullableInt(statement, 6, nullableInt(record, "decreased_stat_id"));
                statement.setString(7, flavorName(nullable(record, "likes_flavor_id")));
                statement.setString(8, flavorName(nullable(record, "hates_flavor_id")));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 nature：{} 条", count);
        }
    }

    private void importMoveLearnMethods(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> proseById = groupByIntKey(records(csvDirectory.resolve("pokemon_move_method_prose.csv")), "pokemon_move_method_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO move_learn_method (id, name, name_en, description) VALUES (?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_move_methods.csv"))) {
                int id = requiredInt(record, "id");
                List<CSVRecord> proses = proseById.getOrDefault(id, List.of());
                String name = localizedName(proses, List.of("zh-hans", "zh-hant", "en", "ja"));
                if (!StringUtils.hasText(name)) {
                    name = nullable(record, "identifier");
                }
                statement.setInt(1, id);
                statement.setString(2, name);
                statement.setString(3, nullable(record, "identifier"));
                statement.setString(4, localizedText(proses, List.of("zh-hans", "zh-hant", "en"), "description"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 move_learn_method：{} 条", count);
        }
    }

    private void importTypes(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("type_names.csv")), "type_id");
        try (PreparedStatement statement = connection.prepareStatement("UPDATE type SET name = ?, name_jp = ? WHERE id = ?")) {
            int count = 0;
            for (int typeId = 1; typeId <= 18; typeId++) {
                List<CSVRecord> names = namesById.getOrDefault(typeId, List.of());
                statement.setString(1, localizedName(names, List.of("zh-hans", "zh-hant", "en", "ja")));
                statement.setString(2, localizedName(names, List.of("ja", "en")));
                statement.setInt(3, typeId);
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 更新 type：{} 条", count);
        }
    }

    private void importTypeEfficacy(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO type_efficacy (attacking_type_id, defending_type_id, damage_factor) VALUES (?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("type_efficacy.csv"))) {
                statement.setInt(1, requiredInt(record, "damage_type_id"));
                statement.setInt(2, requiredInt(record, "target_type_id"));
                statement.setInt(3, requiredInt(record, "damage_factor"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 type_efficacy：{} 条", count);
        }
    }

    private void importAbilities(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("ability_names.csv")), "ability_id");
        Map<Integer, List<CSVRecord>> proseById = groupByIntKey(records(csvDirectory.resolve("ability_prose.csv")), "ability_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO ability (id, name, name_en, name_jp, description, generation_id, is_main_series) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("abilities.csv"))) {
                int id = requiredInt(record, "id");
                statement.setInt(1, id);
                statement.setString(2, localizedName(namesById.getOrDefault(id, List.of()), List.of("zh-hans", "zh-hant", "en", "ja")));
                statement.setString(3, nullable(record, "identifier"));
                statement.setString(4, localizedName(namesById.getOrDefault(id, List.of()), List.of("ja", "en")));
                statement.setString(5, localizedText(proseById.getOrDefault(id, List.of()), List.of("zh-hans", "zh-hant", "en"), "short_effect", "effect"));
                bindNullableInt(statement, 6, nullableInt(record, "generation_id"));
                statement.setInt(7, "1".equals(nullable(record, "is_main_series")) ? 1 : 0);
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 ability：{} 条", count);
        }
    }

    private void importMoves(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("move_names.csv")), "move_id");
        Map<Integer, CSVRecord> latestProseById = latestByKey(records(csvDirectory.resolve("move_flavor_text.csv")), "move_id", "version_group_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO move (id, name, name_en, name_jp, type_id, damage_class_id, target_id, power, pp, accuracy, priority, effect_chance, generation_id, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("moves.csv"))) {
                int id = requiredInt(record, "id");
                statement.setInt(1, id);
                statement.setString(2, localizedName(namesById.getOrDefault(id, List.of()), List.of("zh-hans", "zh-hant", "en", "ja")));
                statement.setString(3, nullable(record, "identifier"));
                statement.setString(4, localizedName(namesById.getOrDefault(id, List.of()), List.of("ja", "en")));
                bindNullableInt(statement, 5, nullableInt(record, "type_id"));
                bindNullableInt(statement, 6, nullableInt(record, "damage_class_id"));
                bindNullableInt(statement, 7, nullableInt(record, "target_id"));
                bindNullableInt(statement, 8, nullableInt(record, "power"));
                bindNullableInt(statement, 9, nullableInt(record, "pp"));
                bindNullableInt(statement, 10, nullableInt(record, "accuracy"));
                statement.setInt(11, nullableInt(record, "priority") == null ? 0 : Objects.requireNonNull(nullableInt(record, "priority")));
                bindNullableInt(statement, 12, nullableInt(record, "effect_chance"));
                bindNullableInt(statement, 13, nullableInt(record, "generation_id"));
                CSVRecord prose = latestProseById.get(id);
                statement.setString(14, prose == null ? null : nullable(prose, "flavor_text"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 move：{} 条", count);
        }
    }

    private void importMoveMeta(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO move_meta (move_id, min_hits, max_hits, min_turns, max_turns, drain, healing, crit_rate, ailment_chance, flinch_chance, stat_chance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("move_meta.csv"))) {
                statement.setInt(1, requiredInt(record, "move_id"));
                bindNullableInt(statement, 2, nullableInt(record, "min_hits"));
                bindNullableInt(statement, 3, nullableInt(record, "max_hits"));
                bindNullableInt(statement, 4, nullableInt(record, "min_turns"));
                bindNullableInt(statement, 5, nullableInt(record, "max_turns"));
                bindNullableInt(statement, 6, nullableInt(record, "drain"));
                bindNullableInt(statement, 7, nullableInt(record, "healing"));
                bindNullableInt(statement, 8, nullableInt(record, "crit_rate"));
                bindNullableInt(statement, 9, nullableInt(record, "ailment_chance"));
                bindNullableInt(statement, 10, nullableInt(record, "flinch_chance"));
                bindNullableInt(statement, 11, nullableInt(record, "stat_chance"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 move_meta：{} 条", count);
        }
    }

    private void importMoveFlags(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement flagStatement = connection.prepareStatement(
                "INSERT OR REPLACE INTO move_flags (id, identifier, name) VALUES (?, ?, ?)");
             PreparedStatement mapStatement = connection.prepareStatement(
                     "INSERT OR REPLACE INTO move_flag_map (move_id, flag_id) VALUES (?, ?)") ) {
            int flagCount = 0;
            for (CSVRecord record : records(csvDirectory.resolve("move_flags.csv"))) {
                flagStatement.setInt(1, requiredInt(record, "id"));
                flagStatement.setString(2, nullable(record, "identifier"));
                flagStatement.setString(3, nullable(record, "identifier"));
                flagStatement.addBatch();
                flagCount = flushBatch(flagStatement, flagCount + 1);
            }
            flushBatch(flagStatement, flagCount, true);

            int mapCount = 0;
            for (CSVRecord record : records(csvDirectory.resolve("move_flag_map.csv"))) {
                mapStatement.setInt(1, requiredInt(record, "move_id"));
                mapStatement.setInt(2, requiredInt(record, "move_flag_id"));
                mapStatement.addBatch();
                mapCount = flushBatch(mapStatement, mapCount + 1);
            }
            flushBatch(mapStatement, mapCount, true);
            log.info("CSV 导入 move_flags：{} 条，move_flag_map：{} 条", flagCount, mapCount);
        }
    }

    private void importMoveStatChanges(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO move_meta_stat_change (move_id, stat_id, \"change\") VALUES (?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("move_meta_stat_changes.csv"))) {
                statement.setInt(1, requiredInt(record, "move_id"));
                statement.setInt(2, requiredInt(record, "stat_id"));
                statement.setInt(3, requiredInt(record, "change"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 move_meta_stat_change：{} 条", count);
        }
    }

    private void importItems(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("item_names.csv")), "item_id");
        Map<Integer, CSVRecord> latestProseById = latestByKey(records(csvDirectory.resolve("item_flavor_text.csv")), "item_id", "version_group_id");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO item (id, name, name_en, name_jp, category_id, cost, fling_power, fling_effect_id, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("items.csv"))) {
                int id = requiredInt(record, "id");
                statement.setInt(1, id);
                String fallback = nullable(record, "identifier");
                statement.setString(2, Optional.ofNullable(localizedName(namesById.getOrDefault(id, List.of()), List.of("zh-hans", "zh-hant", "en", "ja"))).orElse(fallback));
                statement.setString(3, fallback);
                statement.setString(4, Optional.ofNullable(localizedName(namesById.getOrDefault(id, List.of()), List.of("ja", "en"))).orElse(fallback));
                bindNullableInt(statement, 5, nullableInt(record, "category_id"));
                statement.setInt(6, nullableInt(record, "cost") == null ? 0 : Objects.requireNonNull(nullableInt(record, "cost")));
                bindNullableInt(statement, 7, nullableInt(record, "fling_power"));
                bindNullableInt(statement, 8, nullableInt(record, "fling_effect_id"));
                CSVRecord prose = latestProseById.get(id);
                statement.setString(9, prose == null ? null : nullable(prose, "flavor_text"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 item：{} 条", count);
        }
    }

    private void importEffectSeeds(Connection connection) throws Exception {
        try (PreparedStatement deleteAbility = connection.prepareStatement("DELETE FROM ability_effect");
             PreparedStatement deleteItem = connection.prepareStatement("DELETE FROM item_effect");
             PreparedStatement abilityStatement = connection.prepareStatement(
                     "INSERT INTO ability_effect (ability_id, effect_type, effect_value, target, condition, description) VALUES (?, ?, ?, ?, ?, ?)");
             PreparedStatement itemStatement = connection.prepareStatement(
                     "INSERT INTO item_effect (item_id, effect_type, effect_value, target, condition, description) VALUES (?, ?, ?, ?, ?, ?)") ) {
            deleteAbility.executeUpdate();
            deleteItem.executeUpdate();

            Object[][] abilityEffects = {
                    {91, "stab_multiplier", "2.0", "attacker", "is_stab", "本系技能威力提升至2.0倍"},
                    {66, "damage_multiplier", "1.5", "attacker", "type_id=10 AND hp_percent<=33", "HP低于1/3时火系技能威力提升50%"},
                    {67, "damage_multiplier", "1.5", "attacker", "type_id=11 AND hp_percent<=33", "HP低于1/3时水系技能威力提升50%"},
                    {18, "damage_multiplier", "1.5", "attacker", "type_id=7 AND hp_percent<=33", "HP低于1/3时虫系技能威力提升50%"},
                    {65, "damage_multiplier", "1.5", "attacker", "type_id=12 AND hp_percent<=33", "HP低于1/3时草系技能威力提升50%"},
                    {152, "damage_multiplier", "1.3", "attacker", "is_contact", "接触技能威力提升30%"},
                    {232, "damage_multiplier", "1.2", "attacker", "is_punch", "拳类技能威力提升20%"},
                    {86, "damage_multiplier", "1.5", "attacker", "power<=60", "威力60以下技能提升50%"},
                    {62, "damage_multiplier", "0.5", "defender", "type_id IN (10, 14)", "火系和冰系伤害减半"},
                    {153, "damage_multiplier", "0.5", "defender", "hp_percent=100", "满HP时受到伤害减半"},
                    {216, "damage_multiplier", "0.5", "defender", "damage_class=physical", "物理伤害减半"},
                    {22, "stat_boost", "-1", "defender", "on_switch_in", "出场时降低对方攻击1级"},
                    {29, "status_immunity", "flinch", "always", "always", "免疫畏缩效果"},
                    {59, "weather_effect", null, "self", "always", "免疫天气效果"},
                    {31, "terrain_set", "electric", "self", "on_switch_in", "出场时设置电气场地"},
                    {229, "terrain_set", "grassy", "self", "on_switch_in", "出场时设置草地场地"},
                    {268, "terrain_set", "psychic", "self", "on_switch_in", "出场时设置超能力场地"},
                    {243, "terrain_set", "misty", "self", "on_switch_in", "出场时设置薄雾场地"}
            };
            int abilityCount = 0;
            for (Object[] row : abilityEffects) {
                bindSeedRow(abilityStatement, row);
                abilityStatement.addBatch();
                abilityCount = flushBatch(abilityStatement, abilityCount + 1);
            }
            flushBatch(abilityStatement, abilityCount, true);

            Object[][] itemEffects = {
                    {130, "damage_multiplier", "1.3", "attacker", "always", "所有技能威力提升30%"},
                    {327, "damage_multiplier", "1.5", "attacker", "damage_class=special", "特攻技能威力提升50%"},
                    {299, "damage_multiplier", "1.5", "attacker", "damage_class=physical", "物理技能威力提升50%"},
                    {83, "damage_multiplier", "1.2", "attacker", "type_id=10", "火系技能威力提升20%"},
                    {171, "damage_multiplier", "1.2", "attacker", "type_id=13", "电系技能威力提升20%"},
                    {91, "damage_multiplier", "1.2", "attacker", "type_id=9", "钢系技能威力提升20%"},
                    {85, "damage_multiplier", "1.2", "attacker", "type_id=12", "草系技能威力提升20%"},
                    {82, "damage_multiplier", "1.2", "attacker", "type_id=11", "水系技能威力提升20%"},
                    {239, "damage_multiplier", "1.2", "attacker", "type_id=17", "恶系技能威力提升20%"},
                    {267, "damage_multiplier", "1.2", "attacker", "type_id=14", "超能力系技能威力提升20%"},
                    {89, "damage_multiplier", "1.2", "attacker", "type_id=5", "地面系技能威力提升20%"},
                    {88, "damage_multiplier", "1.2", "attacker", "type_id=6", "岩石系技能威力提升20%"},
                    {87, "damage_multiplier", "1.2", "attacker", "type_id=15", "冰系技能威力提升20%"},
                    {84, "damage_multiplier", "1.2", "attacker", "type_id=4", "毒系技能威力提升20%"},
                    {90, "damage_multiplier", "1.2", "attacker", "type_id=3", "飞行系技能威力提升20%"},
                    {279, "damage_multiplier", "1.2", "attacker", "type_id=8", "岩石系技能威力提升20%"},
                    {305, "recoil", "1/6", "attacker", "is_contact", "接触技能受到反伤1/6"}
            };
            int itemCount = 0;
            for (Object[] row : itemEffects) {
                bindSeedRow(itemStatement, row);
                itemStatement.addBatch();
                itemCount = flushBatch(itemStatement, itemCount + 1);
            }
            flushBatch(itemStatement, itemCount, true);
            log.info("导入特性/道具效果种子数据：ability_effect={}，item_effect={}", abilityCount, itemCount);
        }
    }

    private void importEvolutionChains(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO evolution_chain (id, baby_trigger_item_id) VALUES (?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("evolution_chains.csv"))) {
                statement.setInt(1, requiredInt(record, "id"));
                bindNullableInt(statement, 2, nullableInt(record, "baby_trigger_item_id"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 evolution_chain：{} 条", count);
        }
    }

    private Map<Integer, Integer> importPokemonSpecies(Connection connection, Path csvDirectory) throws Exception {
        Map<Integer, List<CSVRecord>> namesById = groupByIntKey(records(csvDirectory.resolve("pokemon_species_names.csv")), "pokemon_species_id");
        Map<Integer, CSVRecord> latestProseById = latestByKey(records(csvDirectory.resolve("pokemon_species_flavor_text.csv")), "species_id", "version_id");
        Map<Integer, Integer> evolvesFromMap = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_species (id, name, name_en, name_jp, genus, generation_id, evolution_chain_id, evolves_from_species_id, gender_rate, capture_rate, base_happiness, hatch_counter, is_baby, is_legendary, is_mythical, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_species.csv"))) {
                int id = requiredInt(record, "id");
                List<CSVRecord> names = namesById.getOrDefault(id, List.of());
                CSVRecord prose = latestProseById.get(id);
                Integer evolvesFrom = nullableInt(record, "evolves_from_species_id");
                if (evolvesFrom != null) {
                    evolvesFromMap.put(id, evolvesFrom);
                }
                statement.setInt(1, id);
                statement.setString(2, localizedName(names, List.of("zh-hans", "zh-hant", "en", "ja")));
                statement.setString(3, nullable(record, "identifier"));
                statement.setString(4, localizedName(names, List.of("ja", "en")));
                statement.setString(5, localizedGenus(names, List.of("zh-hans", "zh-hant", "en", "ja")));
                bindNullableInt(statement, 6, nullableInt(record, "generation_id"));
                bindNullableInt(statement, 7, nullableInt(record, "evolution_chain_id"));
                bindNullableInt(statement, 8, evolvesFrom);
                statement.setInt(9, nullableInt(record, "gender_rate") == null ? -1 : Objects.requireNonNull(nullableInt(record, "gender_rate")));
                statement.setInt(10, nullableInt(record, "capture_rate") == null ? 0 : Objects.requireNonNull(nullableInt(record, "capture_rate")));
                statement.setInt(11, nullableInt(record, "base_happiness") == null ? 70 : Objects.requireNonNull(nullableInt(record, "base_happiness")));
                bindNullableInt(statement, 12, nullableInt(record, "hatch_counter"));
                statement.setInt(13, boolInt(record, "is_baby"));
                statement.setInt(14, boolInt(record, "is_legendary"));
                statement.setInt(15, boolInt(record, "is_mythical"));
                statement.setString(16, prose == null ? null : nullable(prose, "flavor_text"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_species：{} 条", count);
        }
        return evolvesFromMap;
    }

    private void importPokemonForms(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_form (id, species_id, form_name, is_default, is_battle_only, height, weight, base_experience, \"order\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon.csv"))) {
                statement.setInt(1, requiredInt(record, "id"));
                statement.setInt(2, requiredInt(record, "species_id"));
                statement.setString(3, nullable(record, "identifier"));
                statement.setInt(4, boolInt(record, "is_default"));
                statement.setInt(5, 0);
                bindNullableDouble(statement, 6, scaledDecimal(record, "height", 10.0d));
                bindNullableDouble(statement, 7, scaledDecimal(record, "weight", 10.0d));
                bindNullableInt(statement, 8, nullableInt(record, "base_experience"));
                bindNullableInt(statement, 9, nullableInt(record, "order"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_form：{} 条", count);
        }
    }

    private void importPokemonTypes(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_form_type (form_id, type_id, slot) VALUES (?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_types.csv"))) {
                statement.setInt(1, requiredInt(record, "pokemon_id"));
                statement.setInt(2, requiredInt(record, "type_id"));
                statement.setInt(3, requiredInt(record, "slot"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_form_type：{} 条", count);
        }
    }

    private void importPokemonAbilities(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_form_ability (form_id, ability_id, is_hidden, slot) VALUES (?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_abilities.csv"))) {
                statement.setInt(1, requiredInt(record, "pokemon_id"));
                statement.setInt(2, requiredInt(record, "ability_id"));
                statement.setInt(3, boolInt(record, "is_hidden"));
                statement.setInt(4, requiredInt(record, "slot"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_form_ability：{} 条", count);
        }
    }

    private void importPokemonStats(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_form_stat (form_id, stat_id, base_stat, effort) VALUES (?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_stats.csv"))) {
                statement.setInt(1, requiredInt(record, "pokemon_id"));
                statement.setInt(2, requiredInt(record, "stat_id"));
                statement.setInt(3, requiredInt(record, "base_stat"));
                statement.setInt(4, nullableInt(record, "effort") == null ? 0 : Objects.requireNonNull(nullableInt(record, "effort")));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_form_stat：{} 条", count);
        }
    }

    private void importPokemonEggGroups(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_species_egg_group (species_id, egg_group_id) VALUES (?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_egg_groups.csv"))) {
                statement.setInt(1, requiredInt(record, "species_id"));
                statement.setInt(2, requiredInt(record, "egg_group_id"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_species_egg_group：{} 条", count);
        }
    }

    private void importPokemonMoves(Connection connection, Path csvDirectory) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_form_move (form_id, move_id, learn_method_id, level, version_group_id) VALUES (?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_moves.csv"))) {
                statement.setInt(1, requiredInt(record, "pokemon_id"));
                statement.setInt(2, requiredInt(record, "move_id"));
                statement.setInt(3, requiredInt(record, "pokemon_move_method_id"));
                bindNullableInt(statement, 4, nullableInt(record, "level"));
                bindNullableInt(statement, 5, nullableInt(record, "version_group_id"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_form_move：{} 条", count);
        }
    }

    private void importPokemonEvolution(Connection connection, Path csvDirectory, Map<Integer, Integer> evolvesFromMap) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO pokemon_evolution (evolved_species_id, evolves_from_species_id, evolution_trigger_id, min_level, min_happiness, min_affection, time_of_day, held_item_id, evolution_item_id, known_move_id, known_move_type_id, location_id, party_species_id, party_type_id, trade_species_id, needs_overworld_rain, turn_upside_down, relative_physical_stats, gender_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int count = 0;
            for (CSVRecord record : records(csvDirectory.resolve("pokemon_evolution.csv"))) {
                int evolvedSpeciesId = requiredInt(record, "evolved_species_id");
                statement.setInt(1, evolvedSpeciesId);
                bindNullableInt(statement, 2, evolvesFromMap.get(evolvedSpeciesId));
                statement.setInt(3, requiredInt(record, "evolution_trigger_id"));
                bindNullableInt(statement, 4, nullableInt(record, "minimum_level"));
                bindNullableInt(statement, 5, nullableInt(record, "minimum_happiness"));
                bindNullableInt(statement, 6, nullableInt(record, "minimum_affection"));
                statement.setString(7, nullable(record, "time_of_day"));
                bindNullableInt(statement, 8, nullableInt(record, "held_item_id"));
                bindNullableInt(statement, 9, nullableInt(record, "trigger_item_id"));
                bindNullableInt(statement, 10, nullableInt(record, "known_move_id"));
                bindNullableInt(statement, 11, nullableInt(record, "known_move_type_id"));
                bindNullableInt(statement, 12, nullableInt(record, "location_id"));
                bindNullableInt(statement, 13, nullableInt(record, "party_species_id"));
                bindNullableInt(statement, 14, nullableInt(record, "party_type_id"));
                bindNullableInt(statement, 15, nullableInt(record, "trade_species_id"));
                statement.setInt(16, boolInt(record, "needs_overworld_rain"));
                statement.setInt(17, boolInt(record, "turn_upside_down"));
                bindNullableInt(statement, 18, nullableInt(record, "relative_physical_stats"));
                bindNullableInt(statement, 19, nullableInt(record, "gender_id"));
                statement.addBatch();
                count = flushBatch(statement, count + 1);
            }
            flushBatch(statement, count, true);
            log.info("CSV 导入 pokemon_evolution：{} 条", count);
        }
    }

    private Path resolveCsvDirectory() {
        if (StringUtils.hasText(properties.getCsvDirectory())) {
            Path configured = Paths.get(properties.getCsvDirectory());
            if (!configured.isAbsolute()) {
                configured = Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
            }
            if (Files.isDirectory(configured)) {
                return configured;
            }
        }

        List<Path> candidates = new ArrayList<>();
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            candidates.add(cursor.resolve("csv"));
            candidates.add(cursor.resolve("..") .normalize().resolve("csv"));
        }
        return candidates.stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到 CSV 数据目录，请设置 CSV_DIR 或 pokemon-factory.database.csv-directory"));
    }

    private List<CSVRecord> records(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            throw new IllegalStateException("缺少 CSV 文件: " + csvFile.toAbsolutePath().normalize());
        }
        List<List<String>> rows = parseCsvRows(csvFile);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> headers = rows.get(0);
        List<CSVRecord> records = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            Map<String, String> values = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String value = columnIndex < row.size() ? row.get(columnIndex) : "";
                values.put(headers.get(columnIndex), value);
            }
            records.add(new CSVRecord(values));
        }
        return records;
    }

    private List<List<String>> parseCsvRows(Path csvFile) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (PushbackReader reader = new PushbackReader(Files.newBufferedReader(csvFile, StandardCharsets.UTF_8), 2)) {
            List<String> currentRow = new ArrayList<>();
            StringBuilder currentField = new StringBuilder();
            boolean inQuotes = false;
            int read;
            while ((read = reader.read()) != -1) {
                char current = (char) read;
                if (current == '"') {
                    if (inQuotes) {
                        int next = reader.read();
                        if (next == '"') {
                            currentField.append('"');
                        } else {
                            inQuotes = false;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else {
                        inQuotes = true;
                    }
                    continue;
                }
                if (!inQuotes && current == ',') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    continue;
                }
                if (!inQuotes && (current == '\n' || current == '\r')) {
                    if (current == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next != -1) {
                            reader.unread(next);
                        }
                    }
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                    continue;
                }
                currentField.append(current);
            }
            if (currentField.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(currentField.toString());
                rows.add(currentRow);
            }
        }
        return rows;
    }

    private Map<Integer, CSVRecord> firstByIntKey(List<CSVRecord> records, String key) {
        Map<Integer, CSVRecord> grouped = new HashMap<>();
        for (CSVRecord record : records) {
            grouped.putIfAbsent(requiredInt(record, key), record);
        }
        return grouped;
    }

    private Map<Integer, List<CSVRecord>> groupByIntKey(List<CSVRecord> records, String key) {
        Map<Integer, List<CSVRecord>> grouped = new HashMap<>();
        for (CSVRecord record : records) {
            grouped.computeIfAbsent(requiredInt(record, key), ignored -> new ArrayList<>()).add(record);
        }
        return grouped;
    }

    private Map<Integer, CSVRecord> latestByKey(List<CSVRecord> records, String keyField, String orderField) {
        Map<Integer, CSVRecord> latest = new HashMap<>();
        Map<Integer, Integer> order = new HashMap<>();
        for (CSVRecord record : records) {
            int key = requiredInt(record, keyField);
            int currentOrder = nullableInt(record, orderField) == null ? 0 : Objects.requireNonNull(nullableInt(record, orderField));
            if (!order.containsKey(key) || currentOrder > order.get(key)) {
                order.put(key, currentOrder);
                latest.put(key, record);
            }
        }
        return latest;
    }

    private String localizedName(List<CSVRecord> records, List<String> preferredLanguages) {
        for (String language : preferredLanguages) {
            for (CSVRecord record : records) {
                if (language.equals(resolveLanguage(record)) && StringUtils.hasText(nullable(record, "name"))) {
                    return nullable(record, "name");
                }
            }
        }
        return records.isEmpty() ? null : nullable(records.get(0), "name");
    }

    private String localizedGenus(List<CSVRecord> records, List<String> preferredLanguages) {
        for (String language : preferredLanguages) {
            for (CSVRecord record : records) {
                if (language.equals(resolveLanguage(record)) && StringUtils.hasText(nullable(record, "genus"))) {
                    return nullable(record, "genus");
                }
            }
        }
        return records.isEmpty() ? null : nullable(records.get(0), "genus");
    }

    private String localizedText(List<CSVRecord> records, List<String> preferredLanguages, String... candidateFields) {
        for (String language : preferredLanguages) {
            for (CSVRecord record : records) {
                if (!language.equals(resolveLanguage(record))) {
                    continue;
                }
                for (String field : candidateFields) {
                    String value = nullable(record, field);
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        for (CSVRecord record : records) {
            for (String field : candidateFields) {
                String value = nullable(record, field);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String resolveLanguage(CSVRecord record) {
        Integer id = nullableInt(record, "local_language_id");
        return id == null ? null : LANGUAGE_MAP.get(id);
    }

    private Integer nullableInt(CSVRecord record, String key) {
        String value = nullable(record, key);
        return StringUtils.hasText(value) ? Integer.parseInt(value) : null;
    }

    private Double scaledDecimal(CSVRecord record, String key, double divisor) {
        String value = nullable(record, key);
        return StringUtils.hasText(value) ? Double.parseDouble(value) / divisor : null;
    }

    private int requiredInt(CSVRecord record, String key) {
        return Integer.parseInt(Objects.requireNonNull(nullable(record, key), "missing field: " + key));
    }

    private String nullable(CSVRecord record, String key) {
        if (record == null || !record.isMapped(key)) {
            return null;
        }
        String value = record.get(key);
        return StringUtils.hasText(value) ? value : null;
    }

    private int boolInt(CSVRecord record, String key) {
        return "1".equals(nullable(record, key)) ? 1 : 0;
    }

    private String flavorName(String flavorId) {
        return switch (StringUtils.hasText(flavorId) ? flavorId : "") {
            case "1" -> "辣";
            case "2" -> "酸";
            case "3" -> "咸";
            case "4" -> "苦";
            case "5" -> "甜";
            default -> null;
        };
    }

    private int flushBatch(PreparedStatement statement, int count) throws SQLException {
        return flushBatch(statement, count, false);
    }

    private int flushBatch(PreparedStatement statement, int count, boolean force) throws SQLException {
        if (force || count % BATCH_SIZE == 0) {
            statement.executeBatch();
        }
        return count;
    }

    private void bindNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private void bindNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.REAL);
            return;
        }
        statement.setDouble(index, value);
    }

    private void bindSeedRow(PreparedStatement statement, Object[] values) throws SQLException {
        statement.setInt(1, (Integer) values[0]);
        statement.setString(2, (String) values[1]);
        statement.setString(3, (String) values[2]);
        statement.setString(4, (String) values[3]);
        statement.setString(5, (String) values[4]);
        statement.setString(6, (String) values[5]);
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private int count(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static final class CSVRecord {
        private final Map<String, String> values;

        private CSVRecord(Map<String, String> values) {
            this.values = values;
        }

        private boolean isMapped(String key) {
            return values.containsKey(key);
        }

        private String get(String key) {
            return values.get(key);
        }
    }
}