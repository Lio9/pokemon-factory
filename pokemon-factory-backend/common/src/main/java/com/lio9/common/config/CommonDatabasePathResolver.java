package com.lio9.common.config;

import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 统一解析 SQLite 数据库文件路径。
 * <p>
 * 过去默认路径依赖 user.dir，导致：
 * 1. 从聚合根、模块根、IDE 运行配置分别启动时，可能指向不同目录；
 * 2. 一旦默认拼接路径不存在，SQLite 会在错误位置新建库文件，看起来像“项目里有很多 db”；
 * 3. battleFactory 的 SpringBootTest 在不同 Maven 工作目录下不稳定。
 * </p>
 */
public class CommonDatabasePathResolver {

    public String resolveJdbcUrl(String configuredJdbcUrl, String configuredDbPath) {
        return resolveJdbcUrl(configuredJdbcUrl, configuredDbPath, currentWorkingDirectory());
    }

    String resolveJdbcUrl(String configuredJdbcUrl, String configuredDbPath, Path workingDirectory) {
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

    private Path currentWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}