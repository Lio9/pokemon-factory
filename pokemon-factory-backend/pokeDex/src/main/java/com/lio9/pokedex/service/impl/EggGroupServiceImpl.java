package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.EggGroup;
import com.lio9.pokedex.mapper.EggGroupMapper;
import com.lio9.pokedex.service.EggGroupService;
import org.springframework.stereotype.Service;

/**
 * 蛋群服务实现
 */
@Service
public class EggGroupServiceImpl extends ServiceImpl<EggGroupMapper, EggGroup> implements EggGroupService {
}
