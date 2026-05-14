package com.lio9.common.config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQLite数据库方言实现
 */
public class SQLiteDialect implements DatabaseDialect {
    
    @Override
    public String getDbType() {
        return "sqlite";
    }
    
    @Override
    public String getPaginationSyntax(int offset, int limit) {
        return "LIMIT " + limit + " OFFSET " + offset;
    }
    
    @Override
    public String getUpsertStatement(String table, List<String> keys) {
        // SQLite使用 INSERT OR REPLACE 或 INSERT ... ON CONFLICT
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(escapeIdentifier(table));
        sql.append(" VALUES (");
        // 占位符由调用方填充
        sql.append(")");
        sql.append(" ON CONFLICT (");
        sql.append(keys.stream().map(this::escapeIdentifier).collect(Collectors.joining(", ")));
        sql.append(") DO UPDATE SET ");
        // SET子句由调用方填充
        return sql.toString();
    }
    
    @Override
    public boolean supportsJsonColumn() {
        // SQLite 3.38+ 支持JSON函数，但没有原生JSON类型
        return false;
    }
    
    @Override
    public String getCurrentTimestampFunction() {
        return "datetime('now')";
    }
    
    @Override
    public String getLimitClause(int limit) {
        return "LIMIT " + limit;
    }
    
    @Override
    public String getOffsetClause(int offset) {
        return "OFFSET " + offset;
    }
    
    @Override
    public boolean supportsReturningClause() {
        // SQLite 3.35+ 支持RETURNING
        return true;
    }
    
    @Override
    public String escapeIdentifier(String identifier) {
        // SQLite使用双引号转义标识符
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
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
