package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonMove;
import com.lio9.pokedex.mapper.PokemonMoveMapper;
import com.lio9.pokedex.service.PokemonMoveService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦技能服务实现
 */
@Service
public class PokemonMoveServiceImpl extends ServiceImpl<PokemonMoveMapper, PokemonMove> implements PokemonMoveService {
}
