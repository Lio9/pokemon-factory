package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonEv;
import com.lio9.pokedex.mapper.PokemonEvMapper;
import com.lio9.pokedex.service.PokemonEvService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦进化服务实现
 */
@Service
public class PokemonEvServiceImpl extends ServiceImpl<PokemonEvMapper, PokemonEv> implements PokemonEvService {
}
