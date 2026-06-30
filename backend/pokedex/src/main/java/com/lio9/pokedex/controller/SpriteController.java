package com.lio9.pokedex.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pokemon image serving controller 宝可梦图片服务控制器
 * <p>
 * Serves sprite images from the data/image/ directory as a standard Spring MVC controller.
 * This ensures requests go through the normal security pipeline.
 * 从 data/image/ 目录提供精灵图片，走标准 Spring MVC 控制器流程（可被 SecurityConfig 正确处理）。
 * Falls back to Unown_QU.png when the requested image doesn't exist.
 * 当请求的图片不存在时，回退到 Unown_QU.png 默认图片。
 * </p>
 */
@RestController
public class SpriteController {

    private final Path imageRoot;

    public SpriteController() {
        // Resolve the data/image/ directory from project root or backend/ directory
        // 定位 data/image/ 目录，支持从项目根目录或 backend/ 目录启动
        Path root = Paths.get("data", "image").toAbsolutePath().normalize();
        if (!root.toFile().isDirectory()) {
            root = Paths.get("..", "data", "image").toAbsolutePath().normalize();
        }
        this.imageRoot = root;
    }

    /** Serve image from subdirectory (e.g. /api/pokedex/images/pokemon/1.png) */
    @GetMapping("/api/pokedex/images/{subdir}/{filename:.+}")
    public ResponseEntity<Resource> serveSubdir(
            @PathVariable String subdir,
            @PathVariable String filename) {
        return serve(subdir + "/" + filename);
    }

    /** Serve image at root level (e.g. /api/pokedex/images/Unown_QU.png) */
    @GetMapping("/api/pokedex/images/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        return serve(filename);
    }

    private ResponseEntity<Resource> serve(String relativePath) {
        Path file = imageRoot.resolve(relativePath).normalize();
        // Prevent path traversal attacks 防止路径穿越攻击
        if (!file.startsWith(imageRoot)) {
            return serveDefault();
        }
        if (file.toFile().isFile()) {
            return ResponseEntity.ok()
                .contentType(detectMediaType(file.getFileName().toString()))
                .body(new FileSystemResource(file));
        }
        return serveDefault();
    }

    /** Return the default Unown_QU.png as fallback 返回默认图片 Unown_QU.png */
    private ResponseEntity<Resource> serveDefault() {
        Path fallback = imageRoot.resolve("Unown_QU.png");
        if (fallback.toFile().isFile()) {
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(fallback));
        }
        return ResponseEntity.notFound().build();
    }

    /** Detect MediaType from filename extension 根据文件扩展名判断媒体类型 */
    private MediaType detectMediaType(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (filename.endsWith(".webp")) return MediaType.valueOf("image/webp");
        return MediaType.IMAGE_PNG;
    }
}
