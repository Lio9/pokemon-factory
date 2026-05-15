package com.lio9.pokedex.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonEggGroup;
import com.lio9.pokedex.mapper.PokemonEggGroupMapper;
import com.lio9.pokedex.service.PokemonEggGroupService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦蛋群服务实现
 */
@Service
public class PokemonEggGroupServiceImpl extends ServiceImpl<PokemonEggGroupMapper, PokemonEggGroup> implements PokemonEggGroupService {
}
