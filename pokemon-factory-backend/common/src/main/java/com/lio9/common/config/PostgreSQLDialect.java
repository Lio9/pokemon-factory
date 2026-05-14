package com.lio9.common.config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL数据库方言实现
 */
public class PostgreSQLDialect implements DatabaseDialect {
    
    @Override
    public String getDbType() {
        return "postgresql";
    }
    
    @Override
    public String getPaginationSyntax(int offset, int limit) {
        return "LIMIT " + limit + " OFFSET " + offset;
    }
    
    @Override
    public String getUpsertStatement(String table, List<String> keys) {
        // PostgreSQL使用 INSERT ... ON CONFLICT DO UPDATE
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
        // PostgreSQL原生支持JSONB类型
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
        return "OFFSET " + offset;
    }
    
    @Override
    public boolean supportsReturningClause() {
        // PostgreSQL完全支持RETURNING
        return true;
    }
    
    @Override
    public String escapeIdentifier(String identifier) {
        // PostgreSQL使用双引号转义标识符
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
    
    @Override
    public String getBooleanTrue() {
        return "TRUE";
    }
    
    @Override
    public String getBooleanFalse() {
        return "FALSE";
    }
}
