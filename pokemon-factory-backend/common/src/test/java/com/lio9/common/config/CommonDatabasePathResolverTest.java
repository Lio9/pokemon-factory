package com.lio9.common.config;



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