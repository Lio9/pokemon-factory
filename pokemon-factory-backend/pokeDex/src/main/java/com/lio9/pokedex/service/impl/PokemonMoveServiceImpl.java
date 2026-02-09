package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonMoveMapper;
import com.lio9.common.model.PokemonMove;
import com.lio9.common.service.PokemonMoveService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦技能服务实现类
 * 创建人: Lio9
 */
@Service
public class PokemonMoveServiceImpl extends ServiceImpl<PokemonMoveMapper, PokemonMove> implements PokemonMoveService {
}