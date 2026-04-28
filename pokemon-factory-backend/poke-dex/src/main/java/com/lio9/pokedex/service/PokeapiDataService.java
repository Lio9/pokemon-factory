package com.lio9.pokedex.service;



/**
 * PokeapiDataService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * PokeAPI数据服务接口
 * 负责处理宝可梦数据的获取、导入和基础操作
 * 提供优化的批量导入功能和进度监控
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
public interface PokeapiDataService {
    
    /**
     * 导入单个宝可梦数据
     * 根据宝可梦ID从PokeAPI获取并导入单个宝可梦的完整数据
     * 
     * @param id 宝可梦ID
     */
    void importPokemonById(int id);
    
    /**
     * 从PokeAPI获取宝可梦数据（优化版）
     * 执行优化的批量导入流程，提高导入效率
     * 
     * @return 导入结果状态
     */
    Map<String, Object> importAllPokemonDataOptimized();
    
    /**
     * 异步导入所有宝可梦数据
     * 使用CompletableFuture实现异步导入，避免长时间阻塞
     * 
     * @return 异步任务结果
     */
    CompletableFuture<Map<String, Object>> importAllPokemonDataAsync();
    
    /**
     * 获取导入进度状态
     * 提供实时的导入进度信息，用于前端进度显示
     * 
     * @return 进度状态信息
     */
    Map<String, Object> getImportProgressStatus();

    /**
     * 获取导入相关性能统计
     * 用于控制器和前端展示请求成功率、平均耗时与缓存规模。
     *
     * @return 性能统计信息
     */
    Map<String, Object> getPerformanceStats();
}