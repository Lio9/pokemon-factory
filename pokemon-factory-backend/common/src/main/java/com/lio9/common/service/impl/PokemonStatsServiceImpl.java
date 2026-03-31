package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonStatsMapper;
import com.lio9.common.model.PokemonStats;
import com.lio9.common.service.PokemonStatsService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦种族值服务实现
 */
@Service
public class PokemonStatsServiceImpl extends ServiceImpl<PokemonStatsMapper, PokemonStats> implements PokemonStatsService {
}