package com.lio9.common.config;

/**
 * 数据库方言接口
 * 
 * 抽象不同数据库的差异，支持SQLite、MySQL、PostgreSQL等多种数据库后端
 */
public interface DatabaseDialect {
    
    /**
     * 获取数据库类型名称
     */
    String getDbType();
    
    /**
     * 获取分页语法
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 分页SQL片段
     */
    String getPaginationSyntax(int offset, int limit);
    
    /**
     * 获取UPSERT语句（插入或更新）
     * @param table 表名
     * @param keys 唯一键列表
     * @return UPSERT SQL语句模板
     */
    String getUpsertStatement(String table, java.util.List<String> keys);
    
    /**
     * 是否支持JSON列类型
     */
    boolean supportsJsonColumn();
    
    /**
     * 获取当前时间戳函数
     */
    String getCurrentTimestampFunction();
    
    /**
     * 获取LIMIT子句
     * @param limit 限制数量
     * @return LIMIT SQL片段
     */
    String getLimitClause(int limit);
    
    /**
     * 获取OFFSET子句
     * @param offset 偏移量
     * @return OFFSET SQL片段
     */
    String getOffsetClause(int offset);
    
    /**
     * 是否支持RETURNING子句
     */
    boolean supportsReturningClause();
    
    /**
     * 转义标识符（表名、列名等）
     * @param identifier 标识符
     * @return 转义后的标识符
     */
    String escapeIdentifier(String identifier);
    
    /**
     * 获取布尔值 true 的表示
     */
    String getBooleanTrue();
    
    /**
     * 获取布尔值 false 的表示
     */
    String getBooleanFalse();
}
