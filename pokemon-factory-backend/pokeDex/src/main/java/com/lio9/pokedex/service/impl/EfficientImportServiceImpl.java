package com.lio9.pokedex.service.impl;

import com.lio9.pokedex.service.EfficientImportService;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 高效导入服务实现类
 * 提供高效的混合导入功能，结合Python异步网络请求和Java数据库操作
 * 创建人: Lio9
 */
@Service
public class EfficientImportServiceImpl implements EfficientImportService {

    private static final Logger logger = LoggerFactory.getLogger(EfficientImportServiceImpl.class);

    @Value("${import.timeout.minutes:120}")
    private int importTimeoutMinutes;

    @Value("${import.script.path:scripts/import_scheduler.py}")
    private String scriptPath;

    @Value("${import.script.directory:scripts}")
    private String scriptDirectory;

    @Value("${import.log.directory:logs}")
    private String logDirectory;

    @Value("${user.dir}")
    private String projectRootDirectory;
    
    /**
     * 调用高效的Python混合导入脚本
     */
    @Override
    public Map<String, Object> callPythonSchedulerImport() {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("调用高效的Python混合导入脚本，超时时间: {} 分钟", importTimeoutMinutes);

            // 检查Python脚本是否存在
            File scriptFile = new File(projectRootDirectory, scriptPath);
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
            pb.directory(new File(projectRootDirectory, scriptDirectory));

            // 重定向输出到日志文件
            File logFile = new File(projectRootDirectory, logDirectory + "/efficient_import.log");
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
     * 调用高效的Python混合导入脚本（指定类型）
     */
    @Override
    public Map<String, Object> callPythonSchedulerImportType(String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("调用高效的Python混合导入脚本，类型: {}, 超时时间: {} 分钟", type, importTimeoutMinutes);

            // 检查Python脚本是否存在
            File scriptFile = new File(projectRootDirectory, scriptPath);
            if (!scriptFile.exists()) {
                logger.error("Python导入脚本不存在: {}", scriptFile.getAbsolutePath());
                result.put("success", false);
                result.put("error", "Python导入脚本不存在");
                return result;
            }

            // 执行Python脚本
            ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptFile.getAbsolutePath(),
                "--type", type
            );

            // 设置工作目录
            pb.directory(new File(projectRootDirectory, scriptDirectory));

            // 重定向输出到日志文件
            File logFile = new File(projectRootDirectory, logDirectory + "/efficient_import_" + type + ".log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            Process process = pb.start();
            
            // 等待进程完成，使用配置的超时时间
            boolean completed = process.waitFor(importTimeoutMinutes, TimeUnit.MINUTES);
            
            if (completed) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("Python高效导入脚本执行成功，类型: {}", type);
                    result.put("success", true);
                    result.put("message", "Python高效导入脚本执行成功，类型: " + type);
                } else {
                    logger.error("Python高效导入脚本执行失败，类型: {}, 退出码: {}", type, exitCode);
                    result.put("success", false);
                    result.put("error", "Python高效导入脚本执行失败，类型: " + type + "，退出码: " + exitCode);
                }
            } else {
                logger.error("Python高效导入脚本执行超时，类型: {}, 超时时间: {} 分钟", type, importTimeoutMinutes);
                result.put("success", false);
                result.put("error", "Python高效导入脚本执行超时，类型: " + type + "，超时时间: " + importTimeoutMinutes + " 分钟");
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
    @Override
    public Map<String, Object> getImportStatus(String taskId) {
        Map<String, Object> result = new HashMap<>();

        try {
            File logFile = new File(projectRootDirectory, logDirectory + "/efficient_import.log");
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
    
    /**
     * Java代码直接导入所有数据
     */
    @Override
    public Map<String, Object> callJavaDirectImport() {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("Java代码直接导入所有数据");

            // 直接调用Java代码进行导入
            File scriptFile = new File(projectRootDirectory, scriptPath);
            ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptFile.getAbsolutePath()
            );

            pb.directory(new File(projectRootDirectory, scriptDirectory));

            File logFile = new File(projectRootDirectory, logDirectory + "/java_direct_import.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            Process process = pb.start();
            
            boolean completed = process.waitFor(importTimeoutMinutes, TimeUnit.MINUTES);
            
            if (completed) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("Java代码直接导入脚本执行成功");
                    result.put("success", true);
                    result.put("message", "Java代码直接导入脚本执行成功");
                } else {
                    logger.error("Java代码直接导入脚本执行失败，退出码: {}", exitCode);
                    result.put("success", false);
                    result.put("error", "Java代码直接导入脚本执行失败，退出码: " + exitCode);
                }
            } else {
                logger.error("Java代码直接导入脚本执行超时，超时时间: {} 分钟", importTimeoutMinutes);
                result.put("success", false);
                result.put("error", "Java代码直接导入脚本执行超时，超时时间: " + importTimeoutMinutes + " 分钟");
                process.destroyForcibly();
            }
            
        } catch (Exception e) {
            logger.error("调用Java代码直接导入脚本异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "调用Java代码直接导入脚本异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Java代码直接导入特定类型数据
     */
    @Override
    public Map<String, Object> callJavaDirectImportType(String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("Java代码直接导入特定类型数据，类型: {}", type);

            // 直接调用Java代码进行导入
            File scriptFile = new File(projectRootDirectory, scriptPath);
            ProcessBuilder pb = new ProcessBuilder(
                "python",
                scriptFile.getAbsolutePath(),
                "--type", type
            );

            pb.directory(new File(projectRootDirectory, scriptDirectory));

            File logFile = new File(projectRootDirectory, logDirectory + "/java_direct_import_" + type + ".log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            Process process = pb.start();
            
            boolean completed = process.waitFor(importTimeoutMinutes, TimeUnit.MINUTES);
            
            if (completed) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("Java代码直接导入脚本执行成功，类型: {}", type);
                    result.put("success", true);
                    result.put("message", "Java代码直接导入脚本执行成功，类型: " + type);
                } else {
                    logger.error("Java代码直接导入脚本执行失败，类型: {}, 退出码: {}", type, exitCode);
                    result.put("success", false);
                    result.put("error", "Java代码直接导入脚本执行失败，类型: " + type + "，退出码: " + exitCode);
                }
            } else {
                logger.error("Java代码直接导入脚本执行超时，类型: {}, 超时时间: {} 分钟", type, importTimeoutMinutes);
                result.put("success", false);
                result.put("error", "Java代码直接导入脚本执行超时，类型: " + type + "，超时时间: " + importTimeoutMinutes + " 分钟");
                process.destroyForcibly();
            }
            
        } catch (Exception e) {
            logger.error("调用Java代码直接导入脚本异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "调用Java代码直接导入脚本异常: " + e.getMessage());
        }
        
        return result;
    }
}
