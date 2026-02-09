package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonIvMapper;
import com.lio9.common.model.PokemonIv;
import com.lio9.common.service.PokemonIvService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦个体值服务实现类
 * 创建人: Lio9
 */
@Service
public class PokemonIvServiceImpl extends ServiceImpl<PokemonIvMapper, PokemonIv> implements PokemonIvService {
}