package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.EvolutionChainMapper;
import com.lio9.common.model.EvolutionChain;
import com.lio9.common.service.EvolutionChainService;
import org.springframework.stereotype.Service;

/**
 * 进化链服务实现类
 * 创建人: Lio9
 */
@Service
public class EvolutionChainServiceImpl extends ServiceImpl<EvolutionChainMapper, EvolutionChain> implements EvolutionChainService {
}