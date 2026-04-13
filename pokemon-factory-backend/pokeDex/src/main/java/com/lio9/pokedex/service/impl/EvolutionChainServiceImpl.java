package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.EvolutionChain;
import com.lio9.pokedex.mapper.EvolutionChainMapper;
import com.lio9.pokedex.service.EvolutionChainService;
import org.springframework.stereotype.Service;

/**
 * 进化链服务实现
 */
@Service
public class EvolutionChainServiceImpl extends ServiceImpl<EvolutionChainMapper, EvolutionChain> implements EvolutionChainService {
}
