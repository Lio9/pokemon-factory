package com.lio9.pokedex.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonFormAbility;
import com.lio9.pokedex.mapper.PokemonFormAbilityMapper;
import com.lio9.pokedex.service.PokemonFormAbilityService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦形态特性服务实现
 */
@Service
public class PokemonFormAbilityServiceImpl extends ServiceImpl<PokemonFormAbilityMapper, PokemonFormAbility> implements PokemonFormAbilityService {
}
