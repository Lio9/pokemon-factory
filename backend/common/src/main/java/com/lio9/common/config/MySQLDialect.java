package com.lio9.common.config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL数据库方言实现
 */
public class MySQLDialect implements DatabaseDialect {
    
    @Override
    public String getDbType() {
        return "mysql";
    }
    
    @Override
    public String getPaginationSyntax(int offset, int limit) {
        return "LIMIT " + offset + ", " + limit;
    }
    
    @Override
    public String getUpsertStatement(String table, List<String> keys) {
        // MySQL使用 INSERT ... ON DUPLICATE KEY UPDATE
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(escapeIdentifier(table));
        sql.append(" VALUES (");
        // 占位符由调用方填充
        sql.append(")");
        sql.append(" ON DUPLICATE KEY UPDATE ");
        // SET子句由调用方填充
        return sql.toString();
    }
    
    @Override
    public boolean supportsJsonColumn() {
        // MySQL 5.7+ 支持JSON类型
        return true;
    }
    
    @Override
    public String getCurrentTimestampFunction() {
        return "NOW()";
    }
    
    @Override
    public String getLimitClause(int limit) {
        return "LIMIT " + limit;
    }
    
    @Override
    public String getOffsetClause(int offset) {
        // MySQL的OFFSET需要和LIMIT一起使用
        return "";
    }
    
    @Override
    public boolean supportsReturningClause() {
        // MySQL 8.0.21+ 支持RETURNING
        return true;
    }
    
    @Override
    public String escapeIdentifier(String identifier) {
        // MySQL使用反引号转义标识符
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        return "`" + identifier.replace("`", "``") + "`";
    }
    
    @Override
    public String getBooleanTrue() {
        return "1";
    }
    
    @Override
    public String getBooleanFalse() {
        return "0";
    }
}
