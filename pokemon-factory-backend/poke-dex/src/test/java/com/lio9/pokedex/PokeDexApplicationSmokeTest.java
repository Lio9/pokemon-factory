package com.lio9.pokedex;



/**
 * PokeDexApplicationSmokeTest 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：测试文件。
 * 核心职责：负责验证目标模块的边界条件、回归行为与核心输出稳定性。
 * 阅读建议：建议先看测试名称，再看构造数据与断言意图。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = PokeDexApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PokeDexApplicationSmokeTest {

    private static final Path DB_PATH = createTempDatabase("pokedex-smoke");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH.toAbsolutePath());
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 1);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registry.add("spring.main.allow-bean-definition-overriding", () -> true);
        registry.add("pokemon-factory.database.initialize-on-startup", () -> false);
        registry.add("pokemon-factory.database.import-csv-on-startup", () -> false);
        registry.add("spring.cache.type", () -> "none");
    }

    @Test
    void contextLoads() {
    }

    private static Path createTempDatabase(String prefix) {
        try {
            Path tempFile = Files.createTempFile(prefix, ".db");
            tempFile.toFile().deleteOnExit();
            return tempFile;
        } catch (IOException ex) {
            throw new IllegalStateException("创建 smoke test 数据库文件失败", ex);
        }
    }
}