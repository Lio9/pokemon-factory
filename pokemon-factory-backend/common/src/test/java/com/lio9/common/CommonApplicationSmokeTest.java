package com.lio9.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = CommonApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CommonApplicationSmokeTest {

    private static final Path DB_PATH = createTempDatabase("common-smoke");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH.toAbsolutePath());
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 1);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registry.add("pokemon-factory.database.initialize-on-startup", () -> false);
        registry.add("pokemon-factory.database.import-csv-on-startup", () -> false);
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