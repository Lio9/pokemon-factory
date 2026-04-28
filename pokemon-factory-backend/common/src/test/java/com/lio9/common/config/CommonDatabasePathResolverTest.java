package com.lio9.common.config;



/**
 * CommonDatabasePathResolverTest 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonDatabasePathResolverTest {

    private final CommonDatabasePathResolver resolver = new CommonDatabasePathResolver();

    @TempDir
    Path tempDir;

    @Test
    void resolveJdbcUrl_usesSharedDatabaseWhenRunningFromBackendRoot() throws IOException {
        Path backendRoot = createBackendLayout(tempDir);

        String jdbcUrl = resolver.resolveJdbcUrl(null, null, backendRoot);

        assertEquals("jdbc:sqlite:" + backendRoot.resolve("pokemon-factory.db").toAbsolutePath().normalize(), jdbcUrl);
    }

    @Test
    void resolveJdbcUrl_usesSharedDatabaseWhenRunningFromModuleDirectory() throws IOException {
        Path backendRoot = createBackendLayout(tempDir);
        Path moduleRoot = Files.createDirectories(backendRoot.resolve("battleFactory"));

        String jdbcUrl = resolver.resolveJdbcUrl(null, null, moduleRoot);

        assertEquals("jdbc:sqlite:" + backendRoot.resolve("pokemon-factory.db").toAbsolutePath().normalize(), jdbcUrl);
    }

    @Test
    void resolveJdbcUrl_repairsBrokenDuplicatedBackendPath() throws IOException {
        Path backendRoot = createBackendLayout(tempDir);
        Path brokenPath = backendRoot.resolve("pokemon-factory-backend").resolve("pokemon-factory.db");

        String jdbcUrl = resolver.resolveJdbcUrl("jdbc:sqlite:" + brokenPath, null, backendRoot);

        assertEquals("jdbc:sqlite:" + backendRoot.resolve("pokemon-factory.db").toAbsolutePath().normalize(), jdbcUrl);
    }

    private Path createBackendLayout(Path root) throws IOException {
        Path repoRoot = Files.createDirectories(root.resolve("repo"));
        Path backendRoot = Files.createDirectories(repoRoot.resolve("pokemon-factory-backend"));
        Files.writeString(backendRoot.resolve("pokemon-factory.db"), "test");
        return backendRoot;
    }
}