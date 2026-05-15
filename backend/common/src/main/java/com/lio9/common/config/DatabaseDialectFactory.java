package com.lio9.common.config;

/**
 * 数据库方言工厂
 * 
 * 根据数据库类型创建对应的方言实现
 */
public class DatabaseDialectFactory {
    
    /**
     * 根据数据库类型获取方言实例
     * @param dbType 数据库类型（sqlite, postgresql, mysql）
     * @return 对应的方言实现
     */
    public static DatabaseDialect getDialect(String dbType) {
        if (dbType == null || dbType.isEmpty()) {
            // 默认使用SQLite
            return new SQLiteDialect();
        }
        
        switch (dbType.toLowerCase()) {
            case "sqlite":
                return new SQLiteDialect();
            case "postgresql":
            case "postgres":
                return new PostgreSQLDialect();
            case "mysql":
            case "mariadb":
                return new MySQLDialect();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
    
    /**
     * 从JDBC URL推断数据库类型
     * @param jdbcUrl JDBC连接URL
     * @return 数据库类型
     */
    public static String inferDbTypeFromUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return "sqlite";
        }
        
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            return "sqlite";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "postgresql";
        } else if (jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:mariadb:")) {
            return "mysql";
        } else {
            throw new IllegalArgumentException("Cannot infer database type from URL: " + jdbcUrl);
        }
    }
}
