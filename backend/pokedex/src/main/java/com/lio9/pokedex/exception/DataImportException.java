package com.lio9.pokedex.exception;



/**
 * 数据导入异常
 * 当数据导入过程中发生错误时抛出
 *
 * @author Lio9
 * @version 1.0
 */
public class DataImportException extends BusinessException {

    public DataImportException(String message) {
        super("DATA_IMPORT_ERROR", message);
    }

    public DataImportException(String message, Throwable cause) {
        super("DATA_IMPORT_ERROR", message, cause);
    }

    public DataImportException(String importType, String reason) {
        super("DATA_IMPORT_ERROR", String.format("%s 数据导入失败: %s", importType, reason));
    }
}