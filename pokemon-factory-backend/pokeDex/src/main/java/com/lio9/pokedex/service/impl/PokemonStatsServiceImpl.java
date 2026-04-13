package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonStats;
import com.lio9.pokedex.mapper.PokemonStatsMapper;
import com.lio9.pokedex.service.PokemonStatsService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦种族值服务实现
 */
@Service
public class PokemonStatsServiceImpl extends ServiceImpl<PokemonStatsMapper, PokemonStats> implements PokemonStatsService {
}
