package com.lio9.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * common 数据库配置属性。
 * <p>
 * 这里把数据库初始化行为单独抽成配置对象，原因有两个：
 * 1. common 自己启动时需要开启初始化；
 * 2. battleFactory / pokeDex 等业务模块虽然也会加载同一套数据库配置，
 *    但它们只应该复用连接能力，不应该重复执行初始化脚本。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "pokemon-factory.database")
public class CommonDatabaseProperties {
    /**
     * 是否在当前进程启动时执行数据库初始化。
     */
    private boolean initializeOnStartup;

    /**
     * 初始化脚本列表。
     * <p>
     * 顺序非常重要：核心公共表要先建，对战和用户表再跟进，
     * 这样可以保证外键和扩展表总是在基础表之后创建。
     * </p>
     */
    private List<String> bootstrapScripts = new ArrayList<>();

    /**
     * 是否在建表后继续执行 CSV 数据导入。
     */
    private boolean importCsvOnStartup;

    /**
     * 已废弃的本地 CSV 目录配置。
     * <p>
     * 当前版本统一从远程 CSV 源下载并导入数据；保留该配置仅用于兼容旧环境，
     * 运行时会打印警告并忽略其值。
     * </p>
     */
    private String csvDirectory;

    /**
     * 远程 CSV 根地址。
     * <p>
     * 当前版本会从该地址按需下载 `*.csv` 到本地缓存目录后再导入。
     * 例如：
     * https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv
     * </p>
     */
    private String remoteCsvBaseUrl;

    /**
     * 远程 CSV 下载缓存目录。
     * 默认允许为空，运行时会回退到系统临时目录下的 pokemon-factory/csv-cache。
     */
    private String csvCacheDirectory;

    public boolean isInitializeOnStartup() {
        return initializeOnStartup;
    }

    public void setInitializeOnStartup(boolean initializeOnStartup) {
        this.initializeOnStartup = initializeOnStartup;
    }

    public List<String> getBootstrapScripts() {
        return bootstrapScripts;
    }

    public void setBootstrapScripts(List<String> bootstrapScripts) {
        this.bootstrapScripts = bootstrapScripts;
    }

    public boolean isImportCsvOnStartup() {
        return importCsvOnStartup;
    }

    public void setImportCsvOnStartup(boolean importCsvOnStartup) {
        this.importCsvOnStartup = importCsvOnStartup;
    }

    public String getCsvDirectory() {
        return csvDirectory;
    }

    public void setCsvDirectory(String csvDirectory) {
        this.csvDirectory = csvDirectory;
    }

    public String getRemoteCsvBaseUrl() {
        return remoteCsvBaseUrl;
    }

    public void setRemoteCsvBaseUrl(String remoteCsvBaseUrl) {
        this.remoteCsvBaseUrl = remoteCsvBaseUrl;
    }

    public String getCsvCacheDirectory() {
        return csvCacheDirectory;
    }

    public void setCsvCacheDirectory(String csvCacheDirectory) {
        this.csvCacheDirectory = csvCacheDirectory;
    }
}
