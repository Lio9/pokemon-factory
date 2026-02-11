package com.lio9.pokedex.service;

import java.util.Map;

/**
 * 高效导入服务接口
 * 提供高效的混合导入功能，结合Python异步网络请求和Java数据库操作
 * 支持多种导入类型和状态监控
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
public interface EfficientImportService {
    
    /**
     * 调用高效的Python混合导入脚本
     * 执行默认类型的混合导入流程
     * 
     * @return 导入结果状态
     */
    Map<String, Object> callPythonSchedulerImport();
    
    /**
     * 调用高效的Python混合导入脚本（指定类型）
     * 根据指定类型执行对应的混合导入流程
     * 
     * @param type 导入类型
     * @return 导入结果状态
     */
    Map<String, Object> callPythonSchedulerImportType(String type);
    
    /**
     * 获取导入状态
     * 根据任务ID获取导入任务的实时状态
     * 
     * @param taskId 任务ID
     * @return 导入状态信息
     */
    Map<String, Object> getImportStatus(String taskId);
}