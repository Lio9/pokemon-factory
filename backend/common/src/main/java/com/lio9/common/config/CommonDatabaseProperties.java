package com.lio9.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Database configuration properties 数据库配置属性
 * <p>
 * Controls schema migration on startup. Full DB initialization is handled
 * by Python scripts in scripts/, this only handles safe migrations.
 * 启动时仅执行安全的 schema 兼容性迁移，完整初始化由 scripts/ 下的 Python 脚本完成。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "pokemon-factory.database")
public class CommonDatabaseProperties {
    /** Whether to run schema migration on startup. 启动时是否执行 schema 迁移 */
    private boolean migrateOnStartup;

    /** (Deprecated) Local CSV directory path. 已废弃的本地 CSV 目录路径 */
    private String csvDirectory;

    /** Remote CSV base URL for PokeAPI data download. 远程 CSV 下载根地址 */
    private String remoteCsvBaseUrl;

    /** CSV download cache directory. CSV 下载缓存目录 */
    private String csvCacheDirectory;

    public boolean isMigrateOnStartup() { return migrateOnStartup; }
    private boolean importCsvOnStartup;

    public boolean isImportCsvOnStartup() { return importCsvOnStartup; }
    public void setImportCsvOnStartup(boolean v) { this.importCsvOnStartup = v; }

    public void setMigrateOnStartup(boolean v) { this.migrateOnStartup = v; }

    public String getCsvDirectory() { return csvDirectory; }
    public void setCsvDirectory(String v) { this.csvDirectory = v; }

    public String getRemoteCsvBaseUrl() { return remoteCsvBaseUrl; }
    public void setRemoteCsvBaseUrl(String v) { this.remoteCsvBaseUrl = v; }

    public String getCsvCacheDirectory() { return csvCacheDirectory; }
    public void setCsvCacheDirectory(String v) { this.csvCacheDirectory = v; }
}
