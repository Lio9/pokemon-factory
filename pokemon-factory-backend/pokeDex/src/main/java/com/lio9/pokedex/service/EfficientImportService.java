package com.lio9.pokedex.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 高效导入服务
 * 提供高效的混合导入功能，结合Python异步网络请求和Java数据库操作
 */
@Service
public class EfficientImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(EfficientImportService.class);
    
    @Value("${import.timeout.minutes:120}")
    private int importTimeoutMinutes;
    
    /**
     * 调用高效的Python混合导入脚本
     */
    public Map<String, Object> callEfficientPythonImport() {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("调用高效的Python混合导入脚本，超时时间: {} 分钟", importTimeoutMinutes);
            
            // 检查Python脚本是否存在
            File scriptFile = new File("D:\\learn\\pokemon-factory\\scripts\\efficient_import.py");
            if (!scriptFile.exists()) {
                logger.error("Python导入脚本不存在: {}", scriptFile.getAbsolutePath());
                result.put("success", false);
                result.put("error", "Python导入脚本不存在");
                return result;
            }
            
            // 执行Python脚本
            ProcessBuilder pb = new ProcessBuilder(
                "python", 
                scriptFile.getAbsolutePath()
            );
            
            // 设置工作目录
            pb.directory(new File("D:\\learn\\pokemon-factory\\scripts"));
            
            // 重定向输出到日志文件
            File logFile = new File("D:\\learn\\pokemon-factory\\logs\\efficient_import.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            Process process = pb.start();
            
            // 等待进程完成，使用配置的超时时间
            boolean completed = process.waitFor(importTimeoutMinutes, TimeUnit.MINUTES);
            
            if (completed) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("Python高效导入脚本执行成功");
                    result.put("success", true);
                    result.put("message", "Python高效导入脚本执行成功");
                } else {
                    logger.error("Python高效导入脚本执行失败，退出码: {}", exitCode);
                    result.put("success", false);
                    result.put("error", "Python高效导入脚本执行失败，退出码: " + exitCode);
                }
            } else {
                logger.error("Python高效导入脚本执行超时，超时时间: {} 分钟", importTimeoutMinutes);
                result.put("success", false);
                result.put("error", "Python高效导入脚本执行超时，超时时间: " + importTimeoutMinutes + " 分钟");
                process.destroyForcibly();
            }
            
        } catch (Exception e) {
            logger.error("调用Python高效导入脚本异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "调用Python高效导入脚本异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取导入状态
     */
    public Map<String, Object> getImportStatus(String taskId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            File logFile = new File("D:\\learn\\pokemon-factory\\logs\\efficient_import.log");
            if (logFile.exists()) {
                String content = new String(Files.readAllBytes(logFile.toPath()));
                if (content.contains("✅ 所有宝可梦数据导入完成！")) {
                    result.put("taskId", taskId);
                    result.put("status", "completed");
                    result.put("message", "高效混合导入已完成");
                } else if (content.contains("❌ 导入过程中发生错误")) {
                    result.put("taskId", taskId);
                    result.put("status", "failed");
                    result.put("message", "高效混合导入失败");
                } else {
                    result.put("taskId", taskId);
                    result.put("status", "running");
                    result.put("message", "高效混合导入正在进行中...");
                }
            } else {
                result.put("taskId", taskId);
                result.put("status", "pending");
                result.put("message", "高效混合导入尚未开始");
            }
        } catch (Exception e) {
            logger.error("获取导入状态失败: {}", e.getMessage());
            result.put("error", "获取导入状态失败: " + e.getMessage());
        }
        
        return result;
    }
}