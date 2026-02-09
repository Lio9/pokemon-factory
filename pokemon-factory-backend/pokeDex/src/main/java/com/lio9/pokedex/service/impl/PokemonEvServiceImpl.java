package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonEvMapper;
import com.lio9.common.model.PokemonEv;
import com.lio9.common.service.PokemonEvService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦努力值服务实现类
 * 创建人: Lio9
 */
@Service
public class PokemonEvServiceImpl extends ServiceImpl<PokemonEvMapper, PokemonEv> implements PokemonEvService {
}