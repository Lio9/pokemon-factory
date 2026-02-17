package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Move;
import com.lio9.common.service.MoveService;
import com.lio9.common.vo.MoveQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;

/**
 * 招式控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/moves")
@CrossOrigin(origins = "*")
public class MoveController {

    @Autowired
    private MoveService moveService;

    /**
     * 分页获取招式列表
     */
    @GetMapping("/list")
    public Map<String, Object> getMoveList(MoveQueryVO queryVO) {
        Map<String, Object> result = new HashMap<>();
        Page<Move> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Move> movePage = moveService.page(page);

        return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", movePage);
    }

    /**
     * 获取招式详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getMoveDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Move move = moveService.getById(id);

        if (move != null) {
            return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", move);
        } else {
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

        Map<String, Object> result = new HashMap<>();
        Page<Move> page = new Page<>(current, size);
        Page<Move> movePage = moveService.page(page);

        return ResultResponse.buildSuccessResponse(ResponseCode.SUCCESS, "success", movePage);
    }
}
