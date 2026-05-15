package com.lio9.pokedex.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.PokemonFormType;
import com.lio9.pokedex.mapper.PokemonFormTypeMapper;
import com.lio9.pokedex.service.PokemonFormTypeService;
import org.springframework.stereotype.Service;

/**
 * 宝可梦形态属性服务实现
 */
@Service
public class PokemonFormTypeServiceImpl extends ServiceImpl<PokemonFormTypeMapper, PokemonFormType> implements PokemonFormTypeService {
}
