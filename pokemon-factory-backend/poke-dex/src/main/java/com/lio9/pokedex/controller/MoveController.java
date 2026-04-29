package com.lio9.pokedex.controller;



import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.service.MoveService;
import com.lio9.pokedex.vo.MoveQueryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;

/**
 * 招式控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/moves")
public class MoveController {

    private static final Logger logger = LoggerFactory.getLogger(MoveController.class);
    private final MoveService moveService;

    public MoveController(MoveService moveService) {
        this.moveService = moveService;
    }

    /**
     * 分页获取招式列表
     */
    @GetMapping("/list")
    public Map<String, Object> getMoveList(MoveQueryVO queryVO) {
        long startTime = System.currentTimeMillis();
        logger.info("获取招式列表 - 参数: current={}, size={}", queryVO.getCurrent(), queryVO.getSize());

        Page<Move> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Move> movePage = moveService.page(page);

        long endTime = System.currentTimeMillis();
        logger.info("获取招式列表成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), movePage.getTotal());
        
        return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", movePage);
    }

    /**
     * 获取招式详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getMoveDetail(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        logger.info("获取招式详情 - ID: {}", id);

        Move move = moveService.getById(id);

        long endTime = System.currentTimeMillis();
        if (move != null) {
            logger.info("获取招式详情成功 - 耗时: {}ms", (endTime - startTime));
            return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", move);
        } else {
            logger.warn("获取招式详情失败 - 找不到ID为{}的招式, 耗时: {}ms", id, (endTime - startTime));
            return ResultResponse.buildCustomErrorResponse(ResponseCode.NOT_FOUND, "招式不存在", null);
        }
    }

    /**
     * 搜索招式
     */
    @GetMapping("/search")
    public Map<String, Object> searchMoves(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        long startTime = System.currentTimeMillis();
        logger.info("搜索招式 - 关键词: {}, current={}, size={}", keyword, current, size);

        Page<Move> page = new Page<>(current, size);
        Page<Move> movePage = moveService.page(page);

        long endTime = System.currentTimeMillis();
        logger.info("搜索招式成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), movePage.getTotal());
        
        return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", movePage);
    }
}

