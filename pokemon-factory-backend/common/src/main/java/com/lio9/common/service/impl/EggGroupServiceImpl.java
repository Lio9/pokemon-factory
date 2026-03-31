package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.EggGroupMapper;
import com.lio9.common.model.EggGroup;
import com.lio9.common.service.EggGroupService;
import org.springframework.stereotype.Service;

/**
 * 蛋群服务实现
 */
@Service
public class EggGroupServiceImpl extends ServiceImpl<EggGroupMapper, EggGroup> implements EggGroupService {
}