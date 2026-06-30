package com.lio9.common.config;

import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SQLite database path resolver
 * Always uses backend/pokemon-factory.db relative to project root
 * to prevent duplicate database files regardless of working directory.
 */
public class CommonDatabasePathResolver {
    public String resolveJdbcUrl(String a, String b) {
        return resolveJdbcUrl(a, b, currentWorkingDirectory());
    }
    String resolveJdbcUrl(String a, String b, Path wd) {
        return "jdbc:sqlite:" + resolvePath(a, b, wd).toAbsolutePath().normalize();
    }
    private Path resolvePath(String url, String dbPath, Path wd) {
        Path p = parseConfiguredPath(url, dbPath, wd);
        if (p != null && (Files.exists(p) || parentDirectoryExists(p))) return p;
        Path root = findProjectRoot(wd);
        if (root != null) {
            Path db = root.resolve("backend").resolve("pokemon-factory.db").normalize();
            Path parent = db.getParent();
            if (parent != null) { try { Files.createDirectories(parent); } catch (Exception e) {} }
            return db;
        }
        Path fb = wd.resolve("backend").resolve("pokemon-factory.db").normalize();
        Path p2 = fb.getParent();
        if (p2 != null) { try { Files.createDirectories(p2); } catch (Exception e) {} }
        return fb;
    }
    private Path findProjectRoot(Path wd) {
        for (Path base : ancestry(wd)) {
            if (Files.exists(base.resolve("backend").resolve("pom.xml"))) return base;
        }
        return null;
    }
    private Path parseConfiguredPath(String url, String dbPath, Path wd) {
        if (StringUtils.hasText(dbPath)) return normalizePath(dbPath, wd);
        if (StringUtils.hasText(url) && url.startsWith("jdbc:sqlite:")) {
            String r = url.substring("jdbc:sqlite:".length()).trim();
            if (StringUtils.hasText(r)) return normalizePath(r, wd);
        }
        return null;
    }
    private Path normalizePath(String raw, Path wd) {
        Path p = Paths.get(raw);
        return p.isAbsolute() ? p.toAbsolutePath().normalize() : wd.resolve(p).toAbsolutePath().normalize();
    }
    private boolean parentDirectoryExists(Path p) {
        Path parent = p.getParent();
        return parent != null && Files.isDirectory(parent);
    }
    private Set<Path> ancestry(Path start) {
        Set<Path> s = new LinkedHashSet<>();
        Path c = start.toAbsolutePath().normalize();
        while (c != null) { s.add(c); c = c.getParent(); }
        return s;
    }
    private Path currentWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}
