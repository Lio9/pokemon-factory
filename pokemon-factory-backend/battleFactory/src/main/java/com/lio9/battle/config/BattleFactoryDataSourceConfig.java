package com.lio9.battle.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * battleFactory 本地显式声明 SQLite 数据源，避免依赖方因为模块化构建或工作目录不同而指向错误库文件。
 */
@Configuration
public class BattleFactoryDataSourceConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(Environment environment) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name", "org.sqlite.JDBC"));
        dataSource.setJdbcUrl(resolveJdbcUrl(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("SQLITE_DB_PATH")
        ));
        return dataSource;
    }

    private String resolveJdbcUrl(String configuredJdbcUrl, String configuredDbPath) {
        Path workingDirectory = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path resolvedPath = resolvePath(configuredJdbcUrl, configuredDbPath, workingDirectory);
        return "jdbc:sqlite:" + resolvedPath.toAbsolutePath().normalize();
    }

    private Path resolvePath(String configuredJdbcUrl, String configuredDbPath, Path workingDirectory) {
        Path explicitPath = parseConfiguredPath(configuredJdbcUrl, configuredDbPath, workingDirectory);
        if (explicitPath != null) {
            if (Files.exists(explicitPath) || parentDirectoryExists(explicitPath)) {
                return explicitPath;
            }
            Path discoveredExistingPath = discoverExistingDatabase(workingDirectory);
            if (discoveredExistingPath != null) {
                return discoveredExistingPath;
            }
            return explicitPath;
        }

        Path discoveredExistingPath = discoverExistingDatabase(workingDirectory);
        if (discoveredExistingPath != null) {
            return discoveredExistingPath;
        }

        Path defaultPath = defaultDatabasePath(workingDirectory);
        return defaultPath != null ? defaultPath : workingDirectory.resolve("pokemon-factory.db").toAbsolutePath().normalize();
    }

    private Path parseConfiguredPath(String configuredJdbcUrl, String configuredDbPath, Path workingDirectory) {
        if (StringUtils.hasText(configuredDbPath)) {
            return normalizePath(configuredDbPath, workingDirectory);
        }
        if (StringUtils.hasText(configuredJdbcUrl) && configuredJdbcUrl.startsWith("jdbc:sqlite:")) {
            String rawPath = configuredJdbcUrl.substring("jdbc:sqlite:".length()).trim();
            if (StringUtils.hasText(rawPath)) {
                return normalizePath(rawPath, workingDirectory);
            }
        }
        return null;
    }

    private Path normalizePath(String rawPath, Path workingDirectory) {
        Path parsedPath = Paths.get(rawPath);
        if (!parsedPath.isAbsolute()) {
            parsedPath = workingDirectory.resolve(parsedPath);
        }
        return parsedPath.toAbsolutePath().normalize();
    }

    private Path discoverExistingDatabase(Path workingDirectory) {
        for (Path base : ancestry(workingDirectory)) {
            Path direct = base.resolve("pokemon-factory.db");
            if (Files.exists(direct)) {
                return direct.toAbsolutePath().normalize();
            }

            Path backendDb = base.resolve("pokemon-factory-backend").resolve("pokemon-factory.db");
            if (Files.exists(backendDb)) {
                return backendDb.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private Path defaultDatabasePath(Path workingDirectory) {
        for (Path base : ancestry(workingDirectory)) {
            if ("pokemon-factory-backend".equalsIgnoreCase(String.valueOf(base.getFileName()))) {
                return base.resolve("pokemon-factory.db").toAbsolutePath().normalize();
            }

            Path backendDir = base.resolve("pokemon-factory-backend");
            if (Files.isDirectory(backendDir)) {
                return backendDir.resolve("pokemon-factory.db").toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private boolean parentDirectoryExists(Path path) {
        Path parent = path.getParent();
        return parent != null && Files.isDirectory(parent);
    }

    private Set<Path> ancestry(Path start) {
        Set<Path> paths = new LinkedHashSet<>();
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            paths.add(current);
            current = current.getParent();
        }
        return paths;
    }
}