package com.lio9.common.config;



/**
 * CommonCsvDataImporterTest 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonCsvDataImporterTest {

    @TempDir
    Path tempDir;

    @Test
    void importIfNeeded_downloadsRemoteDatasetAndImportsSuccessfully() throws Exception {
        Map<String, String> dataset = createValidDataset();
        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("success.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            importer.importIfNeeded(connection);

            assertEquals(1, count(connection, "pokemon_species"));
            assertEquals(1, count(connection, "move"));
            assertEquals(18, count(connection, "item"));
            assertEquals(1, count(connection, "pokemon_form"));
            assertEquals(1, count(connection, "pokemon_form_type"));
            assertEquals(1, count(connection, "pokemon_form_ability"));
            assertEquals(1, count(connection, "pokemon_form_stat"));
            assertEquals(1, count(connection, "pokemon_form_move"));
            assertEquals(1, count(connection, "pokemon_evolution"));
            assertEquals(1, count(connection, "type_efficacy"));
            assertEquals(1, count(connection, "move_meta"));
            assertEquals(1, count(connection, "move_meta_ailment"));
            assertEquals(1, count(connection, "move_meta_category"));
            assertEquals(1, count(connection, "move_flag_map"));
            assertEquals(1, count(connection, "pokemon_species_egg_group"));
            assertTrue(count(connection, "type") >= 18);
            assertEquals(0, foreignKeyViolations(connection));

            Path cacheDir = tempDir.resolve("csv-cache");
            assertTrue(Files.exists(cacheDir.resolve("moves.csv")));
            assertTrue(Files.exists(cacheDir.resolve("pokemon_species.csv")));
        }
    }

    @Test
    void importIfNeeded_ignoresLocalCsvDirectoryAndUsesRemoteSource() throws Exception {
        Map<String, String> dataset = createValidDataset();
        Path localCsvDir = Files.createDirectories(tempDir.resolve("local-csv"));
        Files.writeString(localCsvDir.resolve("moves.csv"), csv(
                "id,identifier,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance,generation_id",
                "1,broken-local,2,1,40,35,100,0,,1"
        ));

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("ignore-local.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl(), localCsvDir);

            importer.importIfNeeded(connection);

            assertEquals("pound", queryString(connection, "SELECT name_en FROM move WHERE id = 1"));
        }
    }

    @Test
    void importIfNeeded_failsWhenRequiredHeaderMissing() throws Exception {
        Map<String, String> dataset = createValidDataset();
        dataset.put("moves.csv", csv(
                "id,identifier,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance,generation_id",
                "1,pound,2,1,40,35,100,0,,1"
        ));

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("missing-header.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> importer.importIfNeeded(connection));

            assertTrue(exception.getMessage().contains("moves.csv"));
            assertTrue(exception.getMessage().contains("type_id"));
        }
    }

    @Test
    void importIfNeeded_failsWhenRequiredRemoteFileMissing() throws Exception {
        Map<String, String> dataset = createValidDataset();
        dataset.remove("abilities.csv");

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("missing-file.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> importer.importIfNeeded(connection));

            assertTrue(exception.getMessage().contains("abilities.csv"));
        }
    }

    @Test
    void importIfNeeded_importsMoveTargetsAndEvolutionTriggersFromRemoteSource() throws Exception {
        Map<String, String> dataset = createValidDataset();
        dataset.put("move_targets.csv", csv(
                "id,identifier",
                "1,specific-move",
                "16,fainting-pokemon"
        ));
        dataset.put("move_target_prose.csv", csv(
                "move_target_id,local_language_id,name,description",
                "1,12,单体,指定单目标",
                "1,9,Specific move,One specific move.",
                "16,12,濒死宝可梦,对濒死的宝可梦使用",
                "16,9,Fainting Pokémon,Targets a fainting Pokémon."
        ));
        dataset.put("moves.csv", csv(
                "id,identifier,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance,generation_id",
                "1,pound,1,2,16,40,35,100,0,,1"
        ));
        dataset.put("evolution_triggers.csv", csv(
                "id,identifier",
                "1,level-up",
                "16,gimmighoul-coins"
        ));
        dataset.put("evolution_trigger_prose.csv", csv(
                "evolution_trigger_id,local_language_id,name",
                "1,12,升级",
                "1,9,Level up",
                "16,12,索财灵的硬币",
                "16,9,Gimmighoul coins"
        ));
        dataset.put("pokemon_evolution.csv", csv(
                "evolved_species_id,evolution_trigger_id,minimum_level,minimum_happiness,minimum_affection,time_of_day,held_item_id,trigger_item_id,known_move_id,known_move_type_id,location_id,party_species_id,party_type_id,trade_species_id,needs_overworld_rain,turn_upside_down,relative_physical_stats,gender_id",
                "1,16,,,,,,,,,,,,,0,0,,"
        ));

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("remote-dictionaries.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            importer.importIfNeeded(connection);

            assertEquals("fainting-pokemon", queryString(connection, "SELECT name_en FROM move_target WHERE id = 16"));
            assertEquals("濒死宝可梦", queryString(connection, "SELECT name FROM move_target WHERE id = 16"));
            assertEquals("对濒死的宝可梦使用", queryString(connection, "SELECT description FROM move_target WHERE id = 16"));
            assertEquals("单体", queryString(connection, "SELECT name FROM move_target WHERE id = 1"));
            assertEquals("selected-pokemon", queryString(connection, "SELECT name_en FROM move_target WHERE id = 1"));
            assertEquals("指定单目标", queryString(connection, "SELECT description FROM move_target WHERE id = 1"));
            assertEquals("gimmighoul-coins", queryString(connection, "SELECT name_en FROM evolution_trigger WHERE id = 16"));
            assertEquals("索财灵的硬币", queryString(connection, "SELECT name FROM evolution_trigger WHERE id = 16"));
            assertEquals("升级", queryString(connection, "SELECT name FROM evolution_trigger WHERE id = 1"));
            assertEquals(0, foreignKeyViolations(connection));
        }
    }

    @Test
    void importIfNeeded_importsExtendedTypeIdsFromRemoteSource() throws Exception {
        Map<String, String> dataset = createValidDataset();
        dataset.put("type_names.csv", csv(
                "type_id,local_language_id,name",
                "1,9,type-1",
                "2,9,type-2",
                "3,9,type-3",
                "4,9,type-4",
                "5,9,type-5",
                "6,9,type-6",
                "7,9,type-7",
                "8,9,type-8",
                "9,9,type-9",
                "10,9,type-10",
                "11,9,type-11",
                "12,9,type-12",
                "13,9,type-13",
                "14,9,type-14",
                "15,9,type-15",
                "16,9,type-16",
                "17,9,type-17",
                "18,9,type-18",
                "10002,9,Shadow",
                "10002,12,暗",
                "10002,11,ダーク"
        ));
        dataset.put("moves.csv", csv(
                "id,identifier,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance,generation_id",
                "1,shadow-rush,10002,2,1,40,35,100,0,,1"
        ));

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("extended-type.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            importer.importIfNeeded(connection);

            assertEquals("Shadow", queryString(connection, "SELECT name_en FROM type WHERE id = 10002"));
            assertEquals(0, foreignKeyViolations(connection));
        }
    }

    @Test
    void importIfNeeded_prefersSupportedLanguageIdFlavorTextInsteadOfLatestForeignText() throws Exception {
        Map<String, String> dataset = createValidDataset();
        dataset.put("move_flavor_text.csv", csv(
                "move_id,version_group_id,language_id,flavor_text",
                "1,1,12,招式描述-中文",
                "1,2,5,description-fr"
        ));

        try (RemoteCsvServer server = new RemoteCsvServer(dataset);
             Connection connection = openInitializedConnection(tempDir.resolve("language-id.db"))) {
            CommonCsvDataImporter importer = createImporter(server.baseUrl());

            importer.importIfNeeded(connection);

            assertEquals("招式描述-中文", queryString(connection, "SELECT description FROM move WHERE id = 1"));
        }
    }

    private CommonCsvDataImporter createImporter(String baseUrl) {
        return createImporter(baseUrl, null);
    }

    private CommonCsvDataImporter createImporter(String baseUrl, Path localCsvDirectory) {
        CommonDatabaseProperties properties = new CommonDatabaseProperties();
        properties.setImportCsvOnStartup(true);
        properties.setRemoteCsvBaseUrl(baseUrl);
        properties.setCsvCacheDirectory(tempDir.resolve("csv-cache").toString());
        if (localCsvDirectory != null) {
            properties.setCsvDirectory(localCsvDirectory.toString());
        }
        return new CommonCsvDataImporter(properties);
    }

    private Connection openInitializedConnection(Path dbPath) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath().normalize());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/init/001_core_schema.sql"));
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private int count(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private int foreignKeyViolations(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check")) {
            int violations = 0;
            while (resultSet.next()) {
                violations++;
            }
            return violations;
        }
    }

    private String queryString(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private Map<String, String> createValidDataset() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("genders.csv", csv("id,identifier", "1,male"));
        files.put("growth_rate_prose.csv", csv("growth_rate_id,local_language_id,name", "1,9,fast"));
        files.put("growth_rates.csv", csv("id,identifier,formula", "1,fast,n^3"));
        files.put("egg_group_prose.csv", csv("egg_group_id,local_language_id,name", "1,9,monster"));
        files.put("egg_groups.csv", csv("id,identifier", "1,monster"));
        files.put("nature_names.csv", csv("nature_id,local_language_id,name", "1,9,hardy"));
        files.put("natures.csv", csv("id,identifier,increased_stat_id,decreased_stat_id,likes_flavor_id,hates_flavor_id", "1,hardy,,,,"));
        files.put("pokemon_move_method_prose.csv", csv("pokemon_move_method_id,local_language_id,name,description", "1,9,level-up,learn by level"));
        files.put("pokemon_move_methods.csv", csv("id,identifier", "1,level-up"));
        files.put("evolution_triggers.csv", csv("id,identifier", "1,level-up"));
        files.put("evolution_trigger_prose.csv", csv("evolution_trigger_id,local_language_id,name", "1,12,升级", "1,9,Level up"));
        files.put("move_targets.csv", csv("id,identifier", "1,specific-move"));
        files.put("move_target_prose.csv", csv("move_target_id,local_language_id,name,description", "1,12,单体,指定单目标", "1,9,Specific move,One specific move."));
        files.put("move_meta_ailments.csv", csv("id,identifier", "1,burn"));
        files.put("move_meta_categories.csv", csv("id,identifier", "1,damage+ailment"));
        files.put("type_names.csv", typeNamesCsv());
        files.put("type_efficacy.csv", csv("damage_type_id,target_type_id,damage_factor", "1,2,100"));
        files.put("ability_names.csv", abilityNamesCsv());
        files.put("ability_prose.csv", abilityProseCsv());
        files.put("abilities.csv", abilitiesCsv());
        files.put("move_names.csv", csv("move_id,local_language_id,name", "1,9,pound"));
        files.put("move_flavor_text.csv", csv("move_id,version_group_id,language_id,flavor_text", "1,1,9,A physical attack delivered with a long tail."));
        files.put("moves.csv", csv("id,identifier,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance,generation_id", "1,pound,1,2,1,40,35,100,0,,1"));
        files.put("move_meta.csv", csv("move_id,min_hits,max_hits,min_turns,max_turns,drain,healing,crit_rate,ailment_id,category_id,ailment_chance,flinch_chance,stat_chance", "1,,,,,,,,1,1,,,")); 
        files.put("move_flags.csv", csv("id,identifier", "1,contact"));
        files.put("move_flag_map.csv", csv("move_id,move_flag_id", "1,1"));
        files.put("move_meta_stat_changes.csv", csv("move_id,stat_id,change", "1,1,1"));
        files.put("version_groups.csv", csv("id,identifier,generation_id,order", "1,red-blue,1,1"));
        files.put("item_pockets.csv", csv("id,identifier", "1,misc"));
        files.put("item_categories.csv", csv("id,pocket_id,identifier", "1,1,special-items"));
        files.put("item_fling_effects.csv", csv("id,identifier", "1,fling-effect"));
        files.put("item_names.csv", itemNamesCsv());
        files.put("item_flavor_text.csv", itemFlavorTextCsv());
        files.put("items.csv", itemsCsv());
        files.put("evolution_chains.csv", csv("id,baby_trigger_item_id", "1,"));
        files.put("pokemon_species_names.csv", csv("pokemon_species_id,local_language_id,name,genus", "1,9,bulbasaur,Seed Pokémon"));
        files.put("pokemon_species_flavor_text.csv", csv("species_id,version_id,language_id,flavor_text", "1,1,9,A strange seed was planted on its back at birth."));
        files.put("pokemon_species.csv", csv("id,identifier,generation_id,evolution_chain_id,evolves_from_species_id,gender_rate,capture_rate,base_happiness,hatch_counter,is_baby,is_legendary,is_mythical", "1,bulbasaur,1,1,,1,45,70,20,0,0,0"));
        files.put("pokemon.csv", csv("id,species_id,identifier,is_default,height,weight,base_experience,order", "1,1,bulbasaur,1,7,69,64,1"));
        files.put("pokemon_types.csv", csv("pokemon_id,type_id,slot", "1,1,1"));
        files.put("pokemon_abilities.csv", csv("pokemon_id,ability_id,is_hidden,slot", "1,1,0,1"));
        files.put("pokemon_stats.csv", csv("pokemon_id,stat_id,base_stat,effort", "1,1,45,0"));
        files.put("pokemon_egg_groups.csv", csv("species_id,egg_group_id", "1,1"));
        files.put("pokemon_moves.csv", csv("pokemon_id,move_id,pokemon_move_method_id,level,version_group_id", "1,1,1,1,1"));
        files.put("pokemon_evolution.csv", csv("evolved_species_id,evolution_trigger_id,minimum_level,minimum_happiness,minimum_affection,time_of_day,held_item_id,trigger_item_id,known_move_id,known_move_type_id,location_id,party_species_id,party_type_id,trade_species_id,needs_overworld_rain,turn_upside_down,relative_physical_stats,gender_id", "1,1,,,,,,,,,,,,,0,0,,"));
        return files;
    }

    private String typeNamesCsv() {
        StringBuilder builder = new StringBuilder("type_id,local_language_id,name\n");
        for (int typeId = 1; typeId <= 18; typeId++) {
            builder.append(typeId).append(",9,type-").append(typeId).append('\n');
        }
        return builder.toString();
    }

    private String abilitiesCsv() {
        StringBuilder builder = new StringBuilder("id,identifier,generation_id,is_main_series\n");
        for (int abilityId : List.of(1, 18, 22, 29, 31, 59, 62, 65, 66, 67, 86, 91, 152, 153, 216, 229, 232, 243, 268)) {
            builder.append(abilityId).append(",ability-").append(abilityId).append(",1,1\n");
        }
        return builder.toString();
    }

    private String abilityNamesCsv() {
        StringBuilder builder = new StringBuilder("ability_id,local_language_id,name\n");
        for (int abilityId : List.of(1, 18, 22, 29, 31, 59, 62, 65, 66, 67, 86, 91, 152, 153, 216, 229, 232, 243, 268)) {
            builder.append(abilityId).append(",9,ability-").append(abilityId).append('\n');
        }
        return builder.toString();
    }

    private String abilityProseCsv() {
        StringBuilder builder = new StringBuilder("ability_id,local_language_id,short_effect,effect\n");
        for (int abilityId : List.of(1, 18, 22, 29, 31, 59, 62, 65, 66, 67, 86, 91, 152, 153, 216, 229, 232, 243, 268)) {
            builder.append(abilityId).append(",9,short-").append(abilityId).append(",effect-").append(abilityId).append('\n');
        }
        return builder.toString();
    }

    private String itemsCsv() {
        StringBuilder builder = new StringBuilder("id,identifier,category_id,cost,fling_power,fling_effect_id\n");
        for (int itemId : List.of(1, 82, 83, 84, 85, 87, 88, 89, 90, 91, 130, 171, 239, 267, 279, 299, 305, 327)) {
            builder.append(itemId).append(",item-").append(itemId).append(",1,0,,1\n");
        }
        return builder.toString();
    }

    private String itemNamesCsv() {
        StringBuilder builder = new StringBuilder("item_id,local_language_id,name\n");
        for (int itemId : List.of(1, 82, 83, 84, 85, 87, 88, 89, 90, 91, 130, 171, 239, 267, 279, 299, 305, 327)) {
            builder.append(itemId).append(",9,item-").append(itemId).append('\n');
        }
        return builder.toString();
    }

    private String itemFlavorTextCsv() {
        StringBuilder builder = new StringBuilder("item_id,version_group_id,language_id,flavor_text\n");
        for (int itemId : List.of(1, 82, 83, 84, 85, 87, 88, 89, 90, 91, 130, 171, 239, 267, 279, 299, 305, 327)) {
            builder.append(itemId).append(",1,9,flavor-").append(itemId).append('\n');
        }
        return builder.toString();
    }

    private static String csv(String header, String... rows) {
        StringBuilder builder = new StringBuilder(header).append('\n');
        for (String row : rows) {
            builder.append(row).append('\n');
        }
        return builder.toString();
    }

    private static final class RemoteCsvServer implements AutoCloseable {
        private final HttpServer server;
        private final Map<String, byte[]> files;

        private RemoteCsvServer(Map<String, String> files) throws IOException {
            this.files = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                this.files.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String fileName = path.startsWith("/") ? path.substring(1) : path;
            byte[] body = files.get(fileName);
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
